package com.teamred.datapipeline.mock.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Random;

@RestController
public class SalesController {

    private final Random random = new Random();
    private final String[] salesmen = {"John Doe", "Jane Smith", "Bob Johnson", "Alice Williams", "Charlie Brown"};
    private final String[] cities = {"New York", "San Francisco", "Los Angeles", "Chicago", "Boston", "Seattle", "Miami"};
    private final String[] products = {"Laptop", "Mouse", "Keyboard", "Monitor", "Webcam", "Headset"};

    @PostMapping(value = "/ws/sales", produces = MediaType.TEXT_XML_VALUE)
    public String getSales() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">");
        xml.append("<soap:Body>");
        xml.append("<GetSalesResponse xmlns=\"http://teamred.com/datapipeline/sales\">");

        for (int i = 0; i < 5; i++) {
            String salesmanName = salesmen[random.nextInt(salesmen.length)];
            String productName = products[random.nextInt(products.length)];
            String city = cities[random.nextInt(cities.length)];
            int quantity = random.nextInt(10) + 1;
            double unitPrice = 50.0 + random.nextDouble() * 1000.0;

            xml.append("<sale>");
            xml.append(String.format("<saleId>soap-%d-%d</saleId>", System.currentTimeMillis(), i));
            xml.append(String.format("<timestamp>%d</timestamp>", Instant.now().toEpochMilli()));
            xml.append(String.format("<salesmanId>sm-%s</salesmanId>", salesmanName.replace(" ", "-").toLowerCase()));
            xml.append(String.format("<salesmanName>%s</salesmanName>", salesmanName));
            xml.append(String.format("<customerId>cust-%d</customerId>", random.nextInt(1000)));
            xml.append(String.format("<productId>prod-%s</productId>", productName.toLowerCase()));
            xml.append(String.format("<productName>%s</productName>", productName));
            xml.append(String.format("<quantity>%d</quantity>", quantity));
            xml.append(String.format("<unitPrice>%.2f</unitPrice>", unitPrice));
            xml.append(String.format("<totalAmount>%.2f</totalAmount>", quantity * unitPrice));
            xml.append(String.format("<city>%s</city>", city));
            xml.append("<country>USA</country>");
            xml.append("</sale>");
        }

        xml.append("</GetSalesResponse>");
        xml.append("</soap:Body>");
        xml.append("</soap:Envelope>");

        return xml.toString();
    }
}
