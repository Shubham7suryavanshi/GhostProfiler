package com.ghostprofiler.agent.collector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;

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

    // Regex patterns used for SQL normalization.
    // We compile them once at class-load time — Pattern instances are thread-safe.
    /** Matches single-quoted string literals: 'any text including ''escaped'' quotes' */
    private static final Pattern PATTERN_STRING_LITERAL  = Pattern.compile("'([^']|'')*'");
    /** Matches integer and decimal numeric literals */
    private static final Pattern PATTERN_NUMERIC_LITERAL = Pattern.compile("\\b\\d+(\\.\\d+)?\\b");
    /** Collapses multiple whitespace characters into a single space */
    private static final Pattern PATTERN_WHITESPACE      = Pattern.compile("\\s+");

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
        if (sql == null || sql.isBlank()) {
            return;
        }

        String normalized = normalize(sql);
        long nowMs = System.currentTimeMillis();

        // computeIfAbsent gives us the list for this query shape, creating it if absent.
        // The inner list must be synchronized because multiple threads may record
        // the same query pattern concurrently.
        List<Long> timestamps = executionWindows.computeIfAbsent(
                normalized, k -> Collections.synchronizedList(new ArrayList<>()));

        synchronized (timestamps) {
            // 1. Purge timestamps older than the window boundary
            long windowStart = nowMs - windowMs;
            timestamps.removeIf(ts -> ts < windowStart);

            // 2. Record this execution
            timestamps.add(nowMs);

            // 3. Check whether we've crossed the threshold
            if (timestamps.size() >= threshold) {
                emitWarning(normalized, timestamps.size(), nowMs);
                // Clear the window after warning so we don't re-emit every subsequent call.
                // This means the detector fires once per burst, not continuously.
                timestamps.clear();
            }
        }
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
        if (sql == null) {
            return "";
        }
        // Order matters: replace string literals before numerics to avoid
        // matching digits that appear inside quoted strings.
        String s = PATTERN_STRING_LITERAL.matcher(sql).replaceAll("?");
        s = PATTERN_NUMERIC_LITERAL.matcher(s).replaceAll("?");
        s = PATTERN_WHITESPACE.matcher(s).replaceAll(" ");
        return s.trim().toLowerCase();
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private void emitWarning(String normalizedSql, int occurrences, long detectedAtMs) {
        LOG.warning("[GhostProfiler] N+1 detected: query fired " + occurrences +
                    " times in " + windowMs + "ms window. SQL: " + normalizedSql);
        synchronized (warnings) {
            if (warnings.size() >= MAX_WARNINGS) {
                // Drop oldest warning to keep the list bounded (avoid OOM)
                warnings.remove(0);
            }
            warnings.add(new NPlusOneWarning(normalizedSql, occurrences, detectedAtMs));
        }
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
