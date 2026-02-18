package com.teamred.datapipeline.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SalesEventDto {

    @JsonProperty("sale_id")
    private String saleId;

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("salesman_id")
    private String salesmanId;

    @JsonProperty("salesman_name")
    private String salesmanName;

    @JsonProperty("customer_id")
    private String customerId;

    @JsonProperty("product_id")
    private String productId;

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("quantity")
    private int quantity;

    @JsonProperty("unit_price")
    private double unitPrice;

    @JsonProperty("total_amount")
    private double totalAmount;

    @JsonProperty("city")
    private String city;

    @JsonProperty("country")
    private String country;

    @JsonProperty("source_system")
    private String sourceSystem;

    @JsonProperty("ingestion_timestamp")
    private long ingestionTimestamp;

    @JsonProperty("lineage_id")
    private String lineageId;

    public SalesEventDto() {
    }

    public String getSaleId() {
        return saleId;
    }

    public void setSaleId(String saleId) {
        this.saleId = saleId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
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

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public long getIngestionTimestamp() {
        return ingestionTimestamp;
    }

    public void setIngestionTimestamp(long ingestionTimestamp) {
        this.ingestionTimestamp = ingestionTimestamp;
    }

    public String getLineageId() {
        return lineageId;
    }

    public void setLineageId(String lineageId) {
        this.lineageId = lineageId;
    }
}
