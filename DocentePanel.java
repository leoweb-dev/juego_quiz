import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Vista del docente: crear/editar preguntas por grupo, ver estudiantes y resultados. */
public class DocentePanel extends JPanel {

    private static final String TODOS = "Todos los grupos";

    private final Database db;

    // --- pestaña Preguntas
    private final JComboBox<String> cbGrupo = new JComboBox<>();
    private final JTextArea txtEnunciado = new JTextArea(4, 28);
    private final JTextField[] txtOpcion = new JTextField[4];
    private final JRadioButton[] rbCorrecta = new JRadioButton[4];
    private final JComboBox<String> cbFiltro = new JComboBox<>();
    private final DefaultTableModel modeloPreguntas =
            Ui.modeloSoloLectura("Grupo", "Pregunta", "Respuesta correcta");
    private final JTable tablaPreguntas = new JTable(modeloPreguntas);
    private final JLabel lblModo = new JLabel("Nueva pregunta");
    private final JLabel lblEstado = new JLabel(" ");
    private List<Database.Pregunta> visibles = new ArrayList<>();
    private Long editandoId = null;
    private boolean refrescando = false;

    // --- pestañas Estudiantes y Resultados
    private final DefaultTableModel modeloEstudiantes =
            Ui.modeloSoloLectura("Grupo", "Apellido", "Nombre", "Usuario");
    private final DefaultTableModel modeloResultados =
            Ui.modeloSoloLectura("Fecha", "Estudiante", "Grupo", "Aciertos", "Total", "Porcentaje");
    private final JLabel lblEstudiantes = new JLabel(" ");

