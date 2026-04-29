package com.example.licenseplate.service;

import com.example.licenseplate.dto.request.PassportChangeRequestCreateRequest;
import com.example.licenseplate.dto.request.PassportChangeRequestReviewRequest;
import com.example.licenseplate.dto.response.PassportChangeRequestDto;
import com.example.licenseplate.exception.BusinessException;
import com.example.licenseplate.exception.ResourceNotFoundException;
import com.example.licenseplate.model.entity.Applicant;
import com.example.licenseplate.model.entity.Application;
import com.example.licenseplate.model.entity.PassportChangeRequest;
import com.example.licenseplate.model.entity.UserAccount;
import com.example.licenseplate.model.enums.ApplicationStatus;
import com.example.licenseplate.model.enums.PassportChangeRequestStatus;
import com.example.licenseplate.model.enums.UserRole;
import com.example.licenseplate.repository.ApplicantRepository;
import com.example.licenseplate.repository.PassportChangeRequestRepository;
import com.example.licenseplate.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PassportChangeRequestServiceTest {

    @Mock
    private PassportChangeRequestRepository passportChangeRequestRepository;
    @Mock
    private ApplicantRepository applicantRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private AuthService authService;

    @InjectMocks
    private PassportChangeRequestService passportChangeRequestService;

    private Applicant applicant;
    private UserAccount userAccount;
    private UserAccount adminAccount;
    private PassportChangeRequest request;

    @BeforeEach
    void setUp() {
        applicant = Applicant.builder()
            .id(1L)
            .fullName("Ivan Ivanov")
            .passportNumber("MP1234567")
            .applications(new ArrayList<>())
            .build();

        userAccount = UserAccount.builder()
            .id(10L)
            .username("MP1234567")
            .role(UserRole.USER)
            .applicant(applicant)
            .build();

        adminAccount = UserAccount.builder()
            .id(20L)
            .username("admin")
            .role(UserRole.ADMIN)
            .build();

        request = PassportChangeRequest.builder()
            .id(5L)
            .applicant(applicant)
            .currentPassportNumber("MP1234567")
            .requestedPassportNumber("MP7654321")
            .contactPhone("+375291234567")
            .status(PassportChangeRequestStatus.PENDING)
            .createdAt(LocalDateTime.now())
            .build();
    }

    @Test
    void createRequestThrowsWhenCurrentAccountIsNotUser() {
        when(authService.getCurrentAccount("Bearer token")).thenReturn(adminAccount);

        assertThatThrownBy(() -> passportChangeRequestService.createRequest("Bearer token", new PassportChangeRequestCreateRequest()))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void createRequestThrowsWhenApplicantMissingForUser() {
        UserAccount userWithoutApplicant = UserAccount.builder()
            .id(11L)
            .role(UserRole.USER)
            .build();
        when(authService.getCurrentAccount("Bearer token")).thenReturn(userWithoutApplicant);

        assertThatThrownBy(() -> passportChangeRequestService.createRequest("Bearer token", new PassportChangeRequestCreateRequest()))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void createRequestThrowsWhenRequestedPassportIsSame() {
        PassportChangeRequestCreateRequest dto = new PassportChangeRequestCreateRequest();
        dto.setNewPassportNumber("mp1234567");
        dto.setContactPhone("+375291234567");
        when(authService.getCurrentAccount("Bearer token")).thenReturn(userAccount);

        assertThatThrownBy(() -> passportChangeRequestService.createRequest("Bearer token", dto))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void createRequestThrowsWhenPassportAlreadyUsed() {
        PassportChangeRequestCreateRequest dto = new PassportChangeRequestCreateRequest();
        dto.setNewPassportNumber("mp7654321");
        dto.setContactPhone("+375291234567");
        when(authService.getCurrentAccount("Bearer token")).thenReturn(userAccount);
        when(applicantRepository.existsByPassportNumber("MP7654321")).thenReturn(true);

        assertThatThrownBy(() -> passportChangeRequestService.createRequest("Bearer token", dto))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void createRequestThrowsWhenPendingRequestExists() {
        PassportChangeRequestCreateRequest dto = new PassportChangeRequestCreateRequest();
        dto.setNewPassportNumber("mp7654321");
        dto.setContactPhone("+375291234567");
        when(authService.getCurrentAccount("Bearer token")).thenReturn(userAccount);
        when(applicantRepository.existsByPassportNumber("MP7654321")).thenReturn(false);
        when(passportChangeRequestRepository.existsByApplicantIdAndStatus(1L, PassportChangeRequestStatus.PENDING)).thenReturn(true);

        assertThatThrownBy(() -> passportChangeRequestService.createRequest("Bearer token", dto))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void createRequestThrowsWhenPendingApplicationExists() {
        applicant.setApplications(List.of(Application.builder().status(ApplicationStatus.PENDING).build()));
        PassportChangeRequestCreateRequest dto = new PassportChangeRequestCreateRequest();
        dto.setNewPassportNumber("mp7654321");
        dto.setContactPhone("+375291234567");
        when(authService.getCurrentAccount("Bearer token")).thenReturn(userAccount);
        when(applicantRepository.existsByPassportNumber("MP7654321")).thenReturn(false);
        when(passportChangeRequestRepository.existsByApplicantIdAndStatus(1L, PassportChangeRequestStatus.PENDING)).thenReturn(false);

        assertThatThrownBy(() -> passportChangeRequestService.createRequest("Bearer token", dto))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void createRequestSavesAndMapsDto() {
        PassportChangeRequestCreateRequest dto = new PassportChangeRequestCreateRequest();
        dto.setNewPassportNumber("mp7654321");
        dto.setContactPhone(" +375291234567 ");
        when(authService.getCurrentAccount("Bearer token")).thenReturn(userAccount);
        when(applicantRepository.existsByPassportNumber("MP7654321")).thenReturn(false);
        when(passportChangeRequestRepository.existsByApplicantIdAndStatus(1L, PassportChangeRequestStatus.PENDING)).thenReturn(false);
        when(passportChangeRequestRepository.save(any(PassportChangeRequest.class))).thenReturn(request);

        PassportChangeRequestDto result = passportChangeRequestService.createRequest("Bearer token", dto);

        assertThat(result.getRequestedPassportNumber()).isEqualTo("MP7654321");
        assertThat(result.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void createRequestSucceedsWhenApplicationsListIsNull() {
        applicant.setApplications(null);
        PassportChangeRequestCreateRequest dto = new PassportChangeRequestCreateRequest();
        dto.setNewPassportNumber("mp7654321");
        dto.setContactPhone(" +375291234567 ");
        when(authService.getCurrentAccount("Bearer token")).thenReturn(userAccount);
        when(applicantRepository.existsByPassportNumber("MP7654321")).thenReturn(false);
        when(passportChangeRequestRepository.existsByApplicantIdAndStatus(1L, PassportChangeRequestStatus.PENDING)).thenReturn(false);
        when(passportChangeRequestRepository.save(any(PassportChangeRequest.class))).thenReturn(request);

        PassportChangeRequestDto result = passportChangeRequestService.createRequest("Bearer token", dto);

        assertThat(result.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void getMyRequestsThrowsForNonUser() {
        when(authService.getCurrentAccount("Bearer token")).thenReturn(adminAccount);

        assertThatThrownBy(() -> passportChangeRequestService.getMyRequests("Bearer token"))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void getMyRequestsThrowsWhenApplicantMissingForUser() {
        UserAccount userWithoutApplicant = UserAccount.builder()
            .id(11L)
            .role(UserRole.USER)
            .build();
        when(authService.getCurrentAccount("Bearer token")).thenReturn(userWithoutApplicant);

        assertThatThrownBy(() -> passportChangeRequestService.getMyRequests("Bearer token"))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void getMyRequestsReturnsMappedDtos() {
        when(authService.getCurrentAccount("Bearer token")).thenReturn(userAccount);
        when(passportChangeRequestRepository.findByApplicantIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(request));

        assertThat(passportChangeRequestService.getMyRequests("Bearer token")).hasSize(1);
    }

    @Test
    void getAllRequestsThrowsForNonAdmin() {
        when(authService.getCurrentAccount("Bearer token")).thenReturn(userAccount);

        assertThatThrownBy(() -> passportChangeRequestService.getAllRequests("Bearer token", null))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void getAllRequestsUsesAllWhenStatusBlank() {
        when(authService.getCurrentAccount("Bearer token")).thenReturn(adminAccount);
        when(passportChangeRequestRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(request));

        assertThat(passportChangeRequestService.getAllRequests("Bearer token", " ")).hasSize(1);
    }

    @Test
    void getAllRequestsUsesAllWhenStatusNull() {
        when(authService.getCurrentAccount("Bearer token")).thenReturn(adminAccount);
        when(passportChangeRequestRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(request));

        assertThat(passportChangeRequestService.getAllRequests("Bearer token", null)).hasSize(1);
    }

    @Test
    void getAllRequestsUsesStatusFilter() {
        when(authService.getCurrentAccount("Bearer token")).thenReturn(adminAccount);
        when(passportChangeRequestRepository.findByStatusOrderByCreatedAtDesc(PassportChangeRequestStatus.PENDING))
            .thenReturn(List.of(request));

        assertThat(passportChangeRequestService.getAllRequests("Bearer token", "pending")).hasSize(1);
    }

    @Test
    void approveRequestThrowsWhenNotAdmin() {
        when(authService.getCurrentAccount("Bearer token")).thenReturn(userAccount);

        assertThatThrownBy(() -> passportChangeRequestService.approveRequest(1L, "Bearer token", new PassportChangeRequestReviewRequest()))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void approveRequestThrowsWhenRequestMissing() {
        when(authService.getCurrentAccount("Bearer token")).thenReturn(adminAccount);
        when(passportChangeRequestRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passportChangeRequestService.approveRequest(1L, "Bearer token", new PassportChangeRequestReviewRequest()))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void approveRequestThrowsWhenAlreadyProcessed() {
        request.setStatus(PassportChangeRequestStatus.REJECTED);
        when(authService.getCurrentAccount("Bearer token")).thenReturn(adminAccount);
        when(passportChangeRequestRepository.findById(5L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> passportChangeRequestService.approveRequest(5L, "Bearer token", new PassportChangeRequestReviewRequest()))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void approveRequestThrowsWhenRequestedPassportBelongsToAnotherUser() {
        when(authService.getCurrentAccount("Bearer token")).thenReturn(adminAccount);
        when(passportChangeRequestRepository.findById(5L)).thenReturn(Optional.of(request));
        when(applicantRepository.existsByPassportNumber("MP7654321")).thenReturn(true);

        assertThatThrownBy(() -> passportChangeRequestService.approveRequest(5L, "Bearer token", new PassportChangeRequestReviewRequest()))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void approveRequestThrowsWhenPendingApplicationsExist() {
        applicant.setApplications(List.of(Application.builder().status(ApplicationStatus.PENDING).build()));
        when(authService.getCurrentAccount("Bearer token")).thenReturn(adminAccount);
        when(passportChangeRequestRepository.findById(5L)).thenReturn(Optional.of(request));
        when(applicantRepository.existsByPassportNumber("MP7654321")).thenReturn(false);

        assertThatThrownBy(() -> passportChangeRequestService.approveRequest(5L, "Bearer token", new PassportChangeRequestReviewRequest()))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void approveRequestAllowsSamePassportOwnerMatch() {
        request.setRequestedPassportNumber("MP1234567");
        when(authService.getCurrentAccount("Bearer token")).thenReturn(adminAccount);
        when(passportChangeRequestRepository.findById(5L)).thenReturn(Optional.of(request));
        when(applicantRepository.existsByPassportNumber("MP1234567")).thenReturn(true);
        when(userAccountRepository.findByApplicantId(1L)).thenReturn(Optional.of(userAccount));
        when(passportChangeRequestRepository.save(request)).thenReturn(request);

        PassportChangeRequestDto result =
            passportChangeRequestService.approveRequest(5L, "Bearer token", new PassportChangeRequestReviewRequest());

        assertThat(result.getStatus()).isEqualTo("APPROVED");
    }

    @Test
    void approveRequestUpdatesApplicantAndSyncsUsername() {
        PassportChangeRequestReviewRequest review = new PassportChangeRequestReviewRequest();
        review.setAdminComment(" done ");
        when(authService.getCurrentAccount("Bearer token")).thenReturn(adminAccount);
        when(passportChangeRequestRepository.findById(5L)).thenReturn(Optional.of(request));
        when(applicantRepository.existsByPassportNumber("MP7654321")).thenReturn(false);
        when(userAccountRepository.findByApplicantId(1L)).thenReturn(Optional.of(userAccount));
        when(passportChangeRequestRepository.save(request)).thenReturn(request);

        PassportChangeRequestDto result = passportChangeRequestService.approveRequest(5L, "Bearer token", review);

        assertThat(applicant.getPassportNumber()).isEqualTo("MP7654321");
        assertThat(userAccount.getUsername()).isEqualTo("MP7654321");
        assertThat(result.getStatus()).isEqualTo("APPROVED");
        assertThat(result.getAdminComment()).isEqualTo("done");
    }

    @Test
    void approveRequestKeepsNullAdminCommentWhenNotProvided() {
        when(authService.getCurrentAccount("Bearer token")).thenReturn(adminAccount);
        when(passportChangeRequestRepository.findById(5L)).thenReturn(Optional.of(request));
        when(applicantRepository.existsByPassportNumber("MP7654321")).thenReturn(false);
        when(userAccountRepository.findByApplicantId(1L)).thenReturn(Optional.empty());
        when(passportChangeRequestRepository.save(request)).thenReturn(request);

        PassportChangeRequestDto result =
            passportChangeRequestService.approveRequest(5L, "Bearer token", new PassportChangeRequestReviewRequest());

        assertThat(result.getAdminComment()).isNull();
    }

    @Test
    void approveRequestDoesNotSyncUsernameWhenDifferent() {
        userAccount.setUsername("ivan@example.com");
        when(authService.getCurrentAccount("Bearer token")).thenReturn(adminAccount);
        when(passportChangeRequestRepository.findById(5L)).thenReturn(Optional.of(request));
        when(applicantRepository.existsByPassportNumber("MP7654321")).thenReturn(false);
        when(userAccountRepository.findByApplicantId(1L)).thenReturn(Optional.of(userAccount));
        when(passportChangeRequestRepository.save(request)).thenReturn(request);

        passportChangeRequestService.approveRequest(5L, "Bearer token", new PassportChangeRequestReviewRequest());

        verify(userAccountRepository, never()).save(userAccount);
    }

    @Test
    void approveRequestSucceedsWithoutLinkedAccount() {
        when(authService.getCurrentAccount("Bearer token")).thenReturn(adminAccount);
        when(passportChangeRequestRepository.findById(5L)).thenReturn(Optional.of(request));
        when(applicantRepository.existsByPassportNumber("MP7654321")).thenReturn(false);
        when(userAccountRepository.findByApplicantId(1L)).thenReturn(Optional.empty());
        when(passportChangeRequestRepository.save(request)).thenReturn(request);

        PassportChangeRequestDto result =
            passportChangeRequestService.approveRequest(5L, "Bearer token", new PassportChangeRequestReviewRequest());

        assertThat(result.getStatus()).isEqualTo("APPROVED");
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void rejectRequestThrowsWhenProcessed() {
        request.setStatus(PassportChangeRequestStatus.APPROVED);
        when(authService.getCurrentAccount("Bearer token")).thenReturn(adminAccount);
        when(passportChangeRequestRepository.findById(5L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> passportChangeRequestService.rejectRequest(5L, "Bearer token", new PassportChangeRequestReviewRequest()))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectRequestThrowsWhenNotAdmin() {
        when(authService.getCurrentAccount("Bearer token")).thenReturn(userAccount);

        assertThatThrownBy(() -> passportChangeRequestService.rejectRequest(5L, "Bearer token", new PassportChangeRequestReviewRequest()))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectRequestThrowsWhenMissing() {
        when(authService.getCurrentAccount("Bearer token")).thenReturn(adminAccount);
        when(passportChangeRequestRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passportChangeRequestService.rejectRequest(5L, "Bearer token", new PassportChangeRequestReviewRequest()))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectRequestMarksRequestRejected() {
        PassportChangeRequestReviewRequest review = new PassportChangeRequestReviewRequest();
        review.setAdminComment(" ");
        when(authService.getCurrentAccount("Bearer token")).thenReturn(adminAccount);
        when(passportChangeRequestRepository.findById(5L)).thenReturn(Optional.of(request));
        when(passportChangeRequestRepository.save(request)).thenReturn(request);

        PassportChangeRequestDto result = passportChangeRequestService.rejectRequest(5L, "Bearer token", review);

        assertThat(result.getStatus()).isEqualTo("REJECTED");
        assertThat(result.getAdminComment()).isNull();
    }

    @Test
    void rejectRequestTrimsAdminComment() {
        PassportChangeRequestReviewRequest review = new PassportChangeRequestReviewRequest();
        review.setAdminComment(" done ");
        when(authService.getCurrentAccount("Bearer token")).thenReturn(adminAccount);
        when(passportChangeRequestRepository.findById(5L)).thenReturn(Optional.of(request));
        when(passportChangeRequestRepository.save(request)).thenReturn(request);

        PassportChangeRequestDto result = passportChangeRequestService.rejectRequest(5L, "Bearer token", review);

        assertThat(result.getAdminComment()).isEqualTo("done");
    }
}
