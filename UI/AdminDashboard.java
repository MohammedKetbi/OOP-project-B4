
package UI;

import model.Administrator;
import model.Student;
import model.Job;
import model.GPU;
import model.TimeAllowance;

import logic.Scheduler;
import logic.SimulationClock;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.OptionalDouble;

// Administrator dashboard for GPU Utilization Management
public class AdminDashboard extends JPanel {

    // Shared system objects
    private final Administrator administrator;
    private final Scheduler scheduler;
    private final SimulationClock clock;

    // Dashboard information
    private JLabel clockLabel;
    private JLabel utilizationLabel;
    private JLabel runningLabel;
    private JLabel queuedLabel;

    // GPU status labels
    private JLabel[] gpuLabels;

    // Job table
    private JTable jobsTable;
    private DefaultTableModel jobsModel;

    // Student search table
    private JTable studentsTable;
    private DefaultTableModel studentsModel;
    private TableRowSorter<DefaultTableModel> studentsSorter;

    // Search field
    private JTextField studentSearchField;

    // Date and time format
    private final DateTimeFormatter dateFormat =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Constructor
    public AdminDashboard(
            Administrator administrator,
            Scheduler scheduler,
            SimulationClock clock) {

        // Validate required objects
        if (administrator == null ||
            scheduler == null ||
            clock == null) {

            throw new IllegalArgumentException(
                "Administrator, Scheduler and Clock are required"
            );
        }

        // Store shared objects
        this.administrator = administrator;
        this.scheduler = scheduler;
        this.clock = clock;

        // Main panel layout
        setLayout(new BorderLayout(10, 10));

        // Create dashboard tabs
        JTabbedPane tabs = new JTabbedPane();

        tabs.addTab("Dashboard", createDashboardPanel());
        tabs.addTab("Student Search", createStudentSearchPanel());

        // Add tabs to the main panel
        add(tabs, BorderLayout.CENTER);

        // Refresh when switching tabs
        tabs.addChangeListener(e -> refreshAll());

        // Load initial information
        refreshAll();
    }

    // Creates the administrator dashboard
    private JPanel createDashboardPanel() {

        JPanel panel = new JPanel(
            new BorderLayout(10, 10)
        );

        // Create dashboard heading
        JLabel title = new JLabel(
            "Administrator Dashboard",
            SwingConstants.CENTER
        );

        panel.add(title, BorderLayout.NORTH);

        // Create information panel
        JPanel informationPanel = new JPanel(
            new GridLayout(2, 2, 10, 10)
        );

        // Initialize information labels
        clockLabel = new JLabel();
        utilizationLabel = new JLabel();
        runningLabel = new JLabel();
        queuedLabel = new JLabel();

        // Add information labels
        informationPanel.add(clockLabel);
        informationPanel.add(utilizationLabel);
        informationPanel.add(runningLabel);
        informationPanel.add(queuedLabel);

        // Create GPU status panel
        JPanel gpuPanel = new JPanel(
            new GridLayout(2, 2, 10, 10)
        );

        gpuPanel.setBorder(
            BorderFactory.createTitledBorder("GPU Status")
        );

        // Create four GPU labels
        gpuLabels = new JLabel[4];

        for (int i = 0; i < 4; i++) {

            gpuLabels[i] = new JLabel(
                "GPU " + (i + 1) + ": Free"
            );

            gpuPanel.add(gpuLabels[i]);
        }

        // Combine information and GPU status
        JPanel topPanel = new JPanel(
            new GridLayout(2, 1, 10, 10)
        );

        topPanel.add(informationPanel);
        topPanel.add(gpuPanel);

        // Create job table columns
        String[] columns = {
            "Job ID",
            "Student ID",
            "Title",
            "Status",
            "GPU",
            "Duration",
            "Queue Position",
            "Remaining Wait"
        };

        // Create a table model that cannot be edited
        jobsModel = new DefaultTableModel(columns, 0) {

            @Override
            public boolean isCellEditable(
                    int row, int column) {

                return false;
            }
        };

        // Create the jobs table
        jobsTable = new JTable(jobsModel);

        // Allow only one job to be selected
        jobsTable.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );

        // Create scrolling for the job table
        JScrollPane jobsScroll = new JScrollPane(jobsTable);

        jobsScroll.setBorder(
            BorderFactory.createTitledBorder("All Jobs")
        );

        // Combine dashboard details and jobs
        JPanel centerPanel = new JPanel(
            new BorderLayout(10, 10)
        );

