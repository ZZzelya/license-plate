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

    @Query("SELECT l FROM LicensePlate l WHERE UPPER(l.department.region) = UPPER(:region) " +
        "AND NOT EXISTS (SELECT a FROM Application a WHERE a.licensePlate = l " +
        "AND a.status IN ('PENDING', 'CONFIRMED', 'COMPLETED'))")
    List<LicensePlate> findAvailableByRegion(@Param("region") String region);

    @Query("SELECT l FROM LicensePlate l WHERE UPPER(l.department.region) = UPPER((" +
        "SELECT d.region FROM RegistrationDept d WHERE d.id = :departmentId)) " +
        "AND NOT EXISTS (SELECT a FROM Application a WHERE a.licensePlate = l " +
        "AND a.status IN ('PENDING', 'CONFIRMED', 'COMPLETED'))")
    List<LicensePlate> findAvailableByDepartmentId(@Param("departmentId") Long departmentId);

    @Query("SELECT l FROM LicensePlate l WHERE UPPER(l.department.region) = UPPER(:region) " +
        "AND UPPER(l.plateNumber) = UPPER(:plateNumber) " +
        "AND NOT EXISTS (SELECT a FROM Application a WHERE a.licensePlate = l " +
        "AND a.status IN ('PENDING', 'CONFIRMED', 'COMPLETED'))")
    Optional<LicensePlate> findAvailableByRegionAndPlateNumber(
        @Param("region") String region,
        @Param("plateNumber") String plateNumber);

    @Query("SELECT l FROM LicensePlate l WHERE UPPER(l.department.region) = UPPER((" +
        "SELECT d.region FROM RegistrationDept d WHERE d.id = :departmentId)) " +
        "AND UPPER(l.plateNumber) = UPPER(:plateNumber) " +
        "AND NOT EXISTS (SELECT a FROM Application a WHERE a.licensePlate = l " +
        "AND a.status IN ('PENDING', 'CONFIRMED', 'COMPLETED'))")
    Optional<LicensePlate> findAvailableByDepartmentRegionAndPlateNumber(
        @Param("departmentId") Long departmentId,
        @Param("plateNumber") String plateNumber);

    boolean existsByPlateNumber(String plateNumber);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN false ELSE true END " +
        "FROM Application a WHERE a.licensePlate.id = :plateId " +
        "AND a.status IN ('PENDING', 'CONFIRMED', 'COMPLETED')")
    boolean isPlateAvailable(@Param("plateId") Long plateId);
}
