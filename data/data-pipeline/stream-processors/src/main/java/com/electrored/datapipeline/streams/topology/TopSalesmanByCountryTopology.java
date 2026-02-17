package com.electrored.datapipeline.streams.topology;

import com.electrored.datapipeline.streams.model.SaleRecord;
import com.electrored.datapipeline.streams.model.SalesmanAggregate;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Duration;

@Configuration
public class TopSalesmanByCountryTopology {

    private static final Logger logger = LoggerFactory.getLogger(TopSalesmanByCountryTopology.class);

    @Autowired
    public void buildTopology(StreamsBuilder streamsBuilder) {
        // Create serdes
        JsonSerde<SaleRecord> saleRecordSerde = new JsonSerde<>(SaleRecord.class);
        JsonSerde<SalesmanAggregate> salesmanAggregateSerde = new JsonSerde<>(SalesmanAggregate.class);

        // Read from sales topics
        KStream<String, SaleRecord> salesStream = streamsBuilder
            .stream("sales-events",
                Consumed.with(Serdes.String(), saleRecordSerde));

        // Group by salesman and country
        KTable<Windowed<String>, SalesmanAggregate> salesmanAggregates = salesStream
            .groupBy((key, value) -> value.getSellerCode() + "|" + value.getCountry(),
                Grouped.with(Serdes.String(), saleRecordSerde))
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofDays(30))) // Monthly window
            .aggregate(
                SalesmanAggregate::new,
                (compositeKey, sale, aggregate) -> {
                    String[] parts = compositeKey.split("\\|");
                    aggregate.setSellerCode(parts[0]);
                    aggregate.setSellerName(sale.getSellerName());
                    aggregate.setCountry(parts.length > 1 ? parts[1] : sale.getCountry());
                    aggregate.setTotalSales(aggregate.getTotalSales() + sale.getAmount());
                    aggregate.setTransactionCount(aggregate.getTransactionCount() + 1);
                    return aggregate;
                },
                Materialized.with(Serdes.String(), salesmanAggregateSerde)
            );

        // Convert to stream and write to output topic
        salesmanAggregates.toStream()
            .map((windowedKey, aggregate) -> {
                aggregate.setWindowStart(windowedKey.window().start());
                aggregate.setWindowEnd(windowedKey.window().end());
                return KeyValue.pair(windowedKey.key(), aggregate);
            })
            .peek((key, aggregate) ->
                logger.info("Salesman aggregate: {} ({}) in {} - Total: {} from {} transactions",
                    aggregate.getSellerName(), aggregate.getSellerCode(),
                    aggregate.getCountry(), aggregate.getTotalSales(),
                    aggregate.getTransactionCount()))
            .to("top-salesman-by-country",
                Produced.with(Serdes.String(), salesmanAggregateSerde));
    }
}

