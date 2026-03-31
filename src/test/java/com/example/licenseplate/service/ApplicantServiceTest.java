package com.example.licenseplate.service;

import com.example.licenseplate.dto.request.ApplicantCreateDto;
import com.example.licenseplate.dto.response.ApplicantDto;
import com.example.licenseplate.exception.BusinessException;
import com.example.licenseplate.exception.ResourceNotFoundException;
import com.example.licenseplate.model.entity.Applicant;
import com.example.licenseplate.model.entity.Application;
import com.example.licenseplate.model.enums.ApplicationStatus;
import com.example.licenseplate.repository.ApplicantRepository;
import com.example.licenseplate.mapper.ApplicantMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicantService - 100% Coverage Tests")
class ApplicantServiceTest {

    @Mock
    private ApplicantRepository applicantRepository;

    @Mock
    private ApplicantMapper applicantMapper;

    @InjectMocks
    private ApplicantService applicantService;

    private Applicant testApplicant;
    private ApplicantDto testApplicantDto;
    private ApplicantCreateDto testCreateDto;

    @BeforeEach
    void setUp() {
        testApplicant = Applicant.builder()
            .id(1L)
            .fullName("Иванов Иван")
            .passportNumber("MP1234567")
            .phoneNumber("+375291234567")
            .email("ivan@example.com")
            .address("г.Минск, ул.Ленина 1")
            .applications(new ArrayList<>())
            .build();

        testApplicantDto = ApplicantDto.builder()
            .id(1L)
            .fullName("Иванов Иван")
            .passportNumber("MP1234567")
            .phoneNumber("+375291234567")
            .email("ivan@example.com")
            .address("г.Минск, ул.Ленина 1")
            .applicationsCount(0)
            .build();

        testCreateDto = ApplicantCreateDto.builder()
            .fullName("Иванов Иван")
            .passportNumber("MP1234567")
            .phoneNumber("+375291234567")
            .email("ivan@example.com")
            .address("г.Минск, ул.Ленина 1")
            .build();
    }

    @Nested
    @DisplayName("getAllApplicants()")
    class GetAllApplicantsTests {

        @Test
        @DisplayName("Should return list of applicants when repository returns list")
        void shouldReturnListOfApplicants() {
            List<Applicant> applicants = List.of(testApplicant);
            List<ApplicantDto> expectedDtos = List.of(testApplicantDto);

            when(applicantRepository.findAll()).thenReturn(applicants);
            when(applicantMapper.toDtoList(applicants)).thenReturn(expectedDtos);

            List<ApplicantDto> result = applicantService.getAllApplicants();

            assertThat(result).isEqualTo(expectedDtos);
            verify(applicantRepository).findAll();
            verify(applicantMapper).toDtoList(applicants);
        }

        @Test
        @DisplayName("Should return empty list when repository returns empty list")
        void shouldReturnEmptyListWhenNoApplicants() {
            List<Applicant> emptyList = Collections.emptyList();

            when(applicantRepository.findAll()).thenReturn(emptyList);
            when(applicantMapper.toDtoList(emptyList)).thenReturn(Collections.emptyList());

            List<ApplicantDto> result = applicantService.getAllApplicants();

            assertThat(result).isEmpty();
            verify(applicantRepository).findAll();
            verify(applicantMapper).toDtoList(emptyList);
        }

