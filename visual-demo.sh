#!/bin/bash

# SlipStream Visual Demo (without Kafka)
# Shows the beautiful visual output without requiring Kafka infrastructure

set -e

# Colors for output
CYAN='\033[0;36m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${CYAN}"
cat << "EOF"
╔══════════════════════════════════════════════════════════════╗
║                🎬 SLIPSTREAM VISUAL DEMO 🎬                  ║
║            Showcasing Real-Time Anomaly Detection            ║
║                                                              ║
║  This demo shows SlipStream's beautiful visual output        ║
║  Perfect for recording and presentations!                    ║
╚══════════════════════════════════════════════════════════════╝
EOF
echo -e "${NC}"

echo -e "${YELLOW}🎯 This demo demonstrates the visual output without requiring Kafka${NC}"
echo -e "${GREEN}📺 Perfect for screen recording and GitHub showcase${NC}"
echo ""

echo -e "${CYAN}🚀 Starting transaction generator visual demo...${NC}"
echo ""

# Build the project
echo -e "${YELLOW}🔨 Building project...${NC}"
mvn clean compile -q

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Build failed${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Build successful${NC}"
echo ""

echo -e "${CYAN}📊 Watch the colorful transaction stream:${NC}"
echo -e "${YELLOW}   (Kafka connection warnings are expected - focus on the visual output!)${NC}"
echo ""

# Run the transaction generator for visual demo
# It will show beautiful output even without Kafka
timeout 15s mvn exec:java -Dexec.mainClass='com.slipstream.demo.TransactionGenerator' -Dexec.args="10" -q 2>/dev/null || true

echo ""
echo -e "${GREEN}🎉 Visual demo completed!${NC}"
echo ""
echo -e "${CYAN}🎬 This output demonstrates:${NC}"
echo -e "   • 🌈 Color-coded transaction types"
echo -e "   • 💰 Amount highlighting" 
echo -e "   • 🕐 Real-time timestamps"
echo -e "   • 📍 Location information"
echo -e "   • 👤 User identification"
echo -e "   • 🏪 Merchant details"
echo ""
echo -e "${YELLOW}💡 To run the full demo with Kafka:${NC} ./demo.sh"
echo -e "${YELLOW}📖 For detailed demo instructions:${NC} See DEMO.md"