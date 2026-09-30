# 👻 GhostProfiler

> A production-quality Java Application Performance Monitoring (APM) agent.  
> Attaches to **any** running JVM via `-javaagent` — **zero source-code changes** required.

---

## What it does

| Feature | Detail |
|---|---|
| **Method timing** | Instruments entry/exit on configurable packages; builds call trees |
| **Memory metrics** | Live heap and non-heap usage via `MemoryMXBean` |
| **GC metrics** | Collection count and pause time via `GarbageCollectorMXBean` |
| **JDBC interception** | Captures SQL query text and duration |
| **N+1 detection** | Flags repeated identical queries within a sliding time window |
| **HTTP JSON API** | `GET /metrics` on a configurable port (default 9090) |
| **Dashboard** | Plain HTML + Chart.js page that polls `/metrics` in real time |

---

## Project Structure

```
ghostprofiler/
├── ghostprofiler-agent/       ← The Java agent (fat jar)
├── ghostprofiler-demo-app/    ← Spring Boot victim app with deliberate N+1 bug
├── ghostprofiler-dashboard/   ← index.html polling dashboard (no build step)
├── benchmarks/                ← JMH overhead benchmarks
└── docs/
    ├── architecture.md        ← Design decisions (ByteBuddy, premain, threading)
    └── results.md             ← Actual JMH numbers
```

---

## Prerequisites

- Java 17+
- Maven 3.8+

---

## Build

```bash
# From the project root — builds all modules
mvn clean install

# The agent fat jar ends up at:
# ghostprofiler-agent/target/ghostprofiler-agent-1.0.0-SNAPSHOT.jar
```

---

## Attach to Any Java Application

```bash
java \
  -javaagent:ghostprofiler-agent/target/ghostprofiler-agent-1.0.0-SNAPSHOT.jar \
  -Dghost.packages=com.myapp \
  -Dghost.port=9090 \
  -Dghost.n1.threshold=5 \
  -Dghost.n1.windowMs=1000 \
  -jar myapp.jar
```

### Configuration properties

| Property | Default | Description |
|---|---|---|
| `ghost.packages` | *(empty)* | Comma-separated package prefixes to instrument |
| `ghost.port` | `9090` | HTTP server port for `/metrics` |
| `ghost.samplingRate` | `1.0` | Fraction of calls to record (0.0–1.0) |
| `ghost.n1.threshold` | `5` | Repeated query count that triggers N+1 warning |
| `ghost.n1.windowMs` | `1000` | Sliding window duration in ms for N+1 detection |

---

## Run the Demo App

```bash
# Start the Spring Boot demo app with the agent attached
java \
  -javaagent:ghostprofiler-agent/target/ghostprofiler-agent-1.0.0-SNAPSHOT.jar \
  -Dghost.packages=com.ghostprofiler.demo \
  -Dghost.port=9090 \
  -jar ghostprofiler-demo-app/target/ghostprofiler-demo-app-1.0.0-SNAPSHOT.jar
```

Then trigger the endpoints:

```bash
# N+1 bug endpoint (agent will flag repeated SQL)
curl http://localhost:8080/orders

# Artificially slow endpoint
curl http://localhost:8080/orders/slow

# View collected metrics as JSON
curl http://localhost:9090/metrics | python -m json.tool
```

Open the dashboard:

```bash
# No build step — just open the file in a browser
open ghostprofiler-dashboard/index.html
# or on Windows:
start ghostprofiler-dashboard/index.html
```

---

## Run Benchmarks

```bash
mvn clean package -pl benchmarks
java -jar benchmarks/target/benchmarks.jar AgentOverheadBenchmark
```

See `docs/results.md` for recorded numbers.

---

## Sample `/metrics` Output

```json
{
  "timestamp": 1722000000000,
  "memory": {
    "heapUsedMb": 128.5,
    "heapMaxMb": 512.0,
    "nonHeapUsedMb": 64.2
  },
  "gc": [
    { "collector": "G1 Young Generation", "count": 12, "timeMs": 345 }
  ],
  "methods": {
    "com.ghostprofiler.demo.service.OrderService#getAllOrders": {
      "callCount": 42,
      "avgDurationMs": 210.3,
      "maxDurationMs": 412.0
    }
  },
  "nPlusOneWarnings": [
    {
      "sql": "select * from orders where id = ?",
      "occurrences": 50,
      "detectedAt": 1722000001000
    }
  ]
}
```

---

## Architecture

See [`docs/architecture.md`](docs/architecture.md) for detailed design decisions:
- Why ByteBuddy over raw ASM or Javassist
- `premain` vs `agentmain` trade-off
- Infinite instrumentation loop prevention
- Thread safety model
- Overhead measurement strategy

---

## Build Status

| Phase | Status |
|---|---|
| 1 — Scaffold | ✅ Complete |
| 2 — AgentMain premain | 🔲 Pending |
| 3 — MethodTimingTransformer | 🔲 Pending |
| 4 — CallTree + MetricsStore | 🔲 Pending |
| 5 — MemoryMonitor + GcMonitor | 🔲 Pending |
| 6 — JdbcInterceptor + N+1 | 🔲 Pending |
| 7 — Demo App | 🔲 Pending |
| 8 — MetricsHttpServer | 🔲 Pending |
| 9 — Benchmarks | 🔲 Pending |
| 10 — Dashboard | 🔲 Pending |
| 11 — Architecture docs | 🔲 Pending |
| 12 — README polish | 🔲 Pending |
