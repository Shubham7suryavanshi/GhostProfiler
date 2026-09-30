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
        if (includedPackages.isEmpty()) {
            LOG.info("[GhostProfiler] No included packages configured. Instrumentation is disabled.");
            return;
        }

        net.bytebuddy.matcher.ElementMatcher.Junction<TypeDescription> typeMatcher = net.bytebuddy.matcher.ElementMatchers.none();
        for (String pkg : includedPackages) {
            typeMatcher = typeMatcher.or(net.bytebuddy.matcher.ElementMatchers.nameStartsWith(pkg));
        }

        new AgentBuilder.Default()
                .type(typeMatcher)
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.visit(net.bytebuddy.asm.Advice.to(MethodInterceptor.class)
                                .on(net.bytebuddy.matcher.ElementMatchers.isMethod()
                                        .and(net.bytebuddy.matcher.ElementMatchers.not(net.bytebuddy.matcher.ElementMatchers.isAbstract())))))
                .with(new LoggingTransformListener())
                .installOn(instrumentation);

        LOG.info("[GhostProfiler] MethodTimingTransformer installed for packages: " + includedPackages);
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
