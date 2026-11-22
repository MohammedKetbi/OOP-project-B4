

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import model.Student;
import model.User;

// Administrator account

public class Administrator extends User {

// Shared scheduler and clock
private final Scheduler scheduler;
private final SimulationClock clock;

// Records extra time grants
private final List<String> grantHistory = new ArrayList<>();

// Create and register administrator

public Administrator(Scheduler scheduler, SimulationClock clock) {
super("admin", "gpu2026");

if (scheduler == null || clock == null) {
throw new IllegalArgumentException("Scheduler and simulation clock are required.");
 }
this.scheduler = scheduler;
this.clock = clock;
scheduler.registerAdministrator(this);
 }

// Approve with 100 GPU minutes
public Student approve(AccessRequest request, String password) {
     return approve(request, password, 100);
    }
    // Approve with chosen GPU minutes
public Student approve(AccessRequest request, String password,
int allocatedMinutes) {
checkPending(request);
// Validate password
if (password == null || password.trim().isEmpty()) {
throw new IllegalArgumentException("The student's registration password is required.");
}
// Validate initial allowance
if (allocatedMinutes <= 0) {
throw new IllegalArgumentException("Initial GPU minutes must be positive.");
 }
// Create student account
Student student = new Student( String.valueOf(request.getId()),
password, request.getName(), request.getEmail(), request.getPhone(), allocatedMinutes);
// Register student before approving
scheduler.registerStudent(student);
request.approve();
return student;
  }
    // Reject a pending request
    public void reject(AccessRequest request) {
        checkPending(request);
        request.reject();
    }
    // Grant extra GPU minutes
    public void grantTime(Student student, int minutes, String reason) {
if (student == null) {
throw new IllegalArgumentException("A student is required."); 
  }
if (minutes <= 0) {
throw new IllegalArgumentException("Granted minutes must be positive.");
}
if (reason == null || reason.trim().isEmpty()) {
throw new IllegalArgumentException("A reason is required.");
 }
 // Use simulation time
LocalDateTime time = clock.getTime();
if (time == null) {
throw new IllegalStateException("Set the simulation clock before granting time.");
}
  // Build grant record
  DateTimeFormatter format = 
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
String record = "Student: " + student.getStudentId() + ", Minutes: " + minutes+ ", Granted by: " + getUsername()
+ ", Time: " + time.format(format) + ",Reason" + reason.trim();
// Update allowance and history
student.getAllowance().grantTime(minutes);
grantHistory.add(record);
}
// Cancel through the scheduler
public void cancelJob(Job job, String reason) {
        scheduler.cancel(job, this, reason);
    }
// Return a read-only history copy
public List<String> getGrantHistory() {
return Collections.unmodifiableList(new ArrayList<>(grantHistory));
}
// Check request exists and is pending
private void checkPending(AccessRequest request) {
if (request == null) {
throw new IllegalArgumentException("A request is required.");
 }
if (!"Pending".equals(request.getStatus())) {
throw new IllegalStateException("This request has already been processed.");
}
}
}