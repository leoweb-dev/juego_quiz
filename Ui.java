import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;

/** Utilidades de interfaz compartidas. */
final class Ui {

    private Ui() {}

    static JLabel titulo(String texto, float tamano) {
        JLabel l = new JLabel(texto);
        l.setFont(l.getFont().deriveFont(Font.BOLD, tamano));
        return l;
    }

    /** Agrega una fila "etiqueta + campo" a un panel con GridBagLayout. */
    static void fila(JPanel panel, int fila, String etiqueta, JComponent campo) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = fila;
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.NORTHWEST;
        g.gridx = 0;
        panel.add(new JLabel(etiqueta), g);
        g.gridx = 1;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        panel.add(campo, g);
    }

    static void error(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "Atención", JOptionPane.WARNING_MESSAGE);
    }

    static void info(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "Listo", JOptionPane.INFORMATION_MESSAGE);
    }

    static DefaultTableModel modeloSoloLectura(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
    }

    /** Texto actual de un JComboBox editable (incluye lo que se escribió a mano). */
    static String texto(JComboBox<String> combo) {
        Object o = combo.getEditor().getItem();
        return o == null ? "" : o.toString().trim();
    }

    /** Escapa texto para usarlo dentro de HTML de Swing. */
    static String html(String s, int ancho) {
        String e = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
        return "<html><body style='width:" + ancho + "px'>" + e + "</body></html>";
    }
}
