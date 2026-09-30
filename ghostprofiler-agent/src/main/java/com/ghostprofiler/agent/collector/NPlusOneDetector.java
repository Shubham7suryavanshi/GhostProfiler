package com.ghostprofiler.agent.collector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * NPlusOneDetector — detects N+1 query anti-patterns in the instrumented app.
 *
 * <p>An N+1 pattern occurs when the same (or structurally similar) SQL query
 * is executed many times within a short time window — the classic symptom of
 * iterating over a result set and issuing a new query per row instead of
 * fetching all related data in a single JOIN.
 *
 * <p>Detection algorithm:
 * <ol>
 *   <li>Normalize the SQL (strip literal values so "SELECT * FROM orders WHERE id=1"
 *       and "SELECT * FROM orders WHERE id=2" are treated as the same query shape).</li>
 *   <li>Keep a sliding window of recent executions per normalized query.</li>
 *   <li>If the count within the window exceeds {@code threshold}, emit a warning.</li>
 * </ol>
 *
 * <p>STUB — normalization, windowing, and warning logic added in step 6.
 */
public class NPlusOneDetector {

    private static final Logger LOG = Logger.getLogger(NPlusOneDetector.class.getName());

    /** Maximum number of N+1 warnings kept in memory for the /metrics endpoint */
    private static final int MAX_WARNINGS = 100;

    private final int threshold;
    private final long windowMs;

    /**
     * Timestamps (wall-clock ms) of recent executions keyed by normalized SQL.
     * ConcurrentHashMap so multiple instrumented threads can record queries
     * without blocking each other.
     */
    private final Map<String, List<Long>> executionWindows = new ConcurrentHashMap<>();

    /** Accumulated N+1 warnings, capped at MAX_WARNINGS. */
    private final List<NPlusOneWarning> warnings = Collections.synchronizedList(new ArrayList<>());

    /**
     * @param threshold  number of identical queries within windowMs that triggers a warning
     * @param windowMs   sliding window duration in milliseconds
     */
    public NPlusOneDetector(int threshold, long windowMs) {
        this.threshold = threshold;
        this.windowMs = windowMs;
    }

    /**
     * Records a SQL query execution and checks whether it crosses the N+1 threshold.
     *
     * @param sql  the raw SQL string (will be normalized internally)
     */
    public void record(String sql) {
        // TODO (step 6): normalize sql, update sliding window, check threshold
    }

    /**
     * Normalizes a SQL string by replacing all literal numeric and string
     * values with placeholders, so structurally identical queries hash the same.
     *
     * <p>Example: {@code "WHERE id = 42"} → {@code "WHERE id = ?"}
     *
     * @param sql  raw SQL string
     * @return     normalized form suitable for de-duplication
     */
    String normalize(String sql) {
        // TODO (step 6): implement regex-based normalization
        return sql == null ? "" : sql.trim().toLowerCase();
    }

    /**
     * Returns all N+1 warnings captured so far.
     */
    public List<NPlusOneWarning> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    // ── Inner class ───────────────────────────────────────────────────────────

    /**
     * Immutable record of a single N+1 detection event.
     */
    public static class NPlusOneWarning {
        private final String normalizedSql;
        private final int occurrences;
        private final long detectedAtMs;

        public NPlusOneWarning(String normalizedSql, int occurrences, long detectedAtMs) {
            this.normalizedSql = normalizedSql;
            this.occurrences = occurrences;
            this.detectedAtMs = detectedAtMs;
        }

        public String getNormalizedSql() { return normalizedSql; }
        public int getOccurrences() { return occurrences; }
        public long getDetectedAtMs() { return detectedAtMs; }
    }
}
