package com.ghostprofiler.bench;

import com.ghostprofiler.agent.collector.MetricsStore;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * AgentOverheadBenchmark — measures the overhead added by GhostProfiler
 * instrumentation compared to a baseline (non-instrumented) execution.
 *
 * <p>Benchmark design:
 * <ul>
 *   <li>{@link #baseline} — calls a realistic workload (string processing + math)
 *       with zero agent involvement, establishing the raw throughput ceiling.</li>
 *   <li>{@link #withInstrumentation} — calls the same workload but also simulates
 *       the MetricsStore onMethodEnter/onMethodExit path that the @Advice injects.
 *       This isolates exactly the cost of the instrumentation logic.</li>
 * </ul>
 *
 * <p>Run after building:
 * <pre>
 *   java -jar benchmarks/target/benchmarks.jar AgentOverheadBenchmark -rf json -rff docs/results.json
 * </pre>
 *
 * <p>To benchmark with the actual -javaagent attached, run the benchmarks jar with:
 * <pre>
 *   java -javaagent:ghostprofiler-agent/target/ghostprofiler-agent-1.0.0-SNAPSHOT.jar \
 *        -Dghost.packages=com.ghostprofiler.bench \
 *        -jar benchmarks/target/benchmarks.jar
 * </pre>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2)
public class AgentOverheadBenchmark {

    // Pre-generate a deterministic input so the workload is CPU-bound, not memory-bound.
    // This is the same for baseline and instrumented runs so comparisons are fair.
    private static final String WORKLOAD_INPUT = "GhostProfiler-Benchmark-Input-String-2024";

    /**
     * Baseline: a realistic string-processing + math workload with ZERO agent involvement.
     * This establishes the cost floor that the agent adds overhead on top of.
     *
     * <p>We use Blackhole.consume() to prevent the JIT from eliminating dead code.
     *
     * @param bh  JMH blackhole to prevent dead-code elimination
     */
    @Benchmark
    public void baseline(Blackhole bh) {
        // Realistic workload: hash + string concatenation + integer math
        int hash = WORKLOAD_INPUT.hashCode();
        String result = WORKLOAD_INPUT + hash;
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += result.charAt(i % result.length());
        }
        bh.consume(sum);
    }

    /**
     * Instrumented: same workload PLUS the MetricsStore enter/exit path.
     *
     * <p>When the actual -javaagent is NOT attached, this measures the direct
     * MetricsStore API call overhead (a useful lower bound).
     * When the -javaagent IS attached (via the separate run above), ByteBuddy
     * also inlines the @Advice bytecode, and this benchmark captures the total cost.
     *
     * @param bh  JMH blackhole to prevent dead-code elimination
     */
    @Benchmark
    public void withInstrumentation(Blackhole bh) {
        MetricsStore store = MetricsStore.getInstance();

        // Simulate what @Advice.OnMethodEnter injects: record entry + push to stack
        long startNanos = System.nanoTime();
        store.onMethodEnter("com.ghostprofiler.bench.BenchmarkTarget#compute", startNanos);

        // The actual workload (identical to baseline)
        int hash = WORKLOAD_INPUT.hashCode();
        String result = WORKLOAD_INPUT + hash;
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += result.charAt(i % result.length());
        }

        // Simulate what @Advice.OnMethodExit injects: pop + record duration
        long exitNanos = System.nanoTime();
        store.onMethodExit("com.ghostprofiler.bench.BenchmarkTarget#compute", exitNanos, false);

        bh.consume(sum);
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
