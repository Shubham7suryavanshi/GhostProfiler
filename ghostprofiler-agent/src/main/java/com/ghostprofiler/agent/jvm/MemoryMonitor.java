package com.ghostprofiler.agent.jvm;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.logging.Logger;

/**
 * MemoryMonitor — samples JVM heap and non-heap memory usage via
 * {@link MemoryMXBean} (part of {@code java.lang.management}).
 *
 * <p>No third-party libraries are used — this satisfies the requirement
 * to use the built-in JVM management API.
 *
 * <p>The monitor runs as a low-priority daemon thread that samples memory
 * at a configurable interval and stores the latest readings in volatile
 * fields for lock-free reads by the HTTP server thread.
 *
 * <p>STUB — background sampling loop added in step 5.
 */
public class MemoryMonitor {

    private static final Logger LOG = Logger.getLogger(MemoryMonitor.class.getName());

    /** Sampling interval in milliseconds */
    private final long intervalMs;

    /** Built-in JVM bean — no external dependencies required */
    private final MemoryMXBean memoryMXBean;

    // Latest readings — volatile so the HTTP server thread sees the most
    // recent value without locking (safe because reads/writes of long
    // on 64-bit JVMs are effectively atomic with volatile)
    private volatile long heapUsedBytes = 0;
    private volatile long heapMaxBytes = 0;
    private volatile long nonHeapUsedBytes = 0;

    /** The background sampling thread (daemon — dies with the JVM) */
    private Thread samplerThread;

    /**
     * @param intervalMs  how often to sample memory, in milliseconds
     */
    public MemoryMonitor(long intervalMs) {
        this.intervalMs = intervalMs;
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
    }

    /**
     * Starts the background sampling thread.
     * Called once from {@link com.ghostprofiler.agent.AgentMain}.
     */
    public void start() {
        if (samplerThread != null) {
            return;
        }
        
        samplerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
                    MemoryUsage nonHeap = memoryMXBean.getNonHeapMemoryUsage();
                    
                    heapUsedBytes = heap.getUsed();
                    heapMaxBytes = heap.getMax();
                    nonHeapUsedBytes = nonHeap.getUsed();
                    
                    Thread.sleep(intervalMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    LOG.warning("[GhostProfiler] Memory sampling failed: " + e.getMessage());
                }
            }
        });
        samplerThread.setDaemon(true);
        samplerThread.setName("GhostProfiler-MemoryMonitor");
        samplerThread.start();
        
        LOG.info("[GhostProfiler] MemoryMonitor started. Sampling every " + intervalMs + "ms");
    }

    /**
     * Stops the background sampling thread gracefully.
     */
    public void stop() {
        if (samplerThread != null) {
            samplerThread.interrupt();
            samplerThread = null;
        }
    }

    /**
     * Returns the most recently sampled heap usage in bytes.
     * Zero until the sampler thread runs at least once.
     */
    public long getHeapUsedBytes() { return heapUsedBytes; }

    /**
     * Returns the JVM's maximum configured heap size in bytes.
     */
    public long getHeapMaxBytes() { return heapMaxBytes; }

    /**
     * Returns the most recently sampled non-heap (metaspace + code cache)
     * usage in bytes.
     */
    public long getNonHeapUsedBytes() { return nonHeapUsedBytes; }
}
