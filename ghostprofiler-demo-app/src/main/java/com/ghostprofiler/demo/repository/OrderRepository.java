package com.ghostprofiler.demo.repository;

import com.ghostprofiler.demo.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * OrderRepository — Spring Data JPA repository for the Order entity.
 *
 * <p>Spring generates the implementation at runtime; no SQL is written here.
 * The findAllIds() method intentionally returns only IDs so that OrderService
 * can then query each order individually — creating the N+1 bug that
 * GhostProfiler detects.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Returns only the primary keys of all orders.
     * This is a single query: SELECT id FROM orders.
     * OrderService then calls findById() for each ID — the N+1 pattern.
     */
    @Query("SELECT o.id FROM Order o")
    List<Long> findAllIds();
}
