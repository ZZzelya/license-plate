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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
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
    private Application testApplication;
    private ApplicationDto testApplicationDto;

    @BeforeEach
    void setUp() {
        RegistrationDept testDept = RegistrationDept.builder()
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
    }

    @Nested
    @DisplayName("Get Methods Tests")
    class GetMethodsTests {

        @Test
        void getAllApplications_ShouldReturnList() {
            List<Application> applications = List.of(testApplication);
            List<ApplicationDto> expectedDtos = List.of(testApplicationDto);

            when(applicationRepository.findAll()).thenReturn(applications);
            when(applicationMapper.toDtoList(applications)).thenReturn(expectedDtos);

            List<ApplicationDto> result = applicationService.getAllApplications();

            assertThat(result).isEqualTo(expectedDtos);
        }

        @Test
        void getAllApplications_ShouldReturnEmptyList() {
            when(applicationRepository.findAll()).thenReturn(Collections.emptyList());
            when(applicationMapper.toDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

            List<ApplicationDto> result = applicationService.getAllApplications();

            assertThat(result).isEmpty();
        }

        @Test
        void getApplicationById_ShouldReturnApplication() {
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
            when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

            ApplicationDto result = applicationService.getApplicationById(1L);

            assertThat(result).isEqualTo(testApplicationDto);
        }

        @Test
        void getApplicationById_ShouldThrowNotFound() {
            when(applicationRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> applicationService.getApplicationById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void getApplicationWithDetails_ShouldReturnWithDetails() {
            when(applicationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(testApplication));
            when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

            ApplicationDto result = applicationService.getApplicationWithDetails(1L);

            assertThat(result).isEqualTo(testApplicationDto);
        }

        @Test
        void getApplicationsByPassport_ShouldReturnApplications() {
            List<Application> applications = List.of(testApplication);
            List<ApplicationDto> expectedDtos = List.of(testApplicationDto);

            when(applicationRepository.findByApplicantPassport("MP1234567")).thenReturn(applications);
            when(applicationMapper.toDtoList(applications)).thenReturn(expectedDtos);

            List<ApplicationDto> result = applicationService.getApplicationsByPassport("MP1234567");

            assertThat(result).isEqualTo(expectedDtos);
        }

        @Test
        void getApplicationsByPassport_WhenNoApplications_ShouldReturnEmptyList() {
            when(applicationRepository.findByApplicantPassport("MP1234567")).thenReturn(Collections.emptyList());
            when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
            when(applicationMapper.toDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

            List<ApplicationDto> result = applicationService.getApplicationsByPassport("MP1234567");

            assertThat(result).isEmpty();
        }

        @Test
        void getApplicationsByPassport_WhenApplicantNotFound_ShouldThrowException() {
            when(applicationRepository.findByApplicantPassport("NOT_EXIST")).thenReturn(Collections.emptyList());
            when(applicantRepository.findByPassportNumber("NOT_EXIST")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> applicationService.getApplicationsByPassport("NOT_EXIST"))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void getApplicationsByStatusAndRegion_ShouldReturnApplications() {
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
        void getApplicationsByStatusAndRegion_WhenRegionNotFound_ShouldThrowException() {
            when(departmentRepository.existsByRegion("UNKNOWN")).thenReturn(false);

            assertThatThrownBy(() -> applicationService.getApplicationsByStatusAndRegion(
                ApplicationStatus.PENDING, "UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void getApplicationsByStatusAndRegion_WhenNoApplications_ShouldThrowException() {
            when(departmentRepository.existsByRegion("MINSK")).thenReturn(true);
            when(applicationRepository.findByStatusAndDepartmentRegion(ApplicationStatus.PENDING, "MINSK"))
                .thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> applicationService.getApplicationsByStatusAndRegion(
                ApplicationStatus.PENDING, "MINSK"))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void getApplicationsByStatusAndRegionCached_ShouldReturnCachedResult() {
            List<ApplicationDto> cachedResult = List.of(testApplicationDto);

            when(cacheService.get("PENDING", "MINSK")).thenReturn(cachedResult);

            List<ApplicationDto> result = applicationService.getApplicationsByStatusAndRegionCached(
                ApplicationStatus.PENDING, "MINSK");

            assertThat(result).isEqualTo(cachedResult);
        }

        @Test
        void getApplicationsByStatusAndRegionCached_ShouldFetchFromDbAndCache() {
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
        void getApplicationsByStatusAndRegionCached_EmptyResult_ShouldInvalidateCache() {
            when(cacheService.get("PENDING", "MINSK")).thenReturn(Collections.emptyList());

            List<ApplicationDto> result = applicationService.getApplicationsByStatusAndRegionCached(
                ApplicationStatus.PENDING, "MINSK");

            assertThat(result).isEmpty();
            verify(cacheService).invalidate();
        }

        @Test
        void getApplicationsByStatusAndRegionNative_ShouldReturnApplications() {
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
        void getApplicationsByPassportPaginated_ShouldReturnPage() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Application> applicationPage = new PageImpl<>(List.of(testApplication), pageable, 1);

            when(applicationRepository.findByApplicantPassport("MP1234567", pageable))
                .thenReturn(applicationPage);
            when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

            Page<ApplicationDto> result = applicationService.getApplicationsByPassportPaginated("MP1234567", pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Create Application Tests")
    class CreateApplicationTests {

        @Test
        void createApplication_ShouldCreateSuccessfully() {
            AdditionalService service = AdditionalService.builder().id(1L).price(BigDecimal.TEN).build();

            ApplicationCreateDto dto = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(List.of(1L))
                .build();

            when(applicantRepository.findByPassportNumber(anyString())).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber(anyString())).thenReturn(Optional.of(testPlate));
            when(serviceRepository.findAllById(anyList())).thenReturn(List.of(service));
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(any())).thenReturn(testApplicationDto);

            ApplicationDto result = applicationService.createApplication(dto);

            assertThat(result).isEqualTo(testApplicationDto);
            verify(cacheService).invalidate();
        }

        @Test
        void createApplication_WithoutServices_ShouldCreateSuccessfully() {
            ApplicationCreateDto dto = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(null)
                .build();

            when(applicantRepository.findByPassportNumber(anyString())).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber(anyString())).thenReturn(Optional.of(testPlate));
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(any())).thenReturn(testApplicationDto);

            ApplicationDto result = applicationService.createApplication(dto);

            assertThat(result).isEqualTo(testApplicationDto);
            verify(serviceRepository, never()).findAllById(any());
        }

        @Test
        void createApplicationWithoutTransaction_ShouldCreate() {
            ApplicationCreateDto dto = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(null)
                .build();

            when(applicantRepository.findByPassportNumber(anyString())).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber(anyString())).thenReturn(Optional.of(testPlate));
            when(licensePlateRepository.isPlateAvailable(anyLong())).thenReturn(true);
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(any())).thenReturn(testApplicationDto);

            ApplicationDto result = applicationService.createApplicationWithoutTransaction(dto);

            assertThat(result).isEqualTo(testApplicationDto);
        }

        @Test
        void createApplicationWithTransaction_ShouldCreate() {
            ApplicationCreateDto dto = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(null)
                .build();

            when(applicantRepository.findByPassportNumber(anyString())).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber(anyString())).thenReturn(Optional.of(testPlate));
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(any())).thenReturn(testApplicationDto);

            ApplicationDto result = applicationService.createApplicationWithTransaction(dto);

            assertThat(result).isEqualTo(testApplicationDto);
        }

        @Test
        void createApplication_WhenPlateNotAvailable_ShouldThrowException() {
            ApplicationCreateDto dto = ApplicationCreateDto.builder()
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

            when(applicantRepository.findByPassportNumber(anyString())).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber(anyString())).thenReturn(Optional.of(testPlate));

            assertThatThrownBy(() -> applicationService.createApplication(dto))
                .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void createApplication_WhenServicesNotFound_ShouldThrowException() {
            ApplicationCreateDto dto = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(List.of(1L, 2L))
                .build();

            when(applicantRepository.findByPassportNumber(anyString())).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber(anyString())).thenReturn(Optional.of(testPlate));
            when(serviceRepository.findAllById(anyList())).thenReturn(List.of(AdditionalService.builder().id(1L).build()));

            assertThatThrownBy(() -> applicationService.createApplication(dto))
                .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("Status Change Tests")
    class StatusChangeTests {

        @Test
        void confirmApplication_ShouldConfirm() {
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
        void confirmApplication_WhenNotPending_ShouldThrowException() {
            testApplication.setStatus(ApplicationStatus.CONFIRMED);

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));

            assertThatThrownBy(() -> applicationService.confirmApplication(1L))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        void confirmApplication_WhenReservationExpired_ShouldThrowException() {
            testApplication.setStatus(ApplicationStatus.PENDING);
            testApplication.setReservedUntil(LocalDateTime.now().minusHours(1));

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
            when(applicationRepository.save(testApplication)).thenReturn(testApplication);

            assertThatThrownBy(() -> applicationService.confirmApplication(1L))
                .isInstanceOf(BusinessException.class);
            assertThat(testApplication.getStatus()).isEqualTo(ApplicationStatus.EXPIRED);
        }

        @Test
        void completeApplication_ShouldComplete() {
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
        void completeApplication_WhenNotConfirmed_ShouldThrowException() {
            testApplication.setStatus(ApplicationStatus.PENDING);

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));

            assertThatThrownBy(() -> applicationService.completeApplication(1L))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        void cancelApplication_ShouldCancel() {
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
        void cancelApplication_WhenAlreadyCompleted_ShouldThrowException() {
            testApplication.setStatus(ApplicationStatus.COMPLETED);

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));

            assertThatThrownBy(() -> applicationService.cancelApplication(1L))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        void deleteApplication_ShouldDelete() {
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
            doNothing().when(applicationRepository).delete(testApplication);

            applicationService.deleteApplication(1L);

            verify(applicationRepository).delete(testApplication);
            verify(cacheService).invalidate();
        }
    }

    @Nested
    @DisplayName("Bulk Operations Tests")
    class BulkOperationsTests {

        @Test
        void createBulkApplicationsWithTransaction_ShouldCreateAll() {
            ApplicationCreateDto app1 = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(null)
                .build();

            BulkApplicationCreateDto bulkDto = BulkApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .applications(List.of(app1))
                .build();

            when(applicantRepository.findByPassportNumber(anyString())).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber(anyString())).thenReturn(Optional.of(testPlate));
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(any())).thenReturn(testApplicationDto);

            BulkApplicationResult result = applicationService.createBulkApplicationsWithTransaction(bulkDto);

            assertThat(result.getSuccessful()).isEqualTo(1);
            verify(cacheService).invalidate();
        }

        @Test
        void createBulkApplicationsWithoutTransaction_ShouldCreatePartial() {
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

            when(applicantRepository.findByPassportNumber(anyString())).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
            when(licensePlateRepository.findByPlateNumber("NOT_EXIST")).thenReturn(Optional.empty());
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(any())).thenReturn(testApplicationDto);

            BulkApplicationResult result = applicationService.createBulkApplicationsWithoutTransaction(bulkDto);

            assertThat(result.getSuccessful()).isEqualTo(1);
            assertThat(result.getFailed()).isEqualTo(1);
            verify(cacheService).invalidate();
        }

        @Test
        void createBulkApplications_WithDuplicatePlates_ShouldThrowException() {
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
                .isInstanceOf(BusinessException.class);
        }

        @Test
        void createBulkApplications_WhenApplicantNotFound_ShouldThrowException() {
            ApplicationCreateDto app1 = ApplicationCreateDto.builder()
                .passportNumber("NOT_EXIST")
                .plateNumber("1234 AB-7")
                .build();

            BulkApplicationCreateDto bulkDto = BulkApplicationCreateDto.builder()
                .passportNumber("NOT_EXIST")
                .applications(List.of(app1))
                .build();

            when(applicantRepository.findByPassportNumber("NOT_EXIST")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> applicationService.createBulkApplicationsWithTransaction(bulkDto))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Cache Management Tests")
    class CacheManagementTests {

        @Test
        void invalidateCache_ShouldInvalidate() {
            applicationService.invalidateCache();
            verify(cacheService).invalidate();
        }

        @Test
        void invalidateCacheByRegion_ShouldInvalidate() {
            applicationService.invalidateCacheByRegion("MINSK");
            verify(cacheService).invalidateByRegion("MINSK");
        }

        @Test
        void invalidateCacheByStatus_ShouldInvalidate() {
            applicationService.invalidateCacheByStatus("PENDING");
            verify(cacheService).invalidateByStatus("PENDING");
        }
    }

    @Nested
    @DisplayName("Complete Branch Coverage - All Conditions")
    class CompleteBranchCoverageTests {


        @Test
        void coverServiceIdsCondition_AllBranches() {
            AdditionalService service = AdditionalService.builder().id(1L).price(BigDecimal.TEN).build();


            ApplicationCreateDto dtoNull = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(null)
                .build();

            when(applicantRepository.findByPassportNumber(anyString())).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber(anyString())).thenReturn(Optional.of(testPlate));
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(any())).thenReturn(testApplicationDto);

            applicationService.createApplication(dtoNull);
            verify(serviceRepository, never()).findAllById(any());

            ApplicationCreateDto dtoEmpty = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(Collections.emptyList())
                .build();

            applicationService.createApplication(dtoEmpty);
            verify(serviceRepository, never()).findAllById(any());

            ApplicationCreateDto dtoWith = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(List.of(1L))
                .build();

            when(serviceRepository.findAllById(anyList())).thenReturn(List.of(service));
            when(applicationRepository.save(any(Application.class)))
                .thenReturn(testApplication)
                .thenReturn(testApplication);

            applicationService.createApplication(dtoWith);
            verify(serviceRepository, times(1)).findAllById(anyList());
        }

        @Test
        void coverServicesConditionInSaveApplication_AllBranches() {
            ApplicationCreateDto dtoWithout = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(null)
                .build();

            when(applicantRepository.findByPassportNumber(anyString())).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber(anyString())).thenReturn(Optional.of(testPlate));
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(any())).thenReturn(testApplicationDto);

            applicationService.createApplication(dtoWithout);
            verify(applicationRepository, times(1)).save(any(Application.class));

            AdditionalService service = AdditionalService.builder().id(1L).price(BigDecimal.TEN).build();
            ApplicationCreateDto dtoWith = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(List.of(1L))
                .build();

            when(serviceRepository.findAllById(anyList())).thenReturn(List.of(service));
            when(applicationRepository.save(any(Application.class)))
                .thenReturn(testApplication)
                .thenReturn(testApplication);

            applicationService.createApplication(dtoWith);
            verify(applicationRepository, times(3)).save(any(Application.class));
        }

        @Test
        void coverServicesConditionInCreateSingleApplication_AllBranches() {
            AdditionalService service = AdditionalService.builder().id(1L).price(BigDecimal.TEN).build();

            ApplicationCreateDto appDtoWithout = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(null)
                .build();

            BulkApplicationCreateDto bulkDtoWithout = BulkApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .applications(List.of(appDtoWithout))
                .build();

            when(applicantRepository.findByPassportNumber(anyString())).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber(anyString())).thenReturn(Optional.of(testPlate));
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(any())).thenReturn(testApplicationDto);

            applicationService.createBulkApplicationsWithoutTransaction(bulkDtoWithout);
            verify(applicationRepository, times(1)).save(any(Application.class));

            ApplicationCreateDto appDtoWith = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(List.of(1L))
                .build();

            BulkApplicationCreateDto bulkDtoWith = BulkApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .applications(List.of(appDtoWith))
                .build();

            when(serviceRepository.findAllById(anyList())).thenReturn(List.of(service));
            when(applicationRepository.save(any(Application.class)))
                .thenReturn(testApplication)
                .thenReturn(testApplication);

            applicationService.createBulkApplicationsWithoutTransaction(bulkDtoWith);
            verify(applicationRepository, times(3)).save(any(Application.class));
        }

        @Test
        void coverResultSuccessfulCondition_AllBranches() {
            // Branch 1: successful > 0 (true)
            ApplicationCreateDto successApp = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(null)
                .build();

            BulkApplicationCreateDto successBulk = BulkApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .applications(List.of(successApp))
                .build();

            when(applicantRepository.findByPassportNumber(anyString())).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber(anyString())).thenReturn(Optional.of(testPlate));
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(any())).thenReturn(testApplicationDto);

            applicationService.createBulkApplicationsWithoutTransaction(successBulk);
            verify(cacheService, atLeastOnce()).invalidate();

            ApplicationCreateDto failApp = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("NOT_EXIST")
                .serviceIds(null)
                .build();

            BulkApplicationCreateDto failBulk = BulkApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .applications(List.of(failApp))
                .build();

            when(licensePlateRepository.findByPlateNumber("NOT_EXIST")).thenReturn(Optional.empty());

            applicationService.createBulkApplicationsWithoutTransaction(failBulk);
        }
    }
    @Nested
    @DisplayName("100% Coverage for All 4 Conditions")
    class CompleteCoverageAllConditions {

        @Test
        void coverAllServiceConditions() throws Exception {
            java.lang.reflect.Method saveApplicationMethod = ApplicationService.class
                .getDeclaredMethod("saveApplication",
                    Applicant.class,
                    LicensePlate.class,
                    ApplicationCreateDto.class,
                    List.class);
            saveApplicationMethod.setAccessible(true);

            ApplicationCreateDto dto = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .build();

            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(any())).thenReturn(testApplicationDto);

            saveApplicationMethod.invoke(applicationService, testApplicant, testPlate, dto, null);
            verify(applicationRepository, times(1)).save(any(Application.class));

            List<AdditionalService> emptyServices = Collections.emptyList();
            saveApplicationMethod.invoke(applicationService, testApplicant, testPlate, dto, emptyServices);
            verify(applicationRepository, times(2)).save(any(Application.class));

            AdditionalService service = new AdditionalService();
            service.setId(1L);
            List<AdditionalService> notEmptyServices = List.of(service);

            when(applicationRepository.save(any(Application.class)))
                .thenReturn(testApplication)
                .thenReturn(testApplication);

            saveApplicationMethod.invoke(applicationService, testApplicant, testPlate, dto, notEmptyServices);
            verify(applicationRepository, times(4)).save(any(Application.class));
        }

        @Test
        void coverServiceIdsCondition() {
            AdditionalService service = AdditionalService.builder().id(1L).price(BigDecimal.TEN).build();

            ApplicationCreateDto dtoNull = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(null)
                .build();

            when(applicantRepository.findByPassportNumber(anyString())).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber(anyString())).thenReturn(Optional.of(testPlate));
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(any())).thenReturn(testApplicationDto);

            applicationService.createApplication(dtoNull);
            verify(serviceRepository, never()).findAllById(any());

            ApplicationCreateDto dtoEmpty = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(Collections.emptyList())
                .build();

            applicationService.createApplication(dtoEmpty);
            verify(serviceRepository, never()).findAllById(any());

            ApplicationCreateDto dtoWith = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(List.of(1L))
                .build();

            when(serviceRepository.findAllById(anyList())).thenReturn(List.of(service));
            when(applicationRepository.save(any(Application.class)))
                .thenReturn(testApplication)
                .thenReturn(testApplication);

            applicationService.createApplication(dtoWith);
            verify(serviceRepository, times(1)).findAllById(anyList());
        }

        @Test
        void coverCreateSingleApplicationServicesCondition() {
            AdditionalService service = AdditionalService.builder().id(1L).price(BigDecimal.TEN).build();

            ApplicationCreateDto appDtoWithout = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(null)
                .build();

            BulkApplicationCreateDto bulkDtoWithout = BulkApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .applications(List.of(appDtoWithout))
                .build();

            when(applicantRepository.findByPassportNumber(anyString())).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber(anyString())).thenReturn(Optional.of(testPlate));
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(any())).thenReturn(testApplicationDto);

            applicationService.createBulkApplicationsWithoutTransaction(bulkDtoWithout);
            verify(applicationRepository, times(1)).save(any(Application.class));

            ApplicationCreateDto appDtoWith = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(List.of(1L))
                .build();

            BulkApplicationCreateDto bulkDtoWith = BulkApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .applications(List.of(appDtoWith))
                .build();

            when(serviceRepository.findAllById(anyList())).thenReturn(List.of(service));
            when(applicationRepository.save(any(Application.class)))
                .thenReturn(testApplication)
                .thenReturn(testApplication);

            applicationService.createBulkApplicationsWithoutTransaction(bulkDtoWith);
            verify(applicationRepository, times(3)).save(any(Application.class));
        }

        @Test
        void coverResultSuccessfulCondition() {
            ApplicationCreateDto successApp = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(null)
                .build();

            BulkApplicationCreateDto successBulk = BulkApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .applications(List.of(successApp))
                .build();

            when(applicantRepository.findByPassportNumber(anyString())).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber(anyString())).thenReturn(Optional.of(testPlate));
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(any())).thenReturn(testApplicationDto);

            applicationService.createBulkApplicationsWithoutTransaction(successBulk);
            verify(cacheService, atLeastOnce()).invalidate();

            ApplicationCreateDto failApp = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("NOT_EXIST")
                .serviceIds(null)
                .build();

            BulkApplicationCreateDto failBulk = BulkApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .applications(List.of(failApp))
                .build();

            when(licensePlateRepository.findByPlateNumber("NOT_EXIST")).thenReturn(Optional.empty());

            BulkApplicationResult result = applicationService.createBulkApplicationsWithoutTransaction(failBulk);
            assertThat(result.getSuccessful()).isZero();
        }

        @Test
        void coverResultSuccessfulConditionWithTransaction() {
            ApplicationCreateDto successApp = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(null)
                .build();

            BulkApplicationCreateDto successBulk = BulkApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .applications(List.of(successApp))
                .build();

            when(applicantRepository.findByPassportNumber(anyString())).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber(anyString())).thenReturn(Optional.of(testPlate));
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toDto(any())).thenReturn(testApplicationDto);

            applicationService.createBulkApplicationsWithTransaction(successBulk);
            verify(cacheService, atLeastOnce()).invalidate();
        }

        @Test
        void coverAllFourConditionsTogether() {
            AdditionalService service = AdditionalService.builder().id(1L).price(BigDecimal.TEN).build();

            ApplicationCreateDto dto = ApplicationCreateDto.builder()
                .passportNumber("MP1234567")
                .plateNumber("1234 AB-7")
                .serviceIds(List.of(1L))
                .build();

            when(applicantRepository.findByPassportNumber(anyString())).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber(anyString())).thenReturn(Optional.of(testPlate));
            when(serviceRepository.findAllById(anyList())).thenReturn(List.of(service));
            when(applicationRepository.save(any(Application.class)))
                .thenReturn(testApplication)
                .thenReturn(testApplication);
            when(applicationMapper.toDto(any())).thenReturn(testApplicationDto);

            applicationService.createApplication(dto);

            verify(applicationRepository, times(2)).save(any(Application.class));
            verify(serviceRepository, times(1)).findAllById(anyList());
            verify(cacheService, atLeastOnce()).invalidate();
        }
    }
}