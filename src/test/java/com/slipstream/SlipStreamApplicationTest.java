package com.slipstream;

import com.slipstream.config.EnvironmentVariableProvider;
import com.slipstream.config.KafkaConfig;
import com.slipstream.stream.AnomalyDetectionStreams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SlipStreamApplicationTest {

    private EnvironmentVariableProvider environmentProvider;
    private SlipStreamApplication application;
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;

    @BeforeEach
    void setUp() {
        environmentProvider = mock(EnvironmentVariableProvider.class);
        
        // Redirect output streams for testing
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        
        if (application != null) {
            try {
                application.stop();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }

    @Test
    void testApplicationCreation() {
        application = new SlipStreamApplication();
        
        assertNotNull(application);
        assertNotNull(application.getKafkaConfig());
        assertNotNull(application.getAnomalyStreams());
        
        // Verify default configuration
        KafkaConfig config = application.getKafkaConfig();
        assertEquals("localhost:9092", config.getBootstrapServers());
        assertEquals("transactions", config.getInputTopic());
        assertEquals("anomalies", config.getOutputTopic());
        assertEquals("alerts", config.getAlertsTopic());
    }

    @Test
    void testConfigurationFromEnvironmentVariables() {
        // Mock environment variables
        when(environmentProvider.getenv("KAFKA_BOOTSTRAP_SERVERS")).thenReturn("test-server:9092");
        when(environmentProvider.getenv("KAFKA_INPUT_TOPIC")).thenReturn("test-input");
        when(environmentProvider.getenv("KAFKA_OUTPUT_TOPIC")).thenReturn("test-output");
        when(environmentProvider.getenv("KAFKA_ALERTS_TOPIC")).thenReturn("test-alerts");
        when(environmentProvider.getenv("KAFKA_NUM_THREADS")).thenReturn("4");
        when(environmentProvider.getenv("KAFKA_STATE_DIR")).thenReturn("/test/state");
        
        application = new SlipStreamApplication(environmentProvider);
        KafkaConfig config = application.getKafkaConfig();
        
        assertEquals("test-server:9092", config.getBootstrapServers());
        assertEquals("test-input", config.getInputTopic());
        assertEquals("test-output", config.getOutputTopic());
        assertEquals("test-alerts", config.getAlertsTopic());
        assertEquals(4, config.getNumStreamThreads());
        assertEquals("/test/state", config.getStateDir());
    }

        @Test
    void testInvalidNumThreadsEnvironmentVariable() {
        when(environmentProvider.getenv("KAFKA_NUM_THREADS")).thenReturn("invalid");
        
        application = new SlipStreamApplication(environmentProvider);
        KafkaConfig config = application.getKafkaConfig();
        
        // Should use default when parsing fails
        assertEquals(1, config.getNumStreamThreads()); // Default value
    }

    @Test
    void testStartAndStop() {
        application = new SlipStreamApplication();
        
        // Mock the streams to avoid actual Kafka connection
        AnomalyDetectionStreams mockStreams = mock(AnomalyDetectionStreams.class);
        
        // Use reflection to replace the streams instance for testing
        try {
            java.lang.reflect.Field streamsField = SlipStreamApplication.class.getDeclaredField("anomalyStreams");
            streamsField.setAccessible(true);
            streamsField.set(application, mockStreams);
            
            // Test start
            application.start();
            verify(mockStreams).start();
            
            // Test stop
            application.stop();
            verify(mockStreams).stop();
            
        } catch (Exception e) {
            fail("Failed to test start/stop: " + e.getMessage());
        }
    }

    @Test
    void testStartWithException() {
        application = new SlipStreamApplication();
        
        // Mock the streams to throw exception on start
        AnomalyDetectionStreams mockStreams = mock(AnomalyDetectionStreams.class);
        doThrow(new RuntimeException("Kafka connection failed")).when(mockStreams).start();
        
        // Use reflection to replace the streams instance
        try {
            java.lang.reflect.Field streamsField = SlipStreamApplication.class.getDeclaredField("anomalyStreams");
            streamsField.setAccessible(true);
            streamsField.set(application, mockStreams);
            
            // Should throw RuntimeException
            assertThrows(RuntimeException.class, () -> application.start());
            
        } catch (Exception e) {
            fail("Failed to test start exception: " + e.getMessage());
        }
    }

    @Test
    void testStopWithException() {
        application = new SlipStreamApplication();
        
        // Mock the streams and scheduler to test exception handling
        AnomalyDetectionStreams mockStreams = mock(AnomalyDetectionStreams.class);
        doThrow(new RuntimeException("Stop failed")).when(mockStreams).stop();
        
        try {
            java.lang.reflect.Field streamsField = SlipStreamApplication.class.getDeclaredField("anomalyStreams");
            streamsField.setAccessible(true);
            streamsField.set(application, mockStreams);
            
            // Should not throw exception (graceful handling)
            assertDoesNotThrow(() -> application.stop());
            
        } catch (Exception e) {
            fail("Failed to test stop exception: " + e.getMessage());
        }
    }

    @Test
    void testGetters() {
        application = new SlipStreamApplication();
        
        assertNotNull(application.getKafkaConfig());
        assertNotNull(application.getAnomalyStreams());
        
        // Verify they are the correct instances
        assertTrue(application.getKafkaConfig() instanceof KafkaConfig);
        assertTrue(application.getAnomalyStreams() instanceof AnomalyDetectionStreams);
    }

    @Test
    void testMainMethod() {
        // Test that the application can be created without environment variables
        // (which is what the main method does)
        assertDoesNotThrow(() -> new SlipStreamApplication());
        
        // Verify application initialization with environment provider works
        when(environmentProvider.getenv(anyString())).thenReturn(null);
        assertDoesNotThrow(() -> new SlipStreamApplication(environmentProvider));
    }

    @Test
    void testConfigurationLogging() {
        application = new SlipStreamApplication();
        
        // The constructor should log configuration details
        // We can't easily verify log output in unit tests without a logging framework mock
        // But we can verify the application was created successfully with configuration
        KafkaConfig config = application.getKafkaConfig();
        assertNotNull(config.getBootstrapServers());
        assertNotNull(config.getInputTopic());
        assertNotNull(config.getOutputTopic());
        assertNotNull(config.getAlertsTopic());
    }
}