package com.khwaish.job_scheduler.worker;

import com.khwaish.job_scheduler.heartbeat.HeartbeatService;
import com.khwaish.job_scheduler.model.Job;
import com.khwaish.job_scheduler.repository.JobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

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

        try {
            executeJob(jobId);

            Job job = jobRepository.findById(jobId).orElseThrow();
            job.setStatus("COMPLETED");
            job.setUpdatedAt(LocalDateTime.now());
            jobRepository.save(job);

            System.out.println("[Worker " + workerId + "] Completed job id=" + jobId);

        } catch (Exception e) {
            handleFailure(jobId, e);
        }
    }

    private void executeJob(Long jobId) throws InterruptedException {

        Job job = jobRepository.findById(jobId).orElseThrow();

        for (int i = 0; i < 3; i++) {
            heartbeatService.sendHeartbeat(workerId);
            updateHeartbeatInDb(jobId);
            Thread.sleep(1000);
        }

        // Deliberate failure hook for testing: any job submitted with this taskType always fails
        if ("SIMULATE_FAILURE".equals(job.getTaskType())) {
            throw new RuntimeException("Simulated failure for testing retries/DLQ");
        }
    }

    private void handleFailure(Long jobId, Exception e) {

        Job job = jobRepository.findById(jobId).orElseThrow();
        int attempts = job.getAttempts();
        int maxAttempts = job.getMaxAttempts();

        long backoffSeconds = calculateBackoffSeconds(attempts);
        LocalDateTime nextAttemptAt = LocalDateTime.now().plusSeconds(backoffSeconds);
        String errorMessage = e.getClass().getSimpleName() + ": " + e.getMessage();

        jobClaimService.markJobFailed(jobId, nextAttemptAt, errorMessage);

        if (attempts >= maxAttempts) {
            System.out.println("[Worker " + workerId + "] Job id=" + jobId + " exhausted all " + maxAttempts + " attempts. Moved to DEAD. Error: " + errorMessage);
        } else {
            System.out.println("[Worker " + workerId + "] Job id=" + jobId + " failed (attempt " + attempts + "/" + maxAttempts + "). Retrying in " + backoffSeconds + "s. Error: " + errorMessage);
        }
    }

    private long calculateBackoffSeconds(int attempts) {
        long base = (long) Math.pow(2, attempts - 1) * 5; // 5s, 10s, 20s, 40s...
        long jitter = ThreadLocalRandom.current().nextLong(0, 3); // 0-2 extra seconds
        return base + jitter;
    }

    private void updateHeartbeatInDb(Long jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow();
        job.setLastHeartbeat(LocalDateTime.now());
        jobRepository.save(job);
    }
}