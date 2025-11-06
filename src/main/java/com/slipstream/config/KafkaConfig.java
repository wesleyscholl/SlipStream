package com.slipstream.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;

import java.util.Properties;

/**
 * Configuration class for Kafka Streams application.
 * Handles all Kafka-related configuration properties.
 */
public class KafkaConfig {
    
    // Default configuration values
    private static final String DEFAULT_APPLICATION_ID = "slipstream-anomaly-detector";
    private static final String DEFAULT_BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String DEFAULT_INPUT_TOPIC = "transactions";
    private static final String DEFAULT_OUTPUT_TOPIC = "anomalies";
    private static final String DEFAULT_ALERTS_TOPIC = "alerts";
    
    // Configuration properties
    private String applicationId;
    private String bootstrapServers;
    private String inputTopic;
    private String outputTopic;
    private String alertsTopic;
    private int numStreamThreads;
    private String stateDir;
    private long commitIntervalMs;
    private int replicationFactor;

    public KafkaConfig() {
        // Initialize with default values
        this.applicationId = DEFAULT_APPLICATION_ID;
        this.bootstrapServers = DEFAULT_BOOTSTRAP_SERVERS;
        this.inputTopic = DEFAULT_INPUT_TOPIC;
        this.outputTopic = DEFAULT_OUTPUT_TOPIC;
        this.alertsTopic = DEFAULT_ALERTS_TOPIC;
        this.numStreamThreads = 1;
        this.stateDir = "/tmp/kafka-streams";
        this.commitIntervalMs = 30000L; // 30 seconds
        this.replicationFactor = 1;
    }

    /**
     * Creates Kafka Streams properties for the application
     */
    public Properties getStreamsProperties() {
        Properties props = new Properties();
        
        // Required properties
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        
        // Serialization
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        
        // Performance tuning
        props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, numStreamThreads);
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, commitIntervalMs);
        props.put(StreamsConfig.STATESTORE_CACHE_MAX_BYTES_CONFIG, 1024 * 1024L); // 1MB
        
        // State store configuration
        props.put(StreamsConfig.STATE_DIR_CONFIG, stateDir);
        props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, replicationFactor);
        
        // Error handling
        props.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, 
                 "org.apache.kafka.streams.errors.LogAndContinueExceptionHandler");
        
        // Processing guarantee
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);
        
        return props;
    }

    /**
     * Creates consumer properties for Kafka consumers
     */
    public Properties getConsumerProperties() {
        Properties props = new Properties();
        
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, applicationId + "-consumer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, 
                 "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, 
                 "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 5000);
        
        return props;
    }

    /**
     * Creates producer properties for Kafka producers
     */
    public Properties getProducerProperties() {
        Properties props = new Properties();
        
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, 
                 "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, 
                 "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        
        return props;
    }

    // Getters and Setters
    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }

    public String getBootstrapServers() { return bootstrapServers; }
    public void setBootstrapServers(String bootstrapServers) { this.bootstrapServers = bootstrapServers; }

    public String getInputTopic() { return inputTopic; }
    public void setInputTopic(String inputTopic) { this.inputTopic = inputTopic; }

    public String getOutputTopic() { return outputTopic; }
    public void setOutputTopic(String outputTopic) { this.outputTopic = outputTopic; }

    public String getAlertsTopic() { return alertsTopic; }
    public void setAlertsTopic(String alertsTopic) { this.alertsTopic = alertsTopic; }

    public int getNumStreamThreads() { return numStreamThreads; }
    public void setNumStreamThreads(int numStreamThreads) { this.numStreamThreads = numStreamThreads; }

    public String getStateDir() { return stateDir; }
    public void setStateDir(String stateDir) { this.stateDir = stateDir; }

    public long getCommitIntervalMs() { return commitIntervalMs; }
    public void setCommitIntervalMs(long commitIntervalMs) { this.commitIntervalMs = commitIntervalMs; }

    public int getReplicationFactor() { return replicationFactor; }
    public void setReplicationFactor(int replicationFactor) { this.replicationFactor = replicationFactor; }
}