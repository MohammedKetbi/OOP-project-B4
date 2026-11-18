
package UI;

import model.Student;
import model.TimeAllowance;
import model.Job;
import logic.Scheduler;
import logic.SimulationClock;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

// Student interface for GPU Utilization Management
public class StudentPanel extends JPanel {

    // Logged-in student and shared system objects
    private final Student student;
    private final Scheduler scheduler;
    private final SimulationClock clock;

    // GPU allowance labels
    private JLabel allocatedLabel;
    private JLabel usedLabel;
    private JLabel reservedLabel;
    private JLabel availableLabel;

    // My Jobs components
    private JTable jobsTable;
    private DefaultTableModel jobsModel;
    private TableRowSorter<DefaultTableModel> jobsSorter;
    private JTextField searchField;
    private JComboBox<String> statusFilter;

    // Date and time format
    private final DateTimeFormatter dateFormat =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Constructor
    public StudentPanel(Student student,
                        Scheduler scheduler,
                        SimulationClock clock) {

        if (student == null || scheduler == null || clock == null) {
            throw new IllegalArgumentException(
                "Student, Scheduler and Clock are required"
            );
        }

        this.student = student;
        this.scheduler = scheduler;
        this.clock = clock;

        setLayout(new BorderLayout());

        // Create the student tabs
        JTabbedPane tabs = new JTabbedPane();

        tabs.addTab("Home and Submit", createHomePanel());
        tabs.addTab("My Jobs", createJobsPanel());

        add(tabs, BorderLayout.CENTER);

        // Refresh when switching tabs
        tabs.addChangeListener(e -> refreshAll());

        refreshAll();
    }

    // Creates the student home screen
    private JPanel createHomePanel() {

        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Student information
        JPanel studentInfo = new JPanel(new GridLayout(2, 1));

        studentInfo.add(
            new JLabel("Welcome, " + student.getName())
        );

        studentInfo.add(
            new JLabel("Student ID: " + student.getStudentId())
        );

        panel.add(studentInfo, BorderLayout.NORTH);

        // GPU allowance information
        JPanel allowancePanel = new JPanel(
            new GridLayout(2, 2, 10, 10)
        );

        allocatedLabel = new JLabel();
        usedLabel = new JLabel();
        reservedLabel = new JLabel();
        availableLabel = new JLabel();

        allowancePanel.add(allocatedLabel);
        allowancePanel.add(usedLabel);
        allowancePanel.add(reservedLabel);
        allowancePanel.add(availableLabel);

        // Place allowance above the submission form
        JPanel contentPanel = new JPanel();

        contentPanel.setLayout(
            new BoxLayout(contentPanel, BoxLayout.Y_AXIS)
        );

        contentPanel.add(allowancePanel);
        contentPanel.add(createSubmitPanel());

        panel.add(
            new JScrollPane(contentPanel),
            BorderLayout.CENTER
        );

        return panel;
    }

