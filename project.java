
import logic.AppState;
import storage.DataStore;

import model.AccessRequest;
import model.Administrator;
import model.Student;

import ui.ClockSetUpScreen3;
import ui.LoginFrame;
import ui.StudentPanel;
import ui.AdminDashboard;

import javax.swing.*;
import java.awt.Window;
import java.io.IOException;
import java.time.LocalDateTime;

// Main application launcher for GPU Utilization Management
public class project {

    // Shared application state
    private static AppState state;

    // Handles saving and loading data.txt
    private static DataStore dataStore;

    // Prevents further changes while recovery is pending
    private static boolean recoveryPending = false;

    // Starting point of the application
    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            // Create storage manager
            dataStore = new DataStore();

            try {

                // Load previously saved application data
                state = dataStore.load();

                // First run requires clock setup
                if (!state.isClockSet()) {
                    openClockSetup();
                } else {
                    openLogin();
                }

            } catch (IOException ex) {

                JOptionPane.showMessageDialog(
                    null,
                    "Failed to load application data:\n"
                        + ex.getMessage(),
                    "Storage Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }

    // Opens clock setup on the first run
    private static void openClockSetup() {

        ClockSetUpScreen3 setup = new ClockSetUpScreen3(
            new ClockSetUpScreen3.ClockStartHandler() {

                @Override
                public void startClock(
                        LocalDateTime selectedTime)
                        throws Exception {

                    checkRecovery();

                    // Initialize shared clock and scheduler
                    state.initializeClock(selectedTime);

                    // Save initial application state
                    saveOrRecover();

                    // Open login only after successful saving
                    openLogin();
                }
            }
        );

        setup.setVisible(true);
    }

    // Opens the login screen
    private static void openLogin() {

        LoginFrame login = new LoginFrame(
            new LoginFrame.AccountActions() {

                @Override
                public void signIn(
                        String username,
                        String password) {

                    checkRecovery();

                    username = username.trim();

                    Administrator administrator =
                        state.getAdministrator();

                    // Administrator login
                    if ("admin".equals(username)) {

                        if (administrator != null &&
                            administrator.checkPassword(password)) {

                            openAdminDashboard();
                            return;
                        }

                        throw new IllegalArgumentException(
                            "Incorrect administrator password."
                        );
                    }

                    // Student login
                    for (Student student : state.getStudents()) {

                        if (student.getStudentId().equals(username)) {

                            if (student.checkPassword(password)) {

                                openStudentPanel(student);
                                return;
                            }

                            throw new IllegalArgumentException(
                                "Incorrect password."
                            );
                        }
                    }

                    throw new IllegalArgumentException(
                        "Account not found or not approved."
                    );
                }

                // Handles new student registration
                @Override
                public void submitRegistration(
                        String universityId,
                        String fullName,
                        String email,
                        String phone,
                        String password) {

                    checkRecovery();

                    try {

                        int id = Integer.parseInt(
                            universityId.trim()
                        );

                        // Create a pending registration request
                        AccessRequest request = new AccessRequest(
                            id,
                            fullName.trim(),
                            email.trim(),
                            phone.trim(),
                            password
                        );

                        // Add request to shared state
                        state.addRequest(request);

                        // Save registration with recovery protection
                        saveOrRecover();

                    } catch (NumberFormatException ex) {

                        throw new IllegalArgumentException(
                            "University ID must contain valid digits."
                        );
                    }
                }
            }
        );

        login.setVisible(true);
    }

    // Opens the administrator dashboard
    private static void openAdminDashboard() {

        checkRecovery();

        JFrame frame = new JFrame(
            "GPU Utilization Management - Administrator"
        );

        frame.setDefaultCloseOperation(
            JFrame.EXIT_ON_CLOSE
        );

        frame.setSize(1200, 750);
        frame.setLocationRelativeTo(null);

        // Use shared state and protected save callback
        AdminDashboard dashboard = new AdminDashboard(
            state.getAdministrator(),
            state.getScheduler(),
            state.getClock(),
            state,
            () -> saveOrRecover()
        );

        frame.add(dashboard);
        frame.setVisible(true);
    }

    // Opens the student dashboard
    private static void openStudentPanel(Student student) {

        checkRecovery();

        JFrame frame = new JFrame(
            "GPU Utilization Management - Student"
        );

        frame.setDefaultCloseOperation(
            JFrame.EXIT_ON_CLOSE
        );

        frame.setSize(1200, 750);
        frame.setLocationRelativeTo(null);

        // Pass shared state and protected save callback
        StudentPanel panel = new StudentPanel(
            student,
            state.getScheduler(),
            state.getClock(),
            state,
            () -> saveOrRecover()
        );

        frame.add(panel);
        frame.setVisible(true);
    }

    // Prevent operations while recovery is pending
    private static void checkRecovery() {

        if (recoveryPending) {
            throw new IllegalStateException(
                "Storage recovery is in progress."
            );
        }
    }

    // Save the application and schedule recovery if saving fails
    private static void saveOrRecover() {

        checkRecovery();

        try {

            // Save the updated shared application state
            dataStore.save(state);

        } catch (IOException saveError) {

            // Prevent another save while recovery is pending
            recoveryPending = true;

            // Schedule recovery after the current GUI event
            // finishes, avoiding stale references in open screens
            SwingUtilities.invokeLater(() -> {
                recoverSavedState(saveError);
            });

            // Stop the current action from reporting success
            throw new IllegalStateException(
                "Saving failed. Recovery will now begin.",
                saveError
            );
        }
    }

    // Reload the saved state and reopen the application safely
    private static void recoverSavedState(
            IOException saveError) {

        // Disable all existing application windows
        for (Window window : Window.getWindows()) {
            window.setEnabled(false);
        }

        try {

            // Reload the actual saved file
            // This also handles cases where replacement succeeded
            // before an error was reported
            AppState restoredState = dataStore.load();

            // Close old windows holding outdated object references
            for (Window window : Window.getWindows()) {
                window.dispose();
            }

            // Replace the shared state only after loading succeeds
            state = restoredState;

            JOptionPane.showMessageDialog(
                null,
                "A save operation failed.\n"
                    + "The application has reloaded the saved data.\n"
                    + "Please sign in again.\n\n"
                    + "The latest action may not have been saved.\n\n"
                    + saveError.getMessage(),
                "Storage Recovery",
                JOptionPane.WARNING_MESSAGE
            );

            // Recovery is complete
            recoveryPending = false;

            // Open the appropriate screen with fresh objects
            if (state.isClockSet()) {
                openLogin();
            } else {
                openClockSetup();
            }

        } catch (IOException recoveryError) {

            // Do not continue using potentially inconsistent data
            JOptionPane.showMessageDialog(
                null,
                "Saving failed and the saved state "
                    + "could not be reloaded.\n\n"
                    + recoveryError.getMessage()
                    + "\n\nClose and investigate the storage file "
                    + "before restarting.",
                "Recovery Failed",
                JOptionPane.ERROR_MESSAGE
            );

            // Remain blocked rather than using stale state
            // The application can be restarted after fixing storage
        }
    }

    // Saves the current application state
    public static void saveState() {

        try {

            saveOrRecover();

        } catch (IllegalStateException ex) {

            JOptionPane.showMessageDialog(
                null,
                ex.getMessage(),
                "Save Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // Returns the shared application state
    public static AppState getState() {
        return state;
    }
}
