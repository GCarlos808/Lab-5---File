package lab5file;

import java.text.SimpleDateFormat;
import java.util.*;

public class Procesos {

    public interface IGUICallback {
        void imprimir(String texto, EstiloLinea estilo);
        void limpiarPantalla();
        void actualizarPrompt(String prompt);
        void setModoEscritura(boolean activo);
    }

    public enum EstiloLinea {
        NORMAL, ERROR, EXITO, INFO, ENCABEZADO,
        DIR_CARPETA, DIR_ARCHIVO, ESCRITURA
    }

    private final SystemFile   fs;
    private final IGUICallback gui;

    private boolean       modoEscritura  = false;
    private String        archivoDestino = null;
    private StringBuilder bufferEscritura;

    public Procesos(SystemFile fs, IGUICallback gui) {
        this.fs  = fs;
        this.gui = gui;
    }

    public boolean isModoEscritura() { return modoEscritura; }

    public void procesarLinea(String lineaRaw) {
        if (modoEscritura) {
            if (lineaRaw.trim().equals("EXIT")) {
                SystemFile.Resultado r = fs.escribirArchivo(archivoDestino, bufferEscritura.toString());
                gui.imprimir("\u2500".repeat(40), EstiloLinea.ESCRITURA);
                gui.imprimir(r.ok ? "\u2714 " + r.mensaje : "\u2716 " + r.mensaje,
                             r.ok ? EstiloLinea.EXITO : EstiloLinea.ERROR);
                salirModoEscritura();
            } else {
                if (bufferEscritura.length() > 0) bufferEscritura.append("\n");
                bufferEscritura.append(lineaRaw);
            }
            return;
        }

        String trimmed = lineaRaw.trim();
        if (trimmed.isEmpty()) return;

        int    espacio = trimmed.indexOf(' ');
        String cmd     = (espacio < 0 ? trimmed : trimmed.substring(0, espacio)).toLowerCase();
        String args    = (espacio < 0 ? "" : trimmed.substring(espacio + 1)).trim();

        ejecutar(cmd, args);
    }

    private void ejecutar(String cmd, String args) {
        switch (cmd) {
            case "mkdir": cmdMkdir(args); break;
            case "mfile": cmdMfile(args); break;
            case "rm":    cmdRm(args);    break;
            case "cd":    cmdCd(args);    break;
            case "..":    cmdSubir();     break;
            case "dir":   cmdDir();       break;
            case "date":  cmdDate();      break;
            case "time":  cmdTime();      break;
            case "wr":    cmdWr(args);    break;
            case "rd":    cmdRd(args);    break;
            case "cls":   gui.limpiarPantalla(); break;
            case "help":  cmdHelp();      break;
            default:
                gui.imprimir("'" + cmd + "' no se reconoce como comando. Use HELP.", EstiloLinea.ERROR);
        }
    }

    private void cmdMkdir(String args) {
        if (args.isEmpty()) { gui.imprimir("Uso: mkdir <nombre>", EstiloLinea.ERROR); return; }
        SystemFile.Resultado r = fs.mkdir(args);
        gui.imprimir(r.mensaje, r.ok ? EstiloLinea.EXITO : EstiloLinea.ERROR);
    }

    private void cmdMfile(String args) {
        if (args.isEmpty()) { gui.imprimir("Uso: mfile <nombre.ext>", EstiloLinea.ERROR); return; }
        SystemFile.Resultado r = fs.mfile(args);
        gui.imprimir(r.mensaje, r.ok ? EstiloLinea.EXITO : EstiloLinea.ERROR);
    }

    private void cmdRm(String args) {
        if (args.isEmpty()) { gui.imprimir("Uso: rm <nombre>", EstiloLinea.ERROR); return; }
        SystemFile.Resultado r = fs.rm(args);
        gui.imprimir(r.mensaje, r.ok ? EstiloLinea.EXITO : EstiloLinea.ERROR);
    }

    private void cmdCd(String args) {
        if (args.isEmpty()) { gui.imprimir("Uso: cd <carpeta>", EstiloLinea.ERROR); return; }
        if (args.equals("..")) { cmdSubir(); return; }
        SystemFile.Resultado r = fs.cd(args);
        if (r.ok) gui.actualizarPrompt(fs.getPrompt());
        else      gui.imprimir(r.mensaje, EstiloLinea.ERROR);
    }

    private void cmdSubir() {
        SystemFile.Resultado r = fs.regresar();
        if (r.ok) gui.actualizarPrompt(fs.getPrompt());
        else      gui.imprimir(r.mensaje, EstiloLinea.ERROR);
    }

