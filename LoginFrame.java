import java.awt.BorderLayout; //AWTprovides layouts and button event types used by swing
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory; // Swing provides the window, text, fields, buttons, labels, and dialogs
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class LoginFrame extends JFrame implements ActionListener {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel messageLabel;
    private JButton signInButton;
    private JButton registerButton;
    private AccountActions accountActions;

    public LoginFrame(AccountActions accountActions) {
        this.accountActions = accountActions;

        setTitle("GPU Utilization Management");
        setSize(450, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(25, 35, 25, 25));

        JLabel titleLabel = new JLabel("GPU Utilization Management", JLabel.CENTER);
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel fieldsPanel = new JPanel(new GridLayout(4, 1, 5, 4));

        fieldsPanel.add(new JLabel("Username (university ID)"));
        usernameField = new JTextField();
        fieldsPanel.add(usernameField);

        fieldsPanel.add(new JLabel("Password"));
        passwordField = new JPasswordField();
        fieldsPanel.add(passwordField);

        panel.add(fieldsPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new GridLayout(3, 1, 5, 8));

        signInButton = new JButton("Sign in");
        bottomPanel.add(signInButton);


        JPanel registrationPanel = new JPanel();
        registrationPanel.add(new JLabel("New Student?"));

        registerButton = new JButton("<HTML><U>Request an account</U></HTML>");
        registerButton.setForeground(new Color(32, 111, 177));
        registerButton.setBorderPainted(false);
        registerButton.setContentAreaFilled(false);
        registerButton.setFocusPainted(false);
        registrationPanel.add(registerButton);

        bottomPanel.add(registrationPanel);

        messageLabel = new JLabel(" ", JLabel.CENTER);
        messageLabel.setOpaque(true);
        messageLabel.setBackground(new java.awt.Color(250, 240, 230));
         messageLabel.setForeground(new java.awt.Color(180, 75, 20));
        bottomPanel.add(messageLabel);

        panel.add(bottomPanel, BorderLayout.SOUTH);
        add(panel);

        signInButton.addActionListener(this);
        passwordField.addActionListener(this);
        registerButton.addActionListener(event -> openRegistration());
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        signIn();
    }

    private void signIn() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        if (isBlank(username) || isBlank(password)) {
            messageLabel.setText("Enter your username and password");
            return;
        }

        try {
            accountActions.signIn(username, password);

            dispose();
        } catch (IllegalArgumentException error) {
            messageLabel.setText(error.getMessage());
            passwordField.setText("");
        }
    }

    private void openRegistration() {
        RegistrationFrame registration = new RegistrationFrame(accountActions);
        registration.setVisible(true);
        dispose();
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

    public interface AccountActions {
        void signIn(String username, String password);
        void submitRegistration(String universityId, String fullName, String email, String phone, String password);
    }
}
