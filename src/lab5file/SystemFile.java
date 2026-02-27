package lab5file;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;



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
        
        public Nodo buscarHijo(String nombre) {
            if (hijos == null) return null;
            for (Nodo n : hijos)
                if (n.getNombre().equals(nombre)) return n;
            return null;
        }
        
        public boolean eliminarHijo(String nombre) {
            if (hijos == null) return false;
            return hijos.removeIf(n -> n.getNombre().equals(nombre));
        }
        
        public boolean existeHijo(String nombre) {
            return buscarHijo(nombre) != null;
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
        public final List<Long>   tamanos  = new ArrayList<>();
    }
    
    private static final String RAIZ_DISCO = "cmd_data";
    
    private final Nodo         root;
    private final List<String> rutaActual;
    
    public SystemFile() {
        this.root      = new Nodo("root", Nodo.Tipo.DIR);
        this.rutaActual = new ArrayList<>();
        new File(RAIZ_DISCO).mkdirs();
        cargarDescoDisc(root, new File(RAIZ_DISCO));
    }
    
    private void cargarDescoDisc(Nodo nodoVirtual, File dirReal) {
        File[] items = dirReal.listFiles();
        if (items == null) return;
        for (File item : items) {
            if (item.isDirectory()) {
                Nodo sub = new Nodo(item.getName(), Nodo.Tipo.DIR);
                nodoVirtual.getHijos().add(sub);
                cargarDescoDisc(sub, item);
            } else {
                Nodo archivo = new Nodo(item.getName(), Nodo.Tipo.FILE);
                archivo.setContenido(leerDisco(item));
                nodoVirtual.getHijos().add(archivo);
            }
        }
    }
    
    private String leerDisco(File f) {
        StringBuilder sb = new StringBuilder();
        try (FileReader fr = new FileReader(f);
             BufferedReader br = new BufferedReader(fr)) {
            String linea;
            while ((linea = br.readLine()) != null) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(linea);
            }
        } catch (Exception ignored) {}
        return sb.toString();
    }
    
    private File getRutaReal() {
        File f = new File(RAIZ_DISCO);
        for (String parte : rutaActual)
            f = new File(f, parte);
        return f;
    }
    
    private Nodo getDirActual() {
        Nodo nodo = root;
        for (String parte : rutaActual)
            nodo = nodo.buscarHijo(parte);
        return nodo;
    }
    
    public String getRutaString() {
        if (rutaActual.isEmpty()) return "C:\\";
        return "C:\\" + String.join("\\", rutaActual) + "\\";
    }
    
    public String getPrompt() {
        String r = getRutaString();
        return r.endsWith("\\") ? r.substring(0, r.length() - 1) + ">" : r + ">";
    }
    
    public Resultado mkdir(String nombre) {
        nombre = nombre.trim();
        Nodo dir = getDirActual();
        if (dir.existeHijo(nombre))
            return new Resultado(false, "Ya existe: \"" + nombre + "\"");
        
        File carpetaReal = new File(getRutaReal(), nombre);
        if (!carpetaReal.mkdirs())
            return new Resultado(false, "No se pudo crear la carpeta en disco.");
        
        
        dir.getHijos().add(new Nodo(nombre, Nodo.Tipo.DIR));
        return new Resultado(true, "Carpeta creada: " + nombre);
    }
    
    public Resultado cd(String nombre) {
        nombre = nombre.trim();
        Nodo dir  = getDirActual();
        Nodo hijo = dir.buscarHijo(nombre);
        if (hijo == null)
            return new Resultado(false, "No existe la carpeta: \"" + nombre + "\"");
        if (!hijo.esDir())
            return new Resultado(false, "\"" + nombre + "\" no es una carpeta.");
        rutaActual.add(nombre);
        return new Resultado(true, "Directorio actual: " + getRutaString());
    }
    
    public Resultado regresar() {
        if (rutaActual.isEmpty())
            return new Resultado(false, "Ya esta en el directorio raiz C:\\");
            rutaActual.remove(rutaActual.size() - 1);
            return new Resultado(true, "Regreso a: " + (rutaActual.isEmpty() ? "C:\\" : getRutaString()));
    }
    public Resultado mfile(String nombre) {
        nombre = nombre.trim();
        if (!nombre.contains("."))
            return new Resultado(false, "El archivo necesita extension. Ej: .txt .jpge");
        Nodo dir = getDirActual();
        if (dir.existeHijo(nombre))
            return new Resultado(false, "Ya existe: \"" + nombre + "\"");
        
        File archivoReal = new File(getRutaReal(), nombre);
        try {
            new FileWriter(archivoReal, false).close();
        } catch (Exception e) {
            return new Resultado(false, "No se pudo crear el archivo en disco.");
        }
        
        dir.getHijos().add(new Nodo(nombre, Nodo.Tipo.FILE));
        return new Resultado(true, "Archivo creado: " + nombre);
    }
    
    public Resultado rm(String nombre) {
        nombre = nombre.trim();
        Nodo dir = getDirActual();
        if (!dir.existeHijo(nombre))
            return new Resultado(false, "No se encontro: \"" + nombre + "\"");

        File itemReal = new File(getRutaReal(), nombre);
        borrarRecursivo(itemReal);

        dir.eliminarHijo(nombre);
        return new Resultado(true, "\"" + nombre + "\" eliminado.");
    }
    
    private void borrarRecursivo(File f) {
        if (f.isDirectory())
            for (File hijo : f.listFiles())
                borrarRecursivo(hijo);
        f.delete();
    }
    
    public Resultado abrirParaEscritura(String nombre) {
        nombre = nombre.trim();
        Nodo dir  = getDirActual();
        Nodo hijo = dir.buscarHijo(nombre);
        if (hijo == null)
            return new Resultado(false, "El archivo \"" + nombre + "\" no existe. Use Mfile primero.");
        if (!hijo.esFile())
            return new Resultado(false, "\"" + nombre + "\" no es un archivo.");
        return new Resultado(true, "ok", hijo.getContenido());
    }
    
    public Resultado escribirArchivo(String nombre, String contenido) {
        Nodo dir  = getDirActual();
        Nodo hijo = dir.buscarHijo(nombre);
        if (hijo == null)
            return new Resultado(false, "El archivo \"" + nombre + "\" no existe.");
        
        File archivoReal = new File(getRutaReal(), nombre);
        try (FileWriter fw = new FileWriter(archivoReal, false)) {
            fw.write(contenido);
        } catch (Exception e) {
            return new Resultado(false, "Error al escribir en disco: " + e.getMessage());
        }
        
        hijo.setContenido(contenido);
        return new Resultado(true, "Guardado \"" + nombre + "\". " + contenido.length() + " caracter(es).");
    }
    
    public Resultado leerArchivo(String nombre) {
        nombre = nombre.trim();
        Nodo dir  = getDirActual();
        Nodo hijo = dir.buscarHijo(nombre);
        if (hijo == null)
            return new Resultado(false, "El archivo \"" + nombre + "\" no existe.");
        if (!hijo.esFile())
            return new Resultado(false, "\"" + nombre + "\" no es un archivo.");

        File archivoReal = new File(getRutaReal(), nombre);
        String contenido = leerDisco(archivoReal);
        hijo.setContenido(contenido);
        return new Resultado(true, "OK", contenido);
    }
    
    public ListadoDir listar() {
        ListadoDir res = new ListadoDir();
        for (Nodo nodo : getDirActual().getHijos()) {
            if (nodo.esDir()) {
                res.carpetas.add(nodo.getNombre());
            } else {
                res.archivos.add(nodo.getNombre());
                res.tamanos.add(new File(getRutaReal(), nodo.getNombre()).length());
            }
        }
        
        return res;
    }
}