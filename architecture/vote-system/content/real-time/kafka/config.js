export const kafkaConfig = {
  clientId: 'princejs-app',
  brokers: [process.env.KAFKA_BROKER || 'localhost:9092'],
  connectionTimeout: 10000,
  retry: {
    initialRetryTime: 100,
    retries: 8
  }
};

export const topics = {
  EVENTS: 'events-topic',
  EVENTS_SUMMARY: 'events-summary-table'
};

export const consumerConfig = {
  groupId: 'events-aggregator-group',
  sessionTimeout: 30000,
  heartbeatInterval: 3000
};

export const aggregationConfig = {
  // Trigger summary after N events
  batchSize: 10,
  // Or trigger summary every N milliseconds (e.g., 30 seconds)
  intervalMs: 30000
};
