package com.ghostprofiler.demo.repository;

import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * OrderRepository — the data access layer for the demo app.
 *
 * <p>In step 7 this will be a proper Spring Data JPA repository backed by
 * an H2 in-memory database with a pre-seeded dataset (50+ orders) so the
 * N+1 bug in {@link com.ghostprofiler.demo.service.OrderService} is clearly
 * visible in the agent's JDBC interception output.
 *
 * <p>STUB — JPA entity and real queries added in step 7.
 */
@Repository
public class OrderRepository {

    /**
     * Returns all order IDs.
     * In the real implementation this is a single {@code SELECT id FROM orders}.
     */
    public List<Long> findAllIds() {
        // TODO (step 7): implement with JPA
        return List.of(1L, 2L, 3L);
    }

    /**
     * Returns the order details for a single ID.
     * Called in a loop by OrderService, causing the N+1 pattern.
     *
     * @param id  the order primary key
     * @return    order details as a string (will be an entity in step 7)
     */
    public String findById(long id) {
        // TODO (step 7): implement with JPA — SELECT * FROM orders WHERE id = ?
        return "Order#" + id;
    }
}
