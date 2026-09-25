package com.khwaish.job_scheduler.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "jobs")
@Data
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String idempotencyKey;

    @Column(nullable = false)
    private String taskType;

    @Column(columnDefinition = "JSON", nullable = false)
    private String payload;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(nullable = false)
    private Integer priority = 0;

    @Column(nullable = false)
    private Integer attempts = 0;

    @Column(nullable = false)
    private Integer maxAttempts = 3;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    private String lockedBy;

    private LocalDateTime lastHeartbeat;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String errorMessage;
}
