package com.mycompany.servidormulti;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;

/**
 * Clase encargada de gestionar la sesión individual de un usuario conectado.
 * Procesa las peticiones entrantes y distribuye la lógica.
 */
public class ManejadorSesion implements Runnable {

    private final DataOutputStream writer;
    private final DataInputStream reader;
    private final String idSesion;
    private boolean activo = true;

    // Constructor renombrado y simplificado
    public ManejadorSesion(Socket socket, String idInicial) throws IOException {
        this.writer = new DataOutputStream(socket.getOutputStream());
        this.reader = new DataInputStream(socket.getInputStream());
        this.idSesion = idInicial;
    }

    public String obtenerIdUsuario() {
        return idSesion;
    }
    
    public DataOutputStream getWriter() {
    return writer;
    }

    @Override
    public void run() {
        try {
            enviarMenuBienvenida();

            while (activo) {
                String input = reader.readUTF();
                // Separamos el comando de los argumentos (ej: "/login user pass" -> ["/login", "user", "pass"])
                String[] tokens = input.trim().split("\\s+");
                String comando = tokens[0];

                // Estructura Switch para limpieza visual y lógica
                switch (comando) {
                    // --- AUTENTICACIÓN ---
                    case "/registro":
                        procesarRegistro(tokens);
                        break;
                    case "/login":
                        procesarLogin(tokens);
                        break;
                    case "/logout":
                        procesarDesconexionVoluntaria();
                        break;
                    case "/eliminarcuenta":
                        procesarBajaCuenta(tokens);
                        break;

                    // --- INFORMACIÓN Y SOCIAL ---
                    case "/usuarios":
                        mostrarUsuariosConectados();
                        break;
                    case "/ranking":
                        mostrarRanking();
                        break;
                    case "/bloquear":
                        gestionarBloqueo(tokens, true);
                        break;
                    case "/desbloquear":
                        gestionarBloqueo(tokens, false);
                        break;
                    case "/bloqueados":
                        listarBloqueados();
                        break;
                    
                    // --- GRUPOS ---
                    case "/creargrupo":
                    case "/unirsegrupo":
                    case "/eliminargrupo":
                    case "/grupos":
                        gestionarComandosGrupo(comando, tokens);
                        break;

                    // --- JUEGOS ---
                    case "/vs":
                        mostrarStatsVersus(tokens);
                        break;
                    case "/jugar":
                    case "/aceptar":
                    case "/rechazar":
                    case "/mover":
                        gestionarLogicaJuego(comando, tokens);
                        break;

                    // --- MENSAJERÍA ---
                    default:
                        gestionarMensajeria(input, comando);
                        break;
                }
            }
        } catch (IOException e) {
            manejarCierreInesperado(e);
        }
    }

    // --- MÉTODOS AUXILIARES (Refactorización) ---

    private void enviarMenuBienvenida() throws IOException {
        writer.writeUTF("--- CONECTADO AL SERVIDOR ---");
        writer.writeUTF("Usuario actual: " + idSesion);
        writer.writeUTF("Estado: Invitado (Límite: 3 mensajes). Inicia sesión para liberar tu cuenta.");
        writer.writeUTF("Ayuda rápida:");
        writer.writeUTF(" > General: Escribe cualquier texto.");
        writer.writeUTF(" > Privado: @Usuario mensaje");
        writer.writeUTF(" > Cuenta: /registro, /login, /logout, /eliminarcuenta");
        writer.writeUTF(" > Info: /usuarios, /ranking, /vs [p1] [p2]");
        writer.writeUTF(" > Social: /bloquear, /desbloquear, /bloqueados");
        writer.writeUTF(" > Grupos: /creargrupo, /unirsegrupo, /grupos");
        writer.writeUTF(" > Juego: /jugar [user], /aceptar, /rechazar, /mover [x] [y]");
    }

    private void procesarRegistro(String[] args) throws IOException {
        if (args.length != 3) {
            writer.writeUTF("Error de sintaxis: Usa /registro [usuario] [password]");
            return;
        }
        String resp = SistemaAutenticacion.procesarRegistro(idSesion, args[1], args[2]);
        writer.writeUTF(resp);
        if (resp.startsWith("EXITO")) {
            ServidorMulti.notificarTodos(">>> Nuevo usuario registrado: " + args[1], this);
        }
    }

    private void procesarLogin(String[] args) throws IOException {
        if (args.length != 3) {
            writer.writeUTF("Error de sintaxis: Usa /login [usuario] [password]");
            return;
        }
        String resp = SistemaAutenticacion.procesarLogin(idSesion, args[1], args[2]);
        writer.writeUTF(resp);
        if (resp.startsWith("EXITO")) {
            ServidorMulti.notificarTodos(">>> " + idSesion + " se ha identificado como " + args[1], this);
        }
    }

