package com.example.licenseplate.mapper;

import com.example.licenseplate.dto.response.ApplicationDto;
import com.example.licenseplate.model.entity.Application;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ApplicationMapper {

    private final ServiceMapper serviceMapper;

    public ApplicationDto toDto(Application application) {
        if (application == null) {
            return null;
        }

        ApplicationDto dto = new ApplicationDto();
        dto.setId(application.getId());
        dto.setStatus(application.getStatus() != null ?
            application.getStatus().name() : null);
        dto.setSubmissionDate(application.getSubmissionDate());
        dto.setReservedUntil(application.getReservedUntil());
        dto.setConfirmationDate(application.getConfirmationDate());
        dto.setPaymentAmount(application.getPaymentAmount());
        dto.setNotes(application.getNotes());
        dto.setVehicleVin(application.getVehicleVin());
        dto.setVehicleModel(application.getVehicleModel());
        dto.setVehicleYear(application.getVehicleYear());

        if (application.getApplicant() != null) {
            dto.setApplicantId(application.getApplicant().getId());
            dto.setApplicantName(application.getApplicant().getFullName());
            dto.setApplicantPassport(application.getApplicant().getPassportNumber());
        }

        if (application.getLicensePlate() != null) {
            dto.setLicensePlateId(application.getLicensePlate().getId());
            dto.setLicensePlateNumber(application.getLicensePlate().getPlateNumber());
        }

        if (application.getDepartment() != null) {
            dto.setDepartmentId(application.getDepartment().getId());
            dto.setDepartmentName(application.getDepartment().getName());
        }

        if (application.getAdditionalServices() != null) {
            dto.setAdditionalServices(serviceMapper.toNameList(
                application.getAdditionalServices()));
        }

        return dto;
    }

    public List<ApplicationDto> toDtoList(List<Application> applications) {
        if (applications == null) {
            return Collections.emptyList();
        }
        return applications.stream()
            .map(this::toDto)
            .toList();
    }
}