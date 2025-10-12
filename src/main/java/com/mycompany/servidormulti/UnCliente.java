
package com.mycompany.servidormulti;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;


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
            salida.writeUTF("--Usa /registro usuario contrase;a para registrarte");
            salida.writeUTF("--Usa /login usuario contrase;a para iniciar sesion");

        } catch (IOException e) {
            System.out.println("Error enviando mensaje de bienvenida a " + clienteUsuario);
        }
        while(true){
            try {
                mensaje = entrada.readUTF();

                if(mensaje.startsWith("/registro")) {
                    String[] partes = mensaje.split(" ");
                    if (partes.length == 3) {
                        String resultado = Autenticacion.procesarRegistro(clienteUsuario, partes[1], partes[2]);
                        salida.writeUTF(resultado);
                        if (resultado.startsWith("EXITO")) {
                            ServidorMulti.notificarTodos("*** " + clienteUsuario + " ahora es " + partes[1] + "  ***", this);
                        }
                    } else {
                        salida.writeUTF("El formato es /registro usuario contrase;a");
                    }

                    continue;
                }
                    if(mensaje.startsWith("/login ")){
                        String[] parteslog = mensaje.split(" ");
                        if(parteslog.length == 3){
                            String resultado = Autenticacion.procesarLogin(clienteUsuario, parteslog[1], parteslog[2]);
                            salida.writeUTF(resultado);
                            if(resultado.startsWith("EXITO")){
                                ServidorMulti.notificarTodos("*** " + clienteUsuario + " ahora es " + parteslog[1] + "  ***",this);
                            }
                        }else{
                            salida.writeUTF("El formato es /login usuario contrase;a");
                        }
                        continue;
                    }


                if(mensaje.equals("/usuarios")) {
                    StringBuilder usuarios = new StringBuilder("Usuarios conectados: ");
                    for (String user : ServidorMulti.clientes.keySet()) {
                        usuarios.append(Autenticacion.getNombreDisplay(user)).append(", ");
                    }
                    salida.writeUTF(usuarios.toString());
                    continue;
                }
                    if(!Autenticacion.puedeEnviarMensajes(clienteUsuario)){
                        salida.writeUTF("Has alcanzado el limite de 3 mensajes gratuitos");
                        salida.writeUTF("Registrate (/registro) o inicia sesion (/login) para enviar mensajes ilimitados");
                        salida.writeUTF("Puedes seguir viendo los mensajes de los demas usuarios");

                }
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
                        String nombreRemitente = Autenticacion.getNombreDisplay(clienteUsuario);
                        String mensajeFormateado = "[PRIVADO] " + nombreRemitente + " te dice: " + mensajePrivado;
                        clienteDestino.salida.writeUTF(mensajeFormateado);

                        salida.writeUTF("Mensaje privado enviado a " + Autenticacion.getNombreDisplay(clienteDestino.clienteUsuario));
                        int restantes = Autenticacion.incrementarMensajes(clienteUsuario);
                        if(restantes>=0){
                            salida.writeUTF("Te quedan " + restantes + " mensajes gratuitos");
                        }
                    }else{
                        salida.writeUTF("No se pudo enviar un mensaje privado");
                    }
                    continue;
                }
                String nombreRemitente = Autenticacion.getNombreDisplay(clienteUsuario);
                String mensajeConRemitente = nombreRemitente + ": " + mensaje;
                for(UnCliente cliente : ServidorMulti.clientes.values()){
                    if (!clienteUsuario.equals(cliente.getClienteUsuario())) {
                        cliente.salida.writeUTF(mensajeConRemitente);
                    }
                }

                int restantes = Autenticacion.incrementarMensajes(clienteUsuario);
                if(restantes>=0){
                    salida.writeUTF("Te quedan " + restantes + " mensajes gratuitos");
                }

            } catch (IOException e) {
                System.out.println("cliente " + Autenticacion.getNombreDisplay(clienteUsuario) + " se desconecto");
                Autenticacion.limpiarCliente(clienteUsuario);
                ServidorMulti.notificarTodos("*** " + Autenticacion.getNombreDisplay(clienteUsuario) + " se ha desconectado ***",this);
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
            if(Autenticacion.getNombreDisplay(clienteUsuario).equals(nombre)){
                return c;
            }
        }
        return null;
    }
}
