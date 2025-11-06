#!/bin/bash

# SlipStream Demo Script
# This script demonstrates the real-time anomaly detection capabilities

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m' # No Color

# Configuration
DEMO_DURATION=60  # seconds
KAFKA_CONTAINER="kafka"

# Auto-detect Docker/Podman compose command
if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
    COMPOSE_CMD="docker compose"
    echo -e "${BLUE}🐳 Using Docker Compose${NC}"
elif command -v podman >/dev/null 2>&1; then
    COMPOSE_CMD="podman compose"
    echo -e "${BLUE}🐋 Using Podman Compose${NC}"
else
    echo -e "${RED}❌ Neither Docker nor Podman compose found. Please install one of them.${NC}"
    exit 1
fi

echo -e "${CYAN}"
cat << "EOF"
╔══════════════════════════════════════════════════════════════╗
║                    🚀 SLIPSTREAM DEMO 🚀                     ║
║         Real-Time Kafka Anomaly Detection System            ║
║                                                              ║
║  This demo will showcase SlipStream's ability to detect     ║
║  anomalies in transaction streams in real-time              ║
╚══════════════════════════════════════════════════════════════╝
EOF
echo -e "${NC}"

echo -e "${BLUE}📋 Demo Configuration:${NC}"
echo -e "   • Duration: ${YELLOW}${DEMO_DURATION} seconds${NC}"
echo -e "   • Kafka Server: ${YELLOW}localhost:9092${NC}"
echo -e "   • Input Topic: ${YELLOW}transaction-events${NC}"
echo -e "   • Output Topic: ${YELLOW}anomaly-alerts${NC}"
echo ""

# Function to check if Kafka is running
check_kafka() {
    echo -e "${BLUE}🔍 Checking Kafka status...${NC}"
    if $COMPOSE_CMD ps kafka 2>/dev/null | grep -q "Up"; then
        echo -e "${GREEN}✅ Kafka is running${NC}"
        return 0
    else
        echo -e "${RED}❌ Kafka is not running${NC}"
        return 1
    fi
}

# Function to start Kafka if not running
start_kafka() {
    echo -e "${YELLOW}🚀 Starting Kafka infrastructure...${NC}"
    
    # For Podman, ensure the machine is running
    if [[ "$COMPOSE_CMD" == *"podman"* ]]; then
        echo -e "${BLUE}🔧 Ensuring Podman machine is running...${NC}"
        podman machine start 2>/dev/null || true
        sleep 2
    fi
    
    $COMPOSE_CMD up -d
    echo -e "${BLUE}⏳ Waiting for Kafka to be ready...${NC}"
    
    # Wait for Kafka to be ready
    local retries=30
    while [ $retries -gt 0 ]; do
        if $COMPOSE_CMD exec kafka kafka-topics --bootstrap-server localhost:9092 --list >/dev/null 2>&1; then
            echo -e "${GREEN}✅ Kafka is ready!${NC}"
            break
        fi
        echo -e "${YELLOW}   Waiting for Kafka... ($retries retries left)${NC}"
        sleep 2
        retries=$((retries - 1))
    done
    
    if [ $retries -eq 0 ]; then
        echo -e "${RED}❌ Failed to start Kafka${NC}"
        exit 1
    fi
}

# Function to create topics
create_topics() {
    echo -e "${BLUE}📝 Creating Kafka topics...${NC}"
    
    # Create topics if they don't exist
    $COMPOSE_CMD exec kafka kafka-topics \
        --create --if-not-exists \
        --topic transaction-events \
        --partitions 3 \
        --replication-factor 1 \
        --bootstrap-server localhost:9092
    
    $COMPOSE_CMD exec kafka kafka-topics \
        --create --if-not-exists \
        --topic anomaly-alerts \
        --partitions 3 \
        --replication-factor 1 \
        --bootstrap-server localhost:9092
    
    echo -e "${GREEN}✅ Topics created successfully${NC}"
}

# Function to build the application
build_app() {
    echo -e "${BLUE}🔨 Building SlipStream application...${NC}"
    mvn clean compile -q
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Build successful${NC}"
    else
        echo -e "${RED}❌ Build failed${NC}"
        exit 1
    fi
}

