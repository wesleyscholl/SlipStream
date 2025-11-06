# 📺 SlipStream Visual Demo Output

Here's what you'll see when running the SlipStream demo:

## 🎬 Demo Startup

```
╔══════════════════════════════════════════════════════════════╗
║                    🚀 SLIPSTREAM DEMO 🚀                     ║
║              Real-Time Anomaly Detection System              ║
╚══════════════════════════════════════════════════════════════╝

📊 Generating transactions for 60 seconds...
```

## 💳 Transaction Stream Display

```
[15:29:28] NORMAL      | $167.10 | Walmart     | Phoenix, AZ       | user_456
[15:29:29] HIGH_AMOUNT | $15,420.00 | Amazon   | Los Angeles, CA   | user_123
[15:29:30] VELOCITY    | $89.50  | McDonald's  | New York, NY      | user_789
[15:29:31] NORMAL      | $23.99  | CVS         | Chicago, IL       | user_234
[15:29:32] LOCATION    | $450.00 | Shell       | Moscow, Russia    | user_567
[15:29:33] TIME        | $1,200.00 | Best Buy  | Austin, TX        | user_890
[15:29:34] NORMAL      | $8.75   | Starbucks   | San Diego, CA     | user_345
```

**Color Coding:**
- 🟢 **GREEN**: Normal transactions
- 🔴 **RED**: High amount anomalies (>$5,000)
- 🟡 **YELLOW**: Velocity anomalies (rapid transactions)
- 🟣 **PURPLE**: Location anomalies (suspicious locations)
- 🔵 **CYAN**: Time anomalies (unusual hours)

## 🚨 Anomaly Alert Display

```
🚨 ANOMALY DETECTED 🚨
┌─────────────────────────────────────────────────────────────┐
│ Time: 15:29:29   Type:  HIGH     Score: 0.92                │
│ Transaction: TXN-1762460969-123                             │
│ User: user_123                                              │
│ Amount: $15,420.00                                          │
│ Confidence: 94.2%                                           │
├─────────────────────────────────────────────────────────────┤
│ Reason:                                                     │
│ • Unusually high transaction amount detected               │
└─────────────────────────────────────────────────────────────┘

🚨 ANOMALY DETECTED 🚨
┌─────────────────────────────────────────────────────────────┐
│ Time: 15:29:32   Type: LOCATION   Score: 0.85               │
│ Transaction: TXN-1762460972-456                             │
│ User: user_567                                              │
│ Amount: $450.00                                             │
│ Confidence: 87.8%                                           │
├─────────────────────────────────────────────────────────────┤
│ Reason:                                                     │
│ • Transaction from suspicious location: Moscow, Russia     │
└─────────────────────────────────────────────────────────────┘
```

## 🎯 Demo Features Showcased

### 1. **Real-Time Processing**
- Live transaction stream processing
- Immediate anomaly detection
- Sub-second response times

### 2. **Multiple Anomaly Types**
- **High Amount**: Transactions >$5,000
- **Velocity**: Multiple rapid transactions
- **Location**: Suspicious geographical locations
- **Time Pattern**: Transactions at 3 AM
- **Statistical Outliers**: ML-detected anomalies

### 3. **Visual Excellence**
- Color-coded transaction types
- Formatted anomaly alerts
- Progress indicators
- ASCII art headers
- Emoji indicators

### 4. **Technical Demonstration**
- Apache Kafka Streams processing
- Statistical anomaly detection
- Real-time ML inference
- JSON data serialization
- Multi-threaded processing

## 🎥 Perfect for Recording

This demo is designed specifically for:
- **GitHub repository showcases**
- **Technical presentations**
- **Live demonstrations**
- **Training materials**
- **Marketing videos**

## 📱 Running the Demo

```bash
# Quick visual demo (no Kafka required)
./visual-demo.sh

# Full interactive demo (requires Kafka)
./demo.sh

# Manual components
mvn exec:java -Dexec.mainClass='com.slipstream.demo.TransactionGenerator' -Dexec.args="60"
mvn exec:java -Dexec.mainClass='com.slipstream.demo.AnomalyResultConsumer'
```

The visual output makes SlipStream's capabilities immediately apparent and impressive! 🎬✨