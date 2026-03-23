package com.example.licenseplate.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @SuppressWarnings("checkstyle:WhitespaceAround")
    @Pointcut("execution(* com.example.licenseplate.service..*(..))")
    public void serviceMethods() {

    }

    @Around("serviceMethods()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.debug("Executing {}.{} with args: {}", className, methodName, args);

        try {
            Object result = joinPoint.proceed();
            stopWatch.stop();
            log.info("{}.{} executed in {} ms", className, methodName, stopWatch.getTotalTimeMillis());
            return result;
        } catch (Exception e) {
            log.error("Error in {}.{}: {}", className, methodName, e.getMessage(), e);
            throw e;
        }
    }
}