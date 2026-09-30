package com.ghostprofiler.agent.instrumentation;

import net.bytebuddy.asm.Advice;

import java.util.logging.Logger;

/**
 * MethodInterceptor — the advice class injected into instrumented methods.
 *
 * <p>ByteBuddy's {@link Advice} mechanism works by copying static method
 * bytecode directly into the target method's bytecode at the enter/exit
 * points. The annotated static methods below are NOT called via reflection —
 * they are inlined by ByteBuddy at the bytecode level, which keeps overhead
 * very low (NFR1).
 *
 * <p>Key constraint: {@code @Advice} methods must be static and may not
 * reference instance state. All shared state goes through {@code MetricsStore}.
 *
 * <p>STUB — actual timing logic wired to MetricsStore added in step 3.
 */
public class MethodInterceptor {

    private static final Logger LOG = Logger.getLogger(MethodInterceptor.class.getName());

    /**
     * Called at method entry.
     *
     * <p>{@code @Advice.Origin} injects the method descriptor string
     * at bytecode rewrite time — zero runtime reflection cost.
     *
     * @return the entry timestamp in nanoseconds, passed to {@link #onExit}
     *         via the {@code @Advice.Enter} mechanism
     */
    @Advice.OnMethodEnter
    public static long onEnter(@Advice.Origin String methodDescriptor) {
        long startNanos = System.nanoTime();
        com.ghostprofiler.agent.collector.MetricsStore.getInstance().onMethodEnter(methodDescriptor, startNanos);
        return startNanos;
    }

    /**
     * Called at method exit (both normal return and exception throw).
     *
     * @param methodDescriptor  fully-qualified method name injected by ByteBuddy
     * @param startNanos        the value returned by {@link #onEnter}
     * @param thrown            non-null if the method exited via exception
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(
            @Advice.Origin String methodDescriptor,
            @Advice.Enter long startNanos,
            @Advice.Thrown Throwable thrown) {
        try {
            long exitNanos = System.nanoTime();
            com.ghostprofiler.agent.collector.MetricsStore.getInstance().onMethodExit(methodDescriptor, exitNanos, thrown != null);
        } catch (Throwable t) {
            // Never propagate — a bug here must not crash the host app (NFR2)
            LOG.warning("[GhostProfiler] onExit error for [" + methodDescriptor + "]: " + t.getMessage());
        }
    }

    // Private constructor — this class exists only as a ByteBuddy Advice template
    private MethodInterceptor() {}
}
