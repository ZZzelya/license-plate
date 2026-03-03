package com.example.licenseplate.service;

import com.example.licenseplate.dto.request.ApplicantCreateDto;
import com.example.licenseplate.dto.response.ApplicantDto;
import com.example.licenseplate.exception.ResourceNotFoundException;
import com.example.licenseplate.exception.BusinessException;
import com.example.licenseplate.model.entity.Applicant;
import com.example.licenseplate.repository.ApplicantRepository;
import com.example.licenseplate.service.mapper.ApplicantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicantService {

    private final ApplicantRepository applicantRepository;
    private final ApplicantMapper applicantMapper;

    @Transactional(readOnly = true)
    public List<ApplicantDto> getAllApplicants() {
        return applicantMapper.toDtoList(applicantRepository.findAll());
    }

    @Transactional(readOnly = true)
    public ApplicantDto getApplicantById(Long id) {
        Applicant applicant = findApplicantOrThrow(id);
        return applicantMapper.toDto(applicant);
    }

    @Transactional(readOnly = true)
    public ApplicantDto getApplicantByPassport(String passportNumber) {
        Applicant applicant = applicantRepository.findByPassportNumber(passportNumber)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Applicant not found with passport: " + passportNumber));
        return applicantMapper.toDto(applicant);
    }

    @Transactional(readOnly = true)
    public List<ApplicantDto> getActiveApplicants() {
        return applicantMapper.toDtoList(applicantRepository.findAllActive());
    }

    @Transactional
    public ApplicantDto createApplicant(ApplicantCreateDto createDto) {
        if (applicantRepository.existsByPassportNumber(createDto.getPassportNumber())) {
            throw new BusinessException(
                "Applicant with passport " + createDto.getPassportNumber() +
                    " already exists");
        }

        Applicant applicant = applicantMapper.toEntity(createDto);
        Applicant savedApplicant = applicantRepository.save(applicant);
        log.info("Created applicant with id: {}, passport: {}",
            savedApplicant.getId(), savedApplicant.getPassportNumber());

        return applicantMapper.toDto(savedApplicant);
    }

    @Transactional
    public ApplicantDto updateApplicant(Long id, ApplicantCreateDto updateDto) {
        Applicant applicant = findApplicantOrThrow(id);
        applicantMapper.updateEntity(applicant, updateDto);
        Applicant updatedApplicant = applicantRepository.save(applicant);
        log.info("Updated applicant with id: {}", id);

        return applicantMapper.toDto(updatedApplicant);
    }

    @Transactional
    public void deleteApplicant(Long id) {
        Applicant applicant = findApplicantOrThrow(id);

        if (applicant.getApplications() != null &&
            !applicant.getApplications().isEmpty()) {
            throw new BusinessException(
                "Cannot delete applicant with existing applications");
        }

        applicantRepository.delete(applicant);
        log.info("Deleted applicant with id: {}", id);
    }

    private Applicant findApplicantOrThrow(Long id) {
        return applicantRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Applicant not found with id: " + id));
    }
}