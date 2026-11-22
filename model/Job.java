package model;

import java.time.Duration; //Duration calculates the number of minutes between two time instances
import java.time.LocalDateTime; //LocalDateTime represents a date and time without timezone information, where all times in this class come from the simulation clock to avoid the common mistake in the slides
import java.time.format.DateTimeFormatter; //DateTimeFormatter formats a date and time into a string for display


 /* A job represents a task submitted by a student
 this job may be queued, running, completed, or cancelled, but it starts
 in the queued state, then it may transition to running, completed, or cancelled */

public class Job {
    //System fill-ins
    private String jobId; // Unique identifier for the job
    private String studentId; // Unique identifier for the student who submitted the job
    private int workloadUnits; // The number of workload units for the job, which is determined by the task size
    private int estimatedDuration; // The estimated duration of the job in minutes
    private LocalDateTime submissionTime; // The time when the job was submitted
    private String status; // The current status of the job queued, running, completed, or cancelled
    private int assignedGpuId; // The ID of the GPU assigned to the job, 0 if no GPU is assigned
    private LocalDateTime startTime; // The time when the job started running
    private LocalDateTime finishTime; // The time when the job finished running, or it was cancelled
    private String cancelledBy; // The user who cancelled the job
    private String cancelledReason; // The reason for cancelling the job
    private LocalDateTime cancellationTime; // The time when the job was cancelled

    //Student types
    private String title; // The title of the job, provided by the student
    private String applicationName; // The name of the application for the job, provided by the student
    private String description; // The description of the job, provided by the student
    private String taskSize; // The size of the task, provided by the student


    /* Completes the job if it is currently running, the job cannot be completed if it is not running */
    public void complete(LocalDateTime currentTime){
        //Check if the job is currently running, if not (queued or completed or cancelled) throw an exception
        if (!status.equals("Running")) throw new IllegalStateException("Only a running job can complete");
        checkTime(currentTime, startTime); // the current time cannot be before the start time of the job
        LocalDateTime expectedFinish = startTime.plusMinutes(estimatedDuration); //calculate the expected finish time based on the start time and estimated duration

        if (currentTime.isBefore(expectedFinish)) throw new IllegalStateException("This job did not finish running"); // the current time cannot be before the expected finish time
        this.finishTime = expectedFinish; // the finish time is set to the expected finish time
        this.status = "Completed"; // the status of the job is set to completed
    }

    /* Cancels the job if it is currently queued or running, the job cannot be cancelled if it is completed or already cancelled 
    A queued job can be cancelled at any time since it does not have a start time, but running job records the current time as the finish time for charging minutes purposes*/
    public void cancel(LocalDateTime currentTime, String cancelledBy, String cancelledReason){
        //Completed and cancelled jobs cannot be cancelled again, only queued or running jobs can be cancelled
        if (!status.equals("Queued") && !status.equals("Running")) throw new IllegalStateException("Only queued or running jobs can be cancelled");
        checkText(cancelledBy, "Cancelling user"); // Record the user who cancelled the job
        checkText(cancelledReason, "Cancellation reason"); // Record the reason for cancellation
        checkTime(currentTime, startTime); // for queued jobs, checkTime uses submission Time because startTime is null
        if (status.equals("Running")) { 
            checkTime(currentTime, startTime); // the current time cannot be before the start time of the job
            LocalDateTime expectedFinish = startTime.plusMinutes(estimatedDuration); // the expected finish time is calculated based on the start time and estimated duration
            if (currentTime.isAfter(expectedFinish)) throw new IllegalStateException("The runtime has ended. Complete this job instead"); // the current time cannot be after the expected finish time, if it is, the job should be completed instead of cancelled
            this.finishTime = currentTime; //assign the finish time to the current time for charging minutes purposes

        }
        this.cancellationTime = currentTime; // the cancellation time is set to the current time
        this.cancelledBy = cancelledBy; //Record the user who cancelled the job
        this.cancelledReason = cancelledReason; //Record the reason for cancellation
        this.status = "Cancelled"; // the status of the job is set to cancelled
    }

    // Starts the job if it is currently queued, the job cannot be started if it is running, completed, or cancelled
    public void start(int gpuId, LocalDateTime currentTime){
        if (!status.equals("Queued")) throw new IllegalStateException("Only queued job can start"); //checking if the job is currently queued, if not (running or completed or cancelled) throw an exception
        
        if (gpuId < 1 || gpuId > 4) throw new IllegalArgumentException("GPU ID must be from 1 - 4"); //checking if the GPU ID is valid, if not throw an exception
        checkTime(currentTime, submissionTime); // the current time cannot be before the submission time of the job
        this.assignedGpuId = gpuId; // the GPU ID is assigned to the job
        this.startTime = currentTime; // the start time is assigned to the current time
        this.status = "Running"; // the status of the job is set to running
    }  



