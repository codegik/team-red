package com.teamred.datapipeline.lineage;

import org.apache.kafka.common.header.Headers;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class LineageContext {

    public static final String LINEAGE_ID_HEADER = "lineage-id";
    public static final String SOURCE_SYSTEM_HEADER = "source-system";
    public static final String SOURCE_TIMESTAMP_HEADER = "source-timestamp";
    public static final String INGESTION_TIMESTAMP_HEADER = "ingestion-timestamp";

    public static final String LINEAGE_ID_MDC_KEY = "lineageId";
    public static final String SOURCE_SYSTEM_MDC_KEY = "sourceSystem";

    public static String generateLineageId() {
        return UUID.randomUUID().toString();
    }

    public static void addLineageHeaders(Headers headers, String lineageId, String sourceSystem, long sourceTimestamp) {
        headers.add(LINEAGE_ID_HEADER, lineageId.getBytes(StandardCharsets.UTF_8));
        headers.add(SOURCE_SYSTEM_HEADER, sourceSystem.getBytes(StandardCharsets.UTF_8));
        headers.add(SOURCE_TIMESTAMP_HEADER, String.valueOf(sourceTimestamp).getBytes(StandardCharsets.UTF_8));
        headers.add(INGESTION_TIMESTAMP_HEADER, String.valueOf(System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
    }

    public static String getHeaderValue(Headers headers, String key) {
        var header = headers.lastHeader(key);
        if (header == null) {
            return null;
        }
        return new String(header.value(), StandardCharsets.UTF_8);
    }

    public static void setMDC(String lineageId, String sourceSystem) {
        if (lineageId != null) {
            MDC.put(LINEAGE_ID_MDC_KEY, lineageId);
        }
        if (sourceSystem != null) {
            MDC.put(SOURCE_SYSTEM_MDC_KEY, sourceSystem);
        }
    }

    public static void clearMDC() {
        MDC.remove(LINEAGE_ID_MDC_KEY);
        MDC.remove(SOURCE_SYSTEM_MDC_KEY);
    }

    public static void propagateLineageHeaders(Headers sourceHeaders, Headers targetHeaders) {
        var lineageId = getHeaderValue(sourceHeaders, LINEAGE_ID_HEADER);
        var sourceSystem = getHeaderValue(sourceHeaders, SOURCE_SYSTEM_HEADER);
        var sourceTimestamp = getHeaderValue(sourceHeaders, SOURCE_TIMESTAMP_HEADER);

        if (lineageId != null) {
            targetHeaders.add(LINEAGE_ID_HEADER, lineageId.getBytes(StandardCharsets.UTF_8));
        }
        if (sourceSystem != null) {
            targetHeaders.add(SOURCE_SYSTEM_HEADER, sourceSystem.getBytes(StandardCharsets.UTF_8));
        }
        if (sourceTimestamp != null) {
            targetHeaders.add(SOURCE_TIMESTAMP_HEADER, sourceTimestamp.getBytes(StandardCharsets.UTF_8));
        }
        targetHeaders.add(INGESTION_TIMESTAMP_HEADER, String.valueOf(System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
    }
}
