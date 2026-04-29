package com.example.licenseplate.service;

import com.example.licenseplate.dto.request.PassportChangeRequestCreateRequest;
import com.example.licenseplate.dto.request.PassportChangeRequestReviewRequest;
import com.example.licenseplate.dto.response.PassportChangeRequestDto;
import com.example.licenseplate.exception.BusinessException;
import com.example.licenseplate.exception.ResourceNotFoundException;
import com.example.licenseplate.model.entity.Applicant;
import com.example.licenseplate.model.entity.PassportChangeRequest;
import com.example.licenseplate.model.entity.UserAccount;
import com.example.licenseplate.model.enums.ApplicationStatus;
import com.example.licenseplate.model.enums.PassportChangeRequestStatus;
import com.example.licenseplate.model.enums.UserRole;
import com.example.licenseplate.repository.ApplicantRepository;
import com.example.licenseplate.repository.PassportChangeRequestRepository;
import com.example.licenseplate.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PassportChangeRequestService {

    private final PassportChangeRequestRepository passportChangeRequestRepository;
    private final ApplicantRepository applicantRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuthService authService;

    @Transactional
    public PassportChangeRequestDto createRequest(
        String authHeader,
        PassportChangeRequestCreateRequest request
    ) {
        UserAccount account = authService.getCurrentAccount(authHeader);

        if (account.getRole() != UserRole.USER || account.getApplicant() == null) {
            throw new BusinessException("Подать заявку на смену паспорта может только пользователь");
        }

        Applicant applicant = account.getApplicant();
        String requestedPassport = request.getNewPassportNumber().trim().toUpperCase();

        validateApplicantCanRequest(applicant, requestedPassport);

        PassportChangeRequest savedRequest = passportChangeRequestRepository.save(
            PassportChangeRequest.builder()
                .applicant(applicant)
                .currentPassportNumber(applicant.getPassportNumber())
                .requestedPassportNumber(requestedPassport)
                .contactPhone(request.getContactPhone().trim())
                .status(PassportChangeRequestStatus.PENDING)
                .build()
        );

        return toDto(savedRequest);
    }

    @Transactional(readOnly = true)
    public List<PassportChangeRequestDto> getMyRequests(String authHeader) {
        UserAccount account = authService.getCurrentAccount(authHeader);

        if (account.getRole() != UserRole.USER || account.getApplicant() == null) {
            throw new BusinessException("Просмотр заявок доступен только пользователю");
        }

        return passportChangeRequestRepository.findByApplicantIdOrderByCreatedAtDesc(account.getApplicant().getId())
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PassportChangeRequestDto> getAllRequests(String authHeader, String status) {
        UserAccount account = authService.getCurrentAccount(authHeader);
        ensureAdmin(account);

        List<PassportChangeRequest> requests = status == null || status.isBlank()
            ? passportChangeRequestRepository.findAllByOrderByCreatedAtDesc()
            : passportChangeRequestRepository.findByStatusOrderByCreatedAtDesc(
                PassportChangeRequestStatus.valueOf(status.trim().toUpperCase())
            );

        return requests.stream().map(this::toDto).toList();
    }

    @Transactional
    public PassportChangeRequestDto approveRequest(
        Long requestId,
        String authHeader,
        PassportChangeRequestReviewRequest request
    ) {
        UserAccount account = authService.getCurrentAccount(authHeader);
        ensureAdmin(account);

        PassportChangeRequest passportRequest = findRequestOrThrow(requestId);
        ensurePending(passportRequest);

        Applicant applicant = passportRequest.getApplicant();
        String requestedPassport = passportRequest.getRequestedPassportNumber();

        if (applicantRepository.existsByPassportNumber(requestedPassport)
            && !requestedPassport.equalsIgnoreCase(applicant.getPassportNumber())) {
            throw new BusinessException("Указанный новый паспорт уже используется другим пользователем");
        }

        boolean hasPendingApplications = applicant.getApplications() != null
            && applicant.getApplications().stream().anyMatch(app -> app.getStatus() == ApplicationStatus.PENDING);

        if (hasPendingApplications) {
            throw new BusinessException("Нельзя подтвердить смену паспорта, пока есть заявление в " +
                "статусе «На рассмотрении»");
        }

        String previousPassport = applicant.getPassportNumber();
        applicant.setPassportNumber(requestedPassport);
        applicantRepository.save(applicant);

        userAccountRepository.findByApplicantId(applicant.getId()).ifPresent(userAccount -> {
            if (previousPassport.equalsIgnoreCase(userAccount.getUsername())) {
                userAccount.setUsername(requestedPassport);
                userAccountRepository.save(userAccount);
            }
        });

        passportRequest.setStatus(PassportChangeRequestStatus.APPROVED);
        passportRequest.setReviewedAt(LocalDateTime.now());
        passportRequest.setAdminComment(normalizeComment(request.getAdminComment()));

        return toDto(passportChangeRequestRepository.save(passportRequest));
    }

    @Transactional
    public PassportChangeRequestDto rejectRequest(
        Long requestId,
        String authHeader,
        PassportChangeRequestReviewRequest request
    ) {
        UserAccount account = authService.getCurrentAccount(authHeader);
        ensureAdmin(account);

        PassportChangeRequest passportRequest = findRequestOrThrow(requestId);
        ensurePending(passportRequest);

        passportRequest.setStatus(PassportChangeRequestStatus.REJECTED);
        passportRequest.setReviewedAt(LocalDateTime.now());
        passportRequest.setAdminComment(normalizeComment(request.getAdminComment()));

        return toDto(passportChangeRequestRepository.save(passportRequest));
    }

    private void validateApplicantCanRequest(Applicant applicant, String requestedPassport) {
        if (requestedPassport.equalsIgnoreCase(applicant.getPassportNumber())) {
            throw new BusinessException("Укажите новый номер паспорта, который отличается от текущего");
        }

        if (applicantRepository.existsByPassportNumber(requestedPassport)) {
            throw new BusinessException("Указанный новый паспорт уже используется");
        }

        if (passportChangeRequestRepository.existsByApplicantIdAndStatus(applicant.getId(),
            PassportChangeRequestStatus.PENDING)) {
            throw new BusinessException("У вас уже есть необработанная заявка на смену паспорта");
        }

        boolean hasPendingApplications = applicant.getApplications() != null
            && applicant.getApplications().stream().anyMatch(app -> app.getStatus() ==
            ApplicationStatus.PENDING);

        if (hasPendingApplications) {
            throw new BusinessException("Нельзя подать заявку на смену паспорта, пока есть заявление " +
                "в статусе «На рассмотрении»");
        }
    }

    private void ensureAdmin(UserAccount account) {
        if (account.getRole() != UserRole.ADMIN) {
            throw new BusinessException("Операция доступна только администратору");
        }
    }

    private void ensurePending(PassportChangeRequest passportRequest) {
        if (passportRequest.getStatus() != PassportChangeRequestStatus.PENDING) {
            throw new BusinessException("Заявка уже обработана");
        }
    }

    private PassportChangeRequest findRequestOrThrow(Long requestId) {
        return passportChangeRequestRepository.findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("Заявка на смену паспорта не найдена"));
    }

    private String normalizeComment(String comment) {
        return comment == null || comment.isBlank() ? null : comment.trim();
    }

    private PassportChangeRequestDto toDto(PassportChangeRequest request) {
        return PassportChangeRequestDto.builder()
            .id(request.getId())
            .applicantId(request.getApplicant().getId())
            .applicantName(request.getApplicant().getFullName())
            .currentPassportNumber(request.getCurrentPassportNumber())
            .requestedPassportNumber(request.getRequestedPassportNumber())
            .contactPhone(request.getContactPhone())
            .status(request.getStatus().name())
            .adminComment(request.getAdminComment())
            .createdAt(request.getCreatedAt())
            .reviewedAt(request.getReviewedAt())
            .build();
    }
}
