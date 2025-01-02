package com.mantledillusion.essentials.spring.integration.locks;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.integration.support.locks.LockRegistry;

@Configuration
@EnableAspectJAutoProxy
public class TestConfiguration {

    @Bean
    public LockRegistry lockRegistry() {
        return new TestLockFactory();
    }

    @Bean
    public MethodLockAspect methodLockAspect(LockRegistry lockRegistry) {
        return new MethodLockAspect(lockRegistry);
    }

    @Bean
    public TestBean lockedBeanA() {
        return new TestBean();
    }

    @Bean
    public TestBean lockedBeanB() {
        return new TestBean();
    }
}
