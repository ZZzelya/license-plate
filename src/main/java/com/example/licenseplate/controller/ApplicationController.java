package com.example.licenseplate.controller;

import com.example.licenseplate.dto.request.ApplicationActionRequest;
import com.example.licenseplate.dto.request.ApplicationCreateDto;
import com.example.licenseplate.dto.response.ApplicationDto;
import com.example.licenseplate.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    public ResponseEntity<ApplicationDto> createApplication(
        @Valid @RequestBody final ApplicationCreateDto createDto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationService.createApplication(createDto));
    }

    @GetMapping
    public ResponseEntity<List<ApplicationDto>> getAllApplications() {
        return ResponseEntity.ok(applicationService.getAllApplications());
    }

    @GetMapping("/{id}/with-details")
    public ResponseEntity<ApplicationDto> getApplicationWithDetails(@PathVariable final Long id) {
        return ResponseEntity.ok(applicationService.getApplicationWithDetails(id));
    }

    @GetMapping("/by-passport")
    public ResponseEntity<List<ApplicationDto>> getApplicationsByPassport(
        @RequestParam final String passportNumber
    ) {
        return ResponseEntity.ok(applicationService.getApplicationsByPassport(passportNumber));
    }

    @PatchMapping("/{id}/confirm")
    public ResponseEntity<ApplicationDto> confirmApplication(
        @PathVariable final Long id,
        @RequestBody(required = false) final ApplicationActionRequest request
    ) {
        return ResponseEntity.ok(applicationService.confirmApplication(id, request != null ? request.getComment() : null));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ApplicationDto> completeApplication(
        @PathVariable final Long id,
        @RequestBody(required = false) final ApplicationActionRequest request
    ) {
        return ResponseEntity.ok(applicationService.completeApplication(id, request != null ? request.getComment() : null));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApplicationDto> cancelApplication(
        @PathVariable final Long id,
        @RequestBody(required = false) final ApplicationActionRequest request
    ) {
        return ResponseEntity.ok(applicationService.cancelApplication(id, request != null ? request.getComment() : null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable final Long id) {
        applicationService.deleteApplication(id);
        return ResponseEntity.noContent().build();
    }
}
