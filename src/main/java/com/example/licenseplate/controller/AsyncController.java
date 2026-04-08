package com.example.licenseplate.controller;

import com.example.licenseplate.dto.response.AsyncTaskResponse;
import com.example.licenseplate.dto.response.TaskStatusResponse;
import com.example.licenseplate.service.AsyncExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/async")
@RequiredArgsConstructor
@Tag(name = "Асинхронные операции", description = "API для асинхронной обработки данных")
public class AsyncController {

    private final AsyncExportService asyncExportService;

    @Operation(summary = "Асинхронная бизнес-операция для региона")
    @PostMapping("/export-demo/{region}")
    public ResponseEntity<AsyncTaskResponse> startExportDemo(@PathVariable String region) {
        String taskId = UUID.randomUUID().toString();
        log.info("Starting async export task: {} for region: {}", taskId, region);

        CompletableFuture<TaskStatusResponse> future = asyncExportService.exportApplicationsDemoAsync(region, taskId);

        future.exceptionally(ex -> {
            log.error("task {} failed", taskId, ex);
            return null;
        });

        return ResponseEntity.ok(AsyncTaskResponse.builder()
            .taskId(taskId)
            .status("SUBMITTED")
            .message("Exporting 10000 applications for region: " + region + " (takes ~10 seconds)")
            .submittedAt(LocalDateTime.now())
            .build());
    }

    @Operation(summary = "Асинхронная бизнес-операция для региона (JMeter)")
    @PostMapping("/export-load/{region}")
    public ResponseEntity<AsyncTaskResponse> startExportLoad(@PathVariable String region) {
        String taskId = UUID.randomUUID().toString();
        log.info("Starting LOAD async export task: {} for region: {}", taskId, region);

        CompletableFuture<TaskStatusResponse> future = asyncExportService.exportApplicationsLoadAsync(region, taskId);

        future.exceptionally(ex -> {
            log.error("LOAD async task {} failed", taskId, ex);
            return null;
        });

        return ResponseEntity.ok(AsyncTaskResponse.builder()
            .taskId(taskId)
            .status("SUBMITTED")
            .message("LOAD TEST: Fast export for region: " + region + " (instant, for JMeter)")
            .submittedAt(LocalDateTime.now())
            .build());
    }

    @Operation(summary = "Проверить статус задачи по ID")
    @GetMapping("/status/{taskId}")
    public ResponseEntity<TaskStatusResponse> getTaskStatus(@PathVariable String taskId) {
        TaskStatusResponse status = asyncExportService.getTaskStatus(taskId);
        return ResponseEntity.ok(status);
    }

    @Operation(summary = "Получить все задачи")
    @GetMapping("/tasks")
    public ResponseEntity<Map<String, TaskStatusResponse>> getAllTasks() {
        return ResponseEntity.ok(asyncExportService.getAllTasks());
    }
}