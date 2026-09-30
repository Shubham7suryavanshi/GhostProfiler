package com.ghostprofiler.agent.jvm;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * GcMonitor — tracks garbage collection statistics via
 * {@link GarbageCollectorMXBean} (built into {@code java.lang.management}).
 *
 * <p>For each GC collector in the JVM (e.g., "G1 Young Generation",
 * "G1 Old Generation"), this class records:
 * <ul>
 *   <li>Cumulative collection count</li>
 *   <li>Cumulative collection time in milliseconds</li>
 *   <li>Delta since last sample (pause duration per interval)</li>
 * </ul>
 *
 * <p>GC pauses are a key APM signal — long GC pauses cause latency spikes
 * that appear in method timing data without an obvious application-code cause.
 *
 * <p>STUB — sampling and delta calculation added in step 5.
 */
public class GcMonitor {

    private static final Logger LOG = Logger.getLogger(GcMonitor.class.getName());

    private final long intervalMs;

    /** All GC beans available in this JVM — discovered at startup */
    private final List<GarbageCollectorMXBean> gcBeans;

    /** Latest snapshot, replaced atomically on each sampling tick */
    private volatile List<GcStats> latestStats = Collections.emptyList();

    private Thread samplerThread;

    /**
     * @param intervalMs  sampling interval in milliseconds
     */
    public GcMonitor(long intervalMs) {
        this.intervalMs = intervalMs;
        // ManagementFactory returns all GC beans for the current JVM.
        // On G1GC there are typically two: young and old gen collectors.
        this.gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
    }

    /**
     * Starts the background GC sampling thread (daemon).
     */
    public void start() {
        // TODO (step 5): implement
        LOG.info("[GhostProfiler] GcMonitor.start() stub called. Found " +
                 gcBeans.size() + " GC collector(s).");
    }

    /** Stops the sampler gracefully. */
    public void stop() {
        // TODO (step 5): implement
    }

    /**
     * Returns the most recent GC statistics snapshot.
     * Returns an empty list until the sampler thread runs at least once.
     */
    public List<GcStats> getLatestStats() {
        return Collections.unmodifiableList(latestStats);
    }

    // ── Inner class ───────────────────────────────────────────────────────────

    /**
     * Snapshot of one GC collector's statistics at a point in time.
     */
    public static class GcStats {
        private final String collectorName;
        private final long collectionCount;
        private final long collectionTimeMs;

        public GcStats(String collectorName, long collectionCount, long collectionTimeMs) {
            this.collectorName = collectorName;
            this.collectionCount = collectionCount;
            this.collectionTimeMs = collectionTimeMs;
        }

        public String getCollectorName() { return collectorName; }
        public long getCollectionCount() { return collectionCount; }
        public long getCollectionTimeMs() { return collectionTimeMs; }
    }
}
