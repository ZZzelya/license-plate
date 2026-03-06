package com.example.licenseplate.service;

import com.example.licenseplate.dto.request.LicensePlateCreateDto;
import com.example.licenseplate.dto.response.LicensePlateDto;
import com.example.licenseplate.exception.ResourceNotFoundException;
import com.example.licenseplate.exception.BusinessException;
import com.example.licenseplate.model.entity.LicensePlate;
import com.example.licenseplate.model.entity.RegistrationDept;
import com.example.licenseplate.repository.LicensePlateRepository;
import com.example.licenseplate.repository.DepartmentRepository;
import com.example.licenseplate.service.mapper.LicensePlateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LicensePlateService {

    private final LicensePlateRepository licensePlateRepository;
    private final DepartmentRepository departmentRepository;
    private final LicensePlateMapper licensePlateMapper;

    @Transactional(readOnly = true)
    public List<LicensePlateDto> getAllLicensePlates() {
        return licensePlateMapper.toDtoList(licensePlateRepository.findAll());
    }

    @Transactional(readOnly = true)
    public LicensePlateDto getLicensePlateById(Long id) {
        LicensePlate plate = findLicensePlateOrThrow(id);
        return licensePlateMapper.toDto(plate);
    }

    @Transactional(readOnly = true)
    public LicensePlateDto getLicensePlateByNumber(String plateNumber) {
        LicensePlate plate = licensePlateRepository.findByPlateNumber(plateNumber)
            .orElseThrow(() -> new ResourceNotFoundException(
                "License plate not found: " + plateNumber));
        return licensePlateMapper.toDto(plate);
    }

    @Transactional(readOnly = true)
    public List<LicensePlateDto> getAvailablePlatesByRegion(String region) {
        return licensePlateMapper.toDtoList(
            licensePlateRepository.findAvailableByRegion(region));
    }

    @Transactional
    public LicensePlateDto createLicensePlate(LicensePlateCreateDto createDto) {
        if (licensePlateRepository.existsByPlateNumber(createDto.getPlateNumber())) {
            throw new BusinessException(
                "License plate " + createDto.getPlateNumber() + " already exists");
        }

        RegistrationDept department = departmentRepository.findById(
                createDto.getDepartmentId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Department not found with id: " + createDto.getDepartmentId()));

        LicensePlate plate = licensePlateMapper.toEntity(createDto);
        plate.setDepartment(department);

        LicensePlate savedPlate = licensePlateRepository.save(plate);
        log.info("Created license plate: {} in department: {}",
            savedPlate.getPlateNumber(), department.getName());

        return licensePlateMapper.toDto(savedPlate);
    }

    @Transactional
    public LicensePlateDto updateLicensePlate(Long id, LicensePlateCreateDto updateDto) {
        LicensePlate plate = findLicensePlateOrThrow(id);

        if (!plate.getPlateNumber().equals(updateDto.getPlateNumber()) &&
            licensePlateRepository.existsByPlateNumber(updateDto.getPlateNumber())) {
            throw new BusinessException(
                "License plate " + updateDto.getPlateNumber() + " already exists");
        }

        licensePlateMapper.updateEntity(plate, updateDto);
        LicensePlate updatedPlate = licensePlateRepository.save(plate);
        log.info("Updated license plate with id: {}", id);

        return licensePlateMapper.toDto(updatedPlate);
    }

    @Transactional
    public void deleteLicensePlate(Long id) {
        LicensePlate plate = findLicensePlateOrThrow(id);

        if (plate.getApplications() != null && !plate.getApplications().isEmpty()) {
            throw new BusinessException(
                "Cannot delete license plate with existing applications");
        }

        licensePlateRepository.delete(plate);
        log.info("Deleted license plate with id: {}", id);
    }

    private LicensePlate findLicensePlateOrThrow(Long id) {
        return licensePlateRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "License plate not found with id: " + id));
    }
}