
import logic.AppState;
import storage.DataStore;

import model.AccessRequest;
import model.Administrator;
import model.Student;

import UI.*;


import javax.swing.*;
import java.io.IOException;
import java.time.LocalDateTime;

// Main application launcher for GPU Utilization Management
public class project {

    // Shared application state
    private static AppState state;

    // Handles saving and loading data.txt
    private static DataStore dataStore;

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

                    // Initialize shared clock and scheduler
                    state.initializeClock(selectedTime);

                    // Save initial application state
                    dataStore.save(state);

                    // Open login after setup
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

                        // Save registration
                        dataStore.save(state);

                    } catch (NumberFormatException ex) {

                        throw new IllegalArgumentException(
                            "University ID must contain valid digits."
                        );

                    } catch (IOException ex) {

                        throw new IllegalStateException(
                            "Could not save registration: "
                                + ex.getMessage()
                        );
                    }
                }
            }
        );

        login.setVisible(true);
    }

    // Opens the administrator dashboard
    private static void openAdminDashboard() {

        JFrame frame = new JFrame(
            "GPU Utilization Management - Administrator"
        );

        frame.setDefaultCloseOperation(
            JFrame.EXIT_ON_CLOSE
        );

        frame.setSize(1200, 750);
        frame.setLocationRelativeTo(null);

        // Use shared state and save callback
        AdminDashboard dashboard = new AdminDashboard(
            state.getAdministrator(),
            state.getScheduler(),
            state.getClock(),
            state,
            () -> {
                try {

                    dataStore.save(state);

                } catch (IOException ex) {

                    throw new IllegalStateException(
                        "Could not save changes: "
                            + ex.getMessage(),
                        ex
                    );
                }
            }
        );

        frame.add(dashboard);
        frame.setVisible(true);
    }

    // Opens the student dashboard
    private static void openStudentPanel(Student student) {

        JFrame frame = new JFrame(
            "GPU Utilization Management - Student"
        );

        frame.setDefaultCloseOperation(
            JFrame.EXIT_ON_CLOSE
        );

        frame.setSize(1200, 750);
        frame.setLocationRelativeTo(null);

        // Pass shared state and saving callback
        StudentPanel panel = new StudentPanel(
            student,
            state.getScheduler(),
            state.getClock(),
            state,
            () -> {
                try {

                    dataStore.save(state);

                } catch (IOException ex) {

                    throw new IllegalStateException(
                        "Could not save student changes: "
                            + ex.getMessage(),
                        ex
                    );
                }
            }
        );

        frame.add(panel);
        frame.setVisible(true);
    }

    // Saves the current application state
    public static void saveState() {
        try {

            dataStore.save(state);

        } catch (IOException ex) {

            JOptionPane.showMessageDialog(
                null,
                "Failed to save application data:\n"
                    + ex.getMessage(),
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
