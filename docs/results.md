# GhostProfiler — Benchmark Results

> **Status:** Placeholder. Real numbers will be recorded after Build Order step 9.

---

## Overhead Measurement Methodology

Two benchmarks were run:

1. **JMH micro-benchmark** (`AgentOverheadBenchmark.java`)  
   Measures the nanosecond cost of the `@Advice` enter/exit pair in isolation.

2. **Demo-app macro-benchmark**  
   Hits `GET /orders` 1000 times with and without `-javaagent`, records `p50`/`p99` latency.

---

## JMH Results (to be filled in — step 9)

```
Benchmark                                  Mode  Cnt    Score   Error  Units
AgentOverheadBenchmark.baseline            avgt   20   XX.XXX ± X.XXX  ns/op
AgentOverheadBenchmark.withInstrumentation avgt   20   XX.XXX ± X.XXX  ns/op
Overhead:  ~X.X%
```

---

## Demo App Endpoint Latency (to be filled in — step 9)

| Scenario | p50 (ms) | p99 (ms) |
|---|---|---|
| No agent | — | — |
| With agent | — | — |
| **Overhead** | — | — |

---

## Target: < 5–10% added latency (NFR1)

If overhead exceeds the target, the following mitigations will be applied:
- Reduce the call-tree depth limit (cap stack depth at N levels)
- Increase sampling rate (record only 1-in-K calls for very hot methods)
- Profile the agent itself with async-profiler to find hotspots

*Last updated: Phase 1 — Scaffold*
