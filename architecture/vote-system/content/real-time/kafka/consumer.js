import { Kafka } from 'kafkajs';
import { kafkaConfig, topics, consumerConfig, aggregationConfig } from './config.js';
import { kafkaProducer } from './producer.js';

class KafkaConsumerService {
  constructor() {
    this.kafka = new Kafka(kafkaConfig);
    this.consumer = this.kafka.consumer(consumerConfig);
    this.isConnected = false;
    
    // Aggregation state
    this.eventBuffer = [];
    this.aggregationStats = {
      totalEvents: 0,
      sumOfValues: 0,
      minValue: Infinity,
      maxValue: -Infinity,
      lastEventTime: null,
      firstEventTime: null
    };
    
    // Timer for cron-based aggregation
    this.aggregationTimer = null;
  }

  async connect() {
    if (!this.isConnected) {
      await this.consumer.connect();
      await this.consumer.subscribe({ 
        topic: topics.EVENTS, 
        fromBeginning: false 
      });
      this.isConnected = true;
      console.log('Kafka consumer connected and subscribed to', topics.EVENTS);
    }
  }

  async disconnect() {
    if (this.aggregationTimer) {
      clearInterval(this.aggregationTimer);
    }
    
    if (this.isConnected) {
      await this.consumer.disconnect();
      this.isConnected = false;
      console.log('Kafka consumer disconnected');
    }
  }

  resetAggregation() {
    this.eventBuffer = [];
    this.aggregationStats = {
      totalEvents: 0,
      sumOfValues: 0,
      minValue: Infinity,
      maxValue: -Infinity,
      lastEventTime: null,
      firstEventTime: null
    };
  }

  updateAggregation(event) {
    this.eventBuffer.push(event);
    this.aggregationStats.totalEvents++;
    
    // Extract numeric value if available (adjust based on your event structure)
    const value = event.payload?.times || event.value || 0;
    this.aggregationStats.sumOfValues += value;
    this.aggregationStats.minValue = Math.min(this.aggregationStats.minValue, value);
    this.aggregationStats.maxValue = Math.max(this.aggregationStats.maxValue, value);
    
    const now = Date.now();
    if (!this.aggregationStats.firstEventTime) {
      this.aggregationStats.firstEventTime = now;
    }
    this.aggregationStats.lastEventTime = now;
  }

  async publishSummary(broadcastCallback) {
    if (this.aggregationStats.totalEvents === 0) {
      return;
    }

    const summary = {
      id: `summary-${Date.now()}`,
      timestamp: Date.now(),
      aggregationPeriod: {
        start: this.aggregationStats.firstEventTime,
        end: this.aggregationStats.lastEventTime,
        durationMs: this.aggregationStats.lastEventTime - this.aggregationStats.firstEventTime
      },
      statistics: {
        totalEvents: this.aggregationStats.totalEvents,
        sumOfValues: this.aggregationStats.sumOfValues,
        averageValue: this.aggregationStats.sumOfValues / this.aggregationStats.totalEvents,
        minValue: this.aggregationStats.minValue === Infinity ? 0 : this.aggregationStats.minValue,
        maxValue: this.aggregationStats.maxValue === -Infinity ? 0 : this.aggregationStats.maxValue
      },
      eventSample: this.eventBuffer.slice(-5) // Include last 5 events as sample
    };

    console.log('📊 Publishing aggregated summary:', JSON.stringify(summary, null, 2));
    
    await kafkaProducer.sendSummary(summary);
    
    // Broadcast summary to SSE clients if callback provided
    if (broadcastCallback) {
      broadcastCallback({ type: 'summary', data: summary });
    }
    
    this.resetAggregation();
  }

  async start(broadcastCallback) {
    await this.connect();

    // Start cron-based aggregation timer
    this.aggregationTimer = setInterval(async () => {
      console.log('⏰ Cron-triggered aggregation');
      await this.publishSummary(broadcastCallback);
    }, aggregationConfig.intervalMs);

    // Process messages
    await this.consumer.run({
      eachMessage: async ({ topic, partition, message }) => {
        try {
          const event = JSON.parse(message.value.toString());
          console.log('📨 Received event:', event);
          
          this.updateAggregation(event);
          
          // Check if we've reached the batch size threshold
          if (this.aggregationStats.totalEvents >= aggregationConfig.batchSize) {
            console.log('📦 Batch size threshold reached');
            await this.publishSummary(broadcastCallback);
          }
        } catch (error) {
          console.error('Error processing message:', error);
        }
      }
    });
  }

  async getLatestSummary() {
    return {
      currentBuffer: this.eventBuffer.length,
      stats: this.aggregationStats
    };
  }
}

export const kafkaConsumer = new KafkaConsumerService();
