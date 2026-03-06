package com.example.licenseplate.repository;

import com.example.licenseplate.model.entity.RegistrationDept;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<RegistrationDept, Long> {
    List<RegistrationDept> findByRegionIgnoreCase(String region);

    List<RegistrationDept> findByNameContainingIgnoreCase(String name);

    boolean existsByPhoneNumber(String phoneNumber);

    @EntityGraph(attributePaths = {"licensePlates"})
    @Query("SELECT d FROM RegistrationDept d WHERE d.id = :id")
    Optional<RegistrationDept> findByIdWithPlates(@Param("id") Long id);

    @EntityGraph(attributePaths = {"applications"})
    @Query("SELECT d FROM RegistrationDept d WHERE d.id = :id")
    Optional<RegistrationDept> findByIdWithApplications(@Param("id") Long id);

    @Query("SELECT DISTINCT d FROM RegistrationDept d LEFT JOIN FETCH d.licensePlates " +
        "WHERE d.region = :region")
    List<RegistrationDept> findByRegionWithPlatesFetch(@Param("region") String region);

}