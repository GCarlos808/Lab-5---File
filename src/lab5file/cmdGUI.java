package lab5file;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;

public class cmdGUI extends JFrame implements Procesos.IGUICallback {

    private static final Color BG_NEGRO      = new Color(0,   0,   0);
    private static final Color COL_NORMAL    = new Color(192, 192, 192);
    private static final Color COL_ERROR     = new Color(255,  85,  85);
    private static final Color COL_EXITO     = new Color( 85, 255,  85);
    private static final Color COL_INFO      = new Color( 85, 255, 255);
    private static final Color COL_ENCAB     = new Color(255, 255, 255);
    private static final Color COL_CARPETA   = new Color( 85, 255, 255);
    private static final Color COL_ARCHIVO   = new Color(192, 192, 192);
    private static final Color COL_PROMPT    = new Color(192, 192, 192);
    private static final Color COL_ESCRITURA = new Color(255, 255,  85);

    private static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 14);

    private JTextPane      areaTerminal;
    private JTextField     campoInput;
    private JLabel         labelPrompt;
    private JLabel         labelEstado;
    private StyledDocument doc;
    private Procesos       processor;

    public cmdGUI() {
        super("C:\\WINDOWS\\system32\\cmd.exe");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 520);
        setLocationRelativeTo(null);
        setResizable(true);

        ImageIcon icono = cargarIcono("/imagenes/cmd.png");
        if (icono != null) setIconImage(icono.getImage());

        construirUI();
        configurarEventos();
    }

    public void setProcessor(Procesos processor) {
        this.processor = processor;
        actualizarPrompt("C:\\>");
        mostrarBienvenida();
        campoInput.requestFocusInWindow();
    }

    private void construirUI() {
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(BG_NEGRO);

        areaTerminal = new JTextPane();
        areaTerminal.setEditable(false);
        areaTerminal.setBackground(BG_NEGRO);
        areaTerminal.setForeground(COL_NORMAL);
        areaTerminal.setFont(FONT_MONO);
        areaTerminal.setMargin(new Insets(4, 6, 4, 6));
        doc = areaTerminal.getStyledDocument();

        JScrollPane scroll = new JScrollPane(areaTerminal);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setBackground(BG_NEGRO);
        scroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = new Color(100, 100, 100);
                trackColor = new Color(20,  20,  20);
            }
            @Override protected JButton createDecreaseButton(int o) { return miniBtn(); }
            @Override protected JButton createIncreaseButton(int o) { return miniBtn(); }
            private JButton miniBtn() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
        });

        add(scroll, BorderLayout.CENTER);
        add(crearFilaInput(), BorderLayout.SOUTH);
    }

    private JPanel crearFilaInput() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_NEGRO);
        panel.setBorder(BorderFactory.createEmptyBorder(2, 6, 4, 6));

        labelPrompt = new JLabel("C:\\>");
        labelPrompt.setFont(FONT_MONO);
        labelPrompt.setForeground(COL_PROMPT);

        campoInput = new JTextField();
        campoInput.setFont(FONT_MONO);
        campoInput.setBackground(BG_NEGRO);
        campoInput.setForeground(COL_NORMAL);
        campoInput.setCaretColor(COL_NORMAL);
        campoInput.setBorder(BorderFactory.createEmptyBorder());

        labelEstado = new JLabel("[ ESCRITURA - escriba EXIT para guardar ]");
        labelEstado.setFont(new Font("Consolas", Font.PLAIN, 11));
        labelEstado.setForeground(COL_ESCRITURA);
        labelEstado.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        labelEstado.setVisible(false);

        panel.add(labelPrompt, BorderLayout.WEST);
        panel.add(campoInput,  BorderLayout.CENTER);
        panel.add(labelEstado, BorderLayout.EAST);
        return panel;
    }

    private void configurarEventos() {
        campoInput.addActionListener(e -> {
            String linea = campoInput.getText();
            campoInput.setText("");
            if (processor != null) processor.procesarLinea(linea);
        });
    }

    @Override
    public void imprimir(String texto, Procesos.EstiloLinea estilo) {
        SwingUtilities.invokeLater(() -> {
            try {
                Style s = doc.addStyle("s", null);
                StyleConstants.setFontFamily(s, "Consolas");
                StyleConstants.setFontSize(s, 14);
                StyleConstants.setForeground(s, resolverColor(estilo));
                doc.insertString(doc.getLength(), texto + "\n", s);
                areaTerminal.setCaretPosition(doc.getLength());
            } catch (BadLocationException ex) { ex.printStackTrace(); }
        });
    }

    @Override
    public void limpiarPantalla() {
        SwingUtilities.invokeLater(() -> {
            try { doc.remove(0, doc.getLength()); }
            catch (BadLocationException ex) { ex.printStackTrace(); }
        });
    }

    @Override
    public void actualizarPrompt(String prompt) {
        SwingUtilities.invokeLater(() -> labelPrompt.setText(prompt));
    }

    @Override
    public void setModoEscritura(boolean activo) {
        SwingUtilities.invokeLater(() -> {
            Color c = activo ? COL_ESCRITURA : COL_NORMAL;
            campoInput.setForeground(c);
            campoInput.setCaretColor(c);
            labelEstado.setVisible(activo);
        });
    }

    @Override
    public void mostrarAyuda() {}

    private Color resolverColor(Procesos.EstiloLinea estilo) {
        switch (estilo) {
            case ERROR:       return COL_ERROR;
            case EXITO:       return COL_EXITO;
            case INFO:        return COL_INFO;
            case ENCABEZADO:  return COL_ENCAB;
            case DIR_CARPETA: return COL_CARPETA;
            case DIR_ARCHIVO: return COL_ARCHIVO;
            case PROMPT_ECHO: return COL_PROMPT;
            case ESCRITURA:   return COL_ESCRITURA;
            default:          return COL_NORMAL;
        }
    }

    private ImageIcon cargarIcono(String ruta) {
        try {
            URL url = getClass().getResource(ruta);
            if (url != null) return new ImageIcon(url);
            java.io.File f = new java.io.File("." + ruta);
            if (f.exists()) return new ImageIcon(f.getAbsolutePath());
            f = new java.io.File(ruta);
            if (f.exists()) return new ImageIcon(f.getAbsolutePath());
        } catch (Exception ignored) {}
        return null;
    }

    private void mostrarBienvenida() {
        imprimir("Microsoft Windows [Versi\u00F3n 10.0.19045.4651]", Procesos.EstiloLinea.NORMAL);
        imprimir("(c) Microsoft Corporation. Todos los derechos reservados.", Procesos.EstiloLinea.NORMAL);
        imprimir("", Procesos.EstiloLinea.NORMAL);
    }
}