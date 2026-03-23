package com.example.licenseplate.controller;

import com.example.licenseplate.dto.request.ServiceCreateDto;
import com.example.licenseplate.dto.response.ServiceDto;
import com.example.licenseplate.service.ServiceManagementService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
@Tag(name = "Дополнительные услуги", description = "API для управления дополнительными услугами")
public class ServiceManagementController {

    private final ServiceManagementService serviceManagementService;

    @Operation(
        summary = "Создать услугу",
        description = "Регистрирует новую дополнительную услугу"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Услуга успешно создана",
            content = @Content(schema = @Schema(implementation = ServiceDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Неверные данные запроса",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Услуга с таким названием уже существует",
            content = @Content
        )
    })
    @PostMapping
    public ResponseEntity<ServiceDto> createService(
        @Parameter(description = "Данные для создания услуги", required = true)
        @Valid @RequestBody final ServiceCreateDto createDto) {
        ServiceDto created = serviceManagementService.createService(createDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
        summary = "Получить все услуги",
        description = "Возвращает список всех услуг"
    )
    @GetMapping
    public ResponseEntity<List<ServiceDto>> getAllServices() {
        return ResponseEntity.ok(serviceManagementService.getAllServices());
    }

    @Operation(
        summary = "Получить доступные услуги",
        description = "Возвращает список услуг, доступных для подключения"
    )
    @GetMapping("/available")
    public ResponseEntity<List<ServiceDto>> getAvailableServices() {
        return ResponseEntity.ok(serviceManagementService.getAvailableServices());
    }

    @Operation(
        summary = "Получить услугу по ID",
        description = "Возвращает информацию об услуге по ее идентификатору"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Услуга найдена",
            content = @Content(schema = @Schema(implementation = ServiceDto.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Услуга не найдена",
            content = @Content
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<ServiceDto> getServiceById(
        @Parameter(description = "ID услуги", required = true, example = "1")
        @PathVariable final Long id) {
        return ResponseEntity.ok(serviceManagementService.getServiceById(id));
    }

    @Operation(
        summary = "Обновить услугу",
        description = "Обновляет информацию о существующей услуге"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Услуга успешно обновлена",
            content = @Content(schema = @Schema(implementation = ServiceDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Неверные данные запроса",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Услуга не найдена",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Услуга с таким названием уже существует",
            content = @Content
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<ServiceDto> updateService(
        @Parameter(description = "ID услуги", required = true, example = "1")
        @PathVariable final Long id,
        @Parameter(description = "Обновленные данные услуги", required = true)
        @Valid @RequestBody final ServiceCreateDto updateDto) {
        return ResponseEntity.ok(serviceManagementService.updateService(id, updateDto));
    }

    @Operation(
        summary = "Удалить услугу",
        description = "Удаляет услугу из системы. Невозможно удалить услугу, используемую в заявлениях."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Услуга успешно удалена",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Услуга не найдена",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Невозможно удалить услугу, используемую в заявлениях",
            content = @Content
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteService(
        @Parameter(description = "ID услуги", required = true, example = "1")
        @PathVariable final Long id) {
        serviceManagementService.deleteService(id);
        return ResponseEntity.noContent().build();
    }
}