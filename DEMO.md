# 🎬 SlipStream Visual Demo

This demo showcases SlipStream's real-time anomaly detection capabilities with a visual, colorful interface perfect for recording and demonstrations.

## 🚀 Quick Demo

Run the complete interactive demo:

```bash
./demo.sh
```

This script will:
1. ✅ Check and start Kafka infrastructure
2. 📝 Create necessary topics
3. 🔨 Build the application
4. 👀 Start the anomaly result monitor
5. 🎯 Launch SlipStream core engine
6. 📊 Generate realistic transaction data with anomalies

## 🎭 Demo Components

### 1. Transaction Generator (`TransactionGenerator.java`)
Generates realistic transaction streams including:
- **Normal transactions** (70%) - Regular purchases
- **High amount anomalies** (10%) - Unusually large transactions
- **Velocity anomalies** (10%) - Rapid successive transactions  
- **Location anomalies** (5%) - Transactions from suspicious locations
- **Time anomalies** (5%) - Transactions at unusual hours

### 2. Anomaly Monitor (`AnomalyResultConsumer.java`)
Beautiful real-time display of detected anomalies with:
- 🚨 Color-coded alerts
- 📊 Anomaly scores and confidence levels
- 🕐 Real-time timestamps
- 💰 Amount highlighting
- 📍 Location information

### 3. Demo Script (`demo.sh`)
Orchestrates the entire demo with:
- Automatic Kafka setup
- Multi-window terminal management
- Colorful progress indicators
- Graceful cleanup

## 🎥 Manual Demo Steps

For manual control or recording specific scenarios:

### 1. Start Infrastructure
```bash
docker-compose up -d
```

### 2. Start Anomaly Monitor (Terminal 1)
```bash
mvn exec:java -Dexec.mainClass='com.slipstream.demo.AnomalyResultConsumer'
```

### 3. Start SlipStream Core (Terminal 2)  
```bash
mvn exec:java -Dexec.mainClass='com.slipstream.SlipStreamApplication'
```

### 4. Generate Demo Data (Terminal 3)
```bash
# Generate transactions for 60 seconds
mvn exec:java -Dexec.mainClass='com.slipstream.demo.TransactionGenerator' -Dexec.args="60"
```

## 📺 Recording Tips

- **Terminal Setup**: Use a dark theme with good contrast
- **Font Size**: Increase terminal font size for better visibility
- **Window Layout**: Arrange terminals side-by-side to show data flow
- **Timing**: Let each component start fully before proceeding
- **Duration**: 30-60 seconds provides good variety of anomalies

## 🎨 Visual Features

The demo includes:
- 🌈 **Color-coded output** for different transaction types
- 📊 **Real-time metrics** and scoring
- 🚨 **Alert highlighting** for high-severity anomalies
- 📈 **Statistical information** in readable format
- ⚡ **Live data streaming** demonstration

## 🔧 Customization

Modify demo parameters in `TransactionGenerator.java`:
- Transaction frequency (line 114: `Thread.sleep()`)
- Anomaly percentages (line 103-120)  
- Amount ranges (lines 135, 149, 163, etc.)
- Location lists (lines 29-40)

Perfect for showcasing SlipStream's capabilities in presentations, demos, or GitHub documentation! 🎉