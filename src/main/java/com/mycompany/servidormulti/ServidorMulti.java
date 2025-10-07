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
    
    public static void main(String[] args) throws IOException {
        ServerSocket servidorSocket = new ServerSocket(8080);
        int contador = 0;
        while (true){
            Socket s = servidorSocket.accept();
            UnCliente unCliente = new UnCliente(s);
            Thread hilo = new Thread(unCliente);
            clientes.put(Integer.toString(contador), unCliente);
            hilo.start();
            System.out.println("Se conectó el usuario "+contador);
            contador++;
        }
    }
}
