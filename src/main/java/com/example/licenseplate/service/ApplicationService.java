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

    @Transactional(readOnly = true)
    public List<ApplicationDto> getAllApplications() {
        return applicationMapper.toDtoList(applicationRepository.findAll());
    }

    @Transactional(readOnly = true)
    public ApplicationDto getApplicationById(final Long id) {
        Application application = findApplicationOrThrow(id);
        return applicationMapper.toDto(application);
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
        Applicant applicant = findApplicantByPassport(createDto.getPassportNumber());
        LicensePlate plate = findPlateByNumber(createDto.getPlateNumber());

        validatePlateAvailability(plate);

        Application application = createBaseApplication(applicant, plate, createDto);
        application.setPaymentAmount(calculateTotalAmount(plate, createDto.getServiceIds()));

        if (createDto.getServiceIds() != null && !createDto.getServiceIds().isEmpty()) {
            List<AdditionalService> services = serviceRepository.findAllById(createDto.getServiceIds());
            application.setAdditionalServices(services);
        }

        Application savedApplication = applicationRepository.save(application);
        log.info("Created application {} for plate {}",
            savedApplication.getId(), plate.getPlateNumber());

        return applicationMapper.toDto(savedApplication);
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

        Application confirmed = applicationRepository.save(application);
        log.info("Confirmed application with id: {}", id);

        return applicationMapper.toDto(confirmed);
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

        Application completed = applicationRepository.save(application);
        log.info("Completed application with id: {}", id);

        return applicationMapper.toDto(completed);
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

        Application cancelled = applicationRepository.save(application);
        log.info("Cancelled application with id: {}", id);

        return applicationMapper.toDto(cancelled);
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

        LicensePlate plate = findPlateByNumber(createDto.getPlateNumber());
        Applicant applicant = findOrCreateApplicant(createDto.getPassportNumber());

        if (!plate.isAvailable()) {
            throw new IllegalStateException("Plate not available");
        }

        Application application = createBaseApplication(applicant, plate, createDto);
        Application saved = applicationRepository.save(application);
        log.info("Application saved with id: {}", saved.getId());

        if (createDto.getServiceIds() != null && !createDto.getServiceIds().isEmpty()) {
            List<AdditionalService> services = serviceRepository.findAllById(
                createDto.getServiceIds());

            if (services.size() != createDto.getServiceIds().size()) {
                throw new BusinessException(
                    "Some services not found - but application is already saved! " +
                        "Data is now inconsistent!");
            }

            saved.setAdditionalServices(services);
            applicationRepository.save(saved);
        }

        return applicationMapper.toDto(saved);
    }

    @Transactional
    public ApplicationDto createApplicationWithTransaction(
        final ApplicationCreateDto createDto) {
        log.info("=== Demonstrating WITH @Transactional ===");

        Applicant applicant = findOrCreateApplicant(createDto.getPassportNumber());
        LicensePlate plate = findPlateByNumber(createDto.getPlateNumber());

        if (!plate.isAvailable()) {
            throw new IllegalStateException("Plate not available");
        }

        Application application = createBaseApplication(applicant, plate, createDto);
        Application saved = applicationRepository.save(application);
        log.info("Application saved with id: {}", saved.getId());

        if (createDto.getServiceIds() != null && !createDto.getServiceIds().isEmpty()) {
            List<AdditionalService> services = serviceRepository.findAllById(
                createDto.getServiceIds());

            if (services.size() != createDto.getServiceIds().size()) {
                throw new BusinessException(
                    "Some services not found - transaction will rollback! " +
                        "Application will not be saved.");
            }

            saved.setAdditionalServices(services);
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

    private void validatePlateAvailability(LicensePlate plate) {
        if (!plate.isAvailable()) {
            throw new BusinessException(
                "Номер " + plate.getPlateNumber() + " недоступен");
        }
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

    private Application createBaseApplication(Applicant applicant, LicensePlate plate,
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
        return application;
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