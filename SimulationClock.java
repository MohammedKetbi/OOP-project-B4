public class SimulationClock {
    private int hours;
    private int minutes;

    public SimulationClock(int hours, int minutes) {
        this.hours = hours;
        this.minutes = minutes;
    }

    public int getHours() {
        return hours;
    }

    public int getMinutes() {
        return minutes;
    }
}
