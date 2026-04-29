package com.example.licenseplate.repository;

import com.example.licenseplate.model.entity.RegistrationDept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<RegistrationDept, Long> {
    List<RegistrationDept> findByRegionIgnoreCase(String region);

    boolean existsByPhoneNumber(String phoneNumber);
}
