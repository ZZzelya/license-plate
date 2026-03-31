package com.example.licenseplate.service;

import com.example.licenseplate.dto.request.ApplicationCreateDto;
import com.example.licenseplate.dto.request.BulkApplicationCreateDto;
import com.example.licenseplate.dto.response.ApplicationDto;
import com.example.licenseplate.dto.response.BulkApplicationResult;
import com.example.licenseplate.exception.BusinessException;
import com.example.licenseplate.exception.ResourceNotFoundException;
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
import com.example.licenseplate.cache.ApplicationCacheService;
import com.example.licenseplate.mapper.ApplicationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicationService Tests")
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
    private ApplicationCacheService cacheService;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private ApplicationService applicationService;

    private Applicant testApplicant;
    private LicensePlate testPlate;
    private RegistrationDept testDept;
    private Application testApplication;
    private ApplicationDto testApplicationDto;
    private ApplicationCreateDto testCreateDto;

    @BeforeEach
    void setUp() {
        testDept = RegistrationDept.builder()
            .id(1L)
            .name("МРЭО ГАИ")
            .region("MINSK")
            .build();

        testApplicant = Applicant.builder()
            .id(1L)
            .fullName("Иванов Иван")
            .passportNumber("MP1234567")
            .applications(new ArrayList<>())
            .build();

        testPlate = LicensePlate.builder()
            .id(1L)
            .plateNumber("1234 AB-7")
            .price(BigDecimal.valueOf(100))
            .department(testDept)
            .applications(new ArrayList<>())
            .build();

        testApplication = Application.builder()
            .id(1L)
            .status(ApplicationStatus.PENDING)
            .submissionDate(LocalDateTime.now())
            .reservedUntil(LocalDateTime.now().plusHours(1))
            .applicant(testApplicant)
            .licensePlate(testPlate)
            .department(testDept)
            .paymentAmount(BigDecimal.valueOf(100))
            .additionalServices(new ArrayList<>())
            .build();

        testApplicationDto = ApplicationDto.builder()
            .id(1L)
            .status("PENDING")
            .applicantId(1L)
            .applicantName("Иванов Иван")
            .licensePlateNumber("1234 AB-7")
            .departmentId(1L)
            .paymentAmount(BigDecimal.valueOf(100))
            .build();

        testCreateDto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .plateNumber("1234 AB-7")
            .vehicleVin("VIN123")
            .vehicleModel("Toyota")
            .vehicleYear(2020)
            .notes("Test notes")
            .serviceIds(List.of(1L, 2L))
            .build();
    }

    @Nested
    @DisplayName("Get All Applications Tests")
    class GetAllApplicationsTests {

        @Test
        void shouldReturnListOfApplications() {
            List<Application> applications = List.of(testApplication);
            List<ApplicationDto> expectedDtos = List.of(testApplicationDto);

            when(applicationRepository.findAll()).thenReturn(applications);
            when(applicationMapper.toDtoList(applications)).thenReturn(expectedDtos);

            List<ApplicationDto> result = applicationService.getAllApplications();

            assertThat(result).isEqualTo(expectedDtos);
        }

        @Test
        void shouldReturnEmptyListWhenNoApplications() {
            when(applicationRepository.findAll()).thenReturn(Collections.emptyList());
            when(applicationMapper.toDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

            List<ApplicationDto> result = applicationService.getAllApplications();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Application By ID Tests")
    class GetApplicationByIdTests {

        @Test
        void shouldReturnApplicationWhenIdExists() {
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
            when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

            ApplicationDto result = applicationService.getApplicationById(1L);

            assertThat(result).isEqualTo(testApplicationDto);
        }

        @Test
        void shouldThrowNotFoundExceptionWhenIdNotFound() {
            when(applicationRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> applicationService.getApplicationById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get Application With Details Tests")
    class GetApplicationWithDetailsTests {

        @Test
        void shouldReturnApplicationWithDetails() {
            when(applicationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(testApplication));
            when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

            ApplicationDto result = applicationService.getApplicationWithDetails(1L);

            assertThat(result).isEqualTo(testApplicationDto);
        }

        @Test
        void shouldThrowNotFoundExceptionWhenIdNotFound() {
            when(applicationRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> applicationService.getApplicationWithDetails(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get Applications By Passport Tests")
    class GetApplicationsByPassportTests {

        @Test
        void shouldReturnApplicationsWhenPassportExists() {
            List<Application> applications = List.of(testApplication);
            List<ApplicationDto> expectedDtos = List.of(testApplicationDto);

            when(applicationRepository.findByApplicantPassport("MP1234567")).thenReturn(applications);
            when(applicationMapper.toDtoList(applications)).thenReturn(expectedDtos);

            List<ApplicationDto> result = applicationService.getApplicationsByPassport("MP1234567");

            assertThat(result).isEqualTo(expectedDtos);
        }

        @Test
        void shouldThrowNotFoundExceptionWhenPassportNotFound() {
            when(applicationRepository.findByApplicantPassport("NOT_EXIST")).thenReturn(Collections.emptyList());
            when(applicantRepository.findByPassportNumber("NOT_EXIST")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> applicationService.getApplicationsByPassport("NOT_EXIST"))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void shouldReturnEmptyListWhenNoApplications() {
            when(applicationRepository.findByApplicantPassport("MP1234567")).thenReturn(Collections.emptyList());
            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(applicationMapper.toDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

            List<ApplicationDto> result = applicationService.getApplicationsByPassport("MP1234567");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Applications By Status And Region Tests")
    class GetApplicationsByStatusAndRegionTests {

        @Test
        void shouldReturnApplicationsByStatusAndRegion() {
            List<Application> applications = List.of(testApplication);
            List<ApplicationDto> expectedDtos = List.of(testApplicationDto);

            when(departmentRepository.existsByRegion("MINSK")).thenReturn(true);
            when(applicationRepository.findByStatusAndDepartmentRegion(ApplicationStatus.PENDING, "MINSK"))
                .thenReturn(applications);
            when(applicationMapper.toDtoList(applications)).thenReturn(expectedDtos);

            List<ApplicationDto> result = applicationService.getApplicationsByStatusAndRegion(
                ApplicationStatus.PENDING, "MINSK");

            assertThat(result).isEqualTo(expectedDtos);
        }

        @Test
        void shouldThrowNotFoundExceptionWhenRegionNotFound() {
            when(departmentRepository.existsByRegion("UNKNOWN")).thenReturn(false);

            assertThatThrownBy(() -> applicationService.getApplicationsByStatusAndRegion(
                ApplicationStatus.PENDING, "UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void shouldThrowNotFoundExceptionWhenNoApplications() {
            when(departmentRepository.existsByRegion("MINSK")).thenReturn(true);
            when(applicationRepository.findByStatusAndDepartmentRegion(ApplicationStatus.PENDING, "MINSK"))
                .thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> applicationService.getApplicationsByStatusAndRegion(
                ApplicationStatus.PENDING, "MINSK"))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get Applications By Status And Region Cached Tests")
    class GetApplicationsByStatusAndRegionCachedTests {

        @Test
        void shouldReturnCachedResult() {
            List<ApplicationDto> cachedResult = List.of(testApplicationDto);

            when(cacheService.get("PENDING", "MINSK")).thenReturn(cachedResult);

            List<ApplicationDto> result = applicationService.getApplicationsByStatusAndRegionCached(
                ApplicationStatus.PENDING, "MINSK");

            assertThat(result).isEqualTo(cachedResult);
        }

        @Test
        void shouldFetchFromDbAndCacheWhenCacheMiss() {
            List<Application> applications = List.of(testApplication);
            List<ApplicationDto> expectedDtos = List.of(testApplicationDto);

            when(cacheService.get("PENDING", "MINSK")).thenReturn(null);
            when(applicationRepository.findByStatusAndDepartmentRegion(ApplicationStatus.PENDING, "MINSK"))
                .thenReturn(applications);
            when(applicationMapper.toDtoList(applications)).thenReturn(expectedDtos);

            List<ApplicationDto> result = applicationService.getApplicationsByStatusAndRegionCached(
                ApplicationStatus.PENDING, "MINSK");

            assertThat(result).isEqualTo(expectedDtos);
            verify(cacheService).put("PENDING", "MINSK", expectedDtos);
        }

        @Test
        void shouldInvalidateCacheWhenEmptyResult() {
            when(cacheService.get("PENDING", "MINSK")).thenReturn(Collections.emptyList());

            List<ApplicationDto> result = applicationService.getApplicationsByStatusAndRegionCached(
                ApplicationStatus.PENDING, "MINSK");

            assertThat(result).isEmpty();
            verify(cacheService).invalidate();
        }
    }

    @Nested
    @DisplayName("Get Applications By Passport Paginated Tests")
    class GetApplicationsByPassportPaginatedTests {

        @Test
        void shouldReturnPaginatedResults() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Application> applicationPage = new PageImpl<>(List.of(testApplication), pageable, 1);

            when(applicationRepository.findByApplicantPassport("MP1234567", pageable))
                .thenReturn(applicationPage);
            when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

            Page<ApplicationDto> result = applicationService.getApplicationsByPassportPaginated("MP1234567", pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        void shouldReturnEmptyPageWhenNoResults() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Application> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            when(applicationRepository.findByApplicantPassport("MP1234567", pageable))
                .thenReturn(emptyPage);

            Page<ApplicationDto> result = applicationService.getApplicationsByPassportPaginated("MP1234567", pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Create Application Tests")
    class CreateApplicationTests {

        @Test
        void shouldCreateApplicationSuccessfully() {
            AdditionalService service1 = AdditionalService.builder().id(1L).price(BigDecimal.valueOf(50)).build();
            AdditionalService service2 = AdditionalService.builder().id(2L).price(BigDecimal.valueOf(30)).build();

            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
            when(serviceRepository.findAllById(anyList())).thenReturn(List.of(service1, service2));
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

            ApplicationDto result = applicationService.createApplication(testCreateDto);

            assertThat(result).isEqualTo(testApplicationDto);
            verify(cacheService).invalidate();
        }

        @Test
        void shouldCreateApplicationWithoutServices() {
            ApplicationCreateDto dtoWithoutServices = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(null)
                .build();

            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

            ApplicationDto result = applicationService.createApplication(dtoWithoutServices);

            assertThat(result).isEqualTo(testApplicationDto);
            verify(serviceRepository, never()).findAllById(any());
        }
    }

    @Nested
    @DisplayName("Create Application Internal Tests")
    class CreateApplicationInternalTests {

        @Test
        void shouldCreateWithExistingApplicant() {
            ApplicationCreateDto dtoWithoutServices = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(null)
                .build();

            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

            ApplicationDto result = applicationService.createApplication(dtoWithoutServices);

            assertThat(result).isEqualTo(testApplicationDto);
            verify(applicantRepository, never()).save(any());
        }

        @Test
        void shouldCreateNewApplicantWhenNotExists() {
            ApplicationCreateDto dto = ApplicationCreateDto.builder()
                .passportNumber("NEW123")
                .plateNumber("1234 AB-7")
                .serviceIds(null)
                .build();

            Applicant newApplicant = Applicant.builder()
                .id(2L)
                .fullName("UNKNOWN")
                .passportNumber("NEW123")
                .build();

            when(applicantRepository.findByPassportNumber("NEW123")).thenReturn(Optional.empty());
            when(applicantRepository.save(any(Applicant.class))).thenReturn(newApplicant);
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
            when(licensePlateRepository.isPlateAvailable(1L)).thenReturn(true);
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

            ApplicationDto result = applicationService.createApplicationWithoutTransaction(dto);

            assertThat(result).isEqualTo(testApplicationDto);
            verify(applicantRepository).save(any(Applicant.class));
        }

        @Test
        void shouldThrowExceptionWhenPlateNotAvailable() {
            ApplicationCreateDto dtoWithoutServices = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(null)
                .build();

            Application activeApplication = Application.builder()
                .status(ApplicationStatus.PENDING)
                .submissionDate(LocalDateTime.now())
                .reservedUntil(LocalDateTime.now().plusHours(1))
                .build();
            testPlate.getApplications().add(activeApplication);

            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));

            assertThatThrownBy(() -> applicationService.createApplication(dtoWithoutServices))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("недоступен");
        }

        @Test
        void shouldThrowExceptionWhenServicesNotFound() {
            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
            when(serviceRepository.findAllById(anyList())).thenReturn(List.of(AdditionalService.builder().id(1L).build()));

            assertThatThrownBy(() -> applicationService.createApplication(testCreateDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("transaction will rollback");
        }
    }

    @Nested
    @DisplayName("Confirm Application Tests")
    class ConfirmApplicationTests {

        @Test
        void shouldConfirmApplicationSuccessfully() {
            testApplication.setStatus(ApplicationStatus.PENDING);
            testApplication.setReservedUntil(LocalDateTime.now().plusHours(1));

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
            when(applicationRepository.save(testApplication)).thenReturn(testApplication);
            when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

            ApplicationDto result = applicationService.confirmApplication(1L);

            assertThat(result).isEqualTo(testApplicationDto);
            assertThat(testApplication.getStatus()).isEqualTo(ApplicationStatus.CONFIRMED);
            verify(cacheService).invalidate();
        }

        @Test
        void shouldThrowExceptionWhenNotPending() {
            testApplication.setStatus(ApplicationStatus.CONFIRMED);

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));

            assertThatThrownBy(() -> applicationService.confirmApplication(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("не в статусе PENDING");
        }

        @Test
        void shouldThrowExceptionWhenReservationExpired() {
            testApplication.setStatus(ApplicationStatus.PENDING);
            testApplication.setReservedUntil(LocalDateTime.now().minusHours(1));

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
            when(applicationRepository.save(testApplication)).thenReturn(testApplication);

            assertThatThrownBy(() -> applicationService.confirmApplication(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Время бронирования истекло");
        }
    }

    @Nested
    @DisplayName("Complete Application Tests")
    class CompleteApplicationTests {

        @Test
        void shouldCompleteApplicationSuccessfully() {
            testApplication.setStatus(ApplicationStatus.CONFIRMED);

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
            when(licensePlateRepository.save(testPlate)).thenReturn(testPlate);
            when(applicationRepository.save(testApplication)).thenReturn(testApplication);
            when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

            ApplicationDto result = applicationService.completeApplication(1L);

            assertThat(result).isEqualTo(testApplicationDto);
            assertThat(testApplication.getStatus()).isEqualTo(ApplicationStatus.COMPLETED);
            verify(cacheService).invalidate();
        }

        @Test
        void shouldThrowExceptionWhenNotConfirmed() {
            testApplication.setStatus(ApplicationStatus.PENDING);

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));

            assertThatThrownBy(() -> applicationService.completeApplication(1L))
                .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("Cancel Application Tests")
    class CancelApplicationTests {

        @Test
        void shouldCancelApplicationSuccessfully() {
            testApplication.setStatus(ApplicationStatus.PENDING);

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
            when(applicationRepository.save(testApplication)).thenReturn(testApplication);
            when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

            ApplicationDto result = applicationService.cancelApplication(1L);

            assertThat(result).isEqualTo(testApplicationDto);
            assertThat(testApplication.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
            verify(cacheService).invalidate();
        }

        @Test
        void shouldThrowExceptionWhenAlreadyCompleted() {
            testApplication.setStatus(ApplicationStatus.COMPLETED);

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));

            assertThatThrownBy(() -> applicationService.cancelApplication(1L))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        void shouldThrowExceptionWhenAlreadyCancelled() {
            testApplication.setStatus(ApplicationStatus.CANCELLED);

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));

            assertThatThrownBy(() -> applicationService.cancelApplication(1L))
                .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("Delete Application Tests")
    class DeleteApplicationTests {

        @Test
        void shouldDeleteApplicationSuccessfully() {
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
            doNothing().when(applicationRepository).delete(testApplication);

            applicationService.deleteApplication(1L);

            verify(applicationRepository).delete(testApplication);
            verify(cacheService).invalidate();
        }

        @Test
        void shouldThrowNotFoundExceptionWhenIdNotFound() {
            when(applicationRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> applicationService.deleteApplication(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Bulk Operations Tests")
    class BulkOperationsTests {

        @Test
        void shouldCreateBulkApplicationsWithTransactionSuccessfully() {
            ApplicationCreateDto app1 = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(null)
                .build();
            ApplicationCreateDto app2 = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("5678 CD-9")
                .serviceIds(null)
                .build();

            BulkApplicationCreateDto bulkDto = BulkApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .applications(List.of(app1, app2))
                .build();

            LicensePlate plate2 = LicensePlate.builder()
                .id(2L)
                .plateNumber("5678 CD-9")
                .price(BigDecimal.valueOf(150))
                .department(testDept)
                .applications(new ArrayList<>())
                .build();

            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
            when(licensePlateRepository.findByPlateNumber("5678 CD-9")).thenReturn(Optional.of(plate2));
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(any(Application.class))).thenReturn(testApplicationDto);

            BulkApplicationResult result = applicationService.createBulkApplicationsWithTransaction(bulkDto);

            assertThat(result.getTotalRequested()).isEqualTo(2);
            assertThat(result.getSuccessful()).isEqualTo(2);
        }

        @Test
        void shouldThrowExceptionInBulkWithTransactionWhenOneFails() {
            ApplicationCreateDto app1 = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(null)
                .build();
            ApplicationCreateDto app2 = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("NOT_EXIST")
                .serviceIds(null)
                .build();

            BulkApplicationCreateDto bulkDto = BulkApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .applications(List.of(app1, app2))
                .build();

            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
            when(licensePlateRepository.findByPlateNumber("NOT_EXIST")).thenReturn(Optional.empty());
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(any(Application.class))).thenReturn(testApplicationDto);

            assertThatThrownBy(() -> applicationService.createBulkApplicationsWithTransaction(bulkDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Bulk application failed");
        }

        @Test
        void shouldCreateBulkApplicationsWithoutTransactionPartialSuccess() {
            ApplicationCreateDto app1 = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(null)
                .build();
            ApplicationCreateDto app2 = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("NOT_EXIST")
                .serviceIds(null)
                .build();

            BulkApplicationCreateDto bulkDto = BulkApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .applications(List.of(app1, app2))
                .build();

            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
            when(licensePlateRepository.findByPlateNumber("NOT_EXIST")).thenReturn(Optional.empty());
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(any(Application.class))).thenReturn(testApplicationDto);

            BulkApplicationResult result = applicationService.createBulkApplicationsWithoutTransaction(bulkDto);

            assertThat(result.getTotalRequested()).isEqualTo(2);
            assertThat(result.getSuccessful()).isEqualTo(1);
            assertThat(result.getFailed()).isEqualTo(1);

            verify(licensePlateRepository, never()).isPlateAvailable(anyLong());
        }

        @Test
        void shouldThrowExceptionWhenDuplicatePlateNumbers() {
            ApplicationCreateDto app1 = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .build();
            ApplicationCreateDto app2 = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .build();

            BulkApplicationCreateDto bulkDto = BulkApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .applications(List.of(app1, app2))
                .build();

            assertThatThrownBy(() -> applicationService.createBulkApplicationsWithTransaction(bulkDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Duplicate plate numbers");
        }

        @Test
        void shouldThrowExceptionWhenPlateNotAvailable() {
            ApplicationCreateDto app1 = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(null)
                .build();

            BulkApplicationCreateDto bulkDto = BulkApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .applications(List.of(app1))
                .build();

            Application activeApplication = Application.builder()
                .status(ApplicationStatus.PENDING)
                .submissionDate(LocalDateTime.now())
                .reservedUntil(LocalDateTime.now().plusHours(1))
                .build();
            testPlate.getApplications().add(activeApplication);

            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));

            assertThatThrownBy(() -> applicationService.createBulkApplicationsWithTransaction(bulkDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("недоступен");
        }
    }

    @Nested
    @DisplayName("Cache Management Tests")
    class CacheManagementTests {

        @Test
        void shouldInvalidateEntireCache() {
            applicationService.invalidateCache();
            verify(cacheService).invalidate();
        }

        @Test
        void shouldInvalidateCacheByRegion() {
            applicationService.invalidateCacheByRegion("MINSK");
            verify(cacheService).invalidateByRegion("MINSK");
        }

        @Test
        void shouldInvalidateCacheByStatus() {
            applicationService.invalidateCacheByStatus("PENDING");
            verify(cacheService).invalidateByStatus("PENDING");
        }
    }

    @Nested
    @DisplayName("Native Query Tests")
    class NativeQueryTests {

        @Test
        void shouldReturnApplicationsByStatusAndRegionNative() {
            List<Application> applications = List.of(testApplication);
            List<ApplicationDto> expectedDtos = List.of(testApplicationDto);

            when(departmentRepository.existsByRegion("MINSK")).thenReturn(true);
            when(applicationRepository.findByStatusAndDepartmentRegionNative("PENDING", "MINSK"))
                .thenReturn(applications);
            when(applicationMapper.toDtoList(applications)).thenReturn(expectedDtos);

            List<ApplicationDto> result = applicationService.getApplicationsByStatusAndRegionNative(
                ApplicationStatus.PENDING, "MINSK");

            assertThat(result).isEqualTo(expectedDtos);
        }

        @Test
        void shouldThrowExceptionWhenRegionNotFoundNative() {
            when(departmentRepository.existsByRegion("UNKNOWN")).thenReturn(false);

            assertThatThrownBy(() -> applicationService.getApplicationsByStatusAndRegionNative(
                ApplicationStatus.PENDING, "UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void shouldThrowExceptionWhenNoApplicationsNative() {
            when(departmentRepository.existsByRegion("MINSK")).thenReturn(true);
            when(applicationRepository.findByStatusAndDepartmentRegionNative("PENDING", "MINSK"))
                .thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> applicationService.getApplicationsByStatusAndRegionNative(
                ApplicationStatus.PENDING, "MINSK"))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Create Application Without/With Transaction Tests")
    class CreateApplicationTransactionTests {

        @Test
        void shouldCreateApplicationWithoutTransaction() {
            ApplicationCreateDto dto = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(null)
                .build();

            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
            when(licensePlateRepository.isPlateAvailable(1L)).thenReturn(true);
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

            ApplicationDto result = applicationService.createApplicationWithoutTransaction(dto);

            assertThat(result).isEqualTo(testApplicationDto);
        }

        @Test
        void shouldCreateApplicationWithTransaction() {
            ApplicationCreateDto dto = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(null)
                .build();

            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

            ApplicationDto result = applicationService.createApplicationWithTransaction(dto);

            assertThat(result).isEqualTo(testApplicationDto);
        }

        @Test
        void shouldThrowExceptionWhenServicesNotFoundInWithoutTransaction() {
            ApplicationCreateDto dto = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(List.of(1L, 2L))
                .build();

            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
            when(licensePlateRepository.isPlateAvailable(1L)).thenReturn(true);
            when(serviceRepository.findAllById(anyList())).thenReturn(List.of(AdditionalService.builder().id(1L).build()));

            assertThatThrownBy(() -> applicationService.createApplicationWithoutTransaction(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("but application is already saved");
        }

        @Test
        void shouldThrowExceptionWhenServicesNotFoundInWithTransaction() {
            ApplicationCreateDto dto = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(List.of(1L, 2L))  // с сервисами
                .build();

            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
            when(serviceRepository.findAllById(anyList())).thenReturn(List.of(AdditionalService.builder().id(1L).build()));

            assertThatThrownBy(() -> applicationService.createApplicationWithTransaction(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("transaction will rollback");
        }
    }

    @Nested
    @DisplayName("Find Methods Exception Tests")
    class FindMethodsExceptionTests {

        @Test
        void shouldThrowExceptionWhenPlateNotFound() {
            ApplicationCreateDto dto = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("NOT_EXIST")
                .build();

            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("NOT_EXIST")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> applicationService.createApplication(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("не найден");
        }

        @Test
        void shouldThrowExceptionWhenApplicantNotFound() {
            ApplicationCreateDto dto = ApplicationCreateDto.builder()
                .passportNumber("NOT_EXIST")
                .plateNumber("1234 AB-7")
                .build();

            when(applicantRepository.findByPassportNumber("NOT_EXIST")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> applicationService.createApplication(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("не найден");
        }
    }

    @Nested
    @DisplayName("Expire Application Test")
    class ExpireApplicationTest {

        @Test
        void shouldExpireApplicationWhenReservationExpired() {
            testApplication.setStatus(ApplicationStatus.PENDING);
            testApplication.setReservedUntil(LocalDateTime.now().minusHours(1));

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
            when(applicationRepository.save(testApplication)).thenReturn(testApplication);

            assertThatThrownBy(() -> applicationService.confirmApplication(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Время бронирования истекло");
        }
    }

    @Nested
    @DisplayName("Calculate Total Amount Tests")
    class CalculateTotalAmountTests {

        @Test
        void shouldCalculateTotalAmountWithServices() {
            AdditionalService service1 = AdditionalService.builder().id(1L).price(BigDecimal.valueOf(50)).build();
            AdditionalService service2 = AdditionalService.builder().id(2L).price(BigDecimal.valueOf(30)).build();

            ArgumentCaptor<Application> applicationCaptor = ArgumentCaptor.forClass(Application.class);

            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
            when(serviceRepository.findAllById(anyList())).thenReturn(List.of(service1, service2));
            when(applicationRepository.save(applicationCaptor.capture())).thenReturn(testApplication);
            when(applicationMapper.toDto(any(Application.class))).thenReturn(testApplicationDto);

            applicationService.createApplication(testCreateDto);

            verify(applicationRepository, times(2)).save(any(Application.class));

            List<Application> savedApplications = applicationCaptor.getAllValues();
            assertThat(savedApplications).hasSize(2);
            assertThat(savedApplications.getFirst().getPaymentAmount()).isEqualTo(BigDecimal.valueOf(180));
        }

        @Test
        void shouldCalculateTotalAmountWithoutServices() {
            ApplicationCreateDto dtoWithoutServices = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(null)
                .build();

            ArgumentCaptor<Application> applicationCaptor = ArgumentCaptor.forClass(Application.class);

            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
            when(applicationRepository.save(applicationCaptor.capture())).thenReturn(testApplication);
            when(applicationMapper.toDto(any(Application.class))).thenReturn(testApplicationDto);

            applicationService.createApplication(dtoWithoutServices);

            verify(applicationRepository, times(1)).save(any(Application.class));

            Application savedApplication = applicationCaptor.getValue();
            assertThat(savedApplication.getPaymentAmount()).isEqualTo(BigDecimal.valueOf(100));

            verify(serviceRepository, never()).findAllById(any());
        }
    }

    @Nested
    @DisplayName("Find Or Create Applicant Tests")
    class FindOrCreateApplicantTests {

        @Test
        void shouldFindExistingApplicant() {
            ApplicationCreateDto dtoWithoutServices = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .vehicleVin("VIN123")
                .vehicleModel("Toyota")
                .vehicleYear(2020)
                .notes("Test notes")
                .serviceIds(null)
                .build();

            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

            ApplicationDto result = applicationService.createApplication(dtoWithoutServices);

            assertThat(result).isEqualTo(testApplicationDto);
            verify(applicantRepository, never()).save(any());
        }

        @Test
        void shouldCreateNewApplicantWhenNotFound() {
            ApplicationCreateDto dto = ApplicationCreateDto.builder()
                .passportNumber("NEW123")
                .plateNumber("1234 AB-7")
                .serviceIds(null)
                .build();

            Applicant newApplicant = Applicant.builder()
                .id(2L)
                .fullName("UNKNOWN")
                .passportNumber("NEW123")
                .build();

            when(applicantRepository.findByPassportNumber("NEW123")).thenReturn(Optional.empty());
            when(applicantRepository.save(any(Applicant.class))).thenReturn(newApplicant);
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
            when(licensePlateRepository.isPlateAvailable(1L)).thenReturn(true);
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

            ApplicationDto result = applicationService.createApplicationWithoutTransaction(dto);

            assertThat(result).isEqualTo(testApplicationDto);
            verify(applicantRepository).save(any(Applicant.class));
        }
    }
    @Nested
    @DisplayName("Additional Coverage Tests")
    class AdditionalCoverageTests {

        @Test
        void shouldInvalidateCacheWhenEmptyResult() {
            when(cacheService.get("PENDING", "MINSK")).thenReturn(Collections.emptyList());

            List<ApplicationDto> result = applicationService.getApplicationsByStatusAndRegionCached(
                ApplicationStatus.PENDING, "MINSK");

            assertThat(result).isEmpty();
            verify(cacheService).invalidate();
        }

        @Test
        void shouldLogWhenNoApplicationsButApplicantExists() {
            when(applicationRepository.findByApplicantPassport("MP1234567")).thenReturn(Collections.emptyList());
            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(applicationMapper.toDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

            List<ApplicationDto> result = applicationService.getApplicationsByPassport("MP1234567");

            assertThat(result).isEmpty();
        }

        @Test
        void shouldCallExpireApplication() {
            testApplication.setStatus(ApplicationStatus.PENDING);
            testApplication.setReservedUntil(LocalDateTime.now().minusHours(1));

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
            when(applicationRepository.save(testApplication)).thenReturn(testApplication);

            assertThatThrownBy(() -> applicationService.confirmApplication(1L))
                .isInstanceOf(BusinessException.class);

            verify(applicationRepository, times(1)).save(testApplication);
            assertThat(testApplication.getStatus()).isEqualTo(ApplicationStatus.EXPIRED);
        }
    }

    @Nested
    @DisplayName("SonarCloud Coverage Fix Tests")
    class SonarCoverageTests {

        @Test
        @DisplayName("Cover all branches: positive scenario with services")
        void shouldCoverPositiveBranchesWithServices() {
            List<Long> sIds = List.of(1L);
            ApplicationCreateDto dtoWithServices = ApplicationCreateDto.builder()
                .passportNumber(testApplicant.getPassportNumber())
                .plateNumber(testPlate.getPlateNumber())
                .serviceIds(sIds)
                .build();

            AdditionalService mockService = AdditionalService.builder()
                .id(1L).price(BigDecimal.TEN).build();

            when(applicantRepository.findByPassportNumber(anyString())).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber(anyString())).thenReturn(Optional.of(testPlate));

            when(serviceRepository.findAllById(sIds)).thenReturn(List.of(mockService));

            when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));
            when(applicationMapper.toDto(any())).thenReturn(testApplicationDto);

            BulkApplicationCreateDto bulkDto = BulkApplicationCreateDto.builder()
                .passportNumber(testApplicant.getPassportNumber())
                .applications(List.of(dtoWithServices))
                .build();

            applicationService.createBulkApplicationsWithoutTransaction(bulkDto);

            verify(serviceRepository).findAllById(sIds);
            verify(applicationRepository, atLeast(2)).save(any());
            verify(cacheService, atLeastOnce()).invalidate();
        }

        @Test
        @DisplayName("Cover branches: empty services and zero success scenarios")
        void shouldCoverNegativeAndEmptyBranches() {
            ApplicationCreateDto dtoEmptyServices = ApplicationCreateDto.builder()
                .passportNumber(testApplicant.getPassportNumber())
                .plateNumber(testPlate.getPlateNumber())
                .serviceIds(Collections.emptyList())
                .build();

            when(applicantRepository.findByPassportNumber(anyString())).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber(anyString())).thenReturn(Optional.of(testPlate));
            when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));
            when(applicationMapper.toDto(any())).thenReturn(testApplicationDto);

            applicationService.createApplication(dtoEmptyServices);

            verify(serviceRepository, never()).findAllById(any());

            BulkApplicationCreateDto emptyBulk = BulkApplicationCreateDto.builder()
                .passportNumber(testApplicant.getPassportNumber())
                .applications(Collections.emptyList())
                .build();

            applicationService.createBulkApplicationsWithoutTransaction(emptyBulk);

            verify(cacheService, atMost(1)).invalidate();
        }
    }

    @Nested
    @DisplayName("Additional Coverage Tests for 100%")
    class AdditionalCoverageTests1 {

        @Test
        @DisplayName("Should throw exception when applicant not found in bulk operation")
        void shouldThrowExceptionWhenApplicantNotFoundInBulk() {
            ApplicationCreateDto app1 = ApplicationCreateDto.builder()
                .passportNumber("NOT_EXIST")
                .plateNumber("1234 AB-7")
                .serviceIds(null)
                .build();

            BulkApplicationCreateDto bulkDto = BulkApplicationCreateDto.builder()
                .passportNumber("NOT_EXIST")
                .applications(List.of(app1))
                .build();

            when(applicantRepository.findByPassportNumber("NOT_EXIST")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> applicationService.createBulkApplicationsWithTransaction(bulkDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Заявитель с паспортом 'NOT_EXIST' не найден");
        }

        @Test
        @DisplayName("Cover all services and serviceIds branches")
        void shouldCoverAllServicesAndServiceIdsBranches() {
            AdditionalService service1 = AdditionalService.builder().id(1L).price(BigDecimal.valueOf(50)).build();
            AdditionalService service2 = AdditionalService.builder().id(2L).price(BigDecimal.valueOf(30)).build();

            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
            when(serviceRepository.findAllById(anyList())).thenReturn(List.of(service1, service2));
            when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));
            when(applicationMapper.toDto(any(Application.class))).thenReturn(testApplicationDto);

            applicationService.createApplication(testCreateDto);

            verify(serviceRepository, times(1)).findAllById(anyList());
            verify(applicationRepository, times(2)).save(any(Application.class));

            ApplicationCreateDto bulkApp1 = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(null)
                .build();
            ApplicationCreateDto bulkApp2 = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("5678 CD-9")
                .serviceIds(null)
                .build();

            BulkApplicationCreateDto bulkDto = BulkApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .applications(List.of(bulkApp1, bulkApp2))
                .build();

            LicensePlate plate2 = LicensePlate.builder()
                .id(2L)
                .plateNumber("5678 CD-9")
                .price(BigDecimal.valueOf(150))
                .department(testDept)
                .applications(new ArrayList<>())
                .build();

            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
            when(licensePlateRepository.findByPlateNumber("5678 CD-9")).thenReturn(Optional.of(plate2));

            BulkApplicationResult result = applicationService.createBulkApplicationsWithoutTransaction(bulkDto);

            assertThat(result.getSuccessful()).isEqualTo(2);
            verify(cacheService, atLeastOnce()).invalidate();

            ApplicationCreateDto singleAppWithServices = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(List.of(1L, 2L))
                .build();

            BulkApplicationCreateDto singleBulkDto = BulkApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .applications(List.of(singleAppWithServices))
                .build();

            applicationService.createBulkApplicationsWithoutTransaction(singleBulkDto);

            verify(applicationRepository, atLeast(2)).save(any(Application.class));
        }

        @Test
        @DisplayName("Should invalidate cache after successful bulk with transaction")
        void shouldInvalidateCacheAfterBulkWithTransaction() {
            ApplicationCreateDto app1 = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(null)
                .build();
            ApplicationCreateDto app2 = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("5678 CD-9")
                .serviceIds(null)
                .build();

            BulkApplicationCreateDto bulkDto = BulkApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .applications(List.of(app1, app2))
                .build();

            LicensePlate plate2 = LicensePlate.builder()
                .id(2L)
                .plateNumber("5678 CD-9")
                .price(BigDecimal.valueOf(150))
                .department(testDept)
                .applications(new ArrayList<>())
                .build();

            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
            when(licensePlateRepository.findByPlateNumber("5678 CD-9")).thenReturn(Optional.of(plate2));
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(any(Application.class))).thenReturn(testApplicationDto);

            BulkApplicationResult result = applicationService.createBulkApplicationsWithTransaction(bulkDto);

            assertThat(result.getTotalRequested()).isEqualTo(2);
            assertThat(result.getSuccessful()).isEqualTo(2);
            verify(cacheService).invalidate();
        }

        @Test
        @DisplayName("Should invalidate cache after partial success bulk without transaction")
        void shouldInvalidateCacheAfterBulkWithoutTransaction() {
            ApplicationCreateDto app1 = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(null)
                .build();
            ApplicationCreateDto app2 = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("NOT_EXIST")
                .serviceIds(null)
                .build();

            BulkApplicationCreateDto bulkDto = BulkApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .applications(List.of(app1, app2))
                .build();

            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
            when(licensePlateRepository.findByPlateNumber("NOT_EXIST")).thenReturn(Optional.empty());
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(any(Application.class))).thenReturn(testApplicationDto);

            BulkApplicationResult result = applicationService.createBulkApplicationsWithoutTransaction(bulkDto);

            assertThat(result.getTotalRequested()).isEqualTo(2);
            assertThat(result.getSuccessful()).isEqualTo(1);
            assertThat(result.getFailed()).isEqualTo(1);
            verify(cacheService).invalidate();
        }

        @Test
        @DisplayName("Should set CONFIRMED status when transactional in bulk")
        void shouldSetConfirmedStatusWhenTransactional() {
            ApplicationCreateDto app1 = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(null)
                .build();

            BulkApplicationCreateDto bulkDto = BulkApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .applications(List.of(app1))
                .build();

            ArgumentCaptor<Application> applicationCaptor = ArgumentCaptor.forClass(Application.class);

            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
            when(applicationRepository.save(applicationCaptor.capture())).thenReturn(testApplication);
            when(applicationMapper.toDto(any(Application.class))).thenReturn(testApplicationDto);

            applicationService.createBulkApplicationsWithTransaction(bulkDto);

            Application savedApplication = applicationCaptor.getValue();
            assertThat(savedApplication.getStatus()).isEqualTo(ApplicationStatus.CONFIRMED);
        }

        @Test
        @DisplayName("Should set PENDING status when not transactional in bulk")
        void shouldSetPendingStatusWhenNotTransactional() {
            ApplicationCreateDto app1 = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(null)
                .build();

            BulkApplicationCreateDto bulkDto = BulkApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .applications(List.of(app1))
                .build();

            ArgumentCaptor<Application> applicationCaptor = ArgumentCaptor.forClass(Application.class);

            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
            when(applicationRepository.save(applicationCaptor.capture())).thenReturn(testApplication);
            when(applicationMapper.toDto(any(Application.class))).thenReturn(testApplicationDto);

            applicationService.createBulkApplicationsWithoutTransaction(bulkDto);

            Application savedApplication = applicationCaptor.getValue();
            assertThat(savedApplication.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        }

        @Test
        @DisplayName("Should cover else branch for isPlateAvailable in createApplicationInternal")
        void shouldCoverElseBranchForIsPlateAvailable() {
            ApplicationCreateDto dto = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(null)
                .build();

            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
            when(licensePlateRepository.isPlateAvailable(1L)).thenReturn(false);

            assertThatThrownBy(() -> applicationService.createApplicationWithoutTransaction(dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("недоступен");
        }

        @Test
        @DisplayName("Should cover getServices method when serviceIds is null or empty")
        void shouldCoverGetServicesWhenServiceIdsNullOrEmpty() {
            ApplicationCreateDto dtoNull = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(null)
                .build();

            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

            ApplicationDto result = applicationService.createApplication(dtoNull);
            assertThat(result).isEqualTo(testApplicationDto);
            verify(serviceRepository, never()).findAllById(anyList());

            ApplicationCreateDto dtoEmpty = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(Collections.emptyList())
                .build();

            applicationService.createApplication(dtoEmpty);
            verify(serviceRepository, never()).findAllById(anyList());
        }

        @Test
        @DisplayName("Should cover createSingleApplication with services not found exception")
        void shouldCoverCreateSingleApplicationServicesNotFound() {
            ApplicationCreateDto app1 = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(List.of(1L, 2L))
                .build();

            BulkApplicationCreateDto bulkDto = BulkApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .applications(List.of(app1))
                .build();

            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
            when(serviceRepository.findAllById(anyList())).thenReturn(List.of(AdditionalService.builder().id(1L).build()));

            assertThatThrownBy(() -> applicationService.createBulkApplicationsWithTransaction(bulkDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Некоторые услуги не найдены");
        }

        @Test
        @DisplayName("Should cover createSingleApplication with plate not available")
        void shouldCoverCreateSingleApplicationPlateNotAvailable() {
            ApplicationCreateDto app1 = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(null)
                .build();

            BulkApplicationCreateDto bulkDto = BulkApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .applications(List.of(app1))
                .build();

            Application activeApplication = Application.builder()
                .status(ApplicationStatus.PENDING)
                .submissionDate(LocalDateTime.now())
                .reservedUntil(LocalDateTime.now().plusHours(1))
                .build();
            testPlate.getApplications().add(activeApplication);

            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));

            assertThatThrownBy(() -> applicationService.createBulkApplicationsWithTransaction(bulkDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("недоступен");
        }

        @Test
        @DisplayName("Should cover createSingleApplication with plate not found")
        void shouldCoverCreateSingleApplicationPlateNotFound() {
            ApplicationCreateDto app1 = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("NOT_EXIST")
                .serviceIds(null)
                .build();

            BulkApplicationCreateDto bulkDto = BulkApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .applications(List.of(app1))
                .build();

            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("NOT_EXIST")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> applicationService.createBulkApplicationsWithTransaction(bulkDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Bulk application failed");
        }

        @ParameterizedTest
        @DisplayName("Should cover pagination with empty page")
        @CsvSource({"0", "1"})
        void shouldCoverPaginationWithEmptyPage(int pageNumber) {
            Pageable pageable = PageRequest.of(pageNumber, 10);
            Page<Application> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            when(applicationRepository.findByApplicantPassport("MP1234567", pageable))
                .thenReturn(emptyPage);

            Page<ApplicationDto> result = applicationService.getApplicationsByPassportPaginated("MP1234567", pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }
}