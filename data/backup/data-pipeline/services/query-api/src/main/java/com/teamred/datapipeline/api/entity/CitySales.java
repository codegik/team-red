package com.teamred.datapipeline.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "top_sales_by_city")
public class CitySales {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "window_start", nullable = false)
    private Instant windowStart;

    @Column(name = "window_end", nullable = false)
    private Instant windowEnd;

    @Column(name = "total_sales", nullable = false)
    private Double totalSales;

    @Column(name = "transaction_count", nullable = false)
    private Integer transactionCount;

    @Column(name = "top_product")
    private String topProduct;

    @Column(name = "top_product_sales")
    private Double topProductSales;

    @Column(name = "created_at")
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Instant getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(Instant windowStart) {
        this.windowStart = windowStart;
    }

    public Instant getWindowEnd() {
        return windowEnd;
    }

    public void setWindowEnd(Instant windowEnd) {
        this.windowEnd = windowEnd;
    }

    public Double getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(Double totalSales) {
        this.totalSales = totalSales;
    }

    public Integer getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(Integer transactionCount) {
        this.transactionCount = transactionCount;
    }

    public String getTopProduct() {
        return topProduct;
    }

    public void setTopProduct(String topProduct) {
        this.topProduct = topProduct;
    }

    public Double getTopProductSales() {
        return topProductSales;
    }

    public void setTopProductSales(Double topProductSales) {
        this.topProductSales = topProductSales;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
