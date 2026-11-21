
package ui;

import model.Administrator;
import model.Student;
import model.Job;
import model.GPU;
import model.TimeAllowance;
import model.AccessRequest;

import logic.Scheduler;
import logic.SimulationClock;
import logic.AppState;

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

    // Shared registration state and saving callback
    private AppState appState;
    private Runnable saveChanges;

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
    private JTextField studentSearchField;

    // Registration requests table
    private JTable requestsTable;
    private DefaultTableModel requestsModel;

    // Date and time format
    private final DateTimeFormatter dateFormat =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Original constructor for existing test classes
    public AdminDashboard(
            Administrator administrator,
            Scheduler scheduler,
            SimulationClock clock) {

        if (administrator == null ||
            scheduler == null ||
            clock == null) {

            throw new IllegalArgumentException(
                "Administrator, Scheduler and Clock are required"
            );
        }

        this.administrator = administrator;
        this.scheduler = scheduler;
        this.clock = clock;

        setLayout(new BorderLayout(10, 10));

        // Create dashboard tabs
        JTabbedPane tabs = new JTabbedPane();

        tabs.addTab("Dashboard", createDashboardPanel());
        tabs.addTab("Student Search", createStudentSearchPanel());

        add(tabs, BorderLayout.CENTER);

        // Refresh when switching tabs
        tabs.addChangeListener(e -> refreshAll());

        refreshAll();
    }

    // Constructor used by the integrated application
    public AdminDashboard(
            Administrator administrator,
            Scheduler scheduler,
            SimulationClock clock,
            AppState appState,
            Runnable saveChanges) {

        // Reuse the original dashboard constructor
        this(administrator, scheduler, clock);

        if (appState == null || saveChanges == null) {
            throw new IllegalArgumentException(
                "Application state and save callback are required."
            );
        }

        if (appState.getScheduler() != scheduler ||
            appState.getAdministrator() != administrator ||
            appState.getClock() != clock) {

            throw new IllegalArgumentException(
                "Dashboard must use the shared application state."
            );
        }

        this.appState = appState;
        this.saveChanges = saveChanges;

        // Add Registration Requests tab
        JTabbedPane tabs = (JTabbedPane) getComponent(0);

        tabs.addTab(
            "Registration Requests",
            createRequestsPanel()
        );

        refreshRequests();
    }

    // Saves changes through the application controller
    private void saveDashboardChanges() {

        if (saveChanges != null) {
            saveChanges.run();
        }
    }

    // Creates the administrator dashboard
    private JPanel createDashboardPanel() {

        JPanel panel = new JPanel(
            new BorderLayout(10, 10)
        );

        JLabel title = new JLabel(
            "Administrator Dashboard",
            SwingConstants.CENTER
        );

        panel.add(title, BorderLayout.NORTH);

        // Dashboard information
        JPanel informationPanel = new JPanel(
            new GridLayout(2, 2, 10, 10)
        );

        clockLabel = new JLabel();
        utilizationLabel = new JLabel();
        runningLabel = new JLabel();
        queuedLabel = new JLabel();

        informationPanel.add(clockLabel);
        informationPanel.add(utilizationLabel);
        informationPanel.add(runningLabel);
        informationPanel.add(queuedLabel);

        // GPU status panel
        JPanel gpuPanel = new JPanel(
            new GridLayout(2, 2, 10, 10)
        );

        gpuPanel.setBorder(
            BorderFactory.createTitledBorder("GPU Status")
        );

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

        // Job table columns
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

        JScrollPane jobsScroll = new JScrollPane(jobsTable);

        jobsScroll.setBorder(
            BorderFactory.createTitledBorder("All Jobs")
        );

        JPanel centerPanel = new JPanel(
            new BorderLayout(10, 10)
        );

        centerPanel.add(topPanel, BorderLayout.NORTH);
        centerPanel.add(jobsScroll, BorderLayout.CENTER);

        panel.add(centerPanel, BorderLayout.CENTER);

        // Dashboard buttons
        JButton advanceButton = new JButton(
            "Advance 1 Minute"
        );

        JButton refreshButton = new JButton("Refresh");
        JButton cancelButton = new JButton("Cancel Job");

        JPanel buttonPanel = new JPanel();

        buttonPanel.add(advanceButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(cancelButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Advance simulation
        advanceButton.addActionListener(e -> {

            try {

                scheduler.advanceMinute();

                // Save the updated simulation time
                saveDashboardChanges();

                refreshAll();

            } catch (RuntimeException ex) {

                refreshAll();
                Dialog.showError(this, ex.getMessage());
            }
        });

        refreshButton.addActionListener(
            e -> refreshAll()
        );

        cancelButton.addActionListener(
            e -> cancelSelectedJob()
        );

        return panel;
    }

    // Creates the Student Search screen
    private JPanel createStudentSearchPanel() {

        JPanel panel = new JPanel(
            new BorderLayout(10, 10)
        );

        JLabel title = new JLabel(
            "Search Registered Students",
            SwingConstants.CENTER
        );

        // Student table columns
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

        studentsModel = new DefaultTableModel(columns, 0) {

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        studentsTable = new JTable(studentsModel);

        studentsTable.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );

        studentsSorter =
            new TableRowSorter<>(studentsModel);

        studentsTable.setRowSorter(studentsSorter);

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

        JPanel topPanel = new JPanel(
            new BorderLayout(10, 10)
        );

        topPanel.add(title, BorderLayout.NORTH);
        topPanel.add(searchPanel, BorderLayout.SOUTH);

        panel.add(topPanel, BorderLayout.NORTH);

        panel.add(
            new JScrollPane(studentsTable),
            BorderLayout.CENTER
        );

        // Student action buttons
        JButton refreshButton = new JButton(
            "Refresh Students"
        );

        // NEW: Administrator can grant additional GPU minutes
        JButton grantButton = new JButton(
            "Grant Extra Time"
        );

        JPanel buttonsPanel = new JPanel();

        buttonsPanel.add(refreshButton);
        buttonsPanel.add(grantButton);

        panel.add(buttonsPanel, BorderLayout.SOUTH);

        // Search whenever text changes
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

        refreshButton.addActionListener(
            e -> refreshStudents()
        );

        // NEW: Open the Grant Extra Time dialog
        grantButton.addActionListener(
            e -> grantExtraTime()
        );

        return panel;
    }

    // NEW: Grant additional GPU minutes to a selected student
    private void grantExtraTime() {

        // Require the administrator to select a student
        int selectedRow = studentsTable.getSelectedRow();

        if (selectedRow == -1) {
            Dialog.showError(
                this,
                "Please select a student first."
            );
            return;
        }

        // Convert the displayed row to the actual model row
        // This is necessary because the table supports sorting
        int modelRow =
            studentsTable.convertRowIndexToModel(selectedRow);

        String studentId =
            studentsModel.getValueAt(modelRow, 0).toString();

        // Find the original registered Student object
        Student selectedStudent = null;

        for (Student student : scheduler.getStudents()) {

            if (student.getStudentId().equals(studentId)) {
                selectedStudent = student;
                break;
            }
        }

        if (selectedStudent == null) {
            Dialog.showError(this, "Student not found.");
            return;
        }

        // Ask the administrator for additional minutes
        String minutesInput = JOptionPane.showInputDialog(
            this,
            "Enter additional GPU minutes for student "
                + studentId + ":",
            "Grant Extra Time",
            JOptionPane.QUESTION_MESSAGE
        );

        // Cancel closes the dialog without changing anything
        if (minutesInput == null) {
            return;
        }

        minutesInput = minutesInput.trim();

        if (minutesInput.isEmpty()) {
            Dialog.showError(
                this,
                "Please enter the number of minutes."
            );
            return;
        }

        int minutes;

        try {
            // Accept positive whole numbers only
            minutes = Integer.parseInt(minutesInput);

        } catch (NumberFormatException ex) {

            Dialog.showError(
                this,
                "Minutes must be a valid whole number."
            );
            return;
        }

        if (minutes <= 0) {
            Dialog.showError(
                this,
                "Granted minutes must be positive."
            );
            return;
        }

        // Ask for the reason, which must be recorded
        String reason = JOptionPane.showInputDialog(
            this,
            "Enter the reason for granting "
                + minutes + " minutes:",
            "Grant Reason",
            JOptionPane.QUESTION_MESSAGE
        );

        if (reason == null) {
            return;
        }

        reason = reason.trim();

        if (reason.isEmpty()) {
            Dialog.showError(
                this,
                "A reason is required."
            );
            return;
        }

        // Confirm the administrator's decision
        int choice = JOptionPane.showConfirmDialog(
            this,
            "Grant " + minutes
                + " GPU minutes to student "
                + studentId
                + "?\nReason: " + reason,
            "Confirm Time Grant",
            JOptionPane.YES_NO_OPTION
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {

            // Use existing Administrator business logic
            administrator.grantTime(
                selectedStudent,
                minutes,
                reason
            );

            // Save the updated allowance and grant record
            // before displaying a success message
            saveDashboardChanges();

            // Refresh all administrator tables
            refreshAll();

            JOptionPane.showMessageDialog(
                this,
                minutes + " GPU minutes granted to student "
                    + studentId + " successfully.",
                "Grant Successful",
                JOptionPane.INFORMATION_MESSAGE
            );

        } catch (RuntimeException ex) {

            refreshAll();

            Dialog.showError(
                this,
                ex.getMessage() == null
                    ? "Could not grant extra time."
                    : ex.getMessage()
            );
        }
    }

    // Filters students by ID or name
    private void filterStudents() {

        String search = studentSearchField.getText()
            .trim().toLowerCase();

        studentsSorter.setRowFilter(
            new RowFilter<DefaultTableModel, Integer>() {

                @Override
                public boolean include(
                    Entry<? extends DefaultTableModel,
                          ? extends Integer> entry) {

                    String id = entry.getStringValue(0)
                        .toLowerCase();

                    String name = entry.getStringValue(1)
                        .toLowerCase();

                    return id.contains(search)
                        || name.contains(search);
                }
            }
        );
    }

    // Creates the Registration Requests screen
    private JPanel createRequestsPanel() {

        JPanel panel = new JPanel(
            new BorderLayout(10, 10)
        );

        JLabel title = new JLabel(
            "Student Registration Requests",
            SwingConstants.CENTER
        );

        panel.add(title, BorderLayout.NORTH);

        // Registration table columns
        String[] columns = {
            "Student ID",
            "Name",
            "Email",
            "Phone",
            "Status"
        };

        requestsModel = new DefaultTableModel(columns, 0) {

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        requestsTable = new JTable(requestsModel);

        requestsTable.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );

        panel.add(
            new JScrollPane(requestsTable),
            BorderLayout.CENTER
        );

        // Registration action buttons
        JButton approveButton = new JButton("Approve");
        JButton rejectButton = new JButton("Reject");
        JButton refreshButton = new JButton("Refresh");

        JPanel buttonPanel = new JPanel();

        buttonPanel.add(approveButton);
        buttonPanel.add(rejectButton);
        buttonPanel.add(refreshButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        approveButton.addActionListener(
            e -> processRequest(true)
        );

        rejectButton.addActionListener(
            e -> processRequest(false)
        );

        refreshButton.addActionListener(
            e -> refreshRequests()
        );

        return panel;
    }

    // Approves or rejects a registration request
    private void processRequest(boolean approve) {

        int selectedRow = requestsTable.getSelectedRow();

        if (selectedRow == -1) {

            Dialog.showError(
                this,
                "Please select a registration request."
            );
            return;
        }

        int modelRow =
            requestsTable.convertRowIndexToModel(selectedRow);

        int requestId = Integer.parseInt(
            requestsModel.getValueAt(modelRow, 0).toString()
        );

        // Find the original request
        AccessRequest selectedRequest = null;

        for (AccessRequest request : appState.getRequests()) {

            if (request.getId() == requestId) {
                selectedRequest = request;
                break;
            }
        }

        if (selectedRequest == null) {

            Dialog.showError(this, "Request not found.");
            return;
        }

        if (!"Pending".equals(selectedRequest.getStatus())) {

            Dialog.showError(
                this,
                "This request has already been processed."
            );
            return;
        }

        String action = approve ? "approve" : "reject";

        // Confirm administrator decision
        int choice = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to " + action
                + " this request?",
            "Confirm Registration",
            JOptionPane.YES_NO_OPTION
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {

            if (approve) {

                // Use the student's original password
                administrator.approve(
                    selectedRequest,
                    selectedRequest.getPasswordForStorage()
                );

            } else {

                administrator.reject(selectedRequest);
            }

            // Save updated registration state
            saveDashboardChanges();

            // Refresh all tables
            refreshAll();

            JOptionPane.showMessageDialog(
                this,
                approve
                    ? "Student approved successfully."
                    : "Registration rejected successfully.",
                "Registration Updated",
                JOptionPane.INFORMATION_MESSAGE
            );

        } catch (RuntimeException ex) {

            refreshAll();
            Dialog.showError(this, ex.getMessage());
        }
    }

    // Loads registration requests from AppState
    public void refreshRequests() {

        if (appState == null || requestsModel == null) {
            return;
        }

        requestsModel.setRowCount(0);

        for (AccessRequest request : appState.getRequests()) {

            requestsModel.addRow(new Object[]{
                request.getId(),
                request.getName(),
                request.getEmail(),
                request.getPhone(),
                request.getStatus()
            });
        }
    }

    // Cancels a selected job
    private void cancelSelectedJob() {

        int selectedRow = jobsTable.getSelectedRow();

        if (selectedRow == -1) {

            Dialog.showError(
                this,
                "Please select a job first."
            );
            return;
        }

        String jobId = jobsTable.getValueAt(
            selectedRow, 0
        ).toString();

        // Find the actual job
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

            Dialog.showError(
                this,
                "Only queued or running jobs can be cancelled."
            );
            return;
        }

        // Ask for cancellation reason
        String reason = JOptionPane.showInputDialog(
            this,
            "Reason for cancelling job " + jobId + ":"
        );

        if (reason == null) {
            return;
        }

        reason = reason.trim();

        if (reason.isEmpty()) {

            Dialog.showError(
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

            // Cancel through existing administrator logic
            administrator.cancelJob(selectedJob, reason);

            // Save cancellation
            saveDashboardChanges();

            refreshAll();

            JOptionPane.showMessageDialog(
                this,
                "Job " + jobId + " cancelled successfully.",
                "Cancellation Successful",
                JOptionPane.INFORMATION_MESSAGE
            );

        } catch (RuntimeException ex) {

            refreshAll();
            Dialog.showError(this, ex.getMessage());
        }
    }

    // Updates dashboard information
    public void refreshDashboard() {

        clockLabel.setText(
            "Simulation Time: "
                + clock.getTime().format(dateFormat)
        );

        // Calculate GPU utilization
        OptionalDouble utilization =
            scheduler.getUtilization();

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

        runningLabel.setText(
            "Running Jobs: " + running
        );

        queuedLabel.setText(
            "Queued Jobs: " + queued
        );

        // Display GPU statuses
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

        // Clear previous job rows
        jobsModel.setRowCount(0);

        // Load all jobs
        for (Job job : scheduler.getAllJobs()) {

            String position = "-";
            String wait = "-";

            if ("Queued".equals(job.getStatus())) {

                position = String.valueOf(
                    scheduler.getQueuePosition(job)
                );

                wait = scheduler.getRemainingWait(job)
                    + " min";
            }

            String gpuId = job.getAssignedGpuId() == 0
                ? "-"
                : String.valueOf(job.getAssignedGpuId());

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

    // Updates Student Search table
    public void refreshStudents() {

        studentsModel.setRowCount(0);

        for (Student student : scheduler.getStudents()) {

            TimeAllowance allowance =
                student.getAllowance();

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

        filterStudents();
    }

    // Refreshes all administrator screens
    public void refreshAll() {

        refreshDashboard();
        refreshStudents();
        refreshRequests();
    }
}
