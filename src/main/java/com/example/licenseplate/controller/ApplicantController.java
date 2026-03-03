package com.example.licenseplate.controller;

import com.example.licenseplate.dto.request.ApplicantCreateDto;
import com.example.licenseplate.dto.response.ApplicantDto;
import com.example.licenseplate.service.ApplicantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/applicants")
@RequiredArgsConstructor
public class ApplicantController {

    private final ApplicantService applicantService;

    @PostMapping
    public ResponseEntity<ApplicantDto> createApplicant(
        @Valid @RequestBody final ApplicantCreateDto createDto) {
        ApplicantDto created = applicantService.createApplicant(createDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<ApplicantDto>> getAllApplicants() {
        return ResponseEntity.ok(applicantService.getAllApplicants());
    }

    @GetMapping("/active")
    public ResponseEntity<List<ApplicantDto>> getActiveApplicants() {
        return ResponseEntity.ok(applicantService.getActiveApplicants());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicantDto> getApplicantById(@PathVariable final Long id) {
        return ResponseEntity.ok(applicantService.getApplicantById(id));
    }

    @GetMapping("/by-passport")
    public ResponseEntity<ApplicantDto> getApplicantByPassport(
        @RequestParam final String passportNumber) {
        return ResponseEntity.ok(applicantService.getApplicantByPassport(passportNumber));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApplicantDto> updateApplicant(
        @PathVariable final Long id,
        @Valid @RequestBody final ApplicantCreateDto updateDto) {
        return ResponseEntity.ok(applicantService.updateApplicant(id, updateDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplicant(@PathVariable final Long id) {
        applicantService.deleteApplicant(id);
        return ResponseEntity.noContent().build();
    }
}