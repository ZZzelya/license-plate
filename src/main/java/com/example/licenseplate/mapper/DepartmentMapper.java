package com.example.licenseplate.mapper;

import com.example.licenseplate.dto.request.DepartmentCreateDto;
import com.example.licenseplate.dto.response.DepartmentDto;
import com.example.licenseplate.model.entity.RegistrationDept;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class DepartmentMapper {

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
        dto.setApplicationsCount(department.getApplications() != null ? department.getApplications().size() : 0);

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
        if (departments == null) {
            return Collections.emptyList();
        }
        return departments.stream()
            .map(this::toDto)
            .toList();
    }
}
