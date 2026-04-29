package com.example.licenseplate.service;

import com.example.licenseplate.dto.request.AuthLoginRequest;
import com.example.licenseplate.dto.request.AuthRegisterRequest;
import com.example.licenseplate.dto.request.UserProfileUpdateRequest;
import com.example.licenseplate.dto.response.AuthResponse;
import com.example.licenseplate.dto.response.AuthUserDto;
import com.example.licenseplate.exception.BusinessException;
import com.example.licenseplate.exception.UnauthorizedException;
import com.example.licenseplate.model.entity.Applicant;
import com.example.licenseplate.model.entity.UserAccount;
import com.example.licenseplate.model.enums.UserRole;
import com.example.licenseplate.repository.ApplicantRepository;
import com.example.licenseplate.repository.UserAccountRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final ApplicantRepository applicantRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Caching(evict = {
        @CacheEvict(cacheNames = "applicants", allEntries = true),
        @CacheEvict(cacheNames = "applicantById", allEntries = true),
        @CacheEvict(cacheNames = "applicantByPassport", allEntries = true)
    })
    @Transactional
    public AuthResponse register(AuthRegisterRequest request) {
        String normalizedPassport = request.getPassportNumber().trim().toUpperCase();
        String normalizedEmail = normalizeEmail(request.getEmail());
        String username = deriveUsername(normalizedEmail, normalizedPassport);

        if (applicantRepository.existsByPassportNumber(normalizedPassport)) {
            throw new BusinessException("Пользователь с таким паспортом уже существует");
        }

        if (normalizedEmail != null && applicantRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new BusinessException("Пользователь с таким email уже существует");
        }

        if (userAccountRepository.existsByUsernameIgnoreCase(username)) {
            throw new BusinessException("Пользователь с таким логином уже существует");
        }

        Applicant applicant = Applicant.builder()
            .fullName(request.getFullName().trim())
            .passportNumber(normalizedPassport)
            .phoneNumber(request.getPhoneNumber())
            .email(normalizedEmail)
            .address(request.getAddress())
            .build();

        Applicant savedApplicant = applicantRepository.save(applicant);

        UserAccount account = UserAccount.builder()
            .username(username)
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .role(UserRole.USER)
            .applicant(savedApplicant)
            .build();

        UserAccount savedAccount = userAccountRepository.save(account);
        return buildAuthResponse(savedAccount);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(AuthLoginRequest request) {
        UserAccount account = findAccountByIdentifier(request.getIdentifier());

        if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new UnauthorizedException("Неверный логин или пароль");
        }

        return buildAuthResponse(account);
    }

    @Transactional(readOnly = true)
    public AuthUserDto getCurrentUser(String authHeader) {
        return toUserDto(getCurrentAccount(authHeader));
    }

    @Transactional
    public AuthUserDto updateCurrentUserProfile(String authHeader, UserProfileUpdateRequest request) {
        UserAccount account = getCurrentAccount(authHeader);

        if (account.getRole() != UserRole.USER || account.getApplicant() == null) {
            throw new BusinessException("Личный кабинет доступен только обычному пользователю");
        }

        Applicant applicant = account.getApplicant();
        String normalizedEmail = normalizeEmail(request.getEmail());

        if (normalizedEmail != null && !normalizedEmail.equalsIgnoreCase(applicant.getEmail())) {
            applicantRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(existing -> !existing.getId().equals(applicant.getId()))
                .ifPresent(existing -> {
                    throw new BusinessException("Пользователь с таким email уже существует");
                });
        }

        applicant.setFullName(request.getFullName().trim());
        applicant.setPhoneNumber(request.getPhoneNumber());
        applicant.setEmail(normalizedEmail);
        applicant.setAddress(request.getAddress());
        applicantRepository.save(applicant);

        if (normalizedEmail != null && !normalizedEmail.isBlank()) {
            account.setUsername(normalizedEmail);
            userAccountRepository.save(account);
        }

        return toUserDto(account);
    }

    @Transactional(readOnly = true)
    public UserAccount getCurrentAccount(String authHeader) {
        String token = jwtService.extractToken(authHeader);
        Claims claims = jwtService.parseToken(token);
        Long userId = Long.valueOf(claims.getSubject());

        return userAccountRepository.findById(userId)
            .orElseThrow(() -> new UnauthorizedException("Пользователь по токену не найден"));
    }

    private UserAccount findAccountByIdentifier(String identifier) {
        String normalizedIdentifier = identifier.trim();

        return userAccountRepository.findByUsernameIgnoreCase(normalizedIdentifier)
            .or(() -> applicantRepository.findByEmailIgnoreCase(normalizedIdentifier)
                .flatMap(applicant -> userAccountRepository.findByApplicantId(applicant.getId())))
            .or(() -> applicantRepository.findByPassportNumber(normalizedIdentifier.toUpperCase())
                .flatMap(applicant -> userAccountRepository.findByApplicantId(applicant.getId())))
            .orElseThrow(() -> new UnauthorizedException("Неверный логин или пароль"));
    }

    private AuthResponse buildAuthResponse(UserAccount userAccount) {
        return AuthResponse.builder()
            .token(jwtService.generateToken(userAccount))
            .user(toUserDto(userAccount))
            .build();
    }

    private AuthUserDto toUserDto(UserAccount userAccount) {
        Applicant applicant = userAccount.getApplicant();

        return AuthUserDto.builder()
            .id(userAccount.getId())
            .username(userAccount.getUsername())
            .role(userAccount.getRole().name())
            .applicantId(applicant != null ? applicant.getId() : null)
            .fullName(applicant != null ? applicant.getFullName() : "Администратор")
            .email(applicant != null ? applicant.getEmail() : null)
            .passportNumber(applicant != null ? applicant.getPassportNumber() : null)
            .phoneNumber(applicant != null ? applicant.getPhoneNumber() : null)
            .address(applicant != null ? applicant.getAddress() : null)
            .build();
    }

    private String deriveUsername(String email, String passportNumber) {
        return email != null && !email.isBlank() ? email : passportNumber;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        return email.trim().toLowerCase();
    }
}
