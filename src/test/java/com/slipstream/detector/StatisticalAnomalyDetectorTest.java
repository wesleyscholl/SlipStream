package com.slipstream.detector;

import com.slipstream.model.AnomalyResult;
import com.slipstream.model.TransactionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class StatisticalAnomalyDetectorTest {

    private StatisticalAnomalyDetector detector;
    private TransactionEvent normalTransaction;
    private TransactionEvent suspiciousTransaction;

    @BeforeEach
    void setUp() {
        detector = new StatisticalAnomalyDetector();
        
        // Normal transaction
        TransactionEvent.Location location = new TransactionEvent.Location(40.7128, -74.0060, "USA", "New York");
        normalTransaction = new TransactionEvent(
            "tx_normal",
            "user_123",
            "merchant_grocery",
            50.00,
            "USD",
            LocalDateTime.of(2024, 1, 15, 14, 30, 0),
            location,
            "credit_card",
            "grocery",
            new HashMap<>()
        );

        // Suspicious transaction (large amount, unusual time)
        suspiciousTransaction = new TransactionEvent(
            "tx_suspicious",
            "user_123",
            "merchant_unknown",
            15000.00,
            "USD",
            LocalDateTime.of(2024, 1, 15, 3, 30, 0), // 3:30 AM
            location,
            "credit_card",
            "unknown",
            new HashMap<>()
        );
    }

    @Test
    void testDetectorInitialization() {
        assertNotNull(detector);
        assertEquals("Statistical", detector.getDetectorName());
        assertTrue(detector.supportsOnlineLearning());
        assertFalse(detector.isModelTrained());
        assertEquals(0, detector.getGlobalSampleCount());
    }

    @Test
    void testRuleBasedDetection() {
        // Before model is trained, should use rule-based detection
        AnomalyResult result = detector.detect(suspiciousTransaction);
        
        assertNotNull(result);
        assertEquals("tx_suspicious", result.getTransactionId());
        assertTrue(result.isAnomaly()); // Large amount should trigger rule
        assertNotNull(result.getAnomalyType());
        assertTrue(result.getReason().contains("Rule-based detection"));
    }

    @Test
    void testModelUpdate() {
        int initialSize = detector.getGlobalSampleCount();
        
        detector.updateModel(normalTransaction);
        
        assertEquals(initialSize + 1, detector.getGlobalSampleCount());
        assertEquals(1, detector.getTotalTransactions());
    }

    @Test
    void testMultipleUpdatesAndDetection() {
        // Add some normal transactions to build baseline
        for (int i = 0; i < 60; i++) {
            TransactionEvent.Location location = new TransactionEvent.Location(40.7128, -74.0060, "USA", "New York");
            TransactionEvent tx = new TransactionEvent(
                "tx_" + i,
                "user_123",
                "merchant_grocery",
                50.0 + (i % 20), // Vary amount slightly
                "USD",
                LocalDateTime.of(2024, 1, 15, 10 + (i % 10), 30, 0),
                location,
                "credit_card",
                "grocery",
                new HashMap<>()
            );
            detector.updateModel(tx);
        }

        // Model should be trained now
        assertTrue(detector.isModelTrained());
        assertTrue(detector.getGlobalSampleCount() >= 20);

        // Test detection on suspicious transaction
        AnomalyResult result = detector.detect(suspiciousTransaction);
        assertNotNull(result);
        assertEquals("tx_suspicious", result.getTransactionId());
        
        // Should have higher confidence with trained model
        assertTrue(result.getConfidence() > 0.0);
    }

    @Test
    void testNormalTransactionDetection() {
        AnomalyResult result = detector.detect(normalTransaction);
        
        assertNotNull(result);
        assertEquals("tx_normal", result.getTransactionId());
        assertNotNull(result.getDetectedAt());
        assertEquals(normalTransaction, result.getOriginalTransaction());
    }

    @Test
    void testAnomalyTypeDetection() {
        // Test large amount anomaly
        AnomalyResult result = detector.detect(suspiciousTransaction);
        assertNotEquals(AnomalyResult.AnomalyType.UNKNOWN, result.getAnomalyType());
        
        // Test time-based anomaly
        TransactionEvent.Location location = new TransactionEvent.Location(40.7128, -74.0060, "USA", "New York");
        TransactionEvent lateNightTx = new TransactionEvent(
            "tx_late",
            "user_123",
            "merchant_grocery",
            100.00,
            "USD",
            LocalDateTime.of(2024, 1, 15, 2, 30, 0), // 2:30 AM
            location,
            "credit_card",
            "grocery",
            new HashMap<>()
        );
        
        AnomalyResult timeResult = detector.detect(lateNightTx);
        // Should detect time pattern anomaly
        assertTrue(timeResult.isAnomaly() || timeResult.getAnomalyType() == AnomalyResult.AnomalyType.TIME_PATTERN);
    }

    @Test
    void testFeatureExtraction() {
        // This test verifies that the detector can handle various transaction types
        TransactionEvent.Location location1 = new TransactionEvent.Location(40.7128, -74.0060, "USA", "New York");
        TransactionEvent.Location location2 = new TransactionEvent.Location(34.0522, -118.2437, "USA", "Los Angeles");
        
        TransactionEvent tx1 = new TransactionEvent(
            "tx_1", "user_1", "merchant_1", 100.0, "USD",
            LocalDateTime.now(), location1, "credit_card", "grocery", new HashMap<>()
        );
        
        TransactionEvent tx2 = new TransactionEvent(
            "tx_2", "user_2", "merchant_2", 200.0, "EUR",
            LocalDateTime.now(), location2, "debit_card", "restaurant", new HashMap<>()
        );
        
        // Should not throw exceptions
        assertDoesNotThrow(() -> {
            detector.detect(tx1);
            detector.detect(tx2);
            detector.updateModel(tx1);
            detector.updateModel(tx2);
        });
    }
}