package com.khwaish.job_scheduler.worker;

import com.khwaish.job_scheduler.model.Job;
import com.khwaish.job_scheduler.repository.JobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class JobWorker {

    private final JobRepository jobRepository;
    private final String workerId = "worker-" + System.currentTimeMillis();

    @Value("${scheduler.polling.enabled:true}")
    private boolean pollingEnabled;

    public JobWorker(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional
    public Long claimNextJob() {
        Optional<Long> jobIdOpt = jobRepository.findNextClaimableJobId(LocalDateTime.now());

        if (jobIdOpt.isEmpty()) {
            return null;
        }

        Long jobId = jobIdOpt.get();
        jobRepository.markJobAsRunning(jobId, workerId);
        return jobId;
    }

    @Scheduled(fixedDelay = 2000)
    public void pollAndProcessJobs() {

        if (!pollingEnabled) {
            return; // scheduler disabled (e.g., during tests)
        }

        Long jobId = claimNextJob();

        if (jobId == null) {
            System.out.println("[Worker] No pending jobs. Waiting...");
            return;
        }

        System.out.println("[Worker] Claimed job id=" + jobId + ", processing...");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Job job = jobRepository.findById(jobId).orElseThrow();
        job.setStatus("COMPLETED");
        job.setUpdatedAt(LocalDateTime.now());
        jobRepository.save(job);

        System.out.println("[Worker] Completed job id=" + jobId);
    }
}