
package com.mycompany.servidormulti;

public class Mensaje {
    public String[] recipientes;
    public String valor;
    
    public Mensaje (String original){
        if(esParaTodos(original)){
            valor = original;
            return;
        }
        recipientes = encontrarRecipientes(original);
    }

    private boolean esParaTodos(String original) {
        return original != null && original.trim().startsWith("@all");
    }
    private String[] encontrarRecipientes(String original){
        String primeraParte = original.split(" ")[0];
        valor = original.substring(original.indexOf(" "));
        String resto = primeraParte.substring(1);
        return resto.split(",");
    }
}
