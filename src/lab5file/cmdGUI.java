package lab5file;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;

public class cmdGUI extends JFrame implements Procesos.IGUICallback {

    private static final Color C_BG        = new Color(  0,   0,   0);
    private static final Color C_NORMAL    = new Color(192, 192, 192);
    private static final Color C_ERROR     = new Color(255,  85,  85);
    private static final Color C_EXITO     = new Color( 85, 255,  85);
    private static final Color C_INFO      = new Color( 85, 255, 255);
    private static final Color C_ENCAB     = new Color(255, 255, 255);
    private static final Color C_CARPETA   = new Color( 85, 255, 255);
    private static final Color C_ARCHIVO   = new Color(192, 192, 192);
    private static final Color C_PROMPT    = new Color(192, 192, 192);
    private static final Color C_ESCRITURA = new Color(255, 255,  85);

    private static final Font MONO = new Font("Consolas", Font.PLAIN, 14);

    private final JTextPane      terminal;
    private final StyledDocument doc;

    private Procesos processor;
    private String   promptActual  = "C:\\>";
    private int      limiteEntrada = 0;

    public cmdGUI() {
        super("C:\\WINDOWS\\system32\\cmd.exe");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(820, 530);
        setLocationRelativeTo(null);
        setResizable(true);

        ImageIcon ico = cargarIcono("/imagenes/cmd.png");
        if (ico != null) setIconImage(ico.getImage());

        terminal = new JTextPane();
        terminal.setEditable(true);
        terminal.setBackground(C_BG);
        terminal.setForeground(C_NORMAL);
        terminal.setFont(MONO);
        terminal.setMargin(new Insets(4, 8, 4, 8));
        terminal.setCaretColor(C_NORMAL);
        doc = terminal.getStyledDocument();

        instalarFiltroDocumento();
        instalarTecladoListener();

        JScrollPane scroll = new JScrollPane(terminal);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setBackground(C_BG);
        scroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = new Color(80, 80, 80);
                trackColor = new Color(15, 15, 15);
            }
            @Override protected JButton createDecreaseButton(int o) { return btnVacio(); }
            @Override protected JButton createIncreaseButton(int o) { return btnVacio(); }
            private JButton btnVacio() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
        });

        setLayout(new BorderLayout());
        add(scroll, BorderLayout.CENTER);
    }

    public void setProcessor(Procesos processor) {
        this.processor = processor;
        SwingUtilities.invokeLater(() -> {
            insertarTexto("Microsoft Windows [Version 10.0.19045.4651]\n", C_NORMAL);
            insertarTexto("(c) Microsoft Corporation. Todos los derechos reservados.\n\n", C_NORMAL);
            insertarPrompt();
            terminal.requestFocusInWindow();
        });
    }

    private void insertarTexto(String texto, Color color) {
        try {
            Style s = doc.addStyle("tmp", null);
            StyleConstants.setFontFamily(s, "Consolas");
            StyleConstants.setFontSize(s, 14);
            StyleConstants.setForeground(s, color);
            doc.insertString(doc.getLength(), texto, s);
            limiteEntrada = doc.getLength();
            terminal.setCaretPosition(limiteEntrada);
        } catch (BadLocationException e) { e.printStackTrace(); }
    }

    private void insertarPrompt() {
        insertarTexto(promptActual, C_PROMPT);
    }

    private void instalarFiltroDocumento() {
        ((AbstractDocument) doc).setDocumentFilter(new DocumentFilter() {

            private Style estiloUsuario() {
                Style s = doc.addStyle("usr", null);
                StyleConstants.setFontFamily(s, "Consolas");
                StyleConstants.setFontSize(s, 14);
                StyleConstants.setForeground(s, C_NORMAL);
                return s;
            }

            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                if (offset < limiteEntrada) {
                    int desde = Math.max(offset, limiteEntrada);
                    int hasta = offset + length;
                    if (hasta > desde) super.remove(fb, desde, hasta - desde);
                    return;
                }
                super.remove(fb, offset, length);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (offset < limiteEntrada) {
                    super.replace(fb, fb.getDocument().getLength(), 0, text, estiloUsuario());
                    return;
                }
                super.replace(fb, offset, length, text, estiloUsuario());
            }

            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                if (offset < limiteEntrada) {
                    super.insertString(fb, fb.getDocument().getLength(), string, estiloUsuario());
                    return;
                }
                super.insertString(fb, offset, string, estiloUsuario());
            }
        });
    }

    private void instalarTecladoListener() {
        terminal.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume();

                    // Leer SOLO el texto escrito por el usuario desde limiteEntrada
                    String comando = "";
                    try {
                        int longitud = doc.getLength() - limiteEntrada;
                        if (longitud > 0) {
                            comando = doc.getText(limiteEntrada, longitud).trim();
                        }
                    } catch (BadLocationException ex) { ex.printStackTrace(); }

                    final String cmdFinal = comando;

                    SwingUtilities.invokeLater(() -> {
                        try {
                            Style s = doc.addStyle("nl", null);
                            StyleConstants.setFontFamily(s, "Consolas");
                            StyleConstants.setFontSize(s, 14);
                            StyleConstants.setForeground(s, C_NORMAL);
                            doc.insertString(doc.getLength(), "\n", s);
                            limiteEntrada = doc.getLength();
                        } catch (BadLocationException ex) { ex.printStackTrace(); }

                        if (processor != null) processor.procesarLinea(cmdFinal);

                        SwingUtilities.invokeLater(() -> {
                            if (processor != null && !processor.isModoEscritura()) {
                                insertarPrompt();
                            }
                        });
                    });
                    return;
                }

                if (e.getKeyCode() == KeyEvent.VK_LEFT && terminal.getCaretPosition() <= limiteEntrada) {
                    e.consume();
                    return;
                }

                if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN) {
                    e.consume();
                    return;
                }

                if (terminal.getCaretPosition() < limiteEntrada) {
                    terminal.setCaretPosition(doc.getLength());
                }
            }
        });
    }

    @Override
    public void imprimir(String texto, Procesos.EstiloLinea estilo) {
        SwingUtilities.invokeLater(() -> insertarTexto(texto + "\n", colorDeEstilo(estilo)));
    }

    @Override
    public void limpiarPantalla() {
        SwingUtilities.invokeLater(() -> {
            try {
                doc.remove(0, doc.getLength());
                limiteEntrada = 0;
                insertarPrompt();
            } catch (BadLocationException e) { e.printStackTrace(); }
        });
    }

    @Override
    public void actualizarPrompt(String prompt) {
        promptActual = prompt;
    }

    @Override
    public void setModoEscritura(boolean activo) {
        SwingUtilities.invokeLater(() -> terminal.setCaretColor(activo ? C_ESCRITURA : C_NORMAL));
    }

    private Color colorDeEstilo(Procesos.EstiloLinea estilo) {
        switch (estilo) {
            case ERROR:       return C_ERROR;
            case EXITO:       return C_EXITO;
            case INFO:        return C_INFO;
            case ENCABEZADO:  return C_ENCAB;
            case DIR_CARPETA: return C_CARPETA;
            case DIR_ARCHIVO: return C_ARCHIVO;
            case ESCRITURA:   return C_ESCRITURA;
            default:          return C_NORMAL;
        }
    }

    private ImageIcon cargarIcono(String ruta) {
        try {
            URL url = getClass().getResource(ruta);
            if (url != null) return new ImageIcon(url);
            java.io.File f = new java.io.File("." + ruta);
            if (f.exists()) return new ImageIcon(f.getAbsolutePath());
        } catch (Exception ignored) {}
        return null;
    }
}