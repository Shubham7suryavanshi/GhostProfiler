package com.ghostprofiler.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Order — the JPA entity representing a customer order.
 *
 * <p>Backed by an H2 in-memory table pre-seeded via data.sql.
 * Used by OrderService to demonstrate the N+1 query pattern that
 * GhostProfiler NPlusOneDetector catches.
 *
 * <p>"Order" is a reserved SQL keyword, so the table is named "orders"
 * explicitly via @Table to avoid conflicts.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String product;

    @Column(nullable = false)
    private double amount;

    // JPA requires a no-arg constructor
    protected Order() {}

    public Order(String customerName, String product, double amount) {
        this.customerName = customerName;
        this.product = product;
        this.amount = amount;
    }

    public Long getId() { return id; }
    public String getCustomerName() { return customerName; }
    public String getProduct() { return product; }
    public double getAmount() { return amount; }

    @Override
    public String toString() {
        return "Order{id=" + id + ", customer='" + customerName + "', product='" + product
               + "', amount=" + amount + '}';
    }
}
