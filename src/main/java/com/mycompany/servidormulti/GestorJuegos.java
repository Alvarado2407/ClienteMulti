package com.mycompany.servidormulti;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GestorJuegos {
    // Mapa de juegos activos por ID de cliente
    private static HashMap<String, JuegoGato> juegosActivos = new HashMap<>();
    
    // Invitaciones pendientes: clave = destinatario, valor = remitente
    private static HashMap<String, String> invitacionesPendientes = new HashMap<>();
    
    public static void enviarInvitacion(String remitenteId, String destinatarioId, UnCliente remitente, UnCliente destinatario) throws IOException {
        // Verificar si ya existe una invitación pendiente
        if(invitacionesPendientes.containsKey(destinatarioId)){
            remitente.salida.writeUTF("ERROR: " + SistemaAutenticacion.getNombreDisplay(destinatarioId) + " ya tiene una invitación pendiente");
            return;
        }
        
        // Verificar si ya están jugando juntos
        if(yaEstanJugando(remitenteId, destinatarioId)){
            remitente.salida.writeUTF("ERROR: Ya tienes un juego activo con " + SistemaAutenticacion.getNombreDisplay(destinatarioId));
            return;
        }
        
        // Enviar invitación
        invitacionesPendientes.put(destinatarioId, remitenteId);
        String nombreRemitente = SistemaAutenticacion.getNombreDisplay(remitenteId);
        destinatario.salida.writeUTF("\n*** INVITACION DE JUEGO ***");
        destinatario.salida.writeUTF(nombreRemitente + " te ha invitado a jugar al gato!");
        destinatario.salida.writeUTF("Usa /aceptar para aceptar o /rechazar para rechazar");
        
        remitente.salida.writeUTF("Invitacion enviada a " + SistemaAutenticacion.getNombreDisplay(destinatarioId));
    }
    
    public static void aceptarInvitacion(String clienteId, UnCliente cliente) throws IOException {
        if(!invitacionesPendientes.containsKey(clienteId)){
            cliente.salida.writeUTF("No tienes invitaciones pendientes");
            return;
        }
        
        String remitenteId = invitacionesPendientes.remove(clienteId);
        UnCliente remitente = ServidorMulti.clientes.get(remitenteId);
        
        if(remitente == null){
            cliente.salida.writeUTF("El usuario que te invito ya no esta conectado");
            return;
        }
        
        // Crear el juego
        String nombreRemitente = SistemaAutenticacion.getNombreDisplay(remitenteId);
        String nombreDestinatario = SistemaAutenticacion.getNombreDisplay(clienteId);
        
        JuegoGato juego = new JuegoGato(remitenteId, nombreRemitente, clienteId, nombreDestinatario, remitente, cliente);
        
        // Registrar el juego para ambos jugadores
        juegosActivos.put(remitenteId, juego);
        juegosActivos.put(clienteId, juego);
        
        // Iniciar el juego
        juego.iniciarJuego();
    }
    
    public static void rechazarInvitacion(String clienteId, UnCliente cliente) throws IOException {
        if(!invitacionesPendientes.containsKey(clienteId)){
            cliente.salida.writeUTF("No tienes invitaciones pendientes");
            return;
        }
        
        String remitenteId = invitacionesPendientes.remove(clienteId);
        UnCliente remitente = ServidorMulti.clientes.get(remitenteId);
        
        String nombreDestinatario = SistemaAutenticacion.getNombreDisplay(clienteId);
        
        if(remitente != null){
            remitente.salida.writeUTF(nombreDestinatario + " ha rechazado tu invitacion de juego");
        }
        
        cliente.salida.writeUTF("Has rechazado la invitacion");
    }
    
    public static boolean yaEstanJugando(String cliente1Id, String cliente2Id){
        JuegoGato juego1 = juegosActivos.get(cliente1Id);
        if(juego1 == null) return false;
        
        return juego1.involucraJugador(cliente2Id);
    }
    
    public static JuegoGato obtenerJuego(String clienteId){
        return juegosActivos.get(clienteId);
    }
    
    public static void procesarMovimiento(String clienteId, int fila, int columna, UnCliente cliente) throws IOException {
        JuegoGato juego = juegosActivos.get(clienteId);
        
        if(juego == null){
            cliente.salida.writeUTF("No estas en ningun juego activo");
            return;
        }
        
        String resultado = juego.procesarMovimiento(clienteId, fila, columna);
        
        // Si el juego terminó, limpiar
        if(juego.juegoTerminado()){
            juegosActivos.remove(juego.getJugador1Id());
            juegosActivos.remove(juego.getJugador2Id());
        }
    }
    
    public static void manejarDesconexion(String clienteId){
        JuegoGato juego = juegosActivos.get(clienteId);
        
        if(juego != null){
            // Notificar al otro jugador
            juego.notificarAbandonoPorDesconexion(clienteId);
            
            // Limpiar el juego
            juegosActivos.remove(juego.getJugador1Id());
            juegosActivos.remove(juego.getJugador2Id());
        }
        
        // Limpiar invitaciones pendientes
        invitacionesPendientes.remove(clienteId);
        
        // Si este cliente había enviado una invitación a alguien, eliminarla
        List<String> aEliminar = new ArrayList<>();
        for(String destinatario : invitacionesPendientes.keySet()){
            if(invitacionesPendientes.get(destinatario).equals(clienteId)){
                aEliminar.add(destinatario);
            }
        }
        for(String dest : aEliminar){
            invitacionesPendientes.remove(dest);
        }
    }
}
