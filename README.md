# GhostProfiler

**GhostProfiler** is a production-quality, low-overhead Java Application Performance Monitoring (APM) agent. 
It operates at the bytecode level, meaning it requires **zero source code changes** to monitor your applications. It attaches to any running JVM application, weaving in non-blocking metrics collection to track performance bottlenecks, GC pressure, memory usage, and hidden N+1 SQL queries.

## Architecture & N+1 Detection
See [architecture.md](docs/architecture.md) for a detailed deep-dive into the bytecode instrumentation and real-time N+1 query detection mechanics.

## Benchmark Results
GhostProfiler is built with extreme performance in mind. Our JMH benchmarks prove the instrumentation adds sub-microsecond overhead (less than 400ns per method). See [results.md](docs/results.md) for details.

## Quick Start

### 1. Build the Agent
```bash
mvn clean install
```
This generates the agent shadow JAR at:
`ghostprofiler-agent/target/ghostprofiler-agent-1.0.0-SNAPSHOT.jar`

### 2. Run your Application with GhostProfiler
Attach the agent to your application at JVM startup by specifying the `-javaagent` flag. Since the agent instruments classes when they are loaded, this is the most reliable way to monitor an application.

```bash
java -Dcom.ghostprofiler.shaded.bytebuddy.experimental=true -javaagent:ghostprofiler-agent.jar="packages=com.yourcompany" -jar your-app.jar
```

*Note: For Java 25, you must provide the `-Dcom.ghostprofiler.shaded.bytebuddy.experimental=true` flag so that the shaded ByteBuddy supports the newer bytecode version.*

**Agent Arguments (Comma-separated):**
- `packages` (Required): The base package(s) you want to instrument. (e.g., `packages=com.example`)
- `port`: The port for the metrics HTTP endpoint (Default: `9090`).
- `n1.threshold`: Occurrences threshold for N+1 warnings (Default: `5`).
- `n1.windowMs`: Sliding window time in ms for N+1 warnings (Default: `1000`).

### 3. View the Dashboard
Simply open `ghostprofiler-dashboard/index.html` in your web browser. 
The dashboard automatically polls the agent's `/metrics` endpoint (at `http://localhost:9090`) to display:
- **Heap and Non-Heap Usage** (with a live chart)
- **Garbage Collection Pauses** (G1, CMS, Parallel, etc.)
- **Slowest Methods** (Average and Max execution time)
- **N+1 SQL Queries** (Live detection of the N+1 problem)

## Demo Application
A Spring Boot demo application is included to demonstrate GhostProfiler in action.

**Start the demo app with the agent:**
```bash
java -Dcom.ghostprofiler.shaded.bytebuddy.experimental=true -javaagent:ghostprofiler-agent/target/ghostprofiler-agent-1.0.0-SNAPSHOT.jar="packages=com.ghostprofiler.demo,n1.threshold=5,n1.windowMs=2000" -jar ghostprofiler-demo-app/target/ghostprofiler-demo-app-1.0.0-SNAPSHOT.jar
```

Hit the endpoints to generate data:
```bash
curl http://localhost:8080/orders       # Triggers N+1 query detection
curl http://localhost:8080/orders/slow  # Triggers long method execution
```
Open the Dashboard to watch the data appear in real-time!
