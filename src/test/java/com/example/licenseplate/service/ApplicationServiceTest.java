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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
    private ApplicationCacheService cacheService;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private ApplicationService applicationService;

    private Applicant testApplicant;
    private LicensePlate testPlate;
    private RegistrationDept testDepartment;
    private Application testApplication;
    private ApplicationDto testApplicationDto;
    private ApplicationCreateDto testCreateDto;
    private AdditionalService testService;

    @BeforeEach
    void setUp() {
        testDepartment = RegistrationDept.builder()
            .id(1L)
            .name("Test Department")
            .region("Moscow")
            .address("Test Address")
            .phoneNumber("+375291234567")
            .build();

        testApplicant = Applicant.builder()
            .id(1L)
            .fullName("Ivan Petrov")
            .passportNumber("MP1234567")
            .phoneNumber("+375291234567")
            .email("ivan@example.com")
            .address("Test Address")
            .build();

        testPlate = LicensePlate.builder()
            .id(1L)
            .plateNumber("1234 AB-7")
            .price(BigDecimal.valueOf(1000))
            .series("AB")
            .department(testDepartment)
            .build();

        testApplication = Application.builder()
            .id(1L)
            .applicant(testApplicant)
            .licensePlate(testPlate)
            .department(testDepartment)
            .status(ApplicationStatus.PENDING)
            .submissionDate(LocalDateTime.now())
            .reservedUntil(LocalDateTime.now().plusHours(1))
            .vehicleVin("VIN123456789")
            .vehicleModel("Toyota Camry")
            .vehicleYear(2022)
            .notes("Test notes")
            .paymentAmount(BigDecimal.valueOf(1000))
            .build();

        testApplicationDto = ApplicationDto.builder()
            .id(1L)
            .applicantId(1L)
            .applicantName("Ivan Petrov")
            .applicantPassport("MP1234567")
            .licensePlateNumber("1234 AB-7")
            .status(ApplicationStatus.PENDING.name())
            .paymentAmount(BigDecimal.valueOf(1000))
            .build();

        testService = AdditionalService.builder()
            .id(1L)
            .name("Express Service")
            .description("Fast processing")
            .price(BigDecimal.valueOf(500))
            .isAvailable(true)
            .build();

        testCreateDto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .plateNumber("1234 AB-7")
            .vehicleVin("VIN123456789")
            .vehicleModel("Toyota Camry")
            .vehicleYear(2022)
            .notes("Test notes")
            .serviceIds(Collections.singletonList(1L))
            .build();
    }

    @Test
    void getAllApplications_ShouldReturnListOfApplicationDtos() {
        List<Application> applications = Collections.singletonList(testApplication);
        List<ApplicationDto> expectedDtos = Collections.singletonList(testApplicationDto);

        when(applicationRepository.findAll()).thenReturn(applications);
        when(applicationMapper.toDtoList(applications)).thenReturn(expectedDtos);

        List<ApplicationDto> result = applicationService.getAllApplications();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo(testApplicationDto);
        verify(applicationRepository).findAll();
        verify(applicationMapper).toDtoList(applications);
    }

    @Test
    void getAllApplications_WhenEmptyList_ShouldReturnEmptyList() {
        when(applicationRepository.findAll()).thenReturn(Collections.emptyList());
        when(applicationMapper.toDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

        List<ApplicationDto> result = applicationService.getAllApplications();

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(applicationRepository).findAll();
    }


    @Test
    void getApplicationById_WhenApplicationExists_ShouldReturnApplicationDto() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

        ApplicationDto result = applicationService.getApplicationById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(applicationRepository).findById(1L);
        verify(applicationMapper).toDto(testApplication);
    }

    @Test
    void getApplicationById_WhenApplicationNotFound_ShouldThrowResourceNotFoundException() {
        when(applicationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.getApplicationById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Заявление не найдено с id: 99");

        verify(applicationRepository).findById(99L);
        verify(applicationMapper, never()).toDto(any());
    }


    @Test
    void getApplicationWithDetails_WhenApplicationExists_ShouldReturnApplicationDto() {
        when(applicationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(testApplication));
        when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

        ApplicationDto result = applicationService.getApplicationWithDetails(1L);

        assertThat(result).isNotNull();
        verify(applicationRepository).findByIdWithDetails(1L);
        verify(applicationMapper).toDto(testApplication);
    }

    @Test
    void getApplicationWithDetails_WhenApplicationNotFound_ShouldThrowResourceNotFoundException() {
        when(applicationRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.getApplicationWithDetails(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Заявление не найдено с id: 99");

        verify(applicationRepository).findByIdWithDetails(99L);
    }


    @Test
    void getApplicationsByPassport_WhenApplicationsExist_ShouldReturnList() {
        List<Application> applications = Collections.singletonList(testApplication);
        List<ApplicationDto> expectedDtos = Collections.singletonList(testApplicationDto);

        when(applicationRepository.findByApplicantPassport("MP1234567")).thenReturn(applications);
        when(applicationMapper.toDtoList(applications)).thenReturn(expectedDtos);

        List<ApplicationDto> result = applicationService.getApplicationsByPassport("MP1234567");

        assertThat(result).hasSize(1);
        verify(applicationRepository).findByApplicantPassport("MP1234567");
        verify(applicantRepository, never()).findByPassportNumber(any());
    }

    @Test
    void getApplicationsByPassport_WhenNoApplicationsAndApplicantExists_ShouldReturnEmptyList() {
        when(applicationRepository.findByApplicantPassport("MP1234567")).thenReturn(Collections.emptyList());
        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
        when(applicationMapper.toDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

        List<ApplicationDto> result = applicationService.getApplicationsByPassport("MP1234567");

        assertThat(result).isEmpty();
        verify(applicationRepository).findByApplicantPassport("MP1234567");
        verify(applicantRepository).findByPassportNumber("MP1234567");
    }

    @Test
    void getApplicationsByPassport_WhenApplicantNotFound_ShouldThrowResourceNotFoundException() {
        when(applicationRepository.findByApplicantPassport("MP9999999")).thenReturn(Collections.emptyList());
        when(applicantRepository.findByPassportNumber("MP9999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.getApplicationsByPassport("MP9999999"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Заявитель с паспортом 'MP9999999' не найден");

        verify(applicationRepository).findByApplicantPassport("MP9999999");
        verify(applicantRepository).findByPassportNumber("MP9999999");
    }

    @Test
    void getApplicationsByStatusAndRegion_WhenApplicationsExist_ShouldReturnList() {
        List<Application> applications = Collections.singletonList(testApplication);
        List<ApplicationDto> expectedDtos = Collections.singletonList(testApplicationDto);

        when(departmentRepository.existsByRegion("Moscow")).thenReturn(true);
        when(applicationRepository.findByStatusAndDepartmentRegion(ApplicationStatus.PENDING, "Moscow"))
            .thenReturn(applications);
        when(applicationMapper.toDtoList(applications)).thenReturn(expectedDtos);

        List<ApplicationDto> result = applicationService.getApplicationsByStatusAndRegion(
            ApplicationStatus.PENDING, "Moscow");

        assertThat(result).hasSize(1);
        verify(departmentRepository).existsByRegion("Moscow");
        verify(applicationRepository).findByStatusAndDepartmentRegion(ApplicationStatus.PENDING, "Moscow");
    }

    @Test
    void getApplicationsByStatusAndRegion_WhenRegionNotFound_ShouldThrowResourceNotFoundException() {
        when(departmentRepository.existsByRegion("InvalidRegion")).thenReturn(false);

        assertThatThrownBy(() -> applicationService.getApplicationsByStatusAndRegion(
            ApplicationStatus.PENDING, "InvalidRegion"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Регион 'InvalidRegion' не найден");

        verify(departmentRepository).existsByRegion("InvalidRegion");
        verify(applicationRepository, never()).findByStatusAndDepartmentRegion(any(), any());
    }

    @Test
    void getApplicationsByStatusAndRegion_WhenNoApplicationsFound_ShouldThrowResourceNotFoundException() {
        when(departmentRepository.existsByRegion("Moscow")).thenReturn(true);
        when(applicationRepository.findByStatusAndDepartmentRegion(ApplicationStatus.PENDING, "Moscow"))
            .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> applicationService.getApplicationsByStatusAndRegion(
            ApplicationStatus.PENDING, "Moscow"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Заявки со статусом 'PENDING' в регионе 'Moscow' не найдены");

        verify(departmentRepository).existsByRegion("Moscow");
        verify(applicationRepository).findByStatusAndDepartmentRegion(ApplicationStatus.PENDING, "Moscow");
    }


    @Test
    void getApplicationsByStatusAndRegionNative_WhenApplicationsExist_ShouldReturnList() {
        List<Application> applications = Collections.singletonList(testApplication);
        List<ApplicationDto> expectedDtos = Collections.singletonList(testApplicationDto);

        when(departmentRepository.existsByRegion("Moscow")).thenReturn(true);
        when(applicationRepository.findByStatusAndDepartmentRegionNative("PENDING", "Moscow"))
            .thenReturn(applications);
        when(applicationMapper.toDtoList(applications)).thenReturn(expectedDtos);

        List<ApplicationDto> result = applicationService.getApplicationsByStatusAndRegionNative(
            ApplicationStatus.PENDING, "Moscow");

        assertThat(result).hasSize(1);
        verify(departmentRepository).existsByRegion("Moscow");
        verify(applicationRepository).findByStatusAndDepartmentRegionNative("PENDING", "Moscow");
    }

    @Test
    void getApplicationsByStatusAndRegionNative_WhenRegionNotFound_ShouldThrowResourceNotFoundException() {
        when(departmentRepository.existsByRegion("InvalidRegion")).thenReturn(false);

        assertThatThrownBy(() -> applicationService.getApplicationsByStatusAndRegionNative(
            ApplicationStatus.PENDING, "InvalidRegion"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Регион 'InvalidRegion' не найден");
    }

    @Test
    void getApplicationsByStatusAndRegionNative_WhenNoApplicationsFound_ShouldThrowResourceNotFoundException() {
        when(departmentRepository.existsByRegion("Moscow")).thenReturn(true);
        when(applicationRepository.findByStatusAndDepartmentRegionNative("PENDING", "Moscow"))
            .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> applicationService.getApplicationsByStatusAndRegionNative(
            ApplicationStatus.PENDING, "Moscow"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Заявки со статусом 'PENDING' в регионе 'Moscow' не найдены");
    }


    @Test
    void getApplicationsByStatusAndRegionCached_WhenInCache_ShouldReturnCachedResult() {
        List<ApplicationDto> cachedDtos = Collections.singletonList(testApplicationDto);

        when(cacheService.get("PENDING", "Moscow")).thenReturn(cachedDtos);

        List<ApplicationDto> result = applicationService.getApplicationsByStatusAndRegionCached(
            ApplicationStatus.PENDING, "Moscow");

        assertThat(result).hasSize(1);
        verify(cacheService).get("PENDING", "Moscow");
        verify(applicationRepository, never()).findByStatusAndDepartmentRegion(any(), any());
    }

    @Test
    void getApplicationsByStatusAndRegionCached_WhenCacheIsEmptyAndNoResults_ShouldReturnEmptyList() {
        when(cacheService.get("PENDING", "Moscow")).thenReturn(Collections.emptyList());
        when(applicationRepository.findByStatusAndDepartmentRegion(ApplicationStatus.PENDING, "Moscow"))
            .thenReturn(Collections.emptyList());
        when(applicationMapper.toDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

        List<ApplicationDto> result = applicationService.getApplicationsByStatusAndRegionCached(
            ApplicationStatus.PENDING, "Moscow");

        assertThat(result).isEmpty();
        verify(cacheService).get("PENDING", "Moscow");
        verify(cacheService).invalidate();
        verify(applicationRepository).findByStatusAndDepartmentRegion(ApplicationStatus.PENDING, "Moscow");
        verify(cacheService, never()).put(any(), any(), any());
        verify(departmentRepository, never()).existsByRegion(anyString());
    }

    @Test
    void getApplicationsByStatusAndRegionCached_WhenNotInCache_ShouldFetchAndCache() {
        List<Application> applications = Collections.singletonList(testApplication);
        List<ApplicationDto> expectedDtos = Collections.singletonList(testApplicationDto);

        when(cacheService.get("PENDING", "Moscow")).thenReturn(null);
        when(applicationRepository.findByStatusAndDepartmentRegion(ApplicationStatus.PENDING, "Moscow"))
            .thenReturn(applications);
        when(applicationMapper.toDtoList(applications)).thenReturn(expectedDtos);

        List<ApplicationDto> result = applicationService.getApplicationsByStatusAndRegionCached(
            ApplicationStatus.PENDING, "Moscow");

        assertThat(result).hasSize(1);
        verify(cacheService).get("PENDING", "Moscow");
        verify(applicationRepository).findByStatusAndDepartmentRegion(ApplicationStatus.PENDING, "Moscow");
        verify(cacheService).put("PENDING", "Moscow", expectedDtos);
        // Проверяем, что метод existsByRegion НЕ вызывается
        verify(departmentRepository, never()).existsByRegion(anyString());
    }


    @Test
    void getApplicationsByPassportPaginated_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Application> applicationPage = new PageImpl<>(
            Collections.singletonList(testApplication), pageable, 1);

        when(applicationRepository.findByApplicantPassport("MP1234567", pageable))
            .thenReturn(applicationPage);
        when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

        Page<ApplicationDto> result = applicationService.getApplicationsByPassportPaginated(
            "MP1234567", pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(applicationRepository).findByApplicantPassport("MP1234567", pageable);
    }

    @Test
    void getApplicationsByPassportPaginated_WhenEmptyPage_ShouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Application> emptyPage = Page.empty(pageable);

        when(applicationRepository.findByApplicantPassport("MP1234567", pageable))
            .thenReturn(emptyPage);

        Page<ApplicationDto> result = applicationService.getApplicationsByPassportPaginated(
            "MP1234567", pageable);

        assertThat(result).isEmpty();
        verify(applicationRepository).findByApplicantPassport("MP1234567", pageable);
    }


    @Test
    void invalidateCache_ShouldCallCacheService() {
        applicationService.invalidateCache();
        verify(cacheService).invalidate();
    }

    @Test
    void invalidateCacheByRegion_ShouldCallCacheService() {
        applicationService.invalidateCacheByRegion("Moscow");
        verify(cacheService).invalidateByRegion("Moscow");
    }

    @Test
    void invalidateCacheByStatus_ShouldCallCacheService() {
        applicationService.invalidateCacheByStatus("PENDING");
        verify(cacheService).invalidateByStatus("PENDING");
    }


    @Test
    void createApplication_ShouldCreateAndInvalidateCache() {
        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
        when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
        when(serviceRepository.findAllById(Collections.singletonList(1L)))
            .thenReturn(Collections.singletonList(testService));
        when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
        when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

        ApplicationDto result = applicationService.createApplication(testCreateDto);

        assertThat(result).isNotNull();
        verify(applicationRepository, times(2)).save(any(Application.class));
        verify(cacheService).invalidate();
    }

    @Test
    void createApplication_WhenPlateNotAvailable_ShouldThrowException() {
        Application existingApplication = Application.builder()
            .id(2L)
            .status(ApplicationStatus.PENDING)
            .licensePlate(testPlate)
            .build();
        testPlate.setApplications(Collections.singletonList(existingApplication));

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
        when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));

        assertThatThrownBy(() -> applicationService.createApplication(testCreateDto))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Номерной знак '1234 AB-7' недоступен");

        verify(applicationRepository, never()).save(any());
        verify(serviceRepository, never()).findAllById(any());
    }

    @Test
    void createApplication_WhenServicesNotFound_ShouldThrowBusinessException() {
        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
        when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
        when(serviceRepository.findAllById(Collections.singletonList(1L)))
            .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> applicationService.createApplication(testCreateDto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("Некоторые услуги не найдены - transaction will rollback! Application will not be saved.");

        verify(applicationRepository, never()).save(any());
    }


    @Test
    void createApplicationWithoutTransaction_ShouldCreateWithoutTransactional() {
        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
        when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
        when(licensePlateRepository.isPlateAvailable(testPlate.getId())).thenReturn(true);
        when(serviceRepository.findAllById(Collections.singletonList(1L)))
            .thenReturn(Collections.singletonList(testService));
        when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
        when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

        ApplicationDto result = applicationService.createApplicationWithoutTransaction(testCreateDto);

        assertThat(result).isNotNull();
        verify(applicationRepository, times(2)).save(any(Application.class));
        verify(cacheService).invalidate();
    }

    @Test
    void createApplicationWithoutTransaction_WhenServicesNotFound_ShouldThrowBusinessExceptionWithoutRollback() {
        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
        when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
        when(licensePlateRepository.isPlateAvailable(testPlate.getId())).thenReturn(true);
        when(serviceRepository.findAllById(Collections.singletonList(1L)))
            .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> applicationService.createApplicationWithoutTransaction(testCreateDto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("Некоторые услуги не найдены - but application is already saved! Data is now inconsistent!");

        verify(applicationRepository, never()).save(any());
    }


    @Test
    void createApplicationWithTransaction_ShouldCreateWithTransactional() {
        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
        when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
        when(serviceRepository.findAllById(Collections.singletonList(1L)))
            .thenReturn(Collections.singletonList(testService));
        when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
        when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

        ApplicationDto result = applicationService.createApplicationWithTransaction(testCreateDto);

        assertThat(result).isNotNull();
        verify(applicationRepository, times(2)).save(any(Application.class));
        verify(cacheService).invalidate();
    }


    @Test
    void confirmApplication_WhenValid_ShouldConfirm() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
        when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

        ApplicationDto result = applicationService.confirmApplication(1L);

        assertThat(result).isNotNull();
        assertThat(testApplication.getStatus()).isEqualTo(ApplicationStatus.CONFIRMED);
        assertThat(testApplication.getConfirmationDate()).isNotNull();
        verify(applicationRepository).save(testApplication);
        verify(cacheService).invalidate();
    }

    @Test
    void confirmApplication_WhenNotPending_ShouldThrowBusinessException() {
        testApplication.setStatus(ApplicationStatus.CONFIRMED);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));

        assertThatThrownBy(() -> applicationService.confirmApplication(1L))
            .isInstanceOf(BusinessException.class)
            .hasMessage("Заявление не в статусе PENDING: CONFIRMED");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void confirmApplication_WhenReservationExpired_ShouldExpireAndThrowException() {
        testApplication.setReservedUntil(LocalDateTime.now().minusMinutes(1));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(applicationRepository.save(testApplication)).thenReturn(testApplication);

        assertThatThrownBy(() -> applicationService.confirmApplication(1L))
            .isInstanceOf(BusinessException.class)
            .hasMessage("Время бронирования истекло");

        assertThat(testApplication.getStatus()).isEqualTo(ApplicationStatus.EXPIRED);
        verify(applicationRepository).save(testApplication);
    }


    @Test
    void completeApplication_WhenConfirmed_ShouldComplete() {
        testApplication.setStatus(ApplicationStatus.CONFIRMED);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
        when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

        ApplicationDto result = applicationService.completeApplication(1L);

        assertThat(result).isNotNull();
        assertThat(testApplication.getStatus()).isEqualTo(ApplicationStatus.COMPLETED);
        assertThat(testPlate.getIssueDate()).isNotNull();
        assertThat(testPlate.getExpiryDate()).isNotNull();
        verify(licensePlateRepository).save(testPlate);
        verify(applicationRepository).save(testApplication);
        verify(cacheService).invalidate();
    }

    @Test
    void completeApplication_WhenNotConfirmed_ShouldThrowBusinessException() {
        testApplication.setStatus(ApplicationStatus.PENDING);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));

        assertThatThrownBy(() -> applicationService.completeApplication(1L))
            .isInstanceOf(BusinessException.class)
            .hasMessage("Application is not in CONFIRMED status: PENDING");

        verify(licensePlateRepository, never()).save(any());
        verify(applicationRepository, never()).save(any());
    }


    @Test
    void cancelApplication_WhenPending_ShouldCancel() {
        testApplication.setStatus(ApplicationStatus.PENDING);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
        when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

        ApplicationDto result = applicationService.cancelApplication(1L);

        assertThat(result).isNotNull();
        assertThat(testApplication.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
        verify(applicationRepository).save(testApplication);
        verify(cacheService).invalidate();
    }

    @Test
    void cancelApplication_WhenConfirmed_ShouldCancel() {
        testApplication.setStatus(ApplicationStatus.CONFIRMED);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
        when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

        ApplicationDto result = applicationService.cancelApplication(1L);

        assertThat(result).isNotNull();
        assertThat(testApplication.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
    }

    @ParameterizedTest
    @EnumSource(value = ApplicationStatus.class, names = {"COMPLETED", "CANCELLED"})
    void cancelApplication_WhenCompletedOrCancelled_ShouldThrowBusinessException(ApplicationStatus status) {
        testApplication.setStatus(status);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));

        assertThatThrownBy(() -> applicationService.cancelApplication(1L))
            .isInstanceOf(BusinessException.class)
            .hasMessage(String.format("Нельзя отменить заявление в статусе: %s", status));

        verify(applicationRepository, never()).save(any());
    }


    @Test
    void deleteApplication_WhenExists_ShouldDelete() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        doNothing().when(applicationRepository).delete(testApplication);

        applicationService.deleteApplication(1L);

        verify(applicationRepository).findById(1L);
        verify(applicationRepository).delete(testApplication);
        verify(cacheService).invalidate();
    }

    @Test
    void deleteApplication_WhenNotFound_ShouldThrowResourceNotFoundException() {
        when(applicationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.deleteApplication(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Заявление не найдено с id: 99");

        verify(applicationRepository, never()).delete(any());
    }


    @Test
    void createBulkApplicationsWithTransaction_ShouldCreateAll() {
        BulkApplicationCreateDto bulkDto = BulkApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .applications(Collections.singletonList(testCreateDto))
            .build();

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
        when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
        when(serviceRepository.findAllById(Collections.singletonList(1L)))
            .thenReturn(Collections.singletonList(testService));
        when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
        when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

        BulkApplicationResult result = applicationService.createBulkApplicationsWithTransaction(bulkDto);

        assertThat(result).isNotNull();
        assertThat(result.getTotalRequested()).isEqualTo(1);
        assertThat(result.getSuccessful()).isEqualTo(1);
        assertThat(result.getFailed()).isEqualTo(0);
        assertThat(result.getSuccessfulApplications()).hasSize(1);
        assertThat(result.getErrors()).isEmpty();
        verify(cacheService).invalidate();
    }

    @Test
    void createBulkApplicationsWithTransaction_WhenDuplicatePlates_ShouldThrowException() {
        BulkApplicationCreateDto bulkDto = BulkApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .applications(Arrays.asList(testCreateDto, testCreateDto))
            .build();

        assertThatThrownBy(() -> applicationService.createBulkApplicationsWithTransaction(bulkDto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("Duplicate plate numbers in bulk request");

        verify(applicantRepository, never()).findByPassportNumber(any());
    }

    @Test
    void createBulkApplicationsWithTransaction_WhenOneFails_ShouldRollbackAll() {
        ApplicationCreateDto failingDto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .plateNumber("INVALID")
            .build();

        BulkApplicationCreateDto bulkDto = BulkApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .applications(Arrays.asList(testCreateDto, failingDto))
            .build();

        testPlate.setApplications(new ArrayList<>());

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
        when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
        when(licensePlateRepository.findByPlateNumber("INVALID")).thenReturn(Optional.empty());
        when(serviceRepository.findAllById(Collections.singletonList(1L)))
            .thenReturn(Collections.singletonList(testService));
        when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);

        assertThatThrownBy(() -> applicationService.createBulkApplicationsWithTransaction(bulkDto))
            .isInstanceOf(BusinessException.class)
            .hasMessageStartingWith("Bulk application failed:");

        verify(applicationRepository, atLeastOnce()).save(any(Application.class));
    }

    @Test
    void createBulkApplicationsWithoutTransaction_ShouldCreatePartial() {
        ApplicationCreateDto failingDto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .plateNumber("INVALID")
            .build();

        BulkApplicationCreateDto bulkDto = BulkApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .applications(Arrays.asList(testCreateDto, failingDto))
            .build();

        testPlate.setApplications(new ArrayList<>());

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
        when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
        when(licensePlateRepository.findByPlateNumber("INVALID")).thenReturn(Optional.empty());
        when(serviceRepository.findAllById(Collections.singletonList(1L)))
            .thenReturn(Collections.singletonList(testService));
        when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
        when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

        BulkApplicationResult result = applicationService.createBulkApplicationsWithoutTransaction(bulkDto);

        assertThat(result).isNotNull();
        assertThat(result.getTotalRequested()).isEqualTo(2);
        assertThat(result.getSuccessful()).isEqualTo(1);
        assertThat(result.getFailed()).isEqualTo(1);
        assertThat(result.getSuccessfulApplications()).hasSize(1);
        assertThat(result.getErrors()).hasSize(1);
        verify(cacheService).invalidate();
        verify(licensePlateRepository, never()).isPlateAvailable(anyLong());
    }

    @Test
    void createBulkApplicationsWithoutTransaction_WhenApplicantNotFound_ShouldThrowException() {
        BulkApplicationCreateDto bulkDto = BulkApplicationCreateDto.builder()
            .passportNumber("MP9999999")
            .applications(Collections.singletonList(testCreateDto))
            .build();

        when(applicantRepository.findByPassportNumber("MP9999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.createBulkApplicationsWithoutTransaction(bulkDto))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Заявитель с паспортом 'MP9999999' не найден");
    }


    @Test
    void createApplication_WhenNoServicesInDto_ShouldHandleNullServiceIds() {
        testCreateDto.setServiceIds(null);
        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
        when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
        when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
        when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

        ApplicationDto result = applicationService.createApplication(testCreateDto);

        assertThat(result).isNotNull();
        verify(serviceRepository, never()).findAllById(any());
        verify(licensePlateRepository, never()).isPlateAvailable(anyLong());
    }

    @Test
    void createApplication_WhenEmptyServiceIds_ShouldHandleEmptyList() {
        testCreateDto.setServiceIds(Collections.emptyList());
        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
        when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
        when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
        when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

        ApplicationDto result = applicationService.createApplication(testCreateDto);

        assertThat(result).isNotNull();
        verify(serviceRepository, never()).findAllById(any());
    }

    @Test
    void createApplicationInternal_WithExistingApplicant_ShouldUseExisting() {
        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
        when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
        when(serviceRepository.findAllById(Collections.singletonList(1L)))
            .thenReturn(Collections.singletonList(testService));
        when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
        when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

        ApplicationDto result = applicationService.createApplication(testCreateDto);

        assertThat(result).isNotNull();
        verify(applicantRepository, never()).save(any());
        verify(applicationRepository, times(2)).save(any(Application.class));
    }

    @Test
    void createApplicationInternal_WithNewApplicant_ShouldCreateNew() {
        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.empty());
        when(applicantRepository.save(any(Applicant.class))).thenReturn(testApplicant);
        when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(testPlate));
        when(licensePlateRepository.isPlateAvailable(testPlate.getId())).thenReturn(true);
        when(serviceRepository.findAllById(Collections.singletonList(1L)))
            .thenReturn(Collections.singletonList(testService));
        when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
        when(applicationMapper.toDto(testApplication)).thenReturn(testApplicationDto);

        ApplicationDto result = applicationService.createApplicationWithoutTransaction(testCreateDto);

        assertThat(result).isNotNull();
        verify(applicantRepository).save(any(Applicant.class));
    }

    @Test
    void getApplicationsByStatusAndRegionCached_WhenCacheReturnsNullAndRegionNotFound_ShouldReturnEmptyList() {
        when(cacheService.get("PENDING", "InvalidRegion")).thenReturn(null);
        when(applicationRepository.findByStatusAndDepartmentRegion(ApplicationStatus.PENDING, "InvalidRegion"))
            .thenReturn(Collections.emptyList());
        when(applicationMapper.toDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

        List<ApplicationDto> result = applicationService.getApplicationsByStatusAndRegionCached(
            ApplicationStatus.PENDING, "InvalidRegion");

        assertThat(result).isEmpty();
        verify(cacheService).get("PENDING", "InvalidRegion");
        verify(applicationRepository).findByStatusAndDepartmentRegion(ApplicationStatus.PENDING, "InvalidRegion");
        verify(cacheService, never()).put(any(), any(), any());
        verify(departmentRepository, never()).existsByRegion(anyString());
    }

    @Test
    void confirmApplication_WhenApplicationNotFound_ShouldThrowException() {
        when(applicationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.confirmApplication(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Заявление не найдено с id: 99");
    }

    @Test
    void completeApplication_WhenApplicationNotFound_ShouldThrowException() {
        when(applicationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.completeApplication(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Заявление не найдено с id: 99");
    }

    @Test
    void cancelApplication_WhenApplicationNotFound_ShouldThrowException() {
        when(applicationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.cancelApplication(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Заявление не найдено с id: 99");
    }

    @Test
    void calculateTotalAmount_WithServices_ShouldCalculateCorrectly() {
        AdditionalService service1 = AdditionalService.builder()
            .id(100L)
            .name("Express Service")
            .price(BigDecimal.valueOf(500))
            .isAvailable(true)
            .build();

        AdditionalService service2 = AdditionalService.builder()
            .id(200L)
            .name("Insurance Service")
            .price(BigDecimal.valueOf(300))
            .isAvailable(true)
            .build();

        List<AdditionalService> services = Arrays.asList(service1, service2);

        LicensePlate availablePlate = LicensePlate.builder()
            .id(999L)
            .plateNumber("9999 ZZ-9")
            .price(BigDecimal.valueOf(1000))
            .series("ZZ")
            .department(testDepartment)
            .applications(new ArrayList<>())
            .build();

        ApplicationCreateDto newCreateDto = ApplicationCreateDto.builder()
            .passportNumber("MP1234567")
            .plateNumber("9999 ZZ-9")
            .vehicleVin("VIN999999999")
            .vehicleModel("BMW X5")
            .vehicleYear(2023)
            .notes("Test notes")
            .serviceIds(Arrays.asList(100L, 200L))
            .build();

        BigDecimal total = BigDecimal.valueOf(1000).add(BigDecimal.valueOf(800)); // 1800

        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(testApplicant));
        when(licensePlateRepository.findByPlateNumber("9999 ZZ-9")).thenReturn(Optional.of(availablePlate));
        when(licensePlateRepository.isPlateAvailable(availablePlate.getId())).thenReturn(true);
        when(serviceRepository.findAllById(Arrays.asList(100L, 200L))).thenReturn(services);
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> {
            Application savedApp = invocation.getArgument(0);
            assertThat(savedApp.getPaymentAmount()).isEqualTo(total);
            Application newApp = Application.builder()
                .id(1L)
                .applicant(testApplicant)
                .licensePlate(availablePlate)
                .department(testDepartment)
                .status(ApplicationStatus.PENDING)
                .submissionDate(LocalDateTime.now())
                .reservedUntil(LocalDateTime.now().plusHours(1))
                .paymentAmount(savedApp.getPaymentAmount())
                .build();
            return newApp;
        });
        when(applicationMapper.toDto(any(Application.class))).thenReturn(testApplicationDto);

        applicationService.createApplicationWithoutTransaction(newCreateDto);
    }

    @Test
    void getApplicationsByStatusAndRegionCached_WhenCacheHitWithEmptyList_ShouldInvalidateAndFetch() {
        when(cacheService.get("PENDING", "Moscow")).thenReturn(Collections.emptyList());
        when(applicationRepository.findByStatusAndDepartmentRegion(ApplicationStatus.PENDING, "Moscow"))
            .thenReturn(Collections.emptyList());
        when(applicationMapper.toDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

        List<ApplicationDto> result = applicationService.getApplicationsByStatusAndRegionCached(
            ApplicationStatus.PENDING, "Moscow");

        assertThat(result).isEmpty();
        verify(cacheService).get("PENDING", "Moscow");
        verify(cacheService).invalidate();
        verify(applicationRepository).findByStatusAndDepartmentRegion(ApplicationStatus.PENDING,
            "Moscow");
        verify(cacheService, never()).put(any(), any(), any());
        verify(departmentRepository, never()).existsByRegion(anyString());
    }
}