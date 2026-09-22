package com.khwaish.job_scheduler;

import com.khwaish.job_scheduler.model.Job;
import com.khwaish.job_scheduler.repository.JobRepository;
import com.khwaish.job_scheduler.worker.JobClaimService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = "scheduler.polling.enabled=false")
public class JobWorkerConcurrencyTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobClaimService jobClaimService;

    @BeforeEach
    void cleanUp() {
        jobRepository.deleteAll();
    }

    @Test
    void onlyOneThreadShouldSuccessfullyClaimTheJob() throws InterruptedException {

        Job job = new Job();
        job.setIdempotencyKey("concurrency-test-" + System.currentTimeMillis());
        job.setTaskType("TEST_TASK");
        job.setPayload("{}");
        job.setStatus("PENDING");
        job.setScheduledAt(LocalDateTime.now().minusMinutes(1));
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        Job savedJob = jobRepository.save(job);

        int numberOfThreads = 10;
        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            new Thread(() -> {
                Long claimedJobId = jobClaimService.claimNextJob("test-worker");
                if (savedJob.getId().equals(claimedJobId)) {
                    successCount.incrementAndGet();
                }
                latch.countDown();
            }).start();
        }

        latch.await();

        assertEquals(1, successCount.get(), "Expected exactly one thread to claim the job, but got: " + successCount.get());
    }
}