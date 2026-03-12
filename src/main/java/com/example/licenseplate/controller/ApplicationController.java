package com.example.licenseplate.controller;

import com.example.licenseplate.dto.request.ApplicationCreateDto;
import com.example.licenseplate.dto.response.ApplicationDto;
import com.example.licenseplate.model.enums.ApplicationStatus;
import com.example.licenseplate.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    public ResponseEntity<ApplicationDto> createApplication(
        @Valid @RequestBody final ApplicationCreateDto createDto) {
        ApplicationDto created = applicationService.createApplication(createDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/demo/without-tx")
    public ResponseEntity<ApplicationDto> createApplicationWithoutTransaction(
        @Valid @RequestBody final ApplicationCreateDto createDto) {
        ApplicationDto created = applicationService.createApplicationWithoutTransaction(
            createDto);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/demo/with-tx")
    public ResponseEntity<ApplicationDto> createApplicationWithTransaction(
        @Valid @RequestBody final ApplicationCreateDto createDto) {
        ApplicationDto created = applicationService.createApplicationWithTransaction(
            createDto);
        return ResponseEntity.ok(created);
    }

    @GetMapping
    public ResponseEntity<List<ApplicationDto>> getAllApplications() {
        return ResponseEntity.ok(applicationService.getAllApplications());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationDto> getApplicationById(@PathVariable final Long id) {
        return ResponseEntity.ok(applicationService.getApplicationById(id));
    }

    @GetMapping("/{id}/with-details")
    public ResponseEntity<ApplicationDto> getApplicationWithDetails(
        @PathVariable final Long id) {
        return ResponseEntity.ok(applicationService.getApplicationWithDetails(id));
    }

    @GetMapping("/by-passport")
    public ResponseEntity<List<ApplicationDto>> getApplicationsByPassport(
        @RequestParam final String passportNumber) {
        return ResponseEntity.ok(
            applicationService.getApplicationsByPassport(passportNumber));
    }

    @PatchMapping("/{id}/confirm")
    public ResponseEntity<ApplicationDto> confirmApplication(@PathVariable final Long id) {
        return ResponseEntity.ok(applicationService.confirmApplication(id));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ApplicationDto> completeApplication(@PathVariable final Long id) {
        return ResponseEntity.ok(applicationService.completeApplication(id));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApplicationDto> cancelApplication(@PathVariable final Long id) {
        return ResponseEntity.ok(applicationService.cancelApplication(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable final Long id) {
        applicationService.deleteApplication(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/filter")
    public ResponseEntity<List<ApplicationDto>> getApplicationsByStatusAndRegion(
        @RequestParam ApplicationStatus status,
        @RequestParam String region) {
        return ResponseEntity.ok(
            applicationService.getApplicationsByStatusAndRegion(status, region));
    }

    @GetMapping("/filter/cached")
    public ResponseEntity<List<ApplicationDto>> getApplicationsByStatusAndRegionCached(
        @RequestParam ApplicationStatus status,
        @RequestParam String region) {
        return ResponseEntity.ok(
            applicationService.getApplicationsByStatusAndRegionCached(status, region));
    }

    @GetMapping("/filter/native")
    public ResponseEntity<List<ApplicationDto>> getApplicationsByStatusAndRegionNative(
        @RequestParam ApplicationStatus status,
        @RequestParam String region) {
        return ResponseEntity.ok(
            applicationService.getApplicationsByStatusAndRegionNative(status, region));
    }

    @DeleteMapping("/cache")
    public ResponseEntity<Void> invalidateCache() {
        applicationService.invalidateCache();
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/cache/region/{region}")
    public ResponseEntity<Void> invalidateCacheByRegion(@PathVariable String region) {
        applicationService.invalidateCacheByRegion(region);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/cache/status/{status}")
    public ResponseEntity<Void> invalidateCacheByStatus(@PathVariable String status) {
        applicationService.invalidateCacheByStatus(status);
        return ResponseEntity.ok().build();
    }

    // ==================== ИСПРАВЛЕННАЯ ПАГИНАЦИЯ ====================

    /**
     * ПРОСТОЙ И РАБОЧИЙ ВАРИАНТ - используй этот!
     */
    @GetMapping("/by-passport/paginated")
    public ResponseEntity<Page<ApplicationDto>> getApplicationsByPassportPaginated(
        @RequestParam String passportNumber,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {

        log.info("Пагинация: passport={}, page={}, size={}", passportNumber, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("submissionDate").descending());

        return ResponseEntity.ok(
            applicationService.getApplicationsByPassportPaginated(passportNumber, pageable));
    }

    /**
     * С СОРТИРОВКОЙ ПО ОДНОМУ ПОЛЮ
     */
    @GetMapping("/by-passport/paginated/sorted")
    public ResponseEntity<Page<ApplicationDto>> getApplicationsByPassportPaginatedSorted(
        @RequestParam String passportNumber,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "submissionDate") String sortBy,
        @RequestParam(defaultValue = "desc") String direction) {

        log.info("Пагинация с сортировкой: passport={}, page={}, size={}, sortBy={}, direction={}",
            passportNumber, page, size, sortBy, direction);

        Sort sort = direction.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(
            applicationService.getApplicationsByPassportPaginated(passportNumber, pageable));
    }
}