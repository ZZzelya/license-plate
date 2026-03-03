package com.example.licenseplate.service;

import com.example.licenseplate.dto.request.ServiceCreateDto;
import com.example.licenseplate.dto.response.ServiceDto;
import com.example.licenseplate.exception.ResourceNotFoundException;
import com.example.licenseplate.exception.BusinessException;
import com.example.licenseplate.model.entity.AdditionalService;
import com.example.licenseplate.repository.ServiceRepository;
import com.example.licenseplate.service.mapper.ServiceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceManagementService {

    private final ServiceRepository serviceRepository;
    private final ServiceMapper serviceMapper;

    private static final String SERVICE_NOT_FOUND = "Service not found with id: ";

    @Transactional(readOnly = true)
    public List<ServiceDto> getAllServices() {
        return serviceMapper.toDtoList(serviceRepository.findAll());
    }

    @Transactional(readOnly = true)
    public ServiceDto getServiceById(final Long id) {
        AdditionalService service = findServiceOrThrow(id);
        return serviceMapper.toDto(service);
    }

    @Transactional(readOnly = true)
    public List<ServiceDto> getAvailableServices() {
        return serviceMapper.toDtoList(serviceRepository.findByIsAvailableTrue());
    }

    @Transactional
    public ServiceDto createService(final ServiceCreateDto createDto) {
        if (serviceRepository.existsByName(createDto.getName())) {
            throw new BusinessException(
                "Service with name " + createDto.getName() + " already exists");
        }

        AdditionalService service = serviceMapper.toEntity(createDto);
        AdditionalService savedService = serviceRepository.save(service);
        log.info("Created service: {} with price: {}",
            savedService.getName(), savedService.getPrice());

        return serviceMapper.toDto(savedService);
    }

    @Transactional
    public ServiceDto updateService(final Long id, final ServiceCreateDto updateDto) {
        AdditionalService service = findServiceOrThrow(id);

        if (!service.getName().equals(updateDto.getName()) &&
            serviceRepository.existsByName(updateDto.getName())) {
            throw new BusinessException(
                "Service with name " + updateDto.getName() + " already exists");
        }

        serviceMapper.updateEntity(service, updateDto);
        AdditionalService updatedService = serviceRepository.save(service);
        log.info("Updated service with id: {}", id);

        return serviceMapper.toDto(updatedService);
    }

    @Transactional
    public void deleteService(final Long id) {
        AdditionalService service = findServiceOrThrow(id);

        if (service.getApplications() != null && !service.getApplications().isEmpty()) {
            throw new BusinessException(
                "Cannot delete service that is used in applications");
        }

        serviceRepository.delete(service);
        log.info("Deleted service with id: {}", id);
    }

    private AdditionalService findServiceOrThrow(final Long id) {
        return serviceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(SERVICE_NOT_FOUND + id));
    }
}