package com.ghostprofiler.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AgentMain}.
 *
 * <p>Because {@code premain} and {@code agentmain} require a live JVM
 * Instrumentation instance, the tests here verify the fail-safe contract:
 * neither method should throw any exception under any input.
 *
 * <p>Full attach-and-instrument tests will be added in step 2 using a
 * ByteBuddy self-attach helper.
 */
class AgentMainTest {

    @Test
    void premain_withNullArgs_doesNotThrow() {
        // The JVM is allowed to pass null for agentArgs — we must handle it
        assertDoesNotThrow(() -> AgentMain.premain(null, null));
    }

    @Test
    void premain_withArgs_doesNotThrow() {
        assertDoesNotThrow(() -> AgentMain.premain("port=9090,packages=com.example", null));
    }

    @Test
    void agentmain_withNullArgs_doesNotThrow() {
        assertDoesNotThrow(() -> AgentMain.agentmain(null, null));
    }

    @Test
    void agentmain_withArgs_doesNotThrow() {
        assertDoesNotThrow(() -> AgentMain.agentmain("port=9191", null));
    }
}
