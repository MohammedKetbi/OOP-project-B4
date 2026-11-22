package ui;
import java.awt.BorderLayout; //BorderLayout arranges components in five regions: NORTH, SOUTH, EAST, WEST, and CEBTER
import java.awt.GridLayout; //GridLayout arranges components in a rectangular grid of rows and columns
import java.awt.event.ActionEvent; //ActionEvent is generated when a button is clicked or an action occurs
import java.awt.event.ActionListener; //ActionListener is an interface for receiving action events (e.g., button clicks)
import java.awt.Color; //Color class is used to set the color of components in the UI


import javax.swing.BorderFactory; // Creates spacing and borders around components
import javax.swing.JButton; // JButton is a clickable button component
import javax.swing.JFrame; // JFrame is the main window container for the application
import javax.swing.JLabel; // JLabel is a component that displays text or images
import javax.swing.JPanel; // JPanel is a container that can hold other components and arrange them in a layout
import javax.swing.JPasswordField; // JPasswordField is a text field that hides the input for password entry
import javax.swing.JTextField; // JTextField is a single-line text input field



/* Displayes the login screen for both students and administrators */
public class LoginFrame extends JFrame implements ActionListener {

    private JTextField usernameField; // JTextField for entering the username (university ID)
    private JPasswordField passwordField; // JPasswordField for entering the password
    private JLabel messageLabel; // JLabel for displaying error messages or status messages to the user
    private JButton signInButton; // JButton for signing in
    private JButton registerButton; // JButton for opening the registration screen for new students
    private AccountActions accountActions; // Interface for handling account-related actions (sign-in and registration)

    // Constructor for the LoginFrame class. It takes an AccountActions object as a parameter, which is used to handle sign-in and registration actions. The constructor sets up the UI components, including labels, text fields, buttons, and panels, and arranges them in a layout. It also adds action listeners to the buttons and password field to handle user interactions.
    public LoginFrame(AccountActions accountActions) {
        this.accountActions = accountActions;

        setTitle("GPU Utilization Management"); // Set the title of the window
        setSize(450, 350); // Set the size of the window
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Set the default close operation to exit the application when the window is closed
        setLocationRelativeTo(null); // Center the window on the screen

        JPanel panel = new JPanel(new BorderLayout(10, 10)); // Create a main panel with a BorderLayout and set spacing between components
        panel.setBorder(BorderFactory.createEmptyBorder(25, 35, 25, 25)); // Add spacing around the panel

        JLabel titleLabel = new JLabel("GPU Utilization Management", JLabel.CENTER); // Create a title label and center the text
        panel.add(titleLabel, BorderLayout.NORTH); // Add the title label to the top (NORTH) of the panel

        JPanel fieldsPanel = new JPanel(new GridLayout(4, 1, 5, 4)); //Four rows, one column, 5px vertical gap, 4px horizontal gap

        fieldsPanel.add(new JLabel("Username (university ID)")); // Add a label for the username field
        usernameField = new JTextField(); // Create a text field for entering the username
        fieldsPanel.add(usernameField); // Add the username text field to the fields panel

        fieldsPanel.add(new JLabel("Password")); // Add a label for the password field
        passwordField = new JPasswordField(); // Create a password field for entering the password (input will be hidden)
        fieldsPanel.add(passwordField); // Add the password field to the fields panel

        panel.add(fieldsPanel, BorderLayout.CENTER); // Add the fields panel to the center of the main panel

        JPanel bottomPanel = new JPanel(new GridLayout(3, 1, 5, 8)); //Three rows, one column, 5px vertical gap, 8px horizontal gap

        signInButton = new JButton("Sign in"); // Create a button for signing in
        bottomPanel.add(signInButton); // Add the sign-in button to the bottom panel


        JPanel registrationPanel = new JPanel(); // Create a panel for the registration section (new student registration)
        registrationPanel.add(new JLabel("New Student?")); // Add a label indicating that this section is for new students

        registerButton = new JButton("<HTML><U>Request an account</U></HTML>"); // Create a button for requesting a new student account with underlined text
        registerButton.setForeground(new Color(32, 111, 177)); // Set the text color of the registration button to a specific shade of blue
        registerButton.setBorderPainted(false); // Remove the border of the registration button to make it look like a hyperlink
        registerButton.setContentAreaFilled(false); // Remove the background fill of the registration button to make it look like a hyperlink
        registerButton.setFocusPainted(false); // Remove the focus border of the registration button to make it look like a hyperlink
        registrationPanel.add(registerButton); // Add the registration button to the registration panel

        bottomPanel.add(registrationPanel); // Add the registration panel to the bottom panel

        messageLabel = new JLabel(" ", JLabel.CENTER); // Create a label for displaying messages (e.g., error messages) and center the text
        messageLabel.setOpaque(true); // Make the message label opaque so that the background color can be set
        messageLabel.setBackground(new java.awt.Color(250, 240, 230)); // Set the background color of the message label to a light shade of orange indicating an error or warning
        messageLabel.setForeground(new java.awt.Color(180, 75, 20)); // Set the text color of the message label to a darker shade for better readability
        bottomPanel.add(messageLabel); // Add the message label to the bottom panel

        panel.add(bottomPanel, BorderLayout.SOUTH); // Add the bottom panel (containing the sign-in button, registration section, and message label) to the bottom (SOUTH) of the main panel
        add(panel); // Add the main panel to the JFrame (the window)

        signInButton.addActionListener(this); // Add an action listener to the sign-in button to handle the sign-in action when clicked
        passwordField.addActionListener(this); // Add an action listener to the password field to handle the sign-in action when the Enter key is pressed while the password field is focused
        registerButton.addActionListener(event -> openRegistration()); // Add an action listener to the registration button to open the registration screen when clicked
    }

