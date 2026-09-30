package com.ghostprofiler.agent.instrumentation;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;

import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Set;
import java.util.logging.Logger;

/**
 * MethodTimingTransformer — sets up the ByteBuddy {@link AgentBuilder}
 * that intercepts methods in the configured packages.
 *
 * <p>This class has exactly one responsibility: configure WHICH types and
 * methods to intercept, and connect them to {@link MethodInterceptor}.
 * It does not contain any timing or storage logic.
 *
 * <p>STUB — real ByteBuddy DSL wiring will be added in step 3.
 */
public class MethodTimingTransformer {

    private static final Logger LOG = Logger.getLogger(MethodTimingTransformer.class.getName());

    private final Set<String> includedPackages;

    /**
     * @param includedPackages  set of package prefixes to instrument,
     *                          e.g. {@code {"com.example", "com.acme"}}
     */
    public MethodTimingTransformer(Set<String> includedPackages) {
        this.includedPackages = Set.copyOf(includedPackages);
    }

    /**
     * Installs the ByteBuddy agent transformation pipeline.
     *
     * <p>After this method returns, ByteBuddy will intercept every method
     * in the configured packages and delegate timing calls to
     * {@link MethodInterceptor}.
     *
     * @param instrumentation  the JVM instrumentation handle from premain
     */
    public void install(Instrumentation instrumentation) {
        // TODO (step 3): implement AgentBuilder configuration
        LOG.info("[GhostProfiler] MethodTimingTransformer.install() stub called.");
    }

    // ── Inner listener for install-time diagnostics ─────────────────────────

    /**
     * A ByteBuddy listener that logs when a class transformation fails.
     * Failures are logged but never re-thrown (NFR2).
     */
    static class LoggingTransformListener extends AgentBuilder.Listener.Adapter {

        @Override
        public void onError(
                String typeName,
                ClassLoader classLoader,
                JavaModule module,
                boolean loaded,
                Throwable throwable) {
            LOG.warning("[GhostProfiler] Failed to transform [" + typeName + "]: " + throwable.getMessage());
        }
    }
}
