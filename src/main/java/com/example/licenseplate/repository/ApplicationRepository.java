package com.example.licenseplate.repository;

import com.example.licenseplate.model.entity.Application;
import com.example.licenseplate.model.enums.ApplicationStatus;
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

    List<Application> findByApplicantId(Long applicantId);

    List<Application> findByDepartmentId(Long departmentId);

    List<Application> findByStatus(ApplicationStatus status);

    @Query("SELECT a FROM Application a WHERE a.licensePlate.id = :plateId " +
        "AND a.status IN ('PENDING', 'CONFIRMED')")
    List<Application> findActiveApplicationsByPlateId(@Param("plateId") Long plateId);

    @Query("SELECT a FROM Application a WHERE a.status = :status " +
        "AND a.reservedUntil < :now")
    List<Application> findExpiredByStatus(@Param("status") ApplicationStatus status,
                                          @Param("now") LocalDateTime now);

    @EntityGraph(attributePaths = {"applicant", "licensePlate", "department", "additionalServices"})
    @Query("SELECT a FROM Application a WHERE a.id = :id")
    Optional<Application> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT a FROM Application a WHERE a.applicant.passportNumber = :passport")
    List<Application> findByApplicantPassport(@Param("passport") String passport);

    boolean existsByLicensePlateIdAndStatusIn(Long plateId, List<ApplicationStatus> statuses);

    long countByStatus(ApplicationStatus status);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
        "FROM Application a WHERE a.licensePlate.id = :plateId " +
        "AND a.status IN ('PENDING', 'CONFIRMED', 'COMPLETED')")
    boolean hasActiveApplicationsByPlateId(@Param("plateId") Long plateId);

    @Query("SELECT a FROM Application a WHERE a.licensePlate.id = :plateId " +
        "AND a.status IN ('PENDING', 'CONFIRMED', 'COMPLETED') " +
        "ORDER BY a.submissionDate DESC")  // ИЗМЕНЕНО
    List<Application> findActiveApplicationsByPlateIdOrdered(@Param("plateId") Long plateId);
}