package com.ghostprofiler.agent.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.ghostprofiler.agent.collector.MetricsStore;
import com.ghostprofiler.agent.collector.NPlusOneDetector;
import com.ghostprofiler.agent.jvm.GcMonitor;
import com.ghostprofiler.agent.jvm.MemoryMonitor;

import java.util.Map;
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
    /**
     * Builds a complete JSON metrics snapshot from the provided sources.
     *
     * <p>Output structure:
     * <pre>
     * {
     *   "timestamp": 1722000000000,
     *   "memory": {
     *     "heapUsedBytes": 134217728,
     *     "heapMaxBytes": 536870912,
     *     "nonHeapUsedBytes": 67108864
     *   },
     *   "gc": [
     *     { "collectorName": "G1 Young Generation", "collectionCount": 12, "collectionTimeMs": 345 }
     *   ],
     *   "methods": {
     *     "com.example.OrderService#findAll": {
     *       "callCount": 42,
     *       "avgDurationMs": 15.3,
     *       "maxDurationMs": 120.0
     *     }
     *   },
     *   "nPlusOneWarnings": [
     *     { "normalizedSql": "select * from orders where id = ?",
     *       "occurrences": 10,
     *       "detectedAt": 1722000001000 }
     *   ]
     * }
     * </pre>
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
            ObjectNode root = mapper.createObjectNode();
            root.put("timestamp", System.currentTimeMillis());

            // ── Memory section ───────────────────────────────────────────────────
            ObjectNode memNode = root.putObject("memory");
            memNode.put("heapUsedBytes",    memoryMonitor.getHeapUsedBytes());
            memNode.put("heapMaxBytes",     memoryMonitor.getHeapMaxBytes());
            memNode.put("nonHeapUsedBytes", memoryMonitor.getNonHeapUsedBytes());

            // ── GC section ─────────────────────────────────────────────────────
            com.fasterxml.jackson.databind.node.ArrayNode gcArray = root.putArray("gc");
            for (GcMonitor.GcStats stats : gcMonitor.getLatestStats()) {
                ObjectNode gcNode = gcArray.addObject();
                gcNode.put("collectorName",   stats.getCollectorName());
                gcNode.put("collectionCount", stats.getCollectionCount());
                gcNode.put("collectionTimeMs", stats.getCollectionTimeMs());
            }

            // ── Method timings section ────────────────────────────────────────
            ObjectNode methodsNode = root.putObject("methods");
            for (Map.Entry<String, MetricsStore.MethodStats> entry
                    : metricsStore.getMethodStats().entrySet()) {
                MetricsStore.MethodStats ms = entry.getValue();
                ObjectNode mNode = methodsNode.putObject(entry.getKey());
                mNode.put("callCount",     ms.getCallCount());
                mNode.put("avgDurationMs", ms.getAvgDurationMs());
                // Convert nanoseconds to milliseconds for display
                mNode.put("maxDurationMs", ms.getMaxDurationNanos() / 1_000_000.0);
            }

            // ── N+1 warnings section ────────────────────────────────────────
            com.fasterxml.jackson.databind.node.ArrayNode warningsArray =
                    root.putArray("nPlusOneWarnings");
            for (NPlusOneDetector.NPlusOneWarning w
                    : metricsStore.getDetector().getWarnings()) {
                ObjectNode wNode = warningsArray.addObject();
                wNode.put("normalizedSql", w.getNormalizedSql());
                wNode.put("occurrences",   w.getOccurrences());
                wNode.put("detectedAt",    w.getDetectedAtMs());
            }

            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            // Fail-safe: return a minimal error JSON rather than propagating (NFR2)
            LOG.warning("[GhostProfiler] Serialization failed: " + e.getMessage());
            return "{\"error\":\"serialization failed\"}";
        }
    }
}
