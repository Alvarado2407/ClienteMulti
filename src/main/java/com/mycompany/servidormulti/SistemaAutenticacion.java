package com.mycompany.servidormulti;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SistemaAutenticacion {
    private static final int LIMITE_MENSAJES = 3;
    private static HashMap<String, String> usuariosRegistrados = new HashMap<>();
    private static HashMap<String, Integer> mensajesEnviados = new HashMap<>();
    private static HashMap<String, Boolean> usuariosAutenticados = new HashMap<>();
    private static HashMap<String, String> nombresUsuarios = new HashMap<>();

    private static HashMap<String, String> sesionesActivas = new HashMap<>();

    private static final List<String> COMANDOS_RESERVADOS = Arrays.asList(
            "registro", "login", "usuarios", "ranking", "vs",
            "creargrupo", "unirsegrupo", "eliminargrupo", "grupos",
            "jugar", "aceptar", "rechazar", "mover", "logout", "eliminarcuenta",
            "bloquear", "desbloquear", "bloqueados"
    );

    public static boolean puedeEnviarMensajes(String clienteId){
        if(estaAutenticado(clienteId)){
            return true;
        }

        int mensajes = mensajesEnviados.getOrDefault(clienteId, 0);
        return mensajes <  LIMITE_MENSAJES;
    }

    public static int incrementarMensajes(String clienteId){
        if (!estaAutenticado(clienteId)){
            int actual = mensajesEnviados.getOrDefault(clienteId, 0);
            actual++;
            mensajesEnviados.put(clienteId, actual);
            return LIMITE_MENSAJES - actual;
        }
        return -1;
    }

    private static boolean esNombreReservado(String nombre){
        String nombreL = nombre.toLowerCase();
        return COMANDOS_RESERVADOS.contains(nombreL) || nombreL.startsWith("/");
    }

    public static String procesarRegistro (String clienteId, String username, String password) {

        if(estaAutenticado(clienteId)){
            return "ERROR: ya tienes una sesion activa, usa /logout para cerrarla";
        }

        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            return "ERROR: usuario y contrasena no pueden estar vacios";
        }

        if(esNombreReservado(username)){
            return "ERROR: nombre de usuario reservado. Elige otro usuario";
        }

        if (Database.existeUsuario(username)) {
            return "ERROR: Usuario ya existe, usa /login para iniciar sesion";
        }

        if(sesionesActivas.containsKey(username)){
            return "ERROR: Esta cuenta ya tiene una sesion activa en otro dispositivo.";
        }

        if (Database.registrarUsuario(username, password)) {
            usuariosAutenticados.put(clienteId, true);
            nombresUsuarios.put(clienteId, username);
            sesionesActivas.put(username, clienteId);
            return "EXITO: Usuario registrado correctamente, ahora eres: " + username + ". Puedes enviar mensajes ilimitados";
        } else {
            return "ERROR: no se pudo registrar el usuario, intenta con otro nombre";
        }
    }

    public static String procesarLogin(String clienteId, String username, String password){

        if(estaAutenticado(clienteId)){
            return "ERROR: ya tienes una sesion activa con este usuario, usa /logout para cerrarla";
        }

        if(sesionesActivas.containsKey(username)){
            String clienteConSesion = sesionesActivas.get(username);
            if(!clienteConSesion.equals(clienteId)){
                return "ERROR: Esta cuenta ya tiene una sesion activa en otro dispositivo. Cierra sesion en el otro dispositivo primero";
            }
        }

        if (Database.validarLogin(username, password)) {
                usuariosAutenticados.put(clienteId, true);
                nombresUsuarios.put(clienteId, username);
                sesionesActivas.put(username, clienteId);
                return "EXITO: Login exitoso! Bienvenido de vuelta: " + username + ". Puedes enviar mensajes ilimitados.";
            } else {
                if (Database.existeUsuario(username)) {
                    return "ERROR: Contraseña incorrecta";
                } else {
                    return "ERROR: Usuario no encontrado. Usa /registro para crear una cuenta";
                }
            }
        }

        public static String procesarLogout(String clienteId){
        if(!estaAutenticado(clienteId)){
            return "ERROR: no tienes una sesion activa";
        }
        String nombreUsuario = nombresUsuarios.get(clienteId);
        sesionesActivas.remove(nombreUsuario);
        usuariosAutenticados.remove(clienteId);
        nombresUsuarios.remove(clienteId);
        mensajesEnviados.put(clienteId, 0);
        return "EXITO: Sesion cerrada correctamente. Ahora eres " + clienteId + " con 3 mensajes gratuitos";
        }

        public static String procesarEliminarCuenta(String clienteId){
        if(!estaAutenticado(clienteId)){
            return "ERROR: no tienes una sesion activa que eliminar";
        }
        String username = nombresUsuarios.get(clienteId);

        if(username == null){
            return "ERROR: no se pudo identificar el usuario";
        }

        if(Database.eliminarUsuario(username)){
            sesionesActivas.remove(username);
            usuariosAutenticados.remove(clienteId);
            nombresUsuarios.remove(clienteId);
            mensajesEnviados.put(clienteId, 0);
            return "EXITO: Cuenta eliminada permanentemente";
        }else{
            return "ERROR: no se pudo eliminar la cuenta";
        }

        }

        public static boolean estaAutenticado (String clienteId){
            return usuariosAutenticados.getOrDefault(clienteId, false);
        }

        public static String getNombreDisplay(String clienteId){
        if(estaAutenticado(clienteId)){
            return nombresUsuarios.get(clienteId);
        }
        return clienteId;
        }

        public static void limpiarCliente(String clienteId){
        mensajesEnviados.remove(clienteId);

        String username = nombresUsuarios.get(clienteId);
        if(username != null){
            sesionesActivas.remove(username);
        }

        usuariosAutenticados.remove(clienteId);
        nombresUsuarios.remove(clienteId);
        }

    public static String getNombreUsuarioReal(String clienteId){
        if(nombresUsuarios.containsKey(clienteId)){
            return nombresUsuarios.get(clienteId);
        }
        return null;
    }

    public static int getMensajesRestantes(String clienteId){
        if(estaAutenticado(clienteId)){
            return  -1;
        }
        int enviados = mensajesEnviados.getOrDefault(clienteId, 0);
        return LIMITE_MENSAJES - enviados;
        }

    public static boolean registrarUsuarioOffline(String username, String password) {
        if (usuariosRegistrados.containsKey(username)) {
            return false;
        }
        usuariosRegistrados.put(username, password);
        return true;
    }

    public static boolean validarLoginOffline(String username, String password) {
        String storedPassword = usuariosRegistrados.get(username);
        return storedPassword != null && storedPassword.equals(password);
    }

    public static boolean existeUsuarioOffline(String username) {
        return usuariosRegistrados.containsKey(username);
    }
    }


