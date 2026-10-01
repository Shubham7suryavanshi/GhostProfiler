package com.ghostprofiler.agent.server;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import com.ghostprofiler.agent.collector.MetricsStore;
import com.ghostprofiler.agent.jvm.GcMonitor;
import com.ghostprofiler.agent.jvm.MemoryMonitor;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * MetricsHttpServer — an embedded HTTP server that exposes collected metrics
 * as JSON at {@code GET /metrics}.
 *
 * <p>Uses {@code com.sun.net.httpserver.HttpServer} — a lightweight HTTP server
 * built into the JDK since Java 6 — so the agent has no web framework dependency.
 *
 * <p>Endpoints (all produce {@code application/json}):
 * <ul>
 *   <li>{@code GET /metrics}   — full metrics snapshot</li>
 *   <li>{@code GET /health}    — simple liveness check, returns {@code {"status":"ok"}}</li>
 * </ul>
 *
 * <p>STUB — request handler and JSON body wired in step 8.
 */
public class MetricsHttpServer {

    private static final Logger LOG = Logger.getLogger(MetricsHttpServer.class.getName());

    private final int port;
    private final MetricsStore metricsStore;
    private final MemoryMonitor memoryMonitor;
    private final GcMonitor gcMonitor;
    private final MetricsJsonSerializer serializer;

    private HttpServer server;

    /**
     * @param port           TCP port to listen on (configurable via AgentConfig)
     * @param metricsStore   source of method timing and call-tree data
     * @param memoryMonitor  source of heap usage data
     * @param gcMonitor      source of GC pause data
     */
    public MetricsHttpServer(
            int port,
            MetricsStore metricsStore,
            MemoryMonitor memoryMonitor,
            GcMonitor gcMonitor) {
        this.port = port;
        this.metricsStore = metricsStore;
        this.memoryMonitor = memoryMonitor;
        this.gcMonitor = gcMonitor;
        this.serializer = new MetricsJsonSerializer();
    }

    /**
     * Starts the HTTP server on the configured port.
     * The server runs on a single dedicated thread — metrics queries are
     * fast reads of in-memory data and don't need a thread pool.
     *
     * @throws IOException if the port is already in use
     */
    /**
     * Starts the HTTP server on the configured port.
     *
     * <p>The server is given a single-thread executor. All metrics reads are
     * fast in-memory operations, so one thread is sufficient and avoids the
     * overhead of a thread pool.
     *
     * @throws IOException if the port is already in use
     */
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // /metrics — full JSON snapshot of all collected metrics
        server.createContext("/metrics", exchange -> {
            try {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                    return;
                }
                String json = serializer.serialize(metricsStore, memoryMonitor, gcMonitor);
                sendJson(exchange, 200, json);
            } catch (Throwable t) {
                // Never crash the server thread (NFR2)
                LOG.warning("[GhostProfiler] /metrics handler error: " + t.getMessage());
                sendJson(exchange, 500, "{\"error\":\"internal error\"}");
            }
        });

        // /health — simple liveness probe used by the dashboard
        server.createContext("/health", exchange -> {
            try {
                sendJson(exchange, 200, "{\"status\":\"ok\"}");
            } catch (Throwable t) {
                LOG.warning("[GhostProfiler] /health handler error: " + t.getMessage());
            }
        });

        // Use a single-thread executor — metrics reads are cheap in-memory ops
        server.setExecutor(Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "GhostProfiler-HttpServer");
            t.setDaemon(true);
            return t;
        }));
        server.start();
        LOG.info("[GhostProfiler] MetricsHttpServer started on port " + port +
                 ". Endpoints: GET /metrics  GET /health");
    }

    /** Shuts down the HTTP server gracefully. */
    public void stop() {
        if (server != null) {
            server.stop(1); // allow 1 second for in-flight requests to complete
            LOG.info("[GhostProfiler] MetricsHttpServer stopped.");
        }
    }

    // ── Private handler stubs ─────────────────────────────────────────────────

    /**
     * Sends a JSON response with the correct Content-Type header.
     *
     * @param exchange   the HTTP exchange from the server
     * @param statusCode HTTP status code (200, 404, 500, …)
     * @param json       the JSON string to send as the response body
     */
    private void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        // Allow browser dashboard to poll without CORS errors
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
