package ui;
import java.awt.BorderLayout; //BorderLayout arranges components in five regions: NORTH, SOUTH, EAST, WEST, and CEBTER
import java.awt.GridLayout; //GridLayout arranges components in a rectangular grid of rows and columns
import java.awt.event.ActionEvent; //ActionEvent is generated when a button is clicked or an action occurs
import java.awt.event.ActionListener; //ActionListener is an interface for receiving action events (e.g., button clicks)
import java.awt.GridBagConstraints; // GridBagConstraints specifies constraints for components that are laid out using the GridBagLayout manager
import java.awt.GridBagLayout; //GridBagLayout arranges components in a flexible grid of rows and columns, allowing for varying sizes and alignments
import java.awt.Insets; //Insets specifies the space between components in a container
import java.awt.Color; //Color class is used to set the color of components in the UI

import javax.swing.BorderFactory; // Swing provides the window, text, fields, buttons, labels, and dialogs
import javax.swing.JButton; // JButton is a button component that can be clicked to trigger an action
import javax.swing.JFrame; // JFrame is a top-level window with a title and a border that can contain other components
import javax.swing.JLabel; // JLabel is a display area for a short text string or an image, or both
import javax.swing.JOptionPane; // JOptionPane is a class that provides standard dialog boxes for user input and messages
import javax.swing.JPanel; // JPanel is a generic container that can hold a group of components
import javax.swing.JPasswordField; // JPasswordField is a text field that hides the input characters for password entry
import javax.swing.JTextField; // JTextField is a text field that allows the user to enter a single line of text


// This class represents the registration frame in the user interface. It allows users to request a student account by providing their university ID, full name, email, phone number, and password. The frame includes input fields, labels, buttons, and validation logic for user input. It also handles the submission of the registration request and displays appropriate messages based on the success or failure of the operation.
public class RegistrationFrame extends JFrame implements ActionListener {
    private JTextField universityIdField; // JTextField is a text field that allows the user to enter a single line of text
    private JTextField fullNameField; // JTextField is a text field that allows the user to enter a single line of text
    private JTextField emailField; // JTextField is a text field that allows the user to enter a single line of text
    private JTextField phoneField; // JTextField is a text field that allows the user to enter a single line of text
    private JPasswordField passwordField; // JPasswordField is a text field that hides the input characters for password entry

    private JLabel messageLabel; // JLabel is a display area for a short text string or an image, or both
    private JButton submitButton; // JButton is a button component that can be clicked to trigger an action
    private JButton backButton; // JButton is a button component that can be clicked to trigger an action

    private LoginFrame.AccountActions accountActions; // AccountActions is an interface that defines the actions related to user accounts, such as signing in and submitting registration requests. It is used to handle the interaction between the registration frame and the underlying application logic.

