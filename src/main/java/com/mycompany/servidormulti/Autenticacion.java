package com.mycompany.servidormulti;

import java.util.HashMap;

public class Autenticacion {
    private static final int LIMITE_MENSAJES = 3;
    private static HashMap<String, String> usuariosRegistrados = new HashMap<>();
    private static HashMap<String, Integer> mensajesEnviados = new HashMap<>();
    private static HashMap<String, Boolean> usuariosAutenticados = new HashMap<>();
    private static HashMap<String, String> nombresUsuarios = new HashMap<>();

    public static boolean puedeEnviarMensajes(String clienteUsuario) {
        if (estaAutenticado(clienteUsuario)) {
            return true;
        }

        int mensajes = mensajesEnviados.getOrDefault(clienteUsuario, 0);
        return mensajes < LIMITE_MENSAJES;
    }

    public static int incrementarMensajes(String clienteUsuario) {
        if(!estaAutenticado(clienteUsuario)){
            int actual = mensajesEnviados.getOrDefault(clienteUsuario, 0);
            actual++;
            mensajesEnviados.put(clienteUsuario, actual);
            return LIMITE_MENSAJES - actual;
        }
        return -1;
    }

    public static String procesarRegistro(String clienteUsuario, String usuario, String contra) {
        if (usuario == null || contra == null || usuario.trim().isEmpty() || contra.trim().isEmpty()) {
            return "El usuario y contrase;a no pueden estar vacios";
        }
        if (usuariosRegistrados.containsKey(usuario)) {
            return "El usuario ya existe. Usa /login para iniciar sesion";
        }
        usuariosRegistrados.put(usuario, contra);
        usuariosAutenticados.put(clienteUsuario, true);
        nombresUsuarios.put(clienteUsuario, usuario);

        return "EXITO, eres: " + usuario + " puedes enviar mensajes ilimitados.";
    }

    public static String procesarLogin(String clienteUsuario, String usuario, String contra) {
        if(!usuariosRegistrados.containsKey(usuario)){
            return "Usuario no encontrado, usa /registro para crear una cuenta.";
        }
        if(!usuariosRegistrados.get(usuario).equals(contra)){
            return "Contrase;a incorrecta";
        }
        usuariosAutenticados.put(clienteUsuario, true);
        nombresUsuarios.put(clienteUsuario, usuario);
        return "EXITO, bienvenido de vuelta: " + usuario;
    }

    public static boolean estaAutenticado(String clienteUsuario) {
        return usuariosAutenticados.getOrDefault(clienteUsuario, false);
    }

    public static String getNombreDisplay(String clienteUsuario) {
        if(estaAutenticado(clienteUsuario)){
            return nombresUsuarios.get(clienteUsuario);
        }
        return clienteUsuario;
    }

    public static void limpiarCliente(String clienteUsuario) {
        mensajesEnviados.remove(clienteUsuario);
        nombresUsuarios.remove(clienteUsuario);
        usuariosAutenticados.remove(clienteUsuario);
    }


    public static int getMensajesRestantes(String clienteUsuario) {
        if(estaAutenticado(clienteUsuario)){
            return -1;
        }
        int enviados = mensajesEnviados.getOrDefault(clienteUsuario, 0);
        return LIMITE_MENSAJES - enviados;
    }


}

