/*this class is a work in progress as of 4/11/2026 as it
 requires other class objects which have not been completed
  yet current methods are jest templetes for logic  */

import java.util.*;
import model.User;
import java.time.*;
public class Scheduler {

    private List<Job> queue=new ArrayList<Job>(); //records all jobs that are currently in the queue to be run
    private List<Job> allJobs=new ArrayList<Job>(); //records all jobs submitted to the scheduler for record keeping

    private GPU[] gpus; //array of gpus that the scheduler will use to run jobs
    private SimulationClock simClock; //simulation clock object to keep track of time in the simulation

    //complete constructor for scheduler class that takes in a simulation clock and an array of gpus to initialize the scheduler
    public Scheduler(SimulationClock simClock,GPU[] gpus){
        this.simClock=simClock;
        this.gpus=gpus;
        
    }


    //submit method to submit a job to the scheduler and add it to the queue and allJobs list if the job is valid and the user has enough allowance to run the job
    public void submit(Job j) {
    if(j==null ||allJobs.contains(j)){
        throw new IllegalArgumentException("Job is invalid or already submitted");
        }

    if (j.getDuration() <= 0) {
        throw new IllegalArgumentException(
            "Duration must be positive."
        );
    }

    if (j.getDuration() >
        j.getOwner().getAllowance().available()) {

        throw new IllegalArgumentException(
            "Not enough available minutes."
        );
    }

        j.getOwner().getAllowance().reserve(j.getDuration());

        queue.add(j);
        allJobs.add(j);
        
    }

    //advanceMinute method to advance the simulation clock by 1 minute and advance all jobs that are currently running by 1 minute
    public void advanceMinute(){
        simClock.advance();
        for(int i=0;i<allJobs.size();i=i+1){
            if(allJobs.get(i).getStatus().equals("Running") ){
                allJobs.get(i).advanceMinute();

            }
        }
    }
    //cancel method to cancel a job if the user is the owner of the job or an administrator and the job is currently running or queued
    public void cancel(Job job, User by, String why){
        if((by.getUsername().equals(job.getOwner()) || by instanceof Administrator) && ((job.getStatus()).equals("Running") || (job.getStatus()).equals("Queued"))){
            job.setStatus("Cancelled");
            if(queue.contains(job)){
                queue.remove(job);
            }
        }

    }

}
