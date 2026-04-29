package com.example.licenseplate.service;

import com.example.licenseplate.dto.request.ApplicationCreateDto;
import com.example.licenseplate.dto.response.ApplicationDto;
import com.example.licenseplate.exception.BusinessException;
import com.example.licenseplate.exception.ResourceNotFoundException;
import com.example.licenseplate.mapper.ApplicationMapper;
import com.example.licenseplate.model.entity.AdditionalService;
import com.example.licenseplate.model.entity.Applicant;
import com.example.licenseplate.model.entity.Application;
import com.example.licenseplate.model.entity.LicensePlate;
import com.example.licenseplate.model.entity.RegistrationDept;
import com.example.licenseplate.model.enums.ApplicationStatus;
import com.example.licenseplate.repository.ApplicantRepository;
import com.example.licenseplate.repository.ApplicationRepository;
import com.example.licenseplate.repository.DepartmentRepository;
import com.example.licenseplate.repository.LicensePlateRepository;
import com.example.licenseplate.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private static final String AVAILABLE_PLATE_NOT_FOUND =
        "Номерной знак '%s' не найден среди доступных номеров в регионе '%s'";
    private static final String AVAILABLE_PLATES_NOT_FOUND =
        "В регионе '%s' нет доступных номеров для выдачи";
    private static final String REGION_REQUIRED = "Для выдачи номера нужно указать регион";
    private static final String PLATE_REQUIRED = "Для выбора номера нужно указать номерной знак";
    private static final String VEHICLE_TYPE_REQUIRED = "Для подбора номера укажите тип автомобиля";
    private static final String PLATE_TYPE_MISMATCH =
        "Тип номерного знака не соответствует выбранному типу автомобиля";
    private static final String CONFLICTING_PLATE_SERVICES =
        "Нельзя одновременно выбирать услуги выбора и персонализированного номера";
    private static final String INVALID_VIN = "VIN должен содержать ровно 17 латинских букв и цифр";
    private static final String INVALID_PERSONALIZED_PLATE =
        "Персонализированный номер должен быть в формате 3256 XX-2";
    private static final String APPLICANT_NOT_FOUND = "Заявитель с паспортом '%s' не найден";
    private static final String APPLICATION_NOT_FOUND = "Заявление не найдено с id: %d";
    private static final String PLATE_NOT_FOUND = "Номерной знак '%s' не найден";
    private static final String PLATE_NOT_AVAILABLE = "Номерной знак '%s' недоступен";
    private static final String SERVICES_NOT_FOUND = "Некоторые услуги не найдены";
    private static final String INVALID_STATUS =
        "Заявление должно быть в статусе «На рассмотрении», текущий статус: %s";
    private static final String RESERVATION_EXPIRED = "Время бронирования истекло";
    private static final String CANNOT_CANCEL = "Нельзя отменить заявление в статусе: %s";
    private static final String NOT_IN_CONFIRMED_STATUS =
        "Заявление должно быть в статусе «Подтверждено», текущий статус: %s";

    private final ApplicationRepository applicationRepository;
    private final ApplicantRepository applicantRepository;
    private final LicensePlateRepository licensePlateRepository;
    private final ServiceRepository serviceRepository;
    private final ApplicationMapper applicationMapper;
    private final DepartmentRepository departmentRepository;
    private final LicensePlateService licensePlateService;

    @Cacheable("applications")
    @Transactional(readOnly = true)
    public List<ApplicationDto> getAllApplications() {
        return applicationMapper.toDtoList(applicationRepository.findAll());
    }

    @Cacheable(cacheNames = "applicationById", key = "#id")
    @Transactional(readOnly = true)
    public ApplicationDto getApplicationWithDetails(final Long id) {
        Application application = applicationRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new ResourceNotFoundException(String.format(APPLICATION_NOT_FOUND, id)));
        return applicationMapper.toDto(application);
    }

    @Cacheable(cacheNames = "applicationsByPassport", key = "#passportNumber")
    @Transactional(readOnly = true)
    public List<ApplicationDto> getApplicationsByPassport(final String passportNumber) {
        List<Application> applications = applicationRepository.findByApplicantPassport(passportNumber);

        if (applications.isEmpty() && applicantRepository.findByPassportNumber(passportNumber).isEmpty()) {
            throw new ResourceNotFoundException(String.format(APPLICANT_NOT_FOUND, passportNumber));
        }

        return applicationMapper.toDtoList(applications);
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "applications", allEntries = true),
        @CacheEvict(cacheNames = "applicationById", allEntries = true),
        @CacheEvict(cacheNames = "applicationsByPassport", allEntries = true),
        @CacheEvict(cacheNames = "licensePlates", allEntries = true),
        @CacheEvict(cacheNames = "licensePlateById", allEntries = true),
        @CacheEvict(cacheNames = "licensePlateByNumber", allEntries = true),
        @CacheEvict(cacheNames = "availablePlatesByRegion", allEntries = true),
        @CacheEvict(cacheNames = "availablePlatesByDepartment", allEntries = true)
    })
    @Transactional
    public ApplicationDto createApplication(final ApplicationCreateDto createDto) {
        Applicant applicant = findApplicantByPassport(createDto.getPassportNumber());
        List<AdditionalService> services = getServices(createDto);
        LicensePlate plate = resolvePlate(createDto, services);
        validatePlateAvailability(plate, plate.getPlateNumber());
        return saveApplication(applicant, plate, createDto, services, getDepartment(createDto.getDepartmentId()));
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "applications", allEntries = true),
        @CacheEvict(cacheNames = "applicationById", allEntries = true),
        @CacheEvict(cacheNames = "applicationsByPassport", allEntries = true),
        @CacheEvict(cacheNames = "licensePlates", allEntries = true),
        @CacheEvict(cacheNames = "licensePlateById", allEntries = true),
        @CacheEvict(cacheNames = "licensePlateByNumber", allEntries = true),
        @CacheEvict(cacheNames = "availablePlatesByRegion", allEntries = true),
        @CacheEvict(cacheNames = "availablePlatesByDepartment", allEntries = true)
    })
    @Transactional
    public ApplicationDto confirmApplication(final Long id, final String adminComment) {
        Application application = findApplicationOrThrow(id);

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new BusinessException(String.format(INVALID_STATUS, application.getStatus()));
        }

        if (application.getReservedUntil().isBefore(LocalDateTime.now())) {
            expireApplication(application);
            throw new BusinessException(RESERVATION_EXPIRED);
        }

        application.setStatus(ApplicationStatus.CONFIRMED);
        application.setConfirmationDate(LocalDateTime.now());
        application.setAdminComment(normalizeOptionalComment(adminComment));
        return applicationMapper.toDto(applicationRepository.save(application));
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "applications", allEntries = true),
        @CacheEvict(cacheNames = "applicationById", allEntries = true),
        @CacheEvict(cacheNames = "applicationsByPassport", allEntries = true),
        @CacheEvict(cacheNames = "licensePlates", allEntries = true),
        @CacheEvict(cacheNames = "licensePlateById", allEntries = true),
        @CacheEvict(cacheNames = "licensePlateByNumber", allEntries = true),
        @CacheEvict(cacheNames = "availablePlatesByRegion", allEntries = true),
        @CacheEvict(cacheNames = "availablePlatesByDepartment", allEntries = true)
    })
    @Transactional
    public ApplicationDto completeApplication(final Long id, final String adminComment) {
        Application application = findApplicationOrThrow(id);

        if (application.getStatus() != ApplicationStatus.CONFIRMED) {
            throw new BusinessException(String.format(NOT_IN_CONFIRMED_STATUS, application.getStatus()));
        }

        application.setStatus(ApplicationStatus.COMPLETED);
        application.setAdminComment(normalizeOptionalComment(adminComment));

        LicensePlate plate = application.getLicensePlate();
        plate.setIssueDate(LocalDateTime.now());
        plate.setExpiryDate(LocalDateTime.now().plusYears(10));
        licensePlateRepository.save(plate);

        return applicationMapper.toDto(applicationRepository.save(application));
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "applications", allEntries = true),
        @CacheEvict(cacheNames = "applicationById", allEntries = true),
        @CacheEvict(cacheNames = "applicationsByPassport", allEntries = true),
        @CacheEvict(cacheNames = "licensePlates", allEntries = true),
        @CacheEvict(cacheNames = "licensePlateById", allEntries = true),
        @CacheEvict(cacheNames = "licensePlateByNumber", allEntries = true),
        @CacheEvict(cacheNames = "availablePlatesByRegion", allEntries = true),
        @CacheEvict(cacheNames = "availablePlatesByDepartment", allEntries = true)
    })
    @Transactional
    public ApplicationDto cancelApplication(final Long id, final String adminComment) {
        Application application = findApplicationOrThrow(id);

        if (application.getStatus() == ApplicationStatus.COMPLETED
            || application.getStatus() == ApplicationStatus.CANCELLED) {
            throw new BusinessException(String.format(CANNOT_CANCEL, application.getStatus()));
        }

        application.setStatus(ApplicationStatus.CANCELLED);
        application.setAdminComment(normalizeOptionalComment(adminComment));
        return applicationMapper.toDto(applicationRepository.save(application));
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "applications", allEntries = true),
        @CacheEvict(cacheNames = "applicationById", allEntries = true),
        @CacheEvict(cacheNames = "applicationsByPassport", allEntries = true),
        @CacheEvict(cacheNames = "licensePlates", allEntries = true),
        @CacheEvict(cacheNames = "licensePlateById", allEntries = true),
        @CacheEvict(cacheNames = "licensePlateByNumber", allEntries = true),
        @CacheEvict(cacheNames = "availablePlatesByRegion", allEntries = true),
        @CacheEvict(cacheNames = "availablePlatesByDepartment", allEntries = true)
    })
    @Transactional
    public void deleteApplication(final Long id) {
        applicationRepository.delete(findApplicationOrThrow(id));
    }

    private List<AdditionalService> getServices(ApplicationCreateDto createDto) {
        List<Long> serviceIds = createDto.getServiceIds();
        if (serviceIds == null || serviceIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<AdditionalService> services = serviceRepository.findAllById(serviceIds);
        if (services.size() != serviceIds.size()) {
            throw new BusinessException(SERVICES_NOT_FOUND);
        }

        return services;
    }

    private LicensePlate resolvePlate(ApplicationCreateDto createDto, List<AdditionalService> services) {
        PlateSelectionMode selectionMode = determinePlateSelectionMode(services);
        String plateNumber = normalizeValue(createDto.getPlateNumber());
        VehicleType vehicleType = resolveVehicleType(createDto.getVehicleType());
        validateVehicleVin(createDto.getVehicleVin());

        RegistrationDept department = getDepartment(createDto.getDepartmentId());
        String region = department != null ? department.getRegion() : normalizeValue(createDto.getRegion());
        boolean duplicateService = services.stream().anyMatch(this::isDuplicatePlateService);
        boolean keepCurrentPlateService = services.stream().anyMatch(this::isKeepCurrentPlateService);

        if (selectionMode == PlateSelectionMode.RANDOM && plateNumber != null) {
            LicensePlate plate = findPlateByNumber(plateNumber);
            validateRequestedExistingPlate(plate, plateNumber, duplicateService);
            validatePlateVehicleType(plate, vehicleType);
            return plate;
        }

        if ((duplicateService || keepCurrentPlateService) && plateNumber == null) {
            throw new BusinessException(PLATE_REQUIRED);
        }

        return switch (selectionMode) {
            case RANDOM -> {
                if (department != null) {
                    yield getRandomAvailablePlateByDepartment(department.getId(), department.getRegion(), vehicleType);
                }
                if (region == null) {
                    throw new BusinessException(REGION_REQUIRED);
                }
                yield getRandomAvailablePlate(region, vehicleType);
            }
            case AVAILABLE -> {
                if (department != null) {
                    yield findAvailablePlateByDepartment(department.getId(), department.getRegion(), plateNumber, vehicleType);
                }
                if (region == null) {
                    throw new BusinessException(REGION_REQUIRED);
                }
                yield findAvailablePlate(region, plateNumber, vehicleType);
            }
            case PERSONALIZED -> {
                if (department == null) {
                    throw new BusinessException("Для подачи заявления выберите отделение");
                }
                yield resolvePersonalizedPlate(department, plateNumber, vehicleType);
            }
        };
    }

    private PlateSelectionMode determinePlateSelectionMode(List<AdditionalService> services) {
        boolean hasAvailableSelection = services.stream().anyMatch(this::isAvailableSelectionService);
        boolean hasPersonalizedSelection = services.stream().anyMatch(this::isPersonalizedPlateService);

        if (hasAvailableSelection && hasPersonalizedSelection) {
            throw new BusinessException(CONFLICTING_PLATE_SERVICES);
        }
        if (hasPersonalizedSelection) {
            return PlateSelectionMode.PERSONALIZED;
        }
        if (hasAvailableSelection) {
            return PlateSelectionMode.AVAILABLE;
        }
        return PlateSelectionMode.RANDOM;
    }

    private LicensePlate getRandomAvailablePlate(String region, VehicleType vehicleType) {
        List<LicensePlate> availablePlates = filterByVehicleType(
            licensePlateRepository.findAvailableByRegion(region), vehicleType);
        if (availablePlates.isEmpty()) {
            throw new ResourceNotFoundException(String.format(AVAILABLE_PLATES_NOT_FOUND, region));
        }
        return availablePlates.get(ThreadLocalRandom.current().nextInt(availablePlates.size()));
    }

    private LicensePlate getRandomAvailablePlateByDepartment(Long departmentId, String departmentRegion,
                                                            VehicleType vehicleType) {
        List<LicensePlate> availablePlates = filterByVehicleType(
            licensePlateService.getOrCreateAvailablePlatesByDepartment(departmentId), vehicleType);
        if (availablePlates.isEmpty()) {
            throw new ResourceNotFoundException(String.format(AVAILABLE_PLATES_NOT_FOUND, departmentRegion));
        }
        return availablePlates.get(ThreadLocalRandom.current().nextInt(availablePlates.size()));
    }

    private LicensePlate findAvailablePlate(String region, String plateNumber, VehicleType vehicleType) {
        if (plateNumber == null) {
            throw new BusinessException(PLATE_REQUIRED);
        }
        if (PlateFormatSupport.parse(plateNumber) == null) {
            throw new BusinessException(INVALID_PERSONALIZED_PLATE);
        }

        LicensePlate plate = licensePlateRepository.findAvailableByRegionAndPlateNumber(region, plateNumber)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format(AVAILABLE_PLATE_NOT_FOUND, plateNumber, region)));

        validatePlateAvailability(plate, plateNumber);
        validatePlateVehicleType(plate, vehicleType);
        return plate;
    }

    private LicensePlate findAvailablePlateByDepartment(Long departmentId, String departmentRegion,
                                                        String plateNumber, VehicleType vehicleType) {
        if (plateNumber == null) {
            throw new BusinessException(PLATE_REQUIRED);
        }

        LicensePlate plate = filterByVehicleType(licensePlateService.getOrCreateAvailablePlatesByDepartment(departmentId), vehicleType)
            .stream()
            .filter(item -> PlateFormatSupport.normalize(item.getPlateNumber()).equals(PlateFormatSupport.normalize(plateNumber)))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException(String.format(AVAILABLE_PLATE_NOT_FOUND, plateNumber, departmentRegion)));

        validatePlateAvailability(plate, plateNumber);
        validatePlateVehicleType(plate, vehicleType);
        return plate;
    }

    private LicensePlate resolvePersonalizedPlate(RegistrationDept department, String plateNumber,
                                                  VehicleType vehicleType) {
        if (plateNumber == null) {
            throw new BusinessException(PLATE_REQUIRED);
        }

        PlateFormatSupport.PlateParts plateParts = PlateFormatSupport.parse(plateNumber);
        if (plateParts == null) {
            throw new BusinessException(INVALID_PERSONALIZED_PLATE);
        }
        validatePlatePartsVehicleType(plateParts, vehicleType);

        if (!PlateFormatSupport.resolveAllowedRegionCodes(department.getRegion()).contains(plateParts.regionCode())) {
            throw new BusinessException(
                "Код региона в номере не соответствует выбранному отделению: " + department.getRegion());
        }

        Optional<LicensePlate> existingPlate =
            licensePlateRepository.findByPlateNumber(PlateFormatSupport.normalize(plateNumber));
        if (existingPlate.isPresent()) {
            LicensePlate plate = existingPlate.get();
            validatePlateAvailability(plate, plateNumber);
            validatePlateVehicleType(plate, vehicleType);
            return plate;
        }

        LicensePlate personalizedPlate = new LicensePlate();
        personalizedPlate.setPlateNumber(PlateFormatSupport.normalize(plateNumber));
        personalizedPlate.setSeries(plateParts.series());
        personalizedPlate.setPrice(BigDecimal.ZERO);
        personalizedPlate.setDepartment(department);
        return licensePlateRepository.save(personalizedPlate);
    }

    private void validatePlateAvailability(LicensePlate plate, String plateNumber) {
        if (!plate.isAvailable()) {
            throw new BusinessException(String.format(PLATE_NOT_AVAILABLE, plateNumber));
        }
    }

    private VehicleType resolveVehicleType(String vehicleType) {
        String normalized = normalizeValue(vehicleType);
        if (normalized == null) {
            throw new BusinessException(VEHICLE_TYPE_REQUIRED);
        }
        try {
            return VehicleType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(VEHICLE_TYPE_REQUIRED);
        }
    }

    private List<LicensePlate> filterByVehicleType(List<LicensePlate> plates, VehicleType vehicleType) {
        return plates.stream()
            .filter(plate -> isElectricPlate(plate) == (vehicleType == VehicleType.ELECTRIC))
            .toList();
    }

    private void validatePlateVehicleType(LicensePlate plate, VehicleType vehicleType) {
        if (isElectricPlate(plate) != (vehicleType == VehicleType.ELECTRIC)) {
            throw new BusinessException(PLATE_TYPE_MISMATCH);
        }
    }

    private void validatePlatePartsVehicleType(PlateFormatSupport.PlateParts plateParts, VehicleType vehicleType) {
        boolean electricPlate = PlateFormatSupport.isElectricNumberPart(plateParts.numberPart());
        if (electricPlate != (vehicleType == VehicleType.ELECTRIC)) {
            throw new BusinessException(PLATE_TYPE_MISMATCH);
        }
    }

    private boolean isElectricPlate(LicensePlate plate) {
        PlateFormatSupport.PlateParts parts = PlateFormatSupport.parse(plate.getPlateNumber());
        return parts != null && PlateFormatSupport.isElectricNumberPart(parts.numberPart());
    }

    private boolean isAvailableSelectionService(AdditionalService service) {
        String normalizedName = normalizeText(service.getName());
        return normalizedName.contains("DOSTUP")
            || normalizedName.contains("SVOBOD")
            || normalizedName.contains("VYBOR");
    }

    private boolean isPersonalizedPlateService(AdditionalService service) {
        String normalizedName = normalizeText(service.getName());
        return normalizedName.contains("PERSONALIZ")
            || normalizedName.contains("SVOI NOMER")
            || normalizedName.contains("IMENNOI")
            || normalizedName.contains("INDIVIDUAL");
    }

    private boolean isDuplicatePlateService(AdditionalService service) {
        String normalizedName = normalizeText(service.getName());
        return normalizedName.contains("DUBLIK")
            || normalizedName.contains("DUPLIKAT")
            || normalizedName.contains("DUPLICATE");
    }

    private boolean isKeepCurrentPlateService(AdditionalService service) {
        String normalizedName = normalizeText(service.getName());
        return normalizedName.contains("SOHRAN")
            || normalizedName.contains("SAVE")
            || normalizedName.contains("OSTAV")
            || normalizedName.contains("CURRENT PLATE");
    }

    private void validateRequestedExistingPlate(LicensePlate plate, String plateNumber, boolean duplicateService) {
        if (PlateFormatSupport.parse(plateNumber) == null) {
            throw new BusinessException(INVALID_PERSONALIZED_PLATE);
        }

        if (duplicateService && !plate.getApplications().stream().anyMatch(application -> application.getStatus() == ApplicationStatus.COMPLETED)) {
            throw new BusinessException("Для изготовления дубликатов номерной знак должен быть ранее выдан");
        }
    }

    private String normalizeText(String value) {
        return PlateFormatSupport.transliterateToAscii(value == null ? "" : value.trim());
    }

    private String normalizeValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = PlateFormatSupport.normalize(value);
        return normalized == null || normalized.isEmpty() ? null : normalized;
    }

    private String normalizeOptionalComment(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void validateVehicleVin(String vin) {
        String normalizedVin = normalizeValue(vin);
        if (normalizedVin != null && !PlateFormatSupport.VIN_PATTERN.matcher(normalizedVin).matches()) {
            throw new BusinessException(INVALID_VIN);
        }
    }

    private RegistrationDept getDepartment(Long departmentId) {
        if (departmentId == null) {
            return null;
        }

        return departmentRepository.findById(departmentId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Отделение с id " + departmentId + " не найдено"));
    }

    private ApplicationDto saveApplication(
        Applicant applicant,
        LicensePlate plate,
        ApplicationCreateDto dto,
        List<AdditionalService> services,
        RegistrationDept requestDepartment
    ) {
        Application application = buildApplication(applicant, plate, dto, services, requestDepartment);
        Application saved = applicationRepository.save(application);

        if (!services.isEmpty()) {
            saved.setAdditionalServices(services);
            applicationRepository.save(saved);
        }

        return applicationMapper.toDto(saved);
    }

    private Application buildApplication(
        Applicant applicant,
        LicensePlate plate,
        ApplicationCreateDto dto,
        List<AdditionalService> services,
        RegistrationDept requestDepartment
    ) {
        Application application = new Application();
        application.setApplicant(applicant);
        application.setLicensePlate(plate);
        application.setDepartment(requestDepartment != null ? requestDepartment : plate.getDepartment());
        application.setStatus(ApplicationStatus.PENDING);
        application.setSubmissionDate(LocalDateTime.now());
        application.setReservedUntil(LocalDateTime.now().plusHours(1));
        application.setVehicleVin(normalizeValue(dto.getVehicleVin()));
        application.setVehicleModel(dto.getVehicleModel());
        application.setVehicleYear(dto.getVehicleYear());
        application.setNotes(dto.getNotes());
        application.setPaymentAmount(calculateTotalAmount(plate, services));
        return application;
    }

    private BigDecimal calculateTotalAmount(LicensePlate plate, List<AdditionalService> services) {
        BigDecimal total = plate.getPrice() == null ? BigDecimal.ZERO : plate.getPrice();
        if (!services.isEmpty()) {
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

    private LicensePlate findPlateByNumber(String plateNumber) {
        return licensePlateRepository.findByPlateNumber(PlateFormatSupport.normalize(plateNumber))
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

    private enum PlateSelectionMode {
        RANDOM,
        AVAILABLE,
        PERSONALIZED
    }

    private enum VehicleType {
        STANDARD,
        ELECTRIC
    }
}