    @Override // This method is called when an action event occurs (e.g., button click or Enter key press). It handles the sign-in action by calling the signIn() method.
    public void actionPerformed(ActionEvent event) {
        signIn();
    }
    // This method handles the sign-in action. It retrieves the username and password entered by the user, validates the input, and calls the accountActions.signIn() method to perform the sign-in operation. If there are any validation errors or exceptions during sign-in, it displays an error message using the showLoginError() method.
    private void signIn() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        if (isBlank(username)) {
           showLoginError("Enter your username");
           usernameField.requestFocusInWindow();
            return;
        }

        if (isBlank(password)){
            showLoginError("Enter your password");
           passwordField.requestFocusInWindow();
            return;
        }
        messageLabel.setText(" "); // Clear any previous error messages

        try {
            // Call the signIn method of the accountActions interface to perform the sign-in operation with the provided username and password. If the sign-in is successful, clear the password field and close the login window (dispose()). If there is an exception (e.g., invalid credentials), catch it, clear the password field, display an error message using showLoginError(), and set focus back to the password field for user correction.
            accountActions.signIn(username, password);
            passwordField.setText("");
            dispose();
        } catch (IllegalArgumentException | IllegalStateException error) {
            // Clear the password field and display an error message if sign-in fails due to invalid credentials or other issues
            passwordField.setText("");
            showLoginError(error.getMessage());
            passwordField.requestFocusInWindow();
        }
    }
    // This method displays an error message in the messageLabel and also shows a dialog box with the same error message. It takes a string message as a parameter, which is the error message to be displayed. The messageLabel is updated with the error message, and the Dialog.showError() method is called to display a dialog box with the error message.
    private void showLoginError(String message) {
        messageLabel.setText(message);
        Dialog.showError(this, message);
    }

    // This method opens the registration screen by creating a new instance of the RegistrationFrame class and setting it to visible. It then disposes of the current login window.
    private void openRegistration() {
        RegistrationFrame registration = new RegistrationFrame(accountActions);
        registration.setVisible(true);
        dispose();
    }
    // This method checks if a given string is blank (null, empty, or contains only whitespace). It takes a string text as a parameter and returns true if the string is blank, and false otherwise. This method is used for input validation to ensure that the user has entered valid data in the username and password fields.
    private boolean isBlank(String text) {
        if (text == null || text.trim().isEmpty())  return true;
         return false;
        }
    // This method returns the text entered in the username field. It is used to retrieve the username for sign-in or registration purposes.
    public interface AccountActions {
        void signIn(String username, String password);
        void submitRegistration(String universityId, String fullName, String email, String phone, String password);
    }
}
