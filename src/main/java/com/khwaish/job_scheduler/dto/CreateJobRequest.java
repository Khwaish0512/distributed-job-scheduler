package com.khwaish.job_scheduler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CreateJobRequest {

    @NotBlank(message = "idempotencyKey is required")
    private String idempotencyKey;

    @NotBlank(message = "taskType is required")
    private String taskType;

    @NotNull(message = "payload is required")
    private String payload;

    private Integer priority = 0;

    @NotNull(message = "scheduledAt is required")
    private LocalDateTime scheduledAt;
}