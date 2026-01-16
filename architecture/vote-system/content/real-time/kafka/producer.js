import { Kafka } from 'kafkajs';
import { kafkaConfig, topics } from './config.js';

class KafkaProducerService {
  constructor() {
    this.kafka = new Kafka(kafkaConfig);
    this.producer = this.kafka.producer();
    this.isConnected = false;
  }

  async connect() {
    if (!this.isConnected) {
      await this.producer.connect();
      this.isConnected = true;
      console.log('Kafka producer connected');
    }
  }

  async disconnect() {
    if (this.isConnected) {
      await this.producer.disconnect();
      this.isConnected = false;
      console.log('Kafka producer disconnected');
    }
  }

  async sendEvent(event) {
    try {
      await this.connect();
      
      const message = {
        key: event.id || Date.now().toString(),
        value: JSON.stringify(event),
        timestamp: Date.now().toString()
      };

      await this.producer.send({
        topic: topics.EVENTS,
        messages: [message]
      });

      console.log('Event sent to Kafka:', message.key);
      return true;
    } catch (error) {
      console.error('Error sending event to Kafka:', error);
      return false;
    }
  }

  async sendSummary(summary) {
    try {
      await this.connect();
      
      const message = {
        key: summary.id || Date.now().toString(),
        value: JSON.stringify(summary),
        timestamp: Date.now().toString()
      };

      await this.producer.send({
        topic: topics.EVENTS_SUMMARY,
        messages: [message]
      });

      console.log('Summary sent to Kafka table:', message.key);
      return true;
    } catch (error) {
      console.error('Error sending summary to Kafka:', error);
      return false;
    }
  }
}

export const kafkaProducer = new KafkaProducerService();
