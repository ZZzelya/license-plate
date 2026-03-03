package com.example.licenseplate.controller;

import com.example.licenseplate.dto.request.ServiceCreateDto;
import com.example.licenseplate.dto.response.ServiceDto;
import com.example.licenseplate.service.ServiceManagementService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceManagementController {

    private final ServiceManagementService serviceManagementService;

    @PostMapping
    public ResponseEntity<ServiceDto> createService(
        @Valid @RequestBody final ServiceCreateDto createDto) {
        ServiceDto created = serviceManagementService.createService(createDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<ServiceDto>> getAllServices() {
        return ResponseEntity.ok(serviceManagementService.getAllServices());
    }

    @GetMapping("/available")
    public ResponseEntity<List<ServiceDto>> getAvailableServices() {
        return ResponseEntity.ok(serviceManagementService.getAvailableServices());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceDto> getServiceById(@PathVariable final Long id) {
        return ResponseEntity.ok(serviceManagementService.getServiceById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceDto> updateService(
        @PathVariable final Long id,
        @Valid @RequestBody final ServiceCreateDto updateDto) {
        return ResponseEntity.ok(serviceManagementService.updateService(id, updateDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteService(@PathVariable final Long id) {
        serviceManagementService.deleteService(id);
        return ResponseEntity.noContent().build();
    }
}