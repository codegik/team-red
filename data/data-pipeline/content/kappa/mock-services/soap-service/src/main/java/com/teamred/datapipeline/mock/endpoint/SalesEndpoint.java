package com.teamred.datapipeline.mock.endpoint;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.Random;
import java.time.Instant;

@Endpoint
public class SalesEndpoint {

    private static final String NAMESPACE_URI = "http://teamred.com/datapipeline/sales";
    private final Random random = new Random();

    private final String[] salesmen = {"John Doe", "Jane Smith", "Bob Johnson", "Alice Williams", "Charlie Brown"};
    private final String[] cities = {"New York", "San Francisco", "Los Angeles", "Chicago", "Boston", "Seattle", "Miami"};
    private final String[] products = {"Laptop", "Mouse", "Keyboard", "Monitor", "Webcam", "Headset"};

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "GetSalesRequest")
    @ResponsePayload
    public Element getSales(@RequestPayload Element request) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element response = doc.createElementNS(NAMESPACE_URI, "GetSalesResponse");

        for (int i = 0; i < 5; i++) {
            Element sale = doc.createElement("sale");

            addElement(doc, sale, "saleId", "soap-" + System.currentTimeMillis() + "-" + i);
            addElement(doc, sale, "timestamp", String.valueOf(Instant.now().toEpochMilli()));

            String salesmanName = salesmen[random.nextInt(salesmen.length)];
            addElement(doc, sale, "salesmanId", "sm-" + salesmanName.replace(" ", "-").toLowerCase());
            addElement(doc, sale, "salesmanName", salesmanName);

            addElement(doc, sale, "customerId", "cust-" + random.nextInt(1000));

            String productName = products[random.nextInt(products.length)];
            addElement(doc, sale, "productId", "prod-" + productName.toLowerCase());
            addElement(doc, sale, "productName", productName);

            int quantity = random.nextInt(10) + 1;
            double unitPrice = 50.0 + random.nextDouble() * 1000.0;
            addElement(doc, sale, "quantity", String.valueOf(quantity));
            addElement(doc, sale, "unitPrice", String.format("%.2f", unitPrice));
            addElement(doc, sale, "totalAmount", String.format("%.2f", quantity * unitPrice));

            addElement(doc, sale, "city", cities[random.nextInt(cities.length)]);
            addElement(doc, sale, "country", "USA");

            response.appendChild(sale);
        }

        doc.appendChild(response);
        return response;
    }

    private void addElement(Document doc, Element parent, String name, String value) {
        Element element = doc.createElement(name);
        element.setTextContent(value);
        parent.appendChild(element);
    }
}