        @Test
        @DisplayName("Should handle null from mapper gracefully")
        void shouldHandleNullFromMapper() {
            List<Applicant> applicants = List.of(testApplicant);

            when(applicantRepository.findAll()).thenReturn(applicants);
            when(applicantMapper.toDtoList(applicants)).thenReturn(null);

            List<ApplicantDto> result = applicantService.getAllApplicants();

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("getApplicantById()")
    class GetApplicantByIdTests {

        @Test
        @DisplayName("Should return applicant when id exists")
        void shouldReturnApplicantWhenIdExists() {
            when(applicantRepository.findById(1L)).thenReturn(Optional.of(testApplicant));
            when(applicantMapper.toDto(testApplicant)).thenReturn(testApplicantDto);

            ApplicantDto result = applicantService.getApplicantById(1L);

            assertThat(result).isEqualTo(testApplicantDto);
            verify(applicantRepository).findById(1L);
            verify(applicantMapper).toDto(testApplicant);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when id not found")
        void shouldThrowNotFoundExceptionWhenIdNotFound() {
            when(applicantRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> applicantService.getApplicantById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Applicant not found with id: 999");

            verify(applicantRepository).findById(999L);
            verify(applicantMapper, never()).toDto(any());
        }
    }

    @Nested
    @DisplayName("getApplicantByPassport()")
    class GetApplicantByPassportTests {

        @Test
        @DisplayName("Should return applicant when passport exists")
        void shouldReturnApplicantWhenPassportExists() {
            when(applicantRepository.findByPassportNumber("MP1234567"))
                .thenReturn(Optional.of(testApplicant));
            when(applicantMapper.toDto(testApplicant)).thenReturn(testApplicantDto);

            ApplicantDto result = applicantService.getApplicantByPassport("MP1234567");

            assertThat(result).isEqualTo(testApplicantDto);
            verify(applicantRepository).findByPassportNumber("MP1234567");
            verify(applicantMapper).toDto(testApplicant);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when passport not found")
        void shouldThrowNotFoundExceptionWhenPassportNotFound() {
            when(applicantRepository.findByPassportNumber("NOT_EXIST"))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> applicantService.getApplicantByPassport("NOT_EXIST"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Applicant not found with passport: NOT_EXIST");

            verify(applicantRepository).findByPassportNumber("NOT_EXIST");
            verify(applicantMapper, never()).toDto(any());
        }
    }

    @Nested
    @DisplayName("createApplicant()")
    class CreateApplicantTests {

        @Test
        @DisplayName("Should create applicant successfully when passport is unique")
        void shouldCreateApplicantSuccessfully() {
            when(applicantRepository.existsByPassportNumber("MP1234567")).thenReturn(false);
            when(applicantMapper.toEntity(testCreateDto)).thenReturn(testApplicant);
            when(applicantRepository.save(testApplicant)).thenReturn(testApplicant);
            when(applicantMapper.toDto(testApplicant)).thenReturn(testApplicantDto);

            ApplicantDto result = applicantService.createApplicant(testCreateDto);

            assertThat(result).isEqualTo(testApplicantDto);
            verify(applicantRepository).existsByPassportNumber("MP1234567");
            verify(applicantMapper).toEntity(testCreateDto);
            verify(applicantRepository).save(testApplicant);
            verify(applicantMapper).toDto(testApplicant);
        }

        @Test
        @DisplayName("Should throw BusinessException when passport already exists")
        void shouldThrowExceptionWhenPassportExists() {
            when(applicantRepository.existsByPassportNumber("MP1234567")).thenReturn(true);

            assertThatThrownBy(() -> applicantService.createApplicant(testCreateDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Applicant with passport MP1234567 already exists");

            verify(applicantRepository).existsByPassportNumber("MP1234567");
            verify(applicantMapper, never()).toEntity(any());
            verify(applicantRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateApplicant()")
    class UpdateApplicantTests {

        @Test
        @DisplayName("Should update applicant successfully when id exists")
        void shouldUpdateApplicantSuccessfully() {
            when(applicantRepository.findById(1L)).thenReturn(Optional.of(testApplicant));
            doNothing().when(applicantMapper).updateEntity(testApplicant, testCreateDto);
            when(applicantRepository.save(testApplicant)).thenReturn(testApplicant);
            when(applicantMapper.toDto(testApplicant)).thenReturn(testApplicantDto);

            ApplicantDto result = applicantService.updateApplicant(1L, testCreateDto);

            assertThat(result).isEqualTo(testApplicantDto);
            verify(applicantRepository).findById(1L);
            verify(applicantMapper).updateEntity(testApplicant, testCreateDto);
            verify(applicantRepository).save(testApplicant);
            verify(applicantMapper).toDto(testApplicant);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when id not found")
        void shouldThrowNotFoundExceptionWhenIdNotFound() {
            when(applicantRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> applicantService.updateApplicant(999L, testCreateDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Applicant not found with id: 999");

            verify(applicantRepository).findById(999L);
            verify(applicantMapper, never()).updateEntity(any(), any());
            verify(applicantRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle null updateDto")
        void shouldHandleNullUpdateDto() {
            when(applicantRepository.findById(1L)).thenReturn(Optional.of(testApplicant));
            when(applicantRepository.save(testApplicant)).thenReturn(testApplicant);
            when(applicantMapper.toDto(testApplicant)).thenReturn(testApplicantDto);

            ApplicantDto result = applicantService.updateApplicant(1L, null);

            assertThat(result).isEqualTo(testApplicantDto);
            verify(applicantMapper).updateEntity(testApplicant, null);
        }
    }

    @Nested
    @DisplayName("changePassport()")
    class ChangePassportTests {

        @Test
        @DisplayName("Should change passport successfully")
        void shouldChangePassportSuccessfully() {
            when(applicantRepository.findById(1L)).thenReturn(Optional.of(testApplicant));
            when(applicantRepository.existsByPassportNumber("MP7654321")).thenReturn(false);
            when(applicantRepository.save(testApplicant)).thenReturn(testApplicant);
            when(applicantMapper.toDto(testApplicant)).thenReturn(testApplicantDto);

            ApplicantDto result = applicantService.changePassport(1L, "MP7654321");

            assertThat(result).isEqualTo(testApplicantDto);
            assertThat(testApplicant.getPassportNumber()).isEqualTo("MP7654321");
            verify(applicantRepository).findById(1L);
            verify(applicantRepository).existsByPassportNumber("MP7654321");
            verify(applicantRepository).save(testApplicant);
        }

        @Test
        @DisplayName("Should throw BusinessException when new passport already used")
        void shouldThrowExceptionWhenNewPassportAlreadyUsed() {
            when(applicantRepository.findById(1L)).thenReturn(Optional.of(testApplicant));
            when(applicantRepository.existsByPassportNumber("MP7654321")).thenReturn(true);

            assertThatThrownBy(() -> applicantService.changePassport(1L, "MP7654321"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Паспорт MP7654321 уже используется");

            verify(applicantRepository).findById(1L);
            verify(applicantRepository).existsByPassportNumber("MP7654321");
            verify(applicantRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw BusinessException when has active PENDING applications")
        void shouldThrowExceptionWhenHasPendingApplications() {
            Application pendingApp = Application.builder().status(ApplicationStatus.PENDING).build();
            testApplicant.getApplications().add(pendingApp);

            when(applicantRepository.findById(1L)).thenReturn(Optional.of(testApplicant));

            when(applicantRepository.existsByPassportNumber("MP7654321")).thenReturn(false);

            assertThatThrownBy(() -> applicantService.changePassport(1L, "MP7654321"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Нельзя сменить паспорт при активных заявлениях");

            verify(applicantRepository).findById(1L);
            verify(applicantRepository).existsByPassportNumber("MP7654321");
            verify(applicantRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw BusinessException when has active CONFIRMED applications")
        void shouldThrowExceptionWhenHasConfirmedApplications() {
            Application confirmedApp = Application.builder().status(ApplicationStatus.CONFIRMED).build();
            testApplicant.getApplications().add(confirmedApp);

            when(applicantRepository.findById(1L)).thenReturn(Optional.of(testApplicant));
            when(applicantRepository.existsByPassportNumber("MP7654321")).thenReturn(false);

            assertThatThrownBy(() -> applicantService.changePassport(1L, "MP7654321"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Нельзя сменить паспорт при активных заявлениях");

            verify(applicantRepository).findById(1L);
            verify(applicantRepository).existsByPassportNumber("MP7654321");
            verify(applicantRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should allow change when applications are completed or cancelled")
        void shouldAllowChangeWhenApplicationsNotActive() {
            Application completedApp = Application.builder().status(ApplicationStatus.COMPLETED).build();
            Application cancelledApp = Application.builder().status(ApplicationStatus.CANCELLED).build();
            testApplicant.getApplications().add(completedApp);
            testApplicant.getApplications().add(cancelledApp);

            when(applicantRepository.findById(1L)).thenReturn(Optional.of(testApplicant));
            when(applicantRepository.existsByPassportNumber("MP7654321")).thenReturn(false);
            when(applicantRepository.save(testApplicant)).thenReturn(testApplicant);
            when(applicantMapper.toDto(testApplicant)).thenReturn(testApplicantDto);

            ApplicantDto result = applicantService.changePassport(1L, "MP7654321");

            assertThat(result).isEqualTo(testApplicantDto);
            assertThat(testApplicant.getPassportNumber()).isEqualTo("MP7654321");
            verify(applicantRepository).findById(1L);
            verify(applicantRepository).existsByPassportNumber("MP7654321");
            verify(applicantRepository).save(testApplicant);
        }

        @Test
        @DisplayName("Should allow change when applications list is null")
        void shouldAllowChangeWhenApplicationsListIsNull() {
            testApplicant.setApplications(null);

            when(applicantRepository.findById(1L)).thenReturn(Optional.of(testApplicant));
            when(applicantRepository.existsByPassportNumber("MP7654321")).thenReturn(false);
            when(applicantRepository.save(testApplicant)).thenReturn(testApplicant);
            when(applicantMapper.toDto(testApplicant)).thenReturn(testApplicantDto);

            ApplicantDto result = applicantService.changePassport(1L, "MP7654321");

            assertThat(result).isEqualTo(testApplicantDto);
            verify(applicantRepository).findById(1L);
            verify(applicantRepository).existsByPassportNumber("MP7654321");
            verify(applicantRepository).save(testApplicant);
        }

        @Test
        @DisplayName("Should allow change when applications list is empty")
        void shouldAllowChangeWhenApplicationsListIsEmpty() {
            testApplicant.setApplications(Collections.emptyList());

            when(applicantRepository.findById(1L)).thenReturn(Optional.of(testApplicant));
            when(applicantRepository.existsByPassportNumber("MP7654321")).thenReturn(false);
            when(applicantRepository.save(testApplicant)).thenReturn(testApplicant);
            when(applicantMapper.toDto(testApplicant)).thenReturn(testApplicantDto);

            ApplicantDto result = applicantService.changePassport(1L, "MP7654321");

            assertThat(result).isEqualTo(testApplicantDto);
            verify(applicantRepository).findById(1L);
            verify(applicantRepository).existsByPassportNumber("MP7654321");
            verify(applicantRepository).save(testApplicant);
        }
    }

    @Nested
    @DisplayName("deleteApplicant()")
    class DeleteApplicantTests {

        @Test
        @DisplayName("Should delete applicant successfully when no applications")
        void shouldDeleteApplicantSuccessfully() {
            when(applicantRepository.findById(1L)).thenReturn(Optional.of(testApplicant));
            doNothing().when(applicantRepository).delete(testApplicant);

            applicantService.deleteApplicant(1L);

            verify(applicantRepository).findById(1L);
            verify(applicantRepository).delete(testApplicant);
        }

        @Test
        @DisplayName("Should throw BusinessException when has applications")
        void shouldThrowExceptionWhenHasApplications() {
            testApplicant.setApplications(List.of(new Application()));

            when(applicantRepository.findById(1L)).thenReturn(Optional.of(testApplicant));

            assertThatThrownBy(() -> applicantService.deleteApplicant(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot delete applicant with existing applications");

            verify(applicantRepository).findById(1L);
            verify(applicantRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when id not found")
        void shouldThrowNotFoundExceptionWhenIdNotFound() {
            when(applicantRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> applicantService.deleteApplicant(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Applicant not found with id: 999");

            verify(applicantRepository).findById(999L);
            verify(applicantRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should handle null applications list gracefully")
        void shouldHandleNullApplicationsList() {
            testApplicant.setApplications(null);

            when(applicantRepository.findById(1L)).thenReturn(Optional.of(testApplicant));
            doNothing().when(applicantRepository).delete(testApplicant);

            applicantService.deleteApplicant(1L);

            verify(applicantRepository).findById(1L);
            verify(applicantRepository).delete(testApplicant);
        }
    }

    @Nested
    @DisplayName("findApplicantOrThrow() private method coverage through public methods")
    class PrivateMethodCoverage {

        @Test
        @DisplayName("Should throw ResourceNotFoundException for invalid id in multiple methods")
        void shouldThrowExceptionForInvalidIdInAllMethods() {
            when(applicantRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> applicantService.getApplicantById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
            assertThatThrownBy(() -> applicantService.updateApplicant(999L, testCreateDto))
                .isInstanceOf(ResourceNotFoundException.class);
            assertThatThrownBy(() -> applicantService.deleteApplicant(999L))
                .isInstanceOf(ResourceNotFoundException.class);
            assertThatThrownBy(() -> applicantService.changePassport(999L, "MP1234567"))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}