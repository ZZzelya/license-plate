package com.example.licenseplate.repository;

import com.example.licenseplate.model.entity.LicensePlate;
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

    @Query("SELECT l FROM LicensePlate l WHERE l.department.id = :deptId")
    List<LicensePlate> findByDepartment(@Param("deptId") Long deptId);

    @Query("SELECT l FROM LicensePlate l WHERE l.department.region = :region " +
        "AND NOT EXISTS (SELECT a FROM Application a WHERE a.licensePlate = l " +
        "AND a.status IN ('PENDING', 'CONFIRMED', 'COMPLETED'))")
    List<LicensePlate> findAvailableByRegion(@Param("region") String region);

    boolean existsByPlateNumber(String plateNumber);

    @Query("SELECT COUNT(l) FROM LicensePlate l WHERE l.department.id = :deptId")
    long countByDepartmentId(@Param("deptId") Long deptId);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN false ELSE true END " +
        "FROM Application a WHERE a.licensePlate.id = :plateId " +
        "AND a.status IN ('PENDING', 'CONFIRMED', 'COMPLETED')")
    boolean isPlateAvailable(@Param("plateId") Long plateId);
}