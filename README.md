# SlipStream - Kafka-Based Real-Time Anomaly Detector

SlipStream is a high-performance, real-time anomaly detection system built in Java for detecting fraud and anomalies in streaming transaction data using Apache Kafka and machine learning.

## Features

- **Real-time Processing**: Built on Apache Kafka Streams for low-latency stream processing
- **Machine Learning**: Uses Isolation Forest algorithm (via Smile ML) for anomaly detection
- **Scalable Architecture**: Horizontally scalable with Kafka's distributed processing
- **Online Learning**: Adapts to new patterns with continuous model updates
- **Multiple Anomaly Types**: Detects various types of anomalies (fraud, unusual amounts, time patterns, etc.)
- **High Throughput**: Optimized for processing thousands of transactions per second
- **Monitoring**: Built-in metrics and logging for observability

## Architecture

```
[Transaction Stream] → [Kafka Input Topic] → [SlipStream Processor] → [Kafka Output Topics]
                                                     ↓
                                            [Isolation Forest ML Model]
                                                     ↓
                                            [Anomaly Classification]
                                                     ↓
                                    [Normal Results]    [Anomaly Alerts]
```

## Technology Stack

- **Java 17**: Modern Java with performance optimizations
- **Apache Kafka Streams**: Stream processing framework
- **Smile ML**: Machine learning library for anomaly detection
- **Jackson**: JSON serialization/deserialization
- **SLF4J + Logback**: Logging framework
- **JUnit 5**: Testing framework
- **Maven**: Build and dependency management

## Quick Start

### Prerequisites

- Java 17 or higher
- Apache Kafka 3.6+ running on localhost:9092
- Maven 3.6+

### 1. Build the Project

```bash
mvn clean compile
```

### 2. Run Tests

```bash
mvn test
```

### 3. Start Kafka (if not already running)

```bash
# Start Zookeeper
bin/zookeeper-server-start.sh config/zookeeper.properties

# Start Kafka
bin/kafka-server-start.sh config/server.properties
```

### 4. Create Required Topics

```bash
# Input topic for transactions
kafka-topics.sh --create --topic transactions --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1

# Output topic for all results
kafka-topics.sh --create --topic anomalies --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1

# Alerts topic for anomalies only
kafka-topics.sh --create --topic alerts --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
```

### 5. Run SlipStream

```bash
mvn exec:java -Dexec.mainClass="com.slipstream.SlipStreamApplication"
```

Or build and run the JAR:

```bash
mvn package
java -jar target/slipstream-anomaly-detector-1.0.0-SNAPSHOT.jar
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap servers | `localhost:9092` |
| `KAFKA_INPUT_TOPIC` | Input topic for transactions | `transactions` |
| `KAFKA_OUTPUT_TOPIC` | Output topic for all results | `anomalies` |
| `KAFKA_ALERTS_TOPIC` | Alerts topic for anomalies | `alerts` |
| `KAFKA_NUM_THREADS` | Number of stream threads | `1` |
| `KAFKA_STATE_DIR` | Directory for state stores | `/tmp/kafka-streams` |

### Application Properties

Configuration can also be set in `src/main/resources/application.properties`:

```properties
kafka.bootstrap.servers=localhost:9092
kafka.input.topic=transactions
kafka.output.topic=anomalies
kafka.alerts.topic=alerts
detector.isolation_forest.num_trees=100
detector.isolation_forest.anomaly_threshold=0.6
```

## Data Format

### Input Transaction Format

```json
{
  "transaction_id": "tx_12345",
  "user_id": "user_67890",
  "merchant_id": "merchant_abc",
  "amount": 150.75,
  "currency": "USD",
  "timestamp": "2024-01-15T14:30:00",
  "location": {
    "latitude": 40.7128,
    "longitude": -74.0060,
    "country": "USA",
    "city": "New York"
  },
  "payment_method": "credit_card",
  "merchant_category": "grocery",
  "metadata": {
    "device_id": "mobile_123",
    "session_id": "sess_456"
  }
}
```

### Output Anomaly Result Format

```json
{
  "transaction_id": "tx_12345",
  "is_anomaly": true,
  "anomaly_score": 0.85,
  "confidence": 0.92,
  "anomaly_type": "unusual_amount",
  "detected_at": "2024-01-15T14:30:05",
  "original_transaction": { ... },
  "features_used": {
    "amount": 150.75,
    "hour_of_day": 14,
    "amount_ratio": 3.2
  },
  "reason": "Anomaly score: 0.850, Type: unusual_amount, Large transaction amount: $150.75"
}
```

## Testing

### Send Test Transactions

You can use the Kafka console producer to send test transactions:

```bash
kafka-console-producer.sh --topic transactions --bootstrap-server localhost:9092
```

Then paste JSON transactions like:

```json
{"transaction_id":"tx_001","user_id":"user_123","merchant_id":"merchant_grocery","amount":50.0,"currency":"USD","timestamp":"2024-01-15T14:30:00","location":{"latitude":40.7128,"longitude":-74.0060,"country":"USA","city":"New York"},"payment_method":"credit_card","merchant_category":"grocery","metadata":{}}
```

### Monitor Results

```bash
# Monitor all results
kafka-console-consumer.sh --topic anomalies --bootstrap-server localhost:9092 --from-beginning

# Monitor alerts only
kafka-console-consumer.sh --topic alerts --bootstrap-server localhost:9092 --from-beginning
```

## Anomaly Types

SlipStream detects several types of anomalies:

- **FRAUD**: General fraud patterns
- **UNUSUAL_AMOUNT**: Transactions with abnormally high amounts
- **VELOCITY**: High frequency of transactions from same user
- **LOCATION**: Transactions from unusual locations
- **TIME_PATTERN**: Transactions at unusual times
- **MERCHANT_PATTERN**: Unusual merchant interaction patterns
- **STATISTICAL_OUTLIER**: General statistical anomalies

## Performance Tuning

### Kafka Streams Configuration

```properties
# Increase processing threads for higher throughput
KAFKA_NUM_THREADS=4

# Adjust commit interval for latency vs. throughput trade-off
kafka.commit.interval.ms=10000

# State store caching for better performance
kafka.cache.max.bytes.buffering=10485760
```

### JVM Tuning

```bash
java -Xmx4g -Xms2g -XX:+UseG1GC -jar slipstream.jar
```

## Monitoring and Metrics

SlipStream provides built-in metrics logging every 30 seconds:

- Stream processing state
- Training data size
- Model training status
- Total transactions processed
- JVM memory usage

## Development

### Project Structure

```
src/
├── main/java/com/slipstream/
│   ├── SlipStreamApplication.java          # Main application
│   ├── config/
│   │   └── KafkaConfig.java               # Kafka configuration
│   ├── detector/
│   │   ├── AnomalyDetector.java           # Detector interface
│   │   └── IsolationForestDetector.java   # ML-based detector
│   ├── model/
│   │   ├── TransactionEvent.java          # Input data model
│   │   └── AnomalyResult.java             # Output data model
│   └── stream/
│       └── AnomalyDetectionStreams.java   # Kafka Streams topology
├── main/resources/
│   ├── application.properties             # Configuration
│   └── logback.xml                        # Logging configuration
└── test/java/                             # Unit tests
```

### Building and Testing

```bash
# Compile
mvn compile

# Run tests
mvn test

# Package
mvn package

# Run integration tests (requires running Kafka)
mvn verify
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## Support

For questions or issues, please open a GitHub issue or contact the development team.