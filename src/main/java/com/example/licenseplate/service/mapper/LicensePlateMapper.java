package com.example.licenseplate.service.mapper;

import com.example.licenseplate.dto.request.LicensePlateCreateDto;
import com.example.licenseplate.dto.response.LicensePlateDto;
import com.example.licenseplate.model.entity.LicensePlate;
import com.example.licenseplate.model.enums.PlateStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LicensePlateMapper {

    public LicensePlateDto toDto(LicensePlate plate) {
        if (plate == null) {
            return null;
        }

        LicensePlateDto dto = new LicensePlateDto();
        dto.setId(plate.getId());
        dto.setPlateNumber(plate.getPlateNumber());
        dto.setStatus(plate.getStatus() != null ? plate.getStatus().name() : null);
        dto.setPrice(plate.getPrice());
        dto.setSeries(plate.getSeries());
        dto.setIssueDate(plate.getIssueDate());
        dto.setExpiryDate(plate.getExpiryDate());

        if (plate.getDepartment() != null) {
            dto.setDepartmentId(plate.getDepartment().getId());
            dto.setDepartmentName(plate.getDepartment().getName());
        }

        return dto;
    }

    public LicensePlate toEntity(LicensePlateCreateDto dto) {
        if (dto == null) {
            return null;
        }

        LicensePlate plate = new LicensePlate();
        plate.setPlateNumber(dto.getPlateNumber());
        plate.setPrice(dto.getPrice());
        plate.setSeries(dto.getSeries());
        plate.setStatus(PlateStatus.AVAILABLE);

        return plate;
    }

    public void updateEntity(LicensePlate plate, LicensePlateCreateDto dto) {
        if (dto == null || plate == null) {
            return;
        }

        plate.setPlateNumber(dto.getPlateNumber());
        plate.setPrice(dto.getPrice());
        plate.setSeries(dto.getSeries());
    }

    public List<LicensePlateDto> toDtoList(List<LicensePlate> plates) {
        return plates.stream()
            .map(this::toDto)
            .toList();
    }
}