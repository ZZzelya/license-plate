package com.example.licenseplate.repository;

import com.example.licenseplate.model.entity.AdditionalService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<AdditionalService, Long> {
    List<AdditionalService> findByIsAvailableTrue();

    boolean existsByName(String name);
}