
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
    
    UnCliente (Socket s) throws IOException{
        salida = new DataOutputStream(s.getOutputStream());
        entrada = new DataInputStream (s.getInputStream());
    }
    
    @Override
    public void run(){
        String mensaje;
        while (true){
            try{
                mensaje = entrada.readUTF();
                Mensaje mensajeProcesado = new Mensaje(mensaje);
                enviarMensaje(mensajeProcesado);
            }catch(IOException ex){
            }
        }
    }

    private void enviarMensaje(Mensaje mensaje) throws IOException {
        salida.writeUTF(mensaje.toString());
        salida.flush();
    }
}
