package ui;
import java.awt.Component;
import javax.swing.JOptionPane;

public class Dialog {
    public static void showError(Component parent, String message){
        JOptionPane.showMessageDialog(parent, message, "ERROR", JOptionPane.WARNING_MESSAGE);

    }
    
}
