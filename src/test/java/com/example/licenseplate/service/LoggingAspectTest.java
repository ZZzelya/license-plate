package com.example.licenseplate.service;

import com.example.licenseplate.aspect.LoggingAspect;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoggingAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;
    @Mock
    private Signature signature;

    private LoggingAspect loggingAspect;

    @BeforeEach
    void setUp() {
        loggingAspect = new LoggingAspect();
        ReflectionTestUtils.setField(loggingAspect, "slowMethodThresholdMs", 1L);
    }

    @Test
    void logExecutionTimeReturnsResultForSuccessfulCall() throws Throwable {
        when(joinPoint.getTarget()).thenReturn(new Object());
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("run");
        when(joinPoint.getArgs()).thenReturn(new Object[]{"a", 1});
        when(joinPoint.proceed()).thenReturn("ok");

        assertThat(loggingAspect.logExecutionTime(joinPoint)).isEqualTo("ok");
    }

    @Test
    void logExecutionTimeRethrowsThrowable() throws Throwable {
        when(joinPoint.getTarget()).thenReturn(new Object());
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("run");
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> loggingAspect.logExecutionTime(joinPoint))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("boom");
    }
}
