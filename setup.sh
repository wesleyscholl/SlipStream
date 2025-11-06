#!/bin/bash

# SlipStream Development Setup Script
# This script sets up the development environment for SlipStream

set -e

echo "🚀 Setting up SlipStream development environment..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Java 17+ is installed
check_java() {
    print_status "Checking Java installation..."
    if command -v java &> /dev/null; then
        java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
        if [ "$java_version" -ge 17 ]; then
            print_status "Java $java_version found ✓"
        else
            print_error "Java 17+ required. Found Java $java_version"
            exit 1
        fi
    else
        print_error "Java not found. Please install Java 17+"
        exit 1
    fi
}

# Check if Maven is installed
check_maven() {
    print_status "Checking Maven installation..."
    if command -v mvn &> /dev/null; then
        maven_version=$(mvn -version | head -n 1 | awk '{print $3}')
        print_status "Maven $maven_version found ✓"
    else
        print_error "Maven not found. Please install Maven 3.6+"
        exit 1
    fi
}

# Check if Kafka is available
check_kafka() {
    print_status "Checking Kafka availability..."
    
    # Default Kafka installation paths
    KAFKA_PATHS=(
        "/usr/local/kafka"
        "/opt/kafka"
        "$HOME/kafka"
        "$(pwd)/kafka"
    )
    
    KAFKA_HOME=""
    for path in "${KAFKA_PATHS[@]}"; do
        if [ -d "$path" ] && [ -f "$path/bin/kafka-server-start.sh" ]; then
            KAFKA_HOME="$path"
            break
        fi
    done
    
    if [ -z "$KAFKA_HOME" ]; then
        print_warning "Kafka not found in standard locations"
        print_warning "Please ensure Kafka is installed and accessible"
        print_warning "You can download Kafka from: https://kafka.apache.org/downloads"
    else
        print_status "Kafka found at $KAFKA_HOME ✓"
        export KAFKA_HOME
    fi
}

# Create necessary directories
create_directories() {
    print_status "Creating necessary directories..."
    
    mkdir -p logs
    mkdir -p /tmp/slipstream-state
    mkdir -p data/samples
    
    print_status "Directories created ✓"
}

# Build the project
build_project() {
    print_status "Building SlipStream project..."
    
    if mvn clean compile; then
        print_status "Project built successfully ✓"
    else
        print_error "Failed to build project"
        exit 1
    fi
}

# Run tests
run_tests() {
    print_status "Running unit tests..."
    
    if mvn test; then
        print_status "All tests passed ✓"
    else
        print_warning "Some tests failed. Check the output above."
    fi
}

# Create sample data
create_sample_data() {
    print_status "Creating sample transaction data..."
    
    cat > data/samples/sample_transactions.json << 'EOF'
{"transaction_id":"tx_001","user_id":"user_123","merchant_id":"merchant_grocery","amount":50.0,"currency":"USD","timestamp":"2024-01-15T14:30:00","location":{"latitude":40.7128,"longitude":-74.0060,"country":"USA","city":"New York"},"payment_method":"credit_card","merchant_category":"grocery","metadata":{"device_id":"mobile_001"}}
{"transaction_id":"tx_002","user_id":"user_123","merchant_id":"merchant_gas","amount":75.5,"currency":"USD","timestamp":"2024-01-15T08:15:00","location":{"latitude":40.7589,"longitude":-73.9851,"country":"USA","city":"New York"},"payment_method":"credit_card","merchant_category":"gas_station","metadata":{"device_id":"mobile_001"}}
{"transaction_id":"tx_003","user_id":"user_456","merchant_id":"merchant_restaurant","amount":125.0,"currency":"USD","timestamp":"2024-01-15T19:45:00","location":{"latitude":40.7505,"longitude":-73.9934,"country":"USA","city":"New York"},"payment_method":"debit_card","merchant_category":"restaurant","metadata":{"device_id":"mobile_002"}}
{"transaction_id":"tx_004","user_id":"user_123","merchant_id":"merchant_unknown","amount":15000.0,"currency":"USD","timestamp":"2024-01-15T03:30:00","location":{"latitude":25.7617,"longitude":-80.1918,"country":"USA","city":"Miami"},"payment_method":"credit_card","merchant_category":"unknown","metadata":{"device_id":"mobile_001"}}
{"transaction_id":"tx_005","user_id":"user_789","merchant_id":"merchant_pharmacy","amount":25.99,"currency":"USD","timestamp":"2024-01-15T11:20:00","location":{"latitude":40.7282,"longitude":-74.0776,"country":"USA","city":"New York"},"payment_method":"credit_card","merchant_category":"pharmacy","metadata":{"device_id":"mobile_003"}}
EOF
    
    print_status "Sample data created in data/samples/sample_transactions.json ✓"
}

