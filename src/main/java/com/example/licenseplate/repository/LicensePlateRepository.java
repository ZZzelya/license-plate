package com.example.licenseplate.repository;

import com.example.licenseplate.model.entity.LicensePlate;
import com.example.licenseplate.model.enums.PlateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LicensePlateRepository extends JpaRepository<LicensePlate, Long> {
    Optional<LicensePlate> findByPlateNumber(String plateNumber);

    List<LicensePlate> findByDepartmentId(Long departmentId);

    List<LicensePlate> findByStatus(PlateStatus status);

    @Query("SELECT l FROM LicensePlate l WHERE l.department.id = :deptId " +
        "AND l.status = :status")
    List<LicensePlate> findByDepartmentAndStatus(@Param("deptId") Long deptId,
                                                 @Param("status") PlateStatus status);

    @Query("SELECT l FROM LicensePlate l WHERE l.status = 'AVAILABLE' " +
        "AND l.department.region = :region")
    List<LicensePlate> findAvailableByRegion(@Param("region") String region);

    boolean existsByPlateNumber(String plateNumber);

    @Query("SELECT COUNT(l) FROM LicensePlate l WHERE l.department.id = :deptId")
    long countByDepartmentId(@Param("deptId") Long deptId);
}