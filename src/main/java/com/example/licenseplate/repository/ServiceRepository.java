package com.example.licenseplate.repository;

import com.example.licenseplate.model.entity.AdditionalService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<AdditionalService, Long> {
    List<AdditionalService> findByIsAvailableTrue();

    List<AdditionalService> findByNameContainingIgnoreCase(String name);

    @Query("SELECT s FROM AdditionalService s WHERE s.id IN :ids AND s.isAvailable = true")
    List<AdditionalService> findAvailableByIds(@Param("ids") List<Long> ids);

    boolean existsByName(String name);
}