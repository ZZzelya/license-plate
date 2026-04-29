package com.example.licenseplate.controller;

import com.example.licenseplate.dto.request.LicensePlateCreateDto;
import com.example.licenseplate.dto.response.LicensePlateDto;
import com.example.licenseplate.service.LicensePlateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Номерные знаки", description = "API для управления номерными знаками")
public class LicensePlateController {

    private final LicensePlateService licensePlateService;

    @Operation(summary = "Создать номерной знак", description = "Регистрирует новый номерной знак в системе")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Номерной знак успешно создан",
            content = @Content(schema = @Schema(implementation = LicensePlateDto.class))
        ),
        @ApiResponse(responseCode = "400", description = "Неверные данные запроса", content = @Content),
        @ApiResponse(responseCode = "404", description = "Отделение не найдено", content = @Content),
        @ApiResponse(responseCode = "409", description = "Номерной знак уже существует", content = @Content)
    })
    @PostMapping
    public ResponseEntity<LicensePlateDto> createLicensePlate(
        @Parameter(description = "Данные для создания номерного знака", required = true)
        @Valid @RequestBody final LicensePlateCreateDto createDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(licensePlateService.createLicensePlate(createDto));
    }

    @Operation(summary = "Получить все номерные знаки", description = "Возвращает список всех номерных знаков")
    @GetMapping
    public ResponseEntity<List<LicensePlateDto>> getAllLicensePlates() {
        return ResponseEntity.ok(licensePlateService.getAllLicensePlates());
    }

    @Operation(summary = "Получить номерной знак по ID", description = "Возвращает информацию о " +
        "номерном знаке по идентификатору")
    @GetMapping("/{id}")
    public ResponseEntity<LicensePlateDto> getLicensePlateById(
        @Parameter(description = "ID номерного знака", required = true, example = "1")
        @PathVariable final Long id) {
        return ResponseEntity.ok(licensePlateService.getLicensePlateById(id));
    }

    @Operation(summary = "Получить номерной знак по номеру", description = "Возвращает информацию о " +
        "номерном знаке по полному номеру")
    @GetMapping("/by-number")
    public ResponseEntity<LicensePlateDto> getLicensePlateByNumber(
        @Parameter(description = "Номер знака в формате 1234 AB-7", required = true, example = "1234 AB-7")
        @RequestParam final String plateNumber) {
        return ResponseEntity.ok(licensePlateService.getLicensePlateByNumber(plateNumber));
    }

    @Operation(summary = "Получить доступные знаки по региону", description = "Возвращает список свободных " +
        "номерных знаков в указанном регионе")
    @GetMapping("/available")
    public ResponseEntity<List<LicensePlateDto>> getAvailablePlatesByRegion(
        @Parameter(description = "Регион", required = true, example = "Минская область")
        @RequestParam final String region) {
        return ResponseEntity.ok(licensePlateService.getAvailablePlatesByRegion(region));
    }

    @Operation(summary = "Получить доступные знаки по отделению", description = "Возвращает список " +
        "свободных номерных знаков в указанном отделении")
    @GetMapping("/available/by-department")
    public ResponseEntity<List<LicensePlateDto>> getAvailablePlatesByDepartment(
        @Parameter(description = "ID отделения", required = true, example = "1")
        @RequestParam final Long departmentId) {
        return ResponseEntity.ok(licensePlateService.getAvailablePlatesByDepartment(departmentId));
    }

    @Operation(summary = "Обновить номерной знак", description = "Обновляет информацию о существующем номерном знаке")
    @PutMapping("/{id}")
    public ResponseEntity<LicensePlateDto> updateLicensePlate(
        @Parameter(description = "ID номерного знака", required = true, example = "1")
        @PathVariable final Long id,
        @Parameter(description = "Обновлённые данные номерного знака", required = true)
        @Valid @RequestBody final LicensePlateCreateDto updateDto) {
        return ResponseEntity.ok(licensePlateService.updateLicensePlate(id, updateDto));
    }

    @Operation(summary = "Удалить номерной знак", description = "Удаляет номерной знак из системы")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLicensePlate(
        @Parameter(description = "ID номерного знака", required = true, example = "1")
        @PathVariable final Long id) {
        licensePlateService.deleteLicensePlate(id);
        return ResponseEntity.noContent().build();
    }
}
