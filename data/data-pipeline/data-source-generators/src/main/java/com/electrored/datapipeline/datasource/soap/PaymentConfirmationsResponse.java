package com.electrored.datapipeline.datasource.soap;

import jakarta.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement(name = "GetPaymentConfirmationsResponse", namespace = "http://electrored.com/payment-validation")
@XmlAccessorType(XmlAccessType.FIELD)
public class PaymentConfirmationsResponse {

    @XmlElement(required = true)
    private int totalRecords;

    @XmlElementWrapper(name = "confirmations")
    @XmlElement(name = "payment")
    private List<PaymentConfirmation> payments;

    public PaymentConfirmationsResponse() {
    }

    public PaymentConfirmationsResponse(int totalRecords, List<PaymentConfirmation> payments) {
        this.totalRecords = totalRecords;
        this.payments = payments;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public List<PaymentConfirmation> getPayments() {
        return payments;
    }

    public void setPayments(List<PaymentConfirmation> payments) {
        this.payments = payments;
    }
}

@XmlRootElement(name = "payment")
@XmlAccessorType(XmlAccessType.FIELD)
class PaymentConfirmation {

    private String paymentId;
    private String saleId;
    private String status;
    private double amount;
    private String currency;
    private String paymentMethod;
    private String bankReference;
    private String confirmationDate;

    public PaymentConfirmation() {
    }

    public PaymentConfirmation(String paymentId, String saleId, String status, double amount) {
        this.paymentId = paymentId;
        this.saleId = saleId;
        this.status = status;
        this.amount = amount;
        this.currency = "BRL";
        this.paymentMethod = "CREDIT_CARD";
        this.bankReference = "BNK-REF-" + System.currentTimeMillis();
        this.confirmationDate = java.time.LocalDateTime.now().toString();
    }

    // Getters and Setters

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getSaleId() {
        return saleId;
    }

    public void setSaleId(String saleId) {
        this.saleId = saleId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getBankReference() {
        return bankReference;
    }

    public void setBankReference(String bankReference) {
        this.bankReference = bankReference;
    }

    public String getConfirmationDate() {
        return confirmationDate;
    }

    public void setConfirmationDate(String confirmationDate) {
        this.confirmationDate = confirmationDate;
    }
}

