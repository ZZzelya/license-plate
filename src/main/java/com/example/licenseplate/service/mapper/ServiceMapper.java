package com.example.licenseplate.service.mapper;

import com.example.licenseplate.dto.request.ServiceCreateDto;
import com.example.licenseplate.dto.response.ServiceDto;
import com.example.licenseplate.model.entity.AdditionalService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ServiceMapper {

    public ServiceDto toDto(AdditionalService service) {
        if (service == null) {
            return null;
        }

        ServiceDto dto = new ServiceDto();
        dto.setId(service.getId());
        dto.setName(service.getName());
        dto.setDescription(service.getDescription());
        dto.setPrice(service.getPrice());
        dto.setIsAvailable(service.getIsAvailable());

        return dto;
    }

    public AdditionalService toEntity(ServiceCreateDto dto) {
        if (dto == null) {
            return null;
        }

        AdditionalService service = new AdditionalService();
        service.setName(dto.getName());
        service.setDescription(dto.getDescription());
        service.setPrice(dto.getPrice());

        if (dto.getIsAvailable() != null) {
            service.setIsAvailable(dto.getIsAvailable());
        } else {
            service.setIsAvailable(true);
        }

        return service;
    }

    public void updateEntity(AdditionalService service, ServiceCreateDto dto) {
        if (dto == null || service == null) {
            return;
        }

        service.setName(dto.getName());
        service.setDescription(dto.getDescription());
        service.setPrice(dto.getPrice());
        service.setIsAvailable(dto.getIsAvailable());
    }

    public List<ServiceDto> toDtoList(List<AdditionalService> services) {
        return services.stream()
            .map(this::toDto)
            .toList();
    }

    public List<String> toNameList(List<AdditionalService> services) {
        if (services == null) {
            return List.of();
        }
        return services.stream()
            .map(AdditionalService::getName)
            .toList();
    }
}