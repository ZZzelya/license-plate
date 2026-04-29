package com.example.licenseplate.controller;

import com.example.licenseplate.dto.request.ApplicantCreateDto;
import com.example.licenseplate.dto.response.ApplicantDto;
import com.example.licenseplate.model.enums.UserRole;
import com.example.licenseplate.service.ApplicantService;
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
import org.springframework.web.bind.annotation.PatchMapping;
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
@Tag(name = "Заявители", description = "API для управления заявителями")
public class ApplicantController {

    private final ApplicantService applicantService;

    @Operation(
        summary = "Создать заявителя",
        description = "Регистрирует нового заявителя в системе. Паспорт должен быть уникальным."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Заявитель успешно создан",
            content = @Content(schema = @Schema(implementation = ApplicantDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Неверные данные запроса",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Заявитель с таким паспортом уже существует",
            content = @Content
        )
    })
    @PostMapping
    public ResponseEntity<ApplicantDto> createApplicant(
        @Parameter(description = "Данные для создания заявителя", required = true)
        @Valid @RequestBody final ApplicantCreateDto createDto) {
        ApplicantDto created = applicantService.createApplicant(createDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
        summary = "Получить всех заявителей",
        description = "Возвращает список всех зарегистрированных заявителей"
    )
    @GetMapping
    public ResponseEntity<List<ApplicantDto>> getAllApplicants() {
        return ResponseEntity.ok(applicantService.getAllApplicants());
    }

    @Operation(
        summary = "Получить заявителя по ID",
        description = "Возвращает информацию о заявителе по его уникальному идентификатору"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Заявитель найден",
            content = @Content(schema = @Schema(implementation = ApplicantDto.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Заявитель не найден",
            content = @Content
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApplicantDto> getApplicantById(
        @Parameter(description = "ID заявителя", required = true, example = "1")
        @PathVariable final Long id) {
        return ResponseEntity.ok(applicantService.getApplicantById(id));
    }

    @Operation(
        summary = "Получить заявителя по паспорту",
        description = "Возвращает информацию о заявителе по номеру паспорта"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Заявитель найден",
            content = @Content(schema = @Schema(implementation = ApplicantDto.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Заявитель с указанным паспортом не найден",
            content = @Content
        )
    })
    @GetMapping("/by-passport")
    public ResponseEntity<ApplicantDto> getApplicantByPassport(
        @Parameter(description = "Номер паспорта в формате MP1234567", required = true, example = "MP1234567")
        @RequestParam final String passportNumber) {
        return ResponseEntity.ok(applicantService.getApplicantByPassport(passportNumber));
    }

    @Operation(
        summary = "Обновить заявителя",
        description = "Обновляет информацию о существующем заявителе"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Заявитель успешно обновлен",
            content = @Content(schema = @Schema(implementation = ApplicantDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Неверные данные запроса",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Заявитель не найден",
            content = @Content
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApplicantDto> updateApplicant(
        @Parameter(description = "ID заявителя", required = true, example = "1")
        @PathVariable final Long id,
        @Parameter(description = "Обновленные данные заявителя", required = true)
        @Valid @RequestBody final ApplicantCreateDto updateDto) {
        return ResponseEntity.ok(applicantService.updateApplicant(id, updateDto));
    }

    @Operation(
        summary = "Удалить заявителя",
        description = "Удаляет заявителя из системы. Невозможно удалить заявителя с существующими заявлениями."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Заявитель успешно удален",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Заявитель не найден",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Невозможно удалить заявителя с существующими заявлениями",
            content = @Content
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplicant(
        @Parameter(description = "ID заявителя", required = true, example = "1")
        @PathVariable final Long id) {
        applicantService.deleteApplicant(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Сменить паспорт заявителя",
        description = "Изменяет номер паспорта у заявителя. Недоступно при наличии активных заявлений."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Паспорт успешно изменен",
            content = @Content(schema = @Schema(implementation = ApplicantDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Неверные данные запроса",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Заявитель не найден",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Новый паспорт уже используется или есть активные заявления",
            content = @Content
        )
    })
    @PatchMapping("/{id}/passport")
    public ResponseEntity<ApplicantDto> changePassport(
        @Parameter(description = "ID заявителя", required = true, example = "1")
        @PathVariable Long id,
        @Parameter(description = "Новый номер паспорта", required = true, example = "MP7654321")
        @RequestParam String newPassportNumber) {
        return ResponseEntity.ok(applicantService.changePassport(id, newPassportNumber));
    }
    @PatchMapping("/{id}/role")
    public ResponseEntity<ApplicantDto> updateApplicantRole(
        @PathVariable final Long id,
        @RequestParam final UserRole role) {
        return ResponseEntity.ok(applicantService.updateApplicantRole(id, role));
    }
}
