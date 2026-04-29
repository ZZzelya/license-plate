package com.example.licenseplate.repository;

import com.example.licenseplate.model.entity.PassportChangeRequest;
import com.example.licenseplate.model.enums.PassportChangeRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PassportChangeRequestRepository extends JpaRepository<PassportChangeRequest, Long> {
    List<PassportChangeRequest> findAllByOrderByCreatedAtDesc();

    List<PassportChangeRequest> findByStatusOrderByCreatedAtDesc(PassportChangeRequestStatus status);

    List<PassportChangeRequest> findByApplicantIdOrderByCreatedAtDesc(Long applicantId);

    boolean existsByApplicantIdAndStatus(Long applicantId, PassportChangeRequestStatus status);
}
