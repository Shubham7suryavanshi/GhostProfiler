package com.ghostprofiler.bench;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

/**
 * AgentOverheadBenchmark — measures the overhead added by GhostProfiler
 * instrumentation compared to a baseline (non-instrumented) execution.
 *
 * <p>Benchmark design:
 * <ul>
 *   <li>{@link #baseline} — calls a plain method with zero agent involvement,
 *       establishing the raw throughput ceiling.</li>
 *   <li>{@link #withInstrumentation} — calls the same method with the agent's
 *       {@code MethodInterceptor} advice active, measuring added overhead.</li>
 * </ul>
 *
 * <p>Run the benchmark after building with:
 * <pre>
 *   java -jar benchmarks/target/benchmarks.jar AgentOverheadBenchmark -rf json -rff docs/results.json
 * </pre>
 *
 * <p>STUB — real workload and agent attachment wired in step 9.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2, jvmArgsPrepend = {
    // Step 9 will add: "-javaagent:../ghostprofiler-agent/target/ghostprofiler-agent-1.0.0-SNAPSHOT.jar"
    // For the baseline fork, no agent flag is needed.
})
public class AgentOverheadBenchmark {

    /**
     * Baseline: a simple method call with no agent instrumentation.
     * This establishes the "zero overhead" throughput reference.
     *
     * @return a value consumed by JMH's Blackhole to prevent dead-code elimination
     */
    @Benchmark
    public long baseline() {
        // TODO (step 9): replace with realistic workload (e.g., string processing, math)
        return System.nanoTime();
    }

    /**
     * Instrumented: same workload but with the agent active.
     * The difference vs. {@link #baseline} is the agent's overhead.
     *
     * <p>In step 9, this method will be in a separate @Fork that passes
     * the -javaagent flag so the JVM under test has the agent attached.
     *
     * @return a value consumed by JMH's Blackhole
     */
    @Benchmark
    public long withInstrumentation() {
        // TODO (step 9): same workload as baseline — agent overhead is the only diff
        return System.nanoTime();
    }

    /**
     * Convenience main() so the benchmark can also be run from an IDE.
     */
    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
            .include(AgentOverheadBenchmark.class.getSimpleName())
            .build();
        new Runner(options).run();
    }
}
