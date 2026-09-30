package com.ghostprofiler.agent.collector;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link NPlusOneDetector}.
 * The normalization logic can be unit-tested now; the windowing tests
 * will be filled in during step 6.
 */
class NPlusOneDetectorTest {

    private final NPlusOneDetector detector = new NPlusOneDetector(5, 1000L);

    @Test
    void normalize_lowercasesAndTrims() {
        String result = detector.normalize("  SELECT * FROM orders  ");
        assertEquals("select * from orders", result);
    }

    @Test
    void normalize_handlesNull() {
        assertDoesNotThrow(() -> detector.normalize(null));
        assertEquals("", detector.normalize(null));
    }

    @Test
    void record_doesNotThrow() {
        assertDoesNotThrow(() -> detector.record("SELECT * FROM orders WHERE id = 1"));
    }

    @Test
    void getWarnings_returnsEmptyInitially() {
        NPlusOneDetector fresh = new NPlusOneDetector(5, 1000L);
        assertTrue(fresh.getWarnings().isEmpty());
    }
}
