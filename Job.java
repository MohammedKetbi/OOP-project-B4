import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class Job {
    //System fill-ins
    private String jobId;
    private String studentId;
    private int workloadUnits;
    private int estimatedDuration;
    private LocalDateTime submissionTime;
    private String status;
    private int assignedGpuId;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    private String cancelledBy;
    private String cancelledReason;
    private LocalDateTime cancellationTime;
    //Student types
    private String title;
    private String applicationName;
    private String description;
    private String taskSize;    


    public void complete(LocalDateTime currentTime){
        if (!status.equals("Running")) throw new IllegalStateException("Only a running job can complete");
        checkTime(currentTime, startTime);
        LocalDateTime expectedFinish = startTime.plusMinutes(estimatedDuration);

        if (currentTime.isBefore(expectedFinish)) throw new IllegalStateException("This job did not finish running");
        this.finishTime = expectedFinish;
        this.status = "Completed";
    }


    public void cancel(LocalDateTime currentTime, String cancelledBy, String cancelledReason){
        if (!status.equals("Queued") && !status.equals("Running")) throw new IllegalStateException("Only queued or running jobs can be cancelled");
        checkText(cancelledBy, "Cancelling user");
        checkText(cancelledReason, "Cancellation reason");
        checkTime(currentTime, startTime);
        if (status.equals("Running")) {
            checkTime(currentTime, startTime);
            LocalDateTime expectedFinish = startTime.plusMinutes(estimatedDuration);
            if (currentTime.isAfter(expectedFinish)) throw new IllegalStateException("The runtime has ended. Complete this job instead");
            this.finishTime = currentTime;

        }
        this.cancellationTime = currentTime;
        this.cancelledBy = cancelledBy;
        this.cancelledReason = cancelledReason; 
        this.status = "Cancelled";
    }
    public void start(int gpuId, LocalDateTime currentTime){
        if (!status.equals("Queued")) throw new IllegalStateException("Only queued job can start");
        
        if (gpuId < 1 || gpuId > 4) throw new IllegalArgumentException("GPU ID must be from 1 - 4");
        checkTime(currentTime, submissionTime);
        this.assignedGpuId = gpuId;
        this.startTime = currentTime;
        this.status = "Running";
    }  



    //Job constructor
    public Job(String jobId, String studentId, String title,
        String applicationName, String description, String taskSize,
        LocalDateTime submissionTime){
            this.jobId = jobId;
            this.studentId = studentId;
            this.title = title;
            this.applicationName = applicationName;
            this.description = description;
            this.taskSize = taskSize;
            this.submissionTime = submissionTime;




            if (taskSize.equals("Small")) this.workloadUnits = 5;
            else if (taskSize.equals("Medium")) this.workloadUnits = 15;
            else if (taskSize.equals("Large")) this.workloadUnits = 30;
            else if (taskSize.equals("Very large")) this.workloadUnits = 60;
            else throw new IllegalArgumentException("Invalid task size");

            this.estimatedDuration = workloadUnits;
            this.status = "Queued";
            this.assignedGpuId = 0;
            this.startTime = null;
            this.finishTime = null;
            this.cancellationTime = null;
            this.cancelledBy = null;
            this.cancelledReason = null;

        }

    public int getElapsedRuntime(LocalDateTime currenTime){
        checkTime(currenTime, startTime);
        if (startTime == null) return 0;
        checkTime(currenTime, submissionTime);
        LocalDateTime endTime = currenTime;
        if (finishTime != null) {
            checkTime(currenTime, finishTime);
            endTime = finishTime;
        }
        long minutes = Duration.between(startTime, endTime).toMinutes();
        if (minutes > estimatedDuration) return estimatedDuration;
        return (int) minutes;
    }


    public int getRemainingRuntime(LocalDateTime currenTime) {
        int elapsed = getElapsedRuntime(currenTime);
        if (status.equals("Completed") || status.equals("Cancelled")) return 0;
        return estimatedDuration - elapsed;
    }


    public long getActualQueueWait(){
        if (startTime == null) throw new IllegalStateException("This job has not start");
        return Duration.between(submissionTime, startTime).toMinutes();  
    }

    public int getChargeableMinutes() {
        if (status.equals("Completed")) return estimatedDuration;
        else if (status.equals("Cancelled")) {
            if (startTime == null) return 0;
       
        return (int) Duration.between(startTime, finishTime).toMinutes(); }
        else {throw new IllegalStateException("This job has no fina charge yet");}

    }




    public void checkText(String text, String fieldName) {
        if (text == null || text.trim().isEmpty()) throw new IllegalArgumentException(fieldName + "cannot be empty");

    }

    public void checkTime(LocalDateTime time, LocalDateTime earliestTime) {
        if (earliestTime == null) earliestTime = submissionTime;
        if (time == null || time.isBefore(earliestTime)) throw new IllegalArgumentException("Time is missing or before the recorded event");

    }
    public String formatTime(LocalDateTime time){
        if (time == null) return "Not set";
       DateTimeFormatter format =  DateTimeFormatter.ofPattern("dd//MM//yyyy HH:mm");
        return time.format(format);
    }
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

    @Override 
    public String toString(){
        return "Job ID: " + jobId + ", Student ID: " + studentId + ", Title: " + title +  ", Application: " + applicationName + ", Application " + applicationName +
        ", Description: " + description + ", Task size: " + taskSize + ", Workload units: " + workloadUnits + ", Duration: " + estimatedDuration + " minutes" + ", Status: " + status +
        ", GPU ID: " + assignedGpuId + ", Submitted: " + formatTime(submissionTime) + ", Started: " + formatTime(startTime) + ", Finished: " + formatTime(finishTime) + ", Cancelled: " + formatTime(cancellationTime) + 
        ", Cancelled by:" + cancelledBy + ", Cancellation reason: " + cancelledReason;
    }
        

}