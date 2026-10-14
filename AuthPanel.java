import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** Inicio de sesión y registro (docentes o estudiantes). */
public class AuthPanel extends JPanel {

    private final JComboBox<String> cbGrupo = new JComboBox<>();
    private final Database db;

    public AuthPanel(Ventana ventana, Database db, boolean docente) {
        this.db = db;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // ---- barra superior
        JButton volver = new JButton("← Volver");
        volver.addActionListener(e -> ventana.mostrarInicio());
        JPanel top = new JPanel(new BorderLayout());
        top.add(volver, BorderLayout.WEST);
        javax.swing.JLabel titulo = Ui.titulo(docente ? "Acceso para docentes" : "Acceso para estudiantes", 26f);
        titulo.setHorizontalAlignment(SwingConstants.CENTER);
        top.add(titulo, BorderLayout.CENTER);
        top.add(javax.swing.Box.createHorizontalStrut(volver.getPreferredSize().width), BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        add(tabs, BorderLayout.CENTER);

        // ---- pestaña: iniciar sesión
        JTextField loginUsuario = new JTextField(20);
        JPasswordField loginClave = new JPasswordField(20);
        JButton entrar = new JButton("Entrar");

        JPanel formLogin = new JPanel(new GridBagLayout());
        Ui.fila(formLogin, 0, "Usuario:", loginUsuario);
        Ui.fila(formLogin, 1, "Contraseña:", loginClave);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 2;
        g.gridwidth = 2;
        g.insets = new Insets(16, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        formLogin.add(entrar, g);
        tabs.addTab("Iniciar sesión", centrar(formLogin));

        Runnable hacerLogin = () -> {
            Database.Usuario u = db.autenticar(docente, loginUsuario.getText(), new String(loginClave.getPassword()));
            if (u == null) {
                Ui.error(this, "Usuario o contraseña incorrectos.");
                loginClave.setText("");
                return;
            }
            if (docente) ventana.mostrarDocente(u);
            else ventana.mostrarEstudiante(u);
        };
        entrar.addActionListener(e -> hacerLogin.run());
        loginClave.addActionListener(e -> hacerLogin.run());
        loginUsuario.addActionListener(e -> loginClave.requestFocus());

        // ---- pestaña: registrarse
        JTextField regNombre = new JTextField(20);
        JTextField regApellido = new JTextField(20);
        JTextField regUsuario = new JTextField(20);
        JPasswordField regClave = new JPasswordField(20);
        cbGrupo.setEditable(true);
        JButton crear = new JButton("Crear cuenta");

        JPanel formReg = new JPanel(new GridBagLayout());
        int fila = 0;
        Ui.fila(formReg, fila++, "Nombre:", regNombre);
        Ui.fila(formReg, fila++, "Apellido:", regApellido);
        Ui.fila(formReg, fila++, "Usuario:", regUsuario);
        Ui.fila(formReg, fila++, "Contraseña:", regClave);
        if (!docente) Ui.fila(formReg, fila++, "Grupo:", cbGrupo);
        g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = fila;
        g.gridwidth = 2;
        g.insets = new Insets(16, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        formReg.add(crear, g);
        tabs.addTab("Registrarse", centrar(formReg));

        crear.addActionListener(e -> {
            try {
                String clave = new String(regClave.getPassword());
                if (docente) {
                    db.registrarDocente(regNombre.getText(), regApellido.getText(), regUsuario.getText(), clave);
                } else {
                    db.registrarEstudiante(regNombre.getText(), regApellido.getText(), regUsuario.getText(),
                            clave, Ui.texto(cbGrupo));
                }
                Ui.info(this, "Cuenta creada. Ya puedes iniciar sesión.");
                loginUsuario.setText(regUsuario.getText().trim());
                loginClave.setText("");
                regNombre.setText("");
                regApellido.setText("");
                regUsuario.setText("");
                regClave.setText("");
                tabs.setSelectedIndex(0);
                loginClave.requestFocus();
            } catch (RuntimeException ex) {
                Ui.error(this, ex.getMessage());
            }
        });

        // los grupos se actualizan cada vez que se abre la pestaña de registro
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 1) recargarGrupos();
        });
        recargarGrupos();
    }

    private void recargarGrupos() {
        cbGrupo.removeAllItems();
        for (String g : db.grupos()) cbGrupo.addItem(g);
        cbGrupo.setSelectedItem("");
    }

    private static JPanel centrar(JPanel form) {
        JPanel c = new JPanel(new GridBagLayout());
        c.add(form);
        return c;
    }
}
