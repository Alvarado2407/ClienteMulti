
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
            // Enviar mensaje de bienvenida al cliente
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
                    String[] partes = mensaje.split(" ");
                    String deQuien;
                    String aQuien = partes[0].substring(1);
                    UnCliente cliente = ServidorMulti.clientes.get(aQuien);
                    cliente.salida.writeUTF(mensaje);
                    return;
                }
                for(UnCliente cliente : ServidorMulti.clientes.values()){
                    cliente.salida.writeUTF(mensaje);
                }
            } catch (IOException e) {
            }

        }
    }
}
