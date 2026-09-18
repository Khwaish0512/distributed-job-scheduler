package com.khwaish.job_scheduler.repository;

import com.khwaish.job_scheduler.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {
    Optional<Job> findByIdempotencyKey(String idempotencyKey);
}