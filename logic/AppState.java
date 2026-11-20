package logic;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.*;
import model.*;

/** Shared objects to pass to GUI screens later. This class does no file I/O. */
public class AppState {
    private Scheduler scheduler;
    private final List<AccessRequest> requests = new ArrayList<>();
    private BigInteger nextJobId = BigInteger.valueOf(101);

    // A new/empty state has no clock. Keep submissions disabled until setup.
    public boolean isClockSet() { return scheduler != null; }
    public Scheduler getScheduler() { return scheduler; }
    public SimulationClock getClock() {
        return isClockSet() ? scheduler.getClock() : null;
    }
    public Administrator getAdministrator() {
        return isClockSet() ? scheduler.getAdministrator() : null;
    }
    public List<Student> getStudents() {
        return isClockSet() ? scheduler.getStudents() : Collections.emptyList();
    }
    public List<AccessRequest> getRequests() {
        return Collections.unmodifiableList(new ArrayList<>(requests));
    }
    public void addRequest(AccessRequest request) {
        if (request == null) throw new IllegalArgumentException("Request is required.");
        for (AccessRequest existing : requests) {
            if (existing.getId() == request.getId()) {
                throw new IllegalArgumentException("Duplicate registration ID.");
            }
        }
        for (Student student : getStudents()) {
            if (student.getStudentId().equals(String.valueOf(request.getId()))) {
                throw new IllegalArgumentException("Student is already registered.");
            }
        }
        requests.add(request);
    }
    public void initializeClock(LocalDateTime selectedTime) {
        if (isClockSet()) throw new IllegalStateException("Clock is already set.");
        if (selectedTime == null || selectedTime.getSecond() != 0 || selectedTime.getNano() != 0) {
            throw new IllegalArgumentException("Select a date/time with minute precision.");
        }
        SimulationClock clock = new SimulationClock(selectedTime);
        scheduler = new Scheduler(clock,
            new GPU[]{new GPU(1), new GPU(2), new GPU(3), new GPU(4)});
        new Administrator(scheduler, clock);
    }
    // Storage adopts a fully validated NEW object graph, never a half-loaded one.
    public static AppState restored(Scheduler scheduler, List<AccessRequest> requests,
                                    BigInteger nextJobId) {
        if (nextJobId == null || nextJobId.compareTo(BigInteger.valueOf(101)) < 0) {
            throw new IllegalArgumentException("Invalid next job ID.");
        }
        if (requests == null){throw new IllegalArgumentException("Saved requests are required.");}
        AppState state = new AppState();
        state.scheduler = scheduler;
        for (AccessRequest request : requests) state.addRequest(request);
        state.nextJobId = nextJobId;
        if (scheduler != null) {
            for (Job job : scheduler.getAllJobs()) {
                if (new BigInteger(job.getJobId().substring(1)).compareTo(nextJobId) >= 0) {
                    throw new IllegalArgumentException("Next job ID would repeat a saved ID.");
                }
            }
        }
        return state;
    }
    // Use this for NEW jobs, instead of restarting an ID counter at each launch.
    public String allocateJobId() {
        synchronizeJobIds();
        String id = "J" + nextJobId;
        nextJobId = nextJobId.add(BigInteger.ONE);
        return id;
    }
    public BigInteger getNextJobId() {
        synchronizeJobIds();
        return nextJobId;
    }
    private void synchronizeJobIds() {
        if (scheduler != null) {
            for (Job job : scheduler.getAllJobs()) {
                BigInteger after = new BigInteger(job.getJobId().substring(1)).add(BigInteger.ONE);
                if (after.compareTo(nextJobId) > 0) nextJobId = after;
            }
        }
    }
}
