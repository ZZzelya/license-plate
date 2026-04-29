package com.example.licenseplate.mapper;

import com.example.licenseplate.dto.response.LicensePlateDto;
import com.example.licenseplate.model.entity.LicensePlate;
import com.example.licenseplate.service.PlateFormatSupport;
import org.springframework.stereotype.Component;

import java.util.Collections;
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
        dto.setIssueDate(plate.getIssueDate());
        dto.setExpiryDate(plate.getExpiryDate());

        PlateFormatSupport.PlateParts parts = PlateFormatSupport.parse(plate.getPlateNumber());
        dto.setNumberPart(parts != null ? parts.numberPart() : null);
        dto.setSeries(parts != null ? parts.series() : plate.getSeries());
        dto.setRegionCode(parts != null ? parts.regionCode() : null);

        if (plate.getDepartment() != null) {
            dto.setDepartmentId(plate.getDepartment().getId());
            dto.setDepartmentName(plate.getDepartment().getName());
            dto.setRegion(plate.getDepartment().getRegion());
        }

        return dto;
    }

    public List<LicensePlateDto> toDtoList(List<LicensePlate> plates) {
        if (plates == null) {
            return Collections.emptyList();
        }
        return plates.stream()
            .map(this::toDto)
            .toList();
    }
}