    //Job constructor, creates a new job with the given parameters, and initializes the other fields to their default values
    public Job(String jobId, String studentId, String title,
        String applicationName, String description, String taskSize,
        LocalDateTime submissionTime){
            if (jobId == null || !jobId.matches("J[0-9]+")){throw new IllegalArgumentException("Job ID must be like J101");} //Job ID must start with J followed by digits (validation is done using .matches() method with a regular expression returning true if the job ID is valid, false otherwise)
            checkText(studentId, "Student ID"); //comparing the student ID to null or empty string, if it is, throw an exception
            checkText(title, "Title"); //comparing the title to null or empty string, if it is, throw an exception
            checkText(applicationName, "Application name"); //comparing the application name to null or empty string, if it is, throw an exception
            checkText(description, "Description"); //comparing the description to null or empty string, if it is, throw an exception
            checkText(taskSize, "Task size"); //comparing the task size to null or empty string, if it is, throw an exception
            if (submissionTime == null) {throw new IllegalArgumentException("Submission time is required");} //checking if the submission time is null, if it is, throw an exception
            
            this.jobId = jobId; // assigning the job ID to the job
            this.studentId = studentId; // assigning the student ID to the job
            this.title = title; // assigning the title to the job
            this.applicationName = applicationName; // assigning the application name to the job
            this.description = description; // assigning the description to the job
            this.taskSize = taskSize; // assigning the task size to the job
            this.submissionTime = submissionTime; // assigning the submission time to the job



            // Assign workload units based on task size, and set estimated duration to the same value as workload units
            if (taskSize.equals("Small")) this.workloadUnits = 5;
            else if (taskSize.equals("Medium")) this.workloadUnits = 15;
            else if (taskSize.equals("Large")) this.workloadUnits = 30;
            else if (taskSize.equals("Very large")) this.workloadUnits = 60;
            else throw new IllegalArgumentException("Invalid task size");


            this.estimatedDuration = workloadUnits; // The estimated duration is set to the same value as workload units, as per the requirements of the system.
            this.status = "Queued"; // The initial status of the job is set to "Queued" since it has not started yet
            this.assignedGpuId = 0; // The assigned GPU ID is set to 0, indicating that no GPU has been assigned yet
            this.startTime = null; //assigning the start time to null, indicating that the job has not started yet
            this.finishTime = null; //assigning the finish time to null, indicating that the job has not finished yet
            this.cancellationTime = null; //assigning the cancellation time to null, indicating that the job has not been cancelled yet
            this.cancelledBy = null; //assigning the cancelled by field to null, indicating that the job has not been cancelled yet
            this.cancelledReason = null; //assigning the cancelled reason to null, indicating that the job has not been cancelled yet

        }
 /* Returns the elapsed runtime (the number of minutes the job has been running) of the job in minutes */
    public int getElapsedRuntime(LocalDateTime currenTime){
        if (startTime == null) return 0; // If the job has not started yet, the elapsed runtime is 0
        checkTime(currenTime, startTime); // The current time cannot be before the start time of the job, if it is, throw an exception
        LocalDateTime endTime = currenTime; // The end time is set to the current time, but if the job has finished, the end time is set to the finish time 
        if (finishTime != null) { // If the job has finished, the end time is set to the finish time, but we still need to check if the finish time is valid (not before the start time)
            checkTime(currenTime, finishTime); // The current time cannot be before the finish time of the job, if it is, throw an exception
            endTime = finishTime; // The end time is set to the finish time of the job
        }
        long minutes = Duration.between(startTime, endTime).toMinutes(); // Calculate the number of minutes between the start time and the end time of the job
        if (minutes > estimatedDuration) return estimatedDuration; // If the elapsed runtime is greater than the estimated duration, return the estimated duration instead
        return (int) minutes; // Return the elapsed runtime in minutes as an integer
    }

    /* Returns the remaining runtime (the number of minutes left for the job to complete) of the job in minutes */
    public int getRemainingRuntime(LocalDateTime currenTime) {
        int elapsed = getElapsedRuntime(currenTime); // Get the elapsed runtime of the job in minutes
        if (status.equals("Completed") || status.equals("Cancelled")) return 0; // If the job is completed or cancelled, the remaining runtime is 0
        return estimatedDuration - elapsed; // Return the remaining runtime in minutes by subtracting the elapsed runtime from the estimated duration
    }

