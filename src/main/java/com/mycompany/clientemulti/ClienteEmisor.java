package com.mycompany.clientemulti;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;

/**
 * Clase encargada de leer la entrada del teclado del usuario y enviar
 * los mensajes al servidor a través del flujo de salida.
 */
public class ClienteEmisor implements Runnable {
    
    // Cambios: Encapsulación (private) y nombres más descriptivos
    private DataOutputStream outputStream;
    private final BufferedReader keyboardReader;
    private final Socket serverSocket;
    private volatile boolean isRunning = true;

    // Constructor actualizado
    public ClienteEmisor(Socket socket) throws IOException {
        // Inicialización de flujos
        this.outputStream = new DataOutputStream(socket.getOutputStream());
        this.keyboardReader = new BufferedReader(new InputStreamReader(System.in));
        this.serverSocket = socket;
    }
    
    /**
     * Señala que el hilo debe detenerse y cierra los recursos.
     */
    public void stopRunning() {
        this.isRunning = false;
        closeResources();
    }

    @Override
    public void run() {
        String inputMessage;
        
        try {
            while (isRunning) {
                // Bloquea aquí esperando la entrada del teclado
                inputMessage = keyboardReader.readLine();
                
                if (inputMessage == null || inputMessage.equalsIgnoreCase("/salir")) {
                    System.out.println("Desconexión solicitada...");
                    stopRunning(); // Señalamos la parada y salimos del bucle
                    break;
                }
                
                // Envía el mensaje al servidor
                sendMessageToServer(inputMessage);
            }
        } catch (IOException e) {
            handleCommunicationError(e);
        } finally {
            // Asegura que los recursos se cierren si el bucle termina por cualquier razón
            closeResources();
        }
        
        // Solo salimos del sistema si se solicitó explícitamente y el hilo receptor también termina.
        // NOTA: En una aplicación real, el hilo principal manejaría el System.exit(0)
    }

    /**
     * Envía el mensaje al servidor, manejando la posibilidad de que el flujo esté cerrado.
     */
    private void sendMessageToServer(String message) throws IOException {
        if (outputStream != null) {
            outputStream.writeUTF(message);
        }
    }

    /**
     * Maneja errores de comunicación y desconexión del servidor.
     */
    private void handleCommunicationError(IOException e) {
        if (!isRunning) return; // Si ya se estaba cerrando, ignorar la excepción
        
        System.err.println("\n--- ERROR DE COMUNICACIÓN O CONEXIÓN PERDIDA ---");
        
        // Patron de manejo de errores de conexión (mejorado)
        if (e instanceof SocketException) {
            String msg = e.getMessage();
            if (msg.contains("reset") || msg.contains("abort") || msg.contains("pipe") || msg.contains("closed")) {
                System.err.println("El servidor cerró la conexión inesperadamente.");
            } else {
                System.err.println("Error de red: " + msg);
            }
        } else if (e instanceof EOFException) {
            System.err.println("El servidor cerró el flujo de datos.");
        } else {
            System.err.println("Error de I/O desconocido: " + e.getMessage());
        }
        
        System.err.println("Cerrando la aplicación.");
        
        // Notificamos al otro hilo (receptor) que también debe terminar
        // En una aplicación real, necesitarías una referencia al otro hilo (ParaRecibir) para pararlo.
        
        stopRunning();
        // Se espera que el hilo principal o el receptor cierren System.exit(0)
    }
    
    /**
     * Cierra todos los flujos y el socket.
     */
    private void closeResources(){
        try{
            if(outputStream != null){
                outputStream.close();
                outputStream = null; // Para evitar doble cierre accidental
            }
            if(keyboardReader != null){
                // Solo se debe cerrar System.in si la aplicación va a terminar
            }
            if(serverSocket != null && !serverSocket.isClosed()){
                serverSocket.close();
            }
        } catch(IOException e){
            // Ignorar errores al intentar cerrar recursos ya rotos
        }
    }
}