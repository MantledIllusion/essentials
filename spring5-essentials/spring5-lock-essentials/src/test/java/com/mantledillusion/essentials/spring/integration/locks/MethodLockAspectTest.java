package com.mantledillusion.essentials.spring.integration.locks;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class MethodLockAspectTest {

    @Autowired
    private TestBean lockedBeanA;
    @Autowired
    private TestBean lockedBeanB;
    @Autowired
    private TestLockFactory testLockFactory;

    private ExecutorService executor;

    @BeforeEach
    public void before() {
        testLockFactory.getLocks().clear();
        executor = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    public void after() {
        executor.shutdown();
    }

    @Test
    public void testMethodLockAcquisition() throws InterruptedException {
        // RUN JOB A
        CompletableFuture<Void> jobA = new CompletableFuture<>();
        executor.execute(() -> this.lockedBeanA.lockedMethod(jobA));
        Thread.sleep(100);

        // VALIDATE LOCKED
        assertEquals(1, testLockFactory.getLocks().size());
        Map.Entry<Object, ReentrantLock> lock = testLockFactory.getLocks().entrySet().iterator().next();
        assertEquals("TestBean.lockedMethod()", lock.getKey());
        assertTrue(lock.getValue().isLocked());

        // COMPLETE JOB A
        jobA.complete(null);
        Thread.sleep(100);

        // VALIDATE UNLOCKED
        assertFalse(lock.getValue().isLocked());
    }

    @Test
    public void testMethodLocking() throws InterruptedException {
        // RUN JOB A
        CompletableFuture<Void> jobA = new CompletableFuture<>();
        executor.execute(() -> this.lockedBeanA.lockedMethod(jobA));
        Thread.sleep(100);

        // RUN JOB B
        CompletableFuture<Void> jobB = new CompletableFuture<>();
        assertThrows(MethodLockedException.class, () -> this.lockedBeanB.lockedMethod(jobB));

        // COMPLETE JOB A
        jobA.complete(null);
        Thread.sleep(100);

        // RUN JOB B
        jobB.complete(null);
        assertDoesNotThrow(() -> this.lockedBeanB.lockedMethod(jobB));
    }

    @Test
    public void testParameterLockAcquisition() throws InterruptedException {
        // RUN JOB A (foobar)
        CompletableFuture<Void> jobA = new CompletableFuture<>();
        executor.execute(() -> this.lockedBeanA.parameterLockedMethod(jobA, "foobar"));
        Thread.sleep(100);

        // VALIDATE LOCKED (foobar)
        assertEquals(1, testLockFactory.getLocks().size());
        Map.Entry<Object, ReentrantLock> lock = testLockFactory.getLocks().entrySet().iterator().next();
        assertEquals("TestBean.parameterLockedMethod(lockedParameter:foobar)", lock.getKey());
        assertTrue(lock.getValue().isLocked());

        // COMPLETE JOB A
        jobA.complete(null);
        Thread.sleep(100);

        // VALIDATE UNLOCKED
        assertFalse(lock.getValue().isLocked());
    }

    @Test
    public void testParameterLocking() throws InterruptedException {
        // RUN JOB A (foobar)
        CompletableFuture<Void> jobA = new CompletableFuture<>();
        executor.execute(() -> this.lockedBeanA.parameterLockedMethod(jobA, "foobar"));
        Thread.sleep(100);

        // RUN JOB B (foobar)
        CompletableFuture<Void> jobB = new CompletableFuture<>();
        assertThrows(MethodLockedException.class, () -> this.lockedBeanB.parameterLockedMethod(jobB, "foobar"));

        // RUN JOB B (barfoo)
        jobB.complete(null);
        assertDoesNotThrow(() -> this.lockedBeanB.parameterLockedMethod(jobB, "barfoo"));

        // COMPLETE JOB A
        jobA.complete(null);
        Thread.sleep(100);

        // RUN JOB B (foobar)
        jobB.complete(null);
        assertDoesNotThrow(() -> this.lockedBeanB.parameterLockedMethod(jobB, "foobar"));
    }
}
