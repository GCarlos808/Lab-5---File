package lab5file;

import java.util.ArrayList;
import java.util.List;


public class SystemFile {

    public static class Nodo {
        
        public enum Tipo {
            DIR,
            FILE
        }
        
    private final Tipo tipo;
    private final String nombre;
    private String contenido;
    private final List<Nodo> hijos;
    
         public Nodo(String nombre, Tipo tipo) {
                this.nombre = nombre;
                this.tipo = tipo;
                this.contenido = "";
                this.hijos = (tipo == Tipo.DIR) ? new ArrayList<>() : null;
        }
         
        public Tipo getTipo() {
            return tipo;
        }
        public String getNombre() {
            return nombre;
        }
        public String getContenido() {
            return contenido;
        }
        public void setContenido(String c) {
            this.contenido = c;
        }
        public List<Nodo> getHijos() {
            return hijos;
        }
        public boolean esDir() {
            return tipo == Tipo.DIR;
        }
        public boolean esFile() {
            return tipo == Tipo.FILE;
        }
    
    }
}
