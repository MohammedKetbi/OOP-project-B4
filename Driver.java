import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import logic.Scheduler;
import logic.SimulationClock;
import model.AccessRequest;
import model.Administrator;
import model.GPU;
import model.Job;
import model.Student;

public class Driver {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {

        // Fixed simulated time makes results repeatable.
        SimulationClock clock = new SimulationClock(
            LocalDateTime.of(2026, 11, 10, 10, 0)
        );

        GPU[] gpus = {
            new GPU(1),
            new GPU(2),
            new GPU(3),
            new GPU(4)
        };

        Scheduler scheduler = new Scheduler(clock, gpus);

        // Administrator automatically registers itself.
        Administrator admin = new Administrator(scheduler, clock);

        System.out.println("=== GPU MANAGEMENT CONSOLE TEST ===");

        // TEST 1: Approve accounts and check passwords/allowances.
        AccessRequest request1 = new AccessRequest(
            1001, "Ali", "ali@example.com", "0501234567"
        );

        AccessRequest request2 = new AccessRequest(
            1002, "Sara", "sara@example.com", "0507654321"
        );

        Student ali = admin.approve(request1, "ali123");

        // Give Sara exactly enough minutes for one small job.
        Student sara = admin.approve(request2, "sara123", 5);

        check(
            "Approved".equals(request1.getStatus()),
            "Account approved"
        );

        check(
            ali.getAllowance().available() == 100,
            "Default allowance is 100"
        );

        check(
            ali.checkPassword("ali123"),
            "Student password accepted"
        );

        check(
            !ali.checkPassword("wrong"),
            "Wrong password rejected"
        );

        check(
            admin.checkPassword("gpu2026"),
            "Administrator password accepted"
        );

        AccessRequest rejected = new AccessRequest(
            1003, "Omar", "omar@example.com", "0501111111"
        );

        admin.reject(rejected);

        check(
            "Rejected".equals(rejected.getStatus()),
            "Request rejected"
        );

        // Expected errors are caught so testing can continue.
        try {
            admin.approve(request1, "ali123");
            check(false, "Repeated approval rejected");
        } catch (IllegalStateException e) {
            check(true, "Repeated approval rejected");
        }

        // TEST 2: First four jobs start on GPUs 1, 2, 3 and 4.
        Job[] jobs = new Job[4];

        for (int i = 0; i < jobs.length; i++) {

            jobs[i] = createJob(101 + i, ali, "Small", clock);
            scheduler.submit(jobs[i]);

            check(
                jobs[i].getAssignedGpuId() == i + 1,
                "Job assigned to GPU " + (i + 1)
            );
        }

        // All GPUs are busy, so Sara's job must wait.
        Job waiting = createJob(105, sara, "Small", clock);
        scheduler.submit(waiting);

        check(
            "Queued".equals(waiting.getStatus()),
            "Fifth job waits"
        );

        check(
            sara.getAllowance().available() == 0,
            "Exact allowance accepted"
        );

        check(
            scheduler.getQueuePosition(waiting) == 1,
            "Queue position is 1"
        );

        check(
            scheduler.getRemainingWait(waiting) == 5,
            "Predicted wait is 5 minutes"
        );

        check(
            !scheduler.getUtilization().isPresent(),
            "Utilization is N/A at time zero"
        );

        showDashboard(scheduler, clock);

        // TEST 3: Reject submissions exceeding available allowance.
        try {
            scheduler.submit(
                createJob(106, sara, "Small", clock)
            );

            check(false, "Insufficient allowance rejected");

        } catch (IllegalArgumentException e) {
            check(true, "Insufficient allowance rejected");
        }

        check(
            scheduler.getAllJobs().size() == 5,
            "Rejected submission is absent from history"
        );

        // Reject a different Job object with a duplicate ID.
        try {
            scheduler.submit(
                createJob(101, ali, "Small", clock)
            );

            check(false, "Duplicate job ID rejected");

        } catch (IllegalArgumentException e) {
            check(true, "Duplicate job ID rejected");
        }

        // TEST 4: Check cancellation permissions.
        try {
            scheduler.cancel(waiting, ali, "Not my job");

            check(
                false,
                "Cancellation by another student rejected"
            );

        } catch (IllegalArgumentException e) {
            check(
                true,
                "Cancellation by another student rejected"
            );
        }

        // Sara cancels her own queued job.
        scheduler.cancel(
            waiting, sara, "Submitted by mistake"
        );

        check(
            waiting.getChargeableMinutes() == 0,
            "Queued cancellation is free"
        );

        check(
            sara.getAllowance().available() == 5,
            "Queued reservation released"
        );

        // Submit a replacement job using the released allowance.
        Job replacement = createJob(
            107, sara, "Small", clock
        );

        scheduler.submit(replacement);

        // TEST 5: Advance one minute.
        scheduler.advanceMinute();

        check(
            jobs[0].getElapsedRuntime(clock.getTime()) == 1,
            "Running job executes for one minute"
        );

        check(
            replacement.getStartTime() == null,
            "Queued job has not executed"
        );

