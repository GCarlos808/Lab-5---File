package lab5file;

import java.io.*;
import java.util.*;

public class SystemFile {

    public static class Nodo {
        public enum Tipo { DIR, FILE }

        private final String     nombre;
        private final Tipo       tipo;
        private String           contenido;
        private final List<Nodo> hijos;

        public Nodo(String nombre, Tipo tipo) {
            this.nombre    = nombre;
            this.tipo      = tipo;
            this.contenido = "";
            this.hijos     = (tipo == Tipo.DIR) ? new ArrayList<>() : null;
        }

        public String     getNombre()            { return nombre; }
        public boolean    esDir()                { return tipo == Tipo.DIR; }
        public boolean    esFile()               { return tipo == Tipo.FILE; }
        public String     getContenido()         { return contenido; }
        public void       setContenido(String c) { contenido = c; }
        public List<Nodo> getHijos()             { return hijos; }

        public Nodo buscarHijo(String n) {
            if (hijos == null) return null;
            for (Nodo h : hijos)
                if (h.nombre.equalsIgnoreCase(n)) return h;
            return null;
        }
        public boolean existeHijo(String n)   { return buscarHijo(n) != null; }
        public boolean eliminarHijo(String n) {
            if (hijos == null) return false;
            return hijos.removeIf(h -> h.nombre.equalsIgnoreCase(n));
        }
    }

    public static class Resultado {
        public final boolean ok;
        public final String  mensaje;
        public final Object  datos;

        public Resultado(boolean ok, String mensaje)              { this(ok, mensaje, null); }
        public Resultado(boolean ok, String mensaje, Object datos){
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
    private final List<String> rutaActual = new ArrayList<>();

    public SystemFile() {
        root = new Nodo("root", Nodo.Tipo.DIR);
        new File(RAIZ_DISCO).mkdirs();
        cargarDesdeDisco(root, new File(RAIZ_DISCO));
    }

    private void cargarDesdeDisco(Nodo nodo, File dir) {
        File[] items = dir.listFiles();
        if (items == null) return;
        for (File item : items) {
            if (item.isDirectory()) {
                Nodo sub = new Nodo(item.getName(), Nodo.Tipo.DIR);
                nodo.getHijos().add(sub);
                cargarDesdeDisco(sub, item);
            } else {
                Nodo arch = new Nodo(item.getName(), Nodo.Tipo.FILE);
                arch.setContenido(leerDisco(item));
                nodo.getHijos().add(arch);
            }
        }
    }

    private String leerDisco(File f) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(linea);
            }
        } catch (Exception ignored) {}
        return sb.toString();
    }

    private Nodo getDirActual() {
        Nodo n = root;
        for (String parte : rutaActual) n = n.buscarHijo(parte);
        return n;
    }

    private File getRutaReal() {
        File f = new File(RAIZ_DISCO);
        for (String parte : rutaActual) f = new File(f, parte);
        return f;
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
        Nodo dir = getDirActual();
        if (dir.existeHijo(nombre))
            return new Resultado(false, "Ya existe: \"" + nombre + "\"");
        if (!new File(getRutaReal(), nombre).mkdirs())
            return new Resultado(false, "No se pudo crear la carpeta en disco.");
        dir.getHijos().add(new Nodo(nombre, Nodo.Tipo.DIR));
        return new Resultado(true, "Carpeta creada: " + nombre);
    }

    public Resultado mfile(String nombre) {
        if (!nombre.contains("."))
            return new Resultado(false, "El archivo necesita extension. Ej: notas.txt");
        Nodo dir = getDirActual();
        if (dir.existeHijo(nombre))
            return new Resultado(false, "Ya existe: \"" + nombre + "\"");
        try { new FileWriter(new File(getRutaReal(), nombre), false).close(); }
        catch (Exception e) { return new Resultado(false, "Error al crear archivo en disco."); }
        dir.getHijos().add(new Nodo(nombre, Nodo.Tipo.FILE));
        return new Resultado(true, "Archivo creado: " + nombre);
    }

    public Resultado rm(String nombre) {
        Nodo dir = getDirActual();
        if (!dir.existeHijo(nombre))
            return new Resultado(false, "No se encontro: \"" + nombre + "\"");
        borrarRecursivo(new File(getRutaReal(), nombre));
        dir.eliminarHijo(nombre);
        return new Resultado(true, "\"" + nombre + "\" eliminado.");
    }

    private void borrarRecursivo(File f) {
        if (f.isDirectory()) {
            File[] hijos = f.listFiles();
            if (hijos != null) for (File h : hijos) borrarRecursivo(h);
        }
        f.delete();
    }

    public Resultado cd(String nombre) {
        Nodo hijo = getDirActual().buscarHijo(nombre);
        if (hijo == null)
            return new Resultado(false, "No existe la carpeta: \"" + nombre + "\"");
        if (!hijo.esDir())
            return new Resultado(false, "\"" + nombre + "\" no es una carpeta.");
        rutaActual.add(hijo.getNombre());
        return new Resultado(true, "");
    }

    public Resultado regresar() {
        if (rutaActual.isEmpty())
            return new Resultado(false, "Ya esta en el directorio raiz C:\\");
        rutaActual.remove(rutaActual.size() - 1);
        return new Resultado(true, "");
    }

    public ListadoDir listar() {
        ListadoDir res = new ListadoDir();
        for (Nodo n : getDirActual().getHijos()) {
            if (n.esDir()) {
                res.carpetas.add(n.getNombre());
            } else {
                res.archivos.add(n.getNombre());
                res.tamanos.add(new File(getRutaReal(), n.getNombre()).length());
            }
        }
        return res;
    }

    public Resultado abrirParaEscritura(String nombre) {
        Nodo hijo = getDirActual().buscarHijo(nombre);
        if (hijo == null)
            return new Resultado(false, "El archivo \"" + nombre + "\" no existe. Use mfile primero.");
        if (!hijo.esFile())
            return new Resultado(false, "\"" + nombre + "\" no es un archivo.");
        return new Resultado(true, "ok", hijo.getContenido());
    }

    public Resultado escribirArchivo(String nombre, String contenido) {
        Nodo hijo = getDirActual().buscarHijo(nombre);
        if (hijo == null)
            return new Resultado(false, "El archivo \"" + nombre + "\" no existe.");
        try (FileWriter fw = new FileWriter(new File(getRutaReal(), nombre), false)) {
            fw.write(contenido);
        } catch (Exception e) {
            return new Resultado(false, "Error al guardar: " + e.getMessage());
        }
        hijo.setContenido(contenido);
        return new Resultado(true, "Archivo guardado: " + nombre + " (" + contenido.length() + " caracteres)");
    }

    public Resultado leerArchivo(String nombre) {
        Nodo hijo = getDirActual().buscarHijo(nombre);
        if (hijo == null)
            return new Resultado(false, "El archivo \"" + nombre + "\" no existe.");
        if (!hijo.esFile())
            return new Resultado(false, "\"" + nombre + "\" no es un archivo.");
        String contenido = leerDisco(new File(getRutaReal(), nombre));
        hijo.setContenido(contenido);
        return new Resultado(true, "ok", contenido);
    }
}