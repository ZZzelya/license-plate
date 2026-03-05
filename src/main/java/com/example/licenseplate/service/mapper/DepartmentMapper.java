package com.example.licenseplate.service.mapper;

import com.example.licenseplate.dto.request.DepartmentCreateDto;
import com.example.licenseplate.dto.response.DepartmentDto;
import com.example.licenseplate.dto.DepartmentWithPlatesDto;
import com.example.licenseplate.model.entity.RegistrationDept;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DepartmentMapper {

    private final LicensePlateMapper licensePlateMapper;

    public DepartmentDto toDto(RegistrationDept department) {
        if (department == null) {
            return null;
        }

        DepartmentDto dto = new DepartmentDto();
        dto.setId(department.getId());
        dto.setName(department.getName());
        dto.setAddress(department.getAddress());
        dto.setPhoneNumber(department.getPhoneNumber());
        dto.setRegion(department.getRegion());

        if (department.getLicensePlates() != null) {
            dto.setLicensePlatesCount(department.getLicensePlates().size());
        } else {
            dto.setLicensePlatesCount(0);
        }

        return dto;
    }

    public DepartmentWithPlatesDto toDtoWithPlates(RegistrationDept department) {
        if (department == null) {
            return null;
        }

        DepartmentWithPlatesDto dto = new DepartmentWithPlatesDto();
        dto.setId(department.getId());
        dto.setName(department.getName());
        dto.setAddress(department.getAddress());
        dto.setPhoneNumber(department.getPhoneNumber());
        dto.setRegion(department.getRegion());

        if (department.getLicensePlates() != null) {
            dto.setLicensePlates(licensePlateMapper.toDtoList(
                department.getLicensePlates()));
        }

        return dto;
    }

    public RegistrationDept toEntity(DepartmentCreateDto dto) {
        if (dto == null) {
            return null;
        }

        RegistrationDept department = new RegistrationDept();
        department.setName(dto.getName());
        department.setAddress(dto.getAddress());
        department.setPhoneNumber(dto.getPhoneNumber());
        department.setRegion(dto.getRegion());

        return department;
    }

    public void updateEntity(RegistrationDept department, DepartmentCreateDto dto) {
        if (dto == null || department == null) {
            return;
        }

        department.setName(dto.getName());
        department.setAddress(dto.getAddress());
        department.setPhoneNumber(dto.getPhoneNumber());
        department.setRegion(dto.getRegion());
    }

    public List<DepartmentDto> toDtoList(List<RegistrationDept> departments) {
        return departments.stream()
            .map(this::toDto)
            .toList();
    }
}