    private void procesarDesconexionVoluntaria() throws IOException {
        String nickReal = SistemaAutenticacion.getNombreUsuarioReal(idSesion);
        String resp = SistemaAutenticacion.procesarLogout(idSesion);
        writer.writeUTF(resp);
        if (resp.startsWith("EXITO")) {
            ServidorMulti.notificarTodos("<<< " + nickReal + " cerró sesión.", this);
        }
    }

    private void procesarBajaCuenta(String[] args) throws IOException {
        if (args.length < 2 || !args[1].equals("CONFIRMAR")) {
            writer.writeUTF("PELIGRO: Esto borrará tu cuenta. Confirma con: /eliminarcuenta CONFIRMAR");
            return;
        }
        String nickViejo = SistemaAutenticacion.getNombreUsuarioReal(idSesion);
        String res = SistemaAutenticacion.procesarEliminarCuenta(idSesion);
        writer.writeUTF(res);
        if (res.startsWith("EXITO")) {
            writer.writeUTF("Has vuelto a ser invitado: " + idSesion);
            ServidorMulti.notificarTodos("User " + nickViejo + " ha eliminado su cuenta.", this);
        }
    }

    private void mostrarUsuariosConectados() throws IOException {
        StringBuilder sb = new StringBuilder("En línea: ");
        for (String u : ServidorMulti.clientes.keySet()) {
            sb.append(SistemaAutenticacion.getNombreDisplay(u)).append(" | ");
        }
        writer.writeUTF(sb.toString());
    }

    private void mostrarRanking() throws IOException {
        List<String> rank = Database.obtenerRanking();
        rank.forEach(linea -> {
            try { writer.writeUTF(linea); } catch (IOException e) {}
        });
    }

    private void mostrarStatsVersus(String[] args) throws IOException {
        if (args.length != 3) {
            writer.writeUTF("Sintaxis: /vs [jugador1] [jugador2]");
        } else {
            writer.writeUTF(Database.obtenerEstadisticasVs(args[1], args[2]));
        }
    }

    private void gestionarBloqueo(String[] args, boolean esBloquear) throws IOException {
        if (args.length < 2) {
            writer.writeUTF("Indica el nombre del usuario.");
            return;
        }
        if (!SistemaAutenticacion.estaAutenticado(idSesion)) {
            writer.writeUTF("Acción denegada: Requiere inicio de sesión.");
            return;
        }
        
        String objetivo = args[1].trim();
        String miNick = SistemaAutenticacion.getNombreUsuarioReal(idSesion);

        if (esBloquear) {
            // Lógica de bloqueo
            if (objetivo.equals(miNick)) {
                writer.writeUTF("No puedes auto-bloquearte.");
                return;
            }
            if (!Database.existeUsuario(objetivo)) {
                writer.writeUTF("Usuario inexistente.");
                return;
            }
            if (Database.yoBloquee(miNick, objetivo)) {
                writer.writeUTF("Ya estaba bloqueado.");
            } else if (Database.bloquearUsuario(miNick, objetivo)) {
                writer.writeUTF("Usuario " + objetivo + " bloqueado.");
            } else {
                writer.writeUTF("Error al bloquear.");
            }
        } else {
            // Lógica de desbloqueo
            if (Database.desbloquearUsuario(miNick, objetivo)) {
                writer.writeUTF("Desbloqueaste a " + objetivo);
            } else {
                writer.writeUTF("No tenías bloqueado a ese usuario o hubo un error.");
            }
        }
    }

    private void listarBloqueados() throws IOException {
        if (!SistemaAutenticacion.estaAutenticado(idSesion)) {
            writer.writeUTF("Inicia sesión primero.");
            return;
        }
        List<String> lista = Database.obtenerBloqueados(SistemaAutenticacion.getNombreUsuarioReal(idSesion));
        if (lista.isEmpty()) writer.writeUTF("Lista de bloqueos vacía.");
        else {
            writer.writeUTF("--- TUS BLOQUEOS ---");
            for (String s : lista) writer.writeUTF("X " + s);
        }
    }

    private void gestionarComandosGrupo(String cmd, String[] args) throws IOException {
        // Redirección simple al gestor
        String param = args.length > 1 ? args[1].trim() : "";
        
        switch (cmd) {
            case "/grupos":
                GestorGrupos.listarGrupos(this);
                break;
            case "/creargrupo":
                if(param.isEmpty()) writer.writeUTF("Falta nombre del grupo.");
                else GestorGrupos.crearGrupo(idSesion, param, this);
                break;
            case "/unirsegrupo":
                if(param.isEmpty()) writer.writeUTF("Falta nombre del grupo.");
                else GestorGrupos.unirseAGrupo(idSesion, param, this);
                break;
            case "/eliminargrupo":
                if(param.isEmpty()) writer.writeUTF("Falta nombre del grupo.");
                else GestorGrupos.eliminarGrupo(idSesion, param, this);
                break;
        }
    }

