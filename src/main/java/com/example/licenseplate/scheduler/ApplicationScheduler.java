package com.example.licenseplate.scheduler;

import com.example.licenseplate.model.entity.Application;
import com.example.licenseplate.model.enums.ApplicationStatus;
import com.example.licenseplate.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationScheduler {

    private final ApplicationRepository applicationRepository;

    @Scheduled(fixedDelay = 604800000)
    @Transactional
    public void releaseExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<Application> expiredApplications = applicationRepository
            .findExpiredByStatus(ApplicationStatus.PENDING, now);

        for (Application application : expiredApplications) {
            application.setStatus(ApplicationStatus.EXPIRED);
        }

        if (!expiredApplications.isEmpty()) {
            applicationRepository.saveAll(expiredApplications);
            log.info("Found {} expired PENDING applications", expiredApplications.size());
        }
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanUpOldCancelledApplications() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(6);
        List<Application> oldCancelled = applicationRepository
            .findByStatus(ApplicationStatus.CANCELLED)
            .stream()
            .filter(app -> app.getUpdatedAt().isBefore(cutoffDate))
            .toList();

        if (!oldCancelled.isEmpty()) {
            applicationRepository.deleteAll(oldCancelled);
            log.info("Cleaned up {} old cancelled applications", oldCancelled.size());
        }
    }
}