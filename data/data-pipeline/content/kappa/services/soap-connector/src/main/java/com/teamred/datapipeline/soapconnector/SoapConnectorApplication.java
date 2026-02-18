package com.teamred.datapipeline.soapconnector;

import com.teamred.datapipeline.lineage.LineageContext;
import com.teamred.datapipeline.model.SalesEventDto;
import com.teamred.datapipeline.observability.MetricsRegistry;
import com.teamred.datapipeline.serdes.JsonSerializer;
import io.micrometer.core.instrument.Counter;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import jakarta.xml.soap.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SoapConnectorApplication {

    private static final Logger logger = LoggerFactory.getLogger(SoapConnectorApplication.class);
    private static final String TOPIC = "sales.raw.soap";
    private static Counter recordsProcessed;
    private static final Set<String> processedIds = new HashSet<>();

    public static void main(String[] args) {
        recordsProcessed = MetricsRegistry.counter("soap_connector_records_processed");

        String kafkaBootstrapServers = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
        String soapEndpointUrl = System.getenv().getOrDefault("SOAP_ENDPOINT_URL", "http://localhost:8080/ws/sales");
        int pollIntervalSeconds = Integer.parseInt(System.getenv().getOrDefault("POLL_INTERVAL_SECONDS", "30"));

        Properties kafkaProps = new Properties();
        kafkaProps.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        kafkaProps.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        kafkaProps.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class.getName());
        kafkaProps.setProperty(ProducerConfig.ACKS_CONFIG, "all");
        kafkaProps.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");

        KafkaProducer<String, SalesEventDto> producer = new KafkaProducer<>(kafkaProps);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        logger.info("SOAP Connector started, polling every {} seconds", pollIntervalSeconds);

        scheduler.scheduleAtFixedRate(() -> {
            try {
                pollSoapService(soapEndpointUrl, producer);
            } catch (Exception e) {
                logger.error("Error polling SOAP service", e);
            }
        }, 0, pollIntervalSeconds, TimeUnit.SECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down SOAP Connector");
            scheduler.shutdown();
            producer.close();
        }));
    }

    private static void pollSoapService(String endpointUrl, KafkaProducer<String, SalesEventDto> producer) throws Exception {
        SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
        SOAPConnection soapConnection = soapConnectionFactory.createConnection();

        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPPart soapPart = soapMessage.getSOAPPart();

        String serverURI = "http://teamred.com/datapipeline/sales";

        SOAPEnvelope envelope = soapPart.getEnvelope();
        envelope.addNamespaceDeclaration("sal", serverURI);

        SOAPBody soapBody = envelope.getBody();
        SOAPElement getSalesRequest = soapBody.addChildElement("GetSalesRequest", "sal");
        SOAPElement since = getSalesRequest.addChildElement("since", "sal");
        since.addTextNode("0");

        soapMessage.saveChanges();

        SOAPMessage soapResponse = soapConnection.call(soapMessage, endpointUrl);

        Document doc = soapResponse.getSOAPBody().extractContentAsDocument();
        NodeList sales = doc.getElementsByTagName("sale");

        logger.info("Received {} sales from SOAP service", sales.getLength());

        for (int i = 0; i < sales.getLength(); i++) {
            Element sale = (Element) sales.item(i);
            SalesEventDto event = parseSoapSale(sale);

            if (!processedIds.contains(event.getSaleId())) {
                sendEvent(event, producer);
                processedIds.add(event.getSaleId());
            }
        }

        soapConnection.close();
    }

    private static SalesEventDto parseSoapSale(Element sale) {
        SalesEventDto event = new SalesEventDto();
        event.setSaleId(getElementText(sale, "saleId"));
        event.setTimestamp(Long.parseLong(getElementText(sale, "timestamp")));
        event.setSalesmanId(getElementText(sale, "salesmanId"));
        event.setSalesmanName(getElementText(sale, "salesmanName"));
        event.setCustomerId(getElementText(sale, "customerId"));
        event.setProductId(getElementText(sale, "productId"));
        event.setProductName(getElementText(sale, "productName"));
        event.setQuantity(Integer.parseInt(getElementText(sale, "quantity")));
        event.setUnitPrice(Double.parseDouble(getElementText(sale, "unitPrice")));
        event.setTotalAmount(Double.parseDouble(getElementText(sale, "totalAmount")));
        event.setCity(getElementText(sale, "city"));
        event.setCountry(getElementText(sale, "country"));
        event.setSourceSystem("SOAP");
        event.setIngestionTimestamp(System.currentTimeMillis());
        event.setLineageId(LineageContext.generateLineageId());
        return event;
    }

    private static String getElementText(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return "";
    }

    private static void sendEvent(SalesEventDto event, KafkaProducer<String, SalesEventDto> producer) {
        RecordHeaders headers = new RecordHeaders();
        LineageContext.addLineageHeaders(headers, event.getLineageId(), "SOAP", event.getTimestamp());

        ProducerRecord<String, SalesEventDto> record = new ProducerRecord<>(
                TOPIC,
                null,
                event.getSaleId(),
                event,
                headers
        );

        producer.send(record);
        recordsProcessed.increment();
        logger.info("Sent sale event: {}", event.getSaleId());
    }
}
