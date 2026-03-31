package com.example.licenseplate.service;

import com.example.licenseplate.dto.request.DepartmentCreateDto;
import com.example.licenseplate.dto.response.DepartmentDto;
import com.example.licenseplate.dto.DepartmentWithPlatesDto;
import com.example.licenseplate.exception.ResourceNotFoundException;
import com.example.licenseplate.exception.BusinessException;
import com.example.licenseplate.model.entity.RegistrationDept;
import com.example.licenseplate.repository.DepartmentRepository;
import com.example.licenseplate.mapper.DepartmentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    @Transactional(readOnly = true)
    public List<DepartmentDto> getAllDepartments() {
        return departmentMapper.toDtoList(departmentRepository.findAll());
    }

    @Transactional(readOnly = true)
    public DepartmentDto getDepartmentById(Long id) {
        RegistrationDept department = findDepartmentOrThrow(id);
        return departmentMapper.toDto(department);
    }

    @Transactional(readOnly = true)
    public DepartmentWithPlatesDto getDepartmentWithPlates(Long id) {
        RegistrationDept department = departmentRepository.findByIdWithPlates(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Department not found with id: " + id));
        return departmentMapper.toDtoWithPlates(department);
    }

    @Transactional(readOnly = true)
    public List<DepartmentDto> getDepartmentsByRegion(String region) {
        return departmentMapper.toDtoList(
            departmentRepository.findByRegionIgnoreCase(region));
    }

    @Transactional
    public DepartmentDto createDepartment(DepartmentCreateDto createDto) {
        if (departmentRepository.existsByPhoneNumber(createDto.getPhoneNumber())) {
            throw new BusinessException(
                "Department with phone " + createDto.getPhoneNumber() +
                    " already exists");
        }

        RegistrationDept department = departmentMapper.toEntity(createDto);
        RegistrationDept savedDepartment = departmentRepository.save(department);
        log.info("Created department with id: {}, name: {}",
            savedDepartment.getId(), savedDepartment.getName());

        return departmentMapper.toDto(savedDepartment);
    }

    @Transactional
    public DepartmentDto updateDepartment(Long id, DepartmentCreateDto updateDto) {
        RegistrationDept department = findDepartmentOrThrow(id);
        departmentMapper.updateEntity(department, updateDto);
        RegistrationDept updatedDepartment = departmentRepository.save(department);
        log.info("Updated department with id: {}", id);

        return departmentMapper.toDto(updatedDepartment);
    }

    @Transactional
    public void deleteDepartment(Long id) {
        RegistrationDept department = findDepartmentOrThrow(id);

        if (department.getLicensePlates() != null && !department.getLicensePlates().isEmpty()) {
            throw new BusinessException(
                "Cannot delete department with existing license plates");
        }

        departmentRepository.delete(department);
        log.info("Deleted department with id: {}", id);
    }

    @Transactional(readOnly = true)
    public void demonstrateNPlusOneProblem(String region) {
        log.info("=== Demonstrating N+1 problem for region: {} ===", region);

        List<RegistrationDept> departments =
            departmentRepository.findByRegionIgnoreCase(region);

        for (RegistrationDept dept : departments) {
            int platesCount = dept.getLicensePlates().size();
            log.info("Department {} has {} license plates", dept.getName(), platesCount);
        }
    }

    @Transactional(readOnly = true)
    public void solveNPlusOneWithFetchJoin(String region) {
        log.info("=== Solving N+1 with fetch join for region: {} ===", region);

        List<RegistrationDept> departments =
            departmentRepository.findByRegionWithPlatesFetch(region);

        for (RegistrationDept dept : departments) {
            int platesCount = dept.getLicensePlates().size();
            log.info("Department {} has {} license plates", dept.getName(), platesCount);
        }
    }

    private RegistrationDept findDepartmentOrThrow(Long id) {
        return departmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Department not found with id: " + id));
    }
}