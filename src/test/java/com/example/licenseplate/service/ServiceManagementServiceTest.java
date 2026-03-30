package com.example.licenseplate.service;

import com.example.licenseplate.dto.request.ServiceCreateDto;
import com.example.licenseplate.dto.response.ServiceDto;
import com.example.licenseplate.exception.BusinessException;
import com.example.licenseplate.exception.ResourceNotFoundException;
import com.example.licenseplate.model.entity.AdditionalService;
import com.example.licenseplate.model.entity.Application;
import com.example.licenseplate.repository.ServiceRepository;
import com.example.licenseplate.service.mapper.ServiceMapper;
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
@DisplayName("ServiceManagementService Coverage Tests")
class ServiceManagementServiceTest {

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private ServiceMapper serviceMapper;

    @InjectMocks
    private ServiceManagementService serviceManagementService;

    private AdditionalService testService;
    private ServiceDto testServiceDto;
    private ServiceCreateDto testCreateDto;

    @BeforeEach
    void setUp() {
        testService = AdditionalService.builder()
            .id(1L)
            .name("Срочное оформление")
            .description("Оформление за 1 час")
            .price(BigDecimal.valueOf(50))
            .isAvailable(true)
            .applications(new ArrayList<>())
            .build();

        testServiceDto = ServiceDto.builder()
            .id(1L)
            .name("Срочное оформление")
            .description("Оформление за 1 час")
            .price(BigDecimal.valueOf(50))
            .isAvailable(true)
            .build();

        testCreateDto = ServiceCreateDto.builder()
            .name("Срочное оформление")
            .description("Оформление за 1 час")
            .price(BigDecimal.valueOf(50))
            .isAvailable(true)
            .build();
    }

    @Nested
    @DisplayName("getAllServices()")
    class GetAllServicesTests {

        @Test
        @DisplayName("Should return list of services")
        void shouldReturnListOfServices() {
            List<AdditionalService> services = List.of(testService);
            List<ServiceDto> expectedDtos = List.of(testServiceDto);

            when(serviceRepository.findAll()).thenReturn(services);
            when(serviceMapper.toDtoList(services)).thenReturn(expectedDtos);

            List<ServiceDto> result = serviceManagementService.getAllServices();

            assertThat(result).isEqualTo(expectedDtos);
            verify(serviceRepository).findAll();
            verify(serviceMapper).toDtoList(services);
        }

