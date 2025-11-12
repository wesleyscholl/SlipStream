package com.slipstream.examples;

import com.slipstream.monitoring.MetricsCollector;
import com.slipstream.monitoring.DashboardServer;
import com.slipstream.model.AnomalyResult;
import com.slipstream.model.TransactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Example demonstrating the integration of SlipStream with the monitoring dashboard.
 * This creates a standalone application that generates sample data and displays it
 * on the real-time monitoring dashboard.
 */
public class DashboardExample {
    
    private static final Logger logger = LoggerFactory.getLogger(DashboardExample.class);
    
    private final MetricsCollector metricsCollector;
    private final DashboardServer dashboardServer;
    private final ScheduledExecutorService dataGenerator;
    private final Random random;
    
    public DashboardExample() {
        this.metricsCollector = new MetricsCollector();
        this.dashboardServer = new DashboardServer(metricsCollector, 8080);
        this.dataGenerator = Executors.newScheduledThreadPool(2);
        this.random = new Random();
    }
    
    public void start() {
        logger.info("Starting SlipStream Dashboard Example...");
        
        try {
            // Start the dashboard server
            dashboardServer.start();
            
            // Start generating sample transaction data
            startDataGeneration();
            
            logger.info("Dashboard available at: http://localhost:8080/");
            logger.info("Press Ctrl+C to stop the application");
            
        } catch (Exception e) {
            logger.error("Failed to start dashboard example: {}", e.getMessage(), e);
            throw new RuntimeException("Startup failed", e);
        }
    }
    
    public void stop() {
        logger.info("Stopping SlipStream Dashboard Example...");
        
        if (dataGenerator != null && !dataGenerator.isShutdown()) {
            dataGenerator.shutdown();
            try {
                if (!dataGenerator.awaitTermination(5, TimeUnit.SECONDS)) {
                    dataGenerator.shutdownNow();
                }
            } catch (InterruptedException e) {
                dataGenerator.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        if (dashboardServer != null) {
            dashboardServer.stop();
        }
        
        logger.info("Dashboard example stopped");
    }
    
    private void startDataGeneration() {
        // Generate normal transactions
        dataGenerator.scheduleAtFixedRate(this::generateNormalTransaction, 0, 2, TimeUnit.SECONDS);
        
        // Occasionally generate anomalous transactions
        dataGenerator.scheduleAtFixedRate(this::generateAnomalousTransaction, 5, 10, TimeUnit.SECONDS);
        
        // Update system metrics periodically
        dataGenerator.scheduleAtFixedRate(this::updateSystemMetrics, 0, 1, TimeUnit.SECONDS);
    }
    
    private void generateNormalTransaction() {
        try {
            String transactionId = "TXN-" + System.currentTimeMillis() + "-" + random.nextInt(1000);
            double amount = 20.0 + random.nextGaussian() * 5.0; // Normal distribution around $20
            
            // Record the transaction processing time
            long processingTime = 10 + random.nextInt(50); // Simulate 10-60ms processing time
            metricsCollector.recordTransaction(processingTime);
            
            // Simulate occasional anomaly detection (5% chance)
            if (random.nextDouble() < 0.05) {
                AnomalyResult anomaly = createAnomalyResult(transactionId, 0.7 + random.nextDouble() * 0.2, 
                                                          AnomalyResult.AnomalyType.UNUSUAL_AMOUNT, "Unusual amount pattern");
                metricsCollector.recordAnomaly(anomaly);
                logger.info("Anomaly detected: {} (score: {:.3f})", transactionId, anomaly.getAnomalyScore());
            }
            
        } catch (Exception e) {
            logger.error("Error generating normal transaction: {}", e.getMessage(), e);
        }
    }
    
    private void generateAnomalousTransaction() {
        try {
            String transactionId = "ANOM-" + System.currentTimeMillis() + "-" + random.nextInt(1000);
            
            // Simulate longer processing time for anomalous transactions
            long processingTime = 50 + random.nextInt(200); // 50-250ms processing time
            metricsCollector.recordTransaction(processingTime);
            
            // Generate obviously anomalous transaction data
            double anomalyScore = 0.85 + random.nextDouble() * 0.14; // High anomaly score
            AnomalyResult.AnomalyType[] anomalyTypes = {
                AnomalyResult.AnomalyType.FRAUD, 
                AnomalyResult.AnomalyType.VELOCITY, 
                AnomalyResult.AnomalyType.LOCATION, 
                AnomalyResult.AnomalyType.TIME_PATTERN
            };
            AnomalyResult.AnomalyType anomalyType = anomalyTypes[random.nextInt(anomalyTypes.length)];
            String reason = generateAnomalyReason(anomalyType);
            
            AnomalyResult anomaly = createAnomalyResult(transactionId, anomalyScore, anomalyType, reason);
            metricsCollector.recordAnomaly(anomaly);
            
            logger.info("Anomalous transaction generated: {} (score: {:.3f}, type: {})", 
                       transactionId, anomalyScore, anomalyType);
            
        } catch (Exception e) {
            logger.error("Error generating anomalous transaction: {}", e.getMessage(), e);
        }
    }
    
    private void updateSystemMetrics() {
        // Simulate system load and health updates
        metricsCollector.setActiveDetectors(1);
        
        // Occasionally trigger an alert for demonstration
        if (random.nextDouble() < 0.1) { // 10% chance
            String alertId = "ALERT-" + System.currentTimeMillis();
            AnomalyResult alertAnomaly = createAnomalyResult(alertId, 0.95, AnomalyResult.AnomalyType.FRAUD, "High anomaly rate detected");
            metricsCollector.recordAlert(alertAnomaly);
        }
    }
    
    private String generateAnomalyReason(AnomalyResult.AnomalyType type) {
        return switch (type) {
            case FRAUD -> "Potential fraudulent transaction detected";
            case UNUSUAL_AMOUNT -> "Transaction amount outside normal distribution";
            case VELOCITY -> "High transaction velocity detected";
            case LOCATION -> "Transaction from unexpected location";
            case TIME_PATTERN -> "Unusual time of day for transaction";
            case MERCHANT_PATTERN -> "Unusual merchant interaction pattern";
            default -> "Anomalous transaction detected";
        };
    }
    
    private AnomalyResult createAnomalyResult(String transactionId, double score, 
                                            AnomalyResult.AnomalyType type, String reason) {
        // Create a minimal TransactionEvent for the anomaly
        TransactionEvent fakeTransaction = new TransactionEvent();
        
        return new AnomalyResult(
            transactionId,
            true, // isAnomaly
            score, // anomalyScore
            0.9, // confidence
            type, // anomalyType
            LocalDateTime.now(), // detectedAt
            fakeTransaction, // originalTransaction
            Map.of("amount", 1000.0, "risk_score", score), // featuresUsed
            reason // reason
        );
    }
    
    public static void main(String[] args) {
        DashboardExample example = new DashboardExample();
        
        // Add shutdown hook for graceful cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(example::stop));
        
        try {
            example.start();
            
            // Keep the application running
            Thread.currentThread().join();
            
        } catch (InterruptedException e) {
            logger.info("Application interrupted, shutting down...");
            example.stop();
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Fatal error in dashboard example: {}", e.getMessage(), e);
            example.stop();
            System.exit(1);
        }
    }
}