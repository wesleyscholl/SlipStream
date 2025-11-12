package com.slipstream.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnomalyResultTest {

    private ObjectMapper objectMapper;
    private TransactionEvent sampleTransaction;
    private AnomalyResult sampleAnomalyResult;
    private AnomalyResult normalResult;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Create sample transaction
        TransactionEvent.Location location = new TransactionEvent.Location(40.7128, -74.0060, "USA", "New York");
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("device_id", "mobile_123");

        sampleTransaction = new TransactionEvent(
            "tx_12345",
            "user_67890",
            "merchant_abc",
            150.75,
            "USD",
            LocalDateTime.of(2024, 1, 15, 14, 30, 0),
            location,
            "credit_card",
            "grocery",
            metadata
        );

        // Create anomaly result
        Map<String, Double> features = new HashMap<>();
        features.put("amount", 150.75);
        features.put("hour_of_day", 14.0);

        sampleAnomalyResult = new AnomalyResult(
            "tx_12345",
            true,
            0.85,
            0.92,
            AnomalyResult.AnomalyType.UNUSUAL_AMOUNT,
            LocalDateTime.of(2024, 1, 15, 14, 30, 5),
            sampleTransaction,
            features,
            "Large transaction amount detected"
        );

        // Create normal result
        normalResult = new AnomalyResult(
            "tx_normal",
            false,
            0.15,
            0.95,
            AnomalyResult.AnomalyType.UNKNOWN,
            LocalDateTime.of(2024, 1, 15, 14, 30, 5),
            sampleTransaction,
            features,
            "Normal transaction"
        );
    }

    @Test
    void testAnomalyResultCreation() {
        assertNotNull(sampleAnomalyResult);
        assertEquals("tx_12345", sampleAnomalyResult.getTransactionId());
        assertTrue(sampleAnomalyResult.isAnomaly());
        assertEquals(0.85, sampleAnomalyResult.getAnomalyScore(), 0.01);
        assertEquals(0.92, sampleAnomalyResult.getConfidence(), 0.01);
        assertEquals(AnomalyResult.AnomalyType.UNUSUAL_AMOUNT, sampleAnomalyResult.getAnomalyType());
        assertEquals(LocalDateTime.of(2024, 1, 15, 14, 30, 5), sampleAnomalyResult.getDetectedAt());
        assertEquals(sampleTransaction, sampleAnomalyResult.getOriginalTransaction());
        assertNotNull(sampleAnomalyResult.getFeaturesUsed());
        assertEquals("Large transaction amount detected", sampleAnomalyResult.getReason());
    }

    @Test
    void testNormalResult() {
        assertFalse(normalResult.isAnomaly());
        assertEquals(0.15, normalResult.getAnomalyScore(), 0.01);
        assertEquals(AnomalyResult.AnomalyType.UNKNOWN, normalResult.getAnomalyType());
        assertEquals("Normal transaction", normalResult.getReason());
    }

    @Test
    void testAnomalyTypeEnum() {
        AnomalyResult.AnomalyType[] types = AnomalyResult.AnomalyType.values();
        
        assertTrue(types.length >= 7);
        assertTrue(java.util.Arrays.asList(types).contains(AnomalyResult.AnomalyType.FRAUD));
        assertTrue(java.util.Arrays.asList(types).contains(AnomalyResult.AnomalyType.UNUSUAL_AMOUNT));
        assertTrue(java.util.Arrays.asList(types).contains(AnomalyResult.AnomalyType.VELOCITY));
        assertTrue(java.util.Arrays.asList(types).contains(AnomalyResult.AnomalyType.LOCATION));
        assertTrue(java.util.Arrays.asList(types).contains(AnomalyResult.AnomalyType.TIME_PATTERN));
        assertTrue(java.util.Arrays.asList(types).contains(AnomalyResult.AnomalyType.MERCHANT_PATTERN));
        assertTrue(java.util.Arrays.asList(types).contains(AnomalyResult.AnomalyType.STATISTICAL_OUTLIER));
        assertTrue(java.util.Arrays.asList(types).contains(AnomalyResult.AnomalyType.UNKNOWN));
    }

    @Test
    void testJsonSerialization() throws Exception {
        String json = objectMapper.writeValueAsString(sampleAnomalyResult);
        
        assertNotNull(json);
        assertTrue(json.contains("tx_12345"));
        assertTrue(json.contains("unusual_amount"));
        assertTrue(json.contains("0.85"));
        assertTrue(json.contains("0.92"));
        assertTrue(json.contains("Large transaction amount detected"));
    }

    @Test
    void testEqualsAndHashCode() {
        AnomalyResult identical = new AnomalyResult(
            "tx_12345",
            true,
            0.85,
            0.92,
            AnomalyResult.AnomalyType.UNUSUAL_AMOUNT,
            LocalDateTime.of(2024, 1, 15, 14, 30, 5),
            sampleTransaction,
            sampleAnomalyResult.getFeaturesUsed(),
            "Large transaction amount detected"
        );

        assertEquals(sampleAnomalyResult, identical);
        assertEquals(sampleAnomalyResult.hashCode(), identical.hashCode());
        
        // Test inequality
        assertNotEquals(sampleAnomalyResult, normalResult);
        assertNotEquals(sampleAnomalyResult.hashCode(), normalResult.hashCode());
    }

    @Test
    void testToString() {
        String toString = sampleAnomalyResult.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("AnomalyResult"));
        assertTrue(toString.contains("tx_12345"));
        assertTrue(toString.contains("unusual_amount"));
        assertTrue(toString.contains("true")); // isAnomaly
    }

    @Test
    void testResultWithNullFields() {
        AnomalyResult resultWithNulls = new AnomalyResult(
            "tx_null",
            false,
            0.0,
            0.0,
            AnomalyResult.AnomalyType.UNKNOWN,
            LocalDateTime.now(),
            null, // null transaction
            null, // null features
            null  // null reason
        );
        
        assertNotNull(resultWithNulls);
        assertEquals("tx_null", resultWithNulls.getTransactionId());
        assertNull(resultWithNulls.getOriginalTransaction());
        assertNull(resultWithNulls.getFeaturesUsed());
        assertNull(resultWithNulls.getReason());
    }

    @Test
    void testResultWithEmptyFeatures() {
        Map<String, Double> emptyFeatures = new HashMap<>();
        AnomalyResult resultWithEmptyFeatures = new AnomalyResult(
            "tx_empty",
            false,
            0.1,
            0.8,
            AnomalyResult.AnomalyType.UNKNOWN,
            LocalDateTime.now(),
            sampleTransaction,
            emptyFeatures,
            "No features"
        );
        
        assertNotNull(resultWithEmptyFeatures);
        assertEquals(0, resultWithEmptyFeatures.getFeaturesUsed().size());
    }

    @Test
    void testHighConfidenceAnomaly() {
        AnomalyResult highConfidence = new AnomalyResult(
            "tx_high",
            true,
            0.95,
            0.99,
            AnomalyResult.AnomalyType.FRAUD,
            LocalDateTime.now(),
            sampleTransaction,
            new HashMap<>(),
            "High confidence fraud detection"
        );
        
        assertTrue(highConfidence.isAnomaly());
        assertTrue(highConfidence.getAnomalyScore() > 0.9);
        assertTrue(highConfidence.getConfidence() > 0.95);
        assertEquals(AnomalyResult.AnomalyType.FRAUD, highConfidence.getAnomalyType());
    }

    @Test
    void testGettersAndSetters() {
        AnomalyResult result = new AnomalyResult(
            "test",
            false,
            0.0,
            0.0,
            AnomalyResult.AnomalyType.UNKNOWN,
            LocalDateTime.now(),
            null,
            null,
            null
        );
        
        // Test basic getters work
        assertEquals("test", result.getTransactionId());
        assertFalse(result.isAnomaly());
        assertEquals(0.0, result.getAnomalyScore());
        assertEquals(0.0, result.getConfidence());
        assertEquals(AnomalyResult.AnomalyType.UNKNOWN, result.getAnomalyType());
        assertNotNull(result.getDetectedAt());
    }
}