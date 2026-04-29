package com.example.licenseplate.repository;

import com.example.licenseplate.model.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsernameIgnoreCase(String username);

    Optional<UserAccount> findByApplicantId(Long applicantId);

    boolean existsByUsernameIgnoreCase(String username);
}