    // Constructor for the RegistrationFrame class. It initializes the frame with the specified account actions and sets up the user interface components, including labels, input fields, buttons, and layout. It also adds action listeners to the buttons to handle user interactions.
    public RegistrationFrame(LoginFrame.AccountActions accountActions) {
        this.accountActions =accountActions;

        setTitle("Request a student account"); // Set the title of the frame
        setSize(650, 460); // Set the size of the frame
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Set the default close operation to exit the application when the frame is closed
        setLocationRelativeTo(null); // Center the frame on the screen

        JPanel panel = new JPanel(new BorderLayout(10, 10)); // Create a main panel with a BorderLayout to arrange components in five regions: NORTH, SOUTH, EAST, WEST, and CENTER
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30)); // Set an empty border around the panel with specified insets (top, left, bottom, right) to create padding between the panel and its contents

        JPanel headingPanel = new JPanel(new GridLayout(2, 1, 2, 2)); // Create a heading panel with a GridLayout to arrange components in a grid of 2 rows and 1 column, with specified horizontal and vertical gaps between components

        JLabel titleLabel = new JLabel("Request a student account"); // Create a title label with the specified text
        titleLabel.setFont(titleLabel.getFont().deriveFont(20f)); // Set the font size of the title label to 20 points
        headingPanel.add(titleLabel); // Add the title label to the heading panel

        JLabel instructionLabel = new JLabel("An administrator must approve your request before you can sign in"); // Create an instruction label with the specified text

        panel.add(headingPanel, BorderLayout.NORTH); // Add the heading panel to the NORTH region of the main panel

        JPanel fieldsPanel = new JPanel(new GridBagLayout()); // Create a fields panel with a GridBagLayout to arrange components in a flexible grid of rows and columns, allowing for varying sizes and alignments

        
        universityIdField = new JTextField(20); // JTextField is a text field that allows the user to enter a single line of text, in this case, the university ID field with a width of 20 columns
        fullNameField = new JTextField(20); // JTextField is a text field that allows the user to enter a single line of text, in this case, the full name field with a width of 20 columns
        emailField = new JTextField(20); // JTextField is a text field that allows the user to enter a single line of text, in this case, the email field with a width of 20 columns
        phoneField = new JTextField(20); // JTextField is a text field that allows the user to enter a single line of text, in this case, the phone number field with a width of 20 columns
         passwordField = new JPasswordField(20); // JPasswordField is a password field that allows the user to enter a password, in this case, the password field with a width of 20 columns

        addFormItem(fieldsPanel, new JLabel("University ID"), 0, 0); // Add a label for the university ID field to the fields panel at column 0, row 0
        addFormItem(fieldsPanel, new JLabel("Full Name"), 1, 0); // Add a label for the full name field to the fields panel at column 1, row 0

        addFormItem(fieldsPanel, universityIdField, 0, 1); // Add the university ID text field to the fields panel at column 0, row 1
        addFormItem(fieldsPanel, fullNameField, 1, 1); // Add the full name text field to the fields panel at column 1, row 1

        addFormItem(fieldsPanel, new JLabel("Email"), 0, 2); // Add a label for the email field to the fields panel at column 0, row 2
        addFormItem(fieldsPanel, new JLabel("Phone Number"), 1, 2); // Add a label for the phone number field to the fields panel at column 1, row 2

        addFormItem(fieldsPanel, emailField, 0, 3); // Add the email text field to the fields panel at column 0, row 3
        addFormItem(fieldsPanel, phoneField, 1, 3); // Add the phone number text field to the fields panel at column 1, row 3
        addFormItem(fieldsPanel, new JLabel("Password"), 0, 4); // Add a label for the password field to the fields panel at column 0, row 4
        addFormItem(fieldsPanel, passwordField, 0, 5); // Add the password field to the fields panel at column 0, row 5

        panel.add(fieldsPanel, BorderLayout.CENTER); // Add the fields panel to the CENTER region of the main panel

        
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 8)); // Create a bottom panel with a BorderLayout to arrange components in five regions: NORTH, SOUTH, EAST, WEST, and CENTER, with specified horizontal and vertical gaps between components

        messageLabel = new JLabel(" ", JLabel.CENTER); // Create a message label with an initial empty text and center alignment to display messages to the user, such as validation errors or success messages
        messageLabel.setOpaque(true); // Set the message label to be opaque, allowing its background color to be visible
        messageLabel.setBackground(new Color(255, 243, 238)); // Set the background color of the message label to a light red color to indicate an error or warning message
         messageLabel.setForeground(new Color(0, 110, 94)); // Set the foreground color of the message label to a dark green color to indicate a success message
         messageLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8)); // Set an empty border around the message label with specified insets (top, left, bottom, right) to create padding between the label and its contents
        bottomPanel.add(messageLabel, BorderLayout.NORTH); // Add the message label to the NORTH region of the bottom panel

        JPanel buttonsPanel = new JPanel(); // Create a buttons panel to hold the submit and back buttons
        submitButton = new JButton("Submit request"); // Create a submit button with the specified text to allow the user to submit the registration request
        backButton = new JButton("Back to login"); // Create a back button with the specified text to allow the user to return to the login frame

        buttonsPanel.add(submitButton); // Add the submit button to the buttons panel
        buttonsPanel.add(backButton); // Add the back button to the buttons panel

        bottomPanel.add(buttonsPanel, BorderLayout.SOUTH); // Add the buttons panel to the SOUTH region of the bottom panel
        panel.add(bottomPanel, BorderLayout.SOUTH); // Add the bottom panel to the SOUTH region of the main panel

        add(panel); // Add the main panel to the frame

        submitButton.addActionListener(this); // Add an action listener to the submit button to handle the button click event
        backButton.addActionListener(this); // Add an action listener to the back button to handle the button click event
}

