package lab5file;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Lab5File {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            SystemFile fs        = new SystemFile();
            cmdGUI     gui       = new cmdGUI();
            Procesos   processor = new Procesos(fs, gui);
            gui.setProcessor(processor);
            gui.setVisible(true);
        });
    }
}