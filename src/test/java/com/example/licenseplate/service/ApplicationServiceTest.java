package com.example.licenseplate.service;

import com.example.licenseplate.cache.ApplicationCacheService;
import com.example.licenseplate.dto.request.ApplicationCreateDto;
import com.example.licenseplate.dto.request.BulkApplicationCreateDto;
import com.example.licenseplate.dto.response.ApplicationDto;
import com.example.licenseplate.dto.response.BulkApplicationResult;
import com.example.licenseplate.exception.BusinessException;
import com.example.licenseplate.exception.ResourceNotFoundException;
import com.example.licenseplate.mapper.ApplicationMapper;
import com.example.licenseplate.model.entity.*;
import com.example.licenseplate.model.enums.ApplicationStatus;
import com.example.licenseplate.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private ApplicantRepository applicantRepository;
    @Mock private LicensePlateRepository licensePlateRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private ApplicationMapper applicationMapper;
    @Mock private ApplicationCacheService cacheService;
    @Mock private DepartmentRepository departmentRepository;

    @InjectMocks
    private ApplicationService applicationService;

    private Applicant testApplicant;
    private LicensePlate testPlate;
    private Application testApp;

    @BeforeEach
    void setUp() {
        testApplicant = Applicant.builder().id(1L).passportNumber("MP123").fullName("Test").build();
        testPlate = LicensePlate.builder()
            .id(1L).plateNumber("1234AB-7").price(BigDecimal.valueOf(100))
            .department(new RegistrationDept())
            .applications(new ArrayList<>()).build();
        testApp = new Application();
        testApp.setId(100L);
        testApp.setStatus(ApplicationStatus.PENDING);
        testApp.setReservedUntil(LocalDateTime.now().plusHours(1));
        testApp.setLicensePlate(testPlate);
    }

    @Nested
    @DisplayName("Read Operations Coverage")
    class ReadOperations {

        @Test
        void getAllApplications_ShouldWork() {
            when(applicationRepository.findAll()).thenReturn(List.of(testApp));
            applicationService.getAllApplications();
            verify(applicationMapper).toDtoList(any());
        }

        @Test
        void getApplicationById_NotFound_Throws() {
            when(applicationRepository.findById(any())).thenReturn(Optional.empty());
            assertThatThrownBy(() -> applicationService.getApplicationById(1L)).isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void getApplicationWithDetails_NotFound_Throws() {
            when(applicationRepository.findByIdWithDetails(any())).thenReturn(Optional.empty());
            assertThatThrownBy(() -> applicationService.getApplicationWithDetails(1L)).isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void getApplicationsByPassport_ApplicantExistsButNoApps_ShouldLog() {
            when(applicationRepository.findByApplicantPassport("MP123")).thenReturn(Collections.emptyList());
            when(applicantRepository.findByPassportNumber("MP123")).thenReturn(Optional.of(testApplicant));
            applicationService.getApplicationsByPassport("MP123");
            verify(applicationMapper).toDtoList(anyList());
        }

        @Test
        void getApplicationsByPassport_NoApplicant_Throws() {
            when(applicationRepository.findByApplicantPassport("EMPTY")).thenReturn(Collections.emptyList());
            when(applicantRepository.findByPassportNumber("EMPTY")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> applicationService.getApplicationsByPassport("EMPTY")).isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void getApplicationsByStatusAndRegion_RegionNotFound_Throws() {
            when(departmentRepository.existsByRegion("X")).thenReturn(false);
            assertThatThrownBy(() -> applicationService.getApplicationsByStatusAndRegion(ApplicationStatus.PENDING, "X")).isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void getApplicationsByStatusAndRegion_NoAppsFound_Throws() {
            when(departmentRepository.existsByRegion("Y")).thenReturn(true);
            when(applicationRepository.findByStatusAndDepartmentRegion(any(), any())).thenReturn(Collections.emptyList());
            assertThatThrownBy(() -> applicationService.getApplicationsByStatusAndRegion(ApplicationStatus.PENDING, "Y")).isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void getApplicationsByStatusAndRegionNative_RegionNotFound_Throws() {
            when(departmentRepository.existsByRegion("X")).thenReturn(false);
            assertThatThrownBy(() -> applicationService.getApplicationsByStatusAndRegionNative(ApplicationStatus.PENDING, "X")).isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void getApplicationsByStatusAndRegionNative_NoAppsFound_Throws() {
            when(departmentRepository.existsByRegion("Y")).thenReturn(true);
            when(applicationRepository.findByStatusAndDepartmentRegionNative(any(), any())).thenReturn(Collections.emptyList());
            assertThatThrownBy(() -> applicationService.getApplicationsByStatusAndRegionNative(ApplicationStatus.PENDING, "Y")).isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void getApplicationsByStatusAndRegionCached_CacheEmptyBranch() {
            // Покрывает ветку if (cached != null && cached.isEmpty())
            when(cacheService.get(any(), any())).thenReturn(Collections.emptyList());
            when(applicationRepository.findByStatusAndDepartmentRegion(any(), any())).thenReturn(Collections.emptyList());
            applicationService.getApplicationsByStatusAndRegionCached(ApplicationStatus.PENDING, "BY");
            verify(cacheService).invalidate();
        }

        @Test
        void getApplicationsByStatusAndRegionCached_CacheValidBranch() {
            when(cacheService.get(any(), any())).thenReturn(List.of(new ApplicationDto()));
            applicationService.getApplicationsByStatusAndRegionCached(ApplicationStatus.PENDING, "BY");
            verify(applicationRepository, never()).findByStatusAndDepartmentRegion(any(), any());
        }

        @Test
        void getApplicationsByPassportPaginated_Page0EmptyBranch() {
            Pageable p = PageRequest.of(0, 10);
            when(applicationRepository.findByApplicantPassport(any(), any())).thenReturn(Page.empty(p));
            applicationService.getApplicationsByPassportPaginated("MP123", p);
            verify(applicationRepository).findByApplicantPassport("MP123", p);
        }
    }

    @Nested
    @DisplayName("Creation Operations Coverage")
    class CreationOperations {

        @Test
        void createApplication_FullFlow_Success() {
            ApplicationCreateDto dto = ApplicationCreateDto.builder().passportNumber("MP123").plateNumber("1234").serviceIds(List.of(1L)).build();
            when(applicantRepository.findByPassportNumber(any())).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber(any())).thenReturn(Optional.of(testPlate));
            when(serviceRepository.findAllById(any())).thenReturn(List.of(AdditionalService.builder().id(1L).price(BigDecimal.TEN).build()));
            when(applicationRepository.save(any())).thenReturn(testApp);

            applicationService.createApplication(dto);
            verify(cacheService).invalidate();
            verify(applicationRepository, times(2)).save(any());
        }

        @Test
        void createApplicationWithoutTransaction_NewApplicant_Success() {
            ApplicationCreateDto dto = ApplicationCreateDto.builder().passportNumber("NEW").plateNumber("1234").build();
            when(applicantRepository.findByPassportNumber("NEW")).thenReturn(Optional.empty());
            when(applicantRepository.save(any(Applicant.class))).thenReturn(testApplicant);
            when(licensePlateRepository.findByPlateNumber(any())).thenReturn(Optional.of(testPlate));
            when(licensePlateRepository.isPlateAvailable(any())).thenReturn(true);
            when(applicationRepository.save(any())).thenReturn(testApp);

            applicationService.createApplicationWithoutTransaction(dto);
            verify(applicantRepository).save(any(Applicant.class));
        }

        @Test
        void validatePlate_Transactional_ThrowsIfUnavailable() {
            testPlate.setApplications(List.of(new Application())); // Недоступна
            ApplicationCreateDto dto = ApplicationCreateDto.builder().passportNumber("MP123").plateNumber("1234").build();
            when(applicantRepository.findByPassportNumber(any())).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber(any())).thenReturn(Optional.of(testPlate));

            assertThatThrownBy(() -> applicationService.createApplicationWithTransaction(dto)).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void getServices_MismatchSize_NonTransactional_Throws() {
            ApplicationCreateDto dto = ApplicationCreateDto.builder().passportNumber("MP123").plateNumber("1234").serviceIds(List.of(1L, 2L)).build();
            when(applicantRepository.findByPassportNumber(any())).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber(any())).thenReturn(Optional.of(testPlate));
            when(serviceRepository.findAllById(any())).thenReturn(List.of(new AdditionalService())); // Нашли только 1 из 2

            assertThatThrownBy(() -> applicationService.createApplicationWithoutTransaction(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inconsistent");
        }
    }

    @Nested
    @DisplayName("Status Changes Coverage")
    class StatusChanges {

        @Test
        void confirmApplication_Expired_ShouldExpireAndThrow() {
            testApp.setReservedUntil(LocalDateTime.now().minusMinutes(10));
            when(applicationRepository.findById(any())).thenReturn(Optional.of(testApp));

            assertThatThrownBy(() -> applicationService.confirmApplication(100L)).isInstanceOf(BusinessException.class).hasMessageContaining("истекло");
            assertThat(testApp.getStatus()).isEqualTo(ApplicationStatus.EXPIRED);
            verify(applicationRepository).save(testApp);
        }

        @Test
        void confirmApplication_WrongStatus_Throws() {
            testApp.setStatus(ApplicationStatus.CANCELLED);
            when(applicationRepository.findById(any())).thenReturn(Optional.of(testApp));
            assertThatThrownBy(() -> applicationService.confirmApplication(100L)).isInstanceOf(BusinessException.class);
        }

        @Test
        void completeApplication_Success_UpdatesPlate() {
            testApp.setStatus(ApplicationStatus.CONFIRMED);
            when(applicationRepository.findById(any())).thenReturn(Optional.of(testApp));
            when(applicationRepository.save(any())).thenReturn(testApp);

            applicationService.completeApplication(100L);
            assertThat(testApp.getLicensePlate().getIssueDate()).isNotNull();
        }

        @Test
        void cancelApplication_AlreadyFinal_Throws() {
            testApp.setStatus(ApplicationStatus.COMPLETED);
            when(applicationRepository.findById(any())).thenReturn(Optional.of(testApp));
            assertThatThrownBy(() -> applicationService.cancelApplication(100L)).isInstanceOf(BusinessException.class);
        }

        @Test
        void deleteApplication_ShouldDelete() {
            when(applicationRepository.findById(any())).thenReturn(Optional.of(testApp));
            applicationService.deleteApplication(100L);
            verify(applicationRepository).delete(testApp);
        }
    }

    @Nested
    @DisplayName("Bulk Operations Coverage")
    class BulkOperations {

        @Test
        void processBulk_DuplicatePlates_Throws() {
            ApplicationCreateDto d = ApplicationCreateDto.builder().plateNumber("DUP").build();
            BulkApplicationCreateDto bulk = BulkApplicationCreateDto.builder().applications(List.of(d, d)).build();
            assertThatThrownBy(() -> applicationService.createBulkApplicationsWithTransaction(bulk)).isInstanceOf(BusinessException.class);
        }

        @Test
        void processBulk_NoApplicant_Throws() {
            BulkApplicationCreateDto bulk = BulkApplicationCreateDto.builder().passportNumber("X").applications(List.of()).build();
            when(applicantRepository.findByPassportNumber("X")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> applicationService.createBulkApplicationsWithoutTransaction(bulk)).isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void processBulk_NonTransactional_PartialFail() {
            ApplicationCreateDto ok = ApplicationCreateDto.builder().plateNumber("OK").build();
            ApplicationCreateDto fail = ApplicationCreateDto.builder().plateNumber("FAIL").build();
            BulkApplicationCreateDto bulk = BulkApplicationCreateDto.builder().passportNumber("MP").applications(List.of(ok, fail)).build();

            when(applicantRepository.findByPassportNumber("MP")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("OK")).thenReturn(Optional.of(testPlate));
            when(licensePlateRepository.findByPlateNumber("FAIL")).thenThrow(new RuntimeException("Error"));
            when(applicationRepository.save(any())).thenReturn(testApp);

            BulkApplicationResult result = applicationService.createBulkApplicationsWithoutTransaction(bulk);
            assertThat(result.getSuccessful()).isEqualTo(1);
            assertThat(result.getFailed()).isEqualTo(1);
        }

        @Test
        void processBulk_Transactional_FailFast() {
            ApplicationCreateDto fail = ApplicationCreateDto.builder().plateNumber("FAIL").build();
            BulkApplicationCreateDto bulk = BulkApplicationCreateDto.builder().passportNumber("MP").applications(List.of(fail)).build();
            when(applicantRepository.findByPassportNumber("MP")).thenReturn(Optional.of(testApplicant));
            when(licensePlateRepository.findByPlateNumber("FAIL")).thenThrow(new RuntimeException("Fatal"));

            assertThatThrownBy(() -> applicationService.createBulkApplicationsWithTransaction(bulk)).isInstanceOf(BusinessException.class);
        }
    }

    @Test
    void cacheMethods_Coverage() {
        applicationService.invalidateCache();
        applicationService.invalidateCacheByRegion("R");
        applicationService.invalidateCacheByStatus("S");
        verify(cacheService, times(1)).invalidate();
        verify(cacheService).invalidateByRegion("R");
        verify(cacheService).invalidateByStatus("S");
    }
}