import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import model.Student;
import model.TimeAllowance;
import model.User;

public class Scheduler {

    // Jobs currently waiting for a GPU.
    private final List<Job> queue = new ArrayList<>();

    // All accepted jobs, including completed and cancelled jobs.
    private final List<Job> allJobs = new ArrayList<>();

    // ADDED: Job stores a student ID, not a Student object.
    // This map lets us find the student's existing allowance.
    private final Map<String, Student> students = new HashMap<>();

    private final GPU[] gpus;
    private final SimulationClock simClock;

    // ADDED: needed to calculate elapsed time for utilization.
    private final LocalDateTime initialTime;

    // ADMINISTRATOR NOTE:
    // We use User so this class does not depend on the unfinished
    // Administrator class. Admin cancellation stays disabled until
    // trusted application setup registers the real administrator.
    private User administrator;

    public Scheduler(SimulationClock simClock, GPU[] gpus) {

        if (simClock == null || simClock.getTime() == null) {
            throw new IllegalArgumentException(
                "An initialized clock is required."
            );
        }

        if (gpus == null || gpus.length != 4) {
            throw new IllegalArgumentException(
                "Exactly four GPUs are required."
            );
        }

        Set<Integer> ids = new HashSet<>();

        for (GPU gpu : gpus) {
            if (gpu == null ||
                !ids.add(gpu.getId()) ||
                !gpu.isFree()) {

                throw new IllegalArgumentException(
                    "Use four initialized, free GPUs with different IDs."
                );
            }
        }

        this.simClock = simClock;
        this.initialTime = simClock.getTime();

        // Copy the array so changing the original array does not
        // replace the scheduler's GPUs.
        this.gpus = gpus.clone();

        // Free GPUs must receive jobs in increasing GPU ID order.
        Arrays.sort(
            this.gpus,
            Comparator.comparingInt(GPU::getId)
        );
    }

    // ADDED: call this for each approved student before submission.
    // Approval itself belongs in account management.
    public void registerStudent(Student student) {

        if (student == null ||
            student.getStudentId() == null ||
            student.getStudentId().trim().isEmpty()) {

            throw new IllegalArgumentException(
                "A student with an ID is required."
            );
        }

        Student existing = students.get(student.getStudentId());

        if (existing != null && existing != student) {
            throw new IllegalArgumentException(
                "Student ID is already registered."
            );
        }

        students.put(student.getStudentId(), student);
    }

    // ADMINISTRATOR NOTE:
    // Call this from trusted application setup once the real
    // Administrator class extends model.User.
    // Do not accept an arbitrary administrator object from student input.
    public void registerAdministrator(User admin) {

        if (admin == null ||
            admin instanceof Student ||
            !"admin".equals(admin.getUsername())) {

            throw new IllegalArgumentException(
                "The predefined administrator is required."
            );
        }

        if (administrator != null && administrator != admin) {
            throw new IllegalStateException(
                "Administrator is already registered."
            );
        }

        administrator = admin;
    }

    public void submit(Job job) {

        if (job == null ||
            job.getJobId() == null ||
            !job.getJobId().matches("J[0-9]+")) {

            throw new IllegalArgumentException(
                "A job with an ID such as J101 is required."
            );
        }

        // Reject duplicate IDs, even if the objects are different.
        for (Job existing : allJobs) {
            if (existing.getJobId().equals(job.getJobId())) {
                throw new IllegalArgumentException(
                    "Job ID was already submitted."
                );
            }
        }

        if (!"Queued".equals(job.getStatus()) ||
            job.getStartTime() != null ||
            job.getFinishTime() != null ||
            job.getCancellationTime() != null ||
            job.getEstimatedDuration() <= 0) {

            throw new IllegalArgumentException(
                "Only a new queued job can be submitted."
            );
        }

        // Your Job constructor sets submissionTime.
        // Create the job using simClock.getTime().
        if (!simClock.getTime().equals(job.getSubmissionTime())) {
            throw new IllegalArgumentException(
                "Create the job using the current simulation time."
            );
        }

        TimeAllowance allowance = ownerOf(job).getAllowance();

        // Your reserve() method already checks available minutes.
        // An exact match is allowed.
        allowance.reserve(job.getEstimatedDuration());

        queue.add(job);
        allJobs.add(job);

        sortQueue();
        startWaitingJobs();
    }

