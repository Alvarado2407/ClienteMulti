
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

    private String clienteUsuario;

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
            salida.writeUTF("Comandos disponibles:");
            salida.writeUTF("- Escribe un mensaje normal para enviarlo a todos");
            salida.writeUTF("- Usa @NombreUsuario mensaje para enviar mensaje privado");
            salida.writeUTF("- Usa /usuarios para ver usuarios conectados");
        } catch (IOException e) {
            System.out.println("Error enviando mensaje de bienvenida a " + clienteUsuario);
        }
        while(true){
            try {
                mensaje = entrada.readUTF();
                if(mensaje.equals("/usuarios")){
                    StringBuilder usuarios = new StringBuilder("Usuarios conectados: ");
                    for(String user : ServidorMulti.clientes.keySet()){
                        usuarios.append(user).append(" ");
                    }
                    salida.writeUTF(usuarios.toString());
                    continue;
                }
                if(mensaje.startsWith("@")){
                    String[] partes = mensaje.split(" ",2);
                    if(partes.length<2){
                        salida.writeUTF("Formato incorrecto, usa @NombreUsuario mensaje para enviar mensaje privado");
                        continue;
                    }
                    String aQuien = partes[0].substring(1);
                    String mensajePrivado = partes[1];
                    UnCliente clienteDestino = ServidorMulti.clientes.get(aQuien);

                    if(clienteDestino!=null){
                        String mensajeFormateado = "[PRIVADO]" + clienteUsuario + "te dice: " + mensajePrivado;
                        clienteDestino.salida.writeUTF(mensajeFormateado);
                        salida.writeUTF("Mensaje privado enviado a " + aQuien);
                    }else{
                        salida.writeUTF("No se pudo enviar un mensaje privado");
                    }
                    continue;
                }
                String mensajeConRemitente = clienteUsuario + ": " + mensaje;
                for(UnCliente cliente : ServidorMulti.clientes.values()){
                    if (!clienteUsuario.equals(cliente.getClienteUsuario())) {
                        cliente.salida.writeUTF(mensajeConRemitente);
                    }
                }
            } catch (IOException e) {
                System.out.println("cliente " + clienteUsuario + " se desconecto");
                ServidorMulti.notificarTodos("*** " + clienteUsuario + " se ha desconectado ***",this);
                ServidorMulti.clientes.remove(clienteUsuario);
                break;
            }

        }
    }
}
