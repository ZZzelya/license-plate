package com.example.licenseplate.controller;

import com.example.licenseplate.dto.request.ApplicationCreateDto;
import com.example.licenseplate.dto.response.ApplicationDto;
import com.example.licenseplate.model.enums.ApplicationStatus;
import com.example.licenseplate.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Заявления", description = "API для управления заявлениями на номерные знаки")
public class ApplicationController {

    private final ApplicationService applicationService;

    @Operation(
        summary = "Создать заявление",
        description = "Создает новое заявление на номерной знак с бронированием на 1 час"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Заявление успешно создано",
            content = @Content(schema = @Schema(implementation = ApplicationDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Неверные данные запроса или номерной знак недоступен",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Заявитель или номерной знак не найдены",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Номерной знак уже забронирован или занят",
            content = @Content
        )
    })
    @PostMapping
    public ResponseEntity<ApplicationDto> createApplication(
        @Parameter(description = "Данные для создания заявления", required = true)
        @Valid @RequestBody final ApplicationCreateDto createDto) {
        ApplicationDto created = applicationService.createApplication(createDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
        summary = "Создать заявление (без транзакции) - ДЕМО",
        description = "Демонстрационный метод, показывающий проблему при отсутствии @Transactional"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Заявление создано (возможно с неконсистентными данными)",
            content = @Content(schema = @Schema(implementation = ApplicationDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Неверные данные запроса",
            content = @Content
        )
    })
    @PostMapping("/demo/without-tx")
    public ResponseEntity<ApplicationDto> createApplicationWithoutTransaction(
        @Valid @RequestBody final ApplicationCreateDto createDto) {
        ApplicationDto created = applicationService.createApplicationWithoutTransaction(createDto);
        return ResponseEntity.ok(created);
    }

    @Operation(
        summary = "Создать заявление (с транзакцией) - ДЕМО",
        description = "Демонстрационный метод, показывающий корректную работу с @Transactional"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Заявление успешно создано",
            content = @Content(schema = @Schema(implementation = ApplicationDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Неверные данные запроса",
            content = @Content
        )
    })
    @PostMapping("/demo/with-tx")
    public ResponseEntity<ApplicationDto> createApplicationWithTransaction(
        @Valid @RequestBody final ApplicationCreateDto createDto) {
        ApplicationDto created = applicationService.createApplicationWithTransaction(createDto);
        return ResponseEntity.ok(created);
    }

    @Operation(
        summary = "Получить все заявления",
        description = "Возвращает список всех заявлений"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список заявлений успешно получен",
            content = @Content(schema = @Schema(implementation = ApplicationDto.class))
        )
    })
    @GetMapping
    public ResponseEntity<List<ApplicationDto>> getAllApplications() {
        return ResponseEntity.ok(applicationService.getAllApplications());
    }

    @Operation(
        summary = "Получить заявление по ID",
        description = "Возвращает информацию о заявлении по его идентификатору"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Заявление найдено",
            content = @Content(schema = @Schema(implementation = ApplicationDto.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Заявление не найдено",
            content = @Content
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationDto> getApplicationById(
        @Parameter(description = "ID заявления", required = true, example = "1")
        @PathVariable final Long id) {
        return ResponseEntity.ok(applicationService.getApplicationById(id));
    }

    @Operation(
        summary = "Получить заявление с деталями",
        description = "Возвращает заявление с полной информацией о заявителе, номере и услугах"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Заявление с деталями найдено",
            content = @Content(schema = @Schema(implementation = ApplicationDto.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Заявление не найдено",
            content = @Content
        )
    })
    @GetMapping("/{id}/with-details")
    public ResponseEntity<ApplicationDto> getApplicationWithDetails(
        @Parameter(description = "ID заявления", required = true, example = "1")
        @PathVariable final Long id) {
        return ResponseEntity.ok(applicationService.getApplicationWithDetails(id));
    }

    @Operation(
        summary = "Получить заявления по паспорту",
        description = "Возвращает все заявления заявителя по номеру паспорта"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список заявлений найден",
            content = @Content(schema = @Schema(implementation = ApplicationDto.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Заявитель с указанным паспортом не найден",
            content = @Content
        )
    })
    @GetMapping("/by-passport")
    public ResponseEntity<List<ApplicationDto>> getApplicationsByPassport(
        @Parameter(description = "Номер паспорта", required = true, example = "MP1234567")
        @RequestParam final String passportNumber) {
        return ResponseEntity.ok(applicationService.getApplicationsByPassport(passportNumber));
    }

    @Operation(
        summary = "Подтвердить заявление",
        description = "Подтверждает заявление, меняет статус на CONFIRMED"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Заявление подтверждено",
            content = @Content(schema = @Schema(implementation = ApplicationDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Заявление не в статусе PENDING",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Время бронирования истекло",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Заявление не найдено",
            content = @Content
        )
    })
    @PatchMapping("/{id}/confirm")
    public ResponseEntity<ApplicationDto> confirmApplication(
        @Parameter(description = "ID заявления", required = true, example = "1")
        @PathVariable final Long id) {
        return ResponseEntity.ok(applicationService.confirmApplication(id));
    }

    @Operation(
        summary = "Завершить заявление",
        description = "Завершает заявление, выдает номерной знак, меняет статус на COMPLETED"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Заявление завершено",
            content = @Content(schema = @Schema(implementation = ApplicationDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Заявление не в статусе CONFIRMED",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Заявление не найдено",
            content = @Content
        )
    })
    @PatchMapping("/{id}/complete")
    public ResponseEntity<ApplicationDto> completeApplication(
        @Parameter(description = "ID заявления", required = true, example = "1")
        @PathVariable final Long id) {
        return ResponseEntity.ok(applicationService.completeApplication(id));
    }

    @Operation(
        summary = "Отменить заявление",
        description = "Отменяет заявление, меняет статус на CANCELLED"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Заявление отменено",
            content = @Content(schema = @Schema(implementation = ApplicationDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Нельзя отменить заявление в статусе COMPLETED или CANCELLED",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Заявление не найдено",
            content = @Content
        )
    })
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApplicationDto> cancelApplication(
        @Parameter(description = "ID заявления", required = true, example = "1")
        @PathVariable final Long id) {
        return ResponseEntity.ok(applicationService.cancelApplication(id));
    }

    @Operation(
        summary = "Удалить заявление",
        description = "Удаляет заявление из системы"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Заявление успешно удалено",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Заявление не найдено",
            content = @Content
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(
        @Parameter(description = "ID заявления", required = true, example = "1")
        @PathVariable final Long id) {
        applicationService.deleteApplication(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Фильтр заявлений по статусу и региону (JPQL)",
        description = "Возвращает заявления по статусу и региону с использованием JPQL запроса"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список заявлений найден",
            content = @Content(schema = @Schema(implementation = ApplicationDto.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Регион не найден или заявки не найдены",
            content = @Content
        )
    })
    @GetMapping("/filter")
    public ResponseEntity<List<ApplicationDto>> getApplicationsByStatusAndRegion(
        @Parameter(description = "Статус заявления", required = true, example = "PENDING")
        @RequestParam ApplicationStatus status,
        @Parameter(description = "Регион", required = true, example = "MINSK")
        @RequestParam String region) {
        return ResponseEntity.ok(applicationService.getApplicationsByStatusAndRegion(status, region));
    }

    @Operation(
        summary = "Фильтр заявлений с кэшированием",
        description = "Возвращает заявления по статусу и региону с использованием кэша (TTL 5 минут)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список заявлений найден (из кэша или БД)",
            content = @Content(schema = @Schema(implementation = ApplicationDto.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Регион не найден или заявки не найдены",
            content = @Content
        )
    })
    @GetMapping("/filter/cached")
    public ResponseEntity<List<ApplicationDto>> getApplicationsByStatusAndRegionCached(
        @Parameter(description = "Статус заявления", required = true, example = "PENDING")
        @RequestParam ApplicationStatus status,
        @Parameter(description = "Регион", required = true, example = "MINSK")
        @RequestParam String region) {
        return ResponseEntity.ok(applicationService.getApplicationsByStatusAndRegionCached(status, region));
    }

    @Operation(
        summary = "Фильтр заявлений (Native SQL)",
        description = "Возвращает заявления по статусу и региону с использованием нативного SQL запроса"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список заявлений найден",
            content = @Content(schema = @Schema(implementation = ApplicationDto.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Регион не найден или заявки не найдены",
            content = @Content
        )
    })
    @GetMapping("/filter/native")
    public ResponseEntity<List<ApplicationDto>> getApplicationsByStatusAndRegionNative(
        @Parameter(description = "Статус заявления", required = true, example = "PENDING")
        @RequestParam ApplicationStatus status,
        @Parameter(description = "Регион", required = true, example = "MINSK")
        @RequestParam String region) {
        return ResponseEntity.ok(applicationService.getApplicationsByStatusAndRegionNative(status, region));
    }

    @Operation(
        summary = "Очистить весь кэш",
        description = "Полностью инвалидирует кэш заявлений"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Кэш успешно очищен",
            content = @Content
        )
    })
    @DeleteMapping("/cache")
    public ResponseEntity<Void> invalidateCache() {
        applicationService.invalidateCache();
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Очистить кэш по региону",
        description = "Инвалидирует кэш заявлений для указанного региона"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Кэш для региона успешно очищен",
            content = @Content
        )
    })
    @DeleteMapping("/cache/region/{region}")
    public ResponseEntity<Void> invalidateCacheByRegion(
        @Parameter(description = "Регион", required = true, example = "MINSK")
        @PathVariable String region) {
        applicationService.invalidateCacheByRegion(region);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Очистить кэш по статусу",
        description = "Инвалидирует кэш заявлений для указанного статуса"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Кэш для статуса успешно очищен",
            content = @Content
        )
    })
    @DeleteMapping("/cache/status/{status}")
    public ResponseEntity<Void> invalidateCacheByStatus(
        @Parameter(description = "Статус", required = true, example = "PENDING")
        @PathVariable String status) {
        applicationService.invalidateCacheByStatus(status);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Пагинация заявлений по паспорту",
        description = "Возвращает страницу заявлений заявителя с пагинацией (сортировка по дате создания DESC)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Страница заявлений найдена",
            content = @Content(schema = @Schema(implementation = Page.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Заявитель с указанным паспортом не найден",
            content = @Content
        )
    })
    @GetMapping("/by-passport/paginated")
    public ResponseEntity<Page<ApplicationDto>> getApplicationsByPassportPaginated(
        @Parameter(description = "Номер паспорта", required = true, example = "MP1234567")
        @RequestParam String passportNumber,
        @Parameter(description = "Номер страницы (начиная с 0)", example = "0")
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Размер страницы", example = "10")
        @RequestParam(defaultValue = "10") int size) {

        log.info("Пагинация: passport={}, page={}, size={}", passportNumber, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("submissionDate").descending());

        return ResponseEntity.ok(
            applicationService.getApplicationsByPassportPaginated(passportNumber, pageable));
    }

    @Operation(
        summary = "Пагинация заявлений с сортировкой",
        description = "Возвращает страницу заявлений заявителя с пагинацией и кастомной сортировкой"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Страница заявлений найдена",
            content = @Content(schema = @Schema(implementation = Page.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Заявитель с указанным паспортом не найден",
            content = @Content
        )
    })
    @GetMapping("/by-passport/paginated/sorted")
    public ResponseEntity<Page<ApplicationDto>> getApplicationsByPassportPaginatedSorted(
        @Parameter(description = "Номер паспорта", required = true, example = "MP1234567")
        @RequestParam String passportNumber,
        @Parameter(description = "Номер страницы (начиная с 0)", example = "0")
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Размер страницы", example = "10")
        @RequestParam(defaultValue = "10") int size,
        @Parameter(description = "Поле для сортировки", example = "submissionDate")
        @RequestParam(defaultValue = "submissionDate") String sortBy,
        @Parameter(description = "Направление сортировки (asc/desc)", example = "desc")
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