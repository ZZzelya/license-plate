package com.example.licenseplate.service;

import com.example.licenseplate.cache.ApplicationCacheService;
import com.example.licenseplate.dto.request.ApplicationCreateDto;
import com.example.licenseplate.dto.request.BulkApplicationCreateDto;
import com.example.licenseplate.dto.response.ApplicationDto;
import com.example.licenseplate.dto.response.BulkApplicationResult;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private LicensePlate testLicensePlate;
    private Application testApplication;
    private ApplicationDto testApplicationDto;
    private ApplicationCreateDto testCreateDto;
    private AdditionalService testService;

    @BeforeEach
    void setUp() {
        RegistrationDept testDepartment = RegistrationDept.builder()
            .id(1L)
            .name("Test Department")
            .region("MINSK")
            .build();

        testApplicant = Applicant.builder()
            .id(1L)
            .fullName("John Doe")
            .passportNumber("MP1234567")
            .build();

        testLicensePlate = LicensePlate.builder()
            .id(1L)
            .plateNumber("1234 AB-7")
            .price(BigDecimal.valueOf(1000))
            .department(testDepartment)
            .applications(Collections.emptyList())
            .build();

        testService = AdditionalService.builder()
            .id(1L)
            .name("Express Service")
            .price(BigDecimal.valueOf(500))
            .isAvailable(true)
            .build();

        testApplication = Application.builder()
            .id(1L)
            .applicant(testApplicant)
            .licensePlate(testLicensePlate)
            .department(testDepartment)
            .status(ApplicationStatus.PENDING)
            .submissionDate(LocalDateTime.now())
            .reservedUntil(LocalDateTime.now().plusHours(1))
            .paymentAmount(BigDecimal.valueOf(1500))
            .build();

        testApplicationDto = ApplicationDto.builder()
            .id(1L)
            .status("PENDING")
            .paymentAmount(BigDecimal.valueOf(1500))
            .applicantId(1L)
            .applicantName("John Doe")
            .licensePlateNumber("1234 AB-7")
            .build();

        testCreateDto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .plateNumber("1234 AB-7")
            .vehicleVin("1HGCM82633A123456")
            .vehicleModel("Honda Accord")
            .vehicleYear(2023)
            .serviceIds(List.of(1L))
            .notes("Test notes")
            .build();
    }

    @Nested
    @DisplayName("getAllApplications() Tests")
    class GetAllApplicationsTests {

        @Test
        @DisplayName("Should return list of application DTOs when applications exist")
        void shouldReturnListOfApplicationDtosWhenApplicationsExist() {
            List<Application> applications = List.of(testApplication);
            List<ApplicationDto> expectedDtos = List.of(testApplicationDto);

            when(applicationRepository.findAll()).thenReturn(applications);
            when(applicationMapper.toDtoList(applications)).thenReturn(expectedDtos);

            List<ApplicationDto> result = applicationService.getAllApplications();

            assertThat(result)
                .isNotNull()
                .hasSize(1)
                .isEqualTo(expectedDtos);

            verify(applicationRepository).findAll();
            verify(applicationMapper).toDtoList(applications);
        }

        @Test
        @DisplayName("Should return empty list when no applications exist")
        void shouldReturnEmptyListWhenNoApplicationsExist() {
            when(applicationRepository.findAll()).thenReturn(Collections.emptyList());
            when(applicationMapper.toDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

            List<ApplicationDto> result = applicationService.getAllApplications();

            assertThat(result).isEmpty();
            verify(applicationRepository).findAll();
        }
    }

    @Nested
    @DisplayName("getApplicationById() Tests")
    class GetApplicationByIdTests {

        @Test
        @DisplayName("Should return application DTO when application exists")
        void shouldReturnApplicationDtoWhenApplicationExists() {
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
            when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

            ApplicationDto result = applicationService.getApplicationById(1L);

            assertThat(result).isEqualTo(testApplicationDto);
            verify(applicationRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when application does not exist")
        void shouldThrowResourceNotFoundExceptionWhenApplicationDoesNotExist() {
            when(applicationRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> applicationService.getApplicationById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Заявление не найдено с id: 99");

            verify(applicationRepository).findById(99L);
            verify(applicationMapper, never()).toDto(any());
        }
    }

    @Nested
    @DisplayName("getApplicationWithDetails() Tests")
    class GetApplicationWithDetailsTests {

        @Test
        @DisplayName("Should return application DTO with details when application exists")
        void shouldReturnApplicationDtoWithDetailsWhenApplicationExists() {
            when(applicationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(testApplication));
            when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

            ApplicationDto result = applicationService.getApplicationWithDetails(1L);

            assertThat(result).isEqualTo(testApplicationDto);
            verify(applicationRepository).findByIdWithDetails(1L);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when application does not exist")
        void shouldThrowResourceNotFoundExceptionWhenApplicationDoesNotExist() {
            when(applicationRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> applicationService.getApplicationWithDetails(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Заявление не найдено с id: 99");
        }
    }

    @Nested
    @DisplayName("getApplicationsByPassport() Tests")
    class GetApplicationsByPassportTests {

        @Test
        @DisplayName("Should return list of applications when applications exist")
        void shouldReturnListOfApplicationsWhenApplicationsExist() {
            List<Application> applications = List.of(testApplication);
            List<ApplicationDto> expectedDtos = List.of(testApplicationDto);

            when(applicationRepository.findByApplicantPassport("MP1234567")).thenReturn(applications);
            when(applicationMapper.toDtoList(applications)).thenReturn(expectedDtos);

            List<ApplicationDto> result = applicationService.getApplicationsByPassport("MP1234567");

            assertThat(result).isEqualTo(expectedDtos);
            verify(applicationRepository).findByApplicantPassport("MP1234567");
            verify(applicantRepository, never()).findByPassportNumber(anyString());
        }

        @Test
        @DisplayName("Should return empty list when no applications but applicant exists")
        void shouldReturnEmptyListWhenNoApplicationsButApplicantExists() {
            when(applicationRepository.findByApplicantPassport("MP1234567")).thenReturn(Collections.emptyList());
            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(applicationMapper.toDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

            List<ApplicationDto> result = applicationService.getApplicationsByPassport("MP1234567");

            assertThat(result).isEmpty();
            verify(applicationRepository).findByApplicantPassport("MP1234567");
            verify(applicantRepository).findByPassportNumber("MP1234567");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when applicant does not exist")
        void shouldThrowResourceNotFoundExceptionWhenApplicantDoesNotExist() {
            when(applicationRepository.findByApplicantPassport("INVALID")).thenReturn(Collections.emptyList());
            when(applicantRepository.findByPassportNumber("INVALID")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> applicationService.getApplicationsByPassport("INVALID"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Заявитель с паспортом 'INVALID' не найден");
        }
    }

    @Nested
    @DisplayName("getApplicationsByStatusAndRegion() Tests")
    class GetApplicationsByStatusAndRegionTests {

        @Test
        @DisplayName("Should return list of applications when applications exist")
        void shouldReturnListOfApplicationsWhenApplicationsExist() {
            List<Application> applications = List.of(testApplication);
            List<ApplicationDto> expectedDtos = List.of(testApplicationDto);

            when(departmentRepository.existsByRegion("MINSK")).thenReturn(true);
            when(applicationRepository.findByStatusAndDepartmentRegion(ApplicationStatus.PENDING, "MINSK"))
                .thenReturn(applications);
            when(applicationMapper.toDtoList(applications)).thenReturn(expectedDtos);

            List<ApplicationDto> result = applicationService.getApplicationsByStatusAndRegion(
                ApplicationStatus.PENDING, "MINSK");

            assertThat(result).isEqualTo(expectedDtos);
            verify(departmentRepository).existsByRegion("MINSK");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when region does not exist")
        void shouldThrowResourceNotFoundExceptionWhenRegionDoesNotExist() {
            when(departmentRepository.existsByRegion("INVALID")).thenReturn(false);

            assertThatThrownBy(() -> applicationService.getApplicationsByStatusAndRegion(
                ApplicationStatus.PENDING, "INVALID"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Регион 'INVALID' не найден");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when no applications found")
        void shouldThrowResourceNotFoundExceptionWhenNoApplicationsFound() {
            when(departmentRepository.existsByRegion("MINSK")).thenReturn(true);
            when(applicationRepository.findByStatusAndDepartmentRegion(ApplicationStatus.PENDING, "MINSK"))
                .thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> applicationService.getApplicationsByStatusAndRegion(
                ApplicationStatus.PENDING, "MINSK"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Заявки со статусом 'PENDING' в регионе 'MINSK' не найдены");
        }
    }

    @Nested
    @DisplayName("getApplicationsByStatusAndRegionNative() Tests")
    class GetApplicationsByStatusAndRegionNativeTests {

        @Test
        @DisplayName("Should return list of applications using native query")
        void shouldReturnListOfApplicationsUsingNativeQuery() {
            List<Application> applications = List.of(testApplication);
            List<ApplicationDto> expectedDtos = List.of(testApplicationDto);

            when(departmentRepository.existsByRegion("MINSK")).thenReturn(true);
            when(applicationRepository.findByStatusAndDepartmentRegionNative("PENDING", "MINSK"))
                .thenReturn(applications);
            when(applicationMapper.toDtoList(applications)).thenReturn(expectedDtos);

            List<ApplicationDto> result = applicationService.getApplicationsByStatusAndRegionNative(
                ApplicationStatus.PENDING, "MINSK");

            assertThat(result).isEqualTo(expectedDtos);
            verify(applicationRepository).findByStatusAndDepartmentRegionNative("PENDING", "MINSK");
        }
    }

    @Nested
    @DisplayName("getApplicationsByStatusAndRegionCached() Tests")
    class GetApplicationsByStatusAndRegionCachedTests {

        @Test
        @DisplayName("Should return cached result when cache hit")
        void shouldReturnCachedResultWhenCacheHit() {
            List<ApplicationDto> cachedResult = List.of(testApplicationDto);

            when(cacheService.get("PENDING", "MINSK")).thenReturn(cachedResult);

            List<ApplicationDto> result = applicationService.getApplicationsByStatusAndRegionCached(
                ApplicationStatus.PENDING, "MINSK");

            assertThat(result).isEqualTo(cachedResult);
            verify(cacheService).get("PENDING", "MINSK");
            verify(applicationRepository, never()).findByStatusAndDepartmentRegion(any(), any());
        }

        @Test
        @DisplayName("Should fetch from database and cache when cache miss")
        void shouldFetchFromDatabaseAndCacheWhenCacheMiss() {
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
        @DisplayName("Should invalidate cache when empty cached result")
        void shouldInvalidateCacheWhenEmptyCachedResult() {
            when(cacheService.get("PENDING", "MINSK")).thenReturn(Collections.emptyList());

            List<Application> applications = List.of(testApplication);
            List<ApplicationDto> expectedDtos = List.of(testApplicationDto);

            when(applicationRepository.findByStatusAndDepartmentRegion(ApplicationStatus.PENDING, "MINSK"))
                .thenReturn(applications);
            when(applicationMapper.toDtoList(applications)).thenReturn(expectedDtos);

            List<ApplicationDto> result = applicationService.getApplicationsByStatusAndRegionCached(
                ApplicationStatus.PENDING, "MINSK");

            assertThat(result).isEqualTo(expectedDtos);
            verify(cacheService).invalidate();
            verify(cacheService).put("PENDING", "MINSK", expectedDtos);
        }
    }

    @Nested
    @DisplayName("getApplicationsByPassportPaginated() Tests")
    class GetApplicationsByPassportPaginatedTests {

        @Test
        @DisplayName("Should return paginated applications")
        void shouldReturnPaginatedApplications() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Application> applicationPage = new PageImpl<>(List.of(testApplication), pageable, 1);

            when(applicationRepository.findByApplicantPassport("MP1234567", pageable)).thenReturn(applicationPage);

            Page<ApplicationDto> result = applicationService.getApplicationsByPassportPaginated("MP1234567", pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            verify(applicationRepository).findByApplicantPassport("MP1234567", pageable);
        }
    }

    @Nested
    @DisplayName("Cache Invalidation Tests")
    class CacheInvalidationTests {

        @Test
        @DisplayName("Should invalidate entire cache")
        void shouldInvalidateEntireCache() {
            doNothing().when(cacheService).invalidate();

            applicationService.invalidateCache();

            verify(cacheService).invalidate();
        }

        @Test
        @DisplayName("Should invalidate cache by region")
        void shouldInvalidateCacheByRegion() {
            doNothing().when(cacheService).invalidateByRegion("MINSK");

            applicationService.invalidateCacheByRegion("MINSK");

            verify(cacheService).invalidateByRegion("MINSK");
        }

        @Test
        @DisplayName("Should invalidate cache by status")
        void shouldInvalidateCacheByStatus() {
            doNothing().when(cacheService).invalidateByStatus("PENDING");

            applicationService.invalidateCacheByStatus("PENDING");

            verify(cacheService).invalidateByStatus("PENDING");
        }
    }

    @Nested
    @DisplayName("createApplication() Tests")
    class CreateApplicationTests {

        @Test
        @DisplayName("Should create application successfully")
        void shouldCreateApplicationSuccessfully() {
            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testLicensePlate));
            when(serviceRepository.findAllById(List.of(1L))).thenReturn(List.of(testService));
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

            ApplicationDto result = applicationService.createApplication(testCreateDto);

            assertThat(result).isEqualTo(testApplicationDto);
            verify(applicationRepository).save(any(Application.class));
            verify(cacheService).invalidate();
        }

        @Test
        @DisplayName("Should throw exception when plate not available")
        void shouldThrowExceptionWhenPlateNotAvailable() {
            Application activeApplication = Application.builder()
                .id(2L)
                .status(ApplicationStatus.PENDING)
                .licensePlate(testLicensePlate)
                .build();

            testLicensePlate.setApplications(List.of(activeApplication));

            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testLicensePlate));

            assertThatThrownBy(() -> applicationService.createApplication(testCreateDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Номерной знак '1234 AB-7' недоступен");

            verify(applicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when plate not found")
        void shouldThrowExceptionWhenPlateNotFound() {
            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> applicationService.createApplication(testCreateDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Номерной знак '1234 AB-7' не найден");
        }

        @Test
        @DisplayName("Should throw exception when services not found")
        void shouldThrowExceptionWhenServicesNotFound() {
            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testLicensePlate));
            when(serviceRepository.findAllById(List.of(1L))).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> applicationService.createApplication(testCreateDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Некоторые услуги не найдены");

            verify(applicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should create new applicant when not found - using createApplicationWithoutTransaction")
        void shouldCreateNewApplicantWhenNotFound() {
            // Используем createApplicationWithoutTransaction, который вызывает findOrCreateApplicant
            ApplicationCreateDto newApplicantDto = ApplicationCreateDto.builder()
                .passportNumber("NEW123")
                .plateNumber("1234 AB-7")
                .build();

            Applicant newApplicant = Applicant.builder()
                .id(2L)
                .fullName("UNKNOWN")
                .passportNumber("NEW123")
                .build();

            when(applicantRepository.findByPassportNumber("NEW123")).thenReturn(Optional.empty());
            when(applicantRepository.save(any(Applicant.class))).thenReturn(newApplicant);
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testLicensePlate));
            when(licensePlateRepository.isPlateAvailable(testLicensePlate.getId())).thenReturn(true);
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

            ApplicationDto result = applicationService.createApplicationWithoutTransaction(newApplicantDto);

            assertThat(result).isEqualTo(testApplicationDto);
            verify(applicantRepository).save(any(Applicant.class));
        }

        @Test
        @DisplayName("Should calculate total amount correctly with services")
        void shouldCalculateTotalAmountCorrectlyWithServices() {
            ArgumentCaptor<Application> applicationCaptor = ArgumentCaptor.forClass(Application.class);

            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of
                (testLicensePlate));
            when(serviceRepository.findAllById(List.of(1L))).thenReturn(List.of(testService));
            when(applicationRepository.save(applicationCaptor.capture())).thenReturn(testApplication);
            when(applicationMapper.toDto(any(Application.class))).thenReturn(testApplicationDto);

            applicationService.createApplication(testCreateDto);

            Application savedApplication = applicationCaptor.getValue();
            assertThat(savedApplication.getPaymentAmount())
                .isEqualByComparingTo(BigDecimal.valueOf(1500));
        }

        @Test
        @DisplayName("Should calculate total amount correctly without services")
        void shouldCalculateTotalAmountCorrectlyWithoutServices() {
            ApplicationCreateDto createDtoWithoutServices = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(Collections.emptyList())
                .build();

            ArgumentCaptor<Application> applicationCaptor = ArgumentCaptor.forClass(Application.class);

            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testLicensePlate));
            when(applicationRepository.save(applicationCaptor.capture())).thenReturn(testApplication);
            when(applicationMapper.toDto(any(Application.class))).thenReturn(testApplicationDto);

            applicationService.createApplication(createDtoWithoutServices);

            Application savedApplication = applicationCaptor.getValue();
            assertThat(savedApplication.getPaymentAmount())
                .isEqualByComparingTo(BigDecimal.valueOf(1000));

            verify(serviceRepository, never()).findAllById(any());
        }
    }

    @Nested
    @DisplayName("createApplicationWithoutTransaction() Tests")
    class CreateApplicationWithoutTransactionTests {

        @Test
        @DisplayName("Should create application without transaction check")
        void shouldCreateApplicationWithoutTransactionCheck() {
            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testLicensePlate));
            when(licensePlateRepository.isPlateAvailable(testLicensePlate.getId())).thenReturn(true);
            when(serviceRepository.findAllById(List.of(1L))).thenReturn(List.of(testService));
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

            ApplicationDto result = applicationService.createApplicationWithoutTransaction(testCreateDto);

            assertThat(result).isEqualTo(testApplicationDto);
            verify(licensePlateRepository).isPlateAvailable(testLicensePlate.getId());
        }

        @Test
        @DisplayName("Should throw exception when plate not available in without transaction")
        void shouldThrowExceptionWhenPlateNotAvailableWithoutTransaction() {
            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testLicensePlate));
            when(licensePlateRepository.isPlateAvailable(testLicensePlate.getId())).thenReturn(false);

            assertThatThrownBy(() -> applicationService.createApplicationWithoutTransaction(testCreateDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Номерной знак '1234 AB-7' недоступен");
        }
    }

    @Nested
    @DisplayName("createApplicationWithTransaction() Tests")
    class CreateApplicationWithTransactionTests {

        @Test
        @DisplayName("Should create application with transaction check")
        void shouldCreateApplicationWithTransactionCheck() {
            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testLicensePlate));
            when(serviceRepository.findAllById(List.of(1L))).thenReturn(List.of(testService));
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

            ApplicationDto result = applicationService.createApplicationWithTransaction(testCreateDto);

            assertThat(result).isEqualTo(testApplicationDto);
            verify(applicantRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("confirmApplication() Tests")
    class ConfirmApplicationTests {

        @Test
        @DisplayName("Should confirm application when status is PENDING and not expired")
        void shouldConfirmApplicationWhenStatusPendingAndNotExpired() {
            testApplication.setStatus(ApplicationStatus.PENDING);
            testApplication.setReservedUntil(LocalDateTime.now().plusHours(1));

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

            ApplicationDto result = applicationService.confirmApplication(1L);

            assertThat(result).isEqualTo(testApplicationDto);
            assertThat(testApplication.getStatus()).isEqualTo(ApplicationStatus.CONFIRMED);
            assertThat(testApplication.getConfirmationDate()).isNotNull();
            verify(cacheService).invalidate();
        }

        @Test
        @DisplayName("Should throw exception when status is not PENDING")
        void shouldThrowExceptionWhenStatusIsNotPending() {
            testApplication.setStatus(ApplicationStatus.CONFIRMED);

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));

            assertThatThrownBy(() -> applicationService.confirmApplication(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Заявление не в статусе PENDING: CONFIRMED");

            verify(applicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when reservation expired")
        void shouldThrowExceptionWhenReservationExpired() {
            testApplication.setStatus(ApplicationStatus.PENDING);
            testApplication.setReservedUntil(LocalDateTime.now().minusHours(1));

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);

            assertThatThrownBy(() -> applicationService.confirmApplication(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Время бронирования истекло");

            assertThat(testApplication.getStatus()).isEqualTo(ApplicationStatus.EXPIRED);
            verify(applicationRepository).save(testApplication);
        }
    }

    @Nested
    @DisplayName("completeApplication() Tests")
    class CompleteApplicationTests {

        @Test
        @DisplayName("Should complete application when status is CONFIRMED")
        void shouldCompleteApplicationWhenStatusIsConfirmed() {
            testApplication.setStatus(ApplicationStatus.CONFIRMED);

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
            when(licensePlateRepository.save(any(LicensePlate.class))).thenReturn(testLicensePlate);
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

            ApplicationDto result = applicationService.completeApplication(1L);

            assertThat(result).isEqualTo(testApplicationDto);
            assertThat(testApplication.getStatus()).isEqualTo(ApplicationStatus.COMPLETED);
            assertThat(testLicensePlate.getIssueDate()).isNotNull();
            assertThat(testLicensePlate.getExpiryDate()).isNotNull();
            verify(licensePlateRepository).save(testLicensePlate);
            verify(cacheService).invalidate();
        }

        @Test
        @DisplayName("Should throw exception when status is not CONFIRMED")
        void shouldThrowExceptionWhenStatusIsNotConfirmed() {
            testApplication.setStatus(ApplicationStatus.PENDING);

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));

            assertThatThrownBy(() -> applicationService.completeApplication(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Application is not in CONFIRMED status");

            verify(licensePlateRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("cancelApplication() Tests")
    class CancelApplicationTests {

        @Test
        @DisplayName("Should cancel application when status is PENDING")
        void shouldCancelApplicationWhenStatusIsPending() {
            testApplication.setStatus(ApplicationStatus.PENDING);

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

            ApplicationDto result = applicationService.cancelApplication(1L);

            assertThat(result).isEqualTo(testApplicationDto);
            assertThat(testApplication.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
            verify(cacheService).invalidate();
        }

        @Test
        @DisplayName("Should cancel application when status is CONFIRMED")
        void shouldCancelApplicationWhenStatusIsConfirmed() {
            testApplication.setStatus(ApplicationStatus.CONFIRMED);

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

            ApplicationDto result = applicationService.cancelApplication(1L);

            assertThat(result).isEqualTo(testApplicationDto);
            assertThat(testApplication.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
        }

        @Test
        @DisplayName("Should throw exception when status is COMPLETED")
        void shouldThrowExceptionWhenStatusIsCompleted() {
            testApplication.setStatus(ApplicationStatus.COMPLETED);

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));

            assertThatThrownBy(() -> applicationService.cancelApplication(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Нельзя отменить заявление в статусе: COMPLETED");

            verify(applicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when status is CANCELLED")
        void shouldThrowExceptionWhenStatusIsCancelled() {
            testApplication.setStatus(ApplicationStatus.CANCELLED);

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));

            assertThatThrownBy(() -> applicationService.cancelApplication(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Нельзя отменить заявление в статусе: CANCELLED");
        }
    }

    @Nested
    @DisplayName("deleteApplication() Tests")
    class DeleteApplicationTests {

        @Test
        @DisplayName("Should delete application when exists")
        void shouldDeleteApplicationWhenExists() {
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
            doNothing().when(applicationRepository).delete(testApplication);

            applicationService.deleteApplication(1L);

            verify(applicationRepository).delete(testApplication);
            verify(cacheService).invalidate();
        }

        @Test
        @DisplayName("Should throw exception when application does not exist")
        void shouldThrowExceptionWhenApplicationDoesNotExist() {
            when(applicationRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> applicationService.deleteApplication(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Заявление не найдено с id: 99");

            verify(applicationRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("createBulkApplications() Tests")
    class CreateBulkApplicationsTests {

        @Test
        @DisplayName("Should create all applications with transaction")
        void shouldCreateAllApplicationsWithTransaction() {
            BulkApplicationCreateDto bulkDto = BulkApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .applications(List.of(testCreateDto))
                .build();

            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testLicensePlate));
            when(serviceRepository.findAllById(List.of(1L))).thenReturn(List.of(testService));
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

            BulkApplicationResult result = applicationService.createBulkApplicationsWithTransaction(bulkDto);

            assertThat(result.getTotalRequested()).isEqualTo(1);
            assertThat(result.getSuccessful()).isEqualTo(1);
            assertThat(result.getFailed()).isZero();
            assertThat(result.getSuccessfulApplications()).hasSize(1);
            assertThat(result.getErrors()).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when duplicate plates in bulk request")
        void shouldThrowExceptionWhenDuplicatePlatesInBulkRequest() {
            BulkApplicationCreateDto bulkDto = BulkApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .applications(List.of(testCreateDto, testCreateDto))
                .build();

            assertThatThrownBy(() -> applicationService.createBulkApplicationsWithTransaction(bulkDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Duplicate plate numbers in bulk request");
        }

        @Test
        @DisplayName("Should create partial applications without transaction")
        void shouldCreatePartialApplicationsWithoutTransaction() {
            ApplicationCreateDto failingCreateDto = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("INVALID")
                .build();

            BulkApplicationCreateDto bulkDto = BulkApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .applications(List.of(testCreateDto, failingCreateDto))
                .build();

            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testLicensePlate));
            when(licensePlateRepository.findByPlateNumber("INVALID")).thenReturn(Optional.empty());
            when(serviceRepository.findAllById(List.of(1L))).thenReturn(List.of(testService));
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

            BulkApplicationResult result = applicationService.createBulkApplicationsWithoutTransaction(bulkDto);

            assertThat(result.getTotalRequested()).isEqualTo(2);
            assertThat(result.getSuccessful()).isEqualTo(1);
            assertThat(result.getFailed()).isEqualTo(1);
            assertThat(result.getSuccessfulApplications()).hasSize(1);
            assertThat(result.getErrors()).hasSize(1);
        }

        @Test
        @DisplayName("Should throw exception when applicant not found in bulk")
        void shouldThrowExceptionWhenApplicantNotFoundInBulk() {
            BulkApplicationCreateDto bulkDto = BulkApplicationCreateDto.builder()
                .passportNumber("INVALID")
                .applications(List.of(testCreateDto))
                .build();

            when(applicantRepository.findByPassportNumber("INVALID")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> applicationService.createBulkApplicationsWithTransaction(bulkDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Заявитель с паспортом 'INVALID' не найден");
        }
    }
}