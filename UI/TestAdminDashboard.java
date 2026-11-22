
package UI;

import model.Administrator;
import model.Student;
import model.GPU;
import model.Job;

import logic.Scheduler;
import logic.SimulationClock;

import javax.swing.*;
import java.time.LocalDateTime;

// Tests the Administrator Dashboard using real project classes
public class TestAdminDashboard {

    // Main method starts the test application
    public static void main(String[] args) {

        // Run the interface on the Swing event thread
        SwingUtilities.invokeLater(() -> {

            // Create the simulation clock
            SimulationClock clock = new SimulationClock(
                LocalDateTime.of(2026, 11, 20, 10, 0)
            );

            // Create the four GPUs
            GPU[] gpus = {
                new GPU(1),
                new GPU(2),
                new GPU(3),
                new GPU(4)
            };

            // Create the scheduler
            Scheduler scheduler = new Scheduler(clock, gpus);

            // Create the administrator
            // The constructor registers the admin with the scheduler
            Administrator administrator = new Administrator(
                scheduler,
                clock
            );

            // Create the first student
            Student student1 = new Student(
                "100052210",
                "pass123",
                "Sara Ahmed",
                "sara@example.com",
                "0501234567"
            );

            // Create the second student
            Student student2 = new Student(
                "100052211",
                "pass456",
                "Ahmed Ali",
                "ahmed@example.com",
                "0509876543"
            );

            // Register both students with the scheduler
            scheduler.registerStudent(student1);
            scheduler.registerStudent(student2);

            // Create four sample jobs for the first student
            Job job1 = new Job(
                "J101",
                student1.getStudentId(),
                "AI Training",
                "Python",
                "Training an AI model",
                "Small",
                clock.getTime()
            );

            Job job2 = new Job(
                "J102",
                student1.getStudentId(),
                "Data Analysis",
                "MATLAB",
                "Analyzing research data",
                "Medium",
                clock.getTime()
            );

            Job job3 = new Job(
                "J103",
                student1.getStudentId(),
                "Image Processing",
                "Python",
                "Processing image datasets",
                "Small",
                clock.getTime()
            );

            Job job4 = new Job(
                "J104",
                student1.getStudentId(),
                "Machine Learning",
                "TensorFlow",
                "Testing a machine learning model",
                "Large",
                clock.getTime()
            );

            // Create a fifth job for the second student
            Job job5 = new Job(
                "J105",
                student2.getStudentId(),
                "GPU Simulation",
                "Java",
                "Running a GPU simulation",
                "Small",
                clock.getTime()
            );

            // Submit jobs through the real scheduler
            scheduler.submit(job1);
            scheduler.submit(job2);
            scheduler.submit(job3);
            scheduler.submit(job4);
            scheduler.submit(job5);

            // Create the application window
            JFrame frame = new JFrame(
                "GPU Management - Administrator Test"
            );

            // Close the test application when window closes
            frame.setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
            );

            // Set the window size
            frame.setSize(1200, 750);

            // Center the window
            frame.setLocationRelativeTo(null);

            // Create the Administrator Dashboard
            AdminDashboard dashboard = new AdminDashboard(
                administrator,
                scheduler,
                clock
            );

            // Add dashboard to the window
            frame.add(dashboard);

            // Display the application
            frame.setVisible(true);
        });
    }
}