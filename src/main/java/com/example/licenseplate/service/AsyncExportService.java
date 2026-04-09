package com.example.licenseplate.service;

import com.example.licenseplate.dto.response.TaskStatusResponse;
import com.example.licenseplate.model.entity.Application;
import com.example.licenseplate.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
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
    private final ApplicationRepository applicationRepository;

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

        TaskStatusResponse response = TaskStatusResponse.builder()
            .taskId(taskId)
            .status("PROCESSING")
            .region(region)
            .startTime(LocalDateTime.now())
            .totalCount(0)
            .processedCount(0)
            .progressPercent(0)
            .build();
        taskStatusMap.put(taskId, response);

        try {

            log.info("TASK #{}: Fetching applications from database for region: {}", taskNum, region);

            List<Application> applications = applicationRepository.findByStatusAndDepartmentRegion(
                com.example.licenseplate.model.enums.ApplicationStatus.COMPLETED, region
            );

            int totalRecords = applications.size();
            response.setTotalCount(totalRecords);
            taskStatusMap.put(taskId, response);

            log.info("TASK #{}: Found {} applications in DB", taskNum, totalRecords);


            for (int i = 0; i < applications.size(); i++) {
                Application app = applications.get(i);

                totalRecordsProcessed.incrementAndGet();

                if ((i + 1) % 500 == 0 || (i + 1) == totalRecords) {
                    response.setProcessedCount(i + 1);
                    response.setProgressPercent(((i + 1) * 100) / totalRecords);
                    taskStatusMap.put(taskId, response);

                    log.info("TASK #{}: progress {}/{} ({}%)",
                        taskNum, i + 1, totalRecords, ((i + 1) * 100 / totalRecords));
                }
            }

            Thread.sleep(16000);

            totalTasksCompleted.incrementAndGet();
            activeTasks.decrementAndGet();

            response.setStatus("COMPLETED");
            response.setEndTime(LocalDateTime.now());
            response.setProcessedCount(totalRecords);
            response.setTotalCount(totalRecords);
            response.setProgressPercent(100);
            response.setResult(String.format("Successfully processed %d applications from region: %s",
                totalRecords, region));

            taskStatusMap.put(taskId, response);

            log.info("=== TASK #{} COMPLETED ===", taskNum);
            return CompletableFuture.completedFuture(response);

        } catch (Exception e) {
            log.error("TASK #{} FAILED: {}", taskNum, e.getMessage(), e);
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

        log.info("=== TASK #{} STARTED ===", taskNum);
        log.info("Region: {}, taskId: {}", region, taskId);

        TaskStatusResponse response = TaskStatusResponse.builder()
            .taskId(taskId)
            .status("PROCESSING")
            .region(region)
            .startTime(LocalDateTime.now())
            .totalCount(0)
            .processedCount(0)
            .progressPercent(0)
            .build();
        taskStatusMap.put(taskId, response);

        try {

            log.info("TASK #{}: Fetching applications from database for region: {}", taskNum, region);

            List<Application> applications = applicationRepository.findByStatusAndDepartmentRegion(
                com.example.licenseplate.model.enums.ApplicationStatus.COMPLETED, region
            );

            int totalRecords = applications.size();
            response.setTotalCount(totalRecords);
            taskStatusMap.put(taskId, response);

            log.info("TASK #{}: Found {} applications in DB", taskNum, totalRecords);


            for (int i = 0; i < applications.size(); i++) {
                Application app = applications.get(i);

                totalRecordsProcessed.incrementAndGet();

                if ((i + 1) % 500 == 0 || (i + 1) == totalRecords) {
                    response.setProcessedCount(i + 1);
                    response.setProgressPercent(((i + 1) * 100) / totalRecords);
                    taskStatusMap.put(taskId, response);

                    log.info("TASK #{}: progress {}/{} ({}%)",
                        taskNum, i + 1, totalRecords, ((i + 1) * 100 / totalRecords));
                }
            }

            totalTasksCompleted.incrementAndGet();
            activeTasks.decrementAndGet();

            response.setStatus("COMPLETED");
            response.setEndTime(LocalDateTime.now());
            response.setProcessedCount(totalRecords);
            response.setTotalCount(totalRecords);
            response.setProgressPercent(100);
            response.setResult(String.format("Successfully processed %d applications from region: %s",
                totalRecords, region));

            taskStatusMap.put(taskId, response);

            log.info("=== TASK #{} COMPLETED ===", taskNum);
            return CompletableFuture.completedFuture(response);

        } catch (Exception e) {
            log.error("TASK #{} FAILED: {}", taskNum, e.getMessage(), e);
            activeTasks.decrementAndGet();

            response.setStatus("FAILED");
            response.setEndTime(LocalDateTime.now());
            response.setError(e.getMessage());
            taskStatusMap.put(taskId, response);
            return CompletableFuture.failedFuture(e);
        }
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
