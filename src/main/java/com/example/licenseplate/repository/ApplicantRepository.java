package com.example.licenseplate.repository;

import com.example.licenseplate.model.entity.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ApplicantRepository extends JpaRepository<Applicant, Long> {
    Optional<Applicant> findByPassportNumber(String passportNumber);

    boolean existsByPassportNumber(String passportNumber);

}