package UI;
import java.time.format.DateTimeFormatter;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import logic.SimulationClock;

public class ClockSetupTestDriver {

    public static void main(String[] args) {

        // Swing windows should be created on Swing's event thread.
        SwingUtilities.invokeLater(() -> {

            ClockSetUpScreen3 screen =
                new ClockSetUpScreen3(selectedTime -> {

                    // Test only: create a new clock from this selection.
                    // Nothing is saved or loaded between runs.
                    SimulationClock clock =
                        new SimulationClock(selectedTime);

                    DateTimeFormatter format =
                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

                    String message =
                        "Simulation clock created successfully!\n\n"
                        + "Selected time: "
                        + clock.getTime().format(format);

                    System.out.println(message);

                    JOptionPane.showMessageDialog(
                        null,
                        message,
                        "Clock Setup Test",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                });

            screen.setVisible(true);
        });
    }
}