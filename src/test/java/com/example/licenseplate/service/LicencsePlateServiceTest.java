package com.example.licenseplate.service;

import com.example.licenseplate.dto.request.LicensePlateCreateDto;
import com.example.licenseplate.dto.response.LicensePlateDto;
import com.example.licenseplate.exception.BusinessException;
import com.example.licenseplate.exception.ResourceNotFoundException;
import com.example.licenseplate.model.entity.Application;
import com.example.licenseplate.model.entity.LicensePlate;
import com.example.licenseplate.model.entity.RegistrationDept;
import com.example.licenseplate.repository.DepartmentRepository;
import com.example.licenseplate.repository.LicensePlateRepository;
import com.example.licenseplate.mapper.LicensePlateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LicensePlateService Coverage Tests")
class LicencsePlateServiceTest {

    @Mock
    private LicensePlateRepository licensePlateRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private LicensePlateMapper licensePlateMapper;

    @InjectMocks
    private LicensePlateService licensePlateService;

    private RegistrationDept testDept;
    private LicensePlate testPlate;
    private LicensePlateDto testPlateDto;
    private LicensePlateCreateDto testCreateDto;

    @BeforeEach
    void setUp() {
        testDept = RegistrationDept.builder()
            .id(1L)
            .name("МРЭО ГАИ")
            .region("MINSK")
            .build();

        testPlate = LicensePlate.builder()
            .id(1L)
            .plateNumber("1234 AB-7")
            .price(BigDecimal.valueOf(100))
            .series("AB")
            .department(testDept)
            .applications(new ArrayList<>())
            .build();

        testPlateDto = LicensePlateDto.builder()
            .id(1L)
            .plateNumber("1234 AB-7")
            .price(BigDecimal.valueOf(100))
            .series("AB")
            .departmentId(1L)
            .departmentName("МРЭО ГАИ")
            .build();

        testCreateDto = LicensePlateCreateDto.builder()
            .plateNumber("1234 AB-7")
            .price(BigDecimal.valueOf(100))
            .series("AB")
            .departmentId(1L)
            .build();
    }

    @Nested
    @DisplayName("getAllLicensePlates()")
    class GetAllLicensePlatesTests {

