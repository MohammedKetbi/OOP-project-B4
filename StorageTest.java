import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import logic.*;
import model.*;
import storage.DataStore;

/** Automated round-trip and invalid-file tests. Does NOT touch your data.txt. */
public class StorageTest {
    private static int checks;
    private static Path directory;
    private static DataStore store;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static void rejects(String contents, String message) throws Exception {
        Files.writeString(store.getFile(), contents, StandardCharsets.UTF_8);
        try { store.load(); throw new AssertionError("Accepted " + message); }
        catch (IOException expected) {
            check(expected.getMessage().contains(store.getFile().toString()), "File path in error");
        }
    }
    private static Student owner(AppState state, String id) {
        for (Student student : state.getStudents()) if (id.equals(student.getStudentId())) return student;
        throw new AssertionError("Missing student");
    }
    public static void main(String[] args) throws Exception {
        directory = Files.createTempDirectory("gpu-storage-test-");
        store = new DataStore(directory.resolve("data.txt"));
        check(!store.load().isClockSet(), "Missing file is first run");
        check(!Files.exists(store.getFile()), "Load does not create file");
        Files.writeString(store.getFile(), " \n\n");
        check(!store.load().isClockSet(), "Blank file is first run");
        AppState unset = new AppState();
        unset.addRequest(new AccessRequest(9, "Pending, name", "p@example.com", "123", "NONE"));
        store.save(unset);
        AppState unsetLoaded = store.load();
        check(!unsetLoaded.isClockSet(), "Unset clock stays unset");
        check("NONE".equals(unsetLoaded.getRequests().get(0).getPasswordForStorage()), "Literal NONE preserved");
        String[] values = {"a,b:c", "\\", "\\,", "  leading and trailing  ", "line\nnext\r\t", "NONE", "\\0", "أحمد", "🙂"};
        for (String value : values) {
            AppState pending = new AppState();
            pending.addRequest(new AccessRequest(10, value, "a@a", "123", value));
            store.save(pending);
            AccessRequest back = store.load().getRequests().get(0);
            check(value.equals(back.getName()) && value.equals(back.getPasswordForStorage()), "Escaping round trip");
        }
        AppState original = StorageConsoleDemo.createSample();
        store.save(original);
        String before = Files.readString(store.getFile());
        AppState loaded = store.load();
        check(loaded.getClock().getTime().equals(original.getClock().getTime()), "Clock pauses");
        check(loaded.getScheduler().getInitialTime().equals(original.getScheduler().getInitialTime()), "Original clock restored");
        check(loaded.getScheduler().getQueue().size() == 1, "Queue restored");
        check(loaded.getScheduler().getQueue().get(0).getJobId().equals("J107"), "Queue identity/order");
        check(loaded.getScheduler().getRemainingWait(loaded.getScheduler().getQueue().get(0)) == 13, "Predicted wait restored");
        check(loaded.getScheduler().getAllJobs().size() == 8, "All states/history restored");
        check(loaded.getRequests().size() == 7, "All request statuses restored");
        check(loaded.getAdministrator().getGrantRecords().size() == 1, "Grant restored");
        check(loaded.getAdministrator().getGrantRecords().get(0).getReason().equals(
            original.getAdministrator().getGrantRecords().get(0).getReason()), "Grant text preserved");
        check(loaded.getScheduler().getUtilization().equals(original.getScheduler().getUtilization()), "Utilization preserved");
        for (Student student : loaded.getStudents()) {
            Student previous = owner(original, student.getStudentId());
            check(student.checkPassword(previous.getPasswordForStorage()), "Password restored");
            check(student.getAllowance().getUsed() == previous.getAllowance().getUsed(), "Used restored");
            check(student.getAllowance().getReserved() == previous.getAllowance().getReserved(), "Reservation restored");
        }
        for (GPU gpu : loaded.getScheduler().getGpus()) {
            Job running = gpu.getCurrentJob();
            check(running != null && running.getRemainingRuntime(loaded.getClock().getTime()) == 13, "Remaining runtime restored");
            check(loaded.getScheduler().getAllJobs().contains(running), "GPU references history object");
        }
        store.save(loaded);
        check(before.equals(Files.readString(store.getFile())), "Load/save is byte-identical");
        Student first = owner(loaded, "1001");
        Job waiting = loaded.getScheduler().getQueue().get(0);
        loaded.getScheduler().cancel(waiting, first, "Owner cancellation after reload");
        check("Cancelled".equals(waiting.getStatus()), "Owner identity works after load");
        loaded.getAdministrator().cancelJob(loaded.getScheduler().getGpus()[0].getCurrentJob(), "Admin after reload");
        check(first.getAllowance().getUsed() == 9, "Admin charge after load");
        for (int i = 0; i < 13; i++) loaded.getScheduler().advanceMinute();
        check(owner(loaded, "1002").getAllowance().getUsed() == 15, "Completion after reload charged once");
        store.save(loaded);
        AppState twice = store.load();
        twice.getScheduler().advanceMinute();
        check(owner(twice, "1002").getAllowance().getUsed() == 15, "No charge on second restart");
        check(twice.allocateJobId().equals("J109"), "ID continues");
        rejects(before.replace("version: 1", "version: 99"), "unsupported version");
        rejects(before.replace("reserved: 20", "reserved: 9999"), "negative available balance");
        rejects(before.replace("used: 7", "used: 8"), "balance/history disagreement");
        rejects(before.replace("currentJobId: J103", "currentJobId: J999"), "unknown GPU job");
        rejects(before.replace("currentJobId: J103", "currentJobId: NONE"), "missing running assignment");
        rejects(before.replace("status: Running", "status: Mystery"), "unknown status");
        rejects(before.replace("estimatedDuration: 15", "estimatedDuration: 16"), "wrong duration");
        rejects(before.replace("initialTime: 2026-11-18T09:50", "initialTime: 2026-11-19T09:50"), "future initial time");
        rejects(before.replace("nextJobId: 109", "nextJobId: 101"), "reused ID counter");
        rejects(before.substring(0, before.lastIndexOf("recordType: END")), "truncated footer");
        rejects(before + "recordType: VERSION, version: 1\n", "trailing data");
        rejects(before.replace("name: Student 1001", "name: Student 1001, name: duplicate"), "duplicate key");
        rejects(before.replace("name: Student 1001", "name: bad\\q"), "unknown escape");
        rejects("garbage", "malformed record");
        // Validation failure must preserve the old bytes.
        store.save(original);
        String valid = Files.readString(store.getFile());
        original.getStudents().get(0).getAllowance().reserve(1); // intentionally inconsistent
        try { store.save(original); throw new AssertionError("Saved inconsistent state"); }
        catch (IOException expected) { check(valid.equals(Files.readString(store.getFile())), "Old save survives failed save"); }
        Path blocking = directory.resolve("not-a-folder");
        Files.writeString(blocking, "block");
        try { new DataStore(blocking.resolve("data.txt")).save(new AppState()); throw new AssertionError("Ignored write error"); }
        catch (IOException expected) { check(true, "Write error reported"); }
        store.save(new AppState());
        check(!store.load().isClockSet(), "Empty initialized-file round trip");
        System.out.println("Passed " + checks + " storage checks. Temporary test directory: " + directory);
    }
}
