import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import manager.MenuManager;
import manager.OrderManager;
import ui.MainFrame;

/**
 * Launcher for the UB staff canteen desktop client (Swing + org.json).
 * <p>
 * Build (from project root, where {@code data/} lives):
 * {@code ./compile.sh} then {@code ./run.sh}, or:
 * {@code javac -encoding UTF-8 -cp lib/json-20231013.jar -d classes $(find src -name "*.java") && java -cp classes:lib/json-20231013.jar Main}
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException
                | IllegalAccessException | UnsupportedLookAndFeelException ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            MenuManager.getInstance().loadMenu();
            OrderManager.getInstance().loadOrders();
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