    /* Returns the actual queue wait time (the number of minutes the job waited in the queue before starting) of the job in minutes */
    public long getActualQueueWait(){
        if (startTime == null) throw new IllegalStateException("This job has not start"); // If the job has not started yet, throw an exception
        return Duration.between(submissionTime, startTime).toMinutes();  // Calculate the number of minutes between the submission time and the start time of the job, which is the actual queue wait time
    }
 /* Returns the chargeable minutes (the number of minutes the job is charged for) of the job in minutes */
    public int getChargeableMinutes() {
        if (status.equals("Completed")) return estimatedDuration; // If the job is completed, the chargeable minutes is equal to the estimated duration
        else if (status.equals("Cancelled")) { 
            if (startTime == null) return 0; // If the job is cancelled before it starts, the chargeable minutes is 0
       
        return (int) Duration.between(startTime, finishTime).toMinutes();  // If the job is cancelled, the chargeable minutes is equal to the number of minutes between the start time and the finish time of the job
        } else {throw new IllegalStateException("This job has no fina charge yet");}

    }



    /* Checks if the provided text is valid (not null and not empty and removes trailing and leading whitespace) */
    public void checkText(String text, String fieldName) {
        if (text == null || text.trim().isEmpty()) throw new IllegalArgumentException(fieldName + " cannot be empty."); // If the text is null or empty, throw an exception with the field name

    }
 /* Checks if the provided time is valid (not null and not before the earliest allowed time) */
    public void checkTime(LocalDateTime time, LocalDateTime earliestTime) {
        if (earliestTime == null) earliestTime = submissionTime; // If the earliest allowed time is null, set it to the submission time of the job
        if (time == null || time.isBefore(earliestTime)) throw new IllegalArgumentException("Time is missing or before the recorded event"); // If the time is null or before the earliest allowed time, throw an exception

    }
    /* Formats a LocalDateTime object into a string representation */
    public String formatTime(LocalDateTime time){
        if (time == null) return "Not set"; // If the time is null, return "Not set" to indicate that the time is not available
       DateTimeFormatter format =  DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"); // Create a DateTimeFormatter object with the desired format pattern (day/month/year hour:minute)
        return time.format(format); /// Format the LocalDateTime object into a string representation using the specified format pattern and return it
    }
    /* Getter methods since these attributes are private */
    public String getJobId() {
        return jobId;
    }
    public String getStudentId() {
        return studentId;
    }
    public int getWorkloadUnits() {
        return workloadUnits;
    }
    public int getEstimatedDuration() {
        return estimatedDuration;
    }
    public LocalDateTime getSubmissionTime() {
        return submissionTime;
    }
    public String getStatus() {
        return status;
    }
    public int getAssignedGpuId() {
        return assignedGpuId;
    }
    public LocalDateTime getStartTime() {
        return startTime;
    }
    public LocalDateTime getFinishTime() {
        return finishTime;
    }
    public String getCancelledBy() {
        return cancelledBy;
    }
    public String getCancelledReason() {
        return cancelledReason;
    }
    public LocalDateTime getCancellationTime() {
        return cancellationTime;
    }
    public String getTitle() {
        return title;
    }
    public String getApplicationName() {
        return applicationName;
    }
    public String getDescription() {
        return description;
    }
    public String getTaskSize() {
        return taskSize;
    }
    /* Returns a string representation of the job, including its attributes and their values */
    @Override 
    public String toString(){
        return "Job ID: " + jobId + ", Student ID: " + studentId + ", Title: " + title +  ", Application: " + applicationName  +
        ", Description: " + description + ", Task size: " + taskSize + ", Workload units: " + workloadUnits + ", Duration: " + estimatedDuration + " minutes" + ", Status: " + status +
        ", GPU ID: " + assignedGpuId + ", Submitted: " + formatTime(submissionTime) + ", Started: " + formatTime(startTime) + ", Finished: " + formatTime(finishTime) + ", Cancelled: " + formatTime(cancellationTime) + 
        ", Cancelled by:" + cancelledBy + ", Cancellation reason: " + cancelledReason;
    }
        


    // STORAGE: rebuild saved fields without start(), complete() or cancel().
    // Scheduler.restore() validates timeline, ownership, balances and assignments.
    public static Job restore(String jobId, String studentId, String title,
            String applicationName, String description, String taskSize,
            LocalDateTime submitted, String status, int gpuId,
            LocalDateTime started, LocalDateTime finished,
            LocalDateTime cancelled, String by, String reason) {
        Job job = new Job(jobId, studentId, title, applicationName, description,
                          taskSize, submitted);
        job.status = status;
        job.assignedGpuId = gpuId;
        job.startTime = started;
        job.finishTime = finished;
        job.cancellationTime = cancelled;
        job.cancelledBy = by;
        job.cancelledReason = reason;
        return job;
    }

}