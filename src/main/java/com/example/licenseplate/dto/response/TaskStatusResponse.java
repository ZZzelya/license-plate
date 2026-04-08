package com.example.licenseplate.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatusResponse {
    private String taskId;
    private String status;
    private String result;
    private String error;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer processedCount;
    private Integer totalCount;
    private String region;
    private Integer progressPercent;
}