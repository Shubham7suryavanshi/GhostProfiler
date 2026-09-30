package com.ghostprofiler.agent.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AgentConfig}.
 * Verifies default values and that construction is safe with any input.
 */
class AgentConfigTest {

    @Test
    void defaults_areReasonable() {
        AgentConfig config = new AgentConfig(null);
        assertEquals(9090, config.getHttpPort());
        assertEquals(1.0, config.getSamplingRate(), 0.001);
        assertTrue(config.getIncludedPackages().isEmpty(),
                "Default package filter should be empty (opt-in model)");
        assertEquals(5, config.getNPlusOneThreshold());
        assertEquals(1000L, config.getNPlusOneWindowMs());
    }

    @Test
    void constructor_withNullArgs_doesNotThrow() {
        assertDoesNotThrow(() -> new AgentConfig(null));
    }

    @Test
    void constructor_withEmptyArgs_doesNotThrow() {
        assertDoesNotThrow(() -> new AgentConfig(""));
    }

    @Test
    void constructor_withGarbageArgs_doesNotThrow() {
        // Malformed args must never crash the agent (NFR2)
        assertDoesNotThrow(() -> new AgentConfig("!!!not=valid===config!!!"));
    }
}
