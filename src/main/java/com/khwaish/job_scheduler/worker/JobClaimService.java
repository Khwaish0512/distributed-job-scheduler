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
    public void markJobFailed(Long jobId, LocalDateTime nextAttemptAt, String errorMessage) {
        jobRepository.markJobFailed(jobId, nextAttemptAt, errorMessage);
    }
}