    public void advanceMinute() {

        simClock.advance();

        // Your Job class calculates runtime from timestamps.
        // It does not need a separate advanceMinute() method.
        for (GPU gpu : gpus) {

            if (!gpu.isFree()) {

                Job job = gpu.getCurrentJob();

                if (job.getRemainingRuntime(
                        simClock.getTime()) == 0) {

                    completeJob(job, gpu);
                }
            }
        }

        // Start waiting jobs after processing all running jobs.
        // Newly started jobs have zero elapsed runtime at this moment.
        startWaitingJobs();
    }

    // ADDED: handles completion and allowance settlement together.
    private void completeJob(Job job, GPU gpu) {

        job.complete(simClock.getTime());

        // Your settle() method releases the full reservation
        // and charges actual runtime in one operation.
        ownerOf(job).getAllowance().settle(
            job.getEstimatedDuration(),
            job.getChargeableMinutes()
        );

        // Your GPU requires the job to be completed or cancelled
        // before release() can be called.
        gpu.release();
    }

    public void cancel(Job job, User by, String why) {

        requireKnownJob(job);

        if (by == null ||
            why == null ||
            why.trim().isEmpty()) {

            throw new IllegalArgumentException(
                "Cancelling user and reason are required."
            );
        }

        Student owner = ownerOf(job);

        // The caller must pass the actual registered account object.
        boolean isOwner = by == owner;

        boolean isAdmin =
            administrator != null && by == administrator;

        if (!isOwner && !isAdmin) {
            throw new IllegalArgumentException(
                "You cannot cancel another student's job."
            );
        }

        if (!"Queued".equals(job.getStatus()) &&
            !"Running".equals(job.getStatus())) {

            throw new IllegalStateException(
                "Only queued or running jobs can be cancelled."
            );
        }

        GPU assignedGpu = null;

        // Find the assigned GPU before changing the job status.
        if ("Running".equals(job.getStatus())) {

            for (GPU gpu : gpus) {
                if (gpu.getCurrentJob() == job) {
                    assignedGpu = gpu;
                    break;
                }
            }

            if (assignedGpu == null) {
                throw new IllegalStateException(
                    "Running job has no GPU."
                );
            }
        }

        // Your Job.cancel() records the user, reason, cancellation
        // time, and finish time when cancelled while running.
        //
        // REQUIRED: apply the Job.checkTime() null fix shown above.
        job.cancel(
            simClock.getTime(),
            by.getUsername(),
            why.trim()
        );

        // Queued cancellation charges zero.
        // Running cancellation charges elapsed execution time.
        owner.getAllowance().settle(
            job.getEstimatedDuration(),
            job.getChargeableMinutes()
        );

        queue.remove(job);

        if (assignedGpu != null) {
            assignedGpu.release();
        }

        startWaitingJobs();
    }

    // ADDED: shared dispatch logic.
    private void startWaitingJobs() {

        // The constructor sorts GPUs by ID.
        for (GPU gpu : gpus) {

            if (queue.isEmpty()) {
                break;
            }

            if (gpu.isFree()) {

                Job next = queue.get(0);

                // Your GPU.assign() already calls Job.start().
                // Calling start() separately would start the job twice.
                gpu.assign(next, simClock.getTime());

                queue.remove(0);
            }
        }
    }

