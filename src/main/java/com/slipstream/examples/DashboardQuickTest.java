package com.slipstream.examples;

import com.slipstream.monitoring.MetricsCollector;
import com.slipstream.monitoring.DashboardServer;
import com.slipstream.model.AnomalyResult;
import com.slipstream.model.TransactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Quick test to verify dashboard functionality
 */
public class DashboardQuickTest {
    
    private static final Logger logger = LoggerFactory.getLogger(DashboardQuickTest.class);
    
    public static void main(String[] args) throws Exception {
        logger.info("Starting SlipStream Dashboard Quick Test...");
        
        // Use a different port to avoid conflicts
        MetricsCollector metricsCollector = new MetricsCollector();
        DashboardServer dashboardServer = new DashboardServer(metricsCollector, 8082);
        
        try {
            // Add some test data
            metricsCollector.recordTransaction(25L);
            metricsCollector.recordTransaction(30L);
            metricsCollector.recordTransaction(45L);
            
            // Create test anomaly results
            AnomalyResult anomaly1 = createTestAnomalyResult("TEST-001", 0.85, AnomalyResult.AnomalyType.FRAUD);
            AnomalyResult anomaly2 = createTestAnomalyResult("TEST-002", 0.92, AnomalyResult.AnomalyType.UNUSUAL_AMOUNT);
            
            metricsCollector.recordAnomaly(anomaly1);
            metricsCollector.recordAnomaly(anomaly2);
            
            // Start the dashboard
            dashboardServer.start();
            
            logger.info("Dashboard started successfully!");
            logger.info("Access the dashboard at: http://localhost:8082/");
            logger.info("API endpoints available:");
            logger.info("  - Metrics: http://localhost:8082/api/metrics");
            logger.info("  - Health:  http://localhost:8082/api/health");
            logger.info("  - Anomalies: http://localhost:8082/api/anomalies");
            logger.info("  - Distribution: http://localhost:8082/api/distribution");
            
            // Keep running for a minute
            Thread.sleep(60000);
            
        } finally {
            dashboardServer.stop();
            logger.info("Dashboard stopped");
        }
    }
    
    private static AnomalyResult createTestAnomalyResult(String transactionId, double score, AnomalyResult.AnomalyType type) {
        TransactionEvent fakeTransaction = new TransactionEvent();
        
        return new AnomalyResult(
            transactionId,
            true, // isAnomaly
            score, // anomalyScore
            0.9, // confidence
            type, // anomalyType
            LocalDateTime.now(), // detectedAt
            fakeTransaction, // originalTransaction
            Map.of("risk_score", score, "amount", 1000.0), // featuresUsed
            "Test anomaly for demonstration" // reason
        );
    }
}