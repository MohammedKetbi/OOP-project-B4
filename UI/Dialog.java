package UI;
import java.awt.Component;
import javax.swing.JOptionPane;

public class Dialog {
    public static void showError(Component parent, String message){
        JOptionPane.showMessageDialog(parent, message, "Sign in unseccessful", javax.swing.JOptionPane.WARNING_MESSAGE);

    }
    
}
