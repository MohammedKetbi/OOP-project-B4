
//import java.time package to use LocalDateTime object
import java.time.*;

public class SimulationClock {

    private LocalDateTime now;

    // simulation clock constructor to set the start time of the simulation clock
    public SimulationClock(LocalDateTime startTime) {
        now = startTime;
    }

    //advance method to advance the simulation clock by 1 minute
    public void advance() {
        now = now.plusMinutes(1);
    }
    


    //getter for SimulationClock to get the time of the simulation clock
    public LocalDateTime getTime() {
        return now;
    }
    

}
