package com.example.licenseplate.service;

import com.example.licenseplate.dto.request.LicensePlateCreateDto;
import com.example.licenseplate.dto.response.LicensePlateDto;
import com.example.licenseplate.exception.BusinessException;
import com.example.licenseplate.exception.ResourceNotFoundException;
import com.example.licenseplate.mapper.LicensePlateMapper;
import com.example.licenseplate.model.entity.Application;
import com.example.licenseplate.model.entity.LicensePlate;
import com.example.licenseplate.model.entity.RegistrationDept;
import com.example.licenseplate.repository.DepartmentRepository;
import com.example.licenseplate.repository.LicensePlateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LicensePlateServiceTest {

    @Mock
    private LicensePlateRepository licensePlateRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private LicensePlateMapper licensePlateMapper;

    @InjectMocks
    private LicensePlateService licensePlateService;

    private RegistrationDept department;
    private LicensePlate plate;
    private LicensePlateDto plateDto;
    private LicensePlateCreateDto createDto;

    @BeforeEach
    void setUp() {
        department = RegistrationDept.builder().id(1L).name("Dept").region("Minsk").build();
        plate = LicensePlate.builder()
            .id(1L)
            .plateNumber("1234 AB-7")
            .series("AB")
            .price(BigDecimal.ONE)
            .department(department)
            .applications(new ArrayList<>())
            .build();
        plateDto = LicensePlateDto.builder().id(1L).plateNumber("1234 AB-7").series("AB").build();
        createDto = LicensePlateCreateDto.builder()
            .plateNumber("1234")
            .series("AB")
            .regionCode("7")
            .departmentId(1L)
            .build();
    }

    @Test
    void getAllLicensePlatesReturnsMappedList() {
        when(licensePlateRepository.findAll()).thenReturn(List.of(plate));
        when(licensePlateMapper.toDtoList(List.of(plate))).thenReturn(List.of(plateDto));

        assertThat(licensePlateService.getAllLicensePlates()).containsExactly(plateDto);
    }

    @Test
    void getAllLicensePlatesReturnsEmptyList() {
        when(licensePlateRepository.findAll()).thenReturn(List.of());
        when(licensePlateMapper.toDtoList(List.of())).thenReturn(List.of());

        assertThat(licensePlateService.getAllLicensePlates()).isEmpty();
    }

    @Test
    void getLicensePlateByIdThrowsWhenMissing() {
        when(licensePlateRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> licensePlateService.getLicensePlateById(1L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getLicensePlateByIdReturnsMappedPlate() {
        when(licensePlateRepository.findById(1L)).thenReturn(Optional.of(plate));
        when(licensePlateMapper.toDto(plate)).thenReturn(plateDto);

        assertThat(licensePlateService.getLicensePlateById(1L)).isEqualTo(plateDto);
    }

    @Test
    void getLicensePlateByNumberNormalizesInput() {
        when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.of(plate));
        when(licensePlateMapper.toDto(plate)).thenReturn(plateDto);

        assertThat(licensePlateService.getLicensePlateByNumber(" 1234 ab-7 ")).isEqualTo(plateDto);
    }

    @Test
    void getLicensePlateByNumberThrowsWhenMissing() {
        when(licensePlateRepository.findByPlateNumber("1234 AB-7")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> licensePlateService.getLicensePlateByNumber("1234 AB-7"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAvailableMethodsReturnMappedLists() {
        when(licensePlateRepository.findAvailableByRegion("Minsk")).thenReturn(List.of(plate));
        when(licensePlateRepository.findAvailableByDepartmentId(1L)).thenReturn(List.of(plate));
        when(licensePlateMapper.toDtoList(List.of(plate))).thenReturn(List.of(plateDto));

        assertThat(licensePlateService.getAvailablePlatesByRegion("Minsk")).containsExactly(plateDto);
        assertThat(licensePlateService.getAvailablePlatesByDepartment(1L)).containsExactly(plateDto);
    }

    @Test
    void createLicensePlateThrowsWhenDepartmentMissing() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> licensePlateService.createLicensePlate(createDto))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createLicensePlateThrowsWhenRegionCannotBeResolved() {
        RegistrationDept unknownRegionDepartment = RegistrationDept.builder().id(1L).region("Unknown").build();
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(unknownRegionDepartment));

        assertThatThrownBy(() -> licensePlateService.createLicensePlate(createDto))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void createLicensePlateThrowsWhenRegionCodeMismatchesDepartment() {
        RegistrationDept brestDepartment = RegistrationDept.builder().id(1L).region("Brest").build();
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(brestDepartment));

        assertThatThrownBy(() -> licensePlateService.createLicensePlate(createDto))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void createLicensePlateUsesDefaultRegionCodeWhenBlank() {
        LicensePlateCreateDto dto = LicensePlateCreateDto.builder()
            .plateNumber("1234")
            .series("AB")
            .regionCode("")
            .departmentId(1L)
            .build();

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(licensePlateRepository.existsByPlateNumber("1234 AB-7")).thenReturn(false);
        when(licensePlateRepository.save(any(LicensePlate.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(licensePlateMapper.toDto(any(LicensePlate.class))).thenReturn(plateDto);

        assertThat(licensePlateService.createLicensePlate(dto)).isEqualTo(plateDto);
    }

    @Test
    void createLicensePlateUsesDefaultRegionCodeWhenNull() {
        LicensePlateCreateDto dto = LicensePlateCreateDto.builder()
            .plateNumber("1234")
            .series("AB")
            .regionCode(null)
            .departmentId(1L)
            .build();

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(licensePlateRepository.existsByPlateNumber("1234 AB-7")).thenReturn(false);
        when(licensePlateRepository.save(any(LicensePlate.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(licensePlateMapper.toDto(any(LicensePlate.class))).thenReturn(plateDto);

        assertThat(licensePlateService.createLicensePlate(dto)).isEqualTo(plateDto);
    }

    @Test
    void createLicensePlateThrowsWhenDuplicateExists() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(licensePlateRepository.existsByPlateNumber("1234 AB-7")).thenReturn(true);

        assertThatThrownBy(() -> licensePlateService.createLicensePlate(createDto))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void createLicensePlateSavesAndMapsEntity() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(licensePlateRepository.existsByPlateNumber("1234 AB-7")).thenReturn(false);
        when(licensePlateRepository.save(any(LicensePlate.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(licensePlateMapper.toDto(any(LicensePlate.class))).thenReturn(plateDto);

        assertThat(licensePlateService.createLicensePlate(createDto)).isEqualTo(plateDto);
    }

    @Test
    void updateLicensePlateThrowsWhenMissing() {
        when(licensePlateRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> licensePlateService.updateLicensePlate(1L, createDto))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateLicensePlateThrowsWhenDuplicateForNewNumber() {
        when(licensePlateRepository.findById(1L)).thenReturn(Optional.of(plate));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));

        LicensePlateCreateDto other = LicensePlateCreateDto.builder()
            .plateNumber("9999")
            .series("AB")
            .regionCode("7")
            .departmentId(1L)
            .build();

        when(licensePlateRepository.existsByPlateNumber("9999 AB-7")).thenReturn(true);

        assertThatThrownBy(() -> licensePlateService.updateLicensePlate(1L, other))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void updateLicensePlatePreservesExistingPriceWhenPresent() {
        when(licensePlateRepository.findById(1L)).thenReturn(Optional.of(plate));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(licensePlateRepository.save(plate)).thenReturn(plate);
        when(licensePlateMapper.toDto(plate)).thenReturn(plateDto);

        assertThat(licensePlateService.updateLicensePlate(1L, createDto)).isEqualTo(plateDto);
        assertThat(plate.getPrice()).isEqualTo(BigDecimal.ONE);
    }

    @Test
    void updateLicensePlateThrowsWhenDepartmentMissing() {
        when(licensePlateRepository.findById(1L)).thenReturn(Optional.of(plate));
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> licensePlateService.updateLicensePlate(1L, createDto))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateLicensePlateKeepsSameNumberWithoutDuplicateCheck() {
        when(licensePlateRepository.findById(1L)).thenReturn(Optional.of(plate));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(licensePlateRepository.save(plate)).thenReturn(plate);
        when(licensePlateMapper.toDto(plate)).thenReturn(plateDto);

        assertThat(licensePlateService.updateLicensePlate(1L, createDto)).isEqualTo(plateDto);
    }

    @Test
    void updateLicensePlateSetsZeroPriceWhenNull() {
        plate.setPrice(null);
        when(licensePlateRepository.findById(1L)).thenReturn(Optional.of(plate));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(licensePlateRepository.save(plate)).thenReturn(plate);
        when(licensePlateMapper.toDto(plate)).thenReturn(plateDto);

        licensePlateService.updateLicensePlate(1L, createDto);

        assertThat(plate.getPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void updateLicensePlateAllowsChangedUniqueNumber() {
        LicensePlateCreateDto other = LicensePlateCreateDto.builder()
            .plateNumber("9999")
            .series("AB")
            .regionCode("7")
            .departmentId(1L)
            .build();

        when(licensePlateRepository.findById(1L)).thenReturn(Optional.of(plate));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(licensePlateRepository.existsByPlateNumber("9999 AB-7")).thenReturn(false);
        when(licensePlateRepository.save(plate)).thenReturn(plate);
        when(licensePlateMapper.toDto(plate)).thenReturn(plateDto);

        assertThat(licensePlateService.updateLicensePlate(1L, other)).isEqualTo(plateDto);
        assertThat(plate.getPlateNumber()).isEqualTo("9999 AB-7");
    }

    @Test
    void deleteLicensePlateThrowsWhenApplicationsExist() {
        plate.setApplications(List.of(new Application()));
        when(licensePlateRepository.findById(1L)).thenReturn(Optional.of(plate));

        assertThatThrownBy(() -> licensePlateService.deleteLicensePlate(1L))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void deleteLicensePlateDeletesWhenUnused() {
        when(licensePlateRepository.findById(1L)).thenReturn(Optional.of(plate));
        doNothing().when(licensePlateRepository).delete(plate);

        licensePlateService.deleteLicensePlate(1L);

        verify(licensePlateRepository).delete(plate);
    }

    @Test
    void deleteLicensePlateDeletesWhenApplicationsNull() {
        plate.setApplications(null);
        when(licensePlateRepository.findById(1L)).thenReturn(Optional.of(plate));
        doNothing().when(licensePlateRepository).delete(plate);

        licensePlateService.deleteLicensePlate(1L);

        verify(licensePlateRepository).delete(plate);
    }

    @Test
    void deleteLicensePlateThrowsWhenMissing() {
        when(licensePlateRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> licensePlateService.deleteLicensePlate(1L))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
