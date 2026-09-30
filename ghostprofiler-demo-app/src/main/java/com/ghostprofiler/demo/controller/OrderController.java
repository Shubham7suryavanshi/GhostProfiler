package com.ghostprofiler.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * OrderController — REST API for the demo application's order resource.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code GET /orders}      — returns all orders (triggers the N+1 bug)</li>
 *   <li>{@code GET /orders/slow} — artificially slow endpoint for latency testing</li>
 * </ul>
 *
 * <p>STUB — real service calls added in step 7.
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

    // TODO (step 7): inject OrderService via constructor injection

    /**
     * Returns all orders. The underlying service has a deliberate N+1 query bug
     * that the GhostProfiler agent should detect.
     */
    @GetMapping
    public List<String> getAllOrders() {
        // TODO (step 7): return orderService.getAllOrders()
        return List.of("stub-order-1", "stub-order-2");
    }

    /**
     * Artificially slow endpoint — sleeps 200ms to simulate downstream I/O.
     * Useful for verifying that the agent correctly measures and reports latency.
     */
    @GetMapping("/slow")
    public String slowEndpoint() throws InterruptedException {
        // TODO (step 7): call orderService.processSlowly()
        Thread.sleep(200);
        return "{\"message\": \"slow response stub\"}";
    }
}
