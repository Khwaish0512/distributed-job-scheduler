package com.khwaish.job_scheduler.worker;

import com.khwaish.job_scheduler.heartbeat.HeartbeatService;
import com.khwaish.job_scheduler.model.Job;
import com.khwaish.job_scheduler.repository.JobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class JobWorker {

    private final JobRepository jobRepository;
    private final HeartbeatService heartbeatService;
    private final JobClaimService jobClaimService;
    private final String workerId = "worker-" + System.currentTimeMillis();

    @Value("${scheduler.polling.enabled:true}")
    private boolean pollingEnabled;

    public JobWorker(JobRepository jobRepository, HeartbeatService heartbeatService, JobClaimService jobClaimService) {
        this.jobRepository = jobRepository;
        this.heartbeatService = heartbeatService;
        this.jobClaimService = jobClaimService;
    }

    @Scheduled(fixedDelay = 2000)
    public void pollAndProcessJobs() {

        if (!pollingEnabled) {
            return;
        }

        Long jobId = jobClaimService.claimNextJob(workerId);

        if (jobId == null) {
            System.out.println("[Worker " + workerId + "] No pending jobs. Waiting...");
            return;
        }

        System.out.println("[Worker " + workerId + "] Claimed job id=" + jobId + ", processing...");

        for (int i = 0; i < 3; i++) {
            heartbeatService.sendHeartbeat(workerId);
            updateHeartbeatInDb(jobId);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        Job job = jobRepository.findById(jobId).orElseThrow();
        job.setStatus("COMPLETED");
        job.setUpdatedAt(LocalDateTime.now());
        jobRepository.save(job);

        System.out.println("[Worker " + workerId + "] Completed job id=" + jobId);
    }

    private void updateHeartbeatInDb(Long jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow();
        job.setLastHeartbeat(LocalDateTime.now());
        jobRepository.save(job);
    }
}