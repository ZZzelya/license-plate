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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private ApplicantRepository applicantRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private Claims claims;

    @InjectMocks
    private AuthService authService;

    private Applicant applicant;
    private UserAccount account;

    @BeforeEach
    void setUp() {
        applicant = Applicant.builder()
            .id(1L)
            .fullName("Ivan Ivanov")
            .passportNumber("MP1234567")
            .email("ivan@example.com")
            .phoneNumber("+375291234567")
            .address("Minsk")
            .build();

        account = UserAccount.builder()
            .id(10L)
            .username("ivan@example.com")
            .passwordHash("hash")
            .role(UserRole.USER)
            .applicant(applicant)
            .build();
    }

    @Test
    void registerThrowsWhenPassportExists() {
        AuthRegisterRequest request = AuthRegisterRequest.builder()
            .fullName("Ivan Ivanov")
            .passportNumber("mp1234567")
            .email("ivan@example.com")
            .password("secret1")
            .build();
        when(applicantRepository.existsByPassportNumber("MP1234567")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request)).isInstanceOf(BusinessException.class);
    }

    @Test
    void registerThrowsWhenEmailExists() {
        AuthRegisterRequest request = AuthRegisterRequest.builder()
            .fullName("Ivan Ivanov")
            .passportNumber("mp1234567")
            .email("ivan@example.com")
            .password("secret1")
            .build();
        when(applicantRepository.existsByPassportNumber("MP1234567")).thenReturn(false);
        when(applicantRepository.existsByEmailIgnoreCase("ivan@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request)).isInstanceOf(BusinessException.class);
    }

    @Test
    void registerThrowsWhenUsernameExists() {
        AuthRegisterRequest request = AuthRegisterRequest.builder()
            .fullName("Ivan Ivanov")
            .passportNumber("mp1234567")
            .email("ivan@example.com")
            .password("secret1")
            .build();
        when(applicantRepository.existsByPassportNumber("MP1234567")).thenReturn(false);
        when(applicantRepository.existsByEmailIgnoreCase("ivan@example.com")).thenReturn(false);
        when(userAccountRepository.existsByUsernameIgnoreCase("ivan@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request)).isInstanceOf(BusinessException.class);
    }

    @Test
    void registerUsesPassportAsUsernameWhenEmailMissing() {
        AuthRegisterRequest request = AuthRegisterRequest.builder()
            .fullName("Ivan Ivanov")
            .passportNumber("mp1234567")
            .password("secret1")
            .build();

        Applicant savedApplicant = Applicant.builder().id(1L).passportNumber("MP1234567").fullName("Ivan Ivanov").build();
        UserAccount savedAccount = UserAccount.builder()
            .id(10L)
            .username("MP1234567")
            .role(UserRole.USER)
            .applicant(savedApplicant)
            .build();

        when(applicantRepository.existsByPassportNumber("MP1234567")).thenReturn(false);
        when(userAccountRepository.existsByUsernameIgnoreCase("MP1234567")).thenReturn(false);
        when(passwordEncoder.encode("secret1")).thenReturn("encoded");
        when(applicantRepository.save(any(Applicant.class))).thenReturn(savedApplicant);
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(savedAccount);
        when(jwtService.generateToken(savedAccount)).thenReturn("token");

        AuthResponse response = authService.register(request);

        assertThat(response.getToken()).isEqualTo("token");
        assertThat(response.getUser().getUsername()).isEqualTo("MP1234567");
    }

    @Test
    void registerUsesPassportAsUsernameWhenEmailBlank() {
        AuthRegisterRequest request = AuthRegisterRequest.builder()
            .fullName("Ivan Ivanov")
            .passportNumber("mp1234567")
            .email("   ")
            .password("secret1")
            .build();

        when(applicantRepository.existsByPassportNumber("MP1234567")).thenReturn(false);
        when(userAccountRepository.existsByUsernameIgnoreCase("MP1234567")).thenReturn(false);
        when(passwordEncoder.encode("secret1")).thenReturn("encoded");
        when(applicantRepository.save(any(Applicant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(UserAccount.class))).thenReturn("token");

        AuthResponse response = authService.register(request);

        assertThat(response.getUser().getUsername()).isEqualTo("MP1234567");
        assertThat(response.getUser().getEmail()).isNull();
    }

    @Test
    void registerUsesEmailAsUsernameWhenProvided() {
        AuthRegisterRequest request = AuthRegisterRequest.builder()
            .fullName("Ivan Ivanov")
            .passportNumber("mp1234567")
            .email("IVAN@EXAMPLE.COM ")
            .password("secret1")
            .build();

        when(applicantRepository.existsByPassportNumber("MP1234567")).thenReturn(false);
        when(applicantRepository.existsByEmailIgnoreCase("ivan@example.com")).thenReturn(false);
        when(userAccountRepository.existsByUsernameIgnoreCase("ivan@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret1")).thenReturn("encoded");
        when(applicantRepository.save(any(Applicant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(UserAccount.class))).thenReturn("token");

        AuthResponse response = authService.register(request);

        assertThat(response.getUser().getUsername()).isEqualTo("ivan@example.com");
        assertThat(response.getUser().getPassportNumber()).isEqualTo("MP1234567");
    }

    @Test
    void loginFindsByUsernameAndReturnsAuthResponse() {
        AuthLoginRequest request = AuthLoginRequest.builder().identifier("ivan@example.com").password("secret").build();
        when(userAccountRepository.findByUsernameIgnoreCase("ivan@example.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(jwtService.generateToken(account)).thenReturn("token");

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("token");
        assertThat(response.getUser().getUsername()).isEqualTo("ivan@example.com");
    }

    @Test
    void loginFindsByEmailWhenUsernameMissing() {
        AuthLoginRequest request = AuthLoginRequest.builder().identifier("ivan@example.com").password("secret").build();
        when(userAccountRepository.findByUsernameIgnoreCase("ivan@example.com")).thenReturn(Optional.empty());
        when(applicantRepository.findByEmailIgnoreCase("ivan@example.com")).thenReturn(Optional.of(applicant));
        when(userAccountRepository.findByApplicantId(1L)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(jwtService.generateToken(account)).thenReturn("token");

        assertThat(authService.login(request).getToken()).isEqualTo("token");
    }

    @Test
    void loginFindsByPassportWhenOtherIdentifiersMissing() {
        AuthLoginRequest request = AuthLoginRequest.builder().identifier("MP1234567").password("secret").build();
        when(userAccountRepository.findByUsernameIgnoreCase("MP1234567")).thenReturn(Optional.empty());
        when(applicantRepository.findByEmailIgnoreCase("MP1234567")).thenReturn(Optional.empty());
        when(applicantRepository.findByPassportNumber("MP1234567")).thenReturn(Optional.of(applicant));
        when(userAccountRepository.findByApplicantId(1L)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(jwtService.generateToken(account)).thenReturn("token");

        assertThat(authService.login(request).getToken()).isEqualTo("token");
    }

    @Test
    void loginThrowsWhenPasswordInvalid() {
        AuthLoginRequest request = AuthLoginRequest.builder().identifier("ivan@example.com").password("secret").build();
        when(userAccountRepository.findByUsernameIgnoreCase("ivan@example.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void loginThrowsWhenIdentifierCannotBeResolved() {
        AuthLoginRequest request = AuthLoginRequest.builder().identifier("missing").password("secret").build();
        when(userAccountRepository.findByUsernameIgnoreCase("missing")).thenReturn(Optional.empty());
        when(applicantRepository.findByEmailIgnoreCase("missing")).thenReturn(Optional.empty());
        when(applicantRepository.findByPassportNumber("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getCurrentAccountThrowsWhenUserMissing() {
        when(jwtService.extractToken("Bearer token")).thenReturn("token");
        when(jwtService.parseToken("token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("10");
        when(userAccountRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentAccount("Bearer token"))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getCurrentUserReturnsMappedDto() {
        when(jwtService.extractToken("Bearer token")).thenReturn("token");
        when(jwtService.parseToken("token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("10");
        when(userAccountRepository.findById(10L)).thenReturn(Optional.of(account));

        AuthUserDto result = authService.getCurrentUser("Bearer token");

        assertThat(result.getUsername()).isEqualTo("ivan@example.com");
        assertThat(result.getFullName()).isEqualTo("Ivan Ivanov");
    }

    @Test
    void getCurrentUserReturnsAdminDisplayNameForAdminWithoutApplicant() {
        UserAccount admin = UserAccount.builder().id(1L).username("admin").role(UserRole.ADMIN).build();
        when(jwtService.extractToken("Bearer token")).thenReturn("token");
        when(jwtService.parseToken("token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("1");
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(admin));

        AuthUserDto result = authService.getCurrentUser("Bearer token");

        assertThat(result.getFullName()).isEqualTo("\u0410\u0434\u043c\u0438\u043d\u0438\u0441\u0442\u0440\u0430\u0442\u043e\u0440");
        assertThat(result.getApplicantId()).isNull();
    }

    @Test
    void getCurrentAccountReturnsResolvedAccount() {
        when(jwtService.extractToken("Bearer token")).thenReturn("token");
        when(jwtService.parseToken("token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("10");
        when(userAccountRepository.findById(10L)).thenReturn(Optional.of(account));

        assertThat(authService.getCurrentAccount("Bearer token")).isEqualTo(account);
    }

    @Test
    void updateCurrentUserProfileThrowsWhenAccountIsNotRegularUser() {
        UserAccount admin = UserAccount.builder().id(1L).role(UserRole.ADMIN).build();
        when(jwtService.extractToken("Bearer token")).thenReturn("token");
        when(jwtService.parseToken("token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("1");
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> authService.updateCurrentUserProfile("Bearer token", UserProfileUpdateRequest.builder().fullName("Admin").build()))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void updateCurrentUserProfileThrowsWhenApplicantMissingForUserRole() {
        UserAccount userWithoutApplicant = UserAccount.builder().id(1L).role(UserRole.USER).build();
        when(jwtService.extractToken("Bearer token")).thenReturn("token");
        when(jwtService.parseToken("token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("1");
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(userWithoutApplicant));

        assertThatThrownBy(() -> authService.updateCurrentUserProfile(
            "Bearer token",
            UserProfileUpdateRequest.builder().fullName("User").build()))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void updateCurrentUserProfileThrowsWhenNewEmailBelongsToAnotherApplicant() {
        Applicant other = Applicant.builder().id(99L).email("other@example.com").build();
        UserProfileUpdateRequest request = UserProfileUpdateRequest.builder()
            .fullName("Ivan New")
            .email("other@example.com")
            .phoneNumber("+375291234567")
            .address("New")
            .build();

        when(jwtService.extractToken("Bearer token")).thenReturn("token");
        when(jwtService.parseToken("token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("10");
        when(userAccountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(applicantRepository.findByEmailIgnoreCase("other@example.com")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> authService.updateCurrentUserProfile("Bearer token", request))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void updateCurrentUserProfileUpdatesApplicantOnlyWhenEmailBlank() {
        UserProfileUpdateRequest request = UserProfileUpdateRequest.builder()
            .fullName("Ivan New")
            .phoneNumber("+375291234567")
            .address("New address")
            .build();

        when(jwtService.extractToken("Bearer token")).thenReturn("token");
        when(jwtService.parseToken("token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("10");
        when(userAccountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(applicantRepository.save(applicant)).thenReturn(applicant);

        AuthUserDto result = authService.updateCurrentUserProfile("Bearer token", request);

        assertThat(result.getFullName()).isEqualTo("Ivan New");
        verify(userAccountRepository, never()).save(account);
    }

    @Test
    void updateCurrentUserProfileSkipsDuplicateEmailCheckWhenEmailUnchanged() {
        UserProfileUpdateRequest request = UserProfileUpdateRequest.builder()
            .fullName("Ivan New")
            .email("IVAN@example.com")
            .phoneNumber("+375291234567")
            .address("New address")
            .build();

        when(jwtService.extractToken("Bearer token")).thenReturn("token");
        when(jwtService.parseToken("token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("10");
        when(userAccountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(applicantRepository.save(applicant)).thenReturn(applicant);
        when(userAccountRepository.save(account)).thenReturn(account);

        AuthUserDto result = authService.updateCurrentUserProfile("Bearer token", request);

        assertThat(result.getEmail()).isEqualTo("ivan@example.com");
    }

    @Test
    void updateCurrentUserProfileUpdatesUsernameWhenEmailProvided() {
        UserProfileUpdateRequest request = UserProfileUpdateRequest.builder()
            .fullName("Ivan New")
            .email("new@example.com")
            .phoneNumber("+375291234567")
            .address("New address")
            .build();

        when(jwtService.extractToken("Bearer token")).thenReturn("token");
        when(jwtService.parseToken("token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("10");
        when(userAccountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(applicantRepository.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
        when(applicantRepository.save(applicant)).thenReturn(applicant);
        when(userAccountRepository.save(account)).thenReturn(account);

        AuthUserDto result = authService.updateCurrentUserProfile("Bearer token", request);

        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(account.getUsername()).isEqualTo("new@example.com");
    }

    @Test
    void updateCurrentUserProfileAllowsExistingEmailOnSameApplicant() {
        Applicant sameApplicantByEmail = Applicant.builder().id(1L).email("same@example.com").build();
        UserProfileUpdateRequest request = UserProfileUpdateRequest.builder()
            .fullName("Ivan New")
            .email("same@example.com")
            .phoneNumber("+375291234567")
            .address("New address")
            .build();

        applicant.setEmail("old@example.com");
        when(jwtService.extractToken("Bearer token")).thenReturn("token");
        when(jwtService.parseToken("token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("10");
        when(userAccountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(applicantRepository.findByEmailIgnoreCase("same@example.com")).thenReturn(Optional.of(sameApplicantByEmail));
        when(applicantRepository.save(applicant)).thenReturn(applicant);
        when(userAccountRepository.save(account)).thenReturn(account);

        AuthUserDto result = authService.updateCurrentUserProfile("Bearer token", request);

        assertThat(result.getEmail()).isEqualTo("same@example.com");
    }
}
