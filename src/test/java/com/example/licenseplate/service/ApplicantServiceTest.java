package com.example.licenseplate.service;

import com.example.licenseplate.dto.request.ApplicantCreateDto;
import com.example.licenseplate.dto.response.ApplicantDto;
import com.example.licenseplate.exception.BusinessException;
import com.example.licenseplate.exception.ResourceNotFoundException;
import com.example.licenseplate.mapper.ApplicantMapper;
import com.example.licenseplate.model.entity.Applicant;
import com.example.licenseplate.model.entity.Application;
import com.example.licenseplate.model.entity.UserAccount;
import com.example.licenseplate.model.enums.ApplicationStatus;
import com.example.licenseplate.repository.ApplicantRepository;
import com.example.licenseplate.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicantServiceTest {

    @Mock
    private ApplicantRepository applicantRepository;
    @Mock
    private ApplicantMapper applicantMapper;
    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private ApplicantService applicantService;

    private Applicant applicant;
    private ApplicantDto applicantDto;
    private ApplicantCreateDto createDto;

    @BeforeEach
    void setUp() {
        applicant = Applicant.builder()
            .id(1L)
            .fullName("Ivan Ivanov")
            .passportNumber("MP1234567")
            .applications(new ArrayList<>())
            .build();

        applicantDto = ApplicantDto.builder()
            .id(1L)
            .fullName("Ivan Ivanov")
            .passportNumber("MP1234567")
            .build();

        createDto = ApplicantCreateDto.builder()
            .fullName("Ivan Ivanov")
            .passportNumber("MP1234567")
            .email("ivan@example.com")
            .phoneNumber("+375291234567")
            .address("Minsk")
            .build();
    }

    @Test
    void getAllApplicantsReturnsMappedList() {
        when(applicantRepository.findAll()).thenReturn(List.of(applicant));
        when(applicantMapper.toDtoList(List.of(applicant))).thenReturn(List.of(applicantDto));

        assertThat(applicantService.getAllApplicants()).containsExactly(applicantDto);
    }

    @Test
    void getAllApplicantsReturnsEmptyList() {
        when(applicantRepository.findAll()).thenReturn(List.of());
        when(applicantMapper.toDtoList(List.of())).thenReturn(List.of());

        assertThat(applicantService.getAllApplicants()).isEmpty();
    }

    @Test
    void getApplicantByIdReturnsMappedApplicant() {
        when(applicantRepository.findById(1L)).thenReturn(Optional.of(applicant));
        when(applicantMapper.toDto(applicant)).thenReturn(applicantDto);

        assertThat(applicantService.getApplicantById(1L)).isEqualTo(applicantDto);
    }

    @Test
    void getApplicantByIdThrowsWhenMissing() {
        when(applicantRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicantService.getApplicantById(1L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getApplicantByPassportReturnsMappedApplicant() {
        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(applicantMapper.toDto(applicant)).thenReturn(applicantDto);

        assertThat(applicantService.getApplicantByPassport("MP1234567")).isEqualTo(applicantDto);
    }

    @Test
    void getApplicantByPassportThrowsWhenMissing() {
        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicantService.getApplicantByPassport("MP1234567"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createApplicantThrowsWhenPassportExists() {
        when(applicantRepository.existsByPassportNumber("MP1234567")).thenReturn(true);

        assertThatThrownBy(() -> applicantService.createApplicant(createDto))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void createApplicantSavesAndMapsEntity() {
        when(applicantRepository.existsByPassportNumber("MP1234567")).thenReturn(false);
        when(applicantMapper.toEntity(createDto)).thenReturn(applicant);
        when(applicantRepository.save(applicant)).thenReturn(applicant);
        when(applicantMapper.toDto(applicant)).thenReturn(applicantDto);

        assertThat(applicantService.createApplicant(createDto)).isEqualTo(applicantDto);
    }

    @Test
    void updateApplicantThrowsWhenMissing() {
        when(applicantRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicantService.updateApplicant(1L, createDto))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateApplicantUpdatesAndMapsEntity() {
        when(applicantRepository.findById(1L)).thenReturn(Optional.of(applicant));
        when(applicantRepository.save(applicant)).thenReturn(applicant);
        when(applicantMapper.toDto(applicant)).thenReturn(applicantDto);

        assertThat(applicantService.updateApplicant(1L, createDto)).isEqualTo(applicantDto);
        verify(applicantMapper).updateEntity(applicant, createDto);
    }

    @Test
    void changePassportThrowsWhenPassportAlreadyUsed() {
        when(applicantRepository.findById(1L)).thenReturn(Optional.of(applicant));
        when(applicantRepository.existsByPassportNumber("MP7654321")).thenReturn(true);

        assertThatThrownBy(() -> applicantService.changePassport(1L, "MP7654321"))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void changePassportThrowsWhenPendingApplicationsExist() {
        applicant.setApplications(List.of(Application.builder().status(ApplicationStatus.PENDING).build()));
        when(applicantRepository.findById(1L)).thenReturn(Optional.of(applicant));
        when(applicantRepository.existsByPassportNumber("MP7654321")).thenReturn(false);

        assertThatThrownBy(() -> applicantService.changePassport(1L, "MP7654321"))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void changePassportThrowsWhenApplicantMissing() {
        when(applicantRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicantService.changePassport(1L, "MP7654321"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void changePassportUpdatesApplicantAndSyncedUsername() {
        UserAccount account = UserAccount.builder()
            .id(4L)
            .username("MP1234567")
            .applicant(applicant)
            .build();

        when(applicantRepository.findById(1L)).thenReturn(Optional.of(applicant));
        when(applicantRepository.existsByPassportNumber("MP7654321")).thenReturn(false);
        when(applicantRepository.save(applicant)).thenReturn(applicant);
        when(userAccountRepository.findByApplicantId(1L)).thenReturn(Optional.of(account));
        when(applicantMapper.toDto(applicant)).thenReturn(applicantDto);

        applicantService.changePassport(1L, "MP7654321");

        assertThat(applicant.getPassportNumber()).isEqualTo("MP7654321");
        assertThat(account.getUsername()).isEqualTo("MP7654321");
        verify(userAccountRepository).save(account);
    }

    @Test
    void changePassportDoesNotSyncUsernameWhenUsernameDiffers() {
        UserAccount account = UserAccount.builder()
            .id(4L)
            .username("ivan@example.com")
            .applicant(applicant)
            .build();

        when(applicantRepository.findById(1L)).thenReturn(Optional.of(applicant));
        when(applicantRepository.existsByPassportNumber("MP7654321")).thenReturn(false);
        when(applicantRepository.save(applicant)).thenReturn(applicant);
        when(userAccountRepository.findByApplicantId(1L)).thenReturn(Optional.of(account));
        when(applicantMapper.toDto(applicant)).thenReturn(applicantDto);

        applicantService.changePassport(1L, "MP7654321");

        verify(userAccountRepository, never()).save(account);
    }

    @Test
    void changePassportSucceedsWithoutApplicationsAndWithoutAccount() {
        applicant.setApplications(null);
        when(applicantRepository.findById(1L)).thenReturn(Optional.of(applicant));
        when(applicantRepository.existsByPassportNumber("MP7654321")).thenReturn(false);
        when(applicantRepository.save(applicant)).thenReturn(applicant);
        when(userAccountRepository.findByApplicantId(1L)).thenReturn(Optional.empty());
        when(applicantMapper.toDto(applicant)).thenReturn(applicantDto);

        assertThat(applicantService.changePassport(1L, "MP7654321")).isEqualTo(applicantDto);
    }

    @Test
    void changePassportAllowsNonPendingApplications() {
        applicant.setApplications(List.of(Application.builder().status(ApplicationStatus.CANCELLED).build()));
        when(applicantRepository.findById(1L)).thenReturn(Optional.of(applicant));
        when(applicantRepository.existsByPassportNumber("MP7654321")).thenReturn(false);
        when(applicantRepository.save(applicant)).thenReturn(applicant);
        when(userAccountRepository.findByApplicantId(1L)).thenReturn(Optional.empty());
        when(applicantMapper.toDto(applicant)).thenReturn(applicantDto);

        assertThat(applicantService.changePassport(1L, "MP7654321")).isEqualTo(applicantDto);
    }

    @Test
    void deleteApplicantThrowsWhenApplicationsExist() {
        applicant.setApplications(List.of(new Application()));
        when(applicantRepository.findById(1L)).thenReturn(Optional.of(applicant));

        assertThatThrownBy(() -> applicantService.deleteApplicant(1L))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void deleteApplicantThrowsWhenMissing() {
        when(applicantRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicantService.deleteApplicant(1L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteApplicantDeletesLinkedAccountFirst() {
        UserAccount account = UserAccount.builder().id(5L).applicant(applicant).build();
        when(applicantRepository.findById(1L)).thenReturn(Optional.of(applicant));
        when(userAccountRepository.findByApplicantId(1L)).thenReturn(Optional.of(account));
        doNothing().when(userAccountRepository).delete(account);
        doNothing().when(applicantRepository).delete(applicant);

        applicantService.deleteApplicant(1L);

        verify(userAccountRepository).delete(account);
        verify(applicantRepository).delete(applicant);
    }

    @Test
    void deleteApplicantDeletesApplicantWhenNoLinkedAccount() {
        when(applicantRepository.findById(1L)).thenReturn(Optional.of(applicant));
        when(userAccountRepository.findByApplicantId(1L)).thenReturn(Optional.empty());
        doNothing().when(applicantRepository).delete(any(Applicant.class));

        applicantService.deleteApplicant(1L);

        verify(applicantRepository).delete(applicant);
    }

    @Test
    void deleteApplicantDeletesApplicantWhenApplicationsListIsNull() {
        applicant.setApplications(null);
        when(applicantRepository.findById(1L)).thenReturn(Optional.of(applicant));
        when(userAccountRepository.findByApplicantId(1L)).thenReturn(Optional.empty());
        doNothing().when(applicantRepository).delete(any(Applicant.class));

        applicantService.deleteApplicant(1L);

        verify(applicantRepository).delete(applicant);
    }
}
