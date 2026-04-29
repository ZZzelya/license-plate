package com.example.licenseplate.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Pointcut("execution(* com.example.licenseplate.service..*(..))")
    public void serviceMethods() {
    }

    @Value("${app.logging.slow-method-threshold-ms:500}")
    private long slowMethodThresholdMs;

    @Around("serviceMethods()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String method = className + "." + methodName;

        log.debug("Starting service method: {} with {} argument(s)", method, joinPoint.getArgs().length);

        try {
            Object result = joinPoint.proceed();
            stopWatch.stop();

            long executionTime = stopWatch.getTotalTimeMillis();
            if (executionTime >= slowMethodThresholdMs) {
                log.warn("Slow service method detected: {} executed in {} ms", method, executionTime);
            } else {
                log.info("Service method completed: {} executed in {} ms", method, executionTime);
            }

            return result;
        } catch (Throwable throwable) {
            if (stopWatch.isRunning()) {
                stopWatch.stop();
            }
            log.error(
                "Service method failed: {} after {} ms with message: {}",
                method,
                stopWatch.getTotalTimeMillis(),
                throwable.getMessage(),
                throwable
            );
            throw throwable;
        }
    }
}
