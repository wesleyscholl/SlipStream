package com.slipstream.monitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;

import com.slipstream.model.AnomalyResult;
import com.slipstream.model.TransactionEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Test suite for DashboardServer REST API endpoints
 */
public class DashboardServerTest {
    
    private DashboardServer dashboardServer;
    private MetricsCollector metricsCollector;
    private HttpClient httpClient;
    private ObjectMapper objectMapper;
    private static final int TEST_PORT = 8081;
    
    @BeforeEach
    void setUp() throws IOException {
        metricsCollector = new MetricsCollector();
        dashboardServer = new DashboardServer(metricsCollector, TEST_PORT);
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        // Start the server
        dashboardServer.start();
        
        // Give server time to start
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @AfterEach
    void tearDown() {
        if (dashboardServer != null) {
            dashboardServer.stop();
        }
    }
    
    @Test
    void testMetricsEndpoint() throws Exception {
        // Add some test data
        metricsCollector.recordTransaction(45L);
        AnomalyResult testAnomaly = createTestAnomalyResult("test-1", 0.85, AnomalyResult.AnomalyType.UNUSUAL_AMOUNT);
        metricsCollector.recordAnomaly(testAnomaly);
        
        // Make request to metrics endpoint
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/metrics"))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode());
        assertEquals("application/json", response.headers().firstValue("content-type").orElse(""));
        
        // Parse response JSON
        MetricsCollector.SystemMetrics metrics = objectMapper.readValue(
                response.body(), MetricsCollector.SystemMetrics.class);
        
        assertEquals(1, metrics.getTotalTransactions());
        assertEquals(1, metrics.getTotalAnomalies());
        assertTrue(metrics.getAnomalyRate() > 0);
    }
    
    @Test
    void testHealthEndpoint() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/health"))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("healthy"));
        assertTrue(response.body().contains("timestamp"));
    }
    
    @Test
    void testAnomaliesEndpoint() throws Exception {
        // Add test anomaly
        AnomalyResult testAnomaly = createTestAnomalyResult("test-anomaly", 0.95, AnomalyResult.AnomalyType.FRAUD);
        metricsCollector.recordAnomaly(testAnomaly);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/anomalies"))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("test-anomaly"));
        assertTrue(response.body().contains("FRAUD"));
    }
    
    @Test
    void testDistributionEndpoint() throws Exception {
        // Add test data with different anomaly types
        AnomalyResult anomaly1 = createTestAnomalyResult("test-1", 0.8, AnomalyResult.AnomalyType.UNUSUAL_AMOUNT);
        AnomalyResult anomaly2 = createTestAnomalyResult("test-2", 0.9, AnomalyResult.AnomalyType.FRAUD);
        AnomalyResult anomaly3 = createTestAnomalyResult("test-3", 0.7, AnomalyResult.AnomalyType.UNUSUAL_AMOUNT);
        metricsCollector.recordAnomaly(anomaly1);
        metricsCollector.recordAnomaly(anomaly2);
        metricsCollector.recordAnomaly(anomaly3);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/distribution"))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("unusual_amount"));
        assertTrue(response.body().contains("fraud"));
    }
    
    @Test
    void testDashboardHtmlEndpoint() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/"))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode());
        assertEquals("text/html", response.headers().firstValue("content-type").orElse(""));
        assertTrue(response.body().contains("SlipStream Anomaly Detection Dashboard"));
        assertTrue(response.body().contains("Total Transactions"));
    }
    
    @Test
    void testCorsHeaders() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/metrics"))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
        
        assertEquals("*", response.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
    }
    
    @Test
    void testMethodNotAllowed() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/metrics"))
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
        
        assertEquals(405, response.statusCode());
    }
    
    @Test
    void testNotFoundEndpoint() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/nonexistent"))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
        
        assertEquals(404, response.statusCode());
    }
    
    @Test
    void testServerPortConfiguration() {
        assertEquals(TEST_PORT, dashboardServer.getPort());
    }
    
    private AnomalyResult createTestAnomalyResult(String transactionId, double score, AnomalyResult.AnomalyType type) {
        TransactionEvent fakeTransaction = new TransactionEvent();
        
        return new AnomalyResult(
            transactionId,
            true, // isAnomaly
            score, // anomalyScore
            0.9, // confidence
            type, // anomalyType
            LocalDateTime.now(), // detectedAt
            fakeTransaction, // originalTransaction
            java.util.Map.of("test", score), // featuresUsed
            "Test anomaly" // reason
        );
    }
}