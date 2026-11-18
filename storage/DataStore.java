package storage;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import logic.*;
import model.*;

/** Saves one complete snapshot in the agreed editable UTF-8 text format. */
public class DataStore {
    private final Path file;

    public DataStore() { this(Paths.get("data.txt")); }
    public DataStore(Path file) {
        if (file == null) throw new IllegalArgumentException("Save path is required.");
        this.file = file.toAbsolutePath().normalize();
    }
    public Path getFile() { return file; }

    // First run: missing or blank file means no simulation clock exists yet.
    // A nonempty invalid file is an ERROR; it is never silently replaced.
    public AppState load() throws IOException {
        try {
            if (Files.notExists(file)) return new AppState();
            return decode(Files.readAllLines(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IOException("Cannot load " + file + ": " + e.getMessage(), e);
        }
    }

    public void save(AppState state) throws IOException {
        if (state == null) throw new IllegalArgumentException("Application state is required.");
        Path temporary = null;
        try {
            List<String> lines = encode(state);
            // Validate the complete snapshot BEFORE touching the previous save.
            decode(lines);
            Files.createDirectories(file.getParent());
            temporary = Files.createTempFile(file.getParent(), "gpu-save-", ".tmp");
            Files.write(temporary, lines, StandardCharsets.UTF_8);
            // Replace only after the complete temporary file has been written.
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE,
                           StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException e) {
            throw new IOException("Cannot save " + file + ": " + e.getMessage(), e);
        } finally {
            if (temporary != null) {
                // Cleanup must not hide the original write error.
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }

    private List<String> encode(AppState state) {
        List<String> lines = new ArrayList<>();
        lines.add(RecordCodec.line("recordType", "VERSION", "version", "1"));
        Scheduler scheduler = state.getScheduler();
        lines.add(RecordCodec.line("recordType", "CLOCK", "initialTime",
            scheduler == null ? null : text(scheduler.getInitialTime()), "currentTime",
            state.getClock() == null ? null : text(state.getClock().getTime())));
        lines.add(RecordCodec.line("recordType", "NEXT_JOB_ID", "nextJobId",
            state.getNextJobId().toString()));
        for (Student student : state.getStudents()) {
            TimeAllowance a = student.getAllowance();
            lines.add(RecordCodec.line("recordType", "STUDENT", "studentId", student.getStudentId(),
                "name", student.getName(), "email", student.getEmail(), "phone", student.getPhone(),
                "password", student.getPasswordForStorage(), "allocated", text(a.getAllocated()),
                "used", text(a.getUsed()), "reserved", text(a.getReserved())));
        }
        for (AccessRequest r : state.getRequests()) {
            lines.add(RecordCodec.line("recordType", "REQUEST", "studentId", text(r.getId()),
                "name", r.getName(), "email", r.getEmail(), "phone", r.getPhone(),
                "password", r.getPasswordForStorage(), "status", r.getStatus()));
        }
        if (scheduler != null) {
            if (state.getAdministrator() == null) {
                throw new IllegalStateException("Register the administrator before saving.");
            }
            for (TimeGrant grant : state.getAdministrator().getGrantRecords()) {
                lines.add(RecordCodec.line("recordType", "GRANT", "studentId", grant.getStudentId(),
                    "minutes", text(grant.getMinutes()), "grantedBy", grant.getGrantedBy(),
                    "time", text(grant.getTime()), "reason", grant.getReason()));
            }
            for (Job j : scheduler.getAllJobs()) {
                lines.add(RecordCodec.line("recordType", "JOB", "jobId", j.getJobId(),
                    "studentId", j.getStudentId(), "title", j.getTitle(),
                    "applicationName", j.getApplicationName(), "description", j.getDescription(),
                    "taskSize", j.getTaskSize(), "workloadUnits", text(j.getWorkloadUnits()),
                    "estimatedDuration", text(j.getEstimatedDuration()),
                    "submissionTime", text(j.getSubmissionTime()), "status", j.getStatus(),
                    "assignedGpuId", text(j.getAssignedGpuId()), "startTime", text(j.getStartTime()),
                    "finishTime", text(j.getFinishTime()), "cancellationTime", text(j.getCancellationTime()),
                    "cancelledBy", j.getCancelledBy(), "cancelledReason", j.getCancelledReason()));
            }
        }
        for (int id = 1; id <= 4; id++) {
            Job current = null;
            if (scheduler != null) current = scheduler.getGpus()[id - 1].getCurrentJob();
            lines.add(RecordCodec.line("recordType", "GPU", "id", text(id),
                "currentJobId", current == null ? null : current.getJobId()));
        }
        // Required footer detects a missing/truncated end of file.
        lines.add(RecordCodec.line("recordType", "END", "recordCount", text(lines.size())));
        return lines;
    }

    private AppState decode(List<String> lines) throws IOException {
        List<Student> students = new ArrayList<>();
        List<AccessRequest> requests = new ArrayList<>();
        List<Job> jobs = new ArrayList<>();
        List<TimeGrant> grants = new ArrayList<>();
        Map<Integer, String> assignments = new HashMap<>();
        Set<String> studentIds = new HashSet<>();
        Set<Integer> requestIds = new HashSet<>();
        LocalDateTime initial = null, current = null;
        BigInteger next = null;
        boolean versionSeen = false, clockSeen = false, footerSeen = false;
        int records = 0;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().isEmpty()) continue;
            try {
                if (footerSeen) throw new IllegalArgumentException("Records follow END.");
                Row r = new Row(line);
                String type = r.required("recordType");
                switch (type) {
                    case "VERSION":
                        if (versionSeen || records != 0 || !"1".equals(r.required("version"))) {
                            throw new IllegalArgumentException("Expected VERSION 1 as first record.");
                        }
                        versionSeen = true;
                        break;
                    case "CLOCK":
                        if (clockSeen) throw new IllegalArgumentException("Duplicate CLOCK.");
                        clockSeen = true;
                        initial = r.date("initialTime", true);
                        current = r.date("currentTime", true);
                        if ((initial == null) != (current == null)) {
                            throw new IllegalArgumentException("Both clock fields must be set or NONE.");
                        }
                        break;
                    case "NEXT_JOB_ID":
                        if (next != null) throw new IllegalArgumentException("Duplicate NEXT_JOB_ID.");
                        next = new BigInteger(r.required("nextJobId"));
                        break;
                    case "STUDENT": {
                        String id = r.required("studentId");
                        if (!studentIds.add(id)) throw new IllegalArgumentException("Duplicate student ID.");
                        String name = r.required("name"), email = r.required("email");
                        String phone = r.required("phone"), password = r.required("password");
                        Student student = new Student(id, password, name, email, phone, r.integer("allocated"));
                        student.getAllowance().restoreTotals(r.integer("used"), r.integer("reserved"));
                        students.add(student);
                        break;
                    }
                    case "REQUEST": {
                        int id = r.integer("studentId");
                        if (id <= 0 || !requestIds.add(id)) throw new IllegalArgumentException("Invalid/duplicate request ID.");
                        requests.add(AccessRequest.restore(id, r.required("name"), r.required("email"),
                            r.required("phone"), r.required("password"), r.required("status")));
                        break;
                    }
                    case "GRANT":
                        grants.add(new TimeGrant(r.required("studentId"), r.integer("minutes"),
                            r.required("grantedBy"), r.date("time", false), r.required("reason")));
                        break;
                    case "JOB": {
                        String id = r.required("jobId"), owner = r.required("studentId");
                        String title = r.required("title"), app = r.required("applicationName");
                        String description = r.required("description"), size = r.required("taskSize");
                        int workload = r.integer("workloadUnits"), duration = r.integer("estimatedDuration");
                        Job job = Job.restore(id, owner, title, app, description, size,
                            r.date("submissionTime", false), r.required("status"), r.integer("assignedGpuId"),
                            r.date("startTime", true), r.date("finishTime", true),
                            r.date("cancellationTime", true), r.optional("cancelledBy"), r.optional("cancelledReason"));
                        if (workload != job.getWorkloadUnits() || duration != job.getEstimatedDuration()) {
                            throw new IllegalArgumentException("Task size, workload and duration disagree.");
                        }
                        jobs.add(job);
                        break;
                    }
                    case "GPU": {
                        int id = r.integer("id");
                        if (id < 1 || id > 4 || assignments.containsKey(id)) {
                            throw new IllegalArgumentException("Invalid/duplicate GPU ID.");
                        }
                        assignments.put(id, r.optional("currentJobId"));
                        break;
                    }
                    case "END":
                        if (r.integer("recordCount") != records) {
                            throw new IllegalArgumentException("Record count mismatch.");
                        }
                        footerSeen = true;
                        break;
                    default: throw new IllegalArgumentException("Unknown record type: " + type);
                }
                r.finish();
                records++;
            } catch (RuntimeException e) {
                throw new IOException("Line " + (i + 1) + ": " + e.getMessage(), e);
            }
        }
        if (records == 0) return new AppState();
        try {
            if (!versionSeen || !clockSeen || next == null || !footerSeen || assignments.size() != 4) {
                throw new IllegalArgumentException("Missing VERSION, CLOCK, NEXT_JOB_ID, four GPUs or END.");
            }
            Scheduler scheduler = null;
            if (current == null) {
                if (!students.isEmpty() || !jobs.isEmpty() || !grants.isEmpty() ||
                    assignments.values().stream().anyMatch(Objects::nonNull)) {
                    throw new IllegalArgumentException("Execution data exists without a clock.");
                }
            } else {
                SimulationClock clock = new SimulationClock(current);
                scheduler = Scheduler.restore(clock, initial, students, jobs, assignments);
                Administrator admin = new Administrator(scheduler, clock);
                Map<String, Long> grantTotals = new HashMap<>();
                for (TimeGrant grant : grants) {
                    if (!studentIds.contains(grant.getStudentId()) || grant.getTime().isBefore(initial) ||
                        grant.getTime().isAfter(current)) {
                        throw new IllegalArgumentException("Grant has unknown student or invalid time.");
                    }
                    grantTotals.put(grant.getStudentId(),
                        grantTotals.getOrDefault(grant.getStudentId(), 0L) + grant.getMinutes());
                }
                for (Student student : students) {
                    if (grantTotals.getOrDefault(student.getStudentId(), 0L) > student.getAllowance().getAllocated()) {
                        throw new IllegalArgumentException("Grants exceed total allocation.");
                    }
                }
                admin.restoreGrantHistory(grants);
            }
            for (AccessRequest request : requests) {
                boolean accountExists = studentIds.contains(String.valueOf(request.getId()));
                if ("Approved".equals(request.getStatus()) != accountExists) {
                    throw new IllegalArgumentException("Request status disagrees with student account: " + request.getId());
                }
                if (accountExists) {
                    for (Student student : students) {
                        if (student.getStudentId().equals(String.valueOf(request.getId())) &&
                            (!student.checkPassword(request.getPasswordForStorage()) ||
                             !student.getName().equals(request.getName()) ||
                             !student.getEmail().equals(request.getEmail()) ||
                             !student.getPhone().equals(request.getPhone()))) {
                            throw new IllegalArgumentException("Approved request disagrees with student details.");
                        }
                    }
                }
            }
            return AppState.restored(scheduler, requests, next);
        } catch (RuntimeException e) {
            throw new IOException("Saved records are inconsistent: " + e.getMessage(), e);
        }
    }

    private static String text(Object value) { return value == null ? null : value.toString(); }

    // Consume every expected field so misspellings and extra fields are errors.
    private static final class Row {
        private final Map<String, String> fields;
        Row(String line) { fields = RecordCodec.parse(line); }
        String optional(String key) {
            if (!fields.containsKey(key)) throw new IllegalArgumentException("Missing field: " + key);
            return fields.remove(key);
        }
        String required(String key) {
            String value = optional(key);
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException("Value required for " + key);
            }
            return value;
        }
        int integer(String key) { return Integer.parseInt(required(key)); }
        LocalDateTime date(String key, boolean nullable) {
            String value = nullable ? optional(key) : required(key);
            if (value == null) return null;
            LocalDateTime result = LocalDateTime.parse(value);
            if (result.getSecond() != 0 || result.getNano() != 0) {
                throw new IllegalArgumentException("Minute precision required for " + key);
            }
            return result;
        }
        void finish() {
            if (!fields.isEmpty()) throw new IllegalArgumentException("Unexpected fields: " + fields.keySet());
        }
    }
}
