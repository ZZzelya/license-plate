package com.example.licenseplate.service.mapper;

import com.example.licenseplate.dto.request.ApplicantCreateDto;
import com.example.licenseplate.dto.response.ApplicantDto;
import com.example.licenseplate.model.entity.Applicant;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ApplicantMapper {

    public ApplicantDto toDto(Applicant applicant) {
        if (applicant == null) {
            return null;
        }

        ApplicantDto dto = new ApplicantDto();
        dto.setId(applicant.getId());
        dto.setFullName(applicant.getFullName());
        dto.setPassportNumber(applicant.getPassportNumber());
        dto.setPhoneNumber(applicant.getPhoneNumber());
        dto.setEmail(applicant.getEmail());
        dto.setAddress(applicant.getAddress());
        dto.setIsActive(applicant.getIsActive());

        if (applicant.getApplications() != null) {
            dto.setApplicationsCount(applicant.getApplications().size());
        } else {
            dto.setApplicationsCount(0);
        }

        return dto;
    }

    public Applicant toEntity(ApplicantCreateDto dto) {
        if (dto == null) {
            return null;
        }

        Applicant applicant = new Applicant();
        applicant.setFullName(dto.getFullName());
        applicant.setPassportNumber(dto.getPassportNumber());
        applicant.setPhoneNumber(dto.getPhoneNumber());
        applicant.setEmail(dto.getEmail());
        applicant.setAddress(dto.getAddress());
        applicant.setIsActive(true);

        return applicant;
    }

    public void updateEntity(Applicant applicant, ApplicantCreateDto dto) {
        if (dto == null || applicant == null) {
            return;
        }

        applicant.setFullName(dto.getFullName());
        applicant.setPhoneNumber(dto.getPhoneNumber());
        applicant.setEmail(dto.getEmail());
        applicant.setAddress(dto.getAddress());
    }

    public List<ApplicantDto> toDtoList(List<Applicant> applicants) {
        return applicants.stream()
            .map(this::toDto)
            .toList();
    }
}