    // ADDED: submission time first, then numeric job ID.
    private void sortQueue() {

        // Numeric comparison ensures J99 comes before J100.
        queue.sort(
            Comparator.comparing(Job::getSubmissionTime)
                .thenComparing(
                    job -> new BigInteger(
                        job.getJobId().substring(1)
                    )
                )
                .thenComparing(Job::getJobId)
        );
    }

    // ADDED: finds the Student associated with a Job's student ID.
    private Student ownerOf(Job job) {

        Student student = students.get(job.getStudentId());

        if (student == null) {
            throw new IllegalArgumentException(
                "Register the approved student first."
            );
        }

        return student;
    }

    // ADDED: prevents cancellation or queries for unknown jobs.
    private void requireKnownJob(Job job) {

        if (job == null || !allJobs.contains(job)) {
            throw new IllegalArgumentException(
                "Job is not in this scheduler."
            );
        }
    }

    // ADDED: predicts times without changing live GPU assignments.
    // Each array contains:
    // index 0 = predicted start, index 1 = predicted finish.
    //
    // Recalculate whenever the screen needs current predictions.
    public Map<Job, LocalDateTime[]> predictSchedule() {

        LocalDateTime[] available =
            new LocalDateTime[gpus.length];

        Map<Job, LocalDateTime[]> result =
            new LinkedHashMap<>();

        LocalDateTime now = simClock.getTime();

        // Copy availability times from the live GPUs.
        for (int i = 0; i < gpus.length; i++) {

            Job running = gpus[i].getCurrentJob();
            available[i] = now;

            if (running != null) {

                LocalDateTime finish =
                    running.getStartTime().plusMinutes(
                        running.getEstimatedDuration()
                    );

                available[i] = finish;

                result.put(
                    running,
                    new LocalDateTime[]{
                        running.getStartTime(), finish
                    }
                );
            }
        }

        // Schedule waiting jobs on the copied availability times.
        for (Job job : queue) {

            int earliest = 0;

            for (int i = 1; i < available.length; i++) {

                // Equal times keep the lower GPU ID.
                if (available[i].isBefore(available[earliest])) {
                    earliest = i;
                }
            }

            LocalDateTime start = available[earliest];

            LocalDateTime finish =
                start.plusMinutes(job.getEstimatedDuration());

            result.put(
                job,
                new LocalDateTime[]{start, finish}
            );

            available[earliest] = finish;
        }

        return result;
    }

    public long getRemainingWait(Job job) {

        requireKnownJob(job);

        if (!queue.contains(job)) {
            return 0;
        }

        LocalDateTime predictedStart =
            predictSchedule().get(job)[0];

        return Duration.between(
            simClock.getTime(),
            predictedStart
        ).toMinutes();
    }

    // Queue positions start at 1.
    // Returns 0 when the job is not waiting.
    public int getQueuePosition(Job job) {

        requireKnownJob(job);

        return queue.indexOf(job) + 1;
    }

    public OptionalDouble getUtilization() {

        long elapsed = Duration.between(
            initialTime,
            simClock.getTime()
        ).toMinutes();

        // Empty means the GUI should display N/A.
        if (elapsed == 0) {
            return OptionalDouble.empty();
        }

        long busyMinutes = 0;

        for (Job job : allJobs) {

            // Queued jobs have no execution time.
            if (job.getStartTime() != null) {
                busyMinutes += job.getElapsedRuntime(
                    simClock.getTime()
                );
            }
        }

        double utilization =
            100.0 * busyMinutes /
            (gpus.length * (double) elapsed);

        return OptionalDouble.of(utilization);
    }

    // Return read-only copies of the lists so outside code cannot
    // add or remove jobs directly.
    public List<Job> getQueue() {

        return Collections.unmodifiableList(
            new ArrayList<>(queue)
        );
    }

    public List<Job> getAllJobs() {

        return Collections.unmodifiableList(
            new ArrayList<>(allJobs)
        );
    }

    public GPU[] getGpus() {
        return gpus.clone();
    }
}