import java.awt.*;
import java.text.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import model.Student;

// Registration requests screen
public class AdminRequest extends JPanel {
    // Shared data and saving callback
    private final Administrator administrator;
    private final List<AccessRequest> requests;
    private final Map<Integer, String> passwords;
    private final List<Student> students;
    private final Runnable saveChanges;
        // Requests currently displayed
    private final List<AccessRequest> visibleRequests = new ArrayList<>();
    private AccessRequest lastSelection;
        // Filter requests by status
    private final JComboBox<String> statusFilter = new JComboBox<>(new String[]{"Pending","Approved","Rejected","All"});
        // Table columns and display rules
    private final DefaultTableModel tableModel = new DefaultTableModel(new String[]{"University ID","Name","Email","Phone","Password","Status"},0){
    @Override
    public boolean isCellEditable(int row, int column) {
        // Prevent direct table editing
        return false;
    }

    @Override
    public Class<?> getColumnClass(int column) {
        // Sort university IDs as numbers

        return column == 0 ? Integer.class : String.class;
    }
    };
    // Table and approval controls

    private final JTable requestTable = new JTable(tableModel);
    private final JLabel selectedLabel = new JLabel("Select a request.");
    private final JSpinner initialMinutes = new JSpinner(new SpinnerNumberModel(100,1,Integer.MAX_VALUE,1));
    private final JButton approveButton = new JButton("Approve");
    private final JButton rejectButton = new JButton("Reject");
// Create the panel without a saving callback
public AdminRequest(Administrator administrator,
    List<AccessRequest> requests,
    Map<Integer, String> passwords,
    List<Student> students) {
        this(administrator, requests, passwords, students, () -> { });
    }
    // Create the panel with shared data and a saving callback
    public AdminRequest(Administrator administrator,
        List<AccessRequest> requests,
        Map<Integer, String> passwords,
        List<Student> students,
        Runnable saveChanges) {
            this.administrator = Objects.requireNonNull(administrator);
            this.requests = Objects.requireNonNull(requests);
            this.passwords = Objects.requireNonNull(passwords);
            this.students = Objects.requireNonNull(students);
            this.saveChanges = Objects.requireNonNull(saveChanges);
            // Arrange the screen
            setLayout(new BorderLayout(12, 12));
            setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
            // Create the heading and filter
            JLabel title = new JLabel("Registration requests");
            title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
            JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            filterPanel.add(new JLabel("Show:"));
            filterPanel.add(statusFilter);
            JPanel header = new JPanel(new BorderLayout());
            header.add(title, BorderLayout.WEST);
            header.add(filterPanel, BorderLayout.EAST);
            add(header, BorderLayout.NORTH);
            // Configure the requests table
            requestTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            requestTable.setAutoCreateRowSorter(true);
            requestTable.setFillsViewportHeight(true);
            requestTable.setRowHeight(28);
            requestTable.getTableHeader().setReorderingAllowed(false);
            add(new JScrollPane(requestTable), BorderLayout.CENTER);
            // Configure the initial allowance input
            JSpinner.NumberEditor minutesEditor =
            new JSpinner.NumberEditor(initialMinutes, "0");
            initialMinutes.setEditor(minutesEditor);
            minutesEditor.getTextField().setColumns(8);
            JPanel allowancePanel = new JPanel(new BorderLayout(0, 8));
            allowancePanel.add(new JLabel("Initial GPU minutes"), BorderLayout.NORTH);
            allowancePanel.add(initialMinutes, BorderLayout.CENTER);
            // Create the approval buttons
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            approveButton.setBackground(new Color(33, 113, 181));
            approveButton.setForeground(Color.WHITE);
            rejectButton.setBackground(new Color(211, 95, 0));
            rejectButton.setForeground(Color.WHITE);
            buttons.add(approveButton);
            buttons.add(rejectButton);
            JPanel controls = new JPanel(new BorderLayout(12, 0));
            controls.add(allowancePanel, BorderLayout.WEST);
            controls.add(buttons, BorderLayout.EAST);
            selectedLabel.setFont(selectedLabel.getFont().deriveFont(Font.BOLD));
            // Group the selection and action controls
            JPanel footer = new JPanel(new BorderLayout(0, 12));
            footer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(190, 200, 210)),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
            ));
        footer.add(selectedLabel, BorderLayout.NORTH);
        footer.add(controls, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
// Connect interface events
statusFilter.addActionListener(event -> refreshRequests());
requestTable.getSelectionModel().addListSelectionListener(event -> {
    if (!event.getValueIsAdjusting()) {
        updateSelection();
    }
});
approveButton.addActionListener(event -> approveSelected());
rejectButton.addActionListener(event -> rejectSelected());
refreshRequests();
        }
    // Reload requests matching the filter
    public void refreshRequests() {
        requestTable.clearSelection();
        tableModel.setRowCount(0);
        visibleRequests.clear();
        String filter = (String) statusFilter.getSelectedItem();
        for (AccessRequest request : requests) {
            if ("All".equals(filter) || filter.equals(request.getStatus())) {
                visibleRequests.add(request);
                tableModel.addRow(new Object[] {
                        request.getId(), request.getName(), request.getEmail(),
                        request.getPhone(), "******", request.getStatus() });
            }
        }
        updateSelection();
    }
    // Find the request in the sorted table
    private AccessRequest selectedRequest() {
        int row = requestTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return visibleRequests.get(requestTable.convertRowIndexToModel(row));
    }
    // Enable actions for pending requests
    private void updateSelection() {
        AccessRequest request = selectedRequest();
        boolean pending = request != null && "Pending".equals(request.getStatus());
        if (request != lastSelection) {
            initialMinutes.setValue(100);
            lastSelection = request;
        }
        approveButton.setEnabled(pending);
        rejectButton.setEnabled(pending);
        initialMinutes.setEnabled(pending);
        selectedLabel.setText(request == null ? "Select a request."
                : "Selected: " + request.getName() + " ("
                        + request.getId() + ")");
    }
    // Approve the selected request
    private void approveSelected() {
        AccessRequest request = selectedRequest();
        if (request == null) {
            showError("Select a pending request first.");
            return;
        }
        try {
            String text = ((JSpinner.NumberEditor) initialMinutes.getEditor())
                    .getTextField().getText().trim();
            if (!text.matches("[0-9]+")) {
                showError("Enter a positive whole number of GPU minutes.");
                return;
            }
            // Read the validated allowance
            initialMinutes.commitEdit();
            int minutes = ((Number) initialMinutes.getValue()).intValue();
            String password = passwords.get(request.getId());
            // Create the student account
            Student student = administrator.approve(request, password, minutes);
            students.add(student);
        } catch (ParseException exception) {
            showError("Enter a positive whole number of GPU minutes.");
            return;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showError(exception.getMessage());
            return;
        }
        afterChange("Student account approved.");
    }
    // Reject the selected request
    private void rejectSelected() {
        AccessRequest request = selectedRequest();
        if (request == null) {
            showError("Select a pending request first.");
            return;
        }
        try {
            administrator.reject(request);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showError(exception.getMessage());
            return;
        }
        afterChange("Registration request rejected.");
    }
    // Refresh, run the saving callback, and report success
    private void afterChange(String message) {
        refreshRequests();
        try {
            saveChanges.run();
        } catch (RuntimeException exception) {
            showError(message + "\nThe changes could not be saved. "
                    + exception.getMessage());
            return;
        }
        JOptionPane.showMessageDialog(this, message,
                "Registration requests", JOptionPane.INFORMATION_MESSAGE);
    }
    // Display an error dialog
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message,
                "Registration requests", JOptionPane.ERROR_MESSAGE);
    }
    // Launch a demonstration window
    public static void main(String[] args) {
                        // Create the interface on the Swing event thread
                        SwingUtilities.invokeLater(() -> {
                            SimulationClock clock = new SimulationClock(LocalDateTime.of(2026, 11, 16, 10, 20)
                            );
                        // Create the simulation objects
                        GPU[] gpus = {new GPU(1), new GPU(2), new GPU(3), new GPU(4)};
                    Scheduler scheduler = new Scheduler(clock, gpus);
                Administrator administrator = new Administrator(scheduler, clock);
            // Sample registration requests
            List<AccessRequest> requests = new ArrayList<>();
        requests.add(new AccessRequest(100061477, "Omar Khalid",
            "omar.k@ku.ac.ae", "0501234567"));
            requests.add(new AccessRequest(100063052, "Lina Hassan",
                "lina.h@ku.ac.ae", "0559876543"));
                // Sample registration passwords
                Map<Integer, String> passwords = new HashMap<>();
                passwords.put(100061477, "omar123");
                passwords.put(100063052, "lina123");
                List<Student> students = new ArrayList<>();
                JTabbedPane tabs = new JTabbedPane();
                // Demo callback updates the pending count
                AdminRequest panel = new AdminRequest(administrator,requests, passwords, students, () -> {
                    long pending = requests.stream()
                    .filter(request -> "Pending".equals(request.getStatus()))
                    .count();
                    tabs.setTitleAt(0, "Requests (" + pending + ")");
                });
                tabs.addTab("Requests (2)", panel);
                DateTimeFormatter format =
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                JLabel timeLabel = new JLabel(
                    "Simulated time: " + clock.getTime().format(format)
                );
                // Advance simulation time manually
                JButton advanceButton = new JButton("+1 minute");
                advanceButton.addActionListener(event -> {
                    try {
                        scheduler.advanceMinute();
                        timeLabel.setText("Simulated time: "
                            + clock.getTime().format(format));
                            panel.refreshRequests();
                    } catch (RuntimeException exception) {
                        panel.showError(exception.getMessage());
                    }
                });
                JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                timePanel.add(timeLabel);
                timePanel.add(advanceButton);
                JLabel adminLabel = new JLabel("Administrator");
                adminLabel.setFont(adminLabel.getFont().deriveFont(Font.BOLD, 20f));
                JPanel heading = new JPanel(new BorderLayout());
                heading.add(adminLabel, BorderLayout.WEST);
                heading.add(timePanel, BorderLayout.EAST);
                JPanel window = new JPanel(new BorderLayout(0, 12));
                window.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
                window.add(heading, BorderLayout.NORTH);
                window.add(tabs,BorderLayout.CENTER);
                // Create and show the window
                JFrame frame = new JFrame("GPU Utilization Management");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setContentPane(window);
                frame.setSize(1050,520);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                });
}
}