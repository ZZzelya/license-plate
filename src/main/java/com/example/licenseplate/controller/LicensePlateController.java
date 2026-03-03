package com.example.licenseplate.controller;

import com.example.licenseplate.dto.request.LicensePlateCreateDto;
import com.example.licenseplate.dto.response.LicensePlateDto;
import com.example.licenseplate.service.LicensePlateService;
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
@RequestMapping("/api/license-plates")
@RequiredArgsConstructor
public class LicensePlateController {

    private final LicensePlateService licensePlateService;

    @PostMapping
    public ResponseEntity<LicensePlateDto> createLicensePlate(
        @Valid @RequestBody final LicensePlateCreateDto createDto) {
        LicensePlateDto created = licensePlateService.createLicensePlate(createDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<LicensePlateDto>> getAllLicensePlates() {
        return ResponseEntity.ok(licensePlateService.getAllLicensePlates());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LicensePlateDto> getLicensePlateById(@PathVariable final Long id) {
        return ResponseEntity.ok(licensePlateService.getLicensePlateById(id));
    }

    @GetMapping("/by-number")
    public ResponseEntity<LicensePlateDto> getLicensePlateByNumber(
        @RequestParam final String plateNumber) {
        return ResponseEntity.ok(licensePlateService.getLicensePlateByNumber(plateNumber));
    }

    @GetMapping("/available")
    public ResponseEntity<List<LicensePlateDto>> getAvailablePlatesByRegion(
        @RequestParam final String region) {
        return ResponseEntity.ok(licensePlateService.getAvailablePlatesByRegion(region));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LicensePlateDto> updateLicensePlate(
        @PathVariable final Long id,
        @Valid @RequestBody final LicensePlateCreateDto updateDto) {
        return ResponseEntity.ok(licensePlateService.updateLicensePlate(id, updateDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLicensePlate(@PathVariable final Long id) {
        licensePlateService.deleteLicensePlate(id);
        return ResponseEntity.noContent().build();
    }
}