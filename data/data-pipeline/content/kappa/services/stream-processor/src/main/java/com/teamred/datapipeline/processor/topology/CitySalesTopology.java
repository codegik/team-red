package com.teamred.datapipeline.processor.topology;

import com.teamred.datapipeline.model.SalesEventDto;
import com.teamred.datapipeline.processor.aggregation.CitySalesAggregate;
import com.teamred.datapipeline.processor.sink.TimescaleSink;
import com.teamred.datapipeline.serdes.JsonSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class CitySalesTopology {

    private static final Logger logger = LoggerFactory.getLogger(CitySalesTopology.class);
    private final TimescaleSink timescaleSink;

    public CitySalesTopology(TimescaleSink timescaleSink) {
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

        TimeWindowedKStream<String, SalesEventDto> windowedByCity = allSales
                .selectKey((key, value) -> value.getCity())
                .groupByKey(Grouped.with(Serdes.String(), new JsonSerde<>(SalesEventDto.class)))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofHours(1)));

        windowedByCity
                .aggregate(
                        CitySalesAggregate::new,
                        (city, event, aggregate) -> {
                            if (aggregate.getCity() == null) {
                                aggregate.setCity(city);
                            }
                            aggregate.setTotalSales(aggregate.getTotalSales() + event.getTotalAmount());
                            aggregate.setTransactionCount(aggregate.getTransactionCount() + 1);

                            aggregate.getProductSales().merge(
                                    event.getProductName(),
                                    event.getTotalAmount(),
                                    Double::sum
                            );

                            return aggregate;
                        },
                        Materialized.with(Serdes.String(), new JsonSerde<>(CitySalesAggregate.class))
                )
                .toStream()
                .foreach((windowedKey, aggregate) -> {
                    long windowStart = windowedKey.window().start();
                    long windowEnd = windowedKey.window().end();

                    aggregate.setWindowStart(windowStart);
                    aggregate.setWindowEnd(windowEnd);

                    timescaleSink.insertCitySales(
                            aggregate.getCity(),
                            windowStart,
                            windowEnd,
                            aggregate.getTotalSales(),
                            aggregate.getTransactionCount(),
                            aggregate.getTopProduct(),
                            aggregate.getTopProductSales()
                    );

                    logger.info("City sales aggregated: {} - ${} ({} transactions)",
                            aggregate.getCity(), aggregate.getTotalSales(), aggregate.getTransactionCount());
                });

        return builder.build();
    }
}
