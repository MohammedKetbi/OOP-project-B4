package ui;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import javax.swing.*;

public class ClockSetUpScreen3 extends JFrame {

    private final JSpinner datePicker;
    private final JSpinner timePicker;
    private final JButton startButton;

    // Prevent repeated initialization from this screen.
    private boolean clockStarted = false;

    /*
     * The application supplies an implementation of this interface.
     * It creates the shared clock/scheduler, saves the initial state,
     * and opens the administrator dashboard.
     *
     * Keeping this separate prevents GUI code from managing jobs,
     * allowances, or file storage.
     */
    public interface ClockStartHandler {
        void startClock(LocalDateTime selectedTime) throws Exception;
    }

    public ClockSetUpScreen3(ClockStartHandler handler) {

        if (handler == null) {
            throw new IllegalArgumentException(
                "A clock start handler is required."
            );
        }

        setTitle("Start the simulation");

        // Closing setup must not exit the whole application.
        // The controller must continue blocking jobs until setup succeeds.
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout(0, 20));
        content.setBorder(
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        );

        // Instructions at the top.
        JPanel headingPanel = new JPanel();
        headingPanel.setLayout(
            new BoxLayout(headingPanel, BoxLayout.Y_AXIS)
        );

        JLabel titleLabel = new JLabel(
            "Set the starting date and time"
        );

        titleLabel.setFont(
            titleLabel.getFont().deriveFont(Font.BOLD, 18f)
        );

        JLabel instructionLabel = new JLabel(
            "Set once. Afterwards, time moves only with +1 minute."
        );

        headingPanel.add(titleLabel);
        headingPanel.add(Box.createVerticalStrut(8));
        headingPanel.add(instructionLabel);

        content.add(headingPanel, BorderLayout.NORTH);

        /*
         * The computer date/time is only the initial picker display.
         * No simulation clock exists until the user confirms a selection.
         */
        Calendar initial = Calendar.getInstance();
        initial.set(Calendar.SECOND, 0);
        initial.set(Calendar.MILLISECOND, 0);

        Date initialValue = initial.getTime();

        // Date picker: arrows adjust the selected date field.
        datePicker = new JSpinner(
            new SpinnerDateModel(
                initialValue,
                null,
                null,
                Calendar.DAY_OF_MONTH
            )
        );

        JSpinner.DateEditor dateEditor =
            new JSpinner.DateEditor(datePicker, "dd/MM/yyyy");

        datePicker.setEditor(dateEditor);
        configurePicker(dateEditor);

        // Time picker: display hours and minutes in 24-hour format.
        timePicker = new JSpinner(
            new SpinnerDateModel(
                initialValue,
                null,
                null,
                Calendar.MINUTE
            )
        );

        JSpinner.DateEditor timeEditor =
            new JSpinner.DateEditor(timePicker, "HH:mm");

        timePicker.setEditor(timeEditor);
        configurePicker(timeEditor);

        JPanel pickerPanel = new JPanel(
            new GridLayout(2, 2, 16, 8)
        );

        pickerPanel.add(new JLabel("Date"));
        pickerPanel.add(new JLabel("Time (24-hour)"));
        pickerPanel.add(datePicker);
        pickerPanel.add(timePicker);

        content.add(pickerPanel, BorderLayout.CENTER);

        startButton = new JButton("Start Clock");

        startButton.addActionListener(event -> {
            startClock(handler);
        });

        JPanel buttonPanel = new JPanel(
            new FlowLayout(FlowLayout.RIGHT, 0, 0)
        );

        buttonPanel.add(startButton);

        content.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(content);
        getRootPane().setDefaultButton(startButton);

        pack();
        setMinimumSize(new Dimension(560, 240));
        setSize(600, 250);
        setLocationRelativeTo(null);
    }

    private void configurePicker(JSpinner.DateEditor editor) {

        // Reject invalid date values rather than silently adjusting them.
        editor.getFormat().setLenient(false);

        // Project requirement: use pickers rather than free text.
        // Users select a field and use the spinner arrows.
        editor.getTextField().setEditable(false);
        editor.getTextField().setColumns(12);
    }

    private LocalDateTime getSelectedTime() {

        Date selectedDate = (Date) datePicker.getValue();
        Date selectedTime = (Date) timePicker.getValue();

        // Both pickers display values in the computer's local time zone.
        ZoneId zone = ZoneId.systemDefault();

        LocalDateTime datePart =
            LocalDateTime.ofInstant(selectedDate.toInstant(), zone);

        LocalDateTime timePart =
            LocalDateTime.ofInstant(selectedTime.toInstant(), zone);

        // Take the calendar date from one picker and time from the other.
        // Seconds and nanoseconds are zero: simulation precision is minutes.
        return LocalDateTime.of(
            datePart.toLocalDate(),
            timePart.toLocalTime().withSecond(0).withNano(0)
        );
    }

    private void startClock(ClockStartHandler handler) {

        if (clockStarted) {
            return;
        }

        startButton.setEnabled(false);

        try {
            LocalDateTime selectedTime = getSelectedTime();

            /*
             * The application must reject initialization if a clock
             * already exists. It then creates the shared objects,
             * handles initial saving, and opens the admin dashboard.
             */
            handler.startClock(selectedTime);

            // Close only after initialization succeeds.
            clockStarted = true;
            dispose();

        } catch (Exception exception) {

            // Show validation, initialization, or file errors in the GUI.
            String message = exception.getMessage();

            if (message == null || message.trim().isEmpty()) {
                message = "The simulation clock could not be initialized.";
            }

            Dialog.showError(this, message);

            startButton.setEnabled(true);
        }
    }
}