package com.ghostprofiler.agent.collector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MetricsStore}.
 * Verifies the singleton contract, reset, and stub API surface.
 * Full enter/exit stack tests will be filled in during step 4.
 */
class MetricsStoreTest {

    @BeforeEach
    void resetStore() {
        MetricsStore.getInstance().reset();
    }

    @Test
    void testCallStackManagement() {
        MetricsStore store = MetricsStore.getInstance();
        
        // Simulating:
        // root() {
        //   child()
        // }
        
        store.onMethodEnter("root", 1000);
        store.onMethodEnter("child", 2000);
        
        store.onMethodExit("child", 3000, false);
        store.onMethodExit("root", 4000, false);
        
        assertEquals(1, store.getCompletedTrees().size());
        
        CallTreeNode root = store.getCompletedTrees().get(0);
        assertEquals("root", root.getMethodName());
        assertEquals(3000, root.getDurationNanos()); // 4000 - 1000
        
        assertEquals(1, root.getChildren().size());
        CallTreeNode child = root.getChildren().get(0);
        assertEquals("child", child.getMethodName());
        assertEquals(1000, child.getDurationNanos()); // 3000 - 2000
    }

    @Test
    void testConcurrency() throws InterruptedException {
        MetricsStore store = MetricsStore.getInstance();
        int threadCount = 10;
        int iterations = 100;
        
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < iterations; j++) {
                        store.onMethodEnter("concurrentMethod", j * 100);
                        store.onMethodExit("concurrentMethod", (j * 100) + 10, false);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(5, java.util.concurrent.TimeUnit.SECONDS));
        executor.shutdown();
        
        // Assert total calls
        MetricsStore.MethodStats stats = store.getMethodStats().get("concurrentMethod");
        assertEquals(threadCount * iterations, stats.getCallCount());
        assertEquals(threadCount * iterations * 10, stats.getTotalDurationNanos());
        
        // Max size bounded to 100 trees
        assertTrue(store.getCompletedTrees().size() <= 100);
    }
}
