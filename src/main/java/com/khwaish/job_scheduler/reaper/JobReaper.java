package com.khwaish.job_scheduler.reaper;

import com.khwaish.job_scheduler.repository.JobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class JobReaper {

    private final JobRepository jobRepository;

    @Value("${scheduler.polling.enabled:true}")
    private boolean reaperEnabled;

    private static final int STALE_THRESHOLD_SECONDS = 15;

    public JobReaper(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void reclaimStaleJobs() {

        if (!reaperEnabled) {
            return;
        }

        LocalDateTime staleThreshold = LocalDateTime.now().minusSeconds(STALE_THRESHOLD_SECONDS);
        int reclaimed = jobRepository.reclaimStaleJobs(staleThreshold);

        if (reclaimed > 0) {
            System.out.println("[Reaper] Reclaimed " + reclaimed + " stale job(s).");
        }
    }
}