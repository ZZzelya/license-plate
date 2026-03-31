package com.example.licenseplate.service;

import com.example.licenseplate.dto.request.ApplicationCreateDto;
import com.example.licenseplate.dto.request.BulkApplicationCreateDto;
import com.example.licenseplate.dto.response.ApplicationDto;
import com.example.licenseplate.dto.response.BulkApplicationResult;
import com.example.licenseplate.exception.ResourceNotFoundException;
import com.example.licenseplate.exception.BusinessException;
import com.example.licenseplate.model.entity.Applicant;
import com.example.licenseplate.model.entity.Application;
import com.example.licenseplate.model.entity.LicensePlate;
import com.example.licenseplate.model.entity.AdditionalService;
import com.example.licenseplate.model.enums.ApplicationStatus;
import com.example.licenseplate.repository.ApplicantRepository;
import com.example.licenseplate.repository.ApplicationRepository;
import com.example.licenseplate.repository.DepartmentRepository;
import com.example.licenseplate.repository.LicensePlateRepository;
import com.example.licenseplate.repository.ServiceRepository;
import com.example.licenseplate.cache.ApplicationCacheService;
import com.example.licenseplate.mapper.ApplicationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicantRepository applicantRepository;
    private final LicensePlateRepository licensePlateRepository;
    private final ServiceRepository serviceRepository;
    private final ApplicationMapper applicationMapper;
    private final ApplicationCacheService cacheService;
    private final DepartmentRepository departmentRepository;

    private static final String REGION_NOT_FOUND = "Регион '%s' не найден";
    private static final String APPLICANT_NOT_FOUND = "Заявитель с паспортом '%s' не найден";
    private static final String APPLICATION_NOT_FOUND = "Заявление не найдено с id: %d";
    private static final String APPLICATIONS_NOT_FOUND = "Заявки со статусом '%s' в регионе '%s' не найдены";
    private static final String PLATE_NOT_FOUND = "Номерной знак '%s' не найден";
    private static final String PLATE_NOT_AVAILABLE = "Номерной знак '%s' недоступен";
    private static final String SERVICES_NOT_FOUND = "Некоторые услуги не найдены";
    private static final String INVALID_STATUS = "Заявление не в статусе PENDING: %s";
    private static final String RESERVATION_EXPIRED = "Время бронирования истекло";
    private static final String CANNOT_CANCEL = "Нельзя отменить заявление в статусе: %s";
    private static final String NOT_IN_CONFIRMED_STATUS = "Application is not in CONFIRMED status: %s";

    @Transactional(readOnly = true)
    public List<ApplicationDto> getAllApplications() {
        return applicationMapper.toDtoList(applicationRepository.findAll());
    }

    @Transactional(readOnly = true)
    public ApplicationDto getApplicationById(final Long id) {
        return applicationMapper.toDto(findApplicationOrThrow(id));
    }

    @Transactional(readOnly = true)
    public ApplicationDto getApplicationWithDetails(final Long id) {
        Application application = applicationRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format(APPLICATION_NOT_FOUND, id)));
        return applicationMapper.toDto(application);
    }

    @Transactional(readOnly = true)
    public List<ApplicationDto> getApplicationsByPassport(final String passportNumber) {
        List<Application> applications = applicationRepository.findByApplicantPassport(passportNumber);

        if (applications.isEmpty()) {
            Optional<Applicant> applicant = applicantRepository.findByPassportNumber(passportNumber);
            if (applicant.isEmpty()) {
                throw new ResourceNotFoundException(
                    String.format(APPLICANT_NOT_FOUND, passportNumber)
                );
            }
            log.info("У заявителя {} нет заявок", passportNumber);
        }

        return applicationMapper.toDtoList(applications);
    }

    @Transactional(readOnly = true)
    public List<ApplicationDto> getApplicationsByStatusAndRegion(
        ApplicationStatus status, String region) {

        log.info("Fetching applications by status: {} and region: {} (JPQL)", status, region);

        if (!departmentRepository.existsByRegion(region)) {
            log.warn("Region '{}' not found", region);
            throw new ResourceNotFoundException(
                String.format(REGION_NOT_FOUND, region)
            );
        }

        List<Application> applications = applicationRepository
            .findByStatusAndDepartmentRegion(status, region);

        if (applications.isEmpty()) {
            log.warn("Заявки со статусом {} в регионе {} не найдены", status, region);
            throw new ResourceNotFoundException(
                String.format(APPLICATIONS_NOT_FOUND, status, region)
            );
        }

        log.info("Найдено {} заявок в регионе {} со статусом {}", applications.size(), region, status);
        return applicationMapper.toDtoList(applications);
    }

    @Transactional(readOnly = true)
    public List<ApplicationDto> getApplicationsByStatusAndRegionNative(
        ApplicationStatus status, String region) {

        log.info("Fetching applications by status: {} and region: {} (NATIVE)", status, region);

        if (!departmentRepository.existsByRegion(region)) {
            log.warn("Region '{}' not found", region);
            throw new ResourceNotFoundException(
                String.format(REGION_NOT_FOUND, region)
            );
        }

        List<Application> applications = applicationRepository
            .findByStatusAndDepartmentRegionNative(status.name(), region);

        if (applications.isEmpty()) {
            log.warn("Заявки со статусом {} в регионе {} не найдены (native)", status, region);
            throw new ResourceNotFoundException(
                String.format(APPLICATIONS_NOT_FOUND, status, region)
            );
        }

        log.info("Native query: найдено {} заявок в регионе {} со статусом {}", applications.size(), region, status);
        return applicationMapper.toDtoList(applications);
    }

    @Transactional(readOnly = true)
    public List<ApplicationDto> getApplicationsByStatusAndRegionCached(
        ApplicationStatus status, String region) {

        log.info("Fetching applications by status: {} and region: {} (CACHED)", status, region);

        List<ApplicationDto> cached = cacheService.get(status.name(), region);
        if (cached != null && !cached.isEmpty()) {
            log.info("Returning cached result for region: {}, status: {}", region, status);
            return cached;
        }

        if (cached != null && cached.isEmpty()) {
            log.warn("В кэше пустой результат для {} {}", region, status);
            cacheService.invalidate();
        }

        List<Application> applications = applicationRepository
            .findByStatusAndDepartmentRegion(status, region);

        List<ApplicationDto> result = applicationMapper.toDtoList(applications);

        if (!result.isEmpty()) {
            cacheService.put(status.name(), region, result);
            log.info("Cached {} results for region: {}, status: {}", result.size(), region, status);
        }

        return result;
    }

    @Transactional(readOnly = true)
    public Page<ApplicationDto> getApplicationsByPassportPaginated(
        String passportNumber, Pageable pageable) {

        log.info("Пагинация для паспорта: {}, page: {}, size: {}",
            passportNumber, pageable.getPageNumber(), pageable.getPageSize());

        Page<Application> applicationPage = applicationRepository
            .findByApplicantPassport(passportNumber, pageable);

        if (applicationPage.isEmpty() && pageable.getPageNumber() == 0) {
            log.info("У заявителя {} нет заявок", passportNumber);
        }

        log.info("Найдено {} заявок из {}",
            applicationPage.getNumberOfElements(),
            applicationPage.getTotalElements());

        return applicationPage.map(applicationMapper::toDto);
    }

    public void invalidateCache() {
        cacheService.invalidate();
    }

    public void invalidateCacheByRegion(String region) {
        cacheService.invalidateByRegion(region);
    }

    public void invalidateCacheByStatus(String status) {
        cacheService.invalidateByStatus(status);
    }

    @Transactional
    public ApplicationDto createApplication(final ApplicationCreateDto createDto) {
        log.info("Creating application with transaction");
        ApplicationDto result = createApplicationInternal(createDto, true);
        cacheService.invalidate();
        log.info("Cache invalidated after creation");
        return result;
    }

    public ApplicationDto createApplicationWithoutTransaction(
        final ApplicationCreateDto createDto) {
        log.info("=== Demonstrating WITHOUT @Transactional ===");
        ApplicationDto result = createApplicationInternal(createDto, false);
        cacheService.invalidate();
        return result;
    }

    @Transactional
    public ApplicationDto createApplicationWithTransaction(
        final ApplicationCreateDto createDto) {
        log.info("=== Demonstrating WITH @Transactional ===");
        ApplicationDto result = createApplicationInternal(createDto, true);
        cacheService.invalidate();
        return result;
    }

    private ApplicationDto createApplicationInternal(
        ApplicationCreateDto createDto,
        boolean useTransactionalCheck) {

        Applicant applicant = getApplicant(createDto.getPassportNumber(), useTransactionalCheck);
        LicensePlate plate = findPlateByNumber(createDto.getPlateNumber());

        validatePlateAvailability(plate, createDto.getPlateNumber(), useTransactionalCheck);

        List<AdditionalService> services = getServices(createDto, useTransactionalCheck);

        return saveApplication(applicant, plate, createDto, services);
    }

    private Applicant getApplicant(String passportNumber, boolean useExistingApplicant) {
        if (useExistingApplicant) {
            return findApplicantByPassport(passportNumber);
        }
        return findOrCreateApplicant(passportNumber);
    }

    private void validatePlateAvailability(LicensePlate plate, String plateNumber, boolean useTransactionalCheck) {
        if (useTransactionalCheck) {
            if (!plate.isAvailable()) {
                throw new IllegalStateException(String.format(PLATE_NOT_AVAILABLE, plateNumber));
            }
        } else {
            if (!licensePlateRepository.isPlateAvailable(plate.getId())) {
                throw new IllegalStateException(String.format(PLATE_NOT_AVAILABLE, plateNumber));
            }
        }
    }

    private List<AdditionalService> getServices(ApplicationCreateDto createDto, boolean useTransactionalCheck) {
        List<Long> serviceIds = createDto.getServiceIds();
        if (serviceIds == null || serviceIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<AdditionalService> services = serviceRepository.findAllById(serviceIds);

        if (services.size() != serviceIds.size()) {
            String errorMsg = useTransactionalCheck
                ? SERVICES_NOT_FOUND + " - transaction will rollback! Application will not be saved."
                : SERVICES_NOT_FOUND + " - but application is already saved! Data is now inconsistent!";
            throw new BusinessException(errorMsg);
        }

        return services;
    }

    private ApplicationDto saveApplication(Applicant applicant, LicensePlate plate,
                                           ApplicationCreateDto dto, List<AdditionalService> services) {
        Application application = buildApplication(applicant, plate, dto, services);
        Application saved = applicationRepository.save(application);
        log.info("Application saved with id: {}", saved.getId());

        saved.setAdditionalServices(services);
        applicationRepository.save(saved);

        return applicationMapper.toDto(saved);
    }

    private ApplicationDto createSingleApplication(ApplicationCreateDto createDto, Applicant applicant,
                                                   boolean transactional) {
        LicensePlate plate = licensePlateRepository.findByPlateNumber(createDto.getPlateNumber())
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format(PLATE_NOT_FOUND, createDto.getPlateNumber())));

        if (!plate.isAvailable()) {
            throw new BusinessException(String.format(PLATE_NOT_AVAILABLE, createDto.getPlateNumber()));
        }

        List<AdditionalService> services = null;
        if (createDto.getServiceIds() != null && !createDto.getServiceIds().isEmpty()) {
            services = serviceRepository.findAllById(createDto.getServiceIds());
            if (services.size() != createDto.getServiceIds().size()) {
                throw new BusinessException(SERVICES_NOT_FOUND);
            }
        }

        Application application = buildApplication(applicant, plate, createDto, services);
        application.setStatus(transactional ? ApplicationStatus.CONFIRMED : ApplicationStatus.PENDING);

        Application saved = applicationRepository.save(application);
        log.info("Application saved with id: {}", saved.getId());

        if (services != null && !services.isEmpty()) {
            saved.setAdditionalServices(services);
            applicationRepository.save(saved);
        }

        return applicationMapper.toDto(saved);
    }

    private Application buildApplication(Applicant applicant, LicensePlate plate,
                                         ApplicationCreateDto dto, List<AdditionalService> services) {
        Application application = new Application();
        application.setApplicant(applicant);
        application.setLicensePlate(plate);
        application.setDepartment(plate.getDepartment());
        application.setStatus(ApplicationStatus.PENDING);
        application.setSubmissionDate(LocalDateTime.now());
        application.setReservedUntil(LocalDateTime.now().plusHours(1));
        application.setVehicleVin(dto.getVehicleVin());
        application.setVehicleModel(dto.getVehicleModel());
        application.setVehicleYear(dto.getVehicleYear());
        application.setNotes(dto.getNotes());
        application.setPaymentAmount(calculateTotalAmount(plate, services));
        return application;
    }

    private BigDecimal calculateTotalAmount(LicensePlate plate, List<AdditionalService> services) {
        BigDecimal total = plate.getPrice();
        if (services != null && !services.isEmpty()) {
            BigDecimal servicesTotal = services.stream()
                .map(AdditionalService::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            total = total.add(servicesTotal);
        }
        return total;
    }

    private Applicant findApplicantByPassport(String passportNumber) {
        return applicantRepository.findByPassportNumber(passportNumber)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format(APPLICANT_NOT_FOUND, passportNumber)));
    }

    private Applicant findOrCreateApplicant(String passportNumber) {
        return applicantRepository.findByPassportNumber(passportNumber)
            .orElseGet(() -> {
                Applicant newApplicant = new Applicant();
                newApplicant.setFullName("UNKNOWN");
                newApplicant.setPassportNumber(passportNumber);
                return applicantRepository.save(newApplicant);
            });
    }

    private LicensePlate findPlateByNumber(String plateNumber) {
        return licensePlateRepository.findByPlateNumber(plateNumber)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format(PLATE_NOT_FOUND, plateNumber)));
    }

    private void expireApplication(Application application) {
        application.setStatus(ApplicationStatus.EXPIRED);
        applicationRepository.save(application);
        log.info("Application {} expired", application.getId());
    }

    private Application findApplicationOrThrow(final Long id) {
        return applicationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format(APPLICATION_NOT_FOUND, id)));
    }

    @Transactional
    public ApplicationDto confirmApplication(final Long id) {
        Application application = findApplicationOrThrow(id);

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new BusinessException(
                String.format(INVALID_STATUS, application.getStatus()));
        }

        if (application.getReservedUntil().isBefore(LocalDateTime.now())) {
            expireApplication(application);
            throw new BusinessException(RESERVATION_EXPIRED);
        }

        application.setStatus(ApplicationStatus.CONFIRMED);
        application.setConfirmationDate(LocalDateTime.now());

        log.info("Confirmed application with id: {}", id);
        cacheService.invalidate();

        return applicationMapper.toDto(applicationRepository.save(application));
    }

    @Transactional
    public ApplicationDto completeApplication(final Long id) {
        Application application = findApplicationOrThrow(id);

        if (application.getStatus() != ApplicationStatus.CONFIRMED) {
            throw new BusinessException(
                String.format(NOT_IN_CONFIRMED_STATUS, application.getStatus()));
        }

        application.setStatus(ApplicationStatus.COMPLETED);

        LicensePlate plate = application.getLicensePlate();
        plate.setIssueDate(LocalDateTime.now());
        plate.setExpiryDate(LocalDateTime.now().plusYears(10));
        licensePlateRepository.save(plate);

        log.info("Completed application with id: {}", id);
        cacheService.invalidate();

        return applicationMapper.toDto(applicationRepository.save(application));
    }

    @Transactional
    public ApplicationDto cancelApplication(final Long id) {
        Application application = findApplicationOrThrow(id);

        if (application.getStatus() == ApplicationStatus.COMPLETED ||
            application.getStatus() == ApplicationStatus.CANCELLED) {
            throw new BusinessException(
                String.format(CANNOT_CANCEL, application.getStatus()));
        }

        application.setStatus(ApplicationStatus.CANCELLED);

        log.info("Cancelled application with id: {}", id);
        cacheService.invalidate();

        return applicationMapper.toDto(applicationRepository.save(application));
    }

    @Transactional
    public void deleteApplication(final Long id) {
        Application application = findApplicationOrThrow(id);
        applicationRepository.delete(application);
        log.info("Deleted application with id: {}", id);
        cacheService.invalidate();
    }

    @Transactional
    public BulkApplicationResult createBulkApplicationsWithTransaction(BulkApplicationCreateDto bulkDto) {
        log.info("=== Creating bulk applications WITH @Transactional ===");
        return processBulkApplications(bulkDto, true);
    }

    public BulkApplicationResult createBulkApplicationsWithoutTransaction(BulkApplicationCreateDto bulkDto) {
        log.info("=== Creating bulk applications WITHOUT @Transactional ===");
        return processBulkApplications(bulkDto, false);
    }

    private BulkApplicationResult processBulkApplications(BulkApplicationCreateDto bulkDto,
                                                          boolean transactional) {
        BulkApplicationResult result = BulkApplicationResult.builder()
            .totalRequested(bulkDto.getApplications().size())
            .successful(0)
            .failed(0)
            .successfulApplications(new ArrayList<>())
            .errors(new ArrayList<>())
            .build();

        boolean hasDuplicatePlates = bulkDto.getApplications().stream()
            .map(ApplicationCreateDto::getPlateNumber)
            .distinct()
            .count() != bulkDto.getApplications().size();

        if (hasDuplicatePlates) {
            throw new BusinessException("Duplicate plate numbers in bulk request");
        }

        Applicant applicant = applicantRepository.findByPassportNumber(bulkDto.getPassportNumber())
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format(APPLICANT_NOT_FOUND, bulkDto.getPassportNumber())));

        for (ApplicationCreateDto createDto : bulkDto.getApplications()) {
            try {
                ApplicationDto created = createSingleApplication(createDto, applicant, transactional);
                result.getSuccessfulApplications().add(created);
                result.setSuccessful(result.getSuccessful() + 1);
            } catch (Exception e) {
                log.error("Failed to create application for plate {}: {}",
                    createDto.getPlateNumber(), e.getMessage());
                result.getErrors().add(String.format("Plate %s: %s",
                    createDto.getPlateNumber(), e.getMessage()));
                result.setFailed(result.getFailed() + 1);

                if (transactional) {
                    throw new BusinessException("Bulk application failed: " + e.getMessage());
                }
            }
        }

        log.info("Bulk application completed: total={}, success={}, failed={}",
            result.getTotalRequested(), result.getSuccessful(), result.getFailed());

        if (result.getSuccessful() > 0) {
            cacheService.invalidate();
        }

        return result;
    }
}