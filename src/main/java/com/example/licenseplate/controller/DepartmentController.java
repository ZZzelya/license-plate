package com.example.licenseplate.controller;

import com.example.licenseplate.dto.request.DepartmentCreateDto;
import com.example.licenseplate.dto.response.DepartmentDto;
import com.example.licenseplate.dto.DepartmentWithPlatesDto;
import com.example.licenseplate.service.DepartmentService;
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
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@Tag(name = "Отделы ГАИ", description = "API для управления отделами регистрации")
public class DepartmentController {

    private final DepartmentService departmentService;

    @Operation(
        summary = "Создать отдел ГАИ",
        description = "Регистрирует новый отдел регистрации"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Отдел успешно создан",
            content = @Content(schema = @Schema(implementation = DepartmentDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Неверные данные запроса",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Отдел с таким телефоном уже существует",
            content = @Content
        )
    })
    @PostMapping
    public ResponseEntity<DepartmentDto> createDepartment(
        @Parameter(description = "Данные для создания отдела", required = true)
        @Valid @RequestBody final DepartmentCreateDto createDto) {
        DepartmentDto created = departmentService.createDepartment(createDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
        summary = "Получить все отделы",
        description = "Возвращает список всех отделов ГАИ"
    )
    @GetMapping
    public ResponseEntity<List<DepartmentDto>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @Operation(
        summary = "Получить отдел по ID",
        description = "Возвращает информацию об отделе по его идентификатору"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Отдел найден",
            content = @Content(schema = @Schema(implementation = DepartmentDto.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Отдел не найден",
            content = @Content
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<DepartmentDto> getDepartmentById(
        @Parameter(description = "ID отдела", required = true, example = "1")
        @PathVariable final Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    @Operation(
        summary = "Получить отдел с номерными знаками",
        description = "Возвращает информацию об отделе вместе со списком его номерных знаков"
    )
    @GetMapping("/{id}/with-plates")
    public ResponseEntity<DepartmentWithPlatesDto> getDepartmentWithPlates(
        @Parameter(description = "ID отдела", required = true, example = "1")
        @PathVariable final Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentWithPlates(id));
    }

    @Operation(
        summary = "Получить отделы по региону",
        description = "Возвращает список отделов в указанном регионе"
    )
    @GetMapping("/by-region")
    public ResponseEntity<List<DepartmentDto>> getDepartmentsByRegion(
        @Parameter(description = "Регион", required = true, example = "MINSK")
        @RequestParam final String region) {
        return ResponseEntity.ok(departmentService.getDepartmentsByRegion(region));
    }

    @Operation(
        summary = "Обновить отдел",
        description = "Обновляет информацию о существующем отделе"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Отдел успешно обновлен",
            content = @Content(schema = @Schema(implementation = DepartmentDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Неверные данные запроса",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Отдел не найден",
            content = @Content
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<DepartmentDto> updateDepartment(
        @Parameter(description = "ID отдела", required = true, example = "1")
        @PathVariable final Long id,
        @Parameter(description = "Обновленные данные отдела", required = true)
        @Valid @RequestBody final DepartmentCreateDto updateDto) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, updateDto));
    }

    @Operation(
        summary = "Удалить отдел",
        description = "Удаляет отдел из системы. Невозможно удалить отдел с существующими номерными знаками."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Отдел успешно удален",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Отдел не найден",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Невозможно удалить отдел с существующими номерными знаками",
            content = @Content
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(
        @Parameter(description = "ID отдела", required = true, example = "1")
        @PathVariable final Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Демонстрация N+1 проблемы",
        description = "Показывает проблему N+1 запросов при загрузке отделов с номерными знаками"
    )
    @GetMapping("/demo/nplus1")
    public ResponseEntity<String> demonstrateNPlusOne(
        @Parameter(description = "Регион", required = true, example = "MINSK")
        @RequestParam final String region) {
        departmentService.demonstrateNPlusOneProblem(region);
        return ResponseEntity.ok("N+1 problem demonstrated. Check logs!");
    }

    @Operation(
        summary = "Решение N+1 проблемы (Fetch Join)",
        description = "Показывает решение N+1 проблемы с использованием Fetch Join"
    )
    @GetMapping("/demo/solved")
    public ResponseEntity<String> solveNPlusOne(
        @Parameter(description = "Регион", required = true, example = "MINSK")
        @RequestParam final String region) {
        departmentService.solveNPlusOneWithFetchJoin(region);
        return ResponseEntity.ok("N+1 solved with fetch join. Check logs!");
    }
}