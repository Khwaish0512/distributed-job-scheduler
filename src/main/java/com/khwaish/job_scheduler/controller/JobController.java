package com.khwaish.job_scheduler.controller;

import com.khwaish.job_scheduler.dto.CreateJobRequest;
import com.khwaish.job_scheduler.model.Job;
import com.khwaish.job_scheduler.repository.JobRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobRepository jobRepository;

    public JobController(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @PostMapping
    public ResponseEntity<Job> createJob(@Valid @RequestBody CreateJobRequest request) {

        // Idempotency check: has this exact request already been submitted?
        Optional<Job> existing = jobRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            return ResponseEntity.status(HttpStatus.OK).body(existing.get());
        }

        Job job = new Job();
        job.setIdempotencyKey(request.getIdempotencyKey());
        job.setTaskType(request.getTaskType());
        job.setPayload(request.getPayload());
        job.setPriority(request.getPriority());
        job.setScheduledAt(request.getScheduledAt());
        job.setStatus("PENDING");
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());

        Job saved = jobRepository.save(job);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJob(@PathVariable Long id) {
        return jobRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}