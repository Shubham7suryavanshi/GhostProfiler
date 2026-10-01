package com.ghostprofiler.demo.service;

import com.ghostprofiler.demo.model.Order;
import com.ghostprofiler.demo.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * OrderService — the business logic layer for the demo app.
 *
 * <p>This class deliberately contains an N+1 query anti-pattern:
 * getAllOrders() first fetches all order IDs with one query, then issues
 * a SEPARATE query per ID to fetch the order details — the classic N+1.
 *
 * <p>The GhostProfiler agent detects this via NPlusOneDetector and flags it
 * in the /metrics JSON output as an nPlusOneWarning.
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;

    /**
     * Constructor injection is preferred over @Autowired field injection.
     * It makes the dependency explicit and testable.
     */
    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Returns all orders with their full details.
     *
     * <p><b>Warning: contains an intentional N+1 query bug.</b>
     * Step 1: one query fetches all IDs (SELECT id FROM orders).
     * Step 2: for EACH id, a separate query fetches the full order
     *         (SELECT * FROM orders WHERE id = ?).
     * This means for 50 orders, 51 queries are issued instead of 1.
     *
     * @return list of all Order entities
     */
    public List<Order> getAllOrders() {
        // Query 1: get all IDs (fine — this is efficient)
        List<Long> ids = orderRepository.findAllIds();

        // Queries 2..N+1: fetch each order individually (this is the bug!)
        // A proper fix would be: return orderRepository.findAll()
        List<Order> orders = new ArrayList<>();
        for (Long id : ids) {
            // Each findById() issues: SELECT * FROM orders WHERE id = ?
            // This is exactly the pattern NPlusOneDetector watches for.
            orderRepository.findById(id).ifPresent(orders::add);
        }
        return orders;
    }

    /**
     * Simulates a slow processing operation (200ms sleep).
     * Used by the slow endpoint to give the agent visible latency to measure.
     *
     * @return a summary string after the simulated delay
     * @throws InterruptedException if the thread is interrupted while sleeping
     */
    public String processSlowly() throws InterruptedException {
        Thread.sleep(200);
        return "Processed " + orderRepository.count() + " orders (simulated 200ms delay)";
    }
}
