package com.mycompany.clientemulti;

import java.io.IOException;
import java.net.Socket;
import java.net.ConnectException;

public class ClienteMulti {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;
    private static final int MAX_ATTEMPTS = 5;

    public static void main(String[] args) {
        // Encabezado simplificado
        System.out.println("CLIENTE DE CHAT MULTIJUGADOR");
        System.out.println("----------------------------\n");

        Socket socket = attemptConnection();

        if (socket != null) {
            try {
                startCommunicationThreads(socket);
            } catch (IOException e) {
                printError("ERROR AL INICIAR COMUNICACIÓN", e.getMessage());
                System.exit(1);
            }
        }
    }

    /**
     * Intenta conectar al servidor con múltiples reintentos.
     * @return El Socket conectado o null si falla después de los intentos.
     */
    private static Socket attemptConnection() {
        Socket s = null;
        int currentAttempt = 0;

        while (currentAttempt < MAX_ATTEMPTS && s == null) {
            try {
                System.out.println("Intentando conectar a " + SERVER_HOST + ":" + SERVER_PORT + 
                                   "... (Intento " + (currentAttempt + 1) + "/" + MAX_ATTEMPTS + ")");
                s = new Socket(SERVER_HOST, SERVER_PORT);
                System.out.println("¡Conectado exitosamente al servidor!");
                return s; // Éxito
            } catch (ConnectException e) {
                currentAttempt++;
                if (currentAttempt < MAX_ATTEMPTS) {
                    System.out.println("No se pudo conectar. Reintentando en 2 segundos...");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    printConnectionFailure();
                    System.exit(1);
                }
            } catch (IOException e) {
                printError("ERROR GENERAL DE CONEXIÓN", e.getMessage());
                System.exit(1);
            }
        }
        return null;
    }
    
    /**
     * Inicia los hilos de envío y recepción.
     * Usa las clases refactorizadas: ClienteEmisor (antes ParaMandar) y ClienteReceptor (antes ParaRecibir).
     */
    private static void startCommunicationThreads(Socket socket) throws IOException {
        final Socket socketFinal = socket;

        // Agregar shutdown hook para cerrar conexión limpiamente
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if(socketFinal != null && !socketFinal.isClosed()){
                    socketFinal.close();
                }
            } catch (IOException e) {
                // Ignorar
            }
        }));
        
        // Uso de las nuevas clases refactorizadas (ClienteEmisor y ClienteReceptor)
        ClienteEmisor clienteEmisor = new ClienteEmisor(socket);
        Thread hiloEmisor = new Thread(clienteEmisor);
        hiloEmisor.start();
        
        ClienteReceptor clienteReceptor = new ClienteReceptor(socket);
        Thread hiloReceptor = new Thread(clienteReceptor);
        hiloReceptor.start();
    }
    
    /**
     * Imprime un mensaje de error en formato simplificado.
     */
    private static void printError(String title, String message) {
        System.err.println("\n--- " + title + " ---");
        System.err.println("Mensaje: " + message);
        System.err.println("------------------------------------");
    }

    /**
     * Imprime un mensaje de falla de conexión después de todos los intentos.
     */
    private static void printConnectionFailure() {
        System.err.println("\n*** ERROR: NO SE PUDO CONECTAR AL SERVIDOR ***");
        System.err.println("Después de " + MAX_ATTEMPTS + " intentos.");
        System.err.println("Verifica que:");
        System.err.println("1. ServidorMulti esté ejecutándose");
        System.err.println("2. El puerto " + SERVER_PORT + " esté disponible");
        System.err.println("3. Tu firewall permita la conexión");
        System.err.println("******************************************");
    }
}