package com.ghostprofiler.agent.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.ghostprofiler.agent.collector.MetricsStore;
import com.ghostprofiler.agent.collector.NPlusOneDetector;
import com.ghostprofiler.agent.jvm.GcMonitor;
import com.ghostprofiler.agent.jvm.MemoryMonitor;

import java.util.logging.Logger;

/**
 * MetricsJsonSerializer — converts all collected metrics into a single
 * JSON document using Jackson.
 *
 * <p>Separated from {@link MetricsHttpServer} so that the serialization
 * format can be tested independently without starting a real HTTP server.
 *
 * <p>Output structure (subject to refinement in step 8):
 * <pre>
 * {
 *   "timestamp": 1722000000000,
 *   "memory": {
 *     "heapUsedMb": 128.5,
 *     "heapMaxMb": 512.0,
 *     "nonHeapUsedMb": 64.2
 *   },
 *   "gc": [
 *     { "collector": "G1 Young Generation", "count": 12, "timeMs": 345 }
 *   ],
 *   "methods": {
 *     "com.example.OrderService#findAll": {
 *       "callCount": 42,
 *       "avgDurationMs": 15.3,
 *       "maxDurationMs": 120.0
 *     }
 *   },
 *   "nPlusOneWarnings": [
 *     { "sql": "select * from orders where id = ?", "occurrences": 10, "detectedAt": 1722000001000 }
 *   ]
 * }
 * </pre>
 *
 * <p>STUB — actual serialization logic added in step 8.
 */
public class MetricsJsonSerializer {

    private static final Logger LOG = Logger.getLogger(MetricsJsonSerializer.class.getName());

    /**
     * Reuse a single ObjectMapper instance — it is thread-safe after configuration.
     * INDENT_OUTPUT is disabled in production to reduce payload size; it can be
     * enabled for debugging by setting {@code ghost.json.pretty=true}.
     */
    private final ObjectMapper mapper;

    public MetricsJsonSerializer() {
        this.mapper = new ObjectMapper();
        this.mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    /**
     * Builds a complete JSON metrics snapshot from the provided sources.
     *
     * @param metricsStore   method timing data and call trees
     * @param memoryMonitor  current heap and non-heap usage
     * @param gcMonitor      current GC statistics
     * @return               the JSON string ready to send as an HTTP response body
     */
    public String serialize(
            MetricsStore metricsStore,
            MemoryMonitor memoryMonitor,
            GcMonitor gcMonitor) {
        try {
            // TODO (step 8): populate the ObjectNode from all three sources
            ObjectNode root = mapper.createObjectNode();
            root.put("timestamp", System.currentTimeMillis());
            root.put("status", "stub — real data coming in step 8");
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            // Fail-safe: return a minimal error JSON rather than propagating (NFR2)
            LOG.warning("[GhostProfiler] Serialization failed: " + e.getMessage());
            return "{\"error\":\"serialization failed\"}";
        }
    }
}
