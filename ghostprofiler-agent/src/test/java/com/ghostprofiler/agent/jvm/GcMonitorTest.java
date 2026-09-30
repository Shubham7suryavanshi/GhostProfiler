package com.ghostprofiler.agent.jvm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GcMonitor}.
 * Full sampling tests added in step 5.
 */
class GcMonitorTest {

    @Test
    void constructor_doesNotThrow() {
        assertDoesNotThrow(() -> new GcMonitor(500L));
    }

    @Test
    void initialStats_isEmpty() {
        GcMonitor monitor = new GcMonitor(500L);
        assertTrue(monitor.getLatestStats().isEmpty(),
                "Stats should be empty until first sample is taken");
    }

    @Test
    void start_doesNotThrow() {
        GcMonitor monitor = new GcMonitor(500L);
        assertDoesNotThrow(monitor::start);
        monitor.stop();
    }

    @Test
    void stop_doesNotThrow_whenNotStarted() {
        GcMonitor monitor = new GcMonitor(500L);
        assertDoesNotThrow(monitor::stop);
    }
}
