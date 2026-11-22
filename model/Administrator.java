package model;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import logic.Scheduler;
import logic.SimulationClock;

// Administrator account
public class Administrator extends User {

// Shared scheduler and clock
private final Scheduler scheduler;
private final SimulationClock clock;

// Display history of time grants
private final List<String> grantHistory = new ArrayList<>();

// Create and register administrator
public Administrator(Scheduler scheduler, SimulationClock clock) {
super("admin", "gpu2026");

// Check required objects
if (scheduler == null || clock == null) {
throw new IllegalArgumentException("Scheduler and simulation clock are required.");
 }

 // Require the same simulation clock
 if (scheduler.getClock() != clock) {throw new IllegalArgumentException("Scheduler and administrator must use the same clock.");}
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

// Validate registration password
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

// Register student and approve request
scheduler.registerStudent(student);
request.rememberPassword(password);
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

  // Check student is registered
boolean registered = false;
for(Student existing: scheduler.getStudents()){
    if (existing == student){
        registered = true;
        break;
    }
}
 if (!registered) {throw new IllegalArgumentException("Student is not registered.");}

 // Validate granted minutes
if (minutes <= 0) {
throw new IllegalArgumentException("Granted minutes must be positive.");
}

// Require a grant reason
if (reason == null || reason.trim().isEmpty()) {
throw new IllegalArgumentException("A reason is required.");
 }

 // Use simulation time
LocalDateTime time = clock.getTime();
if (time == null) {
throw new IllegalStateException("Set the simulation clock before granting time.");
}

// Build the display record
  DateTimeFormatter format = 
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
String record = "Student: " + student.getStudentId() + ", Minutes: " + minutes+ ", Granted by: " + getUsername()
+ ", Time: " + time.format(format) + ", Reason: " + reason.trim();

// Update allowance and both histories
student.getAllowance().grantTime(minutes);
grantHistory.add(record);
grantRecords.add(new TimeGrant(student.getStudentId(), minutes, getUsername(), time, reason.trim()));
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
// Structured records for storage
    private final List<TimeGrant> grantRecords = new ArrayList<>();

    // Return a read-only records copy
    public List<TimeGrant> getGrantRecords() {
        return Collections.unmodifiableList(new ArrayList<>(grantRecords));
    }

    // Restore saved grant history
    public void restoreGrantHistory(List<TimeGrant> records) {
        if (!grantRecords.isEmpty() || !grantHistory.isEmpty()) {
            throw new IllegalStateException("Grant history already exists.");
        }

        // Rebuild records and display history
        for (TimeGrant record : records) {
            grantRecords.add(record);
            grantHistory.add(record.toString());
        }
    }

}