/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.servidormulti;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

/**
 *
 * @author Yatzi
 */
public class ServidorMulti {
    static HashMap<String, UnCliente> clientes = new HashMap <>();

    public static void notificarTodos(String mensaje, UnCliente excluir){
        for(UnCliente cliente : clientes.values()){
            if(cliente!=excluir){
                try{
                    cliente.salida.writeUTF(mensaje);
                }catch(IOException e){}
            }
        }
    }
    
    public static void main(String[] args) throws IOException {
        Database.inicializar();
        ServerSocket servidorSocket = new ServerSocket(8080);
        int contador = 0;
        if(Database.estaConectado()){
            System.out.println("Base de datos conectada. Los usuarios se guardaran permanentemente");
        }else{
            System.out.println("Modo offline. Los usuarios se guardaran solo en memoria");
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Database.cerrarConexion();
        }));

        while (true){
            Socket s = servidorSocket.accept();
            String clienteUsuario = "Usuario" + contador;
            UnCliente unCliente = new UnCliente(s, clienteUsuario);
            Thread hilo = new Thread(unCliente);
            clientes.put(clienteUsuario, unCliente);
            hilo.start();
            System.out.println("Se conectó el "+clienteUsuario);
            notificarTodos("*** " + clienteUsuario + "se ha conectado al chat ***", unCliente);
            contador++;
        }
    }
}
