package com.ghostprofiler.demo.controller;

import com.ghostprofiler.demo.model.Order;
import com.ghostprofiler.demo.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * OrderController — REST API for the demo application order resource.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code GET /orders}      — returns all orders (triggers the N+1 bug)</li>
 *   <li>{@code GET /orders/slow} — artificially slow endpoint for latency testing</li>
 * </ul>
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    /** Constructor injection — explicit dependency, easy to test. */
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Returns all orders. The underlying service has a deliberate N+1 query bug
     * that the GhostProfiler agent should detect.
     *
     * @return list of all orders as JSON
     */
    @GetMapping
    public List<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    /**
     * Artificially slow endpoint — delegates to OrderService which sleeps 200ms.
     * Useful for verifying that the agent correctly measures and reports latency.
     *
     * @return a JSON message confirming the operation completed
     * @throws InterruptedException if sleep is interrupted
     */
    @GetMapping("/slow")
    public Map<String, String> slowEndpoint() throws InterruptedException {
        String result = orderService.processSlowly();
        return Map.of("message", result);
    }
}
