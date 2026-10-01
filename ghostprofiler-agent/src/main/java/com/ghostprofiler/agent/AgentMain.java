package com.ghostprofiler.agent;

import java.lang.instrument.Instrumentation;
import java.util.logging.Logger;

/**
 * AgentMain — the entry point for the GhostProfiler Java agent.
 *
 * <p>The JVM calls {@link #premain(String, Instrumentation)} before the
 * target application's {@code main()} method when the agent is attached
 * at JVM startup via {@code -javaagent:ghostprofiler.jar}.
 *
 * <p>The JVM calls {@link #agentmain(String, Instrumentation)} when the
 * agent is attached to an already-running JVM at runtime via the
 * Attach API (e.g., through {@code VirtualMachine.attach()}).
 *
 * <p>Both entry points delegate to the same {@link #initialize} method
 * so the agent behaviour is identical regardless of attach timing.
 *
 * <p>STUB — real logic will be added in Build Order step 2.
 */
public class AgentMain {

    private static final Logger LOG = Logger.getLogger(AgentMain.class.getName());

    /**
     * Called by the JVM before the application main() when using -javaagent.
     *
     * @param agentArgs  optional argument string passed after '=' in the -javaagent flag,
     *                   e.g. {@code -javaagent:ghostprofiler.jar=port=9090,packages=com.example}
     * @param instrumentation  the JVM instrumentation handle — needed to register
     *                         class-file transformers (step 3)
     */
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        initialize(agentArgs, instrumentation);
    }

    /**
     * Called by the JVM when the agent is attached to an already-running JVM.
     *
     * @param agentArgs  same format as premain
     * @param instrumentation  same handle as premain
     */
    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        initialize(agentArgs, instrumentation);
    }

    /**
     * Shared initialization logic for both attach modes.
     *
     * <p>Everything here is wrapped in a broad catch so that a bug in the
     * agent can never crash or hang the host application (NFR2).
     *
     * @param agentArgs       raw agent argument string (may be null)
     * @param instrumentation JVM instrumentation API handle
     */
    private static void initialize(String agentArgs, Instrumentation instrumentation) {
        try {
            LOG.info("[GhostProfiler] Agent initializing. args=" + agentArgs);

            // Step 1: Parse configuration from agent args + system properties
            com.ghostprofiler.agent.config.AgentConfig config =
                    new com.ghostprofiler.agent.config.AgentConfig(agentArgs);

            // Step 2: Configure the N+1 detector in MetricsStore with user-supplied thresholds
            com.ghostprofiler.agent.collector.MetricsStore.getInstance()
                    .setDetectorConfig(config.getNPlusOneThreshold(), config.getNPlusOneWindowMs());

            // Step 3: Install method timing + JDBC instrumentation via ByteBuddy
            new com.ghostprofiler.agent.instrumentation.MethodTimingTransformer(
                    config.getIncludedPackages()).install(instrumentation);

            // Step 4: Start memory sampling daemon thread
            com.ghostprofiler.agent.jvm.MemoryMonitor memoryMonitor =
                    new com.ghostprofiler.agent.jvm.MemoryMonitor(5000L); // sample every 5s
            memoryMonitor.start();

            // Step 5: Start GC sampling daemon thread
            com.ghostprofiler.agent.jvm.GcMonitor gcMonitor =
                    new com.ghostprofiler.agent.jvm.GcMonitor(5000L); // sample every 5s
            gcMonitor.start();

            // Step 6: Start the embedded HTTP server for /metrics and /health
            com.ghostprofiler.agent.server.MetricsHttpServer httpServer =
                    new com.ghostprofiler.agent.server.MetricsHttpServer(
                            config.getHttpPort(),
                            com.ghostprofiler.agent.collector.MetricsStore.getInstance(),
                            memoryMonitor,
                            gcMonitor);
            httpServer.start();

            LOG.info("[GhostProfiler] Agent attached successfully. Metrics available at http://localhost:"
                    + config.getHttpPort() + "/metrics");
        } catch (Throwable t) {
            // Fail-safe: log but never propagate — we must not crash the host app (NFR2)
            LOG.severe("[GhostProfiler] Agent initialization failed: " + t.getMessage());
        }
    }

    // Private constructor — this class is never instantiated
    private AgentMain() {}
}
