package com.teamred.datapipeline.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "data_lineage")
public class DataLineage {

    @Id
    @Column(name = "lineage_id", nullable = false, length = 100)
    private String lineageId;

    @Column(name = "sale_id", nullable = false, length = 100)
    private String saleId;

    @Column(name = "source_system", nullable = false, length = 20)
    private String sourceSystem;

    @Column(name = "source_timestamp", nullable = false)
    private Instant sourceTimestamp;

    @Column(name = "ingestion_timestamp", nullable = false)
    private Instant ingestionTimestamp;

    @Column(name = "kafka_topic", length = 255)
    private String kafkaTopic;

    @Column(name = "kafka_partition")
    private Integer kafkaPartition;

    @Column(name = "kafka_offset")
    private Long kafkaOffset;

    @Column(name = "transformation_steps", columnDefinition = "jsonb")
    private String transformationSteps;

    @Column(name = "created_at")
    private Instant createdAt;

    public String getLineageId() {
        return lineageId;
    }

    public void setLineageId(String lineageId) {
        this.lineageId = lineageId;
    }

    public String getSaleId() {
        return saleId;
    }

    public void setSaleId(String saleId) {
        this.saleId = saleId;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public Instant getSourceTimestamp() {
        return sourceTimestamp;
    }

    public void setSourceTimestamp(Instant sourceTimestamp) {
        this.sourceTimestamp = sourceTimestamp;
    }

    public Instant getIngestionTimestamp() {
        return ingestionTimestamp;
    }

    public void setIngestionTimestamp(Instant ingestionTimestamp) {
        this.ingestionTimestamp = ingestionTimestamp;
    }

    public String getKafkaTopic() {
        return kafkaTopic;
    }

    public void setKafkaTopic(String kafkaTopic) {
        this.kafkaTopic = kafkaTopic;
    }

    public Integer getKafkaPartition() {
        return kafkaPartition;
    }

    public void setKafkaPartition(Integer kafkaPartition) {
        this.kafkaPartition = kafkaPartition;
    }

    public Long getKafkaOffset() {
        return kafkaOffset;
    }

    public void setKafkaOffset(Long kafkaOffset) {
        this.kafkaOffset = kafkaOffset;
    }

    public String getTransformationSteps() {
        return transformationSteps;
    }

    public void setTransformationSteps(String transformationSteps) {
        this.transformationSteps = transformationSteps;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
