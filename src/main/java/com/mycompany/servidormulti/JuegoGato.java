package com.mycompany.servidormulti;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JuegoGato {
    private final String jugador1Id;
    private final String jugador2Id;
    private final String jugador1Nombre;
    private final String jugador2Nombre;
    private final UnCliente cliente1;
    private final UnCliente cliente2;

    private char[][] tablero = new char[3][3];
    private String turnoActualId;
    private String turnoActualNombre;
    private char simboloJugador1;
    private char simboloJugador2;
    private boolean juegoTerminado;
    private String ganador;

    public JuegoGato(String jugador1Id, String jugador1Nombre, String jugador2Id, String jugador2Nombre, UnCliente cliente1, UnCliente cliente2){
        this.jugador1Id = jugador1Id;
        this.jugador1Nombre = jugador1Nombre;
        this.jugador2Id = jugador2Id;
        this.jugador2Nombre = jugador2Nombre;
        this.cliente1 = cliente1;
        this.cliente2 = cliente2;
        this.tablero = new char[3][3];
        this.juegoTerminado = false;

        inicializarTablero();
        determinarQuienEmpieza();
    }

    private void inicializarTablero(){
        for(int i=0; i<3; i++){
            for(int j=0; j<3; j++){
                tablero[i][j] = '-';
            }
        }
    }

    private void determinarQuienEmpieza(){
        if(Math.random()<0.5){
            turnoActualId = jugador1Id;
            turnoActualNombre = jugador1Nombre;
            simboloJugador1 = 'X';
            simboloJugador2 = 'O';
        } else {
            turnoActualId = jugador2Id;
            turnoActualNombre = jugador2Nombre;
            simboloJugador1 = 'O';
            simboloJugador2 = 'X';
        }
    }
    public void iniciarJuego(){
     try{
         String mensaje = "\n=== JUEGO DE GATO INICIADO ===\n";
         cliente1.salida.writeUTF(mensaje);
         cliente2.salida.writeUTF(mensaje);

         cliente1.salida.writeUTF("Juegas con el simbolo " + simboloJugador1);
         cliente2.salida.writeUTF("Juegas con el simbolo " + simboloJugador2);

         String mensajeTurno = "Empieza " + turnoActualNombre;
         cliente1.salida.writeUTF(mensajeTurno);
         cliente2.salida.writeUTF(mensajeTurno);

         enviarTablero();
         enviarInstrucciones();
     }catch (IOException e){
         System.out.println("Error al iniciar el juego: " + e.getMessage());
     }
    }

    private void enviarInstrucciones() throws IOException{
        try{
            String instrucciones = "Para jugar usa /mover fila columna (ejemplo: /jugar 1 2)";
            cliente1.salida.writeUTF(instrucciones);
            cliente2.salida.writeUTF(instrucciones);
        }catch (IOException e){
        System.out.println("Error enviando instrucciones " + e.getMessage());
    }
    }



    public boolean esTurnoJugador(String clienteId){
        return turnoActualId.equals(clienteId) && !juegoTerminado;
    }

    public String procesarMovimiento(String clienteId, int fila, int columna) throws IOException {
        if(juegoTerminado){
            return "El juego ya ha terminado";
        }
        if(!esTurnoJugador(clienteId)){
            return "No es tu turno";
        }

        if(fila<0 || fila >2 || columna <0 || columna>2){
            return "Posicion invalida. Usa numeros del 1 al 3";
        }
        if(tablero[fila][columna] != '-'){
            return "Posicion ocupada";
        }

        char simbolo = clienteId.equals(jugador1Id) ? simboloJugador1 : simboloJugador2;
        tablero[fila][columna] = simbolo;

        enviarTablero();

        if(verificarGanador(simbolo)){
            juegoTerminado = true;
            ganador = clienteId.equals(jugador1Id) ? jugador1Nombre : jugador2Nombre;
            String perdedor = clienteId.equals(jugador1Id) ? jugador2Nombre : jugador1Nombre;
            
            // Registrar estadísticas en la base de datos
            String ganadorClienteId = clienteId.equals(jugador1Id) ? jugador1Id : jugador2Id;
            String perdedorClienteId = clienteId.equals(jugador1Id) ? jugador2Id : jugador1Id;

            String nombreGanadorReal = SistemaAutenticacion.getNombreUsuarioReal(ganadorClienteId);
            String nombrePerdedorReal = SistemaAutenticacion.getNombreUsuarioReal(perdedorClienteId);
            if(nombreGanadorReal != null && nombrePerdedorReal != null){
                System.out.println("Registrando victoria: " + nombreGanadorReal + " gana contra " + nombrePerdedorReal);
                Database.registrarVictoria(nombreGanadorReal, nombrePerdedorReal);
            } else {
                System.out.println("No se registraron estadisticas (uno o ambos jugadores son invitados)");
            }
            
            notificarFinJuego(ganador + " ha ganado el juego. Yay :D");
            return "Ganaste el juego";
        }

        if(verificarEmpate()){
            juegoTerminado = true;
            
            // Registrar empate en la base de datos
            String nombreJ1Real = SistemaAutenticacion.getNombreUsuarioReal(jugador1Nombre);
            String nombreJ2Real = SistemaAutenticacion.getNombreUsuarioReal(jugador2Nombre);
            if(nombreJ1Real != null && nombreJ2Real != null){
                System.out.println("Registrando empate entre: " + nombreJ1Real + " y " + nombreJ2Real);
                Database.registrarEmpate(nombreJ1Real, nombreJ2Real);
            } else {
                System.out.println("No se registraron estadisticas (uno o ambos jugadores son invitados)");
            }
            
            notificarFinJuego ("Juego terminado en empate");
            return "Empate";
        }

        cambiarTurno();
        notificarTurno();

        return "Movimiento realizado";
    }

    private void cambiarTurno(){
        if(turnoActualId.equals(jugador1Id)){
            turnoActualId = jugador2Id;
            turnoActualNombre = jugador2Nombre;

        }else{
            turnoActualId = jugador1Id;
            turnoActualNombre = jugador1Nombre;
        }
    }

    private void notificarTurno() throws IOException{
        try{
            String mensaje = "Turno de " + turnoActualNombre;
            cliente1.salida.writeUTF(mensaje);
            cliente2.salida.writeUTF(mensaje);
        }catch (IOException e){
            System.out.println("Error notificando turno " + e.getMessage());
        }
    }

    private void enviarTablero() throws IOException{
        try{
            String tableroStr = generarTableroTexto();
            cliente1.salida.writeUTF(tableroStr);
            cliente2.salida.writeUTF(tableroStr);
        } catch (IOException e) {
            System.err.println("Error enviando tablero: " + e.getMessage());
        }
    }
    private String generarTableroTexto(){
        StringBuilder sb = new StringBuilder("\n   0   1   2\n");
        for(int i=0; i<3; i++){
            sb.append(i).append(" ");
            for(int j=0; j<3; j++){
                sb.append(" ").append(tablero[i][j]).append(" ");
                if(j < 2) sb.append("|");
            }
            sb.append("\n");
            if(i<2) sb.append("  ---+---+---\n");
        }
        return sb.toString();
    }

    private boolean verificarGanador(char simbolo){
        for(int i=0; i<3; i++){
            if(tablero[i][0] == simbolo && tablero[i][1] == simbolo && tablero[i][2] == simbolo){
                return true;
            }
        }

        for (int j=0; j<3; j++){
            if(tablero[0][j] == simbolo && tablero[1][j] == simbolo && tablero[2][j] == simbolo){
                return true;
            }
        }
        if(tablero[0][0] == simbolo && tablero[1][1] == simbolo && tablero[2][2] == simbolo){
            return true;
        }

        if(tablero[0][2] == simbolo && tablero[1][1] == simbolo && tablero[2][0] == simbolo){
            return true;
        }

        return false;
    }
    private boolean verificarEmpate(){
        for(int i=0; i<3; i++){
            for(int j=0; j<3; j++){
                if(tablero[i][j] == '-'){
                    return false;
                }
            }
        }
        return true;
    }

    private void notificarFinJuego(String mensaje) throws IOException{
        try{
            cliente1.salida.writeUTF("\n*** FIN DE JUEGO ***\n");
            cliente1.salida.writeUTF(mensaje);
            cliente2.salida.writeUTF("\n*** FIN DE JUEGO ***\n");
            cliente2.salida.writeUTF(mensaje);
        }catch (IOException e){
            System.out.println("Error notificando fin de juego " + e.getMessage());
        }

    }

    public void notificarAbandonoPorDesconexion(String clienteIdDesconectado){
        juegoTerminado = true;
        String nombreDesconectado = clienteIdDesconectado.equals(jugador1Id) ? jugador1Nombre : jugador2Nombre;
        String nombreGanador = clienteIdDesconectado.equals(jugador1Id) ? jugador2Nombre : jugador1Nombre;
        UnCliente clienteGanador = clienteIdDesconectado.equals(jugador1Id) ? cliente2 : cliente1;

        try{
            clienteGanador.salida.writeUTF("\n*** FIN DE JUEGO ***\n");
            clienteGanador.salida.writeUTF(nombreDesconectado + " se desconecto. Ganaste por abandono");
        }catch (IOException e){
            System.out.println("Error notificando abandono por desconexion " + e.getMessage());
        }
    }
    public boolean involucraJugador(String clienteId){
        return clienteId.equals(jugador1Id) || clienteId.equals(jugador2Id);
    }

    public boolean juegoTerminado(){
        return juegoTerminado;
    }
    public String getJugador1Id(){
    return jugador1Id;
    }

    public String getJugador2Id(){
        return jugador2Id;
    }

}
