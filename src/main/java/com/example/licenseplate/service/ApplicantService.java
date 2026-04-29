package com.example.licenseplate.service;

import com.example.licenseplate.dto.request.ApplicantCreateDto;
import com.example.licenseplate.dto.response.ApplicantDto;
import com.example.licenseplate.exception.ResourceNotFoundException;
import com.example.licenseplate.exception.BusinessException;
import com.example.licenseplate.model.entity.Applicant;
import com.example.licenseplate.model.entity.UserAccount;
import com.example.licenseplate.model.enums.ApplicationStatus;
import com.example.licenseplate.model.enums.UserRole;
import com.example.licenseplate.repository.ApplicantRepository;
import com.example.licenseplate.mapper.ApplicantMapper;
import com.example.licenseplate.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicantService {

    private final ApplicantRepository applicantRepository;
    private final ApplicantMapper applicantMapper;
    private final UserAccountRepository userAccountRepository;

    @Cacheable("applicants")
    @Transactional(readOnly = true)
    public List<ApplicantDto> getAllApplicants() {
        return applicantMapper.toDtoList(applicantRepository.findAll()).stream()
            .map(this::enrichWithAccountState)
            .toList();
    }

    @Cacheable(cacheNames = "applicantById", key = "#id")
    @Transactional(readOnly = true)
    public ApplicantDto getApplicantById(Long id) {
        Applicant applicant = findApplicantOrThrow(id);
        return enrichWithAccountState(applicantMapper.toDto(applicant));
    }

    @Cacheable(cacheNames = "applicantByPassport", key = "#passportNumber")
    @Transactional(readOnly = true)
    public ApplicantDto getApplicantByPassport(String passportNumber) {
        Applicant applicant = applicantRepository.findByPassportNumber(passportNumber)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Заявитель с паспортом " + passportNumber + " не найден"));
        return enrichWithAccountState(applicantMapper.toDto(applicant));
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "applicants", allEntries = true),
        @CacheEvict(cacheNames = "applicantById", allEntries = true),
        @CacheEvict(cacheNames = "applicantByPassport", allEntries = true)
    })
    @Transactional
    public ApplicantDto createApplicant(ApplicantCreateDto createDto) {
        if (applicantRepository.existsByPassportNumber(createDto.getPassportNumber())) {
            throw new BusinessException(
                "Заявитель с паспортом " + createDto.getPassportNumber() +
                    " уже существует");
        }

        Applicant applicant = applicantMapper.toEntity(createDto);
        Applicant savedApplicant = applicantRepository.save(applicant);
        log.info("Created applicant with id: {}, passport: {}",
            savedApplicant.getId(), savedApplicant.getPassportNumber());

        return enrichWithAccountState(applicantMapper.toDto(savedApplicant));
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "applicants", allEntries = true),
        @CacheEvict(cacheNames = "applicantById", allEntries = true),
        @CacheEvict(cacheNames = "applicantByPassport", allEntries = true)
    })
    @Transactional
    public ApplicantDto updateApplicant(Long id, ApplicantCreateDto updateDto) {
        Applicant applicant = findApplicantOrThrow(id);
        applicantMapper.updateEntity(applicant, updateDto);
        Applicant updatedApplicant = applicantRepository.save(applicant);
        log.info("Updated applicant with id: {}", id);

        return enrichWithAccountState(applicantMapper.toDto(updatedApplicant));
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "applicants", allEntries = true),
        @CacheEvict(cacheNames = "applicantById", allEntries = true),
        @CacheEvict(cacheNames = "applicantByPassport", allEntries = true)
    })
    @Transactional
    public ApplicantDto changePassport(Long id, String newPassportNumber) {
        Applicant applicant = findApplicantOrThrow(id);

        if (applicantRepository.existsByPassportNumber(newPassportNumber)) {
            throw new BusinessException("Паспорт " + newPassportNumber + " уже используется"); // 409
        }

        if (applicant.getApplications() != null && !applicant.getApplications().isEmpty()) {
            boolean hasPendingApplications = applicant.getApplications().stream()
                .anyMatch(app -> app.getStatus() == ApplicationStatus.PENDING);

            if (hasPendingApplications) {
                throw new BusinessException("Нельзя изменить паспорт, пока есть заявление в статусе «На рассмотрении»");
            }
        }

        String previousPassportNumber = applicant.getPassportNumber();
        applicant.setPassportNumber(newPassportNumber);
        Applicant updated = applicantRepository.save(applicant);
        userAccountRepository.findByApplicantId(applicant.getId()).ifPresent(userAccount ->
            syncUsername(userAccount, previousPassportNumber, newPassportNumber));
        log.info("Changed passport for applicant {}: {} -> {}",
            id, previousPassportNumber, newPassportNumber);

        return enrichWithAccountState(applicantMapper.toDto(updated));
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "applicants", allEntries = true),
        @CacheEvict(cacheNames = "applicantById", allEntries = true),
        @CacheEvict(cacheNames = "applicantByPassport", allEntries = true)
    })
    @Transactional
    public ApplicantDto updateApplicantRole(Long id, UserRole role) {
        Applicant applicant = findApplicantOrThrow(id);
        UserAccount userAccount = userAccountRepository.findByApplicantId(applicant.getId())
            .orElseThrow(() -> new BusinessException("У заявителя нет связанной учетной записи"));

        userAccount.setRole(role);
        userAccountRepository.save(userAccount);
        log.info("Updated applicant role for applicant {} to {}", applicant.getId(), role);

        return enrichWithAccountState(applicantMapper.toDto(applicant));
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "applicants", allEntries = true),
        @CacheEvict(cacheNames = "applicantById", allEntries = true),
        @CacheEvict(cacheNames = "applicantByPassport", allEntries = true)
    })
    @Transactional
    public void deleteApplicant(Long id) {
        Applicant applicant = findApplicantOrThrow(id);

        if (applicant.getApplications() != null && !applicant.getApplications().isEmpty()) {
            throw new BusinessException("Нельзя удалить заявителя, у которого есть заявления"); // 409
        }

        userAccountRepository.findByApplicantId(applicant.getId()).ifPresent(userAccount -> {
            userAccountRepository.delete(userAccount);
            log.info("Deleted linked user account {} for applicant {}", userAccount.getId(), applicant.getId());
        });

        applicantRepository.delete(applicant);
        log.info("Deleted applicant with id: {}", id);
    }

    private Applicant findApplicantOrThrow(Long id) {
        return applicantRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Заявитель с id " + id + " не найден"));
    }

    private ApplicantDto enrichWithAccountState(ApplicantDto dto) {
        if (dto == null || dto.getId() == null) {
            return dto;
        }

        userAccountRepository.findByApplicantId(dto.getId()).ifPresentOrElse(
            userAccount -> {
                dto.setRole(userAccount.getRole().name());
                dto.setHasUserAccount(true);
            },
            () -> {
                dto.setRole(UserRole.USER.name());
                dto.setHasUserAccount(false);
            }
        );

        return dto;
    }

    private void syncUsername(UserAccount userAccount, String previousPassportNumber, String newPassportNumber) {
        if (previousPassportNumber.equalsIgnoreCase(userAccount.getUsername())) {
            userAccount.setUsername(newPassportNumber);
            userAccountRepository.save(userAccount);
        }
    }
}
