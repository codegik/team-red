package com.teamred.datapipeline.processor.aggregation;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashSet;
import java.util.Set;

public class SalesmanAggregate {
    private String salesmanId;
    private String salesmanName;
    private long windowStart;
    private long windowEnd;
    private double totalSales;
    private int transactionCount;
    private Set<String> citiesCovered;

    public SalesmanAggregate() {
        this.citiesCovered = new HashSet<>();
    }

    public String getSalesmanId() {
        return salesmanId;
    }

    public void setSalesmanId(String salesmanId) {
        this.salesmanId = salesmanId;
    }

    public String getSalesmanName() {
        return salesmanName;
    }

    public void setSalesmanName(String salesmanName) {
        this.salesmanName = salesmanName;
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

    public Set<String> getCitiesCovered() {
        return citiesCovered;
    }

    public void setCitiesCovered(Set<String> citiesCovered) {
        this.citiesCovered = citiesCovered;
    }

    @JsonIgnore
    public int getCitiesCount() {
        return citiesCovered.size();
    }
}
