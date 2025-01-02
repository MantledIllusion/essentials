package com.mantledillusion.essentials.spring.integration.locks;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class TestBean {

    @MethodLock
    public void lockedMethod(CompletableFuture<Void> job) {
        execute(job);
    }

    @MethodLock
    public void parameterLockedMethod(CompletableFuture<Void> job, @ParameterLock String lockedParameter) {
        execute(job);
    }

    private void execute(CompletableFuture<Void> job) {
        try {
            job.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
