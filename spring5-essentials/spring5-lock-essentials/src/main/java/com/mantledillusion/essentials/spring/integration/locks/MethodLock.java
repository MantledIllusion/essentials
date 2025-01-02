package com.mantledillusion.essentials.spring.integration.locks;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AspectJ bean method annotation that prevents simultaneous calls on methods identifying the same lock provided by
 * a {@link org.springframework.integration.support.locks.LockRegistry}.
 * <p>
 * The annotation's {@link #value()} is used to identify the lock.
 * <p>
 * @see MethodLockAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MethodLock {

    /**
     * Returns the method identifier to build the lock's identifier with.
     * <p>
     * For example, a method annotated with <code>@MethodLock("myLock")</code> and an Integer parameter annotated
     * with <code>@ParameterLock("myParam")</code> will generate the lock identifier <code>myLock(myParam:123)</code>
     * when called with the value 123.
     * <p>
     * By default, reflection is used to combine class and method name to a unique identifier.
     *
     * @return The method's identifier
     */
    String value() default "";
}