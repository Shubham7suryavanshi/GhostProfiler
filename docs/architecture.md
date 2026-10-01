# GhostProfiler Architecture & N+1 Detection

GhostProfiler is a Java Application Performance Monitoring (APM) tool implemented as a `-javaagent`. It intercepts application execution at the JVM level to collect runtime metrics with extremely low overhead.

## High-Level Architecture

GhostProfiler is composed of several independent subsystems working in tandem:

1.  **Agent Initialization (`AgentMain` & `AgentConfig`)**
    When the JVM starts, it invokes the agent's `premain` method. The agent parses its configuration (from the `-javaagent` argument string and system properties) and wires together the monitoring subsystems.

2.  **Bytecode Instrumentation (`MethodTimingTransformer`)**
    Using the **ByteBuddy** library, GhostProfiler registers an `AgentBuilder` that intercepts class loading. 
    It applies two transformations:
    *   **Method Interceptor**: Matches all non-abstract methods in configured packages (e.g. `com.example.*`) and injects `@Advice` to track method entry and exit.
    *   **JDBC Interceptor**: Matches any class implementing `java.sql.Statement` (or extending it) and intercepts `execute*` methods taking a SQL string as an argument.

3.  **Metrics Storage (`MetricsStore` & `MethodStats`)**
    The heart of the agent is a highly concurrent data structure.
    *   `ThreadLocal` `ArrayDeque` tracks the call stack of instrumented methods per thread.
    *   `ConcurrentHashMap` stores aggregated statistics per method signature.
    *   `LongAdder` is used to accumulate execution time without causing contention between threads.

4.  **JVM Monitoring (`MemoryMonitor` & `GcMonitor`)**
    Background daemon threads use `java.lang.management` MXBeans to periodically poll heap usage, non-heap usage, and Garbage Collection pause times.

5.  **Metrics Export (`MetricsHttpServer`)**
    An embedded, lightweight `com.sun.net.httpserver` serves the aggregated metrics as a JSON payload at `http://localhost:9090/metrics`.

## N+1 Query Detection Mechanics

The N+1 query problem occurs when an application executes a single query to fetch a list of entities (the "1"), and then sequentially executes another query for *each* entity in the list (the "N") to fetch related data.

GhostProfiler detects this pattern purely at the JDBC layer, without any knowledge of the ORM (Hibernate, JPA, etc.) being used.

### 1. Interception
ByteBuddy injects `JdbcInterceptor` into the `execute` methods of `java.sql.Statement`. When a query is executed, the SQL string is passed to `MetricsStore.recordQuery()`.

### 2. SQL Normalization
The `NPlusOneDetector` normalizes the SQL string to group identical structural queries together.
*   It replaces string literals (e.g., `'Alice'`) with `?`.
*   It replaces numeric literals (e.g., `123`, `45.6`) with `?`.
*   It collapses multiple whitespace characters into a single space and trims the string.
*   It converts the SQL to lowercase.

For example, `SELECT * FROM users WHERE id = 1` and `SELECT * FROM users WHERE id = 2` both normalize to `select * from users where id = ?`.

### 3. Sliding Window Tracking
The detector maintains a `ConcurrentHashMap<String, List<Long>>`, where the key is the normalized SQL and the value is a list of timestamps when the query was executed.

When a query is recorded:
1.  Its timestamp is added to the list for that normalized SQL.
2.  The sliding window logic removes any timestamps older than the configured `windowMs` (e.g., 1000ms).
3.  If the number of remaining timestamps in the list meets or exceeds the `threshold` (e.g., 5), an **N+1 Warning** is emitted and the window for that query is reset (to prevent warning spam).

This approach reliably catches N+1 bursts in real-time, regardless of the thread or transaction context they originate from, while automatically cleaning up old data to prevent memory leaks.
