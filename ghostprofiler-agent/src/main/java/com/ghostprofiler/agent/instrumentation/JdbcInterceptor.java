package com.ghostprofiler.agent.instrumentation;

import net.bytebuddy.asm.Advice;

import java.util.logging.Logger;

/**
 * JdbcInterceptor — advice class for intercepting JDBC
 * {@code Statement.execute*()} and {@code PreparedStatement.execute*()} calls.
 *
 * <p>ByteBuddy will instrument these methods on the JDBC driver classes
 * in the target application's classpath. The interceptor captures:
 * <ul>
 *   <li>The SQL query text</li>
 *   <li>The execution duration in nanoseconds</li>
 * </ul>
 * and hands both to {@link NPlusOneDetector} for pattern analysis.
 *
 * <p>STUB — real JDBC method matching and NPlusOneDetector calls added in step 6.
 */
public class JdbcInterceptor {

    private static final Logger LOG = Logger.getLogger(JdbcInterceptor.class.getName());

    /**
     * Called at JDBC execute* method entry.
     *
     * @param sql  the SQL string from the Statement argument (injected by ByteBuddy)
     * @return     entry timestamp in nanoseconds
     */
    @Advice.OnMethodEnter
    public static long onEnter(@Advice.Argument(0) String sql) {
        // Capture start time at entry; the SQL is passed to onExit via @Advice.Enter
        return System.nanoTime();
    }

    /**
     * Called at JDBC execute* method exit.
     *
     * @param sql        the same SQL string from entry
     * @param startNanos entry timestamp returned by {@link #onEnter}
     * @param thrown     non-null if the JDBC call threw an exception
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(
            @Advice.Argument(0) String sql,
            @Advice.Enter long startNanos,
            @Advice.Thrown Throwable thrown) {
        try {
            long elapsedNanos = System.nanoTime() - startNanos;
            // Record timing in the central store for the /metrics endpoint
            com.ghostprofiler.agent.collector.MetricsStore.getInstance()
                    .recordQuery(sql, elapsedNanos);
            // Pass to N+1 detector for pattern analysis
            com.ghostprofiler.agent.collector.MetricsStore.getInstance()
                    .recordQueryForNPlusOne(sql);
        } catch (Throwable t) {
            // Fail-safe: never crash the host app (NFR2)
            LOG.warning("[GhostProfiler] JdbcInterceptor.onExit error: " + t.getMessage());
        }
    }

    private JdbcInterceptor() {}
}
