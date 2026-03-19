package com.electromart;

public record SalesWriteResult(
    boolean inserted,
    boolean timestampFallbackUsed,
    String saleId,
    String source,
    double totalAmount,
    String pickedUpAt
) {
}
