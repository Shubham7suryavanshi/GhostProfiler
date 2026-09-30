package com.ghostprofiler.agent.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * AgentConfig — all runtime configuration for GhostProfiler.
 *
 * <p>Configuration is read from two sources, in priority order:
 * <ol>
 *   <li>JVM system properties ({@code -Dghost.*})</li>
 *   <li>The agent argument string passed via {@code -javaagent:agent.jar=key=val,key=val}</li>
 * </ol>
 *
 * <p>This centralised config object satisfies NFR3 (all config externalised,
 * nothing hardcoded). Default values are safe for a first run.
 *
 * <p>STUB — parsing logic will be filled in during step 2.
 */
public class AgentConfig {

    private static final Logger LOG = Logger.getLogger(AgentConfig.class.getName());

    // ── HTTP server ────────────────────────────────────────────────────────────

    /** Port the embedded HTTP server listens on. Default: 9090 */
    private int httpPort = 9090;

    // ── Instrumentation filter ─────────────────────────────────────────────────

    /**
     * Package prefixes to instrument. Empty set means "instrument nothing" —
     * the agent is safe by default and only instruments what you explicitly opt in.
     * Example: {"com.example", "com.acme"}
     */
    private Set<String> includedPackages = new HashSet<>();

    // ── Sampling ───────────────────────────────────────────────────────────────

    /**
     * Fraction of method calls to actually record, 0.0–1.0.
     * 1.0 = record every call, 0.1 = record ~10% (for very hot paths).
     */
    private double samplingRate = 1.0;

    // ── N+1 detection ──────────────────────────────────────────────────────────

    /**
     * Number of identical SQL queries within the detection window that
     * triggers an N+1 warning. Default: 5.
     */
    private int nPlusOneThreshold = 5;

    /**
     * Time window (in milliseconds) used when counting repeated queries.
     * Default: 1000 ms (1 second).
     */
    private long nPlusOneWindowMs = 1000L;

    // ── Constructor ────────────────────────────────────────────────────────────

    public AgentConfig(String agentArgs) {
        // Parse agentArgs string (e.g. "port=9090,packages=com.example")
        if (agentArgs != null && !agentArgs.trim().isEmpty()) {
            for (String pair : agentArgs.split(",")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    applyProperty(kv[0].trim(), kv[1].trim());
                }
            }
        }

        // System properties override agentArgs
        applyProperty("port", System.getProperty("ghost.port"));
        applyProperty("packages", System.getProperty("ghost.packages"));
        applyProperty("samplingRate", System.getProperty("ghost.samplingRate"));
        applyProperty("n1.threshold", System.getProperty("ghost.n1.threshold"));
        applyProperty("n1.windowMs", System.getProperty("ghost.n1.windowMs"));

        LOG.info("[GhostProfiler] Config initialized: " + this.toString());
    }

    private void applyProperty(String key, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        try {
            switch (key) {
                case "port":
                    this.httpPort = Integer.parseInt(value);
                    break;
                case "packages":
                    this.includedPackages.addAll(Arrays.asList(value.split(":")));
                    break;
                case "samplingRate":
                    this.samplingRate = Double.parseDouble(value);
                    break;
                case "n1.threshold":
                    this.nPlusOneThreshold = Integer.parseInt(value);
                    break;
                case "n1.windowMs":
                    this.nPlusOneWindowMs = Long.parseLong(value);
                    break;
            }
        } catch (NumberFormatException e) {
            LOG.warning("[GhostProfiler] Invalid numeric value for " + key + ": " + value);
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public int getHttpPort() { return httpPort; }

    public Set<String> getIncludedPackages() { return includedPackages; }

    public double getSamplingRate() { return samplingRate; }

    public int getNPlusOneThreshold() { return nPlusOneThreshold; }

    public long getNPlusOneWindowMs() { return nPlusOneWindowMs; }

    @Override
    public String toString() {
        return "AgentConfig{" +
               "httpPort=" + httpPort +
               ", includedPackages=" + includedPackages +
               ", samplingRate=" + samplingRate +
               ", nPlusOneThreshold=" + nPlusOneThreshold +
               ", nPlusOneWindowMs=" + nPlusOneWindowMs +
               '}';
    }
}
