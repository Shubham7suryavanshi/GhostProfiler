# GhostProfiler — Architecture Decisions

> **Status:** Living document. Updated at the end of each Build Order phase.

---

## 1. Why ByteBuddy over Raw ASM or Javassist

| Criterion | Raw ASM | Javassist | **ByteBuddy** |
|---|---|---|---|
| API level | Visitor-pattern bytecode | Source-level string templates | Fluent, type-safe DSL |
| Type safety | None — wrong opcode = corrupt class | Low — string splicing | High — compile-time checked |
| Java version support | Manual — every new bytecode version needs updates | Lags behind | Maintained by project lead (Rafael Winterhalter), tracks every Java release |
| ClassLoader handling | Manual | Manual | Automatic (handles bootstrap, ext, app loaders) |
| Advice inlining | Manual | No | Yes — `@Advice` copies bytecode at the call site, zero reflection overhead |
| Documentation | JVM spec + OSS reading | Sparse | Comprehensive + active Stack Overflow presence |

**Decision:** ByteBuddy's `@Advice` mechanism is the key reason for this choice.
`@Advice.OnMethodEnter` / `@Advice.OnMethodExit` work by **copying the static method body
directly into the target method's bytecode** at transformation time. The result is:

- No method call overhead (no `invokevirtual` to an interceptor object)
- No reflection
- No proxy objects on the heap

This is the correct design choice for an APM agent where overhead budget is 5–10% (NFR1).

---

## 2. `premain` vs `agentmain` Trade-off

The JVM spec defines two agent attach modes:

| Mode | Trigger | When classes are instrumented |
|---|---|---|
| `premain(String, Instrumentation)` | `-javaagent` at JVM startup | Before `main()` runs — all classes not yet loaded |
| `agentmain(String, Instrumentation)` | Attach API at runtime | After JVM is running — classes may already be loaded |

**Why both are implemented:**

- `premain` is the primary mode (FR1 requires `-javaagent` support). Because the agent
  attaches before any application class is loaded, ByteBuddy can intercept every class
  as it is loaded — no retransformation needed.

- `agentmain` is provided as a convenience for production attach-after-startup scenarios.
  It requires `Can-Retransform-Classes: true` in the MANIFEST (already set) and calls
  `Instrumentation.retransformClasses()` to re-instrument already-loaded classes.

**Infinite instrumentation loop prevention:** ByteBuddy automatically ignores the
agent's own classes (the `com.ghostprofiler` shaded prefix). We additionally call
`.ignore(nameStartsWith("com.ghostprofiler"))` on the `AgentBuilder` so the agent
never attempts to instrument itself.

---

## 3. Class-Loading Safety

The agent jar is a fat/shaded jar with all dependencies relocated under
`com.ghostprofiler.shaded.*`. This prevents conflicts when the target application
bundles its own version of ByteBuddy or Jackson.

The agent classes are loaded by the **bootstrap classloader** (via
`Instrumentation.appendToBootstrapClassLoaderSearch`), which sits above the
application classloader. This means:

- Application code can see agent classes
- Agent classes cannot accidentally import application classes (preventing cycles)

---

## 4. Overhead Measurement Strategy (NFR1)

Two-pronged approach:

1. **Micro-benchmark (JMH):** `AgentOverheadBenchmark` measures the raw nanosecond cost
   of the `@Advice` enter/exit pair on a synthetic method. This isolates the instrumentation
   overhead from application logic.

2. **Macro-benchmark (demo app):** The Spring Boot demo app's `/orders` endpoint is hit
   under load with and without the `-javaagent` flag. Wall-clock `p99` latency is compared.

Results recorded in `docs/results.md` after step 9.

---

## 5. N+1 Detection Algorithm

_(Filled in during step 6)_

---

## 6. Thread Safety Model

| Component | Concurrency mechanism |
|---|---|
| `MetricsStore.methodStats` | `ConcurrentHashMap` + `AtomicLong.addAndGet` |
| `MetricsStore.completedTrees` | `CopyOnWriteArrayList` (reads never block writers) |
| `MetricsStore.callStack` | `ThreadLocal<Deque>` (zero contention — per-thread) |
| `MemoryMonitor` / `GcMonitor` | `volatile` long fields (single-writer daemon thread) |
| `MetricsHttpServer` | Single reader thread; reads only immutable snapshots |

---

*Last updated: Phase 1 — Scaffold*
