package com.slipstream.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.slipstream.config.KafkaConfig;
import com.slipstream.detector.AnomalyDetector;
import com.slipstream.detector.StatisticalAnomalyDetector;
import com.slipstream.model.AnomalyResult;
import com.slipstream.model.TransactionEvent;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Main Kafka Streams processor for real-time anomaly detection.
 * Processes transaction events and produces anomaly detection results.
 */
public class AnomalyDetectionStreams {
    
    private static final Logger logger = LoggerFactory.getLogger(AnomalyDetectionStreams.class);
    
    private final KafkaConfig kafkaConfig;
    private final AnomalyDetector anomalyDetector;
    private final ObjectMapper objectMapper;
    private KafkaStreams streams;

    public AnomalyDetectionStreams(KafkaConfig kafkaConfig) {
        this.kafkaConfig = kafkaConfig;
        this.anomalyDetector = new StatisticalAnomalyDetector();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        logger.info("Initialized AnomalyDetectionStreams with detector: {}", 
                   anomalyDetector.getDetectorName());
    }

    public AnomalyDetectionStreams(KafkaConfig kafkaConfig, AnomalyDetector customDetector) {
        this.kafkaConfig = kafkaConfig;
        this.anomalyDetector = customDetector;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        logger.info("Initialized AnomalyDetectionStreams with custom detector: {}", 
                   anomalyDetector.getDetectorName());
    }

    /**
     * Builds the Kafka Streams topology for anomaly detection
     */
    public void start() {
        StreamsBuilder builder = new StreamsBuilder();
        
        // Create the processing topology
        buildTopology(builder);
        
        // Create and start the streams
        Properties props = kafkaConfig.getStreamsProperties();
        streams = new KafkaStreams(builder.build(), props);
        
        // Add shutdown hook
        addShutdownHook();
        
        // Start processing
        logger.info("Starting Kafka Streams application...");
        streams.start();
        
        logger.info("Kafka Streams application started successfully");
    }

    /**
     * Builds the main processing topology
     */
    private void buildTopology(StreamsBuilder builder) {
        // Input stream of transaction events
        KStream<String, String> transactionStream = builder.stream(
            kafkaConfig.getInputTopic(),
            Consumed.with(Serdes.String(), Serdes.String())
        );

        // Process transactions and detect anomalies
        KStream<String, AnomalyResult> resultStream = transactionStream
            .peek((key, value) -> logger.debug("Processing transaction: {}", key))
            .mapValues(this::parseTransaction)
            .filter((key, transaction) -> transaction != null)
            .mapValues(this::detectAnomaly)
            .peek((key, result) -> {
                // Update model if supported
                if (anomalyDetector.supportsOnlineLearning() && result.getOriginalTransaction() != null) {
                    try {
                        anomalyDetector.updateModel(result.getOriginalTransaction());
                    } catch (Exception e) {
                        logger.warn("Failed to update model for transaction {}: {}", key, e.getMessage());
                    }
                }
            });

        // Convert to JSON for output
        KStream<String, String> anomalyStream = resultStream.mapValues(this::serializeAnomalyResult)
            .filter((key, result) -> result != null);

        // Filter anomalies for alerts
        KStream<String, String> anomalies = anomalyStream.filter((key, value) -> {
            try {
                AnomalyResult result = objectMapper.readValue(value, AnomalyResult.class);
                return result.isAnomaly();
            } catch (Exception e) {
                logger.warn("Error parsing anomaly result for filtering: {}", e.getMessage());
                return false;
            }
        });

        // Send anomalies to alerts topic with higher priority
        anomalies
            .peek((key, value) -> logger.warn("ANOMALY DETECTED: {}", key))
            .to(kafkaConfig.getAlertsTopic(), Produced.with(Serdes.String(), Serdes.String()));

        // Send all results to output topic
        anomalyStream
            .to(kafkaConfig.getOutputTopic(), Produced.with(Serdes.String(), Serdes.String()));

        logger.info("Built topology: {} -> [PROCESSING] -> {} (anomalies: {})", 
                   kafkaConfig.getInputTopic(), 
                   kafkaConfig.getOutputTopic(),
                   kafkaConfig.getAlertsTopic());
    }

    /**
     * Parses JSON string to TransactionEvent
     */
    private TransactionEvent parseTransaction(String jsonString) {
        try {
            return objectMapper.readValue(jsonString, TransactionEvent.class);
        } catch (Exception e) {
            logger.error("Failed to parse transaction JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Performs anomaly detection on a transaction
     */
    private AnomalyResult detectAnomaly(TransactionEvent transaction) {
        try {
            AnomalyResult result = anomalyDetector.detect(transaction);
            
            if (result.isAnomaly()) {
                logger.info("Anomaly detected: transaction={}, score={}, type={}", 
                           transaction.getTransactionId(),
                           result.getAnomalyScore(),
                           result.getAnomalyType());
            } else {
                logger.debug("Normal transaction: {}", transaction.getTransactionId());
            }
            
            return result;
        } catch (Exception e) {
            logger.error("Error during anomaly detection for transaction {}: {}", 
                        transaction.getTransactionId(), e.getMessage());
            
            // Return a safe result indicating processing error
            return new AnomalyResult(
                transaction.getTransactionId(),
                false,
                0.0,
                0.0,
                AnomalyResult.AnomalyType.UNKNOWN,
                java.time.LocalDateTime.now(),
                transaction,
                new java.util.HashMap<>(),
                "Processing error: " + e.getMessage()
            );
        }
    }



    /**
     * Serializes AnomalyResult to JSON string
     */
    private String serializeAnomalyResult(AnomalyResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            logger.error("Failed to serialize anomaly result: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Stops the Kafka Streams application
     */
    public void stop() {
        if (streams != null) {
            logger.info("Stopping Kafka Streams application...");
            streams.close();
            logger.info("Kafka Streams application stopped");
        }
    }

    /**
     * Adds JVM shutdown hook for graceful shutdown
     */
    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown hook triggered");
            stop();
        }));
    }

    /**
     * Returns the current state of the streams application
     */
    public KafkaStreams.State getState() {
        return streams != null ? streams.state() : null;
    }

    /**
     * Returns metrics about the streams application
     */
    public void logMetrics() {
        if (streams != null) {
            logger.info("Streams state: {}", streams.state());
            
            // Log detector-specific metrics if available
            if (anomalyDetector instanceof StatisticalAnomalyDetector) {
                StatisticalAnomalyDetector sad = (StatisticalAnomalyDetector) anomalyDetector;
                logger.info("Detector metrics - Global samples: {}, Model trained: {}, Total transactions: {}, Users: {}",
                           sad.getGlobalSampleCount(), sad.isModelTrained(), sad.getTotalTransactions(), sad.getUserCount());
            }
        }
    }

    public AnomalyDetector getAnomalyDetector() {
        return anomalyDetector;
    }

    public KafkaConfig getKafkaConfig() {
        return kafkaConfig;
    }
}