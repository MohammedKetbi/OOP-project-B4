package logic;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import model.Administrator;
import model.GPU;
import model.Job;
import model.Student;
import model.TimeAllowance;
import model.User;

public class Scheduler {

    // Contains only jobs waiting for a GPU.
    private final List<Job> queue = new ArrayList<>();

    // Keeps every accepted job, including completed and cancelled jobs.
    private final List<Job> allJobs = new ArrayList<>();

    // Connects each student ID to the existing Student object.
    // Job stores a student ID, so this lets us find their allowance.
    private final Map<String, Student> students = new HashMap<>();

    private final GPU[] gpus;
    private final SimulationClock simClock;

    // Starting time for calculating utilization during this session.
    private LocalDateTime initialTime;

    // The completed Administrator registers itself in its constructor.
    private Administrator administrator;

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

        // Check that all GPUs exist, are free, and have different IDs.
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

        // Copy the array while keeping the same GPU objects.
        this.gpus = gpus.clone();

        // Jobs must be assigned to free GPUs in increasing ID order.
        Arrays.sort(
            this.gpus,
            Comparator.comparingInt(GPU::getId)
        );
    }

    // Administrator.approve() automatically calls this method.
    // Store the same Student object so its allowance is shared.
    public void registerStudent(Student student) {

        if (student == null ||
            student.getStudentId() == null ||
            student.getStudentId().trim().isEmpty()) {

            throw new IllegalArgumentException(
                "A student with an ID is required."
            );
        }

        Student existing = students.get(student.getStudentId());

        // Registering the same object again is harmless.
        // A different object with the same ID is rejected.
        if (existing != null && existing != student) {
            throw new IllegalArgumentException(
                "Student ID is already registered."
            );
        }

        students.put(student.getStudentId(), student);
    }

    // Administrator's constructor calls:
    // scheduler.registerAdministrator(this);
    //
    // Create the Scheduler first, then the Administrator.
    public void registerAdministrator(Administrator admin) {

        if (admin == null ||
            !"admin".equals(admin.getUsername())) {

            throw new IllegalArgumentException(
                "The predefined administrator is required."
            );
        }

        // Prevent replacing the registered administrator account.
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

        // Different Job objects must not share the same ID.
        for (Job existing : allJobs) {
            if (existing.getJobId().equals(job.getJobId())) {
                throw new IllegalArgumentException(
                    "Job ID was already submitted."
                );
            }
        }

        // Your Job constructor creates jobs with status "Queued".
        // Reject jobs that have already started or ended.
        if (!"Queued".equals(job.getStatus()) ||
            job.getStartTime() != null ||
            job.getFinishTime() != null ||
            job.getCancellationTime() != null ||
            job.getEstimatedDuration() <= 0) {

            throw new IllegalArgumentException(
                "Only a new queued job can be submitted."
            );
        }

        // Job has no submission-time setter.
        // Its constructor must receive simClock.getTime().
        if (!simClock.getTime().equals(job.getSubmissionTime())) {
            throw new IllegalArgumentException(
                "Create the job using the current simulation time."
            );
        }

        TimeAllowance allowance = ownerOf(job).getAllowance();

        // reserve() checks that enough minutes are available.
        // A job using exactly the available balance is accepted.
        allowance.reserve(job.getEstimatedDuration());

        queue.add(job);
        allJobs.add(job);

        sortQueue();

        // Start immediately if a GPU is free.
        startWaitingJobs();
    }

    public void advanceMinute() {

        simClock.advance();

        // Job calculates runtime from the clock and its timestamps.
        // Queued jobs do not consume GPU minutes.
        for (GPU gpu : gpus) {

            if (!gpu.isFree()) {

                Job job = gpu.getCurrentJob();

                if (job.getRemainingRuntime(
                        simClock.getTime()) == 0) {

                    completeJob(job, gpu);
                }
            }
        }

        // Process all running jobs before starting waiting jobs.
        // Newly started jobs begin with zero elapsed runtime.
        startWaitingJobs();
    }

    private void completeJob(Job job, GPU gpu) {

        job.complete(simClock.getTime());

        // Release the full reservation and charge the completed runtime.
        // Only jobs on busy GPUs reach this method, so completed jobs
        // are not charged again on later clock advances.
        ownerOf(job).getAllowance().settle(
            job.getEstimatedDuration(),
            job.getChargeableMinutes()
        );

        // GPU.release() requires a completed or cancelled job.
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

        // Pass the actual Student object created during approval.
        boolean isOwner = by == owner;

        // Administrator.cancelJob() passes its own object here.
        // Compare with the registered account, not just a username.
        boolean isAdmin =
            administrator != null && by == administrator;

        if (!isOwner && !isAdmin) {
            throw new IllegalArgumentException(
                "You cannot cancel another student's job."
            );
        }

        // Completed and cancelled jobs cannot be cancelled again.
        if (!"Queued".equals(job.getStatus()) &&
            !"Running".equals(job.getStatus())) {

            throw new IllegalStateException(
                "Only queued or running jobs can be cancelled."
            );
        }

        GPU assignedGpu = null;

        // Find the GPU before changing the job's status.
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

        // Job records cancellation time, user and reason.
        // Your updated Job.checkTime() handles queued jobs,
        // which have no start time.
        job.cancel(
            simClock.getTime(),
            by.getUsername(),
            why.trim()
        );

        // Queued cancellation charges zero.
        // Running cancellation charges only elapsed runtime.
        // settle() also releases the full reservation.
        owner.getAllowance().settle(
            job.getEstimatedDuration(),
            job.getChargeableMinutes()
        );

        queue.remove(job);

        if (assignedGpu != null) {
            assignedGpu.release();
        }

        // The cancelled job stays in allJobs for history.
        startWaitingJobs();
    }

    // Shared by submit(), advanceMinute() and cancel().
    private void startWaitingJobs() {

        // GPUs are already sorted by ID in the constructor.
        for (GPU gpu : gpus) {

            if (queue.isEmpty()) {
                break;
            }

            if (gpu.isFree()) {

                Job next = queue.get(0);

                // Your GPU.assign() already calls Job.start().
                // Do not call start() separately.
                gpu.assign(next, simClock.getTime());

                queue.remove(0);
            }
        }
    }

    private void sortQueue() {

        // Order by submission time, then the numeric part of the ID.
        // Numeric comparison puts J99 before J100.
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

    // Find the registered Student using the ID stored in Job.
    private Student ownerOf(Job job) {

        Student student = students.get(job.getStudentId());

        if (student == null) {
            throw new IllegalArgumentException(
                "Register the approved student first."
            );
        }

        return student;
    }

    private void requireKnownJob(Job job) {

        if (job == null || !allJobs.contains(job)) {
            throw new IllegalArgumentException(
                "Job is not in this scheduler."
            );
        }
    }

    // Predict start and finish times without assigning real GPUs.
    // Each result array contains:
    // [0] = predicted start
    // [1] = predicted finish
    //
    // Calling this again recalculates predictions from current state.
    public Map<Job, LocalDateTime[]> predictSchedule() {

        LocalDateTime[] available =
            new LocalDateTime[gpus.length];

        Map<Job, LocalDateTime[]> result =
            new LinkedHashMap<>();

        LocalDateTime now = simClock.getTime();

        // Copy each GPU's availability time.
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
                        running.getStartTime(),
                        finish
                    }
                );
            }
        }

        // Predict queued jobs in their current FCFS order.
        for (Job job : queue) {

            int earliest = 0;

            for (int i = 1; i < available.length; i++) {

                // Equal times retain the lower GPU ID because
                // the GPU array is sorted in increasing ID order.
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

            // Update only the copied availability time.
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
    // Zero means the job is not waiting.
    public int getQueuePosition(Job job) {

        requireKnownJob(job);

        return queue.indexOf(job) + 1;
    }

    public OptionalDouble getUtilization() {

        long elapsed = Duration.between(
            initialTime,
            simClock.getTime()
        ).toMinutes();

        // Empty means the interface should display N/A.
        if (elapsed == 0) {
            return OptionalDouble.empty();
        }

        long busyMinutes = 0;

        for (Job job : allJobs) {

            // Include running, completed and running-cancelled time.
            // Exclude waiting time.
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

    // Read-only list copies prevent outside code from adding
    // or removing jobs directly from the scheduler's collections.
    // The Job objects themselves are still shared.
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

    // STORAGE: read-only views used by DataStore.
    public List<Student> getStudents() {
        List<Student> result = new ArrayList<>(students.values());
        result.sort(Comparator.comparing(Student::getStudentId));
        return Collections.unmodifiableList(result);
    }
    public LocalDateTime getInitialTime() { return initialTime; }
    public SimulationClock getClock() { return simClock; }
    public Administrator getAdministrator() { return administrator; }

    // STORAGE: build a NEW scheduler, then validate before returning it.
    // No submit(), dispatch or allowance settlement is performed during load.
    public static Scheduler restore(SimulationClock clock, LocalDateTime initial,
            List<Student> savedStudents, List<Job> savedJobs,
            Map<Integer, String> assignments) {
        LocalDateTime now = clock.getTime();
        if (initial == null || initial.isAfter(now) || assignments.size() != 4) {
            throw new IllegalArgumentException("Invalid initial time or GPU count.");
        }
        Scheduler scheduler = new Scheduler(clock,
            new GPU[]{new GPU(1), new GPU(2), new GPU(3), new GPU(4)});
        scheduler.initialTime = initial;
        for (Student student : savedStudents) scheduler.registerStudent(student);
        Map<String, Job> byId = new HashMap<>();
        Map<String, Long> reserved = new HashMap<>();
        Map<String, Long> used = new HashMap<>();
        for (Job job : savedJobs) {
            if (job == null || job.getJobId() == null ||
                !job.getJobId().matches("J[0-9]+") || byId.put(job.getJobId(), job) != null) {
                throw new IllegalArgumentException("Missing or duplicate saved job ID.");
            }
            Student owner = scheduler.ownerOf(job);
            LocalDateTime sub = job.getSubmissionTime();
            LocalDateTime start = job.getStartTime();
            LocalDateTime end = job.getFinishTime();
            LocalDateTime cancel = job.getCancellationTime();
            if (sub == null || sub.isBefore(initial) || sub.isAfter(now) ||
                (start != null && (start.isBefore(sub) || start.isAfter(now)))) {
                throw new IllegalArgumentException("Invalid saved job timeline: " + job.getJobId());
            }
            String status = job.getStatus();
            boolean active = "Queued".equals(status) || "Running".equals(status);
            if ("Queued".equals(status)) {
                if (start != null || end != null || job.getAssignedGpuId() != 0) {
                    throw new IllegalArgumentException("Queued job has execution fields.");
                }
                scheduler.queue.add(job);
            } else if ("Running".equals(status)) {
                if (start == null || end != null ||
                    !start.plusMinutes(job.getEstimatedDuration()).isAfter(now)) {
                    throw new IllegalArgumentException("Invalid running job.");
                }
            } else if ("Completed".equals(status)) {
                if (start == null || end == null || end.isAfter(now) ||
                    !end.equals(start.plusMinutes(job.getEstimatedDuration()))) {
                    throw new IllegalArgumentException("Invalid completed job.");
                }
            } else if ("Cancelled".equals(status)) {
                if (cancel == null || cancel.isBefore(sub) || cancel.isAfter(now) ||
                    job.getCancelledBy() == null || job.getCancelledReason() == null ||
                    job.getCancelledReason().trim().isEmpty() ||
                    !("admin".equals(job.getCancelledBy()) ||
                      owner.getStudentId().equals(job.getCancelledBy()))) {
                    throw new IllegalArgumentException("Invalid cancellation record.");
                }
                if (start == null) {
                    if (end != null || job.getAssignedGpuId() != 0) {
                        throw new IllegalArgumentException("Queued cancellation has runtime fields.");
                    }
                } else if (!cancel.equals(end) || cancel.isBefore(start) ||
                    cancel.isAfter(start.plusMinutes(job.getEstimatedDuration()))) {
                    throw new IllegalArgumentException("Invalid cancelled runtime.");
                }
            } else throw new IllegalArgumentException("Unknown saved job status.");
            if (!"Cancelled".equals(status) && (cancel != null ||
                job.getCancelledBy() != null || job.getCancelledReason() != null)) {
                throw new IllegalArgumentException("Unexpected cancellation fields.");
            }
            if (start != null && (job.getAssignedGpuId() < 1 || job.getAssignedGpuId() > 4)) {
                throw new IllegalArgumentException("Invalid execution GPU ID.");
            }
            String id = owner.getStudentId();
            if (active) reserved.put(id, reserved.getOrDefault(id, 0L) + job.getEstimatedDuration());
            else used.put(id, used.getOrDefault(id, 0L) + job.getChargeableMinutes());
            scheduler.allJobs.add(job);
        }
        Set<Job> assigned = new HashSet<>();
        for (GPU gpu : scheduler.gpus) {
            if (!assignments.containsKey(gpu.getId())) {
                throw new IllegalArgumentException("Missing saved GPU.");
            }
            String id = assignments.get(gpu.getId());
            if (id != null) {
                Job job = byId.get(id);
                if (job == null || !assigned.add(job)) {
                    throw new IllegalArgumentException("Unknown or duplicated GPU job.");
                }
                gpu.restoreCurrentJob(job);
            }
        }
        for (Job job : savedJobs) {
            if ("Running".equals(job.getStatus()) && !assigned.contains(job)) {
                throw new IllegalArgumentException("Running job has no GPU.");
            }
        }
        for (Student student : savedStudents) {
            TimeAllowance allowance = student.getAllowance();
            if (reserved.getOrDefault(student.getStudentId(), 0L) != allowance.getReserved() ||
                used.getOrDefault(student.getStudentId(), 0L) != allowance.getUsed()) {
                throw new IllegalArgumentException("Saved allowance disagrees with job history: " + student.getStudentId());
            }
        }
        // A single GPU cannot have executed two jobs at overlapping times.
        // Touching endpoints are valid: one job may start as another finishes.
        for (int i = 0; i < savedJobs.size(); i++) {
            Job a = savedJobs.get(i);
            if (a.getStartTime() == null) continue;
            LocalDateTime aEnd = a.getFinishTime() == null ? now : a.getFinishTime();
            for (int j = i + 1; j < savedJobs.size(); j++) {
                Job b = savedJobs.get(j);
                if (b.getStartTime() == null || a.getAssignedGpuId() != b.getAssignedGpuId()) continue;
                LocalDateTime bEnd = b.getFinishTime() == null ? now : b.getFinishTime();
                if (a.getStartTime().isBefore(aEnd) && b.getStartTime().isBefore(bEnd) &&
                    a.getStartTime().isBefore(bEnd) && b.getStartTime().isBefore(aEnd)) {
                    throw new IllegalArgumentException("Saved GPU execution intervals overlap.");
                }
            }
        }
        scheduler.sortQueue();
        for (GPU gpu : scheduler.gpus) {
            if (gpu.isFree() && !scheduler.queue.isEmpty()) {
                throw new IllegalArgumentException("Waiting jobs exist while a GPU is free.");
            }
        }
        return scheduler;
    }

}