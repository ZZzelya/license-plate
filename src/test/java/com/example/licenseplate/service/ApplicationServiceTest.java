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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private ApplicantRepository applicantRepository;
    @Mock
    private LicensePlateRepository licensePlateRepository;
    @Mock
    private ServiceRepository serviceRepository;
    @Mock
    private ApplicationMapper applicationMapper;
    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private ApplicationService applicationService;

    private Applicant applicant;
    private RegistrationDept department;
    private LicensePlate availablePlate;
    private Application application;
    private ApplicationDto applicationDto;

    @BeforeEach
    void setUp() {
        department = RegistrationDept.builder().id(1L).name("Dept").region("Minsk").build();
        applicant = Applicant.builder().id(1L).passportNumber("MP1234567").fullName("Ivan").build();
        availablePlate = LicensePlate.builder()
            .id(1L)
            .plateNumber("1234 AB-7")
            .price(BigDecimal.TEN)
            .series("AB")
            .department(department)
            .applications(new ArrayList<>())
            .build();
        application = Application.builder()
            .id(5L)
            .status(ApplicationStatus.PENDING)
            .licensePlate(availablePlate)
            .department(department)
            .applicant(applicant)
            .reservedUntil(LocalDateTime.now().plusHours(1))
            .build();
        applicationDto = ApplicationDto.builder().id(5L).licensePlateNumber("1234 AB-7").status("PENDING").build();
    }

    @Test
    void getAllApplicationsReturnsMappedList() {
        when(applicationRepository.findAll()).thenReturn(List.of(application));
        when(applicationMapper.toDtoList(List.of(application))).thenReturn(List.of(applicationDto));

        assertThat(applicationService.getAllApplications()).containsExactly(applicationDto);
    }

    @Test
    void getApplicationWithDetailsThrowsWhenMissing() {
        when(applicationRepository.findByIdWithDetails(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.getApplicationWithDetails(5L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getApplicationWithDetailsReturnsMappedDto() {
        when(applicationRepository.findByIdWithDetails(5L)).thenReturn(Optional.of(application));
        when(applicationMapper.toDto(application)).thenReturn(applicationDto);

        assertThat(applicationService.getApplicationWithDetails(5L)).isEqualTo(applicationDto);
    }

    @Test
    void getApplicationsByPassportThrowsWhenApplicantMissing() {
        when(applicationRepository.findByApplicantPassport("MP1234567")).thenReturn(List.of());
        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.getApplicationsByPassport("MP1234567"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getApplicationsByPassportReturnsEmptyListForExistingApplicant() {
        when(applicationRepository.findByApplicantPassport("MP1234567")).thenReturn(List.of());
        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(applicationMapper.toDtoList(List.of())).thenReturn(List.of());

        assertThat(applicationService.getApplicationsByPassport("MP1234567")).isEmpty();
    }

    @Test
    void createApplicationThrowsWhenApplicantMissing() {
        ApplicationCreateDto dto = ApplicationCreateDto.builder().passportNumber("MP1234567").build();
        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.createApplication(dto))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createApplicationThrowsForInvalidVin() {
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .vehicleVin("bad")
            .region("Minsk")
            .build();
        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));

        assertThatThrownBy(() -> applicationService.createApplication(dto))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void createApplicationThrowsWhenServicesMissing() {
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .serviceIds(List.of(1L, 2L))
            .region("Minsk")
            .build();
        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(serviceRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(new AdditionalService()));

        assertThatThrownBy(() -> applicationService.createApplication(dto))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void createApplicationUsesExplicitPlateNumberInRandomMode() {
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .plateNumber("1234 AB-7")
            .vehicleVin("WDB1240661C123456")
            .build();

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(availablePlate));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationMapper.toDto(any(Application.class))).thenReturn(applicationDto);

        assertThat(applicationService.createApplication(dto)).isEqualTo(applicationDto);
    }

    @Test
    void createApplicationThrowsWhenExplicitRandomPlateMissing() {
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .plateNumber("1234 AB-7")
            .vehicleVin("WDB1240661C123456")
            .build();

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.createApplication(dto))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createApplicationThrowsWhenRandomModeHasNoRegionOrDepartment() {
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .vehicleVin("WDB1240661C123456")
            .build();

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));

        assertThatThrownBy(() -> applicationService.createApplication(dto))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void createApplicationUsesRandomPlateByRegion() {
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .region("Minsk")
            .vehicleVin("WDB1240661C123456")
            .build();

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(licensePlateRepository.findAvailableByRegion("MINSK")).thenReturn(List.of(availablePlate));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationMapper.toDto(any(Application.class))).thenReturn(applicationDto);

        assertThat(applicationService.createApplication(dto)).isEqualTo(applicationDto);
    }

    @Test
    void createApplicationThrowsWhenRandomRegionHasNoAvailablePlates() {
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .region("Minsk")
            .vehicleVin("WDB1240661C123456")
            .build();

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(licensePlateRepository.findAvailableByRegion("MINSK")).thenReturn(List.of());

        assertThatThrownBy(() -> applicationService.createApplication(dto))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createApplicationUsesRandomPlateByDepartment() {
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .departmentId(1L)
            .vehicleVin("WDB1240661C123456")
            .build();

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(licensePlateRepository.findAvailableByDepartmentId(1L)).thenReturn(List.of(availablePlate));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationMapper.toDto(any(Application.class))).thenReturn(applicationDto);

        assertThat(applicationService.createApplication(dto)).isEqualTo(applicationDto);
    }

    @Test
    void createApplicationThrowsWhenRandomDepartmentHasNoAvailablePlates() {
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .departmentId(1L)
            .vehicleVin("WDB1240661C123456")
            .build();

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(licensePlateRepository.findAvailableByDepartmentId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> applicationService.createApplication(dto))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createApplicationThrowsWhenConflictingServicesChosen() {
        AdditionalService available = AdditionalService.builder().id(1L).name("vybor nomer").price(BigDecimal.ONE).build();
        AdditionalService personalized = AdditionalService.builder().id(2L).name("personaliz nomer").price(BigDecimal.ONE).build();
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .serviceIds(List.of(1L, 2L))
            .vehicleVin("WDB1240661C123456")
            .departmentId(1L)
            .plateNumber("1234 AB-7")
            .build();

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(serviceRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(available, personalized));

        assertThatThrownBy(() -> applicationService.createApplication(dto))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void createApplicationThrowsWhenAvailableSelectionHasNoPlateNumber() {
        AdditionalService available = AdditionalService.builder().id(1L).name("vybor nomer").price(BigDecimal.ONE).build();
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .serviceIds(List.of(1L))
            .departmentId(1L)
            .vehicleVin("WDB1240661C123456")
            .build();

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(serviceRepository.findAllById(List.of(1L))).thenReturn(List.of(available));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));

        assertThatThrownBy(() -> applicationService.createApplication(dto))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void createApplicationThrowsWhenAvailableSelectionHasNoRegionOrDepartment() {
        AdditionalService available = AdditionalService.builder().id(1L).name("vybor nomer").price(BigDecimal.ONE).build();
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .serviceIds(List.of(1L))
            .plateNumber("1234 AB-7")
            .vehicleVin("WDB1240661C123456")
            .build();

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(serviceRepository.findAllById(List.of(1L))).thenReturn(List.of(available));

        assertThatThrownBy(() -> applicationService.createApplication(dto))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void createApplicationThrowsWhenDepartmentNotFound() {
        AdditionalService available = AdditionalService.builder().id(1L).name("vybor nomer").price(BigDecimal.ONE).build();
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .serviceIds(List.of(1L))
            .departmentId(1L)
            .plateNumber("1234 AB-7")
            .vehicleVin("WDB1240661C123456")
            .build();

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(serviceRepository.findAllById(List.of(1L))).thenReturn(List.of(available));
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.createApplication(dto))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createApplicationUsesAvailablePlateByDepartment() {
        AdditionalService available = AdditionalService.builder().id(1L).name("vybor nomer").price(BigDecimal.ONE).build();
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .serviceIds(List.of(1L))
            .departmentId(1L)
            .plateNumber("1234 AB-7")
            .vehicleVin("WDB1240661C123456")
            .build();

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(serviceRepository.findAllById(List.of(1L))).thenReturn(List.of(available));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(licensePlateRepository.findAvailableByDepartmentId(1L)).thenReturn(List.of(availablePlate));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationMapper.toDto(any(Application.class))).thenReturn(applicationDto);

        assertThat(applicationService.createApplication(dto)).isEqualTo(applicationDto);
        verify(applicationRepository, times(2)).save(any(Application.class));
    }

    @Test
    void createApplicationRecognizesAvailableServiceByDostupAlias() {
        AdditionalService available = AdditionalService.builder().id(1L).name("dostupny nomer").price(BigDecimal.ONE).build();
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .serviceIds(List.of(1L))
            .departmentId(1L)
            .plateNumber("1234 AB-7")
            .vehicleVin("WDB1240661C123456")
            .build();

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(serviceRepository.findAllById(List.of(1L))).thenReturn(List.of(available));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(licensePlateRepository.findAvailableByDepartmentId(1L)).thenReturn(List.of(availablePlate));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationMapper.toDto(any(Application.class))).thenReturn(applicationDto);

        assertThat(applicationService.createApplication(dto)).isEqualTo(applicationDto);
    }

    @Test
    void createApplicationRecognizesAvailableServiceBySvobodAlias() {
        AdditionalService available = AdditionalService.builder().id(1L).name("svobodny vibor").price(BigDecimal.ONE).build();
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .serviceIds(List.of(1L))
            .departmentId(1L)
            .plateNumber("1234 AB-7")
            .vehicleVin("WDB1240661C123456")
            .build();

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(serviceRepository.findAllById(List.of(1L))).thenReturn(List.of(available));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(licensePlateRepository.findAvailableByDepartmentId(1L)).thenReturn(List.of(availablePlate));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationMapper.toDto(any(Application.class))).thenReturn(applicationDto);

        assertThat(applicationService.createApplication(dto)).isEqualTo(applicationDto);
    }

    @Test
    void createApplicationUsesAvailablePlateByRegion() {
        AdditionalService available = AdditionalService.builder().id(1L).name("vybor nomer").price(BigDecimal.ONE).build();
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .serviceIds(List.of(1L))
            .region("Minsk")
            .plateNumber("1234 AB-7")
            .vehicleVin("WDB1240661C123456")
            .build();

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(serviceRepository.findAllById(List.of(1L))).thenReturn(List.of(available));
        when(licensePlateRepository.findAvailableByRegionAndPlateNumber("MINSK", "1234 AB-7"))
            .thenReturn(Optional.of(availablePlate));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationMapper.toDto(any(Application.class))).thenReturn(applicationDto);

        assertThat(applicationService.createApplication(dto)).isEqualTo(applicationDto);
    }

    @Test
    void createApplicationThrowsWhenAvailableByRegionPlateMissing() {
        AdditionalService available = AdditionalService.builder().id(1L).name("vybor nomer").price(BigDecimal.ONE).build();
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .serviceIds(List.of(1L))
            .region("Minsk")
            .plateNumber("1234 AB-7")
            .vehicleVin("WDB1240661C123456")
            .build();

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(serviceRepository.findAllById(List.of(1L))).thenReturn(List.of(available));
        when(licensePlateRepository.findAvailableByRegionAndPlateNumber("MINSK", "1234 AB-7"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.createApplication(dto))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createApplicationThrowsWhenAvailableByRegionPlateUnavailable() {
        AdditionalService available = AdditionalService.builder().id(1L).name("vybor nomer").price(BigDecimal.ONE).build();
        LicensePlate reservedPlate = LicensePlate.builder()
            .id(1L)
            .plateNumber("1234 AB-7")
            .price(BigDecimal.TEN)
            .series("AB")
            .department(department)
            .applications(List.of(Application.builder()
                .status(ApplicationStatus.PENDING)
                .submissionDate(LocalDateTime.now())
                .build()))
            .build();
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .serviceIds(List.of(1L))
            .region("Minsk")
            .plateNumber("1234 AB-7")
            .vehicleVin("WDB1240661C123456")
            .build();

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(serviceRepository.findAllById(List.of(1L))).thenReturn(List.of(available));
        when(licensePlateRepository.findAvailableByRegionAndPlateNumber("MINSK", "1234 AB-7"))
            .thenReturn(Optional.of(reservedPlate));

        assertThatThrownBy(() -> applicationService.createApplication(dto))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void createApplicationThrowsWhenAvailableByRegionPlateInvalid() {
        AdditionalService available = AdditionalService.builder().id(1L).name("vybor nomer").price(BigDecimal.ONE).build();
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .serviceIds(List.of(1L))
            .region("Minsk")
            .plateNumber("bad")
            .vehicleVin("WDB1240661C123456")
            .build();

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(serviceRepository.findAllById(List.of(1L))).thenReturn(List.of(available));

        assertThatThrownBy(() -> applicationService.createApplication(dto))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void createApplicationThrowsWhenAvailableSelectionPlateMissingInDepartment() {
        AdditionalService available = AdditionalService.builder().id(1L).name("vybor nomer").price(BigDecimal.ONE).build();
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .serviceIds(List.of(1L))
            .departmentId(1L)
            .plateNumber("1234 AB-7")
            .vehicleVin("WDB1240661C123456")
            .build();

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(serviceRepository.findAllById(List.of(1L))).thenReturn(List.of(available));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(licensePlateRepository.findAvailableByDepartmentId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> applicationService.createApplication(dto))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createApplicationThrowsWhenAvailableSelectionPlateIsUnavailableInDepartment() {
        AdditionalService available = AdditionalService.builder().id(1L).name("vybor nomer").price(BigDecimal.ONE).build();
        LicensePlate reservedPlate = LicensePlate.builder()
            .id(2L)
            .plateNumber("1234 AB-7")
            .price(BigDecimal.TEN)
            .series("AB")
            .department(department)
            .applications(List.of(Application.builder()
                .status(ApplicationStatus.CONFIRMED)
                .submissionDate(LocalDateTime.now())
                .build()))
            .build();
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .serviceIds(List.of(1L))
            .departmentId(1L)
            .plateNumber("1234 AB-7")
            .vehicleVin("WDB1240661C123456")
            .build();

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(serviceRepository.findAllById(List.of(1L))).thenReturn(List.of(available));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(licensePlateRepository.findAvailableByDepartmentId(1L)).thenReturn(List.of(reservedPlate));

        assertThatThrownBy(() -> applicationService.createApplication(dto))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void createApplicationThrowsWhenPersonalizedSelectionHasNoDepartment() {
        AdditionalService personalized = AdditionalService.builder().id(1L).name("personaliz nomer").price(BigDecimal.ONE).build();
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .serviceIds(List.of(1L))
            .plateNumber("1234 AB-7")
            .vehicleVin("WDB1240661C123456")
            .build();

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(serviceRepository.findAllById(List.of(1L))).thenReturn(List.of(personalized));

        assertThatThrownBy(() -> applicationService.createApplication(dto))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void createApplicationThrowsWhenPersonalizedFormatInvalid() {
        AdditionalService personalized = AdditionalService.builder().id(1L).name("personaliz nomer").price(BigDecimal.ONE).build();
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .serviceIds(List.of(1L))
            .departmentId(1L)
            .plateNumber("bad")
            .vehicleVin("WDB1240661C123456")
            .build();

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(serviceRepository.findAllById(List.of(1L))).thenReturn(List.of(personalized));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));

        assertThatThrownBy(() -> applicationService.createApplication(dto))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void createApplicationThrowsWhenPersonalizedRegionMismatchesDepartment() {
        AdditionalService personalized = AdditionalService.builder().id(1L).name("personaliz nomer").price(BigDecimal.ONE).build();
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .serviceIds(List.of(1L))
            .departmentId(1L)
            .plateNumber("1234 AB-1")
            .vehicleVin("WDB1240661C123456")
            .build();

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(serviceRepository.findAllById(List.of(1L))).thenReturn(List.of(personalized));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));

        assertThatThrownBy(() -> applicationService.createApplication(dto))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void createApplicationRecognizesPersonalizedServiceBySvoiNomerAlias() {
        AdditionalService personalized = AdditionalService.builder().id(1L).name("svoi nomer").price(BigDecimal.ONE).build();
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .serviceIds(List.of(1L))
            .departmentId(1L)
            .plateNumber("1234 AB-7")
            .vehicleVin("WDB1240661C123456")
            .build();

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(serviceRepository.findAllById(List.of(1L))).thenReturn(List.of(personalized));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.empty());
        when(licensePlateRepository.save(any(LicensePlate.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationMapper.toDto(any(Application.class))).thenReturn(applicationDto);

        assertThat(applicationService.createApplication(dto)).isEqualTo(applicationDto);
    }

    @Test
    void createApplicationRecognizesPersonalizedServiceByImennoiAlias() {
        AdditionalService personalized = AdditionalService.builder().id(1L).name("imennoi").price(BigDecimal.ONE).build();
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .serviceIds(List.of(1L))
            .departmentId(1L)
            .plateNumber("1234 AB-7")
            .vehicleVin("WDB1240661C123456")
            .build();

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(serviceRepository.findAllById(List.of(1L))).thenReturn(List.of(personalized));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.empty());
        when(licensePlateRepository.save(any(LicensePlate.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationMapper.toDto(any(Application.class))).thenReturn(applicationDto);

        assertThat(applicationService.createApplication(dto)).isEqualTo(applicationDto);
    }

    @Test
    void createApplicationRecognizesPersonalizedServiceByIndividualAlias() {
        AdditionalService personalized = AdditionalService.builder().id(1L).name("individual").price(BigDecimal.ONE).build();
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .serviceIds(List.of(1L))
            .departmentId(1L)
            .plateNumber("1234 AB-7")
            .vehicleVin("WDB1240661C123456")
            .build();

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(serviceRepository.findAllById(List.of(1L))).thenReturn(List.of(personalized));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.empty());
        when(licensePlateRepository.save(any(LicensePlate.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationMapper.toDto(any(Application.class))).thenReturn(applicationDto);

        assertThat(applicationService.createApplication(dto)).isEqualTo(applicationDto);
    }

    @Test
    void createApplicationReusesExistingPersonalizedPlateAndReassignsDepartment() {
        AdditionalService personalized = AdditionalService.builder().id(1L).name("personaliz nomer").price(BigDecimal.ONE).build();
        RegistrationDept otherDepartment = RegistrationDept.builder().id(2L).region("Minsk").build();
        LicensePlate existing = LicensePlate.builder()
            .id(8L)
            .plateNumber("1234 AB-7")
            .price(BigDecimal.ZERO)
            .series("AB")
            .department(otherDepartment)
            .applications(new ArrayList<>())
            .build();
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .serviceIds(List.of(1L))
            .departmentId(1L)
            .plateNumber("1234 AB-7")
            .vehicleVin("WDB1240661C123456")
            .build();

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(serviceRepository.findAllById(List.of(1L))).thenReturn(List.of(personalized));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(existing));
        when(licensePlateRepository.save(existing)).thenReturn(existing);
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationMapper.toDto(any(Application.class))).thenReturn(applicationDto);

        assertThat(applicationService.createApplication(dto)).isEqualTo(applicationDto);
        assertThat(existing.getDepartment()).isEqualTo(department);
    }

    @Test
    void createApplicationReusesExistingPersonalizedPlateWhenDepartmentMissingOnPlate() {
        AdditionalService personalized = AdditionalService.builder().id(1L).name("personaliz nomer").price(BigDecimal.ONE).build();
        LicensePlate existing = LicensePlate.builder()
            .id(8L)
            .plateNumber("1234 AB-7")
            .price(BigDecimal.ZERO)
            .series("AB")
            .department(null)
            .applications(new ArrayList<>())
            .build();
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .serviceIds(List.of(1L))
            .departmentId(1L)
            .plateNumber("1234 AB-7")
            .vehicleVin("WDB1240661C123456")
            .build();

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(serviceRepository.findAllById(List.of(1L))).thenReturn(List.of(personalized));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(existing));
        when(licensePlateRepository.save(existing)).thenReturn(existing);
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationMapper.toDto(any(Application.class))).thenReturn(applicationDto);

        assertThat(applicationService.createApplication(dto)).isEqualTo(applicationDto);
        assertThat(existing.getDepartment()).isEqualTo(department);
    }

    @Test
    void createApplicationReusesExistingPersonalizedPlateWithoutReassign() {
        AdditionalService personalized = AdditionalService.builder().id(1L).name("personaliz nomer").price(BigDecimal.ONE).build();
        LicensePlate existing = LicensePlate.builder()
            .id(8L)
            .plateNumber("1234 AB-7")
            .price(BigDecimal.ZERO)
            .series("AB")
            .department(department)
            .applications(new ArrayList<>())
            .build();
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .serviceIds(List.of(1L))
            .departmentId(1L)
            .plateNumber("1234 AB-7")
            .vehicleVin("WDB1240661C123456")
            .build();

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(serviceRepository.findAllById(List.of(1L))).thenReturn(List.of(personalized));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(existing));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationMapper.toDto(any(Application.class))).thenReturn(applicationDto);

        assertThat(applicationService.createApplication(dto)).isEqualTo(applicationDto);
        verify(licensePlateRepository, never()).save(existing);
    }

    @Test
    void createApplicationThrowsWhenExistingPersonalizedPlateUnavailable() {
        AdditionalService personalized = AdditionalService.builder().id(1L).name("personaliz nomer").price(BigDecimal.ONE).build();
        LicensePlate existing = LicensePlate.builder()
            .id(8L)
            .plateNumber("1234 AB-7")
            .price(BigDecimal.ZERO)
            .series("AB")
            .department(department)
            .applications(List.of(Application.builder()
                .status(ApplicationStatus.PENDING)
                .submissionDate(LocalDateTime.now())
                .build()))
            .build();
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .serviceIds(List.of(1L))
            .departmentId(1L)
            .plateNumber("1234 AB-7")
            .vehicleVin("WDB1240661C123456")
            .build();

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(serviceRepository.findAllById(List.of(1L))).thenReturn(List.of(personalized));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> applicationService.createApplication(dto))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void createApplicationThrowsWhenExplicitRandomPlateIsUnavailable() {
        LicensePlate reservedPlate = LicensePlate.builder()
            .id(8L)
            .plateNumber("1234 AB-7")
            .price(BigDecimal.ZERO)
            .series("AB")
            .department(department)
            .applications(List.of(Application.builder()
                .status(ApplicationStatus.COMPLETED)
                .submissionDate(LocalDateTime.now())
                .build()))
            .build();
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .plateNumber("1234 AB-7")
            .vehicleVin("WDB1240661C123456")
            .build();

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(reservedPlate));

        assertThatThrownBy(() -> applicationService.createApplication(dto))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void createApplicationCreatesNewPersonalizedPlateWhenAbsent() {
        AdditionalService personalized = AdditionalService.builder().id(1L).name("personaliz nomer").price(BigDecimal.ONE).build();
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .serviceIds(List.of(1L))
            .departmentId(1L)
            .plateNumber("1234 AB-7")
            .vehicleVin("WDB1240661C123456")
            .notes("note")
            .vehicleModel("Tesla")
            .vehicleYear(2024)
            .build();

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(serviceRepository.findAllById(List.of(1L))).thenReturn(List.of(personalized));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.empty());
        when(licensePlateRepository.save(any(LicensePlate.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationMapper.toDto(any(Application.class))).thenReturn(applicationDto);

        assertThat(applicationService.createApplication(dto)).isEqualTo(applicationDto);
        verify(licensePlateRepository).save(any(LicensePlate.class));
        verify(applicationRepository, times(2)).save(any(Application.class));
    }

    @Test
    void createApplicationUsesZeroPlatePriceWhenPriceMissing() {
        AdditionalService available = AdditionalService.builder().id(1L).name("vybor nomer").price(BigDecimal.ONE).build();
        LicensePlate noPricePlate = LicensePlate.builder()
            .id(1L)
            .plateNumber("1234 AB-7")
            .price(null)
            .series("AB")
            .department(department)
            .applications(new ArrayList<>())
            .build();
        ApplicationCreateDto dto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .serviceIds(List.of(1L))
            .departmentId(1L)
            .plateNumber("1234 AB-7")
            .vehicleVin("WDB1240661C123456")
            .build();
        ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(serviceRepository.findAllById(List.of(1L))).thenReturn(List.of(available));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(licensePlateRepository.findAvailableByDepartmentId(1L)).thenReturn(List.of(noPricePlate));
        when(applicationRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationMapper.toDto(any(Application.class))).thenReturn(applicationDto);

        applicationService.createApplication(dto);

        assertThat(captor.getValue().getPaymentAmount()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void confirmApplicationThrowsForWrongStatus() {
        application.setStatus(ApplicationStatus.CANCELLED);
        when(applicationRepository.findById(5L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> applicationService.confirmApplication(5L))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void confirmApplicationExpiresOldReservation() {
        application.setReservedUntil(LocalDateTime.now().minusMinutes(1));
        when(applicationRepository.findById(5L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);

        assertThatThrownBy(() -> applicationService.confirmApplication(5L))
            .isInstanceOf(BusinessException.class);
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.EXPIRED);
    }

    @Test
    void confirmApplicationSetsConfirmedStatus() {
        when(applicationRepository.findById(5L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);
        when(applicationMapper.toDto(application)).thenReturn(applicationDto);

        assertThat(applicationService.confirmApplication(5L)).isEqualTo(applicationDto);
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.CONFIRMED);
        assertThat(application.getConfirmationDate()).isNotNull();
    }

    @Test
    void confirmApplicationThrowsWhenMissing() {
        when(applicationRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.confirmApplication(5L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void completeApplicationThrowsForWrongStatus() {
        application.setStatus(ApplicationStatus.PENDING);
        when(applicationRepository.findById(5L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> applicationService.completeApplication(5L))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void completeApplicationSetsDatesAndStatus() {
        application.setStatus(ApplicationStatus.CONFIRMED);
        when(applicationRepository.findById(5L)).thenReturn(Optional.of(application));
        when(licensePlateRepository.save(availablePlate)).thenReturn(availablePlate);
        when(applicationRepository.save(application)).thenReturn(application);
        when(applicationMapper.toDto(application)).thenReturn(applicationDto);

        assertThat(applicationService.completeApplication(5L)).isEqualTo(applicationDto);
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.COMPLETED);
        assertThat(availablePlate.getIssueDate()).isNotNull();
        assertThat(availablePlate.getExpiryDate()).isNotNull();
    }

    @Test
    void completeApplicationThrowsWhenMissing() {
        when(applicationRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.completeApplication(5L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cancelApplicationThrowsForCompletedOrCancelled() {
        application.setStatus(ApplicationStatus.COMPLETED);
        when(applicationRepository.findById(5L)).thenReturn(Optional.of(application));
        assertThatThrownBy(() -> applicationService.cancelApplication(5L))
            .isInstanceOf(BusinessException.class);

        application.setStatus(ApplicationStatus.CANCELLED);
        assertThatThrownBy(() -> applicationService.cancelApplication(5L))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void cancelApplicationSetsCancelledStatus() {
        application.setStatus(ApplicationStatus.PENDING);
        when(applicationRepository.findById(5L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);
        when(applicationMapper.toDto(application)).thenReturn(applicationDto);

        assertThat(applicationService.cancelApplication(5L)).isEqualTo(applicationDto);
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
    }

    @Test
    void cancelApplicationThrowsWhenMissing() {
        when(applicationRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.cancelApplication(5L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteApplicationDeletesFoundEntity() {
        when(applicationRepository.findById(5L)).thenReturn(Optional.of(application));

        applicationService.deleteApplication(5L);

        verify(applicationRepository).delete(application);
    }

    @Test
    void deleteApplicationThrowsWhenMissing() {
        when(applicationRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.deleteApplication(5L))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