        @Test
        @DisplayName("Should return empty list when no services")
        void shouldReturnEmptyListWhenNoServices() {
            when(serviceRepository.findAll()).thenReturn(Collections.emptyList());
            when(serviceMapper.toDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

            List<ServiceDto> result = serviceManagementService.getAllServices();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getServiceById()")
    class GetServiceByIdTests {

        @Test
        @DisplayName("Should return service when id exists")
        void shouldReturnServiceWhenIdExists() {
            when(serviceRepository.findById(1L)).thenReturn(Optional.of(testService));
            when(serviceMapper.toDto(testService)).thenReturn(testServiceDto);

            ServiceDto result = serviceManagementService.getServiceById(1L);

            assertThat(result).isEqualTo(testServiceDto);
            verify(serviceRepository).findById(1L);
            verify(serviceMapper).toDto(testService);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when id not found")
        void shouldThrowNotFoundExceptionWhenIdNotFound() {
            when(serviceRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> serviceManagementService.getServiceById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Service not found with id: 999");

            verify(serviceRepository).findById(999L);
            verify(serviceMapper, never()).toDto(any());
        }
    }

    @Nested
    @DisplayName("getAvailableServices()")
    class GetAvailableServicesTests {

        @Test
        @DisplayName("Should return available services")
        void shouldReturnAvailableServices() {
            List<AdditionalService> services = List.of(testService);
            List<ServiceDto> expectedDtos = List.of(testServiceDto);

            when(serviceRepository.findByIsAvailableTrue()).thenReturn(services);
            when(serviceMapper.toDtoList(services)).thenReturn(expectedDtos);

            List<ServiceDto> result = serviceManagementService.getAvailableServices();

            assertThat(result).isEqualTo(expectedDtos);
            verify(serviceRepository).findByIsAvailableTrue();
        }

        @Test
        @DisplayName("Should return empty list when no available services")
        void shouldReturnEmptyListWhenNoAvailableServices() {
            when(serviceRepository.findByIsAvailableTrue()).thenReturn(Collections.emptyList());
            when(serviceMapper.toDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

            List<ServiceDto> result = serviceManagementService.getAvailableServices();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("createService()")
    class CreateServiceTests {

        @Test
        @DisplayName("Should create service successfully with isAvailable=true")
        void shouldCreateServiceSuccessfully() {
            when(serviceRepository.existsByName("Срочное оформление")).thenReturn(false);
            when(serviceMapper.toEntity(testCreateDto)).thenReturn(testService);
            when(serviceRepository.save(testService)).thenReturn(testService);
            when(serviceMapper.toDto(testService)).thenReturn(testServiceDto);

            ServiceDto result = serviceManagementService.createService(testCreateDto);

            assertThat(result).isEqualTo(testServiceDto);
            verify(serviceRepository).existsByName("Срочное оформление");
            verify(serviceMapper).toEntity(testCreateDto);
            verify(serviceRepository).save(testService);
        }

        @Test
        @DisplayName("Should create service with default isAvailable when null")
        void shouldCreateServiceWithDefaultIsAvailable() {
            ServiceCreateDto dtoWithNullAvailable = ServiceCreateDto.builder()
                .name("Новая услуга")
                .price(BigDecimal.valueOf(100))
                .isAvailable(null)
                .build();

            AdditionalService serviceWithDefault = AdditionalService.builder()
                .id(2L)
                .name("Новая услуга")
                .price(BigDecimal.valueOf(100))
                .isAvailable(true)
                .applications(new ArrayList<>())
                .build();

            ServiceDto expectedDto = ServiceDto.builder()
                .id(2L)
                .name("Новая услуга")
                .price(BigDecimal.valueOf(100))
                .isAvailable(true)
                .build();

            when(serviceRepository.existsByName("Новая услуга")).thenReturn(false);
            when(serviceMapper.toEntity(dtoWithNullAvailable)).thenReturn(serviceWithDefault);
            when(serviceRepository.save(serviceWithDefault)).thenReturn(serviceWithDefault);
            when(serviceMapper.toDto(serviceWithDefault)).thenReturn(expectedDto);

            ServiceDto result = serviceManagementService.createService(dtoWithNullAvailable);

            assertThat(result).isEqualTo(expectedDto);
            assertThat(result.getIsAvailable()).isTrue();
        }

        @Test
        @DisplayName("Should throw BusinessException when name already exists")
        void shouldThrowExceptionWhenNameExists() {
            when(serviceRepository.existsByName("Срочное оформление")).thenReturn(true);

            assertThatThrownBy(() -> serviceManagementService.createService(testCreateDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Service with name Срочное оформление already exists");

            verify(serviceRepository).existsByName("Срочное оформление");
            verify(serviceMapper, never()).toEntity(any());
            verify(serviceRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateService()")
    class UpdateServiceTests {

        @Test
        @DisplayName("Should update service successfully when name not changed")
        void shouldUpdateServiceSuccessfullyWhenNameNotChanged() {
            when(serviceRepository.findById(1L)).thenReturn(Optional.of(testService));
            doNothing().when(serviceMapper).updateEntity(testService, testCreateDto);
            when(serviceRepository.save(testService)).thenReturn(testService);
            when(serviceMapper.toDto(testService)).thenReturn(testServiceDto);

            ServiceDto result = serviceManagementService.updateService(1L, testCreateDto);

            assertThat(result).isEqualTo(testServiceDto);
            verify(serviceRepository).findById(1L);
            // Убираем проверку existsByName, так как он НЕ вызывается когда имя не меняется
            verify(serviceRepository, never()).existsByName(anyString());
            verify(serviceMapper).updateEntity(testService, testCreateDto);
            verify(serviceRepository).save(testService);
        }

        @Test
        @DisplayName("Should update service successfully when name changed and unique")
        void shouldUpdateServiceSuccessfullyWhenNameChanged() {
            ServiceCreateDto updateDto = ServiceCreateDto.builder()
                .name("Новое название")
                .price(BigDecimal.valueOf(75))
                .isAvailable(false)
                .build();

            when(serviceRepository.findById(1L)).thenReturn(Optional.of(testService));
            when(serviceRepository.existsByName("Новое название")).thenReturn(false);
            doNothing().when(serviceMapper).updateEntity(testService, updateDto);
            when(serviceRepository.save(testService)).thenReturn(testService);
            when(serviceMapper.toDto(testService)).thenReturn(testServiceDto);

            ServiceDto result = serviceManagementService.updateService(1L, updateDto);

            assertThat(result).isEqualTo(testServiceDto);
            verify(serviceRepository).existsByName("Новое название");
        }

        @Test
        @DisplayName("Should throw BusinessException when new name already exists")
        void shouldThrowExceptionWhenNewNameExists() {
            ServiceCreateDto updateDto = ServiceCreateDto.builder()
                .name("Существующее название")
                .price(BigDecimal.valueOf(75))
                .build();

            when(serviceRepository.findById(1L)).thenReturn(Optional.of(testService));
            when(serviceRepository.existsByName("Существующее название")).thenReturn(true);

            assertThatThrownBy(() -> serviceManagementService.updateService(1L, updateDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Service with name Существующее название already exists");

            verify(serviceMapper, never()).updateEntity(any(), any());
            verify(serviceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should not check name uniqueness when name unchanged")
        void shouldNotCheckNameUniquenessWhenNameUnchanged() {
            when(serviceRepository.findById(1L)).thenReturn(Optional.of(testService));
            doNothing().when(serviceMapper).updateEntity(testService, testCreateDto);
            when(serviceRepository.save(testService)).thenReturn(testService);
            when(serviceMapper.toDto(testService)).thenReturn(testServiceDto);

            ServiceDto result = serviceManagementService.updateService(1L, testCreateDto);

            assertThat(result).isEqualTo(testServiceDto);
            verify(serviceRepository, never()).existsByName(any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when id not found")
        void shouldThrowNotFoundExceptionWhenIdNotFound() {
            when(serviceRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> serviceManagementService.updateService(999L, testCreateDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Service not found with id: 999");
        }
    }

    @Nested
    @DisplayName("deleteService()")
    class DeleteServiceTests {

        @Test
        @DisplayName("Should delete service successfully when no applications")
        void shouldDeleteServiceSuccessfully() {
            when(serviceRepository.findById(1L)).thenReturn(Optional.of(testService));
            doNothing().when(serviceRepository).delete(testService);

            serviceManagementService.deleteService(1L);

            verify(serviceRepository).findById(1L);
            verify(serviceRepository).delete(testService);
        }

        @Test
        @DisplayName("Should throw BusinessException when service has applications")
        void shouldThrowExceptionWhenServiceHasApplications() {
            testService.setApplications(List.of(new Application()));

            when(serviceRepository.findById(1L)).thenReturn(Optional.of(testService));

            assertThatThrownBy(() -> serviceManagementService.deleteService(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot delete service that is used in applications");

            verify(serviceRepository).findById(1L);
            verify(serviceRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should handle null applications list gracefully")
        void shouldHandleNullApplicationsList() {
            testService.setApplications(null);

            when(serviceRepository.findById(1L)).thenReturn(Optional.of(testService));
            doNothing().when(serviceRepository).delete(testService);

            serviceManagementService.deleteService(1L);

            verify(serviceRepository).findById(1L);
            verify(serviceRepository).delete(testService);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when id not found")
        void shouldThrowNotFoundExceptionWhenIdNotFound() {
            when(serviceRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> serviceManagementService.deleteService(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Service not found with id: 999");
        }
    }
}