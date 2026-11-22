
package UI;

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
    private Runnable logoutAction;
    private JButton logoutButton;

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
        logoutButton = new JButton("Log out");
        logoutButton.setEnabled(false);
        JPanel logoutPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        logoutPanel.add(logoutButton);
        add(logoutPanel, BorderLayout.NORTH);
        logoutButton.addActionListener(e -> logout());
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
        JTabbedPane tabs = (JTabbedPane) getComponent(0);
        tabs.addTab("Registration Requests", createRequestsPanel());
        refreshRequests();
    }
    public void setLogoutAction(Runnable logoutAction) {
        if (logoutAction == null) {
            throw new IllegalArgumentException("Logout action is required.");
        }
        this.logoutAction = logoutAction;
        logoutButton.setEnabled(true);
    }
    private void logout() {
        if (logoutAction == null) {
            return;
        }
        logoutButton.setEnabled(false);
        try {
            saveDashboardChanges();
            logoutAction.run();
        } catch (RuntimeException ex) {
            Dialog.showError(this, "Could not log out: " + ex.getMessage());
        } finally {
            logoutButton.setEnabled(true);
        }
    }
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
            public boolean isCellEditable(
                    int row, int column) {
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
            public boolean isCellEditable(
                    int row, int column) {
                return false;
            }
        };

        studentsTable = new JTable(studentsModel);

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

        JButton refreshButton = new JButton(
            "Refresh Students"
        );

        // Keep refresh and time grants together at the bottom of Student Search.
        JButton grantTimeButton = new JButton("Grant Time");
        JPanel studentActions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        studentActions.add(refreshButton);
        studentActions.add(grantTimeButton);
        panel.add(studentActions, BorderLayout.SOUTH);
        studentsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        grantTimeButton.addActionListener(e -> grantTimeToSelectedStudent());

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

        return panel;
    }

    // Resolve the selected VIEW row to its MODEL row. This remains correct
    // when the administrator filters the table or sorts a column.
    private Student getSelectedStudentForGrant() {
        int viewRow = studentsTable.getSelectedRow();
        if (viewRow < 0) {
            throw new IllegalArgumentException("Please select a student first.");
        }
        int modelRow = studentsTable.convertRowIndexToModel(viewRow);
        String studentId = studentsModel.getValueAt(modelRow, 0).toString();
        for (Student student : scheduler.getStudents()) {
            if (student.getStudentId().equals(studentId)) {
                return student;
            }
        }
        throw new IllegalArgumentException("The selected student could not be found.");
    }

    // Validate before changing the allowance. Reject decimals, zero, negative
    // values and numbers that cannot be represented as an int.
    private int readGrantMinutes(String text) {
        if (text == null || !text.trim().matches("[0-9]+")) {
            throw new IllegalArgumentException("Enter a positive whole number of minutes.");
        }
        try {
            int minutes = Integer.parseInt(text.trim());
            if (minutes <= 0) {
                throw new IllegalArgumentException("Minutes must be greater than zero.");
            }
            return minutes;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("The number of minutes is too large.");
        }
    }

    // Use the existing domain method: it updates allocated minutes and records
    // the student, administrator, simulation time and reason in grant history.
    private void applyTimeGrant(Student student, int minutes, String reason) {
        if (student == null || !scheduler.getStudents().contains(student)) {
            throw new IllegalArgumentException("A registered student is required.");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("A reason is required.");
        }
        administrator.grantTime(student, minutes, reason.trim());
    }

    private void grantTimeToSelectedStudent() {
        Student selectedStudent;
        try {
            selectedStudent = getSelectedStudentForGrant();
        } catch (IllegalArgumentException ex) {
            Dialog.showError(this, ex.getMessage());
            return;
        }

        JTextField minutesField = new JTextField(12);
        JTextArea reasonField = new JTextArea(4, 28);
        reasonField.setLineWrap(true);
        reasonField.setWrapStyleWord(true);

        JPanel form = new JPanel(new BorderLayout(8, 8));
        JPanel details = new JPanel(new GridLayout(0, 1, 4, 4));
        details.add(new JLabel("Student: " + selectedStudent.getName()
            + " (" + selectedStudent.getStudentId() + ")"));
        details.add(new JLabel("Extra GPU minutes:"));
        details.add(minutesField);
        details.add(new JLabel("Reason (required):"));
        form.add(details, BorderLayout.NORTH);
        form.add(new JScrollPane(reasonField), BorderLayout.CENTER);

        while (true) {
            int choice = JOptionPane.showConfirmDialog(
                this, form, "Grant Extra GPU Time",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
            );
            if (choice != JOptionPane.OK_OPTION) {
                return; // Cancelling the dialog changes nothing.
            }

            int minutes;
            try {
                minutes = readGrantMinutes(minutesField.getText());
                applyTimeGrant(selectedStudent, minutes, reasonField.getText());
            } catch (RuntimeException ex) {
                Dialog.showError(this, ex.getMessage());
                continue; // Keep the inputs so the administrator can correct them.
            }

            // The domain change succeeded. Refresh balances, then save through
            // the SAME callback used by approvals, cancellations and clock advances.
            refreshAll();
            try {
                saveDashboardChanges();
            } catch (RuntimeException ex) {
                // Do not reopen the grant form: submitting it again would add
                // the same minutes a second time. The in-memory grant remains.
                Dialog.showError(this,
                    "The " + minutes + " minutes were added in this session, "
                    + "but saving failed. Do not grant them again. Retry saving "
                    + "before closing the application.\n" + ex.getMessage());
                return;
            }

            JOptionPane.showMessageDialog(
                this,
                minutes + " GPU minutes granted to " + selectedStudent.getName()
                    + ".\nAllocated: " + selectedStudent.getAllowance().getAllocated()
                    + "\nAvailable: " + selectedStudent.getAllowance().available(),
                "Time Granted", JOptionPane.INFORMATION_MESSAGE
            );
            return;
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
            public boolean isCellEditable(
                    int row, int column) {
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
