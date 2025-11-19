package com.mycompany.servidormulti;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Autenticacion {
    private static final int LIMITE_MENSAJES = 3;
    private static HashMap<String, String> usuariosRegistrados = new HashMap<>();
    private static HashMap<String, Integer> mensajesEnviados = new HashMap<>();
    private static HashMap<String, Boolean> usuariosAutenticados = new HashMap<>();
    private static HashMap<String, String> nombresUsuarios = new HashMap<>();

    private static HashMap<String, String> sesionesActivas = new HashMap<>();


    private static final List<String> COMANDOS_RESERVADOS = Arrays.asList(
            "registro", "login", "usuarios", "ranking", "vs",
            "creargrupo", "unirsegrupo", "eliminargrupo", "grupos",
            "jugar", "aceptar", "rechazar", "mover"
    );


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

    private static boolean esNombreReservado(String nombre){
        String nombreL = nombre.toLowerCase();
        return COMANDOS_RESERVADOS.contains(nombreL) || nombreL.startsWith("/");
    }

    public static String procesarRegistro(String clienteUsuario, String usuario, String contra) {

        if(estaAutenticado(clienteUsuario)){
            return "ERROR: ya tienes una sesion activa, usa /logout para cerrarla";
        }


        if (usuario == null || contra == null || usuario.trim().isEmpty() || contra.trim().isEmpty()) {
            return "El usuario y contrase;a no pueden estar vacios";
        }

        if(esNombreReservado(usuario)){
            return "ERROR: nombre de usuario reservado. Elige otro usuario";
        }

        if(sesionesActivas.containsKey(usuario)){
            return "ERROR: Esta cuenta ya tiene una sesion activa en otro dispositivo.";
        }

        if (usuariosRegistrados.containsKey(usuario)) {
            return "El usuario ya existe. Usa /login para iniciar sesion";
        }
        usuariosRegistrados.put(usuario, contra);
        usuariosAutenticados.put(clienteUsuario, true);
        nombresUsuarios.put(clienteUsuario, usuario);
        sesionesActivas.put(usuario, clienteUsuario);

        return "EXITO, eres: " + usuario + " puedes enviar mensajes ilimitados.";
    }

    public static String procesarLogin(String clienteUsuario, String usuario, String contra) {

        if(estaAutenticado(clienteUsuario)){
            return "ERROR: ya tienes una sesion activa como " + nombresUsuarios + ", usa /logout para cerrarla";
        }

        if(sesionesActivas.containsKey(usuario)){
            String clienteConSesion = sesionesActivas.get(usuario);
            if(!clienteConSesion.equals(clienteUsuario)){
                return "ERROR: Esta cuenta ya tiene una sesion activa en otro dispositivo. Cierra sesion en el otro dispositivo primero";
            }
        }

        if(!usuariosRegistrados.containsKey(usuario)){
            return "Usuario no encontrado, usa /registro para crear una cuenta.";
        }
        if(!usuariosRegistrados.get(usuario).equals(contra)){
            return "Contrase;a incorrecta";
        }
        usuariosAutenticados.put(clienteUsuario, true);
        nombresUsuarios.put(clienteUsuario, usuario);
        sesionesActivas.put(usuario, clienteUsuario);
        return "EXITO, bienvenido de vuelta: " + usuario;
    }

    public static String procesarLogout(String clienteUsuario) {
        if(!estaAutenticado(clienteUsuario)){
            return "ERROR: no tienes una sesion activa";
        }
        String nombreUsuario = nombresUsuarios.get(clienteUsuario);
        sesionesActivas.remove(nombreUsuario);
        usuariosAutenticados.remove(clienteUsuario);
        nombresUsuarios.remove(clienteUsuario);

        mensajesEnviados.put(clienteUsuario,0);
        return "EXITO: Sesion cerrada correctamente. Ahora eres " + clienteUsuario;
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

        String usuario = nombresUsuarios.get(clienteUsuario);
        if(usuario != null){
            sesionesActivas.remove(usuario);
        }

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

