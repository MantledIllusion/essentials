package com.mantledillusion.essentials.spring.integration.locks;

import org.aspectj.lang.ProceedingJoinPoint;

public class MethodLockedException extends RuntimeException {

    private final ProceedingJoinPoint joinPoint;

    public MethodLockedException(String lockKey, ProceedingJoinPoint joinPoint) {
        super(String.format("Key '%s' was locked", lockKey));
        this.joinPoint = joinPoint;
    }

    public <T> T invokeAnyway() throws Throwable {
        return (T) this.joinPoint.proceed();
    }
}
