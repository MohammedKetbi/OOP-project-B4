
package logic;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.*;
import model.*;

/** Shared application state. This class does no file I/O. */
public class AppState {

    private Scheduler scheduler;

    private final List<AccessRequest> requests =
        new ArrayList<>();

    private BigInteger nextJobId =
        BigInteger.valueOf(101);

    // Check whether the simulation clock exists
    public boolean isClockSet() {
        return scheduler != null;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public SimulationClock getClock() {
        return isClockSet() ? scheduler.getClock() : null;
    }

    public Administrator getAdministrator() {
        return isClockSet()
            ? scheduler.getAdministrator()
            : null;
    }

    public List<Student> getStudents() {
        return isClockSet()
            ? scheduler.getStudents()
            : Collections.emptyList();
    }

    public List<AccessRequest> getRequests() {
        return Collections.unmodifiableList(
            new ArrayList<>(requests)
        );
    }

    // Adds a NEW registration request
    public void addRequest(AccessRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                "Request is required."
            );
        }

        // Prevent duplicate registration requests
        for (AccessRequest existing : requests) {

            if (existing.getId() == request.getId()) {
                throw new IllegalArgumentException(
                    "Duplicate registration ID."
                );
            }
        }

        // Prevent registering an existing student again
        for (Student student : getStudents()) {

            if (student.getStudentId().equals(
                    String.valueOf(request.getId()))) {

                throw new IllegalArgumentException(
                    "Student is already registered."
                );
            }
        }

        requests.add(request);
    }

    // Initializes the simulation clock and four GPUs
    public void initializeClock(LocalDateTime selectedTime) {

        if (isClockSet()) {
            throw new IllegalStateException(
                "Clock is already set."
            );
        }

        if (selectedTime == null ||
            selectedTime.getSecond() != 0 ||
            selectedTime.getNano() != 0) {

            throw new IllegalArgumentException(
                "Select a date/time with minute precision."
            );
        }

        SimulationClock clock =
            new SimulationClock(selectedTime);

        scheduler = new Scheduler(
            clock,
            new GPU[]{
                new GPU(1),
                new GPU(2),
                new GPU(3),
                new GPU(4)
            }
        );

        new Administrator(scheduler, clock);
    }

    // Restores an application state from saved records
    public static AppState restored(
            Scheduler scheduler,
            List<AccessRequest> requests,
            BigInteger nextJobId) {

        if (nextJobId == null ||
            nextJobId.compareTo(
                BigInteger.valueOf(101)
            ) < 0) {

            throw new IllegalArgumentException(
                "Invalid next job ID."
            );
        }

        if (requests == null) {
            throw new IllegalArgumentException(
                "Saved requests are required."
            );
        }

        AppState state = new AppState();

        state.scheduler = scheduler;

        // Restore saved requests without treating approved
        // students as duplicate NEW registrations
        for (AccessRequest request : requests) {

            if (request == null) {
                throw new IllegalArgumentException(
                    "Saved request cannot be null."
                );
            }

            // Reject duplicate IDs in saved requests
            for (AccessRequest existing : state.requests) {

                if (existing.getId() == request.getId()) {
                    throw new IllegalArgumentException(
                        "Duplicate saved registration ID."
                    );
                }
            }

            state.requests.add(request);
        }

        state.nextJobId = nextJobId;

        // Ensure the next job ID will not repeat a saved ID
        if (scheduler != null) {

            for (Job job : scheduler.getAllJobs()) {

                BigInteger savedId = new BigInteger(
                    job.getJobId().substring(1)
                );

                if (savedId.compareTo(nextJobId) >= 0) {

                    throw new IllegalArgumentException(
                        "Next job ID would repeat a saved ID."
                    );
                }
            }
        }

        return state;
    }

    // Allocates a unique ID for a NEW job
    public String allocateJobId() {

        synchronizeJobIds();

        String id = "J" + nextJobId;

        nextJobId = nextJobId.add(
            BigInteger.ONE
        );

        return id;
    }

    public BigInteger getNextJobId() {

        synchronizeJobIds();

        return nextJobId;
    }

    // Ensures the counter is greater than all existing IDs
    private void synchronizeJobIds() {

        if (scheduler != null) {

            for (Job job : scheduler.getAllJobs()) {

                BigInteger after = new BigInteger(
                    job.getJobId().substring(1)
                ).add(BigInteger.ONE);

                if (after.compareTo(nextJobId) > 0) {
                    nextJobId = after;
                }
            }
        }
    }
}
