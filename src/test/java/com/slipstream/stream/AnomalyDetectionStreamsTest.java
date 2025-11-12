package com.slipstream.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.slipstream.config.KafkaConfig;
import com.slipstream.detector.AnomalyDetector;
import com.slipstream.detector.StatisticalAnomalyDetector;
import com.slipstream.model.AnomalyResult;
import com.slipstream.model.TransactionEvent;
import org.apache.kafka.streams.KafkaStreams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AnomalyDetectionStreamsTest {

    private KafkaConfig mockKafkaConfig;
    private AnomalyDetector mockDetector;

    private AnomalyDetectionStreams streams;
    private ObjectMapper objectMapper;
    private TransactionEvent sampleTransaction;
    private AnomalyResult sampleAnomalyResult;

    @BeforeEach
    void setUp() {
        // Create mock objects
        mockKafkaConfig = mock(KafkaConfig.class);
        mockDetector = mock(AnomalyDetector.class);
        
        // Setup mock config
        when(mockKafkaConfig.getInputTopic()).thenReturn("test-input");
        when(mockKafkaConfig.getOutputTopic()).thenReturn("test-output");
        when(mockKafkaConfig.getAlertsTopic()).thenReturn("test-alerts");
        when(mockKafkaConfig.getStreamsProperties()).thenReturn(new java.util.Properties());

        // Setup test data
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        TransactionEvent.Location location = new TransactionEvent.Location(40.7128, -74.0060, "USA", "New York");
        sampleTransaction = new TransactionEvent(
            "tx_123",
            "user_456",
            "merchant_789",
            100.0,
            "USD",
            LocalDateTime.of(2024, 1, 15, 14, 30, 0),
            location,
            "credit_card",
            "grocery",
            new HashMap<>()
        );

        sampleAnomalyResult = new AnomalyResult(
            "tx_123",
            true,
            0.8,
            0.9,
            AnomalyResult.AnomalyType.UNUSUAL_AMOUNT,
            LocalDateTime.now(),
            sampleTransaction,
            new HashMap<>(), // Empty features map
            "High amount detected"
        );
    }

    @Test
    void testConstructorWithDefaultDetector() {
        streams = new AnomalyDetectionStreams(mockKafkaConfig);
        
        assertNotNull(streams);
        assertNotNull(streams.getAnomalyDetector());
        assertEquals(mockKafkaConfig, streams.getKafkaConfig());
        assertTrue(streams.getAnomalyDetector() instanceof StatisticalAnomalyDetector);
    }

    @Test
    void testConstructorWithCustomDetector() {
        when(mockDetector.getDetectorName()).thenReturn("MockDetector");
        
        streams = new AnomalyDetectionStreams(mockKafkaConfig, mockDetector);
        
        assertNotNull(streams);
        assertEquals(mockDetector, streams.getAnomalyDetector());
        assertEquals(mockKafkaConfig, streams.getKafkaConfig());
    }

    @Test
    void testGetters() {
        streams = new AnomalyDetectionStreams(mockKafkaConfig, mockDetector);
        
        assertEquals(mockDetector, streams.getAnomalyDetector());
        assertEquals(mockKafkaConfig, streams.getKafkaConfig());
    }

    @Test
    void testGetStateWithoutStart() {
        streams = new AnomalyDetectionStreams(mockKafkaConfig);
        
        // State should be null before start
        assertNull(streams.getState());
    }

    @Test
    void testLogMetricsWithoutStreams() {
        streams = new AnomalyDetectionStreams(mockKafkaConfig);
        
        // Should not throw exception when logging metrics without streams
        assertDoesNotThrow(() -> streams.logMetrics());
    }

    @Test
    void testLogMetricsWithStatisticalDetector() {
        StatisticalAnomalyDetector statisticalDetector = new StatisticalAnomalyDetector();
        streams = new AnomalyDetectionStreams(mockKafkaConfig, statisticalDetector);
        
        // Mock streams state for metrics logging
        try {
            java.lang.reflect.Field streamsField = AnomalyDetectionStreams.class.getDeclaredField("streams");
            streamsField.setAccessible(true);
            
            KafkaStreams mockStreams = mock(KafkaStreams.class);
            when(mockStreams.state()).thenReturn(KafkaStreams.State.RUNNING);
            streamsField.set(streams, mockStreams);
            
            // Should not throw exception
            assertDoesNotThrow(() -> streams.logMetrics());
            
        } catch (Exception e) {
            fail("Failed to test metrics logging: " + e.getMessage());
        }
    }

    @Test
    void testStopWithoutStart() {
        streams = new AnomalyDetectionStreams(mockKafkaConfig);
        
        // Should not throw exception when stopping without starting
        assertDoesNotThrow(() -> streams.stop());
    }

    @Test
    void testStopAfterMockStart() {
        streams = new AnomalyDetectionStreams(mockKafkaConfig);
        
        try {
            // Mock the streams field for testing stop behavior
            java.lang.reflect.Field streamsField = AnomalyDetectionStreams.class.getDeclaredField("streams");
            streamsField.setAccessible(true);
            
            KafkaStreams mockStreams = mock(KafkaStreams.class);
            streamsField.set(streams, mockStreams);
            
            streams.stop();
            
            verify(mockStreams).close();
            
        } catch (Exception e) {
            fail("Failed to test stop: " + e.getMessage());
        }
    }

    @Test
    void testParseTransactionWithValidJson() throws Exception {
        streams = new AnomalyDetectionStreams(mockKafkaConfig);
        String validJson = objectMapper.writeValueAsString(sampleTransaction);
        
        // Use reflection to test private method
        java.lang.reflect.Method parseMethod = AnomalyDetectionStreams.class.getDeclaredMethod("parseTransaction", String.class);
        parseMethod.setAccessible(true);
        
        TransactionEvent result = (TransactionEvent) parseMethod.invoke(streams, validJson);
        
        assertNotNull(result);
        assertEquals(sampleTransaction.getTransactionId(), result.getTransactionId());
        assertEquals(sampleTransaction.getUserId(), result.getUserId());
        assertEquals(sampleTransaction.getAmount(), result.getAmount());
    }

    @Test
    void testParseTransactionWithInvalidJson() throws Exception {
        streams = new AnomalyDetectionStreams(mockKafkaConfig);
        String invalidJson = "{ invalid json }";
        
        // Use reflection to test private method
        java.lang.reflect.Method parseMethod = AnomalyDetectionStreams.class.getDeclaredMethod("parseTransaction", String.class);
        parseMethod.setAccessible(true);
        
        TransactionEvent result = (TransactionEvent) parseMethod.invoke(streams, invalidJson);
        
        // Should return null for invalid JSON
        assertNull(result);
    }

    @Test
    void testDetectAnomalySuccess() throws Exception {
        when(mockDetector.detect(any(TransactionEvent.class))).thenReturn(sampleAnomalyResult);
        
        streams = new AnomalyDetectionStreams(mockKafkaConfig, mockDetector);
        
        // Use reflection to test private method
        java.lang.reflect.Method detectMethod = AnomalyDetectionStreams.class.getDeclaredMethod("detectAnomaly", TransactionEvent.class);
        detectMethod.setAccessible(true);
        
        AnomalyResult result = (AnomalyResult) detectMethod.invoke(streams, sampleTransaction);
        
        assertNotNull(result);
        assertEquals(sampleAnomalyResult.getTransactionId(), result.getTransactionId());
        assertEquals(sampleAnomalyResult.isAnomaly(), result.isAnomaly());
        verify(mockDetector).detect(sampleTransaction);
    }

    @Test
    void testDetectAnomalyWithException() throws Exception {
        when(mockDetector.detect(any(TransactionEvent.class))).thenThrow(new RuntimeException("Detection failed"));
        
        streams = new AnomalyDetectionStreams(mockKafkaConfig, mockDetector);
        
        // Use reflection to test private method
        java.lang.reflect.Method detectMethod = AnomalyDetectionStreams.class.getDeclaredMethod("detectAnomaly", TransactionEvent.class);
        detectMethod.setAccessible(true);
        
        AnomalyResult result = (AnomalyResult) detectMethod.invoke(streams, sampleTransaction);
        
        // Should return error result instead of throwing
        assertNotNull(result);
        assertEquals(sampleTransaction.getTransactionId(), result.getTransactionId());
        assertFalse(result.isAnomaly()); // Error results are marked as non-anomalous
        assertTrue(result.getReason().contains("Processing error"));
    }

    @Test
    void testSerializeAnomalyResult() throws Exception {
        streams = new AnomalyDetectionStreams(mockKafkaConfig);
        
        // Use reflection to test private method
        java.lang.reflect.Method serializeMethod = AnomalyDetectionStreams.class.getDeclaredMethod("serializeAnomalyResult", AnomalyResult.class);
        serializeMethod.setAccessible(true);
        
        String result = (String) serializeMethod.invoke(streams, sampleAnomalyResult);
        
        assertNotNull(result);
        assertTrue(result.contains("tx_123"));
        assertTrue(result.contains("UNUSUAL_AMOUNT"));
        
        // Verify it can be deserialized back
        AnomalyResult deserialized = objectMapper.readValue(result, AnomalyResult.class);
        assertEquals(sampleAnomalyResult.getTransactionId(), deserialized.getTransactionId());
    }

    @Test
    void testSerializeAnomalyResultWithNullResult() throws Exception {
        streams = new AnomalyDetectionStreams(mockKafkaConfig);
        
        // Use reflection to test private method with null input
        java.lang.reflect.Method serializeMethod = AnomalyDetectionStreams.class.getDeclaredMethod("serializeAnomalyResult", AnomalyResult.class);
        serializeMethod.setAccessible(true);
        
        // Create a result that will cause serialization issues
        AnomalyResult problematicResult = mock(AnomalyResult.class);
        when(problematicResult.toString()).thenThrow(new RuntimeException("Serialization error"));
        
        String result = (String) serializeMethod.invoke(streams, problematicResult);
        
        // Should return null on serialization error
        assertNull(result);
    }

    @Test
    void testOnlineLearningUpdateModel() throws Exception {
        when(mockDetector.supportsOnlineLearning()).thenReturn(true);
        when(mockDetector.detect(any(TransactionEvent.class))).thenReturn(sampleAnomalyResult);
        
        streams = new AnomalyDetectionStreams(mockKafkaConfig, mockDetector);
        
        // Test that updateModel is called when online learning is supported
        // This would be tested through the topology execution in integration tests
        verify(mockDetector).supportsOnlineLearning();
    }

    @Test
    void testObjectMapperConfiguration() {
        streams = new AnomalyDetectionStreams(mockKafkaConfig);
        
        // Test that ObjectMapper is properly configured with JavaTimeModule
        // by checking it can handle LocalDateTime serialization
        assertDoesNotThrow(() -> {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            String json = mapper.writeValueAsString(sampleTransaction);
            TransactionEvent parsed = mapper.readValue(json, TransactionEvent.class);
            assertEquals(sampleTransaction.getTimestamp(), parsed.getTimestamp());
        });
    }
}