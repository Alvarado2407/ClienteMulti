package com.mycompany.servidormulti;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GestorGrupos {
    // Grupo actual de cada cliente (clienteId -> nombreGrupo)
    private static Map<String, String> clientesEnGrupos = new ConcurrentHashMap<>();

    // Último mensaje visto por cada usuario en cada grupo (username -> grupoNombre -> timestamp)
    private static Map<String, Map<String, Timestamp>> ultimosMensajesVistos = new ConcurrentHashMap<>();

    public static void inicializar(){
        // El grupo "Todos" siempre existe
        Database.crearGrupoSiNoExiste("Todos");
    }

    public static void unirseAGrupo(String clienteId, String nombreGrupo, ManejadorSesion cliente) throws IOException {
        String username = SistemaAutenticacion.getNombreUsuarioReal(clienteId);

        // Verificar si es invitado y intenta unirse a un grupo que no es "Todos"
        if(username == null && !nombreGrupo.equals("Todos")){
            cliente.getWriter().writeUTF("Los usuarios invitados solo pueden estar en el grupo 'Todos'");
            return;
        }

        // Verificar que el grupo existe
        if(!Database.existeGrupo(nombreGrupo)){
            cliente.getWriter().writeUTF("El grupo '" + nombreGrupo + "' no existe");
            return;
        }

        // Salir del grupo actual si hay uno
        String grupoActual = clientesEnGrupos.get(clienteId);
        if(grupoActual != null){
            notificarGrupo(grupoActual, "*** " + SistemaAutenticacion.getNombreDisplay(clienteId) + " ha salido del grupo ***", cliente);
        }

        // Unirse al nuevo grupo
        clientesEnGrupos.put(clienteId, nombreGrupo);

        if(username != null){
            Database.agregarMiembroAGrupo(nombreGrupo, username);
        }

        notificarGrupo(nombreGrupo, "*** " + SistemaAutenticacion.getNombreDisplay(clienteId) + " se ha unido al grupo ***", cliente);
        cliente.getWriter().writeUTF("Te has unido al grupo: " + nombreGrupo);

        // Enviar mensajes no leídos
        enviarMensajesNoLeidos(clienteId, nombreGrupo, cliente);
    }

    public static void crearGrupo(String clienteId, String nombreGrupo, ManejadorSesion cliente) throws IOException {
        String username = SistemaAutenticacion.getNombreUsuarioReal(clienteId);

        if(username == null){
            cliente.getWriter().writeUTF("Los usuarios invitados no pueden crear grupos");
            return;
        }

        if(nombreGrupo.equals("Todos")){
            cliente.getWriter().writeUTF("El grupo 'Todos' ya existe y no se puede recrear");
            return;
        }

        if(Database.existeGrupo(nombreGrupo)){
            cliente.getWriter().writeUTF("El grupo '" + nombreGrupo + "' ya existe");
            return;
        }

        if(Database.crearGrupo(nombreGrupo, username)){
            cliente.getWriter().writeUTF("Grupo '" + nombreGrupo + "' creado exitosamente");
            System.out.println("Grupo '" + nombreGrupo + "' creado por " + username);
        } else {
            cliente.getWriter().writeUTF("Error al crear el grupo");
        }
    }

    public static void eliminarGrupo(String clienteId, String nombreGrupo, ManejadorSesion cliente) throws IOException {
        String username = SistemaAutenticacion.getNombreUsuarioReal(clienteId);

        if(username == null){
            cliente.getWriter().writeUTF("Los usuarios invitados no pueden eliminar grupos");
            return;
        }

        if(nombreGrupo.equals("Todos")){
            cliente.getWriter().writeUTF("El grupo 'Todos' no se puede eliminar");
            return;
        }

        if(!Database.existeGrupo(nombreGrupo)){
            cliente.getWriter().writeUTF("El grupo '" + nombreGrupo + "' no existe");
            return;
        }

        // Sacar a todos los clientes del grupo
        List<String> clientesAMover = new ArrayList<>();
        for(Map.Entry<String, String> entry : clientesEnGrupos.entrySet()){
            if(entry.getValue().equals(nombreGrupo)){
                clientesAMover.add(entry.getKey());
            }
        }

        for(String cId : clientesAMover){
            clientesEnGrupos.put(cId, "Todos");
            ManejadorSesion c = ServidorMulti.clientes.get(cId);
            if(c != null){
                c.getWriter().writeUTF("El grupo '" + nombreGrupo + "' ha sido eliminado. Has sido movido a 'Todos'");
            }
        }

        if(Database.eliminarGrupo(nombreGrupo)){
            cliente.getWriter().writeUTF("Grupo '" + nombreGrupo + "' eliminado exitosamente");
            System.out.println("Grupo '" + nombreGrupo + "' eliminado por " + username);
        } else {
            cliente.getWriter().writeUTF("Error al eliminar el grupo");
        }
    }

    public static void listarGrupos(ManejadorSesion cliente) throws IOException {
        List<String> grupos = Database.obtenerListaGrupos();
        cliente.getWriter().writeUTF("\n=== GRUPOS DISPONIBLES ===");
        for(String grupo : grupos){
            cliente.getWriter().writeUTF("- " + grupo);
        }
    }

    public static void enviarMensajeGrupo(String clienteId, String mensaje, ManejadorSesion cliente) throws IOException {
        String grupoActual = clientesEnGrupos.getOrDefault(clienteId, "Todos");
        String nombreRemitente = SistemaAutenticacion.getNombreDisplay(clienteId);
        String username = SistemaAutenticacion.getNombreUsuarioReal(clienteId);

        // Guardar mensaje en la base de datos
        if(username != null){
            Database.guardarMensaje(grupoActual, username, mensaje);
        }

        String mensajeFormateado = "[" + grupoActual + "] " + nombreRemitente + ": " + mensaje;

        // Enviar a todos los miembros del grupo
        for(Map.Entry<String, String> entry : clientesEnGrupos.entrySet()){
            if(entry.getValue().equals(grupoActual)){
                String destinoId = entry.getKey();
                if(!destinoId.equals(clienteId)){
                    ManejadorSesion destino = ServidorMulti.clientes.get(destinoId);
                    if(destino != null){
                        try{
                            destino.getWriter().writeUTF(mensajeFormateado);
                        }catch(IOException e){}
                    }
                }
            }
        }

        // Actualizar último mensaje visto para el remitente
        if(username != null){
            actualizarUltimoMensajeVisto(username, grupoActual);
        }
    }

    private static void enviarMensajesNoLeidos(String clienteId, String nombreGrupo, ManejadorSesion cliente) throws IOException {
        String username = SistemaAutenticacion.getNombreUsuarioReal(clienteId);

        if(username == null){
            // Los invitados no tienen historial
            return;
        }

        Timestamp ultimoVisto = obtenerUltimoMensajeVisto(username, nombreGrupo);
        List<String> mensajesNoLeidos = Database.obtenerMensajesNoLeidos(nombreGrupo, ultimoVisto);

        if(!mensajesNoLeidos.isEmpty()){
            cliente.getWriter().writeUTF("\n=== MENSAJES NO LEIDOS EN " + nombreGrupo + " ===");
            for(String msg : mensajesNoLeidos){
                cliente.getWriter().writeUTF(msg);
            }
            cliente.getWriter().writeUTF("=== FIN DE MENSAJES NO LEIDOS ===\n");
        }

        actualizarUltimoMensajeVisto(username, nombreGrupo);
    }

    private static void notificarGrupo(String nombreGrupo, String mensaje, ManejadorSesion excluir){
        for(Map.Entry<String, String> entry : clientesEnGrupos.entrySet()){
            if(entry.getValue().equals(nombreGrupo)){
                ManejadorSesion cliente = ServidorMulti.clientes.get(entry.getKey());
                if(cliente != null && cliente != excluir){
                    try{
                        cliente.getWriter().writeUTF(mensaje);
                    }catch(IOException e){}
                }
            }
        }
    }

    public static void clienteDesconectado(String clienteId){
        String grupoActual = clientesEnGrupos.get(clienteId);
        if(grupoActual != null){
            String nombre = SistemaAutenticacion.getNombreDisplay(clienteId);
            notificarGrupo(grupoActual, "*** " + nombre + " se ha desconectado ***", null);
            clientesEnGrupos.remove(clienteId);
        }
    }

    public static void clienteConectado(String clienteId){
        // Todos los usuarios empiezan en el grupo "Todos"
        clientesEnGrupos.put(clienteId, "Todos");
    }

    public static String getGrupoActual(String clienteId){
        return clientesEnGrupos.getOrDefault(clienteId, "Todos");
    }

    private static Timestamp obtenerUltimoMensajeVisto(String username, String grupo){
        return ultimosMensajesVistos
                .computeIfAbsent(username, k -> new HashMap<>())
                .getOrDefault(grupo, new Timestamp(0));
    }

    private static void actualizarUltimoMensajeVisto(String username, String grupo){
        ultimosMensajesVistos
                .computeIfAbsent(username, k -> new HashMap<>())
                .put(grupo, new Timestamp(System.currentTimeMillis()));
    }
}