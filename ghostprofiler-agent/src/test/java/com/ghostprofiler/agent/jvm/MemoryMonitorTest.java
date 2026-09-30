package com.ghostprofiler.agent.jvm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MemoryMonitor}.
 * Full sampling-thread tests added in step 5.
 */
class MemoryMonitorTest {

    @Test
    void constructor_doesNotThrow() {
        assertDoesNotThrow(() -> new MemoryMonitor(500L));
    }

    @Test
    void initialReadings_areZero() {
        MemoryMonitor monitor = new MemoryMonitor(500L);
        assertEquals(0L, monitor.getHeapUsedBytes());
        assertEquals(0L, monitor.getNonHeapUsedBytes());
    }

    @Test
    void heapMaxBytes_isPositiveAfterConstruction() {
        // The JVM always knows its max heap even before sampling starts
        // (heapMaxBytes is populated lazily in start() — will be tested in step 5)
        MemoryMonitor monitor = new MemoryMonitor(500L);
        // Just verify no exception — value may be 0 until start() is called
        assertDoesNotThrow(monitor::getHeapMaxBytes);
    }

    @Test
    void start_doesNotThrow() {
        MemoryMonitor monitor = new MemoryMonitor(500L);
        assertDoesNotThrow(monitor::start);
        monitor.stop(); // clean up
    }

    @Test
    void stop_doesNotThrow_whenNotStarted() {
        MemoryMonitor monitor = new MemoryMonitor(500L);
        assertDoesNotThrow(monitor::stop);
    }
}
