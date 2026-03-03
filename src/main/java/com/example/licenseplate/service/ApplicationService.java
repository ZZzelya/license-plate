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
import com.example.licenseplate.model.enums.PlateStatus;
import com.example.licenseplate.repository.ApplicantRepository;
import com.example.licenseplate.repository.ApplicationRepository;
import com.example.licenseplate.repository.LicensePlateRepository;
import com.example.licenseplate.repository.ServiceRepository;
import com.example.licenseplate.repository.DepartmentRepository;
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
    private final DepartmentRepository departmentRepository;
    private final ApplicationMapper applicationMapper;

    private static final String APPLICANT_NOT_FOUND = "Applicant not found with passport: ";
    private static final String PLATE_NOT_FOUND = "License plate not found: ";
    private static final String APPLICATION_NOT_FOUND = "Application not found with id: ";

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
            .orElseThrow(() -> new ResourceNotFoundException(APPLICATION_NOT_FOUND + id));
        return applicationMapper.toDto(application);
    }

    @Transactional(readOnly = true)
    public List<ApplicationDto> getApplicationsByPassport(final String passportNumber) {
        return applicationMapper.toDtoList(
            applicationRepository.findByApplicantPassport(passportNumber));
    }

    @Transactional
    public ApplicationDto createApplication(final ApplicationCreateDto createDto) {
        Applicant applicant = applicantRepository.findByPassportNumber(
                createDto.getPassportNumber())
            .orElseThrow(() -> new ResourceNotFoundException(
                APPLICANT_NOT_FOUND + createDto.getPassportNumber()));

        LicensePlate plate = licensePlateRepository.findByPlateNumber(
                createDto.getPlateNumber())
            .orElseThrow(() -> new ResourceNotFoundException(
                PLATE_NOT_FOUND + createDto.getPlateNumber()));

        if (plate.getStatus() != PlateStatus.AVAILABLE) {
            throw new BusinessException(
                "License plate " + createDto.getPlateNumber() + " is not available");
        }

        List<Application> activeApplications = applicationRepository
            .findActiveApplicationsByPlateId(plate.getId());
        if (!activeApplications.isEmpty()) {
            throw new BusinessException(
                "There is already an active application for this plate");
        }

        Application application = applicationMapper.toEntity(createDto);
        application.setApplicant(applicant);
        application.setLicensePlate(plate);
        application.setDepartment(plate.getDepartment());
        application.setStatus(ApplicationStatus.PENDING);
        application.setApplicationDate(LocalDateTime.now());
        application.setReservedUntil(LocalDateTime.now().plusHours(1));
        application.setPaymentAmount(plate.getPrice());

        if (createDto.getServiceIds() != null && !createDto.getServiceIds().isEmpty()) {
            List<AdditionalService> services = serviceRepository.findAllById(
                createDto.getServiceIds());
            application.setAdditionalServices(services);

            BigDecimal servicesTotal = services.stream()
                .map(AdditionalService::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            application.setPaymentAmount(plate.getPrice().add(servicesTotal));
        }

        plate.setStatus(PlateStatus.RESERVED);
        licensePlateRepository.save(plate);

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
            application.setStatus(ApplicationStatus.EXPIRED);
            applicationRepository.save(application);

            LicensePlate plate = application.getLicensePlate();
            plate.setStatus(PlateStatus.AVAILABLE);
            licensePlateRepository.save(plate);

            throw new BusinessException("Reservation time expired");
        }

        application.setStatus(ApplicationStatus.CONFIRMED);
        application.setPaymentDate(LocalDateTime.now());

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
        plate.setStatus(PlateStatus.ISSUED);
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

        if (application.getLicensePlate() != null) {
            LicensePlate plate = application.getLicensePlate();
            plate.setStatus(PlateStatus.AVAILABLE);
            licensePlateRepository.save(plate);
        }

        Application cancelled = applicationRepository.save(application);
        log.info("Cancelled application with id: {}", id);

        return applicationMapper.toDto(cancelled);
    }

    @Transactional
    public void deleteApplication(final Long id) {
        Application application = findApplicationOrThrow(id);

        if (application.getStatus() == ApplicationStatus.PENDING) {
            LicensePlate plate = application.getLicensePlate();
            plate.setStatus(PlateStatus.AVAILABLE);
            licensePlateRepository.save(plate);
        }

        applicationRepository.delete(application);
        log.info("Deleted application with id: {}", id);
    }

    @Transactional
    public ApplicationDto createApplicationWithoutTransaction(
        final ApplicationCreateDto createDto) {
        log.info("=== Demonstrating WITHOUT @Transactional ===");

        Applicant applicant = applicantRepository.findByPassportNumber(
                createDto.getPassportNumber())
            .orElseGet(() -> {
                Applicant newApplicant = new Applicant();
                newApplicant.setFullName("UNKNOWN");
                newApplicant.setPassportNumber(createDto.getPassportNumber());
                newApplicant.setIsActive(true);
                return applicantRepository.save(newApplicant);
            });

        LicensePlate plate = licensePlateRepository.findByPlateNumber(
                createDto.getPlateNumber())
            .orElseThrow(() -> new ResourceNotFoundException(
                PLATE_NOT_FOUND + createDto.getPlateNumber()));

        Application application = applicationMapper.toEntity(createDto);
        application.setApplicant(applicant);
        application.setLicensePlate(plate);
        application.setDepartment(plate.getDepartment());
        application.setStatus(ApplicationStatus.PENDING);
        application.setApplicationDate(LocalDateTime.now());

        Application saved = applicationRepository.save(application);
        log.info("Application saved with id: {}", saved.getId());

        if (createDto.getServiceIds() != null && !createDto.getServiceIds().isEmpty()) {
            List<AdditionalService> services = serviceRepository.findAllById(
                createDto.getServiceIds());

            if (services.size() != createDto.getServiceIds().size()) {
                throw new BusinessException(
                    "Some services not found - but application is already saved!");
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

        Applicant applicant = applicantRepository.findByPassportNumber(
                createDto.getPassportNumber())
            .orElseGet(() -> {
                Applicant newApplicant = new Applicant();
                newApplicant.setFullName("UNKNOWN");
                newApplicant.setPassportNumber(createDto.getPassportNumber());
                newApplicant.setIsActive(true);
                return applicantRepository.save(newApplicant);
            });

        LicensePlate plate = licensePlateRepository.findByPlateNumber(
                createDto.getPlateNumber())
            .orElseThrow(() -> new ResourceNotFoundException(
                PLATE_NOT_FOUND + createDto.getPlateNumber()));

        if (plate.getStatus() != PlateStatus.AVAILABLE) {
            throw new IllegalStateException("Table not available");
        }

        Application application = applicationMapper.toEntity(createDto);
        application.setApplicant(applicant);
        application.setLicensePlate(plate);
        application.setDepartment(plate.getDepartment());
        application.setStatus(ApplicationStatus.PENDING);
        application.setApplicationDate(LocalDateTime.now());

        Application saved = applicationRepository.save(application);
        log.info("Application saved with id: {}", saved.getId());

        if (createDto.getServiceIds() != null && !createDto.getServiceIds().isEmpty()) {
            List<AdditionalService> services = serviceRepository.findAllById(
                createDto.getServiceIds());

            if (services.size() != createDto.getServiceIds().size()) {
                throw new BusinessException(
                    "Some services not found - transaction will rollback!");
            }

            saved.setAdditionalServices(services);
        }

        plate.setStatus(PlateStatus.RESERVED);
        licensePlateRepository.save(plate);

        return applicationMapper.toDto(saved);
    }

    private Application findApplicationOrThrow(final Long id) {
        return applicationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(APPLICATION_NOT_FOUND + id));
    }
}