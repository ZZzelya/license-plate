package com.example.licenseplate.service;

import com.example.licenseplate.dto.request.ServiceCreateDto;
import com.example.licenseplate.dto.response.ServiceDto;
import com.example.licenseplate.exception.BusinessException;
import com.example.licenseplate.exception.ResourceNotFoundException;
import com.example.licenseplate.mapper.ServiceMapper;
import com.example.licenseplate.model.entity.AdditionalService;
import com.example.licenseplate.repository.ServiceRepository;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceManagementServiceTest {

    @Mock
    private ServiceRepository serviceRepository;
    @Mock
    private ServiceMapper serviceMapper;

    @InjectMocks
    private ServiceManagementService serviceManagementService;

    private AdditionalService service;
    private ServiceDto serviceDto;
    private ServiceCreateDto createDto;

    @BeforeEach
    void setUp() {
        service = AdditionalService.builder()
            .id(1L)
            .name("Fast")
            .price(BigDecimal.TEN)
            .isAvailable(true)
            .applications(new ArrayList<>())
            .build();
        serviceDto = ServiceDto.builder().id(1L).name("Fast").price(BigDecimal.TEN).isAvailable(true).build();
        createDto = ServiceCreateDto.builder().name("Fast").price(BigDecimal.TEN).isAvailable(true).build();
    }

    @Test
    void getAllServicesReturnsMappedList() {
        when(serviceRepository.findAll()).thenReturn(List.of(service));
        when(serviceMapper.toDtoList(List.of(service))).thenReturn(List.of(serviceDto));

        assertThat(serviceManagementService.getAllServices()).containsExactly(serviceDto);
    }

    @Test
    void getAllServicesReturnsEmptyList() {
        when(serviceRepository.findAll()).thenReturn(List.of());
        when(serviceMapper.toDtoList(List.of())).thenReturn(List.of());

        assertThat(serviceManagementService.getAllServices()).isEmpty();
    }

    @Test
    void getServiceByIdReturnsMappedService() {
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));
        when(serviceMapper.toDto(service)).thenReturn(serviceDto);

        assertThat(serviceManagementService.getServiceById(1L)).isEqualTo(serviceDto);
    }

    @Test
    void getServiceByIdThrowsWhenMissing() {
        when(serviceRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceManagementService.getServiceById(1L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAvailableServicesReturnsMappedList() {
        when(serviceRepository.findByIsAvailableTrue()).thenReturn(List.of(service));
        when(serviceMapper.toDtoList(List.of(service))).thenReturn(List.of(serviceDto));

        assertThat(serviceManagementService.getAvailableServices()).containsExactly(serviceDto);
    }

    @Test
    void getAvailableServicesReturnsEmptyList() {
        when(serviceRepository.findByIsAvailableTrue()).thenReturn(List.of());
        when(serviceMapper.toDtoList(List.of())).thenReturn(List.of());

        assertThat(serviceManagementService.getAvailableServices()).isEmpty();
    }

    @Test
    void createServiceThrowsWhenNameExists() {
        when(serviceRepository.existsByName("Fast")).thenReturn(true);

        assertThatThrownBy(() -> serviceManagementService.createService(createDto))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void createServiceSavesAndMapsEntity() {
        when(serviceRepository.existsByName("Fast")).thenReturn(false);
        when(serviceMapper.toEntity(createDto)).thenReturn(service);
        when(serviceRepository.save(service)).thenReturn(service);
        when(serviceMapper.toDto(service)).thenReturn(serviceDto);

        assertThat(serviceManagementService.createService(createDto)).isEqualTo(serviceDto);
    }

    @Test
    void updateServiceThrowsWhenMissing() {
        when(serviceRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceManagementService.updateService(1L, createDto))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateServiceThrowsWhenNewNameExists() {
        AdditionalService existing = AdditionalService.builder().id(1L).name("Old").build();
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(serviceRepository.existsByName("Fast")).thenReturn(true);

        assertThatThrownBy(() -> serviceManagementService.updateService(1L, createDto))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void updateServiceSkipsDuplicateCheckWhenNameSame() {
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));
        when(serviceRepository.save(service)).thenReturn(service);
        when(serviceMapper.toDto(service)).thenReturn(serviceDto);

        assertThat(serviceManagementService.updateService(1L, createDto)).isEqualTo(serviceDto);
        verify(serviceMapper).updateEntity(service, createDto);
    }

    @Test
    void updateServiceAllowsUniqueNewName() {
        ServiceCreateDto updateDto = ServiceCreateDto.builder().name("Premium").price(BigDecimal.ONE).isAvailable(false).build();
        AdditionalService existing = AdditionalService.builder().id(1L).name("Old").price(BigDecimal.TEN).build();

        when(serviceRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(serviceRepository.existsByName("Premium")).thenReturn(false);
        when(serviceRepository.save(existing)).thenReturn(existing);
        when(serviceMapper.toDto(existing)).thenReturn(serviceDto);

        assertThat(serviceManagementService.updateService(1L, updateDto)).isEqualTo(serviceDto);
        verify(serviceMapper).updateEntity(existing, updateDto);
    }

    @Test
    void deleteServiceThrowsWhenApplicationsExist() {
        service.setApplications(List.of(new com.example.licenseplate.model.entity.Application()));
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));

        assertThatThrownBy(() -> serviceManagementService.deleteService(1L))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void deleteServiceThrowsWhenMissing() {
        when(serviceRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceManagementService.deleteService(1L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteServiceDeletesWhenUnused() {
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));
        doNothing().when(serviceRepository).delete(service);

        serviceManagementService.deleteService(1L);

        verify(serviceRepository).delete(service);
    }

    @Test
    void deleteServiceDeletesWhenApplicationsNull() {
        service.setApplications(null);
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));
        doNothing().when(serviceRepository).delete(service);

        serviceManagementService.deleteService(1L);

        verify(serviceRepository).delete(service);
    }
}
