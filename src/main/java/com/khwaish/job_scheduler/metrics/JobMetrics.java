package com.khwaish.job_scheduler.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.Set;

@Component
public class JobMetrics {

    private final Counter jobsCompletedCounter;
    private final Counter jobsFailedCounter;
    private final Counter jobsDeadCounter;
    private final Counter jobsReclaimedCounter;

    public JobMetrics(MeterRegistry registry, StringRedisTemplate redisTemplate) {
        this.jobsCompletedCounter = Counter.builder("jobs.completed.total")
                .description("Total number of jobs successfully completed")
                .register(registry);

        this.jobsFailedCounter = Counter.builder("jobs.failed.total")
                .description("Total number of job failures (including retries)")
                .register(registry);

        this.jobsDeadCounter = Counter.builder("jobs.dead.total")
                .description("Total number of jobs that exhausted all retries and moved to DEAD")
                .register(registry);

        this.jobsReclaimedCounter = Counter.builder("jobs.reclaimed.total")
                .description("Total number of jobs reclaimed by the reaper from stale workers")
                .register(registry);

        registry.gauge("workers.active", redisTemplate, template -> {
            Set<String> keys = template.keys("worker:heartbeat:*");
            return keys == null ? 0 : keys.size();
        });
    }

    public void incrementCompleted() {
        jobsCompletedCounter.increment();
    }

    public void incrementFailed() {
        jobsFailedCounter.increment();
    }

    public void incrementDead() {
        jobsDeadCounter.increment();
    }

    public void incrementReclaimed(int count) {
        jobsReclaimedCounter.increment(count);
    }
}