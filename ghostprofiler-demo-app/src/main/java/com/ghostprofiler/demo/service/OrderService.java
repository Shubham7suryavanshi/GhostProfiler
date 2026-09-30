package com.ghostprofiler.demo.service;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * OrderService — the business logic layer for the demo app.
 *
 * <p>This class deliberately contains an N+1 query anti-pattern:
 * {@link #getAllOrders()} first fetches all order IDs, then issues a
 * separate query for each order's details — classic N+1.
 *
 * <p>The GhostProfiler agent should detect this pattern via
 * {@code NPlusOneDetector} and flag it in the /metrics output.
 *
 * <p>STUB — deliberate N+1 bug implemented in step 7.
 */
@Service
public class OrderService {

    // TODO (step 7): inject OrderRepository via constructor

    /**
     * Returns all orders.
     *
     * <p><b>Warning: contains an intentional N+1 query bug.</b>
     * For each order in the list, a separate DB query is issued to fetch
     * the order's line items — this is the pattern GhostProfiler detects.
     *
     * @return list of order summary strings (stub implementation)
     */
    public List<String> getAllOrders() {
        // TODO (step 7): implement deliberate N+1: for each orderId, call repo.findById()
        return List.of("stub");
    }

    /**
     * Simulates a slow processing operation (200ms sleep).
     * Used by the slow endpoint to give the agent visible latency to measure.
     */
    public String processSlowly() throws InterruptedException {
        // TODO (step 7): implement with Thread.sleep(200) + real work
        Thread.sleep(200);
        return "done";
    }
}
