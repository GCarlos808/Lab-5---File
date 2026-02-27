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
    public static class Resultado {
        public final boolean ok;
        public final String mensaje;
        public final Object datos;

        public Resultado(boolean ok, String mensaje) { this(ok, mensaje, null); }
        public Resultado(boolean ok, String mensaje, Object datos) { 
            this.ok = ok; this.mensaje = mensaje; this.datos = datos; 
        }
    }
    public static class ListadoDir {
        public final List<String> carpetas = new ArrayList<>();
        public final List<String> archivos = new ArrayList<>();
        public final List<Long> tamanos = new ArrayList<>();
    }
    private final Nodo root;
    private final List<String> rutaActual;

    public SystemFile() {
        this.root = new Nodo("raiz", Nodo.Tipo.DIR);
        this.rutaActual = new ArrayList<>();
    }
    
    private Nodo buscarHijo(Nodo padre, String nombre) {
        if (padre.getHijos() == null) return null;
        for (Nodo hijo : padre.getHijos()) {
            if (hijo.getNombre().equalsIgnoreCase(nombre)) {
                return hijo;
            }
        }
        return null;
    }
    
    private Nodo getDirActual() {
        Nodo nodo = root;
        for (String parte : rutaActual) {
            nodo = buscarHijo(nodo, parte);
        }
        return nodo;
    }
    
    public String getRutaString() {
        if (rutaActual.isEmpty()) return "C:\\";
        return "C:\\" + String.join("\\", rutaActual) + "\\";
    }
    
    public String getPrompt() {
        String ruta = getRutaString();
        return ruta.endsWith("\\") ? ruta.substring(0, ruta.length() - 1) + ">" : ruta + ">";
    }
}
