package com.example.licenseplate.repository;

import com.example.licenseplate.model.entity.Applicant;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicantRepository extends JpaRepository<Applicant, Long> {
    Optional<Applicant> findByPassportNumber(String passportNumber);

    Optional<Applicant> findByEmail(String email);

    Optional<Applicant> findByPhoneNumber(String phoneNumber);

    List<Applicant> findByFullNameContainingIgnoreCase(String name);

    boolean existsByPassportNumber(String passportNumber);

    @EntityGraph(attributePaths = {"applications"})
    @Query("SELECT a FROM Applicant a WHERE a.id = :id")
    Optional<Applicant> findByIdWithApplications(@Param("id") Long id);

    @Query("SELECT a FROM Applicant a WHERE a.isActive = true")
    List<Applicant> findAllActive();
}