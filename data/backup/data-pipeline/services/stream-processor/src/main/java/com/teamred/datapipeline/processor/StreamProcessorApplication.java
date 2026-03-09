package com.teamred.datapipeline.processor;

import com.teamred.datapipeline.processor.sink.TimescaleSink;
import com.teamred.datapipeline.processor.topology.CitySalesTopology;
import com.teamred.datapipeline.processor.topology.SalesmanTopology;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class StreamProcessorApplication {

    private static final Logger logger = LoggerFactory.getLogger(StreamProcessorApplication.class);

    public static void main(String[] args) {
        String kafkaBootstrapServers = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
        String applicationId = System.getenv().getOrDefault("APPLICATION_ID", "stream-processor");
        String topologyType = System.getenv().getOrDefault("TOPOLOGY_TYPE", "city");

        String timescaleHost = System.getenv().getOrDefault("TIMESCALE_HOST", "localhost");
        int timescalePort = Integer.parseInt(System.getenv().getOrDefault("TIMESCALE_PORT", "5433"));
        String timescaleDatabase = System.getenv().getOrDefault("TIMESCALE_DATABASE", "analyticsdb");
        String timescaleUser = System.getenv().getOrDefault("TIMESCALE_USER", "analyticsuser");
        String timescalePassword = System.getenv().getOrDefault("TIMESCALE_PASSWORD", "analyticspass");

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 10 * 1024 * 1024L);

        TimescaleSink timescaleSink = new TimescaleSink(timescaleHost, timescalePort, timescaleDatabase, timescaleUser, timescalePassword);

        Topology topology;
        if ("city".equalsIgnoreCase(topologyType)) {
            logger.info("Starting City Sales Topology");
            topology = new CitySalesTopology(timescaleSink).build();
        } else if ("salesman".equalsIgnoreCase(topologyType)) {
            logger.info("Starting Salesman Topology");
            topology = new SalesmanTopology(timescaleSink).build();
        } else {
            throw new IllegalArgumentException("Unknown topology type: " + topologyType);
        }

        KafkaStreams streams = new KafkaStreams(topology, props);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down Stream Processor");
            streams.close();
            timescaleSink.close();
        }));

        streams.start();
        logger.info("Stream Processor started with topology: {}", topologyType);
    }
}
