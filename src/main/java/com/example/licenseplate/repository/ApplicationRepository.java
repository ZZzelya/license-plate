package com.example.licenseplate.repository;

import com.example.licenseplate.model.entity.Application;
import com.example.licenseplate.model.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {


    List<Application> findByStatus(ApplicationStatus status);

    @Query("SELECT a FROM Application a WHERE a.status = :status " +
        "AND a.reservedUntil < :now")
    List<Application> findExpiredByStatus(@Param("status") ApplicationStatus status,
                                          @Param("now") LocalDateTime now);

    @EntityGraph(attributePaths = {"applicant", "licensePlate", "department", "additionalServices"})
    @Query("SELECT a FROM Application a WHERE a.id = :id")
    Optional<Application> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT a FROM Application a WHERE a.applicant.passportNumber = :passport")
    List<Application> findByApplicantPassport(@Param("passport") String passport);

    @Query("SELECT a FROM Application a " +
        "WHERE a.status = :status " +
        "AND a.department.region = :region")
    List<Application> findByStatusAndDepartmentRegion(
        @Param("status") ApplicationStatus status,
        @Param("region") String region);

    @Query(value = "SELECT a.* FROM applications a " +
        "JOIN registration_depts d ON a.dept_id = d.id " +
        "WHERE a.status = :status " +
        "AND d.region = :region",
        nativeQuery = true)
    List<Application> findByStatusAndDepartmentRegionNative(
        @Param("status") String status,
        @Param("region") String region);

    @Query("SELECT a FROM Application a WHERE a.applicant.passportNumber = :passport")
    Page<Application> findByApplicantPassport(
        @Param("passport") String passport,
        Pageable pageable);
}