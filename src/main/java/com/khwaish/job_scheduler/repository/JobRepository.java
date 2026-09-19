package com.khwaish.job_scheduler.repository;

import com.khwaish.job_scheduler.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {

    Optional<Job> findByIdempotencyKey(String idempotencyKey);

    @Query(value = """
        SELECT id FROM jobs
        WHERE status = 'PENDING'
        AND scheduled_at <= :now
        ORDER BY priority DESC, scheduled_at ASC
        LIMIT 1
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    Optional<Long> findNextClaimableJobId(@Param("now") LocalDateTime now);

    @Modifying
    @Query("""
        UPDATE Job j
        SET j.status = 'RUNNING',
            j.lockedBy = :workerId,
            j.lastHeartbeat = CURRENT_TIMESTAMP,
            j.attempts = j.attempts + 1,
            j.updatedAt = CURRENT_TIMESTAMP
        WHERE j.id = :jobId
        """)
    void markJobAsRunning(@Param("jobId") Long jobId, @Param("workerId") String workerId);
}