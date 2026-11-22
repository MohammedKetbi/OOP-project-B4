
package UI;

import model.Student;
import model.GPU;
import logic.Scheduler;
import logic.SimulationClock;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDateTime;
import java.util.OptionalDouble;

// Tests StudentPanel, predictions, and GPU utilization
public class TestStudentPanel {

    public static void main(String[] args) {

        // Run the GUI on the Swing event thread
        SwingUtilities.invokeLater(() -> {

            // Create a student with 100 GPU minutes
            Student student = new Student(
                "100052210",
                "pass123",
                "Sara Ahmed",
                "sara@example.com",
                "0501234567"
            );

            // Create the simulation clock
            SimulationClock clock = new SimulationClock(
                LocalDateTime.of(2026, 11, 20, 10, 0)
            );

            // Create four GPUs
            GPU[] gpus = {
                new GPU(1),
                new GPU(2),
                new GPU(3),
                new GPU(4)
            };

            // Create the scheduler
            Scheduler scheduler = new Scheduler(clock, gpus);

            // Register the student
            scheduler.registerStudent(student);

            // Create the main application window
            JFrame frame = new JFrame(
                "GPU Management - Student Test"
            );

            frame.setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
            );

            frame.setSize(1200, 700);
            frame.setLocationRelativeTo(null);

            // Create StudentPanel using shared objects
            StudentPanel studentPanel = new StudentPanel(
                student,
                scheduler,
                clock
            );

            // Main container
            JPanel mainPanel = new JPanel(
                new BorderLayout()
            );

            // Add StudentPanel in the center
            mainPanel.add(
                studentPanel,
                BorderLayout.CENTER
            );

            // Panel for simulation controls
            JPanel controlPanel = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 15, 10)
            );

            // Button to advance simulated time
            JButton advanceButton = new JButton(
                "Advance 1 Minute"
            );

            // Label displaying current simulation time
            JLabel timeLabel = new JLabel(
                "Simulation Time: " + clock.getTime()
            );

            // Label displaying GPU utilization
            JLabel utilizationLabel = new JLabel(
                "GPU Utilization: N/A"
            );

            // Update simulation time and utilization
            advanceButton.addActionListener(e -> {

                try {

                    // Advance the simulation by one minute
                    scheduler.advanceMinute();

                    // Refresh job predictions and allowances
                    studentPanel.refreshAll();

                    // Display the updated simulation time
                    timeLabel.setText(
                        "Simulation Time: " + clock.getTime()
                    );

                    // Get the utilization calculation
                    OptionalDouble utilization =
                        scheduler.getUtilization();

                    // Display utilization if available
                    if (utilization.isPresent()) {

                        utilizationLabel.setText(
                            String.format(
                                "GPU Utilization: %.2f%%",
                                utilization.getAsDouble()
                            )
                        );

                    } else {

                        // At time zero utilization is undefined
                        utilizationLabel.setText(
                            "GPU Utilization: N/A"
                        );
                    }

                } catch (IllegalArgumentException |
                         IllegalStateException ex) {

                    // Show an error if advancing fails
                    JOptionPane.showMessageDialog(
                        frame,
                        ex.getMessage(),
                        "Simulation Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            });

            // Add controls to the bottom panel
            controlPanel.add(advanceButton);
            controlPanel.add(timeLabel);
            controlPanel.add(utilizationLabel);

            // Add controls below StudentPanel
            mainPanel.add(
                controlPanel,
                BorderLayout.SOUTH
            );

            // Add the main container to the window
            frame.setContentPane(mainPanel);

            // Display the application
            frame.setVisible(true);
        });
    }
}