        @Test
        @DisplayName("Should return list of license plates")
        void shouldReturnListOfLicensePlates() {
            List<LicensePlate> plates = List.of(testPlate);
            List<LicensePlateDto> expectedDtos = List.of(testPlateDto);

            when(licensePlateRepository.findAll()).thenReturn(plates);
            when(licensePlateMapper.toDtoList(plates)).thenReturn(expectedDtos);

            List<LicensePlateDto> result = licensePlateService.getAllLicensePlates();

            assertThat(result).isEqualTo(expectedDtos);
            verify(licensePlateRepository).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no plates")
        void shouldReturnEmptyListWhenNoPlates() {
            when(licensePlateRepository.findAll()).thenReturn(Collections.emptyList());
            when(licensePlateMapper.toDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

            List<LicensePlateDto> result = licensePlateService.getAllLicensePlates();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getLicensePlateById()")
    class GetLicensePlateByIdTests {

        @Test
        @DisplayName("Should return license plate when id exists")
        void shouldReturnLicensePlateWhenIdExists() {
            when(licensePlateRepository.findById(1L)).thenReturn(Optional.of(testPlate));
            when(licensePlateMapper.toDto(testPlate)).thenReturn(testPlateDto);

            LicensePlateDto result = licensePlateService.getLicensePlateById(1L);

            assertThat(result).isEqualTo(testPlateDto);
            verify(licensePlateRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when id not found")
        void shouldThrowNotFoundExceptionWhenIdNotFound() {
            when(licensePlateRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> licensePlateService.getLicensePlateById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("License plate not found with id: 999");
        }
    }

    @Nested
    @DisplayName("getLicensePlateByNumber()")
    class GetLicensePlateByNumberTests {

        @Test
        @DisplayName("Should return license plate when number exists")
        void shouldReturnLicensePlateWhenNumberExists() {
            when(licensePlateRepository.findByPlateNumber("1234 AB-7"))
                .thenReturn(Optional.of(testPlate));
            when(licensePlateMapper.toDto(testPlate)).thenReturn(testPlateDto);

            LicensePlateDto result = licensePlateService.getLicensePlateByNumber("1234 AB-7");

            assertThat(result).isEqualTo(testPlateDto);
            verify(licensePlateRepository).findByPlateNumber("1234 AB-7");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when number not found")
        void shouldThrowNotFoundExceptionWhenNumberNotFound() {
            when(licensePlateRepository.findByPlateNumber("NOT_EXIST"))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> licensePlateService.getLicensePlateByNumber("NOT_EXIST"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("License plate not found: NOT_EXIST");
        }
    }

    @Nested
    @DisplayName("getAvailablePlatesByRegion()")
    class GetAvailablePlatesByRegionTests {

        @Test
        @DisplayName("Should return available plates by region")
        void shouldReturnAvailablePlatesByRegion() {
            List<LicensePlate> plates = List.of(testPlate);
            List<LicensePlateDto> expectedDtos = List.of(testPlateDto);

            when(licensePlateRepository.findAvailableByRegion("MINSK")).thenReturn(plates);
            when(licensePlateMapper.toDtoList(plates)).thenReturn(expectedDtos);

            List<LicensePlateDto> result = licensePlateService.getAvailablePlatesByRegion("MINSK");

            assertThat(result).isEqualTo(expectedDtos);
            verify(licensePlateRepository).findAvailableByRegion("MINSK");
        }

        @Test
        @DisplayName("Should return empty list when no available plates")
        void shouldReturnEmptyListWhenNoAvailablePlates() {
            when(licensePlateRepository.findAvailableByRegion("MINSK")).thenReturn(Collections.emptyList());
            when(licensePlateMapper.toDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

            List<LicensePlateDto> result = licensePlateService.getAvailablePlatesByRegion("MINSK");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("createLicensePlate()")
    class CreateLicensePlateTests {

        @Test
        @DisplayName("Should create license plate successfully")
        void shouldCreateLicensePlateSuccessfully() {
            when(licensePlateRepository.existsByPlateNumber("1234 AB-7")).thenReturn(false);
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDept));
            when(licensePlateMapper.toEntity(testCreateDto)).thenReturn(testPlate);
            when(licensePlateRepository.save(any(LicensePlate.class))).thenReturn(testPlate);
            when(licensePlateMapper.toDto(testPlate)).thenReturn(testPlateDto);

            LicensePlateDto result = licensePlateService.createLicensePlate(testCreateDto);

            assertThat(result).isEqualTo(testPlateDto);
            verify(licensePlateRepository).existsByPlateNumber("1234 AB-7");
            verify(departmentRepository).findById(1L);
            verify(licensePlateRepository).save(any(LicensePlate.class));
        }

        @Test
        @DisplayName("Should throw BusinessException when plate number already exists")
        void shouldThrowExceptionWhenPlateNumberExists() {
            when(licensePlateRepository.existsByPlateNumber("1234 AB-7")).thenReturn(true);

            assertThatThrownBy(() -> licensePlateService.createLicensePlate(testCreateDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("License plate 1234 AB-7 already exists");

            verify(licensePlateRepository).existsByPlateNumber("1234 AB-7");
            verify(departmentRepository, never()).findById(any());
            verify(licensePlateRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when department not found")
        void shouldThrowExceptionWhenDepartmentNotFound() {
            when(licensePlateRepository.existsByPlateNumber("1234 AB-7")).thenReturn(false);
            when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> licensePlateService.createLicensePlate(testCreateDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Department not found with id: 1");
        }
    }

    @Nested
    @DisplayName("updateLicensePlate()")
    class UpdateLicensePlateTests {

        @Test
        @DisplayName("Should update license plate successfully when number not changed")
        void shouldUpdateLicensePlateSuccessfullyWhenNumberNotChanged() {
            when(licensePlateRepository.findById(1L)).thenReturn(Optional.of(testPlate));
            doNothing().when(licensePlateMapper).updateEntity(testPlate, testCreateDto);
            when(licensePlateRepository.save(testPlate)).thenReturn(testPlate);
            when(licensePlateMapper.toDto(testPlate)).thenReturn(testPlateDto);

            LicensePlateDto result = licensePlateService.updateLicensePlate(1L, testCreateDto);

            assertThat(result).isEqualTo(testPlateDto);
            verify(licensePlateRepository).findById(1L);
            verify(licensePlateRepository, never()).existsByPlateNumber(any());
            verify(licensePlateMapper).updateEntity(testPlate, testCreateDto);
            verify(licensePlateRepository).save(testPlate);
        }

        @Test
        @DisplayName("Should update license plate successfully when number changed and unique")
        void shouldUpdateLicensePlateSuccessfullyWhenNumberChanged() {
            LicensePlateCreateDto updateDto = LicensePlateCreateDto.builder()
                .plateNumber("5678 CD-9")
                .price(BigDecimal.valueOf(150))
                .series("CD")
                .departmentId(1L)
                .build();

            when(licensePlateRepository.findById(1L)).thenReturn(Optional.of(testPlate));
            when(licensePlateRepository.existsByPlateNumber("5678 CD-9")).thenReturn(false);
            doNothing().when(licensePlateMapper).updateEntity(testPlate, updateDto);
            when(licensePlateRepository.save(testPlate)).thenReturn(testPlate);
            when(licensePlateMapper.toDto(testPlate)).thenReturn(testPlateDto);

            LicensePlateDto result = licensePlateService.updateLicensePlate(1L, updateDto);

            assertThat(result).isEqualTo(testPlateDto);
            verify(licensePlateRepository).existsByPlateNumber("5678 CD-9");
        }

        @Test
        @DisplayName("Should throw BusinessException when new number already exists")
        void shouldThrowExceptionWhenNewNumberExists() {
            LicensePlateCreateDto updateDto = LicensePlateCreateDto.builder()
                .plateNumber("5678 CD-9")
                .price(BigDecimal.valueOf(150))
                .build();

            when(licensePlateRepository.findById(1L)).thenReturn(Optional.of(testPlate));
            when(licensePlateRepository.existsByPlateNumber("5678 CD-9")).thenReturn(true);

            assertThatThrownBy(() -> licensePlateService.updateLicensePlate(1L, updateDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("License plate 5678 CD-9 already exists");

            verify(licensePlateMapper, never()).updateEntity(any(), any());
            verify(licensePlateRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when id not found")
        void shouldThrowNotFoundExceptionWhenIdNotFound() {
            when(licensePlateRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> licensePlateService.updateLicensePlate(999L, testCreateDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("License plate not found with id: 999");
        }
    }

    @Nested
    @DisplayName("deleteLicensePlate()")
    class DeleteLicensePlateTests {

        @Test
        @DisplayName("Should delete license plate successfully when no applications")
        void shouldDeleteLicensePlateSuccessfully() {
            when(licensePlateRepository.findById(1L)).thenReturn(Optional.of(testPlate));
            doNothing().when(licensePlateRepository).delete(testPlate);

            licensePlateService.deleteLicensePlate(1L);

            verify(licensePlateRepository).findById(1L);
            verify(licensePlateRepository).delete(testPlate);
        }

        @Test
        @DisplayName("Should throw BusinessException when plate has applications")
        void shouldThrowExceptionWhenPlateHasApplications() {
            testPlate.setApplications(List.of(new Application()));

            when(licensePlateRepository.findById(1L)).thenReturn(Optional.of(testPlate));

            assertThatThrownBy(() -> licensePlateService.deleteLicensePlate(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot delete license plate with existing applications");

            verify(licensePlateRepository).findById(1L);
            verify(licensePlateRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should handle null applications list gracefully")
        void shouldHandleNullApplicationsList() {
            testPlate.setApplications(null);

            when(licensePlateRepository.findById(1L)).thenReturn(Optional.of(testPlate));
            doNothing().when(licensePlateRepository).delete(testPlate);

            licensePlateService.deleteLicensePlate(1L);

            verify(licensePlateRepository).findById(1L);
            verify(licensePlateRepository).delete(testPlate);
        }

        @Test
        @DisplayName("Should handle empty applications list gracefully")
        void shouldHandleEmptyApplicationsList() {
            testPlate.setApplications(Collections.emptyList());

            when(licensePlateRepository.findById(1L)).thenReturn(Optional.of(testPlate));
            doNothing().when(licensePlateRepository).delete(testPlate);

            licensePlateService.deleteLicensePlate(1L);

            verify(licensePlateRepository).findById(1L);
            verify(licensePlateRepository).delete(testPlate);
        }
    }
}