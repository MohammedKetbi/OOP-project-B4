import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import UI.*;


public class test{
    public static void main(String[] args){
        SwingUtilities.invokeLater(() ->
        { LoginFrame.AccountActions screenTest = new LoginFrame.AccountActions() {
    @Override 
    public void signIn(String username, String password){
        if (username.equals("admin") && password.equals("gpu2026")){
            JOptionPane.showMessageDialog(null, "Admin login successful");
        }
        else {throw new IllegalArgumentException("Incorrect username or password");

        }
    }

    
    @Override 
    public void submitRegistration(String universityId, String fullName, String emailString, String phone, String password){
        JOptionPane.showMessageDialog(null, "Registration submitted for " + fullName + ". (Test Only; not saved)");
    }};
    RegistrationFrame registration = new RegistrationFrame(screenTest);
    registration.setVisible(true);
    });
}}