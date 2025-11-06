package com.slipstream;

import com.slipstream.config.KafkaConfig;
import com.slipstream.stream.AnomalyDetectionStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main application class for SlipStream Anomaly Detector.
 * Handles application lifecycle and configuration.
 */
public class SlipStreamApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(SlipStreamApplication.class);
    
    private final KafkaConfig kafkaConfig;
    private final AnomalyDetectionStreams anomalyStreams;
    private final ScheduledExecutorService scheduler;

    public SlipStreamApplication() {
        this.kafkaConfig = loadConfiguration();
        this.anomalyStreams = new AnomalyDetectionStreams(kafkaConfig);
        this.scheduler = Executors.newScheduledThreadPool(1);
        
        logger.info("SlipStream Application initialized");
    }

    public static void main(String[] args) {
        logger.info("Starting SlipStream Anomaly Detector...");
        
        try {
            SlipStreamApplication app = new SlipStreamApplication();
            app.start();
            
            // Keep the application running
            app.awaitTermination();
            
        } catch (Exception e) {
            logger.error("Fatal error in SlipStream application: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    /**
     * Starts the application and all its components
     */
    public void start() {
        logger.info("Starting SlipStream components...");
        
        try {
            // Start the anomaly detection streams
            anomalyStreams.start();
            
            // Schedule periodic metrics logging
            scheduleMetricsLogging();
            
            logger.info("SlipStream application started successfully");
            
        } catch (Exception e) {
            logger.error("Failed to start SlipStream application: {}", e.getMessage(), e);
            throw new RuntimeException("Application startup failed", e);
        }
    }

    /**
     * Stops the application gracefully
     */
    public void stop() {
        logger.info("Stopping SlipStream application...");
        
        try {
            // Stop metrics logging
            scheduler.shutdown();
            
            // Stop streams processing
            anomalyStreams.stop();
            
            // Wait for scheduler to terminate
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            
            logger.info("SlipStream application stopped successfully");
            
        } catch (Exception e) {
            logger.error("Error during application shutdown: {}", e.getMessage(), e);
        }
    }

    /**
     * Waits for the application to be terminated
     */
    private void awaitTermination() {
        try {
            // Add shutdown hook for graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutdown signal received");
                stop();
            }));
            
            // Keep the main thread alive
            Thread currentThread = Thread.currentThread();
            while (!currentThread.isInterrupted()) {
                Thread.sleep(1000);
                
                // Check if streams are still running
                if (anomalyStreams.getState() != null && 
                    !anomalyStreams.getState().isRunningOrRebalancing()) {
                    logger.warn("Streams application is not running. State: {}", 
                               anomalyStreams.getState());
                    break;
                }
            }
            
        } catch (InterruptedException e) {
            logger.info("Application interrupted");
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Schedules periodic metrics logging
     */
    private void scheduleMetricsLogging() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                logger.info("=== SlipStream Metrics ===");
                anomalyStreams.logMetrics();
                
                // Log JVM metrics
                Runtime runtime = Runtime.getRuntime();
                long totalMemory = runtime.totalMemory();
                long freeMemory = runtime.freeMemory();
                long usedMemory = totalMemory - freeMemory;
                
                logger.info("JVM Memory - Used: {}MB, Free: {}MB, Total: {}MB",
                           usedMemory / (1024 * 1024),
                           freeMemory / (1024 * 1024),
                           totalMemory / (1024 * 1024));
                
            } catch (Exception e) {
                logger.warn("Error logging metrics: {}", e.getMessage());
            }
        }, 30, 30, TimeUnit.SECONDS); // Log every 30 seconds
    }

    /**
     * Loads configuration from system properties and environment variables
     */
    private KafkaConfig loadConfiguration() {
        KafkaConfig config = new KafkaConfig();
        
        // Override with environment variables if present
        String bootstrapServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS");
        if (bootstrapServers != null) {
            config.setBootstrapServers(bootstrapServers);
        }
        
        String inputTopic = System.getenv("KAFKA_INPUT_TOPIC");
        if (inputTopic != null) {
            config.setInputTopic(inputTopic);
        }
        
        String outputTopic = System.getenv("KAFKA_OUTPUT_TOPIC");
        if (outputTopic != null) {
            config.setOutputTopic(outputTopic);
        }
        
        String alertsTopic = System.getenv("KAFKA_ALERTS_TOPIC");
        if (alertsTopic != null) {
            config.setAlertsTopic(alertsTopic);
        }
        
        String numThreads = System.getenv("KAFKA_NUM_THREADS");
        if (numThreads != null) {
            try {
                config.setNumStreamThreads(Integer.parseInt(numThreads));
            } catch (NumberFormatException e) {
                logger.warn("Invalid KAFKA_NUM_THREADS value: {}, using default", numThreads);
            }
        }
        
        String stateDir = System.getenv("KAFKA_STATE_DIR");
        if (stateDir != null) {
            config.setStateDir(stateDir);
        }
        
        // Log configuration
        logger.info("Loaded configuration:");
        logger.info("  Bootstrap Servers: {}", config.getBootstrapServers());
        logger.info("  Input Topic: {}", config.getInputTopic());
        logger.info("  Output Topic: {}", config.getOutputTopic());
        logger.info("  Alerts Topic: {}", config.getAlertsTopic());
        logger.info("  Stream Threads: {}", config.getNumStreamThreads());
        logger.info("  State Directory: {}", config.getStateDir());
        
        return config;
    }

    // Getters for testing
    public KafkaConfig getKafkaConfig() {
        return kafkaConfig;
    }

    public AnomalyDetectionStreams getAnomalyStreams() {
        return anomalyStreams;
    }
}