        // Cancelling the running job frees GPU 1.
        scheduler.cancel(
            jobs[0], ali, "Stop this job"
        );

        check(
            jobs[0].getChargeableMinutes() == 1,
            "Running cancellation charges one minute"
        );

        check(
            replacement.getAssignedGpuId() == 1,
            "Waiting job starts on freed GPU"
        );

        check(
            replacement.getElapsedRuntime(clock.getTime()) == 0,
            "Newly started job has zero elapsed runtime"
        );

        // TEST 6: Administrator cancellation and time grants.
        admin.cancelJob(jobs[1], "Maintenance");

        check(
            "admin".equals(jobs[1].getCancelledBy()),
            "Admin cancellation recorded"
        );

        check(
            ali.getAllowance().getUsed() == 2,
            "Both cancellations charged once"
        );

        admin.grantTime(
            sara, 10, "Additional project allocation"
        );

        check(
            sara.getAllowance().getAllocated() == 15,
            "Extra minutes granted"
        );

        check(
            admin.getGrantHistory().size() == 1,
            "Grant reason recorded"
        );

        // TEST 7: Advance to 10:05.
        // Ali's remaining two jobs started at 10:00.
        for (int i = 0; i < 4; i++) {
            scheduler.advanceMinute();
        }

        check(
            "Completed".equals(jobs[2].getStatus()),
            "Job J103 completes"
        );

        check(
            "Completed".equals(jobs[3].getStatus()),
            "Job J104 completes"
        );

        check(
            ali.getAllowance().getUsed() == 12,
            "Ali charged 1 + 1 + 5 + 5 minutes"
        );

        check(
            ali.getAllowance().getReserved() == 0,
            "Ali has no remaining reservations"
        );

        // Sara started at 10:01 and finishes at 10:06.
        scheduler.advanceMinute();

        check(
            "Completed".equals(replacement.getStatus()),
            "Sara's job completes"
        );

        check(
            sara.getAllowance().getUsed() == 5,
            "Sara charged five minutes"
        );

        check(
            sara.getAllowance().getReserved() == 0,
            "Sara's reservation released"
        );

        // Busy minutes = Ali's 12 + Sara's 5 = 17.
        // Capacity = 4 GPUs * 6 minutes = 24 GPU minutes.
        double expected = 100.0 * 17 / 24;

        check(
            Math.abs(
                scheduler.getUtilization().getAsDouble()
                - expected
            ) < 0.001,
            "Historical utilization is approximately 70.83%"
        );

        try {
            admin.cancelJob(
                replacement, "Already completed"
            );

            check(
                false,
                "Completed job cannot be cancelled"
            );

        } catch (IllegalStateException e) {
            check(
                true,
                "Completed job cannot be cancelled"
            );
        }

        // Another advance must not charge completed jobs again.
        scheduler.advanceMinute();

        check(
            ali.getAllowance().getUsed() == 12,
            "Later advances do not charge completed jobs again"
        );

        check(
            scheduler.getAllJobs().size() == 6,
            "Completed and cancelled jobs remain in history"
        );

        showDashboard(scheduler, clock);
        showAllowance(ali);
        showAllowance(sara);

        System.out.println("\n=== TEST RESULTS ===");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);

        if (failed > 0) {
            throw new AssertionError(
                "Some tests failed. Read the FAIL messages."
            );
        }
    }

    // Creates test jobs using the current simulation time.
    private static Job createJob(
            int id,
            Student student,
            String size,
            SimulationClock clock) {

        return new Job(
            "J" + id,
            student.getStudentId(),
            "Test job " + id,
            "Test application",
            "Console test",
            size,
            clock.getTime()
        );
    }

    // Prints the test result and updates the totals.
    private static void check(
            boolean condition, String description) {

        if (condition) {
            passed++;
            System.out.println("PASS: " + description);
        } else {
            failed++;
            System.out.println("FAIL: " + description);
        }
    }

    // Prints GPUs and job history without needing a GUI.
    private static void showDashboard(
            Scheduler scheduler, SimulationClock clock) {

        DateTimeFormatter format =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        System.out.println(
            "\n--- Dashboard: "
            + clock.getTime().format(format)
            + " ---"
        );

        for (GPU gpu : scheduler.getGpus()) {
            System.out.println(gpu);
        }

        for (Job job : scheduler.getAllJobs()) {
            System.out.println(
                job.getJobId()
                + " | " + job.getStatus()
                + " | student " + job.getStudentId()
            );
        }

        System.out.println(
            "Waiting jobs: " + scheduler.getQueue().size()
        );

        System.out.println();
    }

    private static void showAllowance(Student student) {

        System.out.println(
            "\nAllowance for " + student.getName()
        );

        System.out.println(
            "Allocated: "
            + student.getAllowance().getAllocated()
        );

        System.out.println(
            "Used: " + student.getAllowance().getUsed()
        );

        System.out.println(
            "Reserved: "
            + student.getAllowance().getReserved()
        );

        System.out.println(
            "Available: "
            + student.getAllowance().available()
        );
    }
}