package com.electromart;

public record SalesWriteResult(
    boolean inserted,
    boolean timestampFallbackUsed,
    String source,
    String pickedUpAt
) {
}