# Function to start the anomaly consumer in background
start_anomaly_monitor() {
    echo -e "${BLUE}👀 Starting anomaly result monitor...${NC}"
    
    # Start the anomaly consumer in a new terminal if possible, otherwise background
    if command -v gnome-terminal >/dev/null 2>&1; then
        gnome-terminal --title="Anomaly Monitor" -- bash -c "mvn exec:java -Dexec.mainClass='com.slipstream.demo.AnomalyResultConsumer' -q; read -p 'Press Enter to close...'"
    elif command -v osascript >/dev/null 2>&1; then
        # macOS Terminal
        osascript -e 'tell app "Terminal" to do script "cd \"'$(pwd)'\" && mvn exec:java -Dexec.mainClass=\"com.slipstream.demo.AnomalyResultConsumer\" -q"'
    else
        # Fallback: run in background
        nohup mvn exec:java -Dexec.mainClass='com.slipstream.demo.AnomalyResultConsumer' -q > anomaly-monitor.log 2>&1 &
        ANOMALY_MONITOR_PID=$!
        echo -e "${YELLOW}   Monitor started in background (PID: $ANOMALY_MONITOR_PID)${NC}"
        echo -e "${YELLOW}   Logs available in: anomaly-monitor.log${NC}"
    fi
}

# Function to start SlipStream main application
start_slipstream() {
    echo -e "${BLUE}🎯 Starting SlipStream application...${NC}"
    
    # Start SlipStream in a new terminal if possible, otherwise background
    if command -v gnome-terminal >/dev/null 2>&1; then
        gnome-terminal --title="SlipStream Core" -- bash -c "mvn exec:java -Dexec.mainClass='com.slipstream.SlipStreamApplication' -q; read -p 'Press Enter to close...'"
    elif command -v osascript >/dev/null 2>&1; then
        # macOS Terminal
        osascript -e 'tell app "Terminal" to do script "cd \"'$(pwd)'\" && mvn exec:java -Dexec.mainClass=\"com.slipstream.SlipStreamApplication\" -q"'
    else
        # Fallback: run in background
        nohup mvn exec:java -Dexec.mainClass='com.slipstream.SlipStreamApplication' -q > slipstream.log 2>&1 &
        SLIPSTREAM_PID=$!
        echo -e "${YELLOW}   SlipStream started in background (PID: $SLIPSTREAM_PID)${NC}"
        echo -e "${YELLOW}   Logs available in: slipstream.log${NC}"
    fi
}

# Function to generate demo transactions
start_demo_transactions() {
    echo -e "${BLUE}📊 Starting transaction generator...${NC}"
    echo -e "${YELLOW}   Generating transactions for ${DEMO_DURATION} seconds${NC}"
    echo -e "${CYAN}   Watch the anomaly monitor for real-time detections!${NC}"
    echo ""
    
    mvn exec:java -Dexec.mainClass='com.slipstream.demo.TransactionGenerator' -Dexec.args="$DEMO_DURATION" -q
}

# Function to cleanup
cleanup() {
    echo -e "\n${YELLOW}🧹 Cleaning up...${NC}"
    
    # Kill background processes if they exist
    if [ ! -z "$ANOMALY_MONITOR_PID" ]; then
        kill $ANOMALY_MONITOR_PID 2>/dev/null || true
    fi
    if [ ! -z "$SLIPSTREAM_PID" ]; then
        kill $SLIPSTREAM_PID 2>/dev/null || true
    fi
    
    echo -e "${GREEN}✅ Demo completed successfully!${NC}"
}

# Set up cleanup trap
trap cleanup EXIT

# Main demo execution
main() {
    echo -e "${WHITE}🎬 Starting SlipStream Demo...${NC}"
    echo ""
    
    # Step 1: Check and start Kafka
    if ! check_kafka; then
        start_kafka
    fi
    
    # Step 2: Create topics
    create_topics
    
    # Step 3: Build application
    build_app
    
    # Step 4: Start anomaly monitor
    start_anomaly_monitor
    sleep 3
    
    # Step 5: Start SlipStream
    start_slipstream
    sleep 5
    
    echo -e "${GREEN}🎉 All systems ready! Starting demo...${NC}"
    echo -e "${CYAN}📺 This would be perfect for screen recording!${NC}"
    echo ""
    
    # Step 6: Generate demo data
    start_demo_transactions
    
    echo ""
    echo -e "${GREEN}🏆 Demo completed! Check the anomaly monitor for detected anomalies.${NC}"
    echo -e "${BLUE}💡 Tip: You can run individual components separately:${NC}"
    echo -e "   • Transaction Generator: ${YELLOW}mvn exec:java -Dexec.mainClass='com.slipstream.demo.TransactionGenerator'${NC}"
    echo -e "   • Anomaly Monitor: ${YELLOW}mvn exec:java -Dexec.mainClass='com.slipstream.demo.AnomalyResultConsumer'${NC}"
    echo -e "   • SlipStream Core: ${YELLOW}mvn exec:java -Dexec.mainClass='com.slipstream.SlipStreamApplication'${NC}"
}

# Check if script is being run directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi