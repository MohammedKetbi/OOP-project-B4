package ui;
import java.awt.Component; // Component lets the dialog box be displayed in the center of the parent window
import javax.swing.JOptionPane; // provides a dialog box for displaying messages to the user


/* A class (for validation) for displaying dialog boxes in the UI for displaying error messages */
public class Dialog {
    //Takes a parent component and a message string as parameters and displays an error dialog box with the specified message. The dialog box is displayed in the center of the parent component, and it has the title "ERROR" and uses a warning message icon.
    public static void showError(Component parent, String message){
        JOptionPane.showMessageDialog(parent, message, "ERROR", JOptionPane.WARNING_MESSAGE);

    }
    
}
