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
        // Start each test with a clean slate
        MetricsStore.getInstance().reset();
    }

    @Test
    void getInstance_returnsSameInstance() {
        MetricsStore a = MetricsStore.getInstance();
        MetricsStore b = MetricsStore.getInstance();
        assertSame(a, b, "MetricsStore must be a singleton");
    }

    @Test
    void reset_clearsMethodStats() {
        MetricsStore store = MetricsStore.getInstance();
        // In step 4 this will actually populate data; for now just verify no crash
        store.reset();
        assertTrue(store.getMethodStats().isEmpty());
    }

    @Test
    void reset_clearsCompletedTrees() {
        MetricsStore store = MetricsStore.getInstance();
        store.reset();
        assertTrue(store.getCompletedTrees().isEmpty());
    }

    @Test
    void onMethodEnter_doesNotThrow() {
        assertDoesNotThrow(() ->
            MetricsStore.getInstance().onMethodEnter("com.example.Service#foo", System.nanoTime())
        );
    }

    @Test
    void onMethodExit_doesNotThrow() {
        assertDoesNotThrow(() ->
            MetricsStore.getInstance().onMethodExit("com.example.Service#foo", System.nanoTime(), false)
        );
    }

    @Test
    void recordQuery_doesNotThrow() {
        assertDoesNotThrow(() ->
            MetricsStore.getInstance().recordQuery("SELECT * FROM orders", 1_000_000L)
        );
    }
}
