package lab5file;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Procesos {

    public interface IGUICallback {
        void imprimir(String texto, EstiloLinea estilo);
        void limpiarPantalla();
        void actualizarPrompt(String prompt);
        void setModoEscritura(boolean activo);
        void mostrarAyuda();
    }

    public enum EstiloLinea {
        NORMAL, ERROR, EXITO, INFO, ENCABEZADO,
        DIR_CARPETA, DIR_ARCHIVO, PROMPT_ECHO, ESCRITURA
    }

    private static final String CHARS_INVALIDOS = "/\\:*?\"<>|";

    private final SystemFile   fs;
    private final IGUICallback gui;

    private boolean       modoEscritura   = false;
    private String        archivoDestino  = null;
    private StringBuilder bufferEscritura = new StringBuilder();

    public Procesos(SystemFile fs, IGUICallback gui) {
        this.fs  = fs;
        this.gui = gui;
    }

    public boolean isModoEscritura() { return modoEscritura; }

    public void procesarLinea(String lineaRaw) {
        if (modoEscritura) {
            gui.imprimir("  " + lineaRaw, EstiloLinea.ESCRITURA);
            if (lineaRaw.equals("EXIT")) {
                SystemFile.Resultado r = fs.escribirArchivo(archivoDestino, bufferEscritura.toString());
                gui.imprimir("\u2500".repeat(50), EstiloLinea.ESCRITURA);
                gui.imprimir((r.ok ? "\u2714 " : "\u2716 ") + r.mensaje, r.ok ? EstiloLinea.EXITO : EstiloLinea.ERROR);
                gui.imprimir("", EstiloLinea.NORMAL);
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
        String cmd     = (espacio == -1 ? trimmed : trimmed.substring(0, espacio)).toLowerCase();
        String args    = (espacio == -1 ? "" : trimmed.substring(espacio + 1)).trim();

        despachar(cmd, args);
    }

    private void despachar(String cmd, String args) {
        switch (cmd) {
            case "mkdir":  cmdMkdir(args);        break;
            case "mfile":  cmdMfile(args);        break;
            case "rm":     cmdRm(args);           break;
            case "cd":     cmdCd(args);           break;
            case "<...>":  cmdCd("..");           break;
            case "dir":    cmdDir();              break;
            case "date":   gui.imprimir("Fecha actual: " + new SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", new java.util.Locale("es","ES")).format(new Date()), EstiloLinea.INFO); break;
            case "time":   gui.imprimir("Hora actual: " + new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()), EstiloLinea.INFO); break;
            case "wr":     cmdWr(args);           break;
            case "rd":     cmdRd(args);           break;
            case "echo":   cmdEcho(args);         break;
            case "count":  cmdCount(args);        break;
            case "cls":    gui.limpiarPantalla(); break;
            case "help":   gui.mostrarAyuda();    break;
            default:
                gui.imprimir("'" + cmd + "' no se reconoce como comando.", EstiloLinea.ERROR);
        }
    }

    private boolean requireArgs(String args, String uso) {
        if (args.isEmpty()) {
            gui.imprimir("Uso: " + uso, EstiloLinea.ERROR);
            return false;
        }
        return true;
    }

    private boolean nombreValido(String nombre) {
        for (char c : CHARS_INVALIDOS.toCharArray()) {
            if (nombre.indexOf(c) != -1) {
                gui.imprimir("Nombre invalido. No se permiten: " + CHARS_INVALIDOS, EstiloLinea.ERROR);
                return false;
            }
        }
        return true;
    }

    private void cmdMkdir(String args) {
        if (!requireArgs(args, "Mkdir <nombre>")) return;
        if (!nombreValido(args)) return;
        SystemFile.Resultado r = fs.mkdir(args);
        gui.imprimir(r.mensaje, r.ok ? EstiloLinea.EXITO : EstiloLinea.ERROR);
    }

    private void cmdMfile(String args) {
        if (!requireArgs(args, "Mfile <nombre.ext>")) return;
        if (!nombreValido(args)) return;
        SystemFile.Resultado r = fs.mfile(args);
        gui.imprimir(r.mensaje, r.ok ? EstiloLinea.EXITO : EstiloLinea.ERROR);
    }

    private void cmdRm(String args) {
        if (!requireArgs(args, "Rm <nombre>")) return;
        SystemFile.Resultado r = fs.rm(args);
        gui.imprimir(r.mensaje, r.ok ? EstiloLinea.EXITO : EstiloLinea.ERROR);
    }

    private void cmdCd(String args) {
        if (!requireArgs(args, "Cd <carpeta>")) return;
        SystemFile.Resultado r = args.equals("..") ? fs.regresar() : fs.cd(args);
        if (r.ok) gui.actualizarPrompt(fs.getPrompt());
        gui.imprimir(r.mensaje, r.ok ? EstiloLinea.INFO : EstiloLinea.ERROR);
    }

    private void cmdDir() {
        SystemFile.ListadoDir listado = fs.listar();
        gui.imprimir("", EstiloLinea.NORMAL);
        gui.imprimir(" Directorio de " + fs.getRutaString(), EstiloLinea.ENCABEZADO);
        gui.imprimir("\u2500".repeat(50), EstiloLinea.NORMAL);
        if (listado.carpetas.isEmpty() && listado.archivos.isEmpty()) {
            gui.imprimir("  (Vacio)", EstiloLinea.INFO);
        } else {
            for (String c : listado.carpetas)
                gui.imprimir("  <DIR>          " + c, EstiloLinea.DIR_CARPETA);
            for (int i = 0; i < listado.archivos.size(); i++)
                gui.imprimir(String.format("  %14d bytes  %s", listado.tamanos.get(i), listado.archivos.get(i)), EstiloLinea.DIR_ARCHIVO);
            gui.imprimir("\u2500".repeat(50), EstiloLinea.NORMAL);
            gui.imprimir("  " + listado.carpetas.size() + " carpeta(s)   " + listado.archivos.size() + " archivo(s)", EstiloLinea.INFO);
        }
        gui.imprimir("", EstiloLinea.NORMAL);
    }

    private void cmdWr(String args) {
        if (!requireArgs(args, "Wr <archivo.ext>")) return;
        SystemFile.Resultado r = fs.abrirParaEscritura(args);
        if (!r.ok) { gui.imprimir(r.mensaje, EstiloLinea.ERROR); return; }

        modoEscritura   = true;
        archivoDestino  = args;
        bufferEscritura = new StringBuilder((String) r.datos);

        gui.imprimir("", EstiloLinea.NORMAL);
        gui.imprimir("FileWriter archivo: " + args, EstiloLinea.ESCRITURA);
        gui.imprimir("  Ingrese el texto. Escriba EXIT para guardar y salir.", EstiloLinea.ESCRITURA);
        gui.imprimir("\u2500".repeat(50), EstiloLinea.ESCRITURA);
        gui.setModoEscritura(true);
    }

    private void cmdRd(String args) {
        if (!requireArgs(args, "Rd <archivo.ext>")) return;
        SystemFile.Resultado r = fs.leerArchivo(args);
        if (!r.ok) { gui.imprimir(r.mensaje, EstiloLinea.ERROR); return; }

        String contenido = (String) r.datos;
        gui.imprimir("", EstiloLinea.NORMAL);
        gui.imprimir("FileReader contenido de: " + args, EstiloLinea.ENCABEZADO);
        gui.imprimir("\u2500".repeat(50), EstiloLinea.NORMAL);
        if (contenido == null || contenido.isBlank()) {
            gui.imprimir("  (Archivo vacio)", EstiloLinea.INFO);
        } else {
            for (String linea : contenido.split("\n"))
                gui.imprimir("  " + linea, EstiloLinea.NORMAL);
        }
        gui.imprimir("\u2500".repeat(50), EstiloLinea.NORMAL);
        gui.imprimir("  " + (contenido != null ? contenido.length() : 0) + " caracter(es)", EstiloLinea.INFO);
        gui.imprimir("", EstiloLinea.NORMAL);
    }

    private void cmdEcho(String args) {
        if (!requireArgs(args, "Echo <texto>")) return;
        gui.imprimir(args, EstiloLinea.NORMAL);
    }

    private void cmdCount(String args) {
        if (!requireArgs(args, "Count <archivo.ext>")) return;
        SystemFile.Resultado r = fs.leerArchivo(args);
        if (!r.ok) { gui.imprimir(r.mensaje, EstiloLinea.ERROR); return; }

        String contenido = (String) r.datos;

        if (contenido == null || contenido.isBlank()) {
            gui.imprimir("\"" + args + "\" esta vacio (0 lineas, 0 palabras, 0 caracteres).", EstiloLinea.INFO);
            return;
        }

        String[] lineas   = contenido.split("\n");
        int numLineas     = lineas.length;
        int numPalabras   = 0;
        int numCaracteres = contenido.length();

        for (String linea : lineas)
            if (!linea.trim().isEmpty())
                numPalabras += linea.trim().split("\\s+").length;

        gui.imprimir("", EstiloLinea.NORMAL);
        gui.imprimir(" Estadisticas de: " + args, EstiloLinea.ENCABEZADO);
        gui.imprimir("\u2500".repeat(50), EstiloLinea.NORMAL);
        gui.imprimir("  Lineas:     " + numLineas,     EstiloLinea.INFO);
        gui.imprimir("  Palabras:   " + numPalabras,   EstiloLinea.INFO);
        gui.imprimir("  Caracteres: " + numCaracteres, EstiloLinea.INFO);
        gui.imprimir("\u2500".repeat(50), EstiloLinea.NORMAL);
        gui.imprimir("", EstiloLinea.NORMAL);
    }

    private void salirModoEscritura() {
        modoEscritura   = false;
        archivoDestino  = null;
        bufferEscritura = new StringBuilder();
        gui.setModoEscritura(false);
    }
}