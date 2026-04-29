package com.example.licenseplate.service;

import com.example.licenseplate.dto.request.DepartmentCreateDto;
import com.example.licenseplate.dto.response.DepartmentDto;
import com.example.licenseplate.exception.BusinessException;
import com.example.licenseplate.exception.ResourceNotFoundException;
import com.example.licenseplate.mapper.DepartmentMapper;
import com.example.licenseplate.model.entity.RegistrationDept;
import com.example.licenseplate.repository.DepartmentRepository;
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
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    @Cacheable("departments")
    @Transactional(readOnly = true)
    public List<DepartmentDto> getAllDepartments() {
        return departmentMapper.toDtoList(departmentRepository.findAll());
    }

    @Cacheable(cacheNames = "departmentById", key = "#id")
    @Transactional(readOnly = true)
    public DepartmentDto getDepartmentById(Long id) {
        RegistrationDept department = findDepartmentOrThrow(id);
        return departmentMapper.toDto(department);
    }

    @Cacheable(cacheNames = "departmentsByRegion", key = "#region")
    @Transactional(readOnly = true)
    public List<DepartmentDto> getDepartmentsByRegion(String region) {
        return departmentMapper.toDtoList(departmentRepository.findByRegionIgnoreCase(region));
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "departments", allEntries = true),
        @CacheEvict(cacheNames = "departmentById", allEntries = true),
        @CacheEvict(cacheNames = "departmentsByRegion", allEntries = true),
        @CacheEvict(cacheNames = "applications", allEntries = true),
        @CacheEvict(cacheNames = "applicationById", allEntries = true),
        @CacheEvict(cacheNames = "applicationsByPassport", allEntries = true),
        @CacheEvict(cacheNames = "licensePlates", allEntries = true),
        @CacheEvict(cacheNames = "licensePlateById", allEntries = true),
        @CacheEvict(cacheNames = "licensePlateByNumber", allEntries = true),
        @CacheEvict(cacheNames = "availablePlatesByRegion", allEntries = true),
        @CacheEvict(cacheNames = "availablePlatesByDepartment", allEntries = true)
    })
    @Transactional
    public DepartmentDto createDepartment(DepartmentCreateDto createDto) {
        if (departmentRepository.existsByPhoneNumber(createDto.getPhoneNumber())) {
            throw new BusinessException(
                "РћС‚РґРµР»РµРЅРёРµ СЃ С‚РµР»РµС„РѕРЅРѕРј " + createDto.getPhoneNumber() +
                    " СѓР¶Рµ СЃСѓС‰РµСЃС‚РІСѓРµС‚");
        }

        RegistrationDept department = departmentMapper.toEntity(createDto);
        RegistrationDept savedDepartment = departmentRepository.save(department);
        log.info("Created department with id: {}, name: {}", savedDepartment.getId(), savedDepartment.getName());

        return departmentMapper.toDto(savedDepartment);
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "departments", allEntries = true),
        @CacheEvict(cacheNames = "departmentById", allEntries = true),
        @CacheEvict(cacheNames = "departmentsByRegion", allEntries = true),
        @CacheEvict(cacheNames = "applications", allEntries = true),
        @CacheEvict(cacheNames = "applicationById", allEntries = true),
        @CacheEvict(cacheNames = "applicationsByPassport", allEntries = true),
        @CacheEvict(cacheNames = "licensePlates", allEntries = true),
        @CacheEvict(cacheNames = "licensePlateById", allEntries = true),
        @CacheEvict(cacheNames = "licensePlateByNumber", allEntries = true),
        @CacheEvict(cacheNames = "availablePlatesByRegion", allEntries = true),
        @CacheEvict(cacheNames = "availablePlatesByDepartment", allEntries = true)
    })
    @Transactional
    public DepartmentDto updateDepartment(Long id, DepartmentCreateDto updateDto) {
        RegistrationDept department = findDepartmentOrThrow(id);
        departmentMapper.updateEntity(department, updateDto);
        RegistrationDept updatedDepartment = departmentRepository.save(department);
        log.info("Updated department with id: {}", id);

        return departmentMapper.toDto(updatedDepartment);
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "departments", allEntries = true),
        @CacheEvict(cacheNames = "departmentById", allEntries = true),
        @CacheEvict(cacheNames = "departmentsByRegion", allEntries = true),
        @CacheEvict(cacheNames = "applications", allEntries = true),
        @CacheEvict(cacheNames = "applicationById", allEntries = true),
        @CacheEvict(cacheNames = "applicationsByPassport", allEntries = true),
        @CacheEvict(cacheNames = "licensePlates", allEntries = true),
        @CacheEvict(cacheNames = "licensePlateById", allEntries = true),
        @CacheEvict(cacheNames = "licensePlateByNumber", allEntries = true),
        @CacheEvict(cacheNames = "availablePlatesByRegion", allEntries = true),
        @CacheEvict(cacheNames = "availablePlatesByDepartment", allEntries = true)
    })
    @Transactional
    public void deleteDepartment(Long id) {
        RegistrationDept department = findDepartmentOrThrow(id);

        if (department.getApplications() != null && !department.getApplications().isEmpty()) {
            throw new BusinessException(
                "РќРµР»СЊР·СЏ СѓРґР°Р»РёС‚СЊ РѕС‚РґРµР»РµРЅРёРµ, РІ РєРѕС‚РѕСЂРѕРј РµСЃС‚СЊ Р·Р°СЏРІР»РµРЅРёСЏ");
        }

        departmentRepository.delete(department);
        log.info("Deleted department with id: {}", id);
    }

    @Transactional(readOnly = true)
    public void demonstrateNPlusOneProblem(String region) {
        log.info("=== Demonstrating department traversal for region: {} ===", region);

        List<RegistrationDept> departments = departmentRepository.findByRegionIgnoreCase(region);
        for (RegistrationDept dept : departments) {
            int applicationsCount = dept.getApplications().size();
            log.info("Department {} has {} applications", dept.getName(), applicationsCount);
        }
    }

    @Transactional(readOnly = true)
    public void solveNPlusOneWithFetchJoin(String region) {
        log.info("=== Traversing departments for region: {} ===", region);

        List<RegistrationDept> departments = departmentRepository.findByRegionIgnoreCase(region);
        for (RegistrationDept dept : departments) {
            int applicationsCount = dept.getApplications().size();
            log.info("Department {} has {} applications", dept.getName(), applicationsCount);
        }
    }

    private RegistrationDept findDepartmentOrThrow(Long id) {
        return departmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "РћС‚РґРµР»РµРЅРёРµ СЃ id " + id + " РЅРµ РЅР°Р№РґРµРЅРѕ"));
    }
}
