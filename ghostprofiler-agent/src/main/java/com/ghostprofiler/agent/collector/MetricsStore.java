package com.ghostprofiler.agent.collector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * MetricsStore — the central in-memory store for all collected metrics.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Maintains a per-thread call stack (Deque of {@link CallTreeNode}s)
 *       so that parent-child relationships between nested calls are captured.</li>
 *   <li>Stores completed root-level call trees for later JSON serialization.</li>
 *   <li>Stores flat method timing aggregates (call count + total duration)
 *       for the summary view.</li>
 *   <li>Exposes a snapshot of all metrics to {@code MetricsHttpServer} without
 *       blocking the instrumented threads (lock-free reads via concurrent
 *       collections).</li>
 * </ul>
 *
 * <p>This is a singleton — one instance is shared across the entire agent.
 * All mutations use thread-safe data structures or are confined to the
 * owning thread via ThreadLocal.
 *
 * <p>STUB — real push/pop/record logic added in step 4.
 */
public class MetricsStore {

    /** Singleton instance — created once during agent initialization. */
    private static volatile MetricsStore instance;

    /**
     * Flat timing aggregates keyed by method name.
     * Using ConcurrentHashMap for lock-free reads and fine-grained writes.
     */
    private final Map<String, MethodStats> methodStats = new ConcurrentHashMap<>();

    /**
     * Completed root-level call trees from all threads.
     * CopyOnWriteArrayList means reads (from MetricsHttpServer) are always
     * safe and never block the writer threads.
     */
    private final List<CallTreeNode> completedTrees = new CopyOnWriteArrayList<>();

    /**
     * Dedicated N+1 detector. Initialized with defaults; updated when
     * AgentConfig is available via {@link #setDetectorConfig}.
     */
    private volatile NPlusOneDetector detector = new NPlusOneDetector(5, 1000L);

    /**
     * Per-thread stack tracking the current call chain.
     * Using a Deque-based stack so we can push on enter and pop on exit
     * without touching other threads' stacks.
     */
    private final ThreadLocal<java.util.Deque<CallTreeNode>> callStack =
            ThreadLocal.withInitial(java.util.ArrayDeque::new);

    // Private constructor — use getInstance()
    private MetricsStore() {}

    /**
     * Returns the singleton instance, creating it on first call.
     * Double-checked locking with volatile for thread-safe lazy init.
     */
    public static MetricsStore getInstance() {
        if (instance == null) {
            synchronized (MetricsStore.class) {
                if (instance == null) {
                    instance = new MetricsStore();
                }
            }
        }
        return instance;
    }

    /**
     * Called by {@link com.ghostprofiler.agent.instrumentation.MethodInterceptor}
     * at method entry. Pushes a new node onto this thread's call stack.
     *
     * @param methodName  fully-qualified method descriptor
     * @param startNanos  {@code System.nanoTime()} from the enter advice
     */
    public void onMethodEnter(String methodName, long startNanos) {
        CallTreeNode node = new CallTreeNode(methodName, startNanos);
        callStack.get().push(node);
    }

    /**
     * Called by {@link com.ghostprofiler.agent.instrumentation.MethodInterceptor}
     * at method exit. Pops the top node, links it as a child of its parent,
     * and (if it was a root call) saves it to completedTrees.
     *
     * @param methodName   must match the methodName from the corresponding enter
     * @param exitNanos    {@code System.nanoTime()} from the exit advice
     * @param threwException  true if the method exited via throw
     */
    public void onMethodExit(String methodName, long exitNanos, boolean threwException) {
        java.util.Deque<CallTreeNode> stack = callStack.get();
        CallTreeNode node = stack.poll();

        if (node != null) {
            node.complete(exitNanos, threwException);

            // Update flat stats lock-free
            MethodStats stats = methodStats.computeIfAbsent(methodName, k -> new MethodStats());
            stats.recordCall(node.getDurationNanos());

            CallTreeNode parent = stack.peek();
            if (parent != null) {
                parent.addChild(node);
            } else {
                // Root node completed
                completedTrees.add(node);
                // Keep tree list bounded to avoid OOM
                if (completedTrees.size() > 100) {
                    completedTrees.remove(0);
                }
            }
        }
    }

    /**
     * Records a JDBC query execution timing in the flat stats map.
     *
     * @param sql          the SQL query string (used as the key)
     * @param durationNanos  execution time in nanoseconds
     */
    public void recordQuery(String sql, long durationNanos) {
        if (sql == null || sql.isBlank()) {
            return;
        }
        // Prefix query keys with "[SQL] " so they are visually distinct from
        // method names in the /metrics output.
        String key = "[SQL] " + sql.trim();
        MethodStats stats = methodStats.computeIfAbsent(key, k -> new MethodStats());
        stats.recordCall(durationNanos);
    }

    /**
     * Forwards a SQL query to the N+1 detector for pattern analysis.
     *
     * @param sql  raw SQL string from the JDBC interceptor
     */
    public void recordQueryForNPlusOne(String sql) {
        detector.record(sql);
    }

    /**
     * Replaces the N+1 detector with one using config-specified thresholds.
     * Called from AgentMain after AgentConfig is parsed.
     *
     * @param threshold  occurrence count threshold
     * @param windowMs   sliding window in milliseconds
     */
    public void setDetectorConfig(int threshold, long windowMs) {
        this.detector = new NPlusOneDetector(threshold, windowMs);
    }

    /**
     * Returns the most recent GC statistics snapshot.
     * Safe to call from any thread at any time.
     */
    public Map<String, MethodStats> getMethodStats() {
        return Collections.unmodifiableMap(methodStats);
    }

    /**
     * Returns the list of completed root call trees.
     * CopyOnWriteArrayList ensures this is always consistent.
     */
    public List<CallTreeNode> getCompletedTrees() {
        return completedTrees;
    }

    /**
     * Exposes the N+1 detector for serialization in /metrics output.
     */
    public NPlusOneDetector getDetector() {
        return detector;
    }

    /**
     * Clears all stored metrics. Useful for tests and reset endpoints.
     */
    public void reset() {
        methodStats.clear();
        completedTrees.clear();
    }

    // ── Inner class ───────────────────────────────────────────────────────────

    /**
     * Aggregate statistics for a single method.
     * Uses volatile longs for visibility without locking.
     */
    public static class MethodStats {
        private final java.util.concurrent.atomic.LongAdder callCount = new java.util.concurrent.atomic.LongAdder();
        private final java.util.concurrent.atomic.LongAdder totalDurationNanos = new java.util.concurrent.atomic.LongAdder();
        private final java.util.concurrent.atomic.AtomicLong maxDurationNanos = new java.util.concurrent.atomic.AtomicLong(0);

        public void recordCall(long durationNanos) {
            callCount.increment();
            totalDurationNanos.add(durationNanos);
            long currentMax;
            do {
                currentMax = maxDurationNanos.get();
                if (durationNanos <= currentMax) {
                    break;
                }
            } while (!maxDurationNanos.compareAndSet(currentMax, durationNanos));
        }

        public long getCallCount() { return callCount.sum(); }
        public long getTotalDurationNanos() { return totalDurationNanos.sum(); }
        public long getMaxDurationNanos() { return maxDurationNanos.get(); }

        public double getAvgDurationMs() {
            long count = callCount.sum();
            return count == 0 ? 0 : (totalDurationNanos.sum() / 1_000_000.0) / count;
        }
    }
}