    private void gestionarLogicaJuego(String cmd, String[] args) throws IOException {
        switch (cmd) {
            case "/aceptar":
                GestorJuegos.aceptarInvitacion(idSesion, this);
                break;
            case "/rechazar":
                GestorJuegos.rechazarInvitacion(idSesion, this);
                break;
            case "/jugar":
                if (args.length < 2) {
                    writer.writeUTF("Uso: /jugar [usuario]");
                    return;
                }
                invitarJugador(args[1].trim());
                break;
            case "/mover":
                if (args.length != 3) {
                    writer.writeUTF("Uso: /mover [fila] [columna]");
                    return;
                }
                try {
                    int f = Integer.parseInt(args[1]);
                    int c = Integer.parseInt(args[2]);
                    GestorJuegos.procesarMovimiento(idSesion, f, c, this);
                } catch (NumberFormatException e) {
                    writer.writeUTF("Las coordenadas deben ser números.");
                }
                break;
        }
    }

    private void invitarJugador(String nombreDestino) throws IOException {
        ManejadorSesion destino = buscarSesionActiva(nombreDestino);
        
        if (destino == null) {
            writer.writeUTF("Usuario no encontrado o desconectado.");
            return;
        }
        if (destino.idSesion.equals(idSesion)) {
            writer.writeUTF("No puedes jugar contra ti mismo.");
            return;
        }
        
        // Verificación rápida de bloqueos antes de invitar
        String yo = SistemaAutenticacion.getNombreUsuarioReal(idSesion);
        String el = SistemaAutenticacion.getNombreUsuarioReal(destino.idSesion);
        
        if (yo != null && el != null && Database.estaBloqueado(yo, el)) {
            writer.writeUTF("No es posible conectar con ese usuario (Bloqueo activo).");
        } else {
            GestorJuegos.enviarInvitacion(idSesion, destino.idSesion, this, destino);
        }
    }

    private void gestionarMensajeria(String input, String comando) throws IOException {
        // Verificar cuota de invitado
        if (!SistemaAutenticacion.puedeEnviarMensajes(idSesion)) {
            writer.writeUTF("Has agotado tus 3 mensajes de prueba. Regístrate para continuar.");
            return;
        }

        // Mensaje Privado
        if (input.startsWith("@")) {
            enviarMensajePrivado(input);
            return;
        }

        // Mensaje Público / Grupo
        GestorGrupos.enviarMensajeGrupo(idSesion, input, this);
        actualizarCuotaMensajes();
    }

    private void enviarMensajePrivado(String rawInput) throws IOException {
        String[] partes = rawInput.split(" ", 2);
        if (partes.length < 2) {
            writer.writeUTF("Mensaje vacío. Uso: @Usuario Texto");
            return;
        }
        String targetName = partes[0].substring(1);
        String msg = partes[1];
        
        ManejadorSesion target = buscarSesionActiva(targetName);
        
        if (target != null) {
            // Lógica de bloqueo en privado
            String yo = SistemaAutenticacion.getNombreUsuarioReal(idSesion);
            String el = SistemaAutenticacion.getNombreUsuarioReal(target.idSesion);
            
            if (yo != null && el != null && Database.estaBloqueado(yo, el)) {
                writer.writeUTF("No puedes enviar mensajes a este usuario.");
                return;
            }

            String remitente = SistemaAutenticacion.getNombreDisplay(idSesion);
            target.writer.writeUTF("[MP] " + remitente + ": " + msg);
            writer.writeUTF("-> MP enviado a " + targetName);
            actualizarCuotaMensajes();
        } else {
            writer.writeUTF("El usuario " + targetName + " no está disponible.");
        }
    }

    private void actualizarCuotaMensajes() throws IOException {
        int restantes = SistemaAutenticacion.incrementarMensajes(idSesion);
        if (restantes >= 0) {
            writer.writeUTF("(Te quedan " + restantes + " mensajes gratuitos)");
        }
    }

    private void manejarCierreInesperado(IOException e) {
        String display = SistemaAutenticacion.getNombreDisplay(idSesion);
        String razon = "Desconocida";
        
        if (e instanceof EOFException) razon = "Cierre normal";
        else if (e instanceof SocketException) razon = "Interrupción de red (" + e.getMessage() + ")";

        System.out.println("Cierre de sesión: " + display + " [" + razon + "]");

        GestorJuegos.manejarDesconexion(idSesion);
        GestorGrupos.clienteDesconectado(idSesion);
        SistemaAutenticacion.limpiarCliente(idSesion);
        
        ServidorMulti.notificarTodos("--- " + display + " se ha desconectado ---", this);
        ServidorMulti.clientes.remove(idSesion);
        activo = false;
    }

    // Método de búsqueda helper renombrado
    private ManejadorSesion buscarSesionActiva(String nombreVisible) {
        ManejadorSesion s = (ManejadorSesion) ServidorMulti.clientes.get(nombreVisible); // Cast si es necesario
        if (s != null) return s;
        
        for (Object obj : ServidorMulti.clientes.values()) {
            ManejadorSesion ms = (ManejadorSesion) obj;
            if (SistemaAutenticacion.getNombreDisplay(ms.idSesion).equals(nombreVisible)) {
                return ms;
            }
        }
        return null;
    }
}