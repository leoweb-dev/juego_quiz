import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Vista del estudiante: juega con las preguntas que el docente asignó a su grupo. */
public class EstudiantePanel extends JPanel {

    private static final Color VERDE = new Color(0x1B7F3B);
    private static final Color ROJO = new Color(0xB3261E);

    private final Database db;
    private final Database.Usuario est;

    private final CardLayout cartas = new CardLayout();
    private final JPanel centro = new JPanel(cartas);

    // pantalla de inicio
    private final JLabel lblIntro = new JLabel("", SwingConstants.CENTER);
    private final JButton btnComenzar = new JButton("Comenzar");
    private final DefaultTableModel modeloHistorial =
            Ui.modeloSoloLectura("Fecha", "Aciertos", "Total", "Porcentaje");

    // pantalla de juego
    private final JLabel lblProgreso = new JLabel();
    private final JLabel lblPuntaje = new JLabel();
    private final JTextArea txtPregunta = new JTextArea();
    private final JRadioButton[] rb = new JRadioButton[4];
    private final ButtonGroup grupoRb = new ButtonGroup();
    private final JLabel lblFeedback = new JLabel(" ");
    private final JButton btnAccion = new JButton("Responder");

    // pantalla final
    private final JLabel lblFin = new JLabel("", SwingConstants.CENTER);
    private final JLabel lblFinDetalle = new JLabel("", SwingConstants.CENTER);

    // estado de la partida
    private List<Database.Pregunta> ronda = new ArrayList<>();
    private List<List<Integer>> ordenes = new ArrayList<>(); // orden en que se muestran las opciones
    private int indice;
    private int aciertos;
    private boolean respondida;

    public EstudiantePanel(Ventana ventana, Database db, Database.Usuario estudiante) {
        this.db = db;
        this.est = estudiante;
        setLayout(new BorderLayout());

        JPanel barra = new JPanel(new BorderLayout());
        barra.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        barra.add(Ui.titulo("Estudiante: " + est.nombreCompleto() + "   |   Grupo: " + est.grupo, 20f),
                BorderLayout.WEST);
        JButton salir = new JButton("Cerrar sesión");
        salir.addActionListener(e -> ventana.mostrarInicio());
        barra.add(salir, BorderLayout.EAST);
        add(barra, BorderLayout.NORTH);

        centro.add(crearIntro(), "intro");
        centro.add(crearJuego(), "juego");
        centro.add(crearFin(), "fin");
        add(centro, BorderLayout.CENTER);

        mostrarIntro();
    }

    // ============================================================ pantallas

    private JPanel crearIntro() {
        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setBorder(BorderFactory.createEmptyBorder(30, 60, 30, 60));

        lblIntro.setFont(lblIntro.getFont().deriveFont(Font.PLAIN, 20f));
        btnComenzar.setFont(btnComenzar.getFont().deriveFont(Font.BOLD, 20f));
        btnComenzar.addActionListener(e -> comenzar());

        JPanel arriba = new JPanel(new GridLayout(2, 1, 0, 14));
        arriba.add(lblIntro);
        JPanel boton = new JPanel();
        boton.add(btnComenzar);
        arriba.add(boton);
        p.add(arriba, BorderLayout.NORTH);

        JTable t = new JTable(modeloHistorial);
        t.setRowHeight(24);
        JPanel hist = new JPanel(new BorderLayout(0, 6));
        hist.add(new JLabel("Tus intentos anteriores"), BorderLayout.NORTH);
        hist.add(new JScrollPane(t), BorderLayout.CENTER);
        p.add(hist, BorderLayout.CENTER);
        return p;
    }

    private JPanel crearJuego() {
        JPanel p = new JPanel(new BorderLayout(0, 18));
        p.setBorder(BorderFactory.createEmptyBorder(24, 60, 24, 60));

        lblProgreso.setFont(lblProgreso.getFont().deriveFont(Font.BOLD, 16f));
        lblPuntaje.setFont(lblPuntaje.getFont().deriveFont(Font.BOLD, 16f));
        JPanel top = new JPanel(new BorderLayout());
        top.add(lblProgreso, BorderLayout.WEST);
        top.add(lblPuntaje, BorderLayout.EAST);

        txtPregunta.setEditable(false);
        txtPregunta.setLineWrap(true);
        txtPregunta.setWrapStyleWord(true);
        txtPregunta.setFocusable(false);
        txtPregunta.setOpaque(false);
        txtPregunta.setFont(txtPregunta.getFont().deriveFont(Font.BOLD, 22f));
        JScrollPane sp = new JScrollPane(txtPregunta);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setPreferredSize(new Dimension(100, 120));

        JPanel arriba = new JPanel(new BorderLayout(0, 14));
        arriba.add(top, BorderLayout.NORTH);
        arriba.add(sp, BorderLayout.CENTER);
        p.add(arriba, BorderLayout.NORTH);

        JPanel opciones = new JPanel(new GridLayout(4, 1, 0, 10));
        for (int i = 0; i < 4; i++) {
            rb[i] = new JRadioButton();
            rb[i].setFont(rb[i].getFont().deriveFont(Font.PLAIN, 17f));
            grupoRb.add(rb[i]);
            opciones.add(rb[i]);
        }
        p.add(opciones, BorderLayout.CENTER);

        lblFeedback.setFont(lblFeedback.getFont().deriveFont(Font.BOLD, 16f));
        btnAccion.setFont(btnAccion.getFont().deriveFont(Font.BOLD, 16f));
        btnAccion.addActionListener(e -> accion());
        JPanel sur = new JPanel(new BorderLayout(12, 0));
        sur.add(lblFeedback, BorderLayout.CENTER);
        sur.add(btnAccion, BorderLayout.EAST);
        p.add(sur, BorderLayout.SOUTH);
        return p;
    }

