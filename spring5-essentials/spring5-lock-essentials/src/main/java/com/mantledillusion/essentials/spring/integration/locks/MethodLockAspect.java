package com.mantledillusion.essentials.spring.integration.locks;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.integration.support.locks.LockRegistry;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.stream.IntStream;

/**
 * AspectJ @{@link Aspect} that prevents simultaneous calls to @{@link MethodLock} annotated bean methods though a
 * lock provided by a {@link LockRegistry}.
 * <p>
 * Locks are identified by a combination of the used {@link MethodLock#value()} and {@link ParameterLock#value()}
 * value. The first method call on a lock will lock it until the called method's execution completes. Any subsequent
 * calls on the same lock will cause a {@link MethodLockedException} for as long as it remains locked.
 * <p>
 * By default, both {@link MethodLock} and {@link ParameterLock} will attempt to use reflection when determining their
 * names, identifying locks that are unique for their bean method and parameter name. As a result, overloaded methods
 * might identify the same lock if the annotation's defaults are used.
 * <p>
 * In the same manner, different methods of different beans might also share a lock if the same annotation values
 * are used. This is a deliberate behaviour, as it allows intertwining the methods of different beans with each other.
 * <p>
 * Requires Spring AOP's @{@link EnableAspectJAutoProxy} to be active.
 */
@Aspect
public class MethodLockAspect {

    private final LockRegistry lockRegistry;

    public MethodLockAspect(LockRegistry lockRegistry) {
        this.lockRegistry = lockRegistry;
    }

    @Around("@annotation(com.mantledillusion.essentials.spring.integration.locks.MethodLock)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        String methodKey = getName(method);
        Optional<String> parameterKey = IntStream.range(0, joinPoint.getArgs().length)
                .filter(paramIndex -> method.getParameters()[paramIndex].isAnnotationPresent(ParameterLock.class))
                .mapToObj(paramIndex -> getName(method.getParameters()[paramIndex]) + ':' + joinPoint.getArgs()[paramIndex])
                .reduce((p1, p2) -> p1+','+p2)
                .map(parameters -> '('+parameters+')');
        String lockKey = methodKey + parameterKey.orElse("()");

        Lock lock = lockRegistry.obtain(lockKey);

        if (lock.tryLock()) {
            try {
                return joinPoint.proceed();
            } finally {
                lock.unlock();
            }
        } else {
            throw new MethodLockedException(lockKey, joinPoint);
        }
    }

    private String getName(Method method) {
        MethodLock methodLock = method.getAnnotation(MethodLock.class);
        return methodLock.value().isEmpty()
                ? method.getDeclaringClass().getSimpleName() + '.' + method.getName()
                : methodLock.value();
    }

    private String getName(Parameter parameter) {
        ParameterLock parameterLock = parameter.getAnnotation(ParameterLock.class);
        return parameterLock.value().isEmpty()
                ? parameter.getName()
                : parameterLock.value();
    }
}
