
package com.mycompany.servidormulti;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.List;


public class UnCliente implements Runnable {

    final DataOutputStream salida;

    final BufferedReader teclado = new BufferedReader (new InputStreamReader(System.in));

    final DataInputStream entrada;

    private final String clienteUsuario;

    public UnCliente(Socket s, String clienteUsuario)throws IOException {
        salida = new DataOutputStream(s.getOutputStream());
        entrada = new DataInputStream(s.getInputStream());
        this.clienteUsuario = clienteUsuario;
    }

    public String getClienteUsuario() {
        return clienteUsuario;
    }

    @Override

    public void run(){
        String mensaje;
        try {

            salida.writeUTF("¡Bienvenido! Eres " + clienteUsuario);
            salida.writeUTF("Tienes 3 mensajes gratuitos como invitado, inicia sesion y envia mensajes ilimitados");
            salida.writeUTF("Comandos disponibles:");
            salida.writeUTF("- Escribe un mensaje normal para enviarlo a todos");
            salida.writeUTF("- Usa @NombreUsuario mensaje para enviar mensaje privado");
            salida.writeUTF("- Usa /usuarios para ver usuarios conectados");
            salida.writeUTF("- Usa /ranking para ver el ranking de jugadores");
            salida.writeUTF("- Usa /vs jugador1 jugador2 para ver estadisticas entre dos jugadores");
            salida.writeUTF("- Usa /registro usuario contrase;a para registrarte");
            salida.writeUTF("- Usa /login usuario contrase;a para iniciar sesion");
            salida.writeUTF("- Usa /jugar NombreUsuario para invitar a jugar al gato");
            salida.writeUTF("- Usa /aceptar para aceptar una invitacion");
            salida.writeUTF("- Usa /rechazar para rechazar una invitacion");
            salida.writeUTF("- Durante el juego usa /mover fila columna (ejemplo: /mover 0 1)");

        } catch (IOException e) {
            System.out.println("Error enviando mensaje de bienvenida a " + clienteUsuario);
        }
        while(true){
            try {
                mensaje = entrada.readUTF();

                // Comando de registro
                if(mensaje.startsWith("/registro")) {
                    String[] partes = mensaje.split(" ");
                    if (partes.length == 3) {
                        String resultado = SistemaAutenticacion.procesarRegistro(clienteUsuario, partes[1], partes[2]);
                        salida.writeUTF(resultado);
                        if (resultado.startsWith("EXITO")) {
                            ServidorMulti.notificarTodos("*** " + clienteUsuario + " ahora es " + partes[1] + "  ***", this);
                        }
                    } else {
                        salida.writeUTF("El formato es /registro usuario contrase;a");
                    }
                    continue;
                }
                
                // Comando de login
                if(mensaje.startsWith("/login ")){
                    String[] parteslog = mensaje.split(" ");
                    if(parteslog.length == 3){
                        String resultado = SistemaAutenticacion.procesarLogin(clienteUsuario, parteslog[1], parteslog[2]);
                        salida.writeUTF(resultado);
                        if(resultado.startsWith("EXITO")){
                            ServidorMulti.notificarTodos("*** " + clienteUsuario + " ahora es " + parteslog[1] + "  ***",this);
                        }
                    }else{
                        salida.writeUTF("El formato es /login usuario contrase;a");
                    }
                    continue;
                }

                // Comando para ver usuarios conectados
                if(mensaje.equals("/usuarios")) {
                    StringBuilder usuarios = new StringBuilder("Usuarios conectados: ");
                    for (String user : ServidorMulti.clientes.keySet()) {
                        usuarios.append(SistemaAutenticacion.getNombreDisplay(user)).append(", ");
                    }
                    salida.writeUTF(usuarios.toString());
                    continue;
                }

                if(mensaje.equals("/ranking")){
                    List<String> ranking = Database.obtenerRanking();
                    for(String linea : ranking){
                        salida.writeUTF(linea);
                    }
                    continue;
                }

                if(mensaje.startsWith("/vs ")){
                    String[] partes = mensaje.split(" ");
                    if(partes.length != 3){
                        salida.writeUTF("Uso: /vs jugador1 jugador2");
                        continue;
                    }
                    String estadisticas = Database.obtenerEstadisticasVs(partes[1], partes[2]);
                    salida.writeUTF(estadisticas);
                    continue;
                }
                
                // Comando para invitar a jugar
                if(mensaje.startsWith("/jugar ")){
                    String[] partes = mensaje.split(" ", 2);
                    if(partes.length < 2){
                        salida.writeUTF("Uso: /jugar NombreUsuario");
                        continue;
                    }
                    
                    
                    String nombreDestino = partes[1].trim();
                    UnCliente destinatario = buscarClientePorNombre(nombreDestino);
                    
                    if(destinatario == null){
                        salida.writeUTF("Usuario no encontrado: " + nombreDestino);
                    } else if(destinatario.clienteUsuario.equals(clienteUsuario)){
                        salida.writeUTF("No puedes jugar contigo mismo");
                    } else {
                        GestorJuegos.enviarInvitacion(clienteUsuario, destinatario.clienteUsuario, this, destinatario);
                    }
                    continue;
                }
                
                // Comando para aceptar invitación
                if(mensaje.equals("/aceptar")){
                    GestorJuegos.aceptarInvitacion(clienteUsuario, this);
                    continue;
                }
                
                // Comando para rechazar invitación
                if(mensaje.equals("/rechazar")){
                    GestorJuegos.rechazarInvitacion(clienteUsuario, this);
                    continue;
                }
                
                // Comando para hacer un movimiento en el juego
                if(mensaje.startsWith("/mover ")){
                    String[] partes = mensaje.split(" ");
                    if(partes.length != 3){
                        salida.writeUTF("Uso: /mover fila columna (ejemplo: /mover 0 1)");
                        continue;
                    }
                    
                    try{
                        int fila = Integer.parseInt(partes[1]);
                        int columna = Integer.parseInt(partes[2]);
                        GestorJuegos.procesarMovimiento(clienteUsuario, fila, columna, this);
                    } catch(NumberFormatException e){
                        salida.writeUTF("Fila y columna deben ser numeros del 0 al 2");
                    }
                    continue;
                }
                
                // Verificar límite de mensajes para invitados
                if(!SistemaAutenticacion.puedeEnviarMensajes(clienteUsuario)){
                    salida.writeUTF("Has alcanzado el limite de 3 mensajes gratuitos");
                    salida.writeUTF("Registrate (/registro) o inicia sesion (/login) para enviar mensajes ilimitados");
                    salida.writeUTF("Puedes seguir viendo los mensajes de los demas usuarios");
                    continue;
                }
                
                // Mensaje privado
                if(mensaje.startsWith("@")){
                    String[] partes = mensaje.split(" ",2);
                    if(partes.length<2){
                        salida.writeUTF("Formato incorrecto, usa @NombreUsuario mensaje para enviar mensaje privado");
                        continue;
                    }
                    String aQuien = partes[0].substring(1);
                    String mensajePrivado = partes[1];
                    UnCliente clienteDestino = buscarClientePorNombre(aQuien);

                    if(clienteDestino!=null){
                        String nombreRemitente = SistemaAutenticacion.getNombreDisplay(clienteUsuario);
                        String mensajeFormateado = "[PRIVADO] " + nombreRemitente + " te dice: " + mensajePrivado;
                        clienteDestino.salida.writeUTF(mensajeFormateado);

                        salida.writeUTF("Mensaje privado enviado a " + SistemaAutenticacion.getNombreDisplay(clienteDestino.clienteUsuario));
                        int restantes = SistemaAutenticacion.incrementarMensajes(clienteUsuario);
                        if(restantes>=0){
                            salida.writeUTF("Te quedan " + restantes + " mensajes gratuitos");
                        }
                    }else{
                        salida.writeUTF("No se pudo enviar un mensaje privado");
                    }
                    continue;
                }
                
                // Mensaje público
                String nombreRemitente = SistemaAutenticacion.getNombreDisplay(clienteUsuario);
                String mensajeConRemitente = nombreRemitente + ": " + mensaje;
                for(UnCliente cliente : ServidorMulti.clientes.values()){
                    if (!clienteUsuario.equals(cliente.getClienteUsuario())) {
                        cliente.salida.writeUTF(mensajeConRemitente);
                    }
                }

                int restantes = SistemaAutenticacion.incrementarMensajes(clienteUsuario);
                if(restantes>=0){
                    salida.writeUTF("Te quedan " + restantes + " mensajes gratuitos");
                }

            } catch (IOException e) {
                System.out.println("cliente " + SistemaAutenticacion.getNombreDisplay(clienteUsuario) + " se desconecto");
                
                // Manejar desconexión en juegos activos
                GestorJuegos.manejarDesconexion(clienteUsuario);
                
                SistemaAutenticacion.limpiarCliente(clienteUsuario);
                ServidorMulti.notificarTodos("*** " + SistemaAutenticacion.getNombreDisplay(clienteUsuario) + " se ha desconectado ***",this);
                ServidorMulti.clientes.remove(clienteUsuario);
                break;
            }

        }
    }

    private UnCliente buscarClientePorNombre(String nombre){
        UnCliente cliente = ServidorMulti.clientes.get(nombre);
        if(cliente!=null){
            return cliente;
        }
        for(UnCliente c : ServidorMulti.clientes.values()){
            if(SistemaAutenticacion.getNombreDisplay(c.clienteUsuario).equals(nombre)){
                return c;
            }
        }
        return null;
    }
}
