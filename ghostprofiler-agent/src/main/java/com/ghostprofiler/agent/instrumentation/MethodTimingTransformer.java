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

        // Build a compound type matcher covering all configured package prefixes
        net.bytebuddy.matcher.ElementMatcher.Junction<TypeDescription> typeMatcher =
                net.bytebuddy.matcher.ElementMatchers.none();
        for (String pkg : includedPackages) {
            typeMatcher = typeMatcher.or(net.bytebuddy.matcher.ElementMatchers.nameStartsWith(pkg));
        }

        // ── 1. Method timing transformer ────────────────────────────────────
        // Intercepts every non-abstract method in the configured packages and
        // injects the @Advice bytecode from MethodInterceptor.
        new AgentBuilder.Default()
                // Never instrument the agent's own classes — prevents infinite loops
                .ignore(net.bytebuddy.matcher.ElementMatchers.nameStartsWith("com.ghostprofiler.agent"))
                .type(typeMatcher)
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.visit(net.bytebuddy.asm.Advice.to(MethodInterceptor.class)
                                .on(net.bytebuddy.matcher.ElementMatchers.isMethod()
                                        .and(net.bytebuddy.matcher.ElementMatchers.not(
                                                net.bytebuddy.matcher.ElementMatchers.isAbstract())))))
                .with(new LoggingTransformListener())
                .installOn(instrumentation);

        // ── 2. JDBC interceptor ─────────────────────────────────────────────
        // Intercepts java.sql.Statement.execute*() and
        // java.sql.PreparedStatement.execute*() to capture SQL + duration.
        // We match on the concrete driver implementation classes (not the interface)
        // because ByteBuddy instruments loaded class bytecode, not interfaces.
        new AgentBuilder.Default()
                .ignore(net.bytebuddy.matcher.ElementMatchers.nameStartsWith("com.ghostprofiler.agent"))
                // Match any class whose name contains well-known JDBC implementation patterns
                .type(net.bytebuddy.matcher.ElementMatchers
                        .hasSuperType(net.bytebuddy.matcher.ElementMatchers
                                .named("java.sql.Statement")))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.visit(net.bytebuddy.asm.Advice.to(JdbcInterceptor.class)
                                .on(net.bytebuddy.matcher.ElementMatchers.nameStartsWith("execute")
                                        .and(net.bytebuddy.matcher.ElementMatchers.isMethod())
                                        .and(net.bytebuddy.matcher.ElementMatchers.takesArgument(
                                                0, String.class)))))
                .with(new LoggingTransformListener())
                .installOn(instrumentation);

        LOG.info("[GhostProfiler] MethodTimingTransformer + JdbcInterceptor installed for packages: "
                + includedPackages);
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
