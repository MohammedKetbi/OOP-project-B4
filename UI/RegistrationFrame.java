package UI;
import java.awt.BorderLayout; //AWTprovides layouts and button event types used by swing
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Color;

import javax.swing.BorderFactory; // Swing provides the window, text, fields, buttons, labels, and dialogs
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class RegistrationFrame extends JFrame implements ActionListener {
    private JTextField universityIdField;
    private JTextField fullNameField;
    private JTextField emailField;
    private JTextField phoneField;
    private JPasswordField passwordField;

    private JLabel messageLabel;
    private JButton submitButton;
    private JButton backButton;

    private LoginFrame.AccountActions accountActions;
    public RegistrationFrame(LoginFrame.AccountActions accountActions) {
        this.accountActions =accountActions;

        setTitle("Request a student account");
        setSize(650, 460);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JPanel headingPanel = new JPanel(new GridLayout(2, 1, 2, 2));

        JLabel titleLabel = new JLabel("Request a student account");
        titleLabel.setFont(titleLabel.getFont().deriveFont(20f));
        headingPanel.add(titleLabel);

        JLabel instructionLabel = new JLabel("An administrator must approve your request before you can sign in");
        headingPanel.add(instructionLabel);

        panel.add(headingPanel, BorderLayout.NORTH);

        JPanel fieldsPanel = new JPanel(new GridBagLayout());

        
        universityIdField = new JTextField(20);
        fullNameField = new JTextField(20);
        emailField = new JTextField(20);
        phoneField = new JTextField(20);
         passwordField = new JPasswordField(20);

        addFormItem(fieldsPanel, new JLabel("University ID"), 0, 0);
        addFormItem(fieldsPanel, new JLabel("Full Name"), 1, 0);

        addFormItem(fieldsPanel, universityIdField, 0, 1);
        addFormItem(fieldsPanel, fullNameField, 1, 1);

        addFormItem(fieldsPanel, new JLabel("Email"), 0, 2);
        addFormItem(fieldsPanel, new JLabel("Phone Number"), 1, 2);

        addFormItem(fieldsPanel, emailField, 0, 3);
        addFormItem(fieldsPanel, phoneField, 1, 3);

        addFormItem(fieldsPanel, new JLabel("Password"), 0, 4);
        addFormItem(fieldsPanel, passwordField, 0, 5);

        panel.add(fieldsPanel, BorderLayout.CENTER);

        
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 8));

        messageLabel = new JLabel(" ", JLabel.CENTER);
        messageLabel.setOpaque(true);
        messageLabel.setBackground(new Color(255, 243, 238));
         messageLabel.setForeground(new Color(0, 110, 94));
         messageLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        bottomPanel.add(messageLabel, BorderLayout.NORTH);

        JPanel buttonsPanel = new JPanel();
        submitButton = new JButton("Submit request");
        backButton = new JButton("Back to login");

        buttonsPanel.add(submitButton);
        buttonsPanel.add(backButton);

        bottomPanel.add(buttonsPanel, BorderLayout.SOUTH);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        add(panel); 

        submitButton.addActionListener(this);
        backButton.addActionListener(this);
}


private void addFormItem(JPanel panel, java.awt.Component component, int column, int row){
    GridBagConstraints position = new GridBagConstraints();
    position.gridx = column;
    position.gridy = row;
    position.fill = GridBagConstraints.HORIZONTAL;
    position.anchor = GridBagConstraints.WEST;
    position.weightx = 1;
    position.insets = new Insets(5, 5, 5, 5);
    panel.add(component, position);
}
@Override 
public void actionPerformed(ActionEvent event){
    if (event.getSource() == submitButton) submitRequest();
    else if (event.getSource() == backButton) returnToLogin();
}
private void submitRequest(){
    String universityId = universityIdField.getText();
    String fullName = fullNameField.getText();
    String email = emailField.getText();
    String phone = phoneField.getText();
    String password = new String(passwordField.getPassword());

    messageLabel.setForeground(Color.RED);
    messageLabel.setBackground(new Color(255, 238, 235));

    if(isBlank(universityId) || isBlank(fullName) || isBlank(email) || isBlank(phone) || isBlank(password)){
        messageLabel.setText("Please complete all five fields");
        return;
    }
    if (!email.contains("@") || !email.contains(".")) {
        messageLabel.setText("Enter a valid email address.");
        return;
    }

    if (!phone.matches("\\+?[0-9]{7,15}")) {
        messageLabel.setText("Enter 7 to 15 digits for the phone number");
        return;
    }
    try{
        accountActions.submitRegistration(universityId, fullName, email, phone, password);
        messageLabel.setForeground(new Color(0, 110, 94));
        messageLabel.setBackground(new Color(225, 243, 238));
        messageLabel.setText("Request sent. Status: Pending");

        JOptionPane.showMessageDialog(this, "Your request was sent, wait for administrator approval");

    } catch (IllegalArgumentException error) {
        messageLabel.setText(error.getMessage());
    }
}
    private boolean isBlank(String text) {
        if (text == null || text.isEmpty()) {
            return true;
        }
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isWhitespace(text.charAt(i))){
            return false;
        }
    }
        return true;
    }
    private void returnToLogin(){
        LoginFrame login = new LoginFrame(accountActions);
        login.setVisible(true);
        dispose();
    }
}
