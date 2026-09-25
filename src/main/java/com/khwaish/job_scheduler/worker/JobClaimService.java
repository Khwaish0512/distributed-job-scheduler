package com.khwaish.job_scheduler.worker;

import com.khwaish.job_scheduler.repository.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class JobClaimService {

    private final JobRepository jobRepository;

    public JobClaimService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional
    public Long claimNextJob(String workerId) {
        Optional<Long> jobIdOpt = jobRepository.findNextClaimableJobId(LocalDateTime.now());

        if (jobIdOpt.isEmpty()) {
            return null;
        }

        Long jobId = jobIdOpt.get();
        jobRepository.markJobAsRunning(jobId, workerId);
        return jobId;
    }

    @Transactional
    public boolean markJobCompleted(Long jobId, String workerId) {
        int rowsUpdated = jobRepository.markJobCompleted(jobId, workerId);
        return rowsUpdated > 0;
    }

    @Transactional
    public boolean markJobFailed(Long jobId, String workerId, LocalDateTime nextAttemptAt, String errorMessage) {
        int rowsUpdated = jobRepository.markJobFailed(jobId, workerId, nextAttemptAt, errorMessage);
        return rowsUpdated > 0;
    }
}