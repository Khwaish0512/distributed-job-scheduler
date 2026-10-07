package com.khwaish.job_scheduler.metrics;

import com.khwaish.job_scheduler.repository.JobRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class JobMetrics {

    private final Counter jobsCompletedCounter;
    private final Counter jobsFailedCounter;
    private final Counter jobsReclaimedCounter;

    public JobMetrics(MeterRegistry registry, StringRedisTemplate redisTemplate, JobRepository jobRepository) {
        this.jobsCompletedCounter = Counter.builder("jobs.completed.total")
                .description("Total number of jobs successfully completed")
                .register(registry);

        this.jobsFailedCounter = Counter.builder("jobs.failed.total")
                .description("Total number of job failures (including retries)")
                .register(registry);

        this.jobsReclaimedCounter = Counter.builder("jobs.reclaimed.total")
                .description("Total number of jobs reclaimed by the reaper from stale workers")
                .register(registry);

        registry.gauge("workers.active", redisTemplate, template -> {
            Set<String> keys = template.keys("worker:heartbeat:*");
            return keys == null ? 0 : keys.size();
        });

        registry.gauge("jobs.dead.current", jobRepository, repo -> repo.countByStatus("DEAD"));
    }

    public void incrementCompleted() {
        jobsCompletedCounter.increment();
    }

    public void incrementFailed() {
        jobsFailedCounter.increment();
    }

    public void incrementReclaimed(int count) {
        jobsReclaimedCounter.increment(count);
    }
}