    // Creates the job submission form
    private JPanel createSubmitPanel() {

        JPanel panel = new JPanel(
            new GridLayout(5, 2, 10, 10)
        );

        // Job information fields
        JTextField titleField = new JTextField();
        JTextField applicationField = new JTextField();
        JTextArea descriptionArea = new JTextArea(3, 20);

        // Task sizes supported by Job
        String[] sizes = {
            "Small",
            "Medium",
            "Large",
            "Very large"
        };

        JComboBox<String> sizeBox = new JComboBox<>(sizes);

        JButton submitButton = new JButton("Submit Job");

        // Add form components
        panel.add(new JLabel("Job Title:"));
        panel.add(titleField);

        panel.add(new JLabel("Application:"));
        panel.add(applicationField);

        panel.add(new JLabel("Description:"));
        panel.add(new JScrollPane(descriptionArea));

        panel.add(new JLabel("Task Size:"));
        panel.add(sizeBox);

        panel.add(new JLabel());
        panel.add(submitButton);

        // Submit button action
        submitButton.addActionListener(e -> {

            String title = titleField.getText().trim();
            String application = applicationField.getText().trim();
            String description = descriptionArea.getText().trim();
            String size = (String) sizeBox.getSelectedItem();

            // Check required fields
            if (title.isEmpty() ||
                application.isEmpty() ||
                description.isEmpty()) {

                JOptionPane.showMessageDialog(
                    this,
                    "Please fill in all job details.",
                    "Missing Information",
                    JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            // Calculate job duration
            int duration;

            switch (size) {
                case "Small":
                    duration = 5;
                    break;
                case "Medium":
                    duration = 15;
                    break;
                case "Large":
                    duration = 30;
                    break;
                case "Very large":
                    duration = 60;
                    break;
                default:
                    JOptionPane.showMessageDialog(
                        this,
                        "Invalid task size."
                    );
                    return;
            }

            int available = student.getAllowance().available();

            // Check available GPU minutes
            if (duration > available) {

                JOptionPane.showMessageDialog(
                    this,
                    "Required: " + duration
                        + " minutes\nAvailable: "
                        + available + " minutes",
                    "Insufficient Allowance",
                    JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            // Confirm job submission
            int choice = JOptionPane.showConfirmDialog(
                this,
                "Title: " + title
                    + "\nApplication: " + application
                    + "\nTask Size: " + size
                    + "\nDuration: " + duration + " minutes"
                    + "\nAvailable after submission: "
                    + (available - duration)
                    + " minutes\n\nSubmit this job?",
                "Confirm Submission",
                JOptionPane.YES_NO_OPTION
            );

            if (choice != JOptionPane.YES_OPTION) {
                return;
            }

            try {

                // Generate the next available job ID
                int nextId = 101;

                for (Job existing : scheduler.getAllJobs()) {

                    String id = existing.getJobId();

                    if (id != null && id.matches("J[0-9]+")) {

                        int number = Integer.parseInt(
                            id.substring(1)
                        );

                        if (number >= nextId) {
                            nextId = number + 1;
                        }
                    }
                }

                // Create a job using the simulation clock
                Job job = new Job(
                    "J" + nextId,
                    student.getStudentId(),
                    title,
                    application,
                    description,
                    size,
                    clock.getTime()
                );

                // Submit job to the scheduler
                scheduler.submit(job);

                // Update the interface
                refreshAll();

                JOptionPane.showMessageDialog(
                    this,
                    "Job " + job.getJobId()
                        + " submitted successfully!"
                        + "\nStatus: " + job.getStatus(),
                    "Submission Successful",
                    JOptionPane.INFORMATION_MESSAGE
                );

                // Clear the form
                titleField.setText("");
                applicationField.setText("");
                descriptionArea.setText("");
                sizeBox.setSelectedIndex(0);

            } catch (IllegalArgumentException |
                     IllegalStateException ex) {

                JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Submission Failed",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        });

        return panel;
    }

    // Creates the My Jobs screen
    private JPanel createJobsPanel() {

        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Table columns
        String[] columns = {
            "Job ID",
            "Title",
            "Application",
            "Status",
            "GPU",
            "Duration",
            "Submitted",
            "Predicted Start",
            "Predicted Finish",
            "Wait"
        };

        // Create a non-editable table
        jobsModel = new DefaultTableModel(columns, 0) {

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        jobsTable = new JTable(jobsModel);

        jobsTable.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );

        // Enable sorting and filtering
        jobsSorter = new TableRowSorter<>(jobsModel);
        jobsTable.setRowSorter(jobsSorter);

        // Search and status filter
        searchField = new JTextField();

        String[] statuses = {
            "All",
            "Queued",
            "Running",
            "Completed",
            "Cancelled"
        };

        statusFilter = new JComboBox<>(statuses);

        JPanel searchPanel = new JPanel(
            new GridLayout(2, 2, 10, 10)
        );

        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);

        searchPanel.add(new JLabel("Status:"));
        searchPanel.add(statusFilter);

        // Job action buttons
        JButton cancelButton = new JButton("Cancel Job");
        JButton refreshButton = new JButton("Refresh");

        JPanel buttonPanel = new JPanel();

        buttonPanel.add(cancelButton);
        buttonPanel.add(refreshButton);

        // Organize top controls
        JPanel topPanel = new JPanel(
            new BorderLayout(10, 10)
        );

        topPanel.add(
            new JLabel("My Jobs", SwingConstants.CENTER),
            BorderLayout.NORTH
        );

        topPanel.add(searchPanel, BorderLayout.SOUTH);

        panel.add(topPanel, BorderLayout.NORTH);

        // Display the jobs table
        panel.add(
            new JScrollPane(jobsTable),
            BorderLayout.CENTER
        );

        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Search whenever the user types
        searchField.getDocument().addDocumentListener(
            new DocumentListener() {

                public void insertUpdate(DocumentEvent e) {
                    filterJobs();
                }

                public void removeUpdate(DocumentEvent e) {
                    filterJobs();
                }

                public void changedUpdate(DocumentEvent e) {
                    filterJobs();
                }
            }
        );

        // Filter when the selected status changes
        statusFilter.addActionListener(e -> filterJobs());

        // Cancel the selected job
        cancelButton.addActionListener(e -> cancelSelectedJob());

        // Refresh jobs and allowance
        refreshButton.addActionListener(e -> refreshAll());

        return panel;
    }

    // Filters the jobs table
    private void filterJobs() {

        String search = searchField.getText()
            .trim().toLowerCase();

        String status =
            (String) statusFilter.getSelectedItem();

        jobsSorter.setRowFilter(
            new RowFilter<DefaultTableModel, Integer>() {

                @Override
                public boolean include(
                    Entry<? extends DefaultTableModel,
                          ? extends Integer> entry) {

                    // Search by ID, title, or application
                    boolean matchesSearch =
                        entry.getStringValue(0)
                            .toLowerCase().contains(search)
                        || entry.getStringValue(1)
                            .toLowerCase().contains(search)
                        || entry.getStringValue(2)
                            .toLowerCase().contains(search);

                    // Filter by job status
                    boolean matchesStatus =
                        "All".equals(status)
                        || entry.getStringValue(3).equals(status);

                    return matchesSearch && matchesStatus;
                }
            }
        );
    }

    // Cancels the selected job
    private void cancelSelectedJob() {

        int selectedRow = jobsTable.getSelectedRow();

        if (selectedRow == -1) {

            JOptionPane.showMessageDialog(
                this,
                "Please select a job first."
            );
            return;
        }

        // Convert displayed row to model row
        int modelRow =
            jobsTable.convertRowIndexToModel(selectedRow);

        String jobId =
            jobsModel.getValueAt(modelRow, 0).toString();

        // Find the real job belonging to this student
        Job selectedJob = null;

        for (Job job : scheduler.getAllJobs()) {

            if (job.getJobId().equals(jobId)
                && job.getStudentId().equals(
                    student.getStudentId())) {

                selectedJob = job;
                break;
            }
        }

        if (selectedJob == null) {

            JOptionPane.showMessageDialog(
                this,
                "Job not found."
            );
            return;
        }

        // Check whether cancellation is allowed
        String status = selectedJob.getStatus();

        if (!status.equals("Queued") &&
            !status.equals("Running")) {

            JOptionPane.showMessageDialog(
                this,
                "Only queued or running jobs can be cancelled."
            );
            return;
        }

        // Ask for cancellation reason
        String reason = JOptionPane.showInputDialog(
            this,
            "Reason for cancelling " + jobId + ":"
        );

        if (reason == null) {
            return;
        }

        reason = reason.trim();

        if (reason.isEmpty()) {

            JOptionPane.showMessageDialog(
                this,
                "A cancellation reason is required."
            );
            return;
        }

        // Confirm cancellation
        int choice = JOptionPane.showConfirmDialog(
            this,
            "Cancel job " + jobId + "?",
            "Confirm Cancellation",
            JOptionPane.YES_NO_OPTION
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {

            // Scheduler handles cancellation and charging
            scheduler.cancel(selectedJob, student, reason);

            refreshAll();

            JOptionPane.showMessageDialog(
                this,
                "Job " + jobId + " cancelled successfully."
            );

        } catch (IllegalArgumentException |
                 IllegalStateException ex) {

            JOptionPane.showMessageDialog(
                this,
                ex.getMessage(),
                "Cancellation Failed",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // Formats simulated dates and times
    private String formatTime(LocalDateTime time) {

        if (time == null) {
            return "-";
        }

        return time.format(dateFormat);
    }

    // Loads real jobs belonging to the logged-in student
    public void refreshJobs() {

        // Clear previous table rows
        jobsModel.setRowCount(0);

        // Get predicted start and finish times
        Map<Job, LocalDateTime[]> predictions =
            scheduler.predictSchedule();

        // Read all accepted jobs
        for (Job job : scheduler.getAllJobs()) {

            // Display only this student's jobs
            if (!student.getStudentId().equals(
                    job.getStudentId())) {
                continue;
            }

            LocalDateTime predictedStart = null;
            LocalDateTime predictedFinish = null;

            LocalDateTime[] prediction =
                predictions.get(job);

            if (prediction != null) {
                predictedStart = prediction[0];
                predictedFinish = prediction[1];
            }

            // Use recorded times for finished jobs
            if ("Completed".equals(job.getStatus())
                    || "Cancelled".equals(job.getStatus())) {

                predictedStart = job.getStartTime();
                predictedFinish = job.getFinishTime();
            }

            // Display GPU ID or dash if not assigned
            String gpu = job.getAssignedGpuId() == 0
                ? "-"
                : String.valueOf(job.getAssignedGpuId());

            // Calculate waiting time
            String wait = "-";

            if ("Queued".equals(job.getStatus())) {

                wait = scheduler.getRemainingWait(job)
                    + " min";

            } else if (job.getStartTime() != null) {

                wait = job.getActualQueueWait() + " min";
            }

            // Add job information to the table
            jobsModel.addRow(new Object[]{
                job.getJobId(),
                job.getTitle(),
                job.getApplicationName(),
                job.getStatus(),
                gpu,
                job.getEstimatedDuration() + " min",
                formatTime(job.getSubmissionTime()),
                formatTime(predictedStart),
                formatTime(predictedFinish),
                wait
            });
        }

        // Apply current search and filter settings
        filterJobs();
    }

    // Updates GPU allowance labels
    public void refreshAllowance() {

        TimeAllowance allowance = student.getAllowance();

        allocatedLabel.setText(
            "Allocated: " + allowance.getAllocated()
        );

        usedLabel.setText(
            "Used: " + allowance.getUsed()
        );

        reservedLabel.setText(
            "Reserved: " + allowance.getReserved()
        );

        availableLabel.setText(
            "Available: " + allowance.available()
        );
    }

    // Refreshes the complete student interface
    public void refreshAll() {

        refreshAllowance();
        refreshJobs();
    }
}