        centerPanel.add(topPanel, BorderLayout.NORTH);
        centerPanel.add(jobsScroll, BorderLayout.CENTER);

        panel.add(centerPanel, BorderLayout.CENTER);

        // Create dashboard buttons
        JButton advanceButton = new JButton(
            "Advance 1 Minute"
        );

        JButton refreshButton = new JButton("Refresh");

        JButton cancelButton = new JButton("Cancel Job");

        // Add buttons to a panel
        JPanel buttonPanel = new JPanel();

        buttonPanel.add(advanceButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(cancelButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Advance the simulation clock
        advanceButton.addActionListener(e -> {

            try {

                // Advance simulation by one minute
                scheduler.advanceMinute();

                // Refresh dashboard information
                refreshAll();

            } catch (IllegalArgumentException |
                     IllegalStateException ex) {

                Dialog.showError(this, ex.getMessage());
            }
        });

        // Refresh dashboard
        refreshButton.addActionListener(e -> refreshAll());

        // Cancel selected job
        cancelButton.addActionListener(e -> cancelSelectedJob());

        return panel;
    }

    // Creates the Student Search screen
    private JPanel createStudentSearchPanel() {

        JPanel panel = new JPanel(
            new BorderLayout(10, 10)
        );

        // Heading
        JLabel title = new JLabel(
            "Search Registered Students",
            SwingConstants.CENTER
        );

        panel.add(title, BorderLayout.NORTH);

        // Create student table columns
        String[] columns = {
            "Student ID",
            "Name",
            "Email",
            "Phone",
            "Allocated",
            "Used",
            "Reserved",
            "Available"
        };

        // Prevent direct editing of student information
        studentsModel = new DefaultTableModel(columns, 0) {

            @Override
            public boolean isCellEditable(
                    int row, int column) {

                return false;
            }
        };

        // Create student table
        studentsTable = new JTable(studentsModel);

        // Allow sorting and filtering
        studentsSorter =
            new TableRowSorter<>(studentsModel);

        studentsTable.setRowSorter(studentsSorter);

        // Create student search field
        studentSearchField = new JTextField();

        JPanel searchPanel = new JPanel(
            new BorderLayout(10, 10)
        );

        searchPanel.add(
            new JLabel("Search by Student ID or Name:"),
            BorderLayout.WEST
        );

        searchPanel.add(
            studentSearchField,
            BorderLayout.CENTER
        );

        // Combine heading and search field
        JPanel topPanel = new JPanel(
            new BorderLayout(10, 10)
        );

        topPanel.add(title, BorderLayout.NORTH);
        topPanel.add(searchPanel, BorderLayout.SOUTH);

        panel.add(topPanel, BorderLayout.NORTH);

        // Add student table
        panel.add(
            new JScrollPane(studentsTable),
            BorderLayout.CENTER
        );

        // Create refresh button
        JButton refreshButton = new JButton(
            "Refresh Students"
        );

        panel.add(refreshButton, BorderLayout.SOUTH);

        // Search when text changes
        studentSearchField.getDocument().addDocumentListener(
            new DocumentListener() {

                @Override
                public void insertUpdate(DocumentEvent e) {
                    filterStudents();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    filterStudents();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    filterStudents();
                }
            }
        );

        // Refresh student information
        refreshButton.addActionListener(
            e -> refreshStudents()
        );

        return panel;
    }

    // Filters students by university ID or name
    private void filterStudents() {

        String search = studentSearchField.getText()
            .trim().toLowerCase();

        studentsSorter.setRowFilter(
            new RowFilter<DefaultTableModel, Integer>() {

                @Override
                public boolean include(
                    Entry<? extends DefaultTableModel,
                          ? extends Integer> entry) {

                    // Read student ID and name
                    String id = entry.getStringValue(0)
                        .toLowerCase();

                    String name = entry.getStringValue(1)
                        .toLowerCase();

                    // Display matching students
                    return id.contains(search)
                        || name.contains(search);
                }
            }
        );
    }

    // Cancels a job selected by the administrator
    private void cancelSelectedJob() {

        // Get selected row
        int selectedRow = jobsTable.getSelectedRow();

        if (selectedRow == -1) {

            Dialog.showError(this, "Please select a job first.");

            return;
        }

        // Get selected job ID
        String jobId = jobsTable.getValueAt(
            selectedRow, 0
        ).toString();

        // Find the real job
        Job selectedJob = null;

        for (Job job : scheduler.getAllJobs()) {

            if (job.getJobId().equals(jobId)) {

                selectedJob = job;
                break;
            }
        }

        if (selectedJob == null) {

            Dialog.showError(this, "Job not found.");

            return;
        }

        // Only active jobs can be cancelled
        if (!"Queued".equals(selectedJob.getStatus())
                && !"Running".equals(selectedJob.getStatus())) {

            Dialog.showError(this, "Only queued or running jobs can be cancelled.");

            return;
        }

        // Ask administrator for cancellation reason
        String reason = JOptionPane.showInputDialog(
            this,
            "Reason for cancelling job " + jobId + ":"
        );

        // Stop if administrator closes dialog
        if (reason == null) {
            return;
        }

        reason = reason.trim();

        // Require a cancellation reason
        if (reason.isEmpty()) {

            Dialog.showError(this, "A cancellation reason is required.");

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

            // Administrator cancels through existing scheduler
            administrator.cancelJob(selectedJob, reason);

            // Refresh displayed information
            refreshAll();

            JOptionPane.showMessageDialog(this, "Job " + jobId + " cancelled successfully.", "Cancellation Successful", JOptionPane.INFORMATION_MESSAGE);

        } catch (IllegalArgumentException |
                 IllegalStateException ex) {

            Dialog.showError(this, ex.getMessage());
        }
    }

    // Updates dashboard information
    public void refreshDashboard() {

        // Display current simulation time
        clockLabel.setText(
            "Simulation Time: "
                + clock.getTime().format(dateFormat)
        );

        // Get GPU utilization
        OptionalDouble utilization =
            scheduler.getUtilization();

        // Display utilization percentage or N/A
        if (utilization.isPresent()) {

            utilizationLabel.setText(
                String.format(
                    "GPU Utilization: %.2f%%",
                    utilization.getAsDouble()
                )
            );

        } else {

            utilizationLabel.setText(
                "GPU Utilization: N/A"
            );
        }

        // Count running and queued jobs
        int running = 0;
        int queued = 0;

        for (Job job : scheduler.getAllJobs()) {

            if ("Running".equals(job.getStatus())) {
                running++;
            }

            if ("Queued".equals(job.getStatus())) {
                queued++;
            }
        }

        // Update counters
        runningLabel.setText(
            "Running Jobs: " + running
        );

        queuedLabel.setText(
            "Queued Jobs: " + queued
        );

        // Update GPU status labels
        for (GPU gpu : scheduler.getGpus()) {

            int index = gpu.getId() - 1;

            if (gpu.isFree()) {

                gpuLabels[index].setText(
                    "GPU " + gpu.getId() + ": Free"
                );

            } else {

                gpuLabels[index].setText(
                    "GPU " + gpu.getId()
                        + ": Running "
                        + gpu.getCurrentJob().getJobId()
                );
            }
        }

        // Clear old job rows
        jobsModel.setRowCount(0);

        // Load all jobs
        for (Job job : scheduler.getAllJobs()) {

            // Show queue position for waiting jobs
            String position = "-";
            String wait = "-";

            if ("Queued".equals(job.getStatus())) {

                position = String.valueOf(
                    scheduler.getQueuePosition(job)
                );

                wait = scheduler.getRemainingWait(job)
                    + " min";
            }

            // Display assigned GPU or dash
            String gpuId = job.getAssignedGpuId() == 0
                ? "-"
                : String.valueOf(job.getAssignedGpuId());

            // Add job to table
            jobsModel.addRow(new Object[]{
                job.getJobId(),
                job.getStudentId(),
                job.getTitle(),
                job.getStatus(),
                gpuId,
                job.getEstimatedDuration() + " min",
                position,
                wait
            });
        }
    }

    // Updates the Student Search table
    public void refreshStudents() {

        // Clear old student information
        studentsModel.setRowCount(0);

        // Read registered students from scheduler
        for (Student student : scheduler.getStudents()) {

            // Get student's GPU allowance
            TimeAllowance allowance =
                student.getAllowance();

            // Add student information
            studentsModel.addRow(new Object[]{
                student.getStudentId(),
                student.getName(),
                student.getEmail(),
                student.getPhone(),
                allowance.getAllocated(),
                allowance.getUsed(),
                allowance.getReserved(),
                allowance.available()
            });
        }

        // Reapply the search filter
        filterStudents();
    }

    // Refreshes both administrator screens
    public void refreshAll() {

        refreshDashboard();
        refreshStudents();
    }
}