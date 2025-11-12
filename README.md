# ⚡️ SlipStream - Kafka-Based Real-Time Anomaly Detector

**Status**: Enterprise-grade fraud detection system with real-time streaming analytics - production-ready Java application for financial security workflows.

> 🔍 **A high-performance, real-time anomaly detection system** built in Java for detecting fraud and anomalies in streaming transaction data using Apache Kafka and statistical machine learning.

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.java.net/)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-3.6+-blue.svg)](https://kafka.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Build](https://img.shields.io/badge/Build-Maven-red.svg)](https://maven.apache.org/)

## 🎥 Demo
![SlipStreamDemo](https://github.com/user-attachments/assets/118edb6b-9f98-43aa-b474-a116b61c531e)


## ✨ Features

- 🚀 **Real-time Processing**: Built on Apache Kafka Streams for low-latency stream processing
- 🤖 **Statistical ML**: Uses advanced statistical algorithms for anomaly detection  
- 📈 **Scalable Architecture**: Horizontally scalable with Kafka's distributed processing
- 🧠 **Adaptive Learning**: Adapts to new patterns with continuous model updates
- 🎯 **Multiple Anomaly Types**: Detects various types of anomalies (fraud, unusual amounts, time patterns, etc.)
- ⚡ **High Throughput**: Optimized for processing thousands of transactions per second
- 📊 **Built-in Monitoring**: Real-time metrics and comprehensive logging for observability
- 🔧 **Easy Configuration**: Environment variables and properties-based configuration
- 🛡️ **Production Ready**: Comprehensive error handling and graceful shutdown
- 🎨 **Visual Demo**: Beautiful colored output for presentations and demos

## 🏗️ Architecture

```
📡 [Transaction Stream] → 📥 [Kafka Input Topic] → 🔄 [SlipStream Processor] → 📤 [Kafka Output Topics]
                                                            ↓
                                                   🧠 [Statistical ML Engine]
                                                            ↓
                                                   🎯 [Anomaly Classification]
                                                            ↓
                                            ✅ [Normal Results]    🚨 [Anomaly Alerts]
```

### 🔄 Processing Flow

1. **📨 Data Ingestion**: Transactions stream into Kafka topics in real-time
2. **🔍 Feature Extraction**: Extract relevant features (amount, location, time, velocity)
3. **🧮 Statistical Analysis**: Apply Z-score analysis and composite scoring
4. **🎯 Anomaly Detection**: Identify outliers using configurable thresholds
5. **📊 Result Classification**: Route normal vs anomalous transactions
6. **🚨 Alert Generation**: Send high-confidence anomalies to alert topics

## 🛠️ Technology Stack

- ☕ **Java 17**: Modern Java with performance optimizations and latest features
- 🌊 **Apache Kafka Streams**: Stream processing framework for real-time data
- 📊 **Apache Commons Math**: Statistical functions for anomaly detection
- 🔄 **Jackson**: High-performance JSON serialization/deserialization
- 📝 **SLF4J + Logback**: Comprehensive logging framework with file rotation
- ✅ **JUnit 5**: Modern testing framework with comprehensive test coverage
- 🔧 **Maven**: Build automation and dependency management
- 🐳 **Docker Compose**: Containerized infrastructure setup
- 🎨 **ANSI Colors**: Beautiful terminal output for demos

## 🎬 Visual Demo

**Perfect for presentations and recording!**

Experience SlipStream's real-time anomaly detection with our interactive visual demo:

```bash
# Quick visual demo (no Kafka required)
./visual-demo.sh

# Full interactive demo (with Kafka)
./demo.sh
```

This launches a complete demonstration with:
- 🌈 **Colorful real-time transaction streams** 
- 🚨 **Live anomaly alerts with visual highlighting**
- 📊 **Anomaly scoring and confidence levels**
- 🎯 **Multiple anomaly types** (high amount, velocity, location, time)

**Demo Resources:**
- 📖 [Complete Demo Guide](DEMO.md) - Setup and customization instructions
- 📺 [Visual Output Examples](DEMO_OUTPUT.md) - See exactly what the demo looks like
- 🎬 [Recording Tips](DEMO.md#-recording-tips) - Perfect your demo recordings

## 🚀 Quick Start

### 📋 Prerequisites

- ☕ Java 17 or higher
- 🌊 Apache Kafka 3.6+ running on localhost:9092
- 🔧 Maven 3.6+
- 🐳 Docker & Docker Compose (for easy Kafka setup)

### 1. 🔨 Build the Project

```bash
mvn clean compile
```

### 2. ✅ Run Tests

```bash
mvn test
```

### 3. 🐳 Start Kafka (Docker Compose - Recommended)

```bash
# Start all services (Kafka, Zookeeper, UI)
docker compose up -d

# Or use Podman
podman compose up -d
```

### 4. 📥 Create Required Topics

```bash
# The init-kafka container automatically creates these topics:
# - transactions (input)
# - anomalies (all results) 
# - alerts (anomalies only)

# Verify topics were created
docker compose exec kafka kafka-topics --list --bootstrap-server localhost:9092
```

### 5. 🎯 Run SlipStream

```bash
mvn exec:java -Dexec.mainClass="com.slipstream.SlipStreamApplication"
```

Or build and run the JAR:

```bash
mvn package
java -jar target/slipstream-anomaly-detector-1.0.0-SNAPSHOT.jar
```

### 6. 🎬 Try the Demo

```bash
# Quick visual demo (no Kafka needed)
./visual-demo.sh

# Full interactive demo (with real Kafka)
./demo.sh
```

## ⚙️ Configuration

### 🌍 Environment Variables

| Variable | Description | Default | Example |
|----------|-------------|---------|---------|
| `KAFKA_BOOTSTRAP_SERVERS` | 🌊 Kafka bootstrap servers | `localhost:9092` | `kafka1:9092,kafka2:9092` |
| `KAFKA_INPUT_TOPIC` | 📥 Input topic for transactions | `transactions` | `payment-events` |
| `KAFKA_OUTPUT_TOPIC` | 📤 Output topic for all results | `anomalies` | `fraud-results` |
| `KAFKA_ALERTS_TOPIC` | 🚨 Alerts topic for anomalies | `alerts` | `fraud-alerts` |
| `KAFKA_NUM_THREADS` | 🔄 Number of stream threads | `1` | `4` |
| `KAFKA_STATE_DIR` | 💾 Directory for state stores | `/tmp/kafka-streams` | `/data/streams` |

### 📝 Application Properties

Configuration can also be set in `src/main/resources/application.properties`:

```properties
# 🌊 Kafka Configuration
kafka.bootstrap.servers=localhost:9092
kafka.input.topic=transactions
kafka.output.topic=anomalies
kafka.alerts.topic=alerts

# 🧠 Statistical Detector Settings
detector.statistical.zscore_threshold=2.5
detector.statistical.anomaly_threshold=0.7
detector.statistical.min_samples=20

# 🔄 Stream Processing
kafka.num.stream.threads=1
kafka.commit.interval.ms=10000
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

## 🔧 Testing

### 📨 Send Test Transactions

You can use the Kafka console producer to send test transactions:

```bash
# Start the console producer
docker compose exec kafka kafka-console-producer --topic transactions --bootstrap-server localhost:9092

# Or use our demo transaction generator
mvn exec:java -Dexec.mainClass='com.slipstream.demo.TransactionGenerator' -Dexec.args="30"
```

### 📋 Sample Transaction

```json
{
  "transaction_id": "tx_001",
  "user_id": "user_123", 
  "merchant_id": "merchant_grocery",
  "amount": 50.0,
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
  "metadata": {}
}
```

### 👀 Monitor Results

```bash
# 📊 Monitor all results
docker compose exec kafka kafka-console-consumer --topic anomalies --bootstrap-server localhost:9092 --from-beginning

# 🚨 Monitor alerts only
docker compose exec kafka kafka-console-consumer --topic alerts --bootstrap-server localhost:9092 --from-beginning

# 🎨 Use our beautiful anomaly monitor
mvn exec:java -Dexec.mainClass='com.slipstream.demo.AnomalyResultConsumer'
```

## 🎯 Anomaly Types

SlipStream detects several types of anomalies using statistical analysis:

- 🚨 **FRAUD**: General fraud patterns and suspicious behavior
- 💰 **UNUSUAL_AMOUNT**: Transactions with abnormally high amounts (>$5,000)
- ⚡ **VELOCITY**: High frequency of transactions from same user (>3 in 5 minutes)
- 🌍 **LOCATION**: Transactions from unusual or suspicious locations
- 🕐 **TIME_PATTERN**: Transactions at unusual times (late night/early morning)
- 🏪 **MERCHANT_PATTERN**: Unusual merchant interaction patterns
- 📊 **STATISTICAL_OUTLIER**: General statistical anomalies using Z-score analysis

### 🧮 Detection Algorithms

- **Z-Score Analysis**: Statistical outlier detection based on standard deviations
- **Composite Scoring**: Combines multiple factors for accurate detection
- **Velocity Detection**: Tracks transaction frequency per user
- **Location Analysis**: Identifies geographically suspicious transactions
- **Time Pattern Recognition**: Detects unusual timing patterns

## ⚡ Performance Tuning

### 🌊 Kafka Streams Configuration

```properties
# 🚀 Increase processing threads for higher throughput
KAFKA_NUM_THREADS=4

# ⏱️ Adjust commit interval for latency vs. throughput trade-off
kafka.commit.interval.ms=10000

# 🗄️ State store caching for better performance
kafka.cache.max.bytes.buffering=10485760

# 🔄 Processing guarantee
kafka.processing.guarantee=at_least_once

# 📦 Batch size optimization
kafka.batch.size=16384
```

### ☕ JVM Tuning

```bash
# 🚀 Production JVM settings
java -Xmx4g -Xms2g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=100 \
     -XX:+UseStringDeduplication \
     -jar slipstream.jar

# 🛠️ Development settings
java -Xmx1g -Xms512m \
     -XX:+UseG1GC \
     -jar slipstream.jar
```

### 📊 Performance Metrics

Expected performance on modern hardware:
- **Throughput**: 10,000+ transactions/second
- **Latency**: <50ms for anomaly detection
- **Memory**: 1-4GB depending on state store size
- **CPU**: 2-8 cores for optimal performance

## 📊 Monitoring and Metrics

SlipStream provides comprehensive observability:

### 📈 Built-in Metrics (logged every 30 seconds)

- 🔄 **Stream Processing State**: Current topology status
- 📚 **Training Data Size**: Number of samples in the statistical model
- 🧠 **Model Status**: Training completion and health
- 📊 **Transaction Counters**: Total processed, normal, anomalous
- 💾 **JVM Memory Usage**: Heap and non-heap memory statistics
- ⏱️ **Processing Latency**: Average and 99th percentile latencies

### 📝 Logging Levels

```properties
# 🐛 Debug mode for development
logging.level.com.slipstream=DEBUG

# 📊 Info mode for production (default)
logging.level.com.slipstream=INFO

# 🔇 Reduce Kafka noise
logging.level.org.apache.kafka=WARN
```

### 🎯 Health Checks

The application provides several health indicators:
- ✅ **Kafka Connectivity**: Connection to Kafka brokers
- 🔄 **Stream State**: Kafka Streams topology health
- 🧠 **Model Health**: Statistical model training status
- 💾 **Memory Usage**: JVM memory consumption
- 📊 **Processing Rate**: Transactions per second

## 👨‍💻 Development

### 📁 Project Structure

```
src/
├── 📱 main/java/com/slipstream/
│   ├── 🚀 SlipStreamApplication.java          # Main application entry point
│   ├── ⚙️ config/
│   │   └── KafkaConfig.java                   # Kafka configuration management
│   ├── 🧠 detector/
│   │   ├── AnomalyDetector.java               # Detector interface
│   │   └── StatisticalAnomalyDetector.java    # Statistical ML detector
│   ├── 📊 model/
│   │   ├── TransactionEvent.java              # Input transaction data model
│   │   └── AnomalyResult.java                 # Output anomaly result model
│   ├── 🎨 demo/
│   │   ├── TransactionGenerator.java          # Demo transaction generator
│   │   └── AnomalyResultConsumer.java         # Visual anomaly monitor
│   └── 🌊 stream/
│       └── AnomalyDetectionStreams.java       # Kafka Streams topology
├── 📝 main/resources/
│   ├── application.properties                 # Application configuration
│   └── logback.xml                           # Logging configuration
└── ✅ test/java/                              # Comprehensive unit tests
    ├── StatisticalAnomalyDetectorTest.java
    └── TransactionEventTest.java
```

### 🛠️ Building and Testing

```bash
# 🔧 Compile only
mvn compile

# ✅ Run all tests
mvn test

# 📊 Run tests with coverage
mvn test jacoco:report

# 📦 Create executable JAR
mvn package

# 🚀 Run integration tests (requires running Kafka)
mvn verify

# 🧹 Clean build artifacts
mvn clean
```

### 🎯 IDE Setup

**IntelliJ IDEA**:
```bash
# Import as Maven project
# Enable annotation processing
# Set Java 17 as project SDK
```

**VS Code**:
```bash
# Install Java Extension Pack
# Install Maven for Java extension
# Configure Java 17 runtime
```

## 🤝 Contributing

We welcome contributions! Here's how you can help make SlipStream better:

### 🚀 Quick Start for Contributors

1. **🍴 Fork** the repository
2. **🌿 Create** a feature branch: `git checkout -b feature/amazing-feature`
3. **💾 Commit** your changes: `git commit -m 'Add amazing feature'`
4. **📤 Push** to branch: `git push origin feature/amazing-feature`
5. **🔃 Open** a Pull Request

### 📋 Development Guidelines

- ✅ Follow Java coding standards and conventions
- 📝 Add comprehensive unit tests for new features
- 📖 Update documentation for any API changes
- 🎯 Ensure all tests pass before submitting
- 🚨 Include integration tests for stream processing changes
- 🔍 Use meaningful commit messages

### 🐛 Bug Reports

Found a bug? Please open an issue with:
- 📝 Clear description of the problem
- 🔄 Steps to reproduce the issue
- 💻 Your environment details (Java version, OS, etc.)
- 📊 Expected vs actual behavior
- 📋 Any error logs or stack traces

### 💡 Feature Requests

Have an idea? We'd love to hear it! Include:
- 🎯 Clear description of the proposed feature
- 🤔 Explanation of why it would be useful
- 📈 Examples of how it would be used
- 🛠️ Any implementation suggestions

### 🧪 Testing Guidelines

```bash
# Run all tests before submitting
mvn clean test

# Check code coverage
mvn jacoco:report

# Run integration tests
mvn verify

# Performance tests
mvn test -Dtest=*PerformanceTest
```

### 📜 Code Review Process

1. All submissions require code review
2. Maintainers will review and provide feedback
3. Address any requested changes
4. Once approved, your PR will be merged!

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙋‍♀️ Support & Questions

- 📖 **Documentation**: Check our comprehensive docs above
- 🐛 **Issues**: [GitHub Issues](https://github.com/yourusername/slipstream/issues)
- 💬 **Discussions**: [GitHub Discussions](https://github.com/yourusername/slipstream/discussions)
- 📧 **Email**: For security issues, please email directly
- 📺 **Demo Videos**: See our [DEMO.md](DEMO.md) for recording tips

## 🌟 Acknowledgments

- ☕ **Apache Kafka** - For the incredible streaming platform
- 🌊 **Kafka Streams** - For making stream processing accessible
- 📊 **Apache Commons Math** - For robust statistical functions
- 🐳 **Docker** - For containerization magic
- 🎯 **Maven** - For dependency management
- 🧪 **JUnit** - For comprehensive testing framework

---

<div align="center">

**🚀 Ready to detect anomalies in real-time? Let's get started! 🚀**

Made with ❤️ by the SlipStream team

⭐ **Don't forget to star this repo if you found it helpful!** ⭐

</div>