# Create helper scripts
create_scripts() {
    print_status "Creating helper scripts..."
    
    # Kafka topics creation script
    cat > scripts/create-topics.sh << 'EOF'
#!/bin/bash

# Create Kafka topics for SlipStream

KAFKA_HOME=${KAFKA_HOME:-"/usr/local/kafka"}
BOOTSTRAP_SERVERS=${BOOTSTRAP_SERVERS:-"localhost:9092"}

if [ ! -f "$KAFKA_HOME/bin/kafka-topics.sh" ]; then
    echo "Error: Kafka not found at $KAFKA_HOME"
    echo "Please set KAFKA_HOME environment variable"
    exit 1
fi

echo "Creating Kafka topics..."

# Create input topic
$KAFKA_HOME/bin/kafka-topics.sh --create \
    --topic transactions \
    --bootstrap-server $BOOTSTRAP_SERVERS \
    --partitions 3 \
    --replication-factor 1 \
    --if-not-exists

# Create output topic
$KAFKA_HOME/bin/kafka-topics.sh --create \
    --topic anomalies \
    --bootstrap-server $BOOTSTRAP_SERVERS \
    --partitions 3 \
    --replication-factor 1 \
    --if-not-exists

# Create alerts topic
$KAFKA_HOME/bin/kafka-topics.sh --create \
    --topic alerts \
    --bootstrap-server $BOOTSTRAP_SERVERS \
    --partitions 3 \
    --replication-factor 1 \
    --if-not-exists

echo "Topics created successfully!"

# List topics to verify
echo "Existing topics:"
$KAFKA_HOME/bin/kafka-topics.sh --list --bootstrap-server $BOOTSTRAP_SERVERS
EOF

    # Producer script for sending test data
    cat > scripts/send-test-data.sh << 'EOF'
#!/bin/bash

# Send test transaction data to Kafka

KAFKA_HOME=${KAFKA_HOME:-"/usr/local/kafka"}
BOOTSTRAP_SERVERS=${BOOTSTRAP_SERVERS:-"localhost:9092"}
TOPIC=${TOPIC:-"transactions"}
DATA_FILE=${DATA_FILE:-"data/samples/sample_transactions.json"}

if [ ! -f "$KAFKA_HOME/bin/kafka-console-producer.sh" ]; then
    echo "Error: Kafka not found at $KAFKA_HOME"
    echo "Please set KAFKA_HOME environment variable"
    exit 1
fi

if [ ! -f "$DATA_FILE" ]; then
    echo "Error: Data file not found at $DATA_FILE"
    exit 1
fi

echo "Sending test data from $DATA_FILE to topic $TOPIC..."

cat $DATA_FILE | $KAFKA_HOME/bin/kafka-console-producer.sh \
    --topic $TOPIC \
    --bootstrap-server $BOOTSTRAP_SERVERS

echo "Test data sent successfully!"
EOF

    # Consumer script for monitoring results
    cat > scripts/monitor-results.sh << 'EOF'
#!/bin/bash

# Monitor SlipStream results

KAFKA_HOME=${KAFKA_HOME:-"/usr/local/kafka"}
BOOTSTRAP_SERVERS=${BOOTSTRAP_SERVERS:-"localhost:9092"}
TOPIC=${1:-"anomalies"}

if [ ! -f "$KAFKA_HOME/bin/kafka-console-consumer.sh" ]; then
    echo "Error: Kafka not found at $KAFKA_HOME"
    echo "Please set KAFKA_HOME environment variable"
    exit 1
fi

echo "Monitoring topic: $TOPIC"
echo "Press Ctrl+C to stop..."

$KAFKA_HOME/bin/kafka-console-consumer.sh \
    --topic $TOPIC \
    --bootstrap-server $BOOTSTRAP_SERVERS \
    --from-beginning
EOF

    # Make scripts executable
    chmod +x scripts/create-topics.sh
    chmod +x scripts/send-test-data.sh
    chmod +x scripts/monitor-results.sh
    
    print_status "Helper scripts created in scripts/ directory ✓"
}

# Main setup process
main() {
    echo "========================================"
    echo "    SlipStream Development Setup"
    echo "========================================"
    echo
    
    check_java
    check_maven
    check_kafka
    create_directories
    build_project
    run_tests
    create_sample_data
    
    # Create scripts directory
    mkdir -p scripts
    create_scripts
    
    echo
    echo "========================================"
    print_status "Setup completed successfully! 🎉"
    echo "========================================"
    echo
    echo "Next steps:"
    echo "1. Start Kafka (if not already running)"
    echo "2. Run: ./scripts/create-topics.sh"
    echo "3. Start SlipStream: mvn exec:java -Dexec.mainClass=\"com.slipstream.SlipStreamApplication\""
    echo "4. Send test data: ./scripts/send-test-data.sh"
    echo "5. Monitor results: ./scripts/monitor-results.sh"
    echo
    echo "For more information, see README.md"
}

# Run main function
main "$@"