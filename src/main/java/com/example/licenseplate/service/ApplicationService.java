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
import com.example.licenseplate.repository.LicensePlateRepository;
import com.example.licenseplate.repository.ServiceRepository;
import com.example.licenseplate.service.mapper.ApplicationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicantRepository applicantRepository;
    private final LicensePlateRepository licensePlateRepository;
    private final ServiceRepository serviceRepository;
    private final ApplicationMapper applicationMapper;

    // ==================== READ OPERATIONS ====================

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
            .orElseThrow(() -> new ResourceNotFoundException("Заявление не найдено с id: " + id));
        return applicationMapper.toDto(application);
    }

    @Transactional(readOnly = true)
    public List<ApplicationDto> getApplicationsByPassport(final String passportNumber) {
        return applicationMapper.toDtoList(
            applicationRepository.findByApplicantPassport(passportNumber));
    }

    @Transactional
    public ApplicationDto createApplication(final ApplicationCreateDto createDto) {
        log.info("Creating application with transaction");
        return createApplicationInternal(createDto, true, true);
    }

    @Transactional
    public ApplicationDto confirmApplication(final Long id) {
        Application application = findApplicationOrThrow(id);

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new BusinessException(
                "Application is not in PENDING status: " + application.getStatus());
        }

        if (application.getReservedUntil().isBefore(LocalDateTime.now())) {
            expireApplication(application);
            throw new BusinessException("Reservation time expired");
        }

        application.setStatus(ApplicationStatus.CONFIRMED);
        application.setConfirmationDate(LocalDateTime.now());

        log.info("Confirmed application with id: {}", id);
        return applicationMapper.toDto(applicationRepository.save(application));
    }

    @Transactional
    public ApplicationDto completeApplication(final Long id) {
        Application application = findApplicationOrThrow(id);

        if (application.getStatus() != ApplicationStatus.CONFIRMED) {
            throw new BusinessException(
                "Application is not in CONFIRMED status: " + application.getStatus());
        }

        application.setStatus(ApplicationStatus.COMPLETED);

        LicensePlate plate = application.getLicensePlate();
        plate.setIssueDate(LocalDateTime.now());
        plate.setExpiryDate(LocalDateTime.now().plusYears(10));
        licensePlateRepository.save(plate);

        log.info("Completed application with id: {}", id);
        return applicationMapper.toDto(applicationRepository.save(application));
    }

    @Transactional
    public ApplicationDto cancelApplication(final Long id) {
        Application application = findApplicationOrThrow(id);

        if (application.getStatus() == ApplicationStatus.COMPLETED ||
            application.getStatus() == ApplicationStatus.CANCELLED) {
            throw new BusinessException(
                "Cannot cancel application in status: " + application.getStatus());
        }

        application.setStatus(ApplicationStatus.CANCELLED);

        log.info("Cancelled application with id: {}", id);
        return applicationMapper.toDto(applicationRepository.save(application));
    }

    @Transactional
    public void deleteApplication(final Long id) {
        Application application = findApplicationOrThrow(id);
        applicationRepository.delete(application);
        log.info("Deleted application with id: {}", id);
    }

    public ApplicationDto createApplicationWithoutTransaction(
        final ApplicationCreateDto createDto) {
        log.info("=== Demonstrating WITHOUT @Transactional ===");
        return createApplicationInternal(createDto, false, false);
    }

    @Transactional
    public ApplicationDto createApplicationWithTransaction(
        final ApplicationCreateDto createDto) {
        log.info("=== Demonstrating WITH @Transactional ===");
        return createApplicationInternal(createDto, true, false);
    }

    // ==================== PRIVATE METHODS ====================

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
                throw new IllegalStateException("Plate not available");
            }
        } else {
            if (!licensePlateRepository.isPlateAvailable(plate.getId())) {
                throw new IllegalStateException("Plate not available");
            }
        }

        // 4. Создаём заявление
        Application application = buildApplication(applicant, plate, createDto);
        Application saved = applicationRepository.save(application);
        log.info("Application saved with id: {}", saved.getId());

        // 5. Добавляем услуги
        if (createDto.getServiceIds() != null && !createDto.getServiceIds().isEmpty()) {
            List<AdditionalService> services = serviceRepository.findAllById(
                createDto.getServiceIds());

            // Проверяем, все ли услуги найдены
            if (services.size() != createDto.getServiceIds().size()) {
                String errorMsg = useTransactionalCheck
                    ? "Some services not found - transaction will rollback! Application will not be saved."
                    : "Some services not found - but application is already saved! Data is now inconsistent!";
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
                "Заявитель не найден: " + passportNumber));
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
                "Номер не найден: " + plateNumber));
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
        log.info("Application {} expired", application.getId());
    }

    private Application findApplicationOrThrow(final Long id) {
        return applicationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Заявление не найдено с id: " + id));
    }
}