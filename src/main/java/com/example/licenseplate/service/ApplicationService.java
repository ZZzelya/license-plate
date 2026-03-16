package com.example.licenseplate.service;

import com.example.licenseplate.dto.request.ApplicationCreateDto;
import com.example.licenseplate.dto.response.ApplicationDto;
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
import com.example.licenseplate.service.cache.ApplicationCacheService;
import com.example.licenseplate.service.mapper.ApplicationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private static final String NO_APPLICATIONS = "У заявителя {} нет заявок";
    private static final String CACHE_INVALIDATED = "Cache invalidated after creation";
    private static final String APPLICATION_SAVED = "Application saved with id: {}";
    private static final String APPLICATION_CONFIRMED = "Confirmed application with id: {}";
    private static final String APPLICATION_COMPLETED = "Completed application with id: {}";
    private static final String APPLICATION_CANCELLED = "Cancelled application with id: {}";
    private static final String APPLICATION_DELETED = "Deleted application with id: {}";
    private static final String APPLICATION_EXPIRED = "Application {} expired";
    private static final String FETCHING_JPQL = "Fetching applications by status: {} and region: {} (JPQL)";
    private static final String FETCHING_NATIVE = "Fetching applications by status: {} and region: {} (NATIVE)";
    private static final String FETCHING_CACHED = "Fetching applications by status: {} and region: {} (CACHED)";
    private static final String REGION_NOT_FOUND_LOG = "Region '{}' not found";
    private static final String CACHE_HIT = "Returning cached result for region: {}, status: {}";
    private static final String FOUND_APPLICATIONS = "Найдено {} заявок в регионе {} со статусом {}";
    private static final String FOUND_APPLICATIONS_NATIVE = "Native query: найдено {} заявок в регионе {} " +
        "со статусом {}";
    private static final String CACHED_RESULTS = "Cached {} results for region: {}, status: {}";
    private static final String PAGINATION_LOG = "Пагинация для паспорта: {}, page: {}, size: {}";
    private static final String PAGINATION_RESULT = "Найдено {} заявок из {}";
    private static final String CREATING_APPLICATION = "Creating application with transaction";
    private static final String WITHOUT_TX = "=== Demonstrating WITHOUT @Transactional ===";
    private static final String WITH_TX = "=== Demonstrating WITH @Transactional ===";
    private static final String APPLICATIONS_WITH_STATUS_NOT_FOUND = "Заявки со статусом {} в регионе {} не найдены";
    private static final String APPLICATIONS_WITH_STATUS_NOT_FOUND_NATIVE = "Заявки со статусом {} в регионе {} " +
        "не найдены (native)";
    private static final String NOT_IN_CONFIRMED_STATUS = "Application is not in CONFIRMED status: {}";

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
            log.info(NO_APPLICATIONS, passportNumber);
        }

        return applicationMapper.toDtoList(applications);
    }

    @Transactional(readOnly = true)
    public List<ApplicationDto> getApplicationsByStatusAndRegion(
        ApplicationStatus status, String region) {

        log.info(FETCHING_JPQL, status, region);

        if (!departmentRepository.existsByRegion(region)) {
            log.warn(REGION_NOT_FOUND_LOG, region);
            throw new ResourceNotFoundException(
                String.format(REGION_NOT_FOUND, region)
            );
        }

        List<Application> applications = applicationRepository
            .findByStatusAndDepartmentRegion(status, region);

        if (applications.isEmpty()) {
            log.warn(APPLICATIONS_WITH_STATUS_NOT_FOUND, status, region);
            throw new ResourceNotFoundException(
                String.format(APPLICATIONS_NOT_FOUND, status, region)
            );
        }

        log.info(FOUND_APPLICATIONS, applications.size(), region, status);
        return applicationMapper.toDtoList(applications);
    }

    @Transactional(readOnly = true)
    public List<ApplicationDto> getApplicationsByStatusAndRegionNative(
        ApplicationStatus status, String region) {

        log.info(FETCHING_NATIVE, status, region);

        if (!departmentRepository.existsByRegion(region)) {
            log.warn(REGION_NOT_FOUND_LOG, region);
            throw new ResourceNotFoundException(
                String.format(REGION_NOT_FOUND, region)
            );
        }

        List<Application> applications = applicationRepository
            .findByStatusAndDepartmentRegionNative(status.name(), region);

        if (applications.isEmpty()) {
            log.warn(APPLICATIONS_WITH_STATUS_NOT_FOUND_NATIVE, status, region);
            throw new ResourceNotFoundException(
                String.format(APPLICATIONS_NOT_FOUND, status, region)
            );
        }

        log.info(FOUND_APPLICATIONS_NATIVE, applications.size(), region, status);
        return applicationMapper.toDtoList(applications);
    }

    @Transactional(readOnly = true)
    public List<ApplicationDto> getApplicationsByStatusAndRegionCached(
        ApplicationStatus status, String region) {

        log.info(FETCHING_CACHED, status, region);

        if (!departmentRepository.existsByRegion(region)) {
            log.warn(REGION_NOT_FOUND_LOG, region);
            throw new ResourceNotFoundException(
                String.format(REGION_NOT_FOUND, region)
            );
        }

        List<ApplicationDto> cached = cacheService.get(status.name(), region);
        if (cached != null) {
            log.info(CACHE_HIT, region, status);

            if (cached.isEmpty()) {
                log.warn("В кэше пустой результат для {} {}", region, status);
                cacheService.invalidate();
            } else {
                return cached;
            }
        }

        List<Application> applications = applicationRepository
            .findByStatusAndDepartmentRegion(status, region);

        if (applications.isEmpty()) {
            log.warn(APPLICATIONS_WITH_STATUS_NOT_FOUND, status, region);
            throw new ResourceNotFoundException(
                String.format(APPLICATIONS_NOT_FOUND, status, region)
            );
        }

        List<ApplicationDto> result = applicationMapper.toDtoList(applications);
        cacheService.put(status.name(), region, result);

        log.info(CACHED_RESULTS, result.size(), region, status);

        return result;
    }

    @Transactional(readOnly = true)
    public Page<ApplicationDto> getApplicationsByPassportPaginated(
        String passportNumber, Pageable pageable) {

        log.info(PAGINATION_LOG,
            passportNumber, pageable.getPageNumber(), pageable.getPageSize());

        Page<Application> applicationPage = applicationRepository
            .findByApplicantPassport(passportNumber, pageable);

        if (applicationPage.isEmpty() && pageable.getPageNumber() == 0) {
            log.info(NO_APPLICATIONS, passportNumber);
        }

        log.info(PAGINATION_RESULT,
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
        log.info(CREATING_APPLICATION);
        ApplicationDto result = createApplicationInternal(createDto, true, true);
        cacheService.invalidate();
        log.info(CACHE_INVALIDATED);
        return result;
    }

    @Transactional
    public ApplicationDto createApplicationWithoutTransaction(
        final ApplicationCreateDto createDto) {
        log.info(WITHOUT_TX);
        ApplicationDto result = createApplicationInternal(createDto, false, false);
        cacheService.invalidate();
        return result;
    }

    @Transactional
    public ApplicationDto createApplicationWithTransaction(
        final ApplicationCreateDto createDto) {
        log.info(WITH_TX);
        ApplicationDto result = createApplicationInternal(createDto, true, false);
        cacheService.invalidate();
        return result;
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

        log.info(APPLICATION_CONFIRMED, id);
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

        log.info(APPLICATION_COMPLETED, id);
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

        log.info(APPLICATION_CANCELLED, id);
        cacheService.invalidate();

        return applicationMapper.toDto(applicationRepository.save(application));
    }

    @Transactional
    public void deleteApplication(final Long id) {
        Application application = findApplicationOrThrow(id);
        applicationRepository.delete(application);
        log.info(APPLICATION_DELETED, id);
        cacheService.invalidate();
    }

    private ApplicationDto createApplicationInternal(
        ApplicationCreateDto createDto,
        boolean useTransactionalCheck,
        boolean useExistingApplicant) {

        Applicant applicant = useExistingApplicant
            ? findApplicantByPassport(createDto.getPassportNumber())
            : findOrCreateApplicant(createDto.getPassportNumber());

        LicensePlate plate = findPlateByNumber(createDto.getPlateNumber());

        if (useTransactionalCheck) {
            if (!plate.isAvailable()) {
                throw new IllegalStateException(
                    String.format(PLATE_NOT_AVAILABLE, createDto.getPlateNumber()));
            }
        } else {
            if (!licensePlateRepository.isPlateAvailable(plate.getId())) {
                throw new IllegalStateException(
                    String.format(PLATE_NOT_AVAILABLE, createDto.getPlateNumber()));
            }
        }

        Application application = buildApplication(applicant, plate, createDto);
        Application saved = applicationRepository.save(application);
        log.info(APPLICATION_SAVED, saved.getId());

        if (createDto.getServiceIds() != null && !createDto.getServiceIds().isEmpty()) {
            List<AdditionalService> services = serviceRepository.findAllById(
                createDto.getServiceIds());

            if (services.size() != createDto.getServiceIds().size()) {
                String errorMsg = useTransactionalCheck
                    ? SERVICES_NOT_FOUND + " - transaction will rollback! Application will not be saved."
                    : SERVICES_NOT_FOUND + " - but application is already saved! Data is now inconsistent!";
                throw new BusinessException(errorMsg);
            }

            saved.setAdditionalServices(services);
            applicationRepository.save(saved);
        }

        return applicationMapper.toDto(saved);
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

    private Application buildApplication(Applicant applicant, LicensePlate plate,
                                         ApplicationCreateDto dto) {
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
        application.setPaymentAmount(calculateTotalAmount(plate, dto.getServiceIds()));
        return application;
    }

    private BigDecimal calculateTotalAmount(LicensePlate plate, List<Long> serviceIds) {
        BigDecimal total = plate.getPrice();

        if (serviceIds != null && !serviceIds.isEmpty()) {
            List<AdditionalService> services = serviceRepository.findAllById(serviceIds);
            BigDecimal servicesTotal = services.stream()
                .map(AdditionalService::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            total = total.add(servicesTotal);
        }

        return total;
    }

    private void expireApplication(Application application) {
        application.setStatus(ApplicationStatus.EXPIRED);
        applicationRepository.save(application);
        log.info(APPLICATION_EXPIRED, application.getId());
    }

    private Application findApplicationOrThrow(final Long id) {
        return applicationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format(APPLICATION_NOT_FOUND, id)));
    }
}