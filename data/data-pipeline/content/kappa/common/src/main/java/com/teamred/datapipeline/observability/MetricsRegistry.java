package com.teamred.datapipeline.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

public class MetricsRegistry {

    private static MeterRegistry registry;

    public static MeterRegistry getRegistry() {
        if (registry == null) {
            registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        }
        return registry;
    }

    public static void setRegistry(MeterRegistry meterRegistry) {
        registry = meterRegistry;
    }

    public static Counter counter(String name, String... tags) {
        return getRegistry().counter(name, tags);
    }

    public static Timer timer(String name, String... tags) {
        return getRegistry().timer(name, tags);
    }
}
