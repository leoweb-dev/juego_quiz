import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

/** Ventana principal: cambia entre inicio, acceso, vista docente y vista estudiante. */
public class Ventana extends JFrame {

    private final Database db;

    public Ventana(Database db) {
        super("Preguntas y Respuestas");
        this.db = db;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 620));
        setSize(1020, 700);
        setLocationRelativeTo(null);
        mostrarInicio();
    }

    private void mostrar(JComponent panel) {
        setContentPane(panel);
        revalidate();
        repaint();
    }

    public void mostrarInicio() {
        mostrar(crearInicio());
    }

    public void mostrarAuth(boolean docente) {
        mostrar(new AuthPanel(this, db, docente));
    }

    public void mostrarDocente(Database.Usuario u) {
        mostrar(new DocentePanel(this, db, u));
    }

    public void mostrarEstudiante(Database.Usuario u) {
        mostrar(new EstudiantePanel(this, db, u));
    }

    private JPanel crearInicio() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.insets = new Insets(8, 8, 8, 8);

        g.gridy = 0;
        p.add(Ui.titulo("Preguntas y Respuestas", 38f), g);

        JLabel sub = new JLabel("¿Cómo quieres ingresar?", SwingConstants.CENTER);
        sub.setFont(sub.getFont().deriveFont(Font.PLAIN, 18f));
        g.gridy = 1;
        g.insets = new Insets(0, 8, 30, 8);
        p.add(sub, g);

        JPanel botones = new JPanel(new GridLayout(1, 2, 24, 0));
        JButton docente = new JButton("Soy docente");
        JButton estudiante = new JButton("Soy estudiante");
        for (JButton b : new JButton[]{docente, estudiante}) {
            b.setFont(b.getFont().deriveFont(Font.BOLD, 20f));
            b.setPreferredSize(new Dimension(240, 100));
            b.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            botones.add(b);
        }
        docente.addActionListener(e -> mostrarAuth(true));
        estudiante.addActionListener(e -> mostrarAuth(false));
        g.gridy = 2;
        p.add(botones, g);
        return p;
    }
}
