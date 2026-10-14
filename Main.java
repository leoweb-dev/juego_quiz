import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception ignorada) {
                // si Nimbus no está disponible se usa el aspecto por defecto
            }

            Database db;
            try {
                db = new Database(Paths.get("datos.json"));
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, e.getMessage(), "No se pudo abrir la base de datos",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            new Ventana(db).setVisible(true);
        });
    }
}
