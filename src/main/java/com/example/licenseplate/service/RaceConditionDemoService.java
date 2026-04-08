package com.example.licenseplate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class RaceConditionDemoService {

    private int unsafeCounter = 0;
    private final AtomicInteger safeCounter = new AtomicInteger(0);
    private int synchronizedCounter = 0;

    public void demonstrateRaceCondition() throws InterruptedException {
        log.info("=== RACE CONDITION DEMONSTRATION ===");

        int threadCount = 50;
        int incrementsPerThread = 1000;
        int expectedTotal = threadCount * incrementsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        log.info("Starting UNSAFE counter with {} threads...", threadCount);
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    unsafeCounter++;
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        log.info("=== RESULTS ===");
        log.info("Expected value: {}", expectedTotal);
        log.info("Actual unsafe counter value: {} (LOST UPDATES!)", unsafeCounter);
        log.info("Lost updates count: {}", expectedTotal - unsafeCounter);

        unsafeCounter = 0;
    }

    public void solveWithAtomic() throws InterruptedException {
        log.info("=== SOLVING WITH ATOMIC INTEGER ===");

        int threadCount = 50;
        int incrementsPerThread = 1000;
        int expectedTotal = threadCount * incrementsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    safeCounter.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        long endTime = System.currentTimeMillis();

        log.info("Expected value: {}", expectedTotal);
        log.info("Actual safe counter value: {}", safeCounter.get());
        log.info("Is correct: {}", safeCounter.get() == expectedTotal);
        log.info("Time taken: {} ms", endTime - startTime);
    }

    public void solveWithSynchronized() throws InterruptedException {
        log.info("=== SOLVING WITH SYNCHRONIZED ===");

        int threadCount = 50;
        int incrementsPerThread = 1000;
        int expectedTotal = threadCount * incrementsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    synchronized (this) {
                        synchronizedCounter++;
                    }
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        long endTime = System.currentTimeMillis();

        log.info("Expected value: {}", expectedTotal);
        log.info("Actual synchronized counter value: {}", synchronizedCounter);
        log.info("Is correct: {}", synchronizedCounter == expectedTotal);
        log.info("Time taken: {} ms", endTime - startTime);
    }

    public void reset() {
        unsafeCounter = 0;
        safeCounter.set(0);
        synchronizedCounter = 0;
    }
}