    public DocentePanel(Ventana ventana, Database db, Database.Usuario docente) {
        this.db = db;
        setLayout(new BorderLayout());

        JPanel barra = new JPanel(new BorderLayout());
        barra.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        barra.add(Ui.titulo("Docente: " + docente.nombreCompleto(), 20f), BorderLayout.WEST);
        JButton salir = new JButton("Cerrar sesión");
        salir.addActionListener(e -> ventana.mostrarInicio());
        barra.add(salir, BorderLayout.EAST);
        add(barra, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Preguntas", crearTabPreguntas());
        tabs.addTab("Estudiantes", crearTabEstudiantes());
        tabs.addTab("Resultados", crearTabResultados());
        tabs.addChangeListener(e -> refrescarTodo());
        add(tabs, BorderLayout.CENTER);

        refrescarTodo();
    }

    // ================================================================ pestañas

    private JComponent crearTabPreguntas() {
        // ---------- formulario (izquierda)
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        lblModo.setFont(lblModo.getFont().deriveFont(Font.BOLD, 16f));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 2;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(0, 6, 8, 6);
        form.add(lblModo, g);

        cbGrupo.setEditable(true);
        Ui.fila(form, 1, "Grupo:", cbGrupo);

        txtEnunciado.setLineWrap(true);
        txtEnunciado.setWrapStyleWord(true);
        Ui.fila(form, 2, "Pregunta:", new JScrollPane(txtEnunciado));

        ButtonGroup bg = new ButtonGroup();
        String[] letras = {"A", "B", "C", "D"};
        for (int i = 0; i < 4; i++) {
            txtOpcion[i] = new JTextField(24);
            rbCorrecta[i] = new JRadioButton();
            rbCorrecta[i].setToolTipText("Marcar como respuesta correcta");
            bg.add(rbCorrecta[i]);
            JPanel f = new JPanel(new BorderLayout(6, 0));
            f.add(rbCorrecta[i], BorderLayout.WEST);
            f.add(txtOpcion[i], BorderLayout.CENTER);
            Ui.fila(form, 3 + i, "Opción " + letras[i] + ":", f);
        }
        rbCorrecta[0].setSelected(true);

        JLabel ayuda = new JLabel(Ui.html(
                "Marca el círculo de la opción correcta. Deja vacías las opciones que no uses (mínimo 2).", 360));
        ayuda.setFont(ayuda.getFont().deriveFont(Font.ITALIC, 12f));
        g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 7;
        g.gridwidth = 2;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(2, 6, 8, 6);
        form.add(ayuda, g);

        JButton guardar = new JButton("Guardar pregunta");
        JButton nueva = new JButton("Nueva pregunta");
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.add(guardar);
        botones.add(nueva);
        g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 8;
        g.gridwidth = 2;
        g.anchor = GridBagConstraints.WEST;
        form.add(botones, g);

        g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 9;
        g.gridwidth = 2;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(8, 6, 0, 6);
        form.add(lblEstado, g);

        g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 10;
        g.weighty = 1;
        form.add(new JPanel(), g);

        guardar.addActionListener(e -> guardarPregunta());
        nueva.addActionListener(e -> {
            limpiarFormulario();
            lblEstado.setText(" ");
        });

        // ---------- lista (derecha)
        JPanel lista = new JPanel(new BorderLayout(0, 8));
        lista.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel filtro = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtro.add(new JLabel("Ver:"));
        filtro.add(cbFiltro);
        lista.add(filtro, BorderLayout.NORTH);

        tablaPreguntas.setRowHeight(24);
        tablaPreguntas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaPreguntas.getColumnModel().getColumn(0).setPreferredWidth(90);
        tablaPreguntas.getColumnModel().getColumn(1).setPreferredWidth(320);
        tablaPreguntas.getColumnModel().getColumn(2).setPreferredWidth(150);
        lista.add(new JScrollPane(tablaPreguntas), BorderLayout.CENTER);

        JButton eliminar = new JButton("Eliminar seleccionada");
        JPanel sur = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        sur.add(eliminar);
        lista.add(sur, BorderLayout.SOUTH);

        cbFiltro.addActionListener(e -> {
            if (!refrescando) refrescarPreguntas();
        });
        tablaPreguntas.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || refrescando) return;
            int fila = tablaPreguntas.getSelectedRow();
            if (fila >= 0 && fila < visibles.size()) cargarEnFormulario(visibles.get(fila));
        });
        eliminar.addActionListener(e -> eliminarSeleccionada());

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(form), lista);
        split.setResizeWeight(0.42);
        split.setDividerLocation(470);
        split.setBorder(null);
        return split;
    }

    private JComponent crearTabEstudiantes() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JTable t = new JTable(modeloEstudiantes);
        t.setRowHeight(24);
        JButton actualizar = new JButton("Actualizar");
        actualizar.addActionListener(e -> refrescarTodo());
        JPanel arriba = new JPanel(new BorderLayout());
        arriba.add(lblEstudiantes, BorderLayout.WEST);
        arriba.add(actualizar, BorderLayout.EAST);
        p.add(arriba, BorderLayout.NORTH);
        p.add(new JScrollPane(t), BorderLayout.CENTER);
        return p;
    }

    private JComponent crearTabResultados() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JTable t = new JTable(modeloResultados);
        t.setRowHeight(24);
        JButton actualizar = new JButton("Actualizar");
        actualizar.addActionListener(e -> refrescarTodo());
        JPanel arriba = new JPanel(new BorderLayout());
        arriba.add(new JLabel("Intentos de los estudiantes (más recientes primero)"), BorderLayout.WEST);
        arriba.add(actualizar, BorderLayout.EAST);
        p.add(arriba, BorderLayout.NORTH);
        p.add(new JScrollPane(t), BorderLayout.CENTER);
        return p;
    }

    // ============================================================== acciones

    private void guardarPregunta() {
        List<String> ops = new ArrayList<>();
        int correcta = -1;
        for (int i = 0; i < 4; i++) {
            String t = txtOpcion[i].getText().trim();
            if (t.isEmpty()) continue;
            if (rbCorrecta[i].isSelected()) correcta = ops.size();
            ops.add(t);
        }
        if (ops.size() < 2) {
            Ui.error(this, "Escribe al menos dos opciones de respuesta.");
            return;
        }
        if (correcta < 0) {
            Ui.error(this, "La opción marcada como correcta está vacía. Marca otra o escribe su texto.");
            return;
        }
        try {
            boolean nueva = editandoId == null;
            db.guardarPregunta(editandoId, txtEnunciado.getText(), ops, correcta, Ui.texto(cbGrupo));
            limpiarFormulario();
            refrescarGrupos();
            refrescarPreguntas();
            lblEstado.setText(nueva ? "Pregunta guardada." : "Pregunta actualizada.");
        } catch (RuntimeException ex) {
            Ui.error(this, ex.getMessage());
        }
    }

    private void eliminarSeleccionada() {
        int fila = tablaPreguntas.getSelectedRow();
        if (fila < 0 || fila >= visibles.size()) {
            Ui.error(this, "Selecciona una pregunta de la tabla.");
            return;
        }
        int r = JOptionPane.showConfirmDialog(this, "¿Eliminar esta pregunta?", "Confirmar",
                JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;
        try {
            long id = visibles.get(fila).id;
            db.eliminarPregunta(id);
            if (editandoId != null && editandoId == id) limpiarFormulario();
            refrescarGrupos();
            refrescarPreguntas();
            lblEstado.setText("Pregunta eliminada.");
        } catch (RuntimeException ex) {
            Ui.error(this, ex.getMessage());
        }
    }

    private void cargarEnFormulario(Database.Pregunta p) {
        editandoId = p.id;
        lblModo.setText("Editando pregunta");
        lblEstado.setText(" ");
        cbGrupo.setSelectedItem(p.grupo);
        txtEnunciado.setText(p.enunciado);
        for (int i = 0; i < 4; i++) txtOpcion[i].setText(i < p.opciones.size() ? p.opciones.get(i) : "");
        rbCorrecta[Math.min(p.correcta, 3)].setSelected(true);
    }

    private void limpiarFormulario() {
        editandoId = null;
        lblModo.setText("Nueva pregunta");
        txtEnunciado.setText("");
        for (JTextField t : txtOpcion) t.setText("");
        rbCorrecta[0].setSelected(true);
        refrescando = true;
        tablaPreguntas.clearSelection();
        refrescando = false;
    }

    // ============================================================= refrescos

    private void refrescarTodo() {
        try {
            db.recargar();
        } catch (IOException ex) {
            // se conservan los datos que ya estaban en memoria
        }
        refrescarGrupos();
        refrescarPreguntas();
        refrescarEstudiantes();
        refrescarResultados();
    }

    private void refrescarGrupos() {
        refrescando = true;
        try {
            String grupoActual = Ui.texto(cbGrupo);
            Object filtroActual = cbFiltro.getSelectedItem();
            List<String> grupos = db.grupos();
            cbGrupo.removeAllItems();
            cbFiltro.removeAllItems();
            cbFiltro.addItem(TODOS);
            for (String gr : grupos) {
                cbGrupo.addItem(gr);
                cbFiltro.addItem(gr);
            }
            cbGrupo.setSelectedItem(grupoActual);
            cbFiltro.setSelectedItem(filtroActual != null ? filtroActual : TODOS);
            if (cbFiltro.getSelectedItem() == null) cbFiltro.setSelectedItem(TODOS);
        } finally {
            refrescando = false;
        }
    }

    private void refrescarPreguntas() {
        refrescando = true;
        try {
            Object f = cbFiltro.getSelectedItem();
            visibles = new ArrayList<>();
            modeloPreguntas.setRowCount(0);
            for (Database.Pregunta p : db.preguntas()) {
                if (f != null && !TODOS.equals(f) && !p.grupo.equalsIgnoreCase(f.toString())) continue;
                visibles.add(p);
                modeloPreguntas.addRow(new Object[]{p.grupo, p.enunciado.replace('\n', ' '),
                        p.opciones.get(p.correcta)});
            }
            if (editandoId != null) {
                for (int i = 0; i < visibles.size(); i++) {
                    if (visibles.get(i).id == editandoId) tablaPreguntas.setRowSelectionInterval(i, i);
                }
            }
        } finally {
            refrescando = false;
        }
    }

    private void refrescarEstudiantes() {
        modeloEstudiantes.setRowCount(0);
        List<Database.Usuario> lista = db.estudiantes();
        for (Database.Usuario u : lista) {
            modeloEstudiantes.addRow(new Object[]{u.grupo, u.apellido, u.nombre, u.usuario});
        }
        lblEstudiantes.setText("Estudiantes registrados: " + lista.size());
    }

    private void refrescarResultados() {
        modeloResultados.setRowCount(0);
        for (Database.Resultado r : db.resultados()) {
            int pct = r.total > 0 ? Math.round(r.aciertos * 100f / r.total) : 0;
            modeloResultados.addRow(new Object[]{r.fecha, r.nombre, r.grupo, r.aciertos, r.total, pct + " %"});
        }
    }
}
