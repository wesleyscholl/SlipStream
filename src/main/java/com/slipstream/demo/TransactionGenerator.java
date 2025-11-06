package com.slipstream.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.slipstream.model.TransactionEvent;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Demo transaction generator that creates realistic transaction data
 * including both normal and anomalous transactions for demonstration purposes.
 */
public class TransactionGenerator {
    private static final Logger logger = LoggerFactory.getLogger(TransactionGenerator.class);
    
    private final KafkaProducer<String, String> producer;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();
    
    // Demo data
    private static final String[] MERCHANTS = {
        "Amazon", "Walmart", "Target", "Starbucks", "McDonald's", 
        "Shell", "Exxon", "CVS", "Walgreens", "Home Depot",
        "Best Buy", "Apple Store", "Netflix", "Spotify", "Uber"
    };
    
    private static final String[] LOCATIONS = {
        "New York, NY", "Los Angeles, CA", "Chicago, IL", "Houston, TX",
        "Phoenix, AZ", "Philadelphia, PA", "San Antonio, TX", "San Diego, CA",
        "Dallas, TX", "San Jose, CA", "Austin, TX", "Jacksonville, FL"
    };
    
    private static final String[] SUSPICIOUS_LOCATIONS = {
        "Moscow, Russia", "Lagos, Nigeria", "Bucharest, Romania",
        "Unknown Location", "VPN_DETECTED", "TOR_EXIT_NODE"
    };
    
    // Color codes for console output
    private static final String RESET = "\033[0m";
    private static final String GREEN = "\033[32m";
    private static final String YELLOW = "\033[33m";
    private static final String RED = "\033[31m";
    private static final String BLUE = "\033[34m";
    private static final String PURPLE = "\033[35m";
    private static final String CYAN = "\033[36m";
    
    public TransactionGenerator() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        
        this.producer = new KafkaProducer<>(props);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    public void generateDemoTransactions(int durationSeconds) {
        System.out.println(CYAN + "\n" +
            "╔══════════════════════════════════════════════════════════════╗\n" +
            "║                    🚀 SLIPSTREAM DEMO 🚀                     ║\n" +
            "║              Real-Time Anomaly Detection System              ║\n" +
            "╚══════════════════════════════════════════════════════════════╝\n" + RESET);
        
        System.out.println(BLUE + "📊 Generating transactions for " + durationSeconds + " seconds...\n" + RESET);
        
        long endTime = System.currentTimeMillis() + (durationSeconds * 1000);
        int transactionCount = 0;
        
        while (System.currentTimeMillis() < endTime) {
            try {
                TransactionEvent transaction;
                
                // Generate different types of transactions with realistic distribution
                double anomalyChance = random.nextDouble();
                
                if (anomalyChance < 0.7) {
                    // 70% normal transactions
                    transaction = generateNormalTransaction();
                    printTransaction(transaction, "NORMAL", GREEN);
                } else if (anomalyChance < 0.8) {
                    // 10% high amount anomalies
                    transaction = generateHighAmountAnomaly();
                    printTransaction(transaction, "HIGH_AMOUNT", RED);
                } else if (anomalyChance < 0.9) {
                    // 10% velocity anomalies
                    transaction = generateVelocityAnomaly();
                    printTransaction(transaction, "VELOCITY", YELLOW);
                } else if (anomalyChance < 0.95) {
                    // 5% location anomalies
                    transaction = generateLocationAnomaly();
                    printTransaction(transaction, "LOCATION", PURPLE);
                } else {
                    // 5% time anomalies
                    transaction = generateTimeAnomaly();
                    printTransaction(transaction, "TIME", CYAN);
                }
                
                // Send to Kafka
                String json = objectMapper.writeValueAsString(transaction);
                ProducerRecord<String, String> record = new ProducerRecord<>(
                    "transaction-events", 
                    transaction.getUserId(), 
                    json
                );
                
                producer.send(record);
                transactionCount++;
                
                // Wait between transactions (realistic pace)
                Thread.sleep(ThreadLocalRandom.current().nextInt(500, 2000));
                
            } catch (Exception e) {
                logger.error("Error generating transaction", e);
            }
        }
        
        System.out.println(GREEN + "\n✅ Demo completed! Generated " + transactionCount + " transactions." + RESET);
        producer.close();
    }
    
    private TransactionEvent generateNormalTransaction() {
        TransactionEvent transaction = new TransactionEvent();
        transaction.setTransactionId(generateTransactionId());
        transaction.setUserId("user_" + random.nextInt(1000));
        transaction.setAmount(ThreadLocalRandom.current().nextDouble(5.0, 200.0));
        transaction.setMerchantId(MERCHANTS[random.nextInt(MERCHANTS.length)]);
        transaction.setLocation(createLocation(LOCATIONS[random.nextInt(LOCATIONS.length)]));
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setPaymentMethod("VISA");
        transaction.setCurrency("USD");
        transaction.setMerchantCategory("RETAIL");
        return transaction;
    }
    