// This method adds a component to a panel at the specified column and row using GridBagConstraints. It sets the gridx and gridy properties to specify the position of the component in the grid, and it sets other properties such as fill, anchor, weightx, and insets to control the layout behavior of the component within the panel.
private void addFormItem(JPanel panel, java.awt.Component component, int column, int row){
    GridBagConstraints position = new GridBagConstraints(); // Create a new GridBagConstraints object to specify the layout constraints for the component being added to the panel
    position.gridx = column; // Set the gridx property to specify the column position of the component in the grid
    position.gridy = row; // Set the gridy property to specify the row position of the component in the grid
    position.fill = GridBagConstraints.HORIZONTAL; // Set the fill property to specify that the component should expand horizontally to fill the available space in its cell
    position.anchor = GridBagConstraints.WEST; // Set the anchor property to specify that the component should be aligned to the west (left) side of its cell
    position.weightx = 1; // Set the weightx property to specify that the component should take up extra horizontal space if available, allowing it to grow and shrink with the panel
    position.insets = new Insets(5, 5, 5, 5); // Set the insets property to specify the external padding of the component, creating a 5-pixel space on all sides (top, left, bottom, right) between the component and its neighboring components
    panel.add(component, position); // Add the component to the panel with the specified layout constraints, allowing it to be positioned and sized according to the defined properties
}
@Override 
// This method is called when an action event occurs, such as a button click. It checks the source of the event and calls the appropriate method to handle the action. If the submit button is clicked, it calls the submitRequest() method to process the registration request. If the back button is clicked, it calls the returnToLogin() method to navigate back to the login frame.
public void actionPerformed(ActionEvent event){
    if (event.getSource() == submitButton) submitRequest(); // Call the submitRequest() method to handle the registration request when the submit button is clicked
    else if (event.getSource() == backButton) returnToLogin(); // Call the returnToLogin() method to navigate back to the login frame when the back button is clicked
}
// This method handles the submission of the registration request. It reads the user input from the text fields, validates the input, and sends the registration request to the application controller. If any validation fails, it displays an error message. If the registration is successful, it displays a success message and informs the user that their request is pending approval.
private void submitRequest() {

    // Confirm that the Submit button was clicked
    System.out.println("DEBUG: Submit button clicked");

    // Read student information
    String universityId = universityIdField.getText().trim(); // Read the university ID from the text field and trim any leading or trailing whitespace
    String fullName = fullNameField.getText().trim(); // Read the full name from the text field and trim any leading or trailing whitespace
    String email = emailField.getText().trim(); // Read the email address from the text field and trim any leading or trailing whitespace
    String phone = phoneField.getText().trim(); // Read the phone number from the text field and trim any leading or trailing whitespace
    String password = new String(passwordField.getPassword()); // Read the password from the password field and convert it to a string

    // Reset message appearance
    messageLabel.setForeground(Color.RED); // Set the foreground color of the message label to red to indicate an error message
    messageLabel.setBackground(new Color(255, 238, 235)); // Set the background color of the message label to a light red color to indicate an error message

    // Check that all fields are completed
    if (isBlank(universityId) ||
        isBlank(fullName) ||
        isBlank(email) ||
        isBlank(phone) ||
        isBlank(password)) {

        showRegistrationError("Please complete all five fields.");
        return;
    } // If any of the fields are blank, display an error message and return without submitting the request

    // Validate university ID by checking if it contains only digits using a regular expression. If the university ID is invalid, display an error message and set focus back to the university ID field.
    if (!universityId.matches("[0-9]+")) {

        showRegistrationError(
            "University ID must contain digits only."
        );
        universityIdField.requestFocusInWindow();
        return;
    }

    // Validate phone number by checking if it contains only digits and has a length between 7 and 15 characters using a regular expression. If the phone number is invalid, display an error message and set focus back to the phone number field.
    if (!phone.matches("\\+?[0-9]{7,15}")) {

        showRegistrationError(
            "Enter a phone number with 7 to 15 digits."
        );
        phoneField.requestFocusInWindow();
        return;
    }

    // Validate email address by checking if it matches a basic email pattern using a regular expression. If the email address is invalid, display an error message and set focus back to the email field.
    if (!email.matches(
            "^[^\\s@]+@[^\\s@.]+(\\.[^\\s@.]+)+$")) {

        showRegistrationError(
            "Enter a valid email address."
        );
        emailField.requestFocusInWindow();
        return;
    }
    // Validate password by checking if it has at least 8 characters, contains at least one uppercase letter, one lowercase letter, and one digit using a regular expression. If the password is invalid, display an error message and set focus back to the password field.
    System.out.println("DEBUG: Registration validation passed");

    try {
        
        // Send registration to the application controller
        accountActions.submitRegistration(
            universityId,
            fullName,
            email,
            phone,
            password
        );

        System.out.println("DEBUG: Registration callback completed");

        // Display successful registration
        messageLabel.setForeground(new Color(0, 110, 94));
        messageLabel.setBackground(new Color(225, 243, 238));
        // Set the message label to indicate that the registration request was sent successfully and is pending approval
        messageLabel.setText(
            "Request sent. Status: Pending"
        );
        // Show a dialog box to inform the user that their registration request was sent and is pending approval
        JOptionPane.showMessageDialog(
            this,
            "Your request was sent.\n"
            + "Wait for administrator approval.",
            "Registration Successful",
            JOptionPane.INFORMATION_MESSAGE
        );
    } catch (IllegalArgumentException |
             IllegalStateException error) {

        // Display registration or storage errors
        System.out.println(
            "DEBUG: Registration failed: " + error.getMessage()
        );
        // Show an error dialog box to inform the user that the registration request failed, displaying the error message if available, or a generic message if not
        showRegistrationError(
            error.getMessage() == null
                ? "Registration failed."
                : error.getMessage()
        );
    }
}
// This method displays an error message in the message label and shows a dialog box with the specified error message. It sets the foreground color of the message label to red and the background color to a light red color to indicate an error message. It also uses the Dialog class to show a warning dialog box with the specified message.
private void showRegistrationError(String message){
    messageLabel.setForeground(Color.RED);
    messageLabel.setBackground(new Color(255, 238, 235));
    messageLabel.setText(message);
    Dialog.showError(this, message);
} 
  // This method checks if a given string is blank (null, empty, or contains only whitespace). It takes a string text as a parameter and returns true if the string is blank, and false otherwise. This method is used for input validation to ensure that the user has entered valid data in the username and password fields.
    private boolean isBlank(String text) {
        if (text == null || text.trim().isEmpty())  return true;
         return false;
        }
        // This method is called when the back button is clicked. It creates a new instance of the LoginFrame class, passing the accountActions object to it, and makes the login frame visible. It then disposes of the current registration frame, effectively returning the user to the login screen.
    private void returnToLogin(){
        LoginFrame login = new LoginFrame(accountActions); // Create a new instance of the LoginFrame class, passing the accountActions object to it. This allows the login frame to have access to the same account actions as the registration frame.
        login.setVisible(true); // Make the login frame visible to the user
        dispose(); // Dispose of the current registration frame to free up resources and close the window
    }
}
