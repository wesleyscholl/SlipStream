package com.slipstream.detector;

import com.slipstream.model.AnomalyResult;
import com.slipstream.model.TransactionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnhancedMLAnomalyDetectorTest {

    private EnhancedMLAnomalyDetector detector;
    private TransactionEvent normalTransaction;
    private TransactionEvent anomalousTransaction;

    @BeforeEach
    void setUp() {
        detector = new EnhancedMLAnomalyDetector(0.7);
        
        // Create normal transaction
        TransactionEvent.Location location = new TransactionEvent.Location(40.7128, -74.0060, "USA", "New York");
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("device_id", "mobile_123");

        normalTransaction = new TransactionEvent(
            "tx_normal",
            "user_123",
            "merchant_abc",
            50.0,
            "USD",
            LocalDateTime.of(2024, 1, 15, 14, 30, 0),
            location,
            "credit_card",
            "grocery",
            metadata
        );

        // Create anomalous transaction (very high amount)
        anomalousTransaction = new TransactionEvent(
            "tx_anomaly",
            "user_123",
            "merchant_xyz",
            5000.0,
            "USD",
            LocalDateTime.of(2024, 1, 15, 14, 35, 0),
            location,
            "credit_card",
            "electronics",
            metadata
        );
    }

    @Test
    void testDetectorInitialization() {
        assertNotNull(detector);
        assertEquals("Enhanced ML", detector.getDetectorName());
        assertTrue(detector.supportsOnlineLearning());
    }

    @Test
    void testInitialDetectionBeforeTraining() {
        AnomalyResult result = detector.detect(normalTransaction);
        
        assertNotNull(result);
        assertFalse(result.isAnomaly());
        assertTrue(result.getReason().contains("Model not trained"));
    }

    @Test
    void testModelTraining() {
        // Train with normal transactions
        for (int i = 0; i < 60; i++) {
            TransactionEvent tx = createTrainingTransaction(i);
            detector.updateModel(tx);
        }

        Map<String, Object> metrics = detector.getModelMetrics();
        assertEquals(60, metrics.get("total_transactions"));
        assertTrue((Boolean) metrics.get("model_trained"));
        assertTrue((Integer) metrics.get("unique_users") > 0);
    }

    @Test
    void testAnomalyDetectionAfterTraining() {
        // Train the model
        trainModel();

        // Test normal transaction
        AnomalyResult normalResult = detector.detect(normalTransaction);
        assertNotNull(normalResult);
        // After sufficient training, normal transactions should have low scores
        assertTrue(normalResult.getAnomalyScore() < 0.5);

        // Test anomalous transaction
        AnomalyResult anomalyResult = detector.detect(anomalousTransaction);
        assertNotNull(anomalyResult);
        // Anomalous transaction should have higher score than normal
        assertTrue(anomalyResult.getAnomalyScore() > normalResult.getAnomalyScore());
    }

    @Test
    void testFeatureExtraction() {
        trainModel();
        
        AnomalyResult result = detector.detect(normalTransaction);
        Map<String, Double> features = result.getFeaturesUsed();
        
        assertNotNull(features);
        assertTrue(features.containsKey("amount"));
        assertTrue(features.containsKey("hour_of_day"));
        assertTrue(features.containsKey("day_of_week"));
        assertEquals(50.0, features.get("amount"));
        assertEquals(14.0, features.get("hour_of_day"));
    }

    @Test
    void testVelocityDetection() {
        trainModel();
        
        // Create multiple rapid transactions
        LocalDateTime baseTime = LocalDateTime.of(2024, 1, 15, 14, 30, 0);
        for (int i = 0; i < 5; i++) {
            TransactionEvent rapidTx = new TransactionEvent(
                "tx_rapid_" + i,
                "user_velocity",
                "merchant_abc",
                100.0,
                "USD",
                baseTime.plusMinutes(i),
                normalTransaction.getLocation(),
                "credit_card",
                "grocery",
                new HashMap<>()
            );
            
            AnomalyResult result = detector.detect(rapidTx);
            if (i >= 3) {
                // Later transactions in rapid sequence should have higher scores
                assertTrue(result.getAnomalyScore() > 0.0);
            }
            detector.updateModel(rapidTx);
        }
    }

    @Test
    void testAdaptiveThresholds() {
        trainModel();
        
        // Create user with high variability
        for (int i = 0; i < 20; i++) {
            double amount = 50.0 + (i % 2 == 0 ? 500.0 : 0.0); // Variable amounts
            TransactionEvent variableTx = createTransactionForUser("variable_user", amount, i);
            detector.updateModel(variableTx);
        }
        
        Map<String, Object> metrics = detector.getModelMetrics();
        Double avgThreshold = (Double) metrics.get("avg_adaptive_threshold");
        assertNotNull(avgThreshold);
        assertTrue(avgThreshold >= 0.7); // Should be at least the base threshold
    }

    @Test
    void testBehavioralProfiling() {
        trainModel();
        
        // Train user with specific patterns
        String userId = "behavioral_user";
        for (int i = 0; i < 30; i++) {
            TransactionEvent tx = createTransactionForUser(userId, 25.0, i);
            detector.updateModel(tx);
        }
        
        // Test transaction with unusual merchant category
        TransactionEvent unusualTx = new TransactionEvent(
            "tx_unusual",
            userId,
            "merchant_unusual",
            25.0,
            "USD",
            LocalDateTime.of(2024, 1, 15, 14, 30, 0),
            normalTransaction.getLocation(),
            "credit_card",
            "luxury", // Different category
            new HashMap<>()
        );
        
        AnomalyResult result = detector.detect(unusualTx);
        // Unusual merchant category should contribute to higher score
        assertTrue(result.getAnomalyScore() > 0.1);
    }

    private void trainModel() {
        for (int i = 0; i < 60; i++) {
            TransactionEvent tx = createTrainingTransaction(i);
            detector.updateModel(tx);
        }
    }

    private TransactionEvent createTrainingTransaction(int index) {
        TransactionEvent.Location location = new TransactionEvent.Location(40.7128, -74.0060, "USA", "New York");
        return new TransactionEvent(
            "tx_train_" + index,
            "user_" + (index % 5), // 5 different users
            "merchant_" + (index % 3), // 3 different merchants
            25.0 + (index % 50), // Amounts between 25-75
            "USD",
            LocalDateTime.of(2024, 1, 15, 10 + (index % 8), 30, 0), // Different hours
            location,
            "credit_card",
            "grocery",
            new HashMap<>()
        );
    }

    private TransactionEvent createTransactionForUser(String userId, double amount, int index) {
        TransactionEvent.Location location = new TransactionEvent.Location(40.7128, -74.0060, "USA", "New York");
        return new TransactionEvent(
            "tx_user_" + index,
            userId,
            "merchant_abc",
            amount,
            "USD",
            LocalDateTime.of(2024, 1, 15, 10 + (index % 8), 30, 0),
            location,
            "credit_card",
            "grocery",
            new HashMap<>()
        );
    }
}