package com.slipstream.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.slipstream.model.AnomalyResult;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Properties;

/**
 * Visual consumer for displaying anomaly detection results in real-time
 * with colored output and formatted display for demo purposes.
 */
public class AnomalyResultConsumer {
    private static final Logger logger = LoggerFactory.getLogger(AnomalyResultConsumer.class);
    
    private final KafkaConsumer<String, String> consumer;
    private final ObjectMapper objectMapper;
    
    // Color codes for console output
    private static final String RESET = "\033[0m";
    private static final String BOLD = "\033[1m";
    private static final String RED = "\033[31m";
    private static final String GREEN = "\033[32m";
    private static final String YELLOW = "\033[33m";
    private static final String BLUE = "\033[34m";
    private static final String PURPLE = "\033[35m";
    private static final String CYAN = "\033[36m";
    private static final String WHITE = "\033[37m";
    private static final String BG_RED = "\033[41m";
    private static final String BG_YELLOW = "\033[43m";
    
    public AnomalyResultConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "anomaly-demo-consumer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        
        this.consumer = new KafkaConsumer<>(props);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    public void startConsumingAnomalies() {
        System.out.println(CYAN + "\n" +
            "╔══════════════════════════════════════════════════════════════╗\n" +
            "║                  🚨 ANOMALY ALERT MONITOR 🚨                 ║\n" +
            "║               Real-Time Fraud Detection System               ║\n" +
            "╚══════════════════════════════════════════════════════════════╝\n" + RESET);
        
        consumer.subscribe(Collections.singletonList("anomaly-alerts"));
        
        System.out.println(BLUE + "👀 Monitoring for anomalies... (Press Ctrl+C to stop)\n" + RESET);
        
        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                
                for (ConsumerRecord<String, String> record : records) {
                    try {
                        AnomalyResult anomaly = objectMapper.readValue(record.value(), AnomalyResult.class);
                        displayAnomaly(anomaly);
                    } catch (Exception e) {
                        logger.error("Error processing anomaly record", e);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in consumer", e);
        } finally {
            consumer.close();
        }
    }
    
    private void displayAnomaly(AnomalyResult anomaly) {
        String timestamp = anomaly.getDetectedAt().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        String severity = getSeverityDisplay(anomaly.getAnomalyType().toString());
        String scoreColor = getScoreColor(anomaly.getAnomalyScore());
        
        System.out.println(BOLD + RED + "🚨 ANOMALY DETECTED 🚨" + RESET);
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.printf("│ %sTime:%s %-10s %sType:%s %-15s %sScore:%s %s%.2f%s │%n", 
            CYAN, RESET, timestamp, 
            YELLOW, RESET, severity,
            WHITE, RESET, scoreColor, anomaly.getAnomalyScore(), RESET);
        System.out.printf("│ %sTransaction:%s %-43s │%n", 
            BLUE, RESET, anomaly.getTransactionId());
        System.out.printf("│ %sUser:%s %-50s │%n", 
            GREEN, RESET, anomaly.getOriginalTransaction().getUserId());
        System.out.printf("│ %sAmount:%s %s$%.2f%s %-42s │%n", 
            PURPLE, RESET, 
            getAmountColor(anomaly.getOriginalTransaction().getAmount()), 
            anomaly.getOriginalTransaction().getAmount(), RESET, "");
        System.out.printf("│ %sConfidence:%s %.1f%% %-43s │%n", 
            CYAN, RESET, anomaly.getConfidence() * 100, "");
        
        // Display reason if available
        if (anomaly.getReason() != null && !anomaly.getReason().isEmpty()) {
            System.out.println("├─────────────────────────────────────────────────────────────┤");
            System.out.println("│ " + YELLOW + BOLD + "Reason:" + RESET + "                                             │");
            System.out.printf("│ %s• %s%-54s%s │%n", 
                RED, RESET, anomaly.getReason(), "");
        }
        
        System.out.println("└─────────────────────────────────────────────────────────────┘");
        System.out.println();
    }
    
    private String getSeverityDisplay(String severity) {
        return switch (severity.toUpperCase()) {
            case "HIGH" -> BG_RED + WHITE + BOLD + " HIGH " + RESET;
            case "MEDIUM" -> BG_YELLOW + " MED " + RESET;
            case "LOW" -> YELLOW + "LOW" + RESET;
            default -> severity;
        };
    }
    
    private String getScoreColor(double score) {
        if (score >= 0.8) return BG_RED + WHITE + BOLD;
        if (score >= 0.6) return RED + BOLD;
        if (score >= 0.4) return YELLOW;
        return GREEN;
    }
    
    private String getAmountColor(double amount) {
        if (amount >= 10000) return BG_RED + WHITE + BOLD;
        if (amount >= 1000) return RED + BOLD;
        if (amount >= 500) return YELLOW;
        return GREEN;
    }
    
    public static void main(String[] args) {
        AnomalyResultConsumer consumer = new AnomalyResultConsumer();
        
        // Add shutdown hook for graceful closure
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println(CYAN + "\n👋 Shutting down anomaly monitor..." + RESET);
        }));
        
        consumer.startConsumingAnomalies();
    }
}