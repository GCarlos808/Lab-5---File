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
    private StyledDocument doc;
    private Procesos       processor;
    private String         promptActual   = "C:\\>";
    
    private int            limiteEntrada  = 0;
    
    public cmdGUI() {
        super("C:\\WINDOWS\\system32\\cmd.exe");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 520);
        setLocationRelativeTo(null);
        setResizable(true);
        
        ImageIcon icono = cargarIcono("/imagenes/cmd.png");
        if (icono != null) setIconImage(icono.getImage());
        
        construirUI();
    }
    
    public void setProcessor(Procesos processor) {
        this.processor = processor;
        mostrarBienvenida();
        imprimirPrompt();
        areaTerminal.requestFocusInWindow();
    }
    
    private void construirUI() {
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(BG_NEGRO);
        
        areaTerminal = new JTextPane();
        areaTerminal.setEditable(true);
        areaTerminal.setBackground(BG_NEGRO);
        areaTerminal.setForeground(COL_NORMAL);
        areaTerminal.setFont(FONT_MONO);
        areaTerminal.setMargin(new Insets(4, 6, 4, 6));
        areaTerminal.setCaretColor(COL_NORMAL);
        doc = areaTerminal.getStyledDocument();
        
        ((AbstractDocument) doc).setDocumentFilter(new DocumentFilter() {
            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                if (offset < limiteEntrada) {
                    int start = Math.max(offset, limiteEntrada);
                    int end   = offset + length;
                    if (end > start) super.remove(fb, start, end - start);
                    return;
                }
                super.remove(fb, offset, length);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (offset < limiteEntrada) {
                    int docEnd = fb.getDocument().getLength();
                    super.replace(fb, docEnd, 0, text, attrs);
                    return;
                }
                super.replace(fb, offset, length, text, attrs);
            }
            
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                if (offset < limiteEntrada) {
                    int docEnd = fb.getDocument().getLength();
                    super.insertString(fb, docEnd, string, attr);
                    return;
                }
                super.insertString(fb, offset, string, attr);
            }
        });
        
        areaTerminal.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int caret = areaTerminal.getCaretPosition();
                
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume();
                    
                    String todo    = areaTerminal.getText();
                    String comando = (doc.getLength() >= limiteEntrada)
                            ? todo.substring(limiteEntrada).trim()
                            : "";
                    appendTexto("\n", COL_NORMAL);
                    if (processor != null) processor.procesarLinea(comando);
                    if (!processor.isModoEscritura()) imprimirPrompt();
                    return;
                }
                
                if (e.getKeyCode() == KeyEvent.VK_LEFT && caret <= limiteEntrada) {
                    e.consume();
                    return;
                }
                
                if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN) {
                    e.consume();
                    return;
                }
                
                if (caret < limiteEntrada) {
                    areaTerminal.setCaretPosition(doc.getLength());
                }
            }
        });
        
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
    }
    
    private void imprimirPrompt() {
        appendTexto(promptActual, COL_PROMPT);
    }
    
    private void appendTexto(String texto, Color color) {
        SwingUtilities.invokeLater(() -> {
            try {
                Style s = doc.addStyle("s", null);
                StyleConstants.setFontFamily(s, "Consolas");
                StyleConstants.setFontSize(s, 14);
                StyleConstants.setForeground(s, color);
                doc.insertString(doc.getLength(), texto, s);
                // Update the editable limit AFTER inserting the prompt
                limiteEntrada = doc.getLength();
                areaTerminal.setCaretPosition(doc.getLength());
            } catch (BadLocationException ex) { ex.printStackTrace(); }
        });
    }
    
    @Override
    public void imprimir(String texto, Procesos.EstiloLinea estilo) {
        appendTexto(texto + "\n", resolverColor(estilo));
    }
    
    @Override
    public void limpiarPantalla() {
        SwingUtilities.invokeLater(() -> {
            try {
                doc.remove(0, doc.getLength());
                limiteEntrada = 0;
                imprimirPrompt();
            } catch (BadLocationException ex) { ex.printStackTrace(); }
        });
    }
    
    @Override
    public void actualizarPrompt(String prompt) {
        promptActual = prompt;
    }
    
    @Override
    public void setModoEscritura(boolean activo) {
        SwingUtilities.invokeLater(() ->
            areaTerminal.setCaretColor(activo ? COL_ESCRITURA : COL_NORMAL)
        );
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
        appendTexto("Microsoft Windows [Versi\u00F3n 10.0.19045.4651]\n", COL_NORMAL);
        appendTexto("(c) Microsoft Corporation. Todos los derechos reservados.\n\n", COL_NORMAL);
    }
}