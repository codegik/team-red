package com.teamred.datapipeline.processor.aggregation;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;
import java.util.Map;

public class CitySalesAggregate {
    private String city;
    private long windowStart;
    private long windowEnd;
    private double totalSales;
    private int transactionCount;
    private Map<String, Double> productSales;

    public CitySalesAggregate() {
        this.productSales = new HashMap<>();
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public long getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(long windowStart) {
        this.windowStart = windowStart;
    }

    public long getWindowEnd() {
        return windowEnd;
    }

    public void setWindowEnd(long windowEnd) {
        this.windowEnd = windowEnd;
    }

    public double getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(double totalSales) {
        this.totalSales = totalSales;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }

    public Map<String, Double> getProductSales() {
        return productSales;
    }

    public void setProductSales(Map<String, Double> productSales) {
        this.productSales = productSales;
    }

    @JsonIgnore
    public String getTopProduct() {
        return productSales.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    @JsonIgnore
    public Double getTopProductSales() {
        return productSales.values().stream()
                .max(Double::compareTo)
                .orElse(0.0);
    }
}
