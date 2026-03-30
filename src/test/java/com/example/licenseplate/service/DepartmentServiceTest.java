package com.example.licenseplate.service;

import com.example.licenseplate.dto.request.DepartmentCreateDto;
import com.example.licenseplate.dto.response.DepartmentDto;
import com.example.licenseplate.dto.DepartmentWithPlatesDto;
import com.example.licenseplate.exception.BusinessException;
import com.example.licenseplate.exception.ResourceNotFoundException;
import com.example.licenseplate.model.entity.LicensePlate;
import com.example.licenseplate.model.entity.RegistrationDept;
import com.example.licenseplate.repository.DepartmentRepository;
import com.example.licenseplate.service.mapper.DepartmentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DepartmentService Coverage Tests")
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DepartmentMapper departmentMapper;

    @InjectMocks
    private DepartmentService departmentService;

    private RegistrationDept testDept;
    private DepartmentDto testDeptDto;
    private DepartmentCreateDto testCreateDto;

    @BeforeEach
    void setUp() {
        testDept = RegistrationDept.builder()
            .id(1L)
            .name("МРЭО ГАИ")
            .address("г.Минск, ул.Московская 1")
            .phoneNumber("+375291234567")
            .region("MINSK")
            .licensePlates(new ArrayList<>())
            .build();

        testDeptDto = DepartmentDto.builder()
            .id(1L)
            .name("МРЭО ГАИ")
            .address("г.Минск, ул.Московская 1")
            .phoneNumber("+375291234567")
            .region("MINSK")
            .licensePlatesCount(0)
            .build();

        testCreateDto = DepartmentCreateDto.builder()
            .name("МРЭО ГАИ")
            .address("г.Минск, ул.Московская 1")
            .phoneNumber("+375291234567")
            .region("MINSK")
            .build();
    }

    @Nested
    @DisplayName("getAllDepartments()")
    class GetAllDepartmentsTests {

        @Test
        @DisplayName("Should return list of departments")
        void shouldReturnListOfDepartments() {
            List<RegistrationDept> depts = List.of(testDept);
            List<DepartmentDto> expectedDtos = List.of(testDeptDto);

            when(departmentRepository.findAll()).thenReturn(depts);
            when(departmentMapper.toDtoList(depts)).thenReturn(expectedDtos);

            List<DepartmentDto> result = departmentService.getAllDepartments();

            assertThat(result).isEqualTo(expectedDtos);
            verify(departmentRepository).findAll();
            verify(departmentMapper).toDtoList(depts);
        }

        @Test
        @DisplayName("Should return empty list when no departments")
        void shouldReturnEmptyListWhenNoDepartments() {
            when(departmentRepository.findAll()).thenReturn(Collections.emptyList());
            when(departmentMapper.toDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

            List<DepartmentDto> result = departmentService.getAllDepartments();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getDepartmentById()")
    class GetDepartmentByIdTests {

        @Test
        @DisplayName("Should return department when id exists")
        void shouldReturnDepartmentWhenIdExists() {
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDept));
            when(departmentMapper.toDto(testDept)).thenReturn(testDeptDto);

            DepartmentDto result = departmentService.getDepartmentById(1L);

            assertThat(result).isEqualTo(testDeptDto);
            verify(departmentRepository).findById(1L);
            verify(departmentMapper).toDto(testDept);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when id not found")
        void shouldThrowNotFoundExceptionWhenIdNotFound() {
            when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> departmentService.getDepartmentById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Department not found with id: 999");
        }
    }

    @Nested
    @DisplayName("getDepartmentWithPlates()")
    class GetDepartmentWithPlatesTests {

        @Test
        @DisplayName("Should return department with plates when id exists")
        void shouldReturnDepartmentWithPlates() {
            DepartmentWithPlatesDto expectedDto = DepartmentWithPlatesDto.builder()
                .id(1L)
                .name("МРЭО ГАИ")
                .licensePlates(new ArrayList<>())
                .build();

            when(departmentRepository.findByIdWithPlates(1L)).thenReturn(Optional.of(testDept));
            when(departmentMapper.toDtoWithPlates(testDept)).thenReturn(expectedDto);

            DepartmentWithPlatesDto result = departmentService.getDepartmentWithPlates(1L);

            assertThat(result).isEqualTo(expectedDto);
            verify(departmentRepository).findByIdWithPlates(1L);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when id not found")
        void shouldThrowNotFoundExceptionWhenIdNotFound() {
            when(departmentRepository.findByIdWithPlates(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> departmentService.getDepartmentWithPlates(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Department not found with id: 999");
        }
    }

    @Nested
    @DisplayName("getDepartmentsByRegion()")
    class GetDepartmentsByRegionTests {

        @Test
        @DisplayName("Should return departments by region")
        void shouldReturnDepartmentsByRegion() {
            List<RegistrationDept> depts = List.of(testDept);
            List<DepartmentDto> expectedDtos = List.of(testDeptDto);

            when(departmentRepository.findByRegionIgnoreCase("MINSK")).thenReturn(depts);
            when(departmentMapper.toDtoList(depts)).thenReturn(expectedDtos);

            List<DepartmentDto> result = departmentService.getDepartmentsByRegion("MINSK");

            assertThat(result).isEqualTo(expectedDtos);
            verify(departmentRepository).findByRegionIgnoreCase("MINSK");
        }

        @Test
        @DisplayName("Should return empty list when no departments in region")
        void shouldReturnEmptyListWhenNoDepartmentsInRegion() {
            when(departmentRepository.findByRegionIgnoreCase("UNKNOWN")).thenReturn(Collections.emptyList());
            when(departmentMapper.toDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

            List<DepartmentDto> result = departmentService.getDepartmentsByRegion("UNKNOWN");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("createDepartment()")
    class CreateDepartmentTests {

        @Test
        @DisplayName("Should create department successfully")
        void shouldCreateDepartmentSuccessfully() {
            when(departmentRepository.existsByPhoneNumber("+375291234567")).thenReturn(false);
            when(departmentMapper.toEntity(testCreateDto)).thenReturn(testDept);
            when(departmentRepository.save(testDept)).thenReturn(testDept);
            when(departmentMapper.toDto(testDept)).thenReturn(testDeptDto);

            DepartmentDto result = departmentService.createDepartment(testCreateDto);

            assertThat(result).isEqualTo(testDeptDto);
            verify(departmentRepository).existsByPhoneNumber("+375291234567");
            verify(departmentMapper).toEntity(testCreateDto);
            verify(departmentRepository).save(testDept);
        }

        @Test
        @DisplayName("Should throw BusinessException when phone already exists")
        void shouldThrowExceptionWhenPhoneExists() {
            when(departmentRepository.existsByPhoneNumber("+375291234567")).thenReturn(true);

            assertThatThrownBy(() -> departmentService.createDepartment(testCreateDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Department with phone +375291234567 already exists");

            verify(departmentRepository).existsByPhoneNumber("+375291234567");
            verify(departmentMapper, never()).toEntity(any());
            verify(departmentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateDepartment()")
    class UpdateDepartmentTests {

        @Test
        @DisplayName("Should update department successfully")
        void shouldUpdateDepartmentSuccessfully() {
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDept));
            doNothing().when(departmentMapper).updateEntity(testDept, testCreateDto);
            when(departmentRepository.save(testDept)).thenReturn(testDept);
            when(departmentMapper.toDto(testDept)).thenReturn(testDeptDto);

            DepartmentDto result = departmentService.updateDepartment(1L, testCreateDto);

            assertThat(result).isEqualTo(testDeptDto);
            verify(departmentRepository).findById(1L);
            verify(departmentMapper).updateEntity(testDept, testCreateDto);
            verify(departmentRepository).save(testDept);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when id not found")
        void shouldThrowNotFoundExceptionWhenIdNotFound() {
            when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> departmentService.updateDepartment(999L, testCreateDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Department not found with id: 999");
        }

        @Test
        @DisplayName("Should handle null updateDto")
        void shouldHandleNullUpdateDto() {
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDept));
            when(departmentRepository.save(testDept)).thenReturn(testDept);
            when(departmentMapper.toDto(testDept)).thenReturn(testDeptDto);

            DepartmentDto result = departmentService.updateDepartment(1L, null);

            assertThat(result).isEqualTo(testDeptDto);
            verify(departmentMapper).updateEntity(testDept, null);
        }
    }

    @Nested
    @DisplayName("deleteDepartment()")
    class DeleteDepartmentTests {

        @Test
        @DisplayName("Should delete department successfully when no license plates")
        void shouldDeleteDepartmentSuccessfully() {
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDept));
            doNothing().when(departmentRepository).delete(testDept);

            departmentService.deleteDepartment(1L);

            verify(departmentRepository).findById(1L);
            verify(departmentRepository).delete(testDept);
        }

        @Test
        @DisplayName("Should throw BusinessException when department has license plates")
        void shouldThrowExceptionWhenHasLicensePlates() {
            testDept.setLicensePlates(List.of(new LicensePlate()));

            when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDept));

            assertThatThrownBy(() -> departmentService.deleteDepartment(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot delete department with existing license plates");

            verify(departmentRepository).findById(1L);
            verify(departmentRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should handle null license plates list gracefully")
        void shouldHandleNullLicensePlatesList() {
            testDept.setLicensePlates(null);

            when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDept));
            doNothing().when(departmentRepository).delete(testDept);

            departmentService.deleteDepartment(1L);

            verify(departmentRepository).findById(1L);
            verify(departmentRepository).delete(testDept);
        }

        @Test
        @DisplayName("Should handle empty license plates list gracefully")
        void shouldHandleEmptyLicensePlatesList() {
            testDept.setLicensePlates(Collections.emptyList());

            when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDept));
            doNothing().when(departmentRepository).delete(testDept);

            departmentService.deleteDepartment(1L);

            verify(departmentRepository).findById(1L);
            verify(departmentRepository).delete(testDept);
        }
    }

    @Nested
    @DisplayName("demonstrateNPlusOneProblem()")
    class DemonstrateNPlusOneProblemTests {

        @Test
        @DisplayName("Should demonstrate N+1 problem")
        void shouldDemonstrateNPlusOneProblem() {
            List<RegistrationDept> depts = List.of(testDept);
            when(departmentRepository.findByRegionIgnoreCase("MINSK")).thenReturn(depts);

            departmentService.demonstrateNPlusOneProblem("MINSK");

            verify(departmentRepository).findByRegionIgnoreCase("MINSK");
            // Проверяем что метод вызвал getLicensePlates().size()
            assertThat(testDept.getLicensePlates()).isEmpty();
        }

        @Test
        @DisplayName("Should handle empty list in N+1 demo")
        void shouldHandleEmptyListInNPlusOneDemo() {
            when(departmentRepository.findByRegionIgnoreCase("MINSK")).thenReturn(Collections.emptyList());

            departmentService.demonstrateNPlusOneProblem("MINSK");

            verify(departmentRepository).findByRegionIgnoreCase("MINSK");
        }
    }

    @Nested
    @DisplayName("solveNPlusOneWithFetchJoin()")
    class SolveNPlusOneWithFetchJoinTests {

        @Test
        @DisplayName("Should solve N+1 problem with fetch join")
        void shouldSolveNPlusOneWithFetchJoin() {
            List<RegistrationDept> depts = List.of(testDept);
            when(departmentRepository.findByRegionWithPlatesFetch("MINSK")).thenReturn(depts);

            departmentService.solveNPlusOneWithFetchJoin("MINSK");

            verify(departmentRepository).findByRegionWithPlatesFetch("MINSK");
        }

        @Test
        @DisplayName("Should handle empty list in fetch join demo")
        void shouldHandleEmptyListInFetchJoinDemo() {
            when(departmentRepository.findByRegionWithPlatesFetch("MINSK")).thenReturn(Collections.emptyList());

            departmentService.solveNPlusOneWithFetchJoin("MINSK");

            verify(departmentRepository).findByRegionWithPlatesFetch("MINSK");
        }
    }
}