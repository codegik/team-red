package com.electrored.datapipeline.streams.topology;

import com.electrored.datapipeline.streams.model.CityAggregate;
import com.electrored.datapipeline.streams.model.SaleRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Duration;

@Configuration
public class TopSalesByCityTopology {

    private static final Logger logger = LoggerFactory.getLogger(TopSalesByCityTopology.class);

    @Autowired
    public void buildTopology(StreamsBuilder streamsBuilder) {
        // Create serdes
        JsonSerde<SaleRecord> saleRecordSerde = new JsonSerde<>(SaleRecord.class);
        JsonSerde<CityAggregate> cityAggregateSerde = new JsonSerde<>(CityAggregate.class);

        // Read from sales topics
        KStream<String, SaleRecord> salesStream = streamsBuilder
            .stream("sales-events",
                Consumed.with(Serdes.String(), saleRecordSerde))
            .peek((key, value) ->
                logger.debug("Processing sale: {} in city: {}", value.getSaleId(), value.getCity()));

        // Aggregate by city with tumbling windows (1 day)
        KTable<Windowed<String>, CityAggregate> cityAggregates = salesStream
            .groupBy((key, value) -> value.getCity(),
                Grouped.with(Serdes.String(), saleRecordSerde))
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofDays(1)))
            .aggregate(
                CityAggregate::new,
                (city, sale, aggregate) -> {
                    aggregate.setCity(city);
                    aggregate.setCountry(sale.getCountry());
                    aggregate.setTotalSales(aggregate.getTotalSales() + sale.getAmount());
                    aggregate.setTransactionCount(aggregate.getTransactionCount() + 1);
                    return aggregate;
                },
                Materialized.with(Serdes.String(), cityAggregateSerde)
            );

        // Convert to stream and write to output topic
        cityAggregates.toStream()
            .map((windowedKey, aggregate) -> {
                aggregate.setWindowStart(windowedKey.window().start());
                aggregate.setWindowEnd(windowedKey.window().end());
                return KeyValue.pair(windowedKey.key(), aggregate);
            })
            .peek((city, aggregate) ->
                logger.info("City aggregate: {} - Total: {} from {} transactions",
                    city, aggregate.getTotalSales(), aggregate.getTransactionCount()))
            .to("top-sales-by-city",
                Produced.with(Serdes.String(), cityAggregateSerde));
    }
}

