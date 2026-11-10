

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import model.Student;
import model.User;
public class Administrator extends User {

private final Scheduler scheduler;
private final SimulationClock clock;
private final List<String> grantHistory = new ArrayList<>();

public Administrator(Scheduler scheduler, SimulationClock clock) {
super("admin", "gpu2026");

if (scheduler == null || clock == null) {
throw new IllegalArgumentException("Scheduler and simulation clock are required.");
 }
this.scheduler = scheduler;
this.clock = clock;
scheduler.registerAdministrator(this);
 }

public Student approve(AccessRequest request, String password) {
     return approve(request, password, 100);
    }
public Student approve(AccessRequest request, String password,
int allocatedMinutes) {
checkPending(request);
if (password == null || password.trim().isEmpty()) {
throw new IllegalArgumentException("The student's registration password is required.");
}
if (allocatedMinutes <= 0) {
throw new IllegalArgumentException("Initial GPU minutes must be positive.");
 }

Student student = new Student( String.valueOf(request.getId()),
password, request.getName(), request.getEmail(), request.getPhone(), allocatedMinutes);
scheduler.registerStudent(student);
request.approve();
return student;
  }

    public void reject(AccessRequest request) {
        checkPending(request);
        request.reject();
    }

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
LocalDateTime time = clock.getTime();
if (time == null) {
throw new IllegalStateException("Set the simulation clock before granting time.");
}
  DateTimeFormatter format = 
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
String record = "Student: " + student.getStudentId() + ", Minutes: " + minutes+ ", Granted by: " + getUsername()
+ ", Time: " + time.format(format) + ",Reason" + reason.trim();
student.getAllowance().grantTime(minutes);
grantHistory.add(record);
}
public void cancelJob(Job job, String reason) {
        scheduler.cancel(job, this, reason);
    }

public List<String> getGrantHistory() {
return Collections.unmodifiableList(new ArrayList<>(grantHistory));
}
private void checkPending(AccessRequest request) {
if (request == null) {
throw new IllegalArgumentException("A request is required.");
 }
if (!"Pending".equals(request.getStatus())) {
throw new IllegalStateException("This request has already been processed.");
}
}
}