package model;
import java.time.LocalDateTime;


public class GPU {
    private int id;
    private Job currentJob;
    
    public GPU(int id){
        if (id < 1 || id > 4) throw new IllegalArgumentException("GPU ID must be from 1 - 4");
        this.id = id;
        this.currentJob = null;

    }
    
    public int getId() {
        return id;
    }

    public Job getCurrentJob() {
        return currentJob;
    }
    
    public boolean isFree(){
        return currentJob == null;
    }

    public void assign(Job job, LocalDateTime currenTime){
        if (!isFree()) throw new IllegalStateException("This GPU is busy");
        if (job == null) throw new IllegalArgumentException("Job is required");
        job.start(id, currenTime);
        this.currentJob = job;
    };
    public void release(){
        if (isFree()) throw new IllegalStateException("This GPU is already free");
        if (!currentJob.getStatus().equals("Completed") && !currentJob.getStatus().equals("Cancelled")) throw new IllegalStateException("The running job must finish or be cancelled first");
        this.currentJob = null;

    }
    @Override 
    public String toString(){
        if (isFree()) return "GPU ID: " + id + ", Status: Free";
        return "GPU ID: " + id + ", Status: Busy, Current job " + currentJob.getJobId();
    }
    

    // STORAGE: reconnect an ALREADY running job; assign() would start it again.
    public void restoreCurrentJob(Job job) {
        if (!isFree() || job == null || !"Running".equals(job.getStatus()) ||
            job.getAssignedGpuId() != id) {
            throw new IllegalArgumentException("Invalid saved GPU assignment.");
        }
        currentJob = job;
    }

}
