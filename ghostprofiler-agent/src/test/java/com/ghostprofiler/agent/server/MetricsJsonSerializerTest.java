package com.ghostprofiler.agent.server;

import com.ghostprofiler.agent.collector.MetricsStore;
import com.ghostprofiler.agent.jvm.GcMonitor;
import com.ghostprofiler.agent.jvm.MemoryMonitor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MetricsJsonSerializer}.
 * Full field-by-field JSON validation added in step 8.
 */
class MetricsJsonSerializerTest {

    private final MetricsJsonSerializer serializer = new MetricsJsonSerializer();
    private final MetricsStore store = MetricsStore.getInstance();
    private final MemoryMonitor memory = new MemoryMonitor(500L);
    private final GcMonitor gc = new GcMonitor(500L);

    @Test
    void serialize_returnsValidJson() {
        String json = serializer.serialize(store, memory, gc);
        assertNotNull(json);
        assertFalse(json.isBlank());
        // Must be parseable JSON (starts and ends with braces)
        assertTrue(json.trim().startsWith("{"), "Expected JSON object");
        assertTrue(json.trim().endsWith("}"), "Expected JSON object");
    }

    @Test
    void serialize_containsTimestamp() {
        String json = serializer.serialize(store, memory, gc);
        assertTrue(json.contains("timestamp"), "JSON must include a timestamp field");
    }

    @Test
    void serialize_neverThrows() {
        // Even with null-ish state, serializer must not propagate exceptions (NFR2)
        assertDoesNotThrow(() -> serializer.serialize(store, memory, gc));
    }
}
