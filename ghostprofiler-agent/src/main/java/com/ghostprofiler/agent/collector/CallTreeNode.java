package com.ghostprofiler.agent.collector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * CallTreeNode — one node in the per-thread method call tree.
 *
 * <p>Each node represents a single method invocation and stores:
 * <ul>
 *   <li>The fully-qualified method name</li>
 *   <li>Wall-clock start time and elapsed duration in nanoseconds</li>
 *   <li>Whether the method exited via exception</li>
 *   <li>Ordered list of child nodes (methods called by this method)</li>
 * </ul>
 *
 * <p>Nodes are arranged into a tree by {@link MetricsStore}, which maintains
 * a thread-local stack. When a method is entered, a new node is pushed;
 * when it exits, the node is popped and attached as a child of the current
 * top-of-stack node.
 *
 * <p>STUB — real construction/population wired in step 4.
 */
public class CallTreeNode {

    private static final AtomicLong ID_GENERATOR = new AtomicLong(0);

    /** Unique node ID (useful for serialization and de-duplication) */
    private final long id;

    /** Fully-qualified method descriptor, e.g. "com.example.OrderService#findAll" */
    private final String methodName;

    /** System.nanoTime() at method entry */
    private final long startNanos;

    /** Duration in nanoseconds; -1 until the method exits */
    private long durationNanos = -1L;

    /** True if the method threw an exception */
    private boolean threwException = false;

    /** Child nodes — methods invoked by this method */
    private final List<CallTreeNode> children = new ArrayList<>();

    /**
     * @param methodName  the fully-qualified method descriptor
     * @param startNanos  {@code System.nanoTime()} captured at method entry
     */
    public CallTreeNode(String methodName, long startNanos) {
        this.id = ID_GENERATOR.incrementAndGet();
        this.methodName = methodName;
        this.startNanos = startNanos;
    }

    /**
     * Records the method exit. Called by {@link MetricsStore} when the
     * thread-local stack is popped.
     *
     * @param exitNanos     {@code System.nanoTime()} at method exit
     * @param threwException  true if the method threw
     */
    public void complete(long exitNanos, boolean threwException) {
        this.durationNanos = exitNanos - startNanos;
        this.threwException = threwException;
    }

    /** Adds a child node (a method called by this method). */
    public void addChild(CallTreeNode child) {
        children.add(child);
    }

    // ── Accessors ──────────────────────────────────────────────────────────────

    public long getId() { return id; }
    public String getMethodName() { return methodName; }
    public long getStartNanos() { return startNanos; }
    public long getDurationNanos() { return durationNanos; }
    public boolean isThrewException() { return threwException; }
    public List<CallTreeNode> getChildren() { return Collections.unmodifiableList(children); }

    @Override
    public String toString() {
        return "CallTreeNode{id=" + id +
               ", method='" + methodName + '\'' +
               ", durationMs=" + (durationNanos / 1_000_000.0) +
               ", children=" + children.size() + '}';
    }
}
