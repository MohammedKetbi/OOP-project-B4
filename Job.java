class Job {
    //System fill-ins
    private String jobId;
    private String studentId;
    private int workloadUnits;
    private int estimatedDuration;
    private String submissionTime;
    private String status = "Queued";
    private String assignedGpuId;
    private String startTime;
    private String finishTime;
    private String cancelInfo;
    //Student types
    private String title;
    private String applicationName;
    private String description;
    private String taskSize;    



    public void complete(){}
    public void cancel(String by, String who cancelled, String reason){}
    public void start(){}
}