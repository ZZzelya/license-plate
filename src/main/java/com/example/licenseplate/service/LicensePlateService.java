package com.example.licenseplate.service;

import com.example.licenseplate.dto.request.LicensePlateCreateDto;
import com.example.licenseplate.dto.response.LicensePlateDto;
import com.example.licenseplate.exception.BusinessException;
import com.example.licenseplate.exception.ResourceNotFoundException;
import com.example.licenseplate.mapper.LicensePlateMapper;
import com.example.licenseplate.model.entity.LicensePlate;
import com.example.licenseplate.model.entity.RegistrationDept;
import com.example.licenseplate.repository.DepartmentRepository;
import com.example.licenseplate.repository.LicensePlateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class LicensePlateService {

    private static final int MIN_AVAILABLE_STANDARD = 10;
    private static final int MIN_AVAILABLE_ELECTRIC = 10;
    private static final char[] STANDARD_SERIES = PlateFormatSupport.ALLOWED_SERIES_LETTERS.toCharArray();
    private static final char[] ELECTRIC_SERIES = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private final LicensePlateRepository licensePlateRepository;
    private final DepartmentRepository departmentRepository;
    private final LicensePlateMapper licensePlateMapper;

    @Cacheable("licensePlates")
    @Transactional(readOnly = true)
    public List<LicensePlateDto> getAllLicensePlates() {
        return licensePlateMapper.toDtoList(licensePlateRepository.findAll());
    }

    @Cacheable(cacheNames = "licensePlateById", key = "#id")
    @Transactional(readOnly = true)
    public LicensePlateDto getLicensePlateById(Long id) {
        return licensePlateMapper.toDto(findLicensePlateOrThrow(id));
    }

    @Cacheable(cacheNames = "licensePlateByNumber", key = "#plateNumber")
    @Transactional(readOnly = true)
    public LicensePlateDto getLicensePlateByNumber(String plateNumber) {
        LicensePlate plate = licensePlateRepository.findByPlateNumber(PlateFormatSupport.normalize(plateNumber))
            .orElseThrow(() -> new ResourceNotFoundException("Номерной знак " + plateNumber + " не найден"));
        return licensePlateMapper.toDto(plate);
    }

    @Cacheable(cacheNames = "availablePlatesByRegion", key = "#region")
    @Transactional(readOnly = true)
    public List<LicensePlateDto> getAvailablePlatesByRegion(String region) {
        return licensePlateMapper.toDtoList(licensePlateRepository.findAvailableByRegion(region));
    }

    @Cacheable(cacheNames = "availablePlatesByDepartment", key = "#departmentId")
    @Transactional
    public List<LicensePlateDto> getAvailablePlatesByDepartment(Long departmentId) {
        return licensePlateMapper.toDtoList(getOrCreateAvailablePlatesByDepartment(departmentId));
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "licensePlates", allEntries = true),
        @CacheEvict(cacheNames = "licensePlateById", allEntries = true),
        @CacheEvict(cacheNames = "licensePlateByNumber", allEntries = true),
        @CacheEvict(cacheNames = "availablePlatesByRegion", allEntries = true),
        @CacheEvict(cacheNames = "availablePlatesByDepartment", allEntries = true),
        @CacheEvict(cacheNames = "applications", allEntries = true),
        @CacheEvict(cacheNames = "applicationById", allEntries = true),
        @CacheEvict(cacheNames = "applicationsByPassport", allEntries = true)
    })
    @Transactional
    public LicensePlateDto createLicensePlate(LicensePlateCreateDto createDto) {
        RegistrationDept department = findDepartment(createDto.getDepartmentId());
        String fullPlateNumber = composePlateNumber(createDto, department);

        if (licensePlateRepository.existsByPlateNumber(fullPlateNumber)) {
            throw new BusinessException("Номерной знак " + fullPlateNumber + " уже существует");
        }

        LicensePlate plate = new LicensePlate();
        plate.setPlateNumber(fullPlateNumber);
        plate.setSeries(PlateFormatSupport.normalize(createDto.getSeries()));
        plate.setPrice(BigDecimal.ZERO);
        plate.setDepartment(department);

        LicensePlate savedPlate = licensePlateRepository.save(plate);
        log.info("Created license plate: {} in department: {}", savedPlate.getPlateNumber(), department.getName());
        return licensePlateMapper.toDto(savedPlate);
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "licensePlates", allEntries = true),
        @CacheEvict(cacheNames = "licensePlateById", allEntries = true),
        @CacheEvict(cacheNames = "licensePlateByNumber", allEntries = true),
        @CacheEvict(cacheNames = "availablePlatesByRegion", allEntries = true),
        @CacheEvict(cacheNames = "availablePlatesByDepartment", allEntries = true),
        @CacheEvict(cacheNames = "applications", allEntries = true),
        @CacheEvict(cacheNames = "applicationById", allEntries = true),
        @CacheEvict(cacheNames = "applicationsByPassport", allEntries = true)
    })
    @Transactional
    public LicensePlateDto updateLicensePlate(Long id, LicensePlateCreateDto updateDto) {
        LicensePlate plate = findLicensePlateOrThrow(id);
        RegistrationDept department = findDepartment(updateDto.getDepartmentId());
        String fullPlateNumber = composePlateNumber(updateDto, department);

        if (!fullPlateNumber.equals(plate.getPlateNumber()) && licensePlateRepository.existsByPlateNumber(fullPlateNumber)) {
            throw new BusinessException("Номерной знак " + fullPlateNumber + " уже существует");
        }

        plate.setPlateNumber(fullPlateNumber);
        plate.setSeries(PlateFormatSupport.normalize(updateDto.getSeries()));
        plate.setPrice(plate.getPrice() == null ? BigDecimal.ZERO : plate.getPrice());
        plate.setDepartment(department);

        LicensePlate updatedPlate = licensePlateRepository.save(plate);
        log.info("Updated license plate with id: {}", id);
        return licensePlateMapper.toDto(updatedPlate);
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "licensePlates", allEntries = true),
        @CacheEvict(cacheNames = "licensePlateById", allEntries = true),
        @CacheEvict(cacheNames = "licensePlateByNumber", allEntries = true),
        @CacheEvict(cacheNames = "availablePlatesByRegion", allEntries = true),
        @CacheEvict(cacheNames = "availablePlatesByDepartment", allEntries = true),
        @CacheEvict(cacheNames = "applications", allEntries = true),
        @CacheEvict(cacheNames = "applicationById", allEntries = true),
        @CacheEvict(cacheNames = "applicationsByPassport", allEntries = true)
    })
    @Transactional
    public void deleteLicensePlate(Long id) {
        LicensePlate plate = findLicensePlateOrThrow(id);

        if (plate.getApplications() != null && !plate.getApplications().isEmpty()) {
            throw new BusinessException("Нельзя удалить номерной знак, у которого есть заявления");
        }

        licensePlateRepository.delete(plate);
        log.info("Deleted license plate with id: {}", id);
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "licensePlates", allEntries = true),
        @CacheEvict(cacheNames = "licensePlateById", allEntries = true),
        @CacheEvict(cacheNames = "licensePlateByNumber", allEntries = true),
        @CacheEvict(cacheNames = "availablePlatesByRegion", allEntries = true),
        @CacheEvict(cacheNames = "availablePlatesByDepartment", allEntries = true)
    })
    @Transactional
    public List<LicensePlate> getOrCreateAvailablePlatesByDepartment(Long departmentId) {
        RegistrationDept department = findDepartment(departmentId);
        List<LicensePlate> availablePlates = new ArrayList<>(licensePlateRepository.findAvailableByDepartmentId(departmentId));

        long standardCount = availablePlates.stream().filter(plate -> !isElectricPlate(plate)).count();
        long electricCount = availablePlates.stream().filter(this::isElectricPlate).count();

        if (standardCount < MIN_AVAILABLE_STANDARD) {
            createGeneratedPlates(department, MIN_AVAILABLE_STANDARD - (int) standardCount, false);
        }
        if (electricCount < MIN_AVAILABLE_ELECTRIC) {
            createGeneratedPlates(department, MIN_AVAILABLE_ELECTRIC - (int) electricCount, true);
        }

        return licensePlateRepository.findAvailableByDepartmentId(departmentId);
    }

    private void createGeneratedPlates(RegistrationDept department, int amount, boolean electric) {
        String regionCode = resolveRegionCode(null, department.getRegion());
        int created = 0;
        int attempts = 0;

        while (created < amount && attempts < amount * 200) {
            attempts++;

            String numberPart = electric ? generateElectricNumberPart() : generateStandardNumberPart();
            String series = electric ? generateSeries(ELECTRIC_SERIES) : generateSeries(STANDARD_SERIES);
            String fullPlateNumber = PlateFormatSupport.buildPlateNumber(numberPart, series, regionCode);

            if (licensePlateRepository.existsByPlateNumber(fullPlateNumber)) {
                continue;
            }

            LicensePlate plate = new LicensePlate();
            plate.setPlateNumber(fullPlateNumber);
            plate.setSeries(series);
            plate.setPrice(BigDecimal.ZERO);
            plate.setDepartment(department);
            licensePlateRepository.save(plate);
            created++;
        }
    }

    private String generateStandardNumberPart() {
        while (true) {
            int value = ThreadLocalRandom.current().nextInt(1000, 10000);
            String numberPart = String.format("%04d", value);
            if (!isPersonalizedLikeStandard(numberPart)) {
                return numberPart;
            }
        }
    }

    private String generateElectricNumberPart() {
        int value = ThreadLocalRandom.current().nextInt(1, 1000);
        return "E" + String.format("%03d", value);
    }

    private String generateSeries(char[] alphabet) {
        return new String(new char[]{
            alphabet[ThreadLocalRandom.current().nextInt(alphabet.length)],
            alphabet[ThreadLocalRandom.current().nextInt(alphabet.length)]
        });
    }

    private boolean isPersonalizedLikeStandard(String numberPart) {
        return numberPart.chars().distinct().count() == 1;
    }

    private boolean isElectricPlate(LicensePlate plate) {
        PlateFormatSupport.PlateParts parts = PlateFormatSupport.parse(plate.getPlateNumber());
        return parts != null && PlateFormatSupport.isElectricNumberPart(parts.numberPart());
    }

    private RegistrationDept findDepartment(Long departmentId) {
        return departmentRepository.findById(departmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Отделение с id " + departmentId + " не найдено"));
    }

    private String composePlateNumber(LicensePlateCreateDto dto, RegistrationDept department) {
        String numberPart = PlateFormatSupport.normalize(dto.getPlateNumber());
        String series = PlateFormatSupport.normalize(dto.getSeries());
        String regionCode = resolveRegionCode(dto.getRegionCode(), department.getRegion());
        String fullPlateNumber = PlateFormatSupport.buildPlateNumber(numberPart, series, regionCode);

        if (PlateFormatSupport.parse(fullPlateNumber) == null) {
            throw new BusinessException("Неверный формат номерного знака");
        }

        return fullPlateNumber;
    }

    private String resolveRegionCode(String requestedCode, String departmentRegion) {
        Set<String> allowedCodes = PlateFormatSupport.resolveAllowedRegionCodes(departmentRegion);
        if (allowedCodes.isEmpty()) {
            throw new BusinessException(
                "Для отделения не удалось определить код региона по значению '" + departmentRegion + "'");
        }

        if (requestedCode == null || requestedCode.isBlank()) {
            return allowedCodes.iterator().next();
        }

        String normalizedCode = PlateFormatSupport.normalize(requestedCode);
        if (!allowedCodes.contains(normalizedCode)) {
            throw new BusinessException(
                "Код региона " + normalizedCode + " не соответствует отделению '" + departmentRegion + "'");
        }

        return normalizedCode;
    }

    private LicensePlate findLicensePlateOrThrow(Long id) {
        return licensePlateRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Номерной знак с id " + id + " не найден"));
    }
}
