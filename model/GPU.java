package model;
import java.time.LocalDateTime; //Represents date and time
/* This class represents a GPU in the system.
   Each GPU has a unique ID and can be assigned a job (one job at a time) to process.
   The GPU can be free or busy, and it can release the current job when it is completed or cancelled. */


public class GPU {
    private int id; // Unique identifier for the GPU (must be between 1 and 4)
    private Job currentJob; // The job currently assigned to the GPU (null if the GPU is free)
    // Constructor for the GPU class. It takes an integer id as a parameter and initializes the GPU with that id. The id must be between 1 and 4, inclusive. If the id is outside this range, an IllegalArgumentException is thrown. The currentJob is initialized to null, indicating that the GPU is free.
    public GPU(int id){
        if (id < 1 || id > 4) throw new IllegalArgumentException("GPU ID must be from 1 - 4");
        this.id = id;
        this.currentJob = null;

    }
    // Getter methods for the GPU class. These methods allow access to the private attributes of the GPU class. The getId() method returns the unique identifier of the GPU, and the getCurrentJob() method returns the job currently assigned to the GPU (or null if the GPU is free).
    public int getId() {
        return id;
    }

    public Job getCurrentJob() {
        return currentJob;
    }
    // Checks if the GPU is free (not assigned to any job). Returns true if the GPU is free, false otherwise.
    public boolean isFree(){
        return currentJob == null;
    }
    // Assigns a job to the GPU. The assign() method takes a Job object and a LocalDateTime object (representing the current time) as parameters. It checks if the GPU is free and if the job is not null. If the GPU is busy or the job is null, an exception is thrown. If both checks pass, the job's start() method is called with the GPU's id and the current time, and the currentJob attribute is set to the assigned job.
    public void assign(Job job, LocalDateTime currenTime){
        if (!isFree()) throw new IllegalStateException("This GPU is busy");
        if (job == null) throw new IllegalArgumentException("Job is required");
        job.start(id, currenTime);
        this.currentJob = job;
    }
    // Releases the current job from the GPU. The release() method checks if the GPU is free or if the current job is not completed or cancelled. If either condition is true, an exception is thrown. If both checks pass, the currentJob attribute is set to null, indicating that the GPU is now free and it has finished processing the job.
    public void release(){
        if (isFree()) throw new IllegalStateException("This GPU is already free");
        if (!currentJob.getStatus().equals("Completed") && !currentJob.getStatus().equals("Cancelled")) throw new IllegalStateException("The running job must finish or be cancelled first");
        this.currentJob = null;

    }
    @Override // Returns a string representation of the GPU, including its id and status (free or busy). If the GPU is busy, it also includes the id of the current job being processed.
    public String toString(){
        if (isFree()) return "GPU ID: " + id + ", Status: Free"; // If the GPU is free, return a string indicating that it is free
        return "GPU ID: " + id + ", Status: Busy, Current job " + currentJob.getJobId(); // If the GPU is busy, return a string indicating that it is busy and include the id of the current job being processed
    }
    

    // STORAGE: reconnect an ALREADY running job; assign() would start it again.
    public void restoreCurrentJob(Job job) {
        if (!isFree() || job == null || !"Running".equals(job.getStatus()) ||
            job.getStartTime() == null || job.getAssignedGpuId() != id) {
            throw new IllegalArgumentException("Invalid saved GPU assignment.");
        }
        currentJob = job;
    }

}
