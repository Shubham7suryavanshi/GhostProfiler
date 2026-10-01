# GhostProfiler JMH Benchmark Results

**Date**: 2026-10-01
**OS/JVM**: Windows 11 / JDK 25

We ran `AgentOverheadBenchmark` using JMH to measure the exact per-method overhead introduced by GhostProfiler's `MetricsStore` tracking stack. 

## Benchmark Details
*   **Mode**: AverageTime (ns/op)
*   **Workload**: String concatenation and hashing (deterministic)
*   **baseline**: Raw performance without any MetricsStore API calls.
*   **withInstrumentation**: The exact same workload, wrapped in `MetricsStore.onMethodEnter()` and `MetricsStore.onMethodExit()` calls to measure the direct overhead of the thread-local call stack and concurrent hash map aggregations.

## Results

```text
Benchmark                                   Mode  Cnt    Score    Error  Units
AgentOverheadBenchmark.baseline             avgt   20   30.733 ±  6.012  ns/op
AgentOverheadBenchmark.withInstrumentation  avgt   20  356.992 ± 60.665  ns/op
```

## Analysis

The baseline execution takes `~31ns`.
When wrapped with our instrumentation tracking (the exact code ByteBuddy's `@Advice` inlines into target methods), the execution takes `~357ns`.

**Overhead per method call**: `~326 ns`

Adding just ~326 nanoseconds of overhead per method call is well within the acceptable boundaries for a production-grade APM agent. This is achieved by:
1.  Using a lightweight ThreadLocal `ArrayDeque` for call stack tracking.
2.  Delaying heavy synchronization until tree completion.
3.  Using `ConcurrentHashMap` combined with thread-safe `LongAdder` (inside MethodStats) for highly concurrent metric aggregation without locks.
