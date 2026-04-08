package com.example.licenseplate.controller;

import com.example.licenseplate.service.RaceConditionDemoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/demo/race-condition")
@RequiredArgsConstructor
@Tag(name = "Race Condition Demo", description = "Демонстрация race condition и его решений")
public class RaceConditionController {

    private final RaceConditionDemoService raceConditionDemoService;

    @Operation(summary = "Демонстрация race condition (50+ потоков)")
    @PostMapping("/demonstrate")
    public ResponseEntity<String> demonstrateRaceCondition() {
        try {
            raceConditionDemoService.demonstrateRaceCondition();
            return ResponseEntity.ok("Race condition demonstrated. Check logs!");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.internalServerError().body("Demo failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Решение race condition с AtomicInteger")
    @PostMapping("/solve-atomic")
    public ResponseEntity<String> solveWithAtomic() {
        try {
            raceConditionDemoService.solveWithAtomic();
            return ResponseEntity.ok("Atomic solution demonstrated. Check logs!");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.internalServerError().body("Demo failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Решение race condition с synchronized")
    @PostMapping("/solve-synchronized")
    public ResponseEntity<String> solveWithSynchronized() {
        try {
            raceConditionDemoService.solveWithSynchronized();
            return ResponseEntity.ok("Synchronized solution demonstrated. Check logs!");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.internalServerError().body("Demo failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Сбросить все счётчики")
    @PostMapping("/reset")
    public ResponseEntity<String> resetCounters() {
        raceConditionDemoService.reset();
        return ResponseEntity.ok("All counters reset");
    }
}