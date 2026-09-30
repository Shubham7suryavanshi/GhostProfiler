package com.ghostprofiler.agent.instrumentation;

import com.ghostprofiler.agent.collector.MetricsStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodInterceptorTest {

    @BeforeEach
    void setUp() {
        MetricsStore.getInstance().reset();
    }

    @AfterEach
    void tearDown() {
        MetricsStore.getInstance().reset();
    }

    @Test
    void testOnEnterReturnsValidTimestamp() {
        long start = System.nanoTime();
        long enterTime = MethodInterceptor.onEnter("com.test.DummyMethod");
        long end = System.nanoTime();
        
        assertTrue(enterTime >= start && enterTime <= end, "Enter time should be within the bounds of execution");
    }

    @Test
    void testOnExitExecutesWithoutError() {
        // Since MetricsStore methods are mostly stubbed or lock-free updates,
        // we mainly test that it doesn't throw any exceptions that would leak to the host app.
        long start = System.nanoTime();
        MethodInterceptor.onExit("com.test.DummyMethod", start, null);
        // Execution shouldn't throw
        assertTrue(true);
    }
}
