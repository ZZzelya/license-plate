package com.example.licenseplate.service;

import com.example.licenseplate.dto.request.DepartmentCreateDto;
import com.example.licenseplate.dto.response.DepartmentDto;
import com.example.licenseplate.exception.BusinessException;
import com.example.licenseplate.exception.ResourceNotFoundException;
import com.example.licenseplate.mapper.DepartmentMapper;
import com.example.licenseplate.model.entity.Application;
import com.example.licenseplate.model.entity.RegistrationDept;
import com.example.licenseplate.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private DepartmentMapper departmentMapper;

    @InjectMocks
    private DepartmentService departmentService;

    private RegistrationDept department;
    private DepartmentDto departmentDto;
    private DepartmentCreateDto createDto;

    @BeforeEach
    void setUp() {
        department = RegistrationDept.builder()
            .id(1L)
            .name("Dept")
            .region("Minsk")
            .phoneNumber("+375291234567")
            .applications(new ArrayList<>())
            .build();

        departmentDto = DepartmentDto.builder()
            .id(1L)
            .name("Dept")
            .region("Minsk")
            .applicationsCount(0)
            .build();
        createDto = DepartmentCreateDto.builder()
            .name("Dept")
            .region("Minsk")
            .phoneNumber("+375291234567")
            .address("Address")
            .build();
    }

    @Test
    void getAllDepartmentsReturnsMappedList() {
        when(departmentRepository.findAll()).thenReturn(List.of(department));
        when(departmentMapper.toDtoList(List.of(department))).thenReturn(List.of(departmentDto));

        assertThat(departmentService.getAllDepartments()).containsExactly(departmentDto);
    }

    @Test
    void getDepartmentByIdReturnsMappedDepartment() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentMapper.toDto(department)).thenReturn(departmentDto);

        assertThat(departmentService.getDepartmentById(1L)).isEqualTo(departmentDto);
    }

    @Test
    void getDepartmentByIdThrowsWhenMissing() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.getDepartmentById(1L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getDepartmentsByRegionReturnsMappedList() {
        when(departmentRepository.findByRegionIgnoreCase("Minsk")).thenReturn(List.of(department));
        when(departmentMapper.toDtoList(List.of(department))).thenReturn(List.of(departmentDto));

        assertThat(departmentService.getDepartmentsByRegion("Minsk")).containsExactly(departmentDto);
    }

    @Test
    void getDepartmentsByRegionReturnsEmptyList() {
        when(departmentRepository.findByRegionIgnoreCase("Unknown")).thenReturn(List.of());
        when(departmentMapper.toDtoList(List.of())).thenReturn(List.of());

        assertThat(departmentService.getDepartmentsByRegion("Unknown")).isEmpty();
    }

    @Test
    void createDepartmentThrowsWhenPhoneExists() {
        when(departmentRepository.existsByPhoneNumber("+375291234567")).thenReturn(true);

        assertThatThrownBy(() -> departmentService.createDepartment(createDto))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void createDepartmentSavesAndMapsEntity() {
        when(departmentRepository.existsByPhoneNumber("+375291234567")).thenReturn(false);
        when(departmentMapper.toEntity(createDto)).thenReturn(department);
        when(departmentRepository.save(department)).thenReturn(department);
        when(departmentMapper.toDto(department)).thenReturn(departmentDto);

        assertThat(departmentService.createDepartment(createDto)).isEqualTo(departmentDto);
    }

    @Test
    void updateDepartmentThrowsWhenMissing() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.updateDepartment(1L, createDto))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateDepartmentUpdatesEntity() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentRepository.save(department)).thenReturn(department);
        when(departmentMapper.toDto(department)).thenReturn(departmentDto);

        assertThat(departmentService.updateDepartment(1L, createDto)).isEqualTo(departmentDto);
        verify(departmentMapper).updateEntity(department, createDto);
    }

    @Test
    void deleteDepartmentThrowsWhenApplicationsExist() {
        department.setApplications(List.of(new Application()));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));

        assertThatThrownBy(() -> departmentService.deleteDepartment(1L))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void deleteDepartmentDeletesWhenNoApplications() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        doNothing().when(departmentRepository).delete(department);

        departmentService.deleteDepartment(1L);

        verify(departmentRepository).delete(department);
    }

    @Test
    void deleteDepartmentDeletesWhenApplicationsListIsNull() {
        department.setApplications(null);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        doNothing().when(departmentRepository).delete(department);

        departmentService.deleteDepartment(1L);

        verify(departmentRepository).delete(department);
    }

    @Test
    void deleteDepartmentThrowsWhenMissing() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.deleteDepartment(1L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void demonstrateNPlusOneProblemTraversesDepartments() {
        department.setApplications(List.of(new Application()));
        when(departmentRepository.findByRegionIgnoreCase("Minsk")).thenReturn(List.of(department));

        assertThatCode(() -> departmentService.demonstrateNPlusOneProblem("Minsk")).doesNotThrowAnyException();
    }

    @Test
    void solveNPlusOneWithFetchJoinTraversesDepartments() {
        department.setApplications(List.of(new Application()));
        when(departmentRepository.findByRegionIgnoreCase("Minsk")).thenReturn(List.of(department));

        assertThatCode(() -> departmentService.solveNPlusOneWithFetchJoin("Minsk")).doesNotThrowAnyException();
    }
}