    private void cmdDir() {
        SystemFile.ListadoDir ld = fs.listar();
        gui.imprimir("", EstiloLinea.NORMAL);
        gui.imprimir(" Directorio: " + fs.getRutaString(), EstiloLinea.ENCABEZADO);
        gui.imprimir("\u2500".repeat(48), EstiloLinea.NORMAL);
        if (ld.carpetas.isEmpty() && ld.archivos.isEmpty()) {
            gui.imprimir("  (vacio)", EstiloLinea.INFO);
        } else {
            for (String c : ld.carpetas)
                gui.imprimir("  <DIR>          " + c, EstiloLinea.DIR_CARPETA);
            for (int i = 0; i < ld.archivos.size(); i++)
                gui.imprimir(String.format("  %14d  %s", ld.tamanos.get(i), ld.archivos.get(i)),
                             EstiloLinea.DIR_ARCHIVO);
            gui.imprimir("\u2500".repeat(48), EstiloLinea.NORMAL);
            gui.imprimir("  " + ld.carpetas.size() + " carpeta(s)   " + ld.archivos.size() + " archivo(s)",
                         EstiloLinea.INFO);
        }
        gui.imprimir("", EstiloLinea.NORMAL);
    }

    private void cmdDate() {
        gui.imprimir("Fecha actual: " + new SimpleDateFormat(
                "EEEE, dd 'de' MMMM 'de' yyyy", new Locale("es", "ES")).format(new Date()),
                EstiloLinea.INFO);
    }

    private void cmdTime() {
        gui.imprimir("Hora actual:  " + new SimpleDateFormat("HH:mm:ss").format(new Date()),
                EstiloLinea.INFO);
    }

    private void cmdWr(String args) {
        if (args.isEmpty()) { gui.imprimir("Uso: wr <archivo.ext>", EstiloLinea.ERROR); return; }
        SystemFile.Resultado r = fs.abrirParaEscritura(args);
        if (!r.ok) { gui.imprimir(r.mensaje, EstiloLinea.ERROR); return; }
        modoEscritura   = true;
        archivoDestino  = args;
        bufferEscritura = new StringBuilder();
        gui.imprimir("", EstiloLinea.NORMAL);
        gui.imprimir("FileWriter -> " + args, EstiloLinea.ESCRITURA);
        gui.imprimir("  Ingrese texto. Escriba EXIT para guardar y salir.", EstiloLinea.ESCRITURA);
        gui.imprimir("\u2500".repeat(40), EstiloLinea.ESCRITURA);
        gui.setModoEscritura(true);
    }

    private void cmdRd(String args) {
        if (args.isEmpty()) { gui.imprimir("Uso: rd <archivo.ext>", EstiloLinea.ERROR); return; }
        SystemFile.Resultado r = fs.leerArchivo(args);
        if (!r.ok) { gui.imprimir(r.mensaje, EstiloLinea.ERROR); return; }
        String contenido = (String) r.datos;
        gui.imprimir("", EstiloLinea.NORMAL);
        gui.imprimir("FileReader -> " + args, EstiloLinea.ENCABEZADO);
        gui.imprimir("\u2500".repeat(40), EstiloLinea.NORMAL);
        if (contenido == null || contenido.isBlank()) {
            gui.imprimir("  (archivo vacio)", EstiloLinea.INFO);
        } else {
            for (String linea : contenido.split("\n", -1))
                gui.imprimir("  " + linea, EstiloLinea.NORMAL);
        }
        gui.imprimir("\u2500".repeat(40), EstiloLinea.NORMAL);
        gui.imprimir("  " + (contenido != null ? contenido.length() : 0) + " caracter(es)", EstiloLinea.INFO);
        gui.imprimir("", EstiloLinea.NORMAL);
    }

    private void cmdHelp() {
        gui.imprimir("", EstiloLinea.NORMAL);
        gui.imprimir("  Comandos disponibles:", EstiloLinea.ENCABEZADO);
        gui.imprimir("\u2500".repeat(48), EstiloLinea.NORMAL);
        String[][] tabla = {
            {"mkdir <nombre>",      "Crear nueva carpeta"},
            {"mfile <nombre.ext>",  "Crear nuevo archivo"},
            {"rm    <nombre>",      "Eliminar archivo o carpeta"},
            {"cd    <carpeta>",     "Entrar a una carpeta"},
            {"..   ",               "Subir al directorio anterior"},
            {"dir",                 "Listar contenido del directorio"},
            {"date",                "Mostrar fecha actual"},
            {"time",                "Mostrar hora actual"},
            {"wr    <archivo.ext>", "Escribir en un archivo (EXIT para guardar)"},
            {"rd    <archivo.ext>", "Leer contenido de un archivo"},
            {"cls",                 "Limpiar la pantalla"},
            {"help",                "Mostrar esta ayuda"},
        };
        for (String[] fila : tabla)
            gui.imprimir(String.format("  %-22s  %s", fila[0], fila[1]), EstiloLinea.INFO);
        gui.imprimir("\u2500".repeat(48), EstiloLinea.NORMAL);
        gui.imprimir("", EstiloLinea.NORMAL);
    }

    private void salirModoEscritura() {
        modoEscritura   = false;
        archivoDestino  = null;
        bufferEscritura = null;
        gui.setModoEscritura(false);
    }
}