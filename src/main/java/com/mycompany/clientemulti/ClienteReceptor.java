package com.mycompany.clientemulti;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

/**
 * Clase encargada de leer y mostrar los mensajes entrantes del servidor.
 * También maneja las desconexiones inesperadas.
 */
public class ClienteReceptor implements Runnable{
    
    // Cambios: Encapsulación (private) y nombres más descriptivos
    private final DataInputStream inputStream;
    private final Socket serverSocket;
    private volatile boolean isRunning = true;
    
    // Constructor actualizado
    public ClienteReceptor (Socket socket) throws IOException{
        this.serverSocket = socket;
        this.inputStream = new DataInputStream(socket.getInputStream());
    }
    
    /**
     * Señala que el hilo debe detenerse (generalmente llamado desde ClienteEmisor 
     * o desde el hilo principal al detectarse un cierre).
     */
    public void stopRunning() {
        this.isRunning = false;
        closeResources();
    }
    
    @Override
    public void run(){
        String message;
        
        try {
            while (isRunning){
                try{
                    // Bloquea esperando un mensaje
                    message = inputStream.readUTF();
                    System.out.println(message);
                    
                } catch (EOFException e){
                    // El servidor cerró limpiamente su flujo de salida
                    handleDisconnection("CONEXIÓN CERRADA: El servidor finalizó la sesión limpiamente.");
                    break;
                } catch (SocketException e){
                    // Error de red (reset, pipe roto, cierre forzado)
                    handleSocketError(e);
                    break;
                } catch (IOException e){
                    // Otros errores de comunicación
                    handleDisconnection("ERROR DE COMUNICACIÓN: " + e.getMessage());
                    break;
                }
            }
        } finally {
            // Aseguramos el cierre de recursos y la salida de la aplicación
            finalCleanup();
        }
    }

    /**
     * Muestra el mensaje de error de red y detiene la ejecución.
     */
    private void handleSocketError(SocketException e) {
        if (!isRunning) return; // Si ya se estaba cerrando, ignorar la excepción
        
        String msg = e.getMessage();
        
        // ** FORMATO SIMPLIFICADO **
        System.err.println("\n************************************************");
        if (msg.contains("reset") || msg.contains("closed") || msg.contains("pipe") || msg.contains("Conexión") || msg.contains("Connection")) {
            System.err.println("* ERROR DE RED: Se perdió la conexión con el servidor *");
            System.err.println("* Posibles causas: Servidor detenido, interrupción de red. *");
        } else {
            System.err.println("* ERROR DE CONEXIÓN: " + msg);
        }
        System.err.println("************************************************");
        
        stopRunning();
    }
    
    /**
     * Muestra un mensaje de desconexión general y detiene la ejecución.
     */
    private void handleDisconnection(String reason) {
        if (!isRunning) return;
        
        // ** FORMATO SIMPLIFICADO **
        System.err.println("\n------------------------------------------------");
        System.err.println("-- " + reason + " --");
        System.err.println("------------------------------------------------");
        
        stopRunning();
    }

    /**
     * Cierra los recursos.
     */
    private void closeResources(){
        try{
            if(inputStream != null){
                inputStream.close(); 
            }
            if(serverSocket != null && !serverSocket.isClosed()){
                serverSocket.close();
            }
        } catch(IOException e){
            // Ignorar errores al cerrar
        }
    }
    
    /**
     * Realiza las acciones finales después de la desconexión.
     */
    private void finalCleanup() {
        System.err.println("\nLa aplicación se cerrará en 5 segundos...");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.exit(0);
    }
}