    private TransactionEvent generateHighAmountAnomaly() {
        TransactionEvent transaction = new TransactionEvent();
        transaction.setTransactionId(generateTransactionId());
        transaction.setUserId("user_" + random.nextInt(1000));
        transaction.setAmount(ThreadLocalRandom.current().nextDouble(5000.0, 50000.0));
        transaction.setMerchantId(MERCHANTS[random.nextInt(MERCHANTS.length)]);
        transaction.setLocation(createLocation(LOCATIONS[random.nextInt(LOCATIONS.length)]));
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setPaymentMethod("VISA");
        transaction.setCurrency("USD");
        transaction.setMerchantCategory("RETAIL");
        return transaction;
    }
    
    private TransactionEvent generateVelocityAnomaly() {
        String userId = "user_" + random.nextInt(100); // More likely to be same user
        TransactionEvent transaction = new TransactionEvent();
        transaction.setTransactionId(generateTransactionId());
        transaction.setUserId(userId);
        transaction.setAmount(ThreadLocalRandom.current().nextDouble(50.0, 500.0));
        transaction.setMerchantId(MERCHANTS[random.nextInt(MERCHANTS.length)]);
        transaction.setLocation(createLocation(LOCATIONS[random.nextInt(LOCATIONS.length)]));
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setPaymentMethod("VISA");
        transaction.setCurrency("USD");
        transaction.setMerchantCategory("RETAIL");
        return transaction;
    }
    
    private TransactionEvent generateLocationAnomaly() {
        TransactionEvent transaction = new TransactionEvent();
        transaction.setTransactionId(generateTransactionId());
        transaction.setUserId("user_" + random.nextInt(1000));
        transaction.setAmount(ThreadLocalRandom.current().nextDouble(100.0, 1000.0));
        transaction.setMerchantId(MERCHANTS[random.nextInt(MERCHANTS.length)]);
        transaction.setLocation(createLocation(SUSPICIOUS_LOCATIONS[random.nextInt(SUSPICIOUS_LOCATIONS.length)]));
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setPaymentMethod("VISA");
        transaction.setCurrency("USD");
        transaction.setMerchantCategory("RETAIL");
        return transaction;
    }
    
    private TransactionEvent generateTimeAnomaly() {
        // Generate transaction at 3 AM
        LocalDateTime anomalyTime = LocalDateTime.now()
            .withHour(3)
            .withMinute(random.nextInt(60))
            .withSecond(random.nextInt(60));
            
        TransactionEvent transaction = new TransactionEvent();
        transaction.setTransactionId(generateTransactionId());
        transaction.setUserId("user_" + random.nextInt(1000));
        transaction.setAmount(ThreadLocalRandom.current().nextDouble(100.0, 1000.0));
        transaction.setMerchantId(MERCHANTS[random.nextInt(MERCHANTS.length)]);
        transaction.setLocation(createLocation(LOCATIONS[random.nextInt(LOCATIONS.length)]));
        transaction.setTimestamp(anomalyTime);
        transaction.setPaymentMethod("VISA");
        transaction.setCurrency("USD");
        transaction.setMerchantCategory("RETAIL");
        return transaction;
    }
    
    private TransactionEvent.Location createLocation(String locationString) {
        // Parse location string and create Location object
        String[] parts = locationString.split(", ");
        TransactionEvent.Location location = new TransactionEvent.Location();
        
        if (parts.length >= 2) {
            location.setCity(parts[0]);
            location.setCountry(parts[1]);
        } else {
            location.setCity(locationString);
            location.setCountry("Unknown");
        }
        
        // Add some random coordinates for demo purposes
        location.setLatitude(ThreadLocalRandom.current().nextDouble(-90.0, 90.0));
        location.setLongitude(ThreadLocalRandom.current().nextDouble(-180.0, 180.0));
        
        return location;
    }
    
    private String generateTransactionId() {
        return "TXN-" + System.currentTimeMillis() + "-" + random.nextInt(1000);
    }
    
    private void printTransaction(TransactionEvent transaction, String type, String color) {
        String timestamp = transaction.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String locationStr = transaction.getLocation().getCity() + ", " + transaction.getLocation().getCountry();
        System.out.printf("%s[%s] %s | $%.2f | %s | %s | %s%s%n",
            color,
            timestamp,
            type,
            transaction.getAmount(),
            transaction.getMerchantId(),
            locationStr,
            transaction.getUserId(),
            RESET
        );
    }
    
    public static void main(String[] args) {
        int duration = args.length > 0 ? Integer.parseInt(args[0]) : 30;
        TransactionGenerator generator = new TransactionGenerator();
        generator.generateDemoTransactions(duration);
    }
}