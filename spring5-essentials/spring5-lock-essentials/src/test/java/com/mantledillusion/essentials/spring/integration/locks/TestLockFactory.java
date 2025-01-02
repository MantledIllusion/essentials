package com.mantledillusion.essentials.spring.integration.locks;

import org.springframework.integration.support.locks.LockRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TestLockFactory implements LockRegistry {

    private final Map<Object, ReentrantLock> locks = new HashMap<>();

    @Override
    public Lock obtain(Object lockKey) {
        return this.locks.computeIfAbsent(lockKey, this::create);
    }

    public Map<Object, ReentrantLock> getLocks() {
        return this.locks;
    }

    private ReentrantLock create(Object lockKey) {
        return new ReentrantLock();
    }
}
