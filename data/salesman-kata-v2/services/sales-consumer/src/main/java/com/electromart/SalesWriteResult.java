package com.electromart;

public record SalesWriteResult(
    boolean inserted,
    boolean timestampFallbackUsed,
    String saleId,
    String source,
    String status,
    double totalAmount,
    String pickedUpAt
) {
}
