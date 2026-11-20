import java.nio.file.*;
import java.time.LocalDateTime;
import logic.*;
import model.*;
import storage.DataStore;

/** Manual console entry point. Uses data.txt in the current working directory. */
public class StorageConsoleDemo {
    public static void main(String[] args) {
        DataStore store = new DataStore();
        try {
            AppState state = store.load();
           // Clicking Run without arguments creates and saves sample data.
            String command = args.length == 0 ? "init" : args[0];
            switch (command) {
                case "init":
                    if (state.isClockSet() || !state.getRequests().isEmpty()) {
                        throw new IllegalStateException("Data already exists; init will not overwrite it.");
                    }
                    state = createSample();
                    store.save(state);
                    System.out.println("Sample simulation saved to " + store.getFile());
                    break;
                case "advance":
                    requireClock(state);
                    int minutes = args.length == 2 ? Integer.parseInt(args[1]) : 1;
                    if (minutes <= 0 || minutes > 10000) {
                        throw new IllegalArgumentException("Use 1 to 10000 minutes.");
                    }
                    for (int i = 0; i < minutes; i++) {
                        state.getScheduler().advanceMinute();
                        store.save(state); // Save each successful clock advance.
                    }
                    break;
                case "cancel":
                    requireClock(state);
                    if (args.length != 2) throw new IllegalArgumentException("Use: cancel J103");
                    Job selected = findJob(state, args[1]);
                    state.getAdministrator().cancelJob(selected, "Console demonstration");
                    store.save(state);
                    break;
                case "status": break;
                default: throw new IllegalArgumentException("Commands: status, init, advance [minutes], cancel J103");
            }
            printState(state);
        } catch (Exception e) {
            // A future GUI should show this message in a dialog.
            // Do not save an empty state after a failed load.
            System.err.println("Storage error: " + e.getMessage());
            System.exit(1);
        }
    }
    public static AppState createSample() {
        AppState state = new AppState();
        state.initializeClock(LocalDateTime.of(2026, 11, 18, 9, 50));
        Administrator admin = state.getAdministrator();
        for (int id = 1001; id <= 1005; id++) {
            AccessRequest request = new AccessRequest(id, "Student " + id,
                "s" + id + "@example.com", "0501234567", "pw" + id);
            state.addRequest(request);
            admin.approve(request, request.getPasswordForStorage());
        }
        state.addRequest(new AccessRequest(1006, "Pending student", "pending@example.com", "0501234567", "pendingPW"));
        AccessRequest rejected = new AccessRequest(1007, "Rejected student", "rejected@example.com", "0501234567", "rejectedPW");
        state.addRequest(rejected);
        admin.reject(rejected);
        Student first = state.getStudents().get(0);
        admin.grantTime(first, 20, "Extra time, with colon: reason\nand a backslash \\");
        Job complete = makeJob(state, first, "Small");
        Job cancel = makeJob(state, first, "Medium");
        state.getScheduler().advanceMinute();
        state.getScheduler().advanceMinute();
        admin.cancelJob(cancel, "Wrong input, retry later: C:\\data\nSecond line");
        for (int i = 0; i < 3; i++) state.getScheduler().advanceMinute();
        for (int i = 0; i < 4; i++) makeJob(state, state.getStudents().get(i), "Medium");
        makeJob(state, first, "Small"); // Waiting behind four running jobs.
        Job queuedCancel = makeJob(state, first, "Small");
        state.getScheduler().cancel(queuedCancel, first, "No longer needed");
        state.getScheduler().advanceMinute();
        state.getScheduler().advanceMinute();
        return state;
    }
    public static Job makeJob(AppState state, Student owner, String size) {
        Job job = new Job(state.allocateJobId(), owner.getStudentId(), "Test, job: title",
            "Python", "Description with comma, colon: and newline\nC:\\data", size,
            state.getClock().getTime());
        state.getScheduler().submit(job);
        return job;
    }
    private static Job findJob(AppState state, String id) {
        for (Job job : state.getScheduler().getAllJobs()) {
            if (job.getJobId().equals(id)) return job;
        }
        throw new IllegalArgumentException("Unknown job ID: " + id);
    }
    private static void requireClock(AppState state) {
        if (!state.isClockSet()) throw new IllegalStateException("No clock yet. Run init to create sample data.");
    }
    private static void printState(AppState state) {
        if (!state.isClockSet()) {
            System.out.println("First run: clock is unset. No file is created by reading.");
            return;
        }
        System.out.println("Clock: " + state.getClock().getTime());
        System.out.println("Original start: " + state.getScheduler().getInitialTime());
        for (GPU gpu : state.getScheduler().getGpus()) System.out.println(gpu);
        for (Job job : state.getScheduler().getAllJobs()) {
            System.out.println(job.getJobId() + " " + job.getStatus() +
                " remaining=" + job.getRemainingRuntime(state.getClock().getTime()));
        }
        for (Student student : state.getStudents()) {
            TimeAllowance a = student.getAllowance();
            System.out.println(student.getStudentId() + " allocated=" + a.getAllocated() +
                " used=" + a.getUsed() + " reserved=" + a.getReserved() + " available=" + a.available());
        }
        System.out.println("Requests: " + state.getRequests().size() + ", grants: " +
            state.getAdministrator().getGrantRecords().size());
        System.out.println("Next job ID: J" + state.getNextJobId());
        System.out.println("Utilization: " + (state.getScheduler().getUtilization().isPresent()
            ? state.getScheduler().getUtilization().getAsDouble() + "%" : "N/A"));
    }
}
