

package com.mycompany.clientemulti;

import java.io.IOException;
import java.net.Socket;
import java.net.ConnectException;

public class ClienteMulti {

    public static void main(String[] args) {
        int maxIntentos = 5;
        int intentoActual = 0;
        Socket s = null;
        
        while (intentoActual < maxIntentos && s == null) {
            try {
                System.out.println("Intentando conectar al servidor... (Intento " + (intentoActual + 1) + "/" + maxIntentos + ")");
                s = new Socket("localhost", 8080);
                System.out.println("¡Conectado exitosamente al servidor!");
            } catch (ConnectException e) {
                intentoActual++;
                if (intentoActual < maxIntentos) {
                    System.out.println("No se pudo conectar. Reintentando en 2 segundos...");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    System.err.println("ERROR: No se pudo conectar al servidor después de " + maxIntentos + " intentos.");
                    System.err.println("Verifica que ServidorMulti esté ejecutándose en localhost:8080");
                    System.exit(1);
                }
            } catch (IOException e) {
                System.err.println("Error de conexión: " + e.getMessage());
                System.exit(1);
            }
        }
        
        try {
            ParaMandar paraMandar = new ParaMandar(s);
            Thread hiloParaMandar = new Thread(paraMandar);
            hiloParaMandar.start();
            
            ParaRecibir paraRecibir = new ParaRecibir(s);
            Thread hiloParaRecibir = new Thread(paraRecibir);
            hiloParaRecibir.start();
        } catch (IOException e) {
            System.err.println("Error al iniciar los hilos de comunicación: " + e.getMessage());
            System.exit(1);
        }
    }
}
