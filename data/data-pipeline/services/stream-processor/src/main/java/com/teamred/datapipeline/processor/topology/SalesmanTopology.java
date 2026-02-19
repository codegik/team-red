package com.teamred.datapipeline.processor.topology;

import com.teamred.datapipeline.model.SalesEventDto;
import com.teamred.datapipeline.processor.aggregation.SalesmanAggregate;
import com.teamred.datapipeline.processor.sink.TimescaleSink;
import com.teamred.datapipeline.serdes.JsonSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class SalesmanTopology {

    private static final Logger logger = LoggerFactory.getLogger(SalesmanTopology.class);
    private final TimescaleSink timescaleSink;

    public SalesmanTopology(TimescaleSink timescaleSink) {
        this.timescaleSink = timescaleSink;
    }

    public Topology build() {
        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, SalesEventDto> salesStream = builder.stream(
                "sales.raw.db",
                Consumed.with(Serdes.String(), new JsonSerde<>(SalesEventDto.class))
        );

        KStream<String, SalesEventDto> allSales = salesStream
                .merge(builder.stream("sales.raw.file", Consumed.with(Serdes.String(), new JsonSerde<>(SalesEventDto.class))))
                .merge(builder.stream("sales.raw.soap", Consumed.with(Serdes.String(), new JsonSerde<>(SalesEventDto.class))));

        TimeWindowedKStream<String, SalesEventDto> windowedBySalesman = allSales
                .selectKey((key, value) -> value.getSalesmanId())
                .groupByKey(Grouped.with(Serdes.String(), new JsonSerde<>(SalesEventDto.class)))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofHours(1)));

        windowedBySalesman
                .aggregate(
                        SalesmanAggregate::new,
                        (salesmanId, event, aggregate) -> {
                            if (aggregate.getSalesmanId() == null) {
                                aggregate.setSalesmanId(salesmanId);
                                aggregate.setSalesmanName(event.getSalesmanName());
                            }
                            aggregate.setTotalSales(aggregate.getTotalSales() + event.getTotalAmount());
                            aggregate.setTransactionCount(aggregate.getTransactionCount() + 1);
                            aggregate.getCitiesCovered().add(event.getCity());

                            return aggregate;
                        },
                        Materialized.with(Serdes.String(), new JsonSerde<>(SalesmanAggregate.class))
                )
                .toStream()
                .foreach((windowedKey, aggregate) -> {
                    long windowStart = windowedKey.window().start();
                    long windowEnd = windowedKey.window().end();

                    aggregate.setWindowStart(windowStart);
                    aggregate.setWindowEnd(windowEnd);

                    timescaleSink.insertSalesmanStats(
                            aggregate.getSalesmanId(),
                            aggregate.getSalesmanName(),
                            windowStart,
                            windowEnd,
                            aggregate.getTotalSales(),
                            aggregate.getTransactionCount(),
                            aggregate.getCitiesCount()
                    );

                    logger.info("Salesman sales aggregated: {} - ${} ({} transactions, {} cities)",
                            aggregate.getSalesmanName(), aggregate.getTotalSales(),
                            aggregate.getTransactionCount(), aggregate.getCitiesCount());
                });

        return builder.build();
    }
}
