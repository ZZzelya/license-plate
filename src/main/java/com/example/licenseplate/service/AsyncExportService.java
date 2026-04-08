package com.example.licenseplate.service;

import com.example.licenseplate.dto.response.TaskStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncExportService {

    private final Map<String, TaskStatusResponse> taskStatusMap = new ConcurrentHashMap<>();

    private final AtomicInteger totalTasksSubmitted = new AtomicInteger(0);
    private final AtomicInteger totalTasksCompleted = new AtomicInteger(0);
    private final AtomicLong totalRecordsProcessed = new AtomicLong(0);
    private final AtomicInteger activeTasks = new AtomicInteger(0);

    @Async("taskExecutor")
    public CompletableFuture<TaskStatusResponse> exportApplicationsDemoAsync(String region, String taskId) {

        int taskNum = totalTasksSubmitted.incrementAndGet();
        activeTasks.incrementAndGet();

        log.info("=== TASK #{} STARTED ===", taskNum);
        log.info("Region: {}, taskId: {}", region, taskId);
        log.info("Stats: submitted={}, active={}, totalRecordsProcessed={}",
            totalTasksSubmitted.get(), activeTasks.get(), totalRecordsProcessed.get());

        TaskStatusResponse response = TaskStatusResponse.builder()
            .taskId(taskId)
            .status("PROCESSING")
            .region(region)
            .startTime(LocalDateTime.now())
            .totalCount(10000)
            .processedCount(0)
            .progressPercent(0)
            .build();
        taskStatusMap.put(taskId, response);

        try {
            int totalRecords = 10000;
            long startTime = System.currentTimeMillis();
            long targetDuration = 10000;
            long taskRecordsProcessed = 0;

            for (int i = 1; i <= totalRecords; i++) {

                if (i % 100 == 0) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    long expectedElapsed = (targetDuration * i) / totalRecords;
                    if (elapsed < expectedElapsed) {
                        Thread.sleep(expectedElapsed - elapsed);
                    }
                }

                taskRecordsProcessed++;
                totalRecordsProcessed.incrementAndGet();

                if (i % 500 == 0 || i == totalRecords) {
                    response.setProcessedCount(i);
                    response.setProgressPercent((i * 100) / totalRecords);
                    taskStatusMap.put(taskId, response);

                    log.info("TASK #{}: progress {}/{} ({}%), totalRecordsProcessed: {}",
                        taskNum, i, totalRecords, (i * 100 / totalRecords), totalRecordsProcessed.get());
                }
            }

            long totalTime = System.currentTimeMillis() - startTime;

            totalTasksCompleted.incrementAndGet();
            activeTasks.decrementAndGet();

            response.setStatus("COMPLETED");
            response.setEndTime(LocalDateTime.now());
            response.setProcessedCount(totalRecords);
            response.setTotalCount(totalRecords);
            response.setProgressPercent(100);
            response.setResult(String.format("Successfully exported %d applications " +
                    "from region: %s in %d ms. Task #%d processed %d records.",
                totalRecords, region, totalTime, taskNum, taskRecordsProcessed));

            taskStatusMap.put(taskId, response);

            log.info("=== TASK #{} COMPLETED ===", taskNum);
            log.info("Execution time: {} ms", totalTime);
            log.info("Final stats: submitted={}, completed={}, active={}, totalRecordsProcessed={}",
                totalTasksSubmitted.get(), totalTasksCompleted.get(), activeTasks.get(), totalRecordsProcessed.get());

            return CompletableFuture.completedFuture(response);

        } catch (Exception e) {
            log.error("TASK #{} FAILED: {}", taskNum, e.getMessage());
            activeTasks.decrementAndGet();

            response.setStatus("FAILED");
            response.setEndTime(LocalDateTime.now());
            response.setError(e.getMessage());
            taskStatusMap.put(taskId, response);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("taskExecutor")
    public CompletableFuture<TaskStatusResponse> exportApplicationsLoadAsync(String region, String taskId) {

        int taskNum = totalTasksSubmitted.incrementAndGet();
        activeTasks.incrementAndGet();

        log.info("LOAD TASK #{}: region={}, taskId={}", taskNum, region, taskId);
        log.info("Current stats: submitted={}, active={}", totalTasksSubmitted.get(), activeTasks.get());

        totalRecordsProcessed.addAndGet(10000);

        totalTasksCompleted.incrementAndGet();
        activeTasks.decrementAndGet();

        TaskStatusResponse response = TaskStatusResponse.builder()
            .taskId(taskId)
            .status("COMPLETED")
            .region(region)
            .startTime(LocalDateTime.now())
            .endTime(LocalDateTime.now())
            .totalCount(10000)
            .processedCount(10000)
            .progressPercent(100)
            .result(String.format("LOAD TEST: Successfully exported 10000 applications from region: %s. " +
                "Task #%d", region, taskNum))
            .build();

        taskStatusMap.put(taskId, response);

        log.info("LOAD TASK #{} completed. Stats: completed={}, totalRecordsProcessed={}",
            taskNum, totalTasksCompleted.get(), totalRecordsProcessed.get());

        return CompletableFuture.completedFuture(response);
    }

    public TaskStatusResponse getTaskStatus(String taskId) {
        TaskStatusResponse response = taskStatusMap.get(taskId);
        if (response == null) {
            return TaskStatusResponse.builder()
                .taskId(taskId)
                .status("NOT_FOUND")
                .progressPercent(0)
                .build();
        }
        return response;
    }

    public Map<String, TaskStatusResponse> getAllTasks() {
        return Map.copyOf(taskStatusMap);
    }
}