    private JPanel crearFin() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.insets = new Insets(8, 8, 8, 8);

        lblFin.setFont(lblFin.getFont().deriveFont(Font.BOLD, 36f));
        lblFinDetalle.setFont(lblFinDetalle.getFont().deriveFont(Font.PLAIN, 24f));
        g.gridy = 0;
        p.add(lblFin, g);
        g.gridy = 1;
        p.add(lblFinDetalle, g);

        JButton otra = new JButton("Jugar de nuevo");
        JButton volver = new JButton("Volver al inicio");
        otra.addActionListener(e -> comenzar());
        volver.addActionListener(e -> mostrarIntro());
        JPanel botones = new JPanel(new GridLayout(1, 2, 16, 0));
        botones.add(otra);
        botones.add(volver);
        g.gridy = 2;
        g.insets = new Insets(24, 8, 8, 8);
        p.add(botones, g);
        return p;
    }

    // ============================================================== lógica

    private void mostrarIntro() {
        try {
            db.recargar();
        } catch (IOException ex) {
            // se usan los datos en memoria
        }
        int n = db.preguntasDeGrupo(est.grupo).size();
        if (n == 0) {
            lblIntro.setText("Tu docente aún no ha asignado preguntas al grupo " + est.grupo + ".");
            btnComenzar.setEnabled(false);
        } else {
            lblIntro.setText(n == 1 ? "Hay 1 pregunta para tu grupo." : "Hay " + n + " preguntas para tu grupo.");
            btnComenzar.setEnabled(true);
        }
        modeloHistorial.setRowCount(0);
        for (Database.Resultado r : db.resultados()) {
            if (!r.usuario.equalsIgnoreCase(est.usuario)) continue;
            modeloHistorial.addRow(new Object[]{r.fecha, r.aciertos, r.total, porcentaje(r.aciertos, r.total) + " %"});
        }
        cartas.show(centro, "intro");
    }

    private void comenzar() {
        try {
            db.recargar();
        } catch (IOException ex) {
            // se usan los datos en memoria
        }
        ronda = new ArrayList<>(db.preguntasDeGrupo(est.grupo));
        if (ronda.isEmpty()) {
            mostrarIntro();
            return;
        }
        Collections.shuffle(ronda);
        ordenes = new ArrayList<>();
        for (Database.Pregunta p : ronda) {
            List<Integer> orden = new ArrayList<>();
            for (int i = 0; i < p.opciones.size(); i++) orden.add(i);
            Collections.shuffle(orden);
            ordenes.add(orden);
        }
        indice = 0;
        aciertos = 0;
        mostrarPregunta();
        cartas.show(centro, "juego");
    }

    private void mostrarPregunta() {
        Database.Pregunta p = ronda.get(indice);
        List<Integer> orden = ordenes.get(indice);
        respondida = false;

        lblProgreso.setText("Pregunta " + (indice + 1) + " de " + ronda.size());
        lblPuntaje.setText("Aciertos: " + aciertos);
        txtPregunta.setText(p.enunciado);
        txtPregunta.setCaretPosition(0);

        grupoRb.clearSelection();
        for (int i = 0; i < 4; i++) {
            if (i < orden.size()) {
                rb[i].setText(Ui.html(p.opciones.get(orden.get(i)), 700));
                rb[i].setEnabled(true);
                rb[i].setVisible(true);
            } else {
                rb[i].setVisible(false);
            }
        }
        lblFeedback.setText(" ");
        lblFeedback.setForeground(null);
        btnAccion.setText("Responder");
    }

    private void accion() {
        if (!respondida) {
            Database.Pregunta p = ronda.get(indice);
            List<Integer> orden = ordenes.get(indice);
            int elegida = -1;
            for (int i = 0; i < orden.size(); i++) if (rb[i].isSelected()) elegida = i;
            if (elegida < 0) {
                lblFeedback.setForeground(ROJO);
                lblFeedback.setText("Selecciona una respuesta.");
                return;
            }
            int correctaVisual = orden.indexOf(p.correcta);
            if (elegida == correctaVisual) {
                aciertos++;
                lblFeedback.setForeground(VERDE);
                lblFeedback.setText("¡Correcto!");
            } else {
                lblFeedback.setForeground(ROJO);
                lblFeedback.setText(Ui.html("Incorrecto. La respuesta correcta era: " + p.opciones.get(p.correcta), 600));
            }
            for (int i = 0; i < 4; i++) rb[i].setEnabled(false);
            lblPuntaje.setText("Aciertos: " + aciertos);
            respondida = true;
            btnAccion.setText(indice == ronda.size() - 1 ? "Ver resultado" : "Siguiente");
        } else if (indice == ronda.size() - 1) {
            terminar();
        } else {
            indice++;
            mostrarPregunta();
        }
    }

    private void terminar() {
        try {
            db.guardarResultado(est, aciertos, ronda.size());
        } catch (RuntimeException ex) {
            Ui.error(this, "No se pudo guardar tu resultado: " + ex.getMessage());
        }
        int pct = porcentaje(aciertos, ronda.size());
        lblFin.setText(pct >= 60 ? "¡Buen trabajo!" : "¡Sigue practicando!");
        lblFinDetalle.setText("Acertaste " + aciertos + " de " + ronda.size() + " (" + pct + " %)");
        cartas.show(centro, "fin");
    }

    private static int porcentaje(int aciertos, int total) {
        return total > 0 ? Math.round(aciertos * 100f / total) : 0;
    }
}
