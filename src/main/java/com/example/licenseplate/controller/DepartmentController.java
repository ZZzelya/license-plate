package com.example.licenseplate.controller;

import com.example.licenseplate.dto.request.DepartmentCreateDto;
import com.example.licenseplate.dto.response.DepartmentDto;
import com.example.licenseplate.dto.DepartmentWithPlatesDto;
import com.example.licenseplate.service.DepartmentService;
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
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    public ResponseEntity<DepartmentDto> createDepartment(
        @Valid @RequestBody final DepartmentCreateDto createDto) {
        DepartmentDto created = departmentService.createDepartment(createDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<DepartmentDto>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartmentDto> getDepartmentById(@PathVariable final Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    @GetMapping("/{id}/with-plates")
    public ResponseEntity<DepartmentWithPlatesDto> getDepartmentWithPlates(
        @PathVariable final Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentWithPlates(id));
    }

    @GetMapping("/by-region")
    public ResponseEntity<List<DepartmentDto>> getDepartmentsByRegion(
        @RequestParam final String region) {
        return ResponseEntity.ok(departmentService.getDepartmentsByRegion(region));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepartmentDto> updateDepartment(
        @PathVariable final Long id,
        @Valid @RequestBody final DepartmentCreateDto updateDto) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, updateDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable final Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/demo/nplus1")
    public ResponseEntity<String> demonstrateNPlusOne(@RequestParam final String region) {
        departmentService.demonstrateNPlusOneProblem(region);
        return ResponseEntity.ok("N+1 problem demonstrated. Check logs!");
    }

    @GetMapping("/demo/solved")
    public ResponseEntity<String> solveNPlusOne(@RequestParam final String region) {
        departmentService.solveNPlusOneWithFetchJoin(region);
        return ResponseEntity.ok("N+1 solved with fetch join. Check logs!");
    }
}