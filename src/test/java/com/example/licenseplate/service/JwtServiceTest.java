package com.example.licenseplate.service;

import com.example.licenseplate.exception.UnauthorizedException;
import com.example.licenseplate.model.entity.Applicant;
import com.example.licenseplate.model.entity.UserAccount;
import com.example.licenseplate.model.enums.UserRole;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(
            jwtService,
            "secret",
            "1234567890123456789012345678901212345678901234567890123456789012"
        );
        ReflectionTestUtils.setField(jwtService, "expirationMs", 60_000L);
        jwtService.init();
    }

    @Test
    void generateAndParseTokenWithApplicant() {
        UserAccount account = UserAccount.builder()
            .id(1L)
            .username("user@example.com")
            .role(UserRole.USER)
            .applicant(Applicant.builder().id(3L).build())
            .build();

        String token = jwtService.generateToken(account);
        Claims claims = jwtService.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(claims.get("username", String.class)).isEqualTo("user@example.com");
    }

    @Test
    void generateAndParseTokenWithoutApplicant() {
        UserAccount account = UserAccount.builder()
            .id(2L)
            .username("admin")
            .role(UserRole.ADMIN)
            .build();

        String token = jwtService.generateToken(account);
        Claims claims = jwtService.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo("2");
        assertThat(claims.get("applicantId")).isNull();
    }

    @Test
    void parseTokenThrowsOnInvalidValue() {
        assertThatThrownBy(() -> jwtService.parseToken("broken-token"))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void extractTokenReturnsRawValue() {
        assertThat(jwtService.extractToken("Bearer abc")).isEqualTo("abc");
    }

    @Test
    void extractTokenThrowsForMissingHeader() {
        assertThatThrownBy(() -> jwtService.extractToken(null))
            .isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> jwtService.extractToken("Basic abc"))
            .isInstanceOf(UnauthorizedException.class);
    }
}
