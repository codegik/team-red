package com.electrored.datapipeline.datasource.soap;

import org.springframework.stereotype.Service;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@Endpoint
public class PaymentValidationEndpoint {

    private static final String NAMESPACE_URI = "http://electrored.com/payment-validation";
    private final AtomicInteger paymentCounter = new AtomicInteger(1);
    private final Random random = new Random();

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "GetPaymentConfirmationsRequest")
    @ResponsePayload
    public PaymentConfirmationsResponse getPaymentConfirmations() {
        // Generate mock payment confirmations
        List<PaymentConfirmation> payments = new ArrayList<>();
        int count = random.nextInt(20) + 10; // 10-30 payments

        for (int i = 0; i < count; i++) {
            int paymentNum = paymentCounter.getAndIncrement();
            String paymentId = String.format("PAY-%08d", paymentNum);
            String saleId = String.format("SALE-%08d", paymentNum);

            // 90% confirmed, 10% rejected
            String status = random.nextDouble() < 0.9 ? "CONFIRMED" : "REJECTED";
            double amount = 1000.0 + random.nextDouble() * 20000.0;

            payments.add(new PaymentConfirmation(paymentId, saleId, status, amount));
        }

        return new PaymentConfirmationsResponse(payments.size(), payments);
    }
}

