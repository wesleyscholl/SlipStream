#!/bin/bash

# SlipStream Showcase - Quick visual demonstration
echo -e "\033[36m"
cat << "EOF"
╔══════════════════════════════════════════════════════════════╗
║           🎬 SLIPSTREAM VISUAL SHOWCASE 🎬                   ║
║                                                              ║
║  Real-Time Kafka Anomaly Detection System                   ║
║  Built in Java with Statistical ML Detection                ║
╚══════════════════════════════════════════════════════════════╝
EOF
echo -e "\033[0m"

echo -e "\033[33m📊 Sample Transaction Stream:\033[0m"
echo -e "\033[32m[15:29:28] NORMAL      | \$167.10    | Walmart     | Phoenix, AZ       | user_456\033[0m"
echo -e "\033[31m[15:29:29] HIGH_AMOUNT | \$15,420.00 | Amazon      | Los Angeles, CA   | user_123\033[0m"
echo -e "\033[33m[15:29:30] VELOCITY    | \$89.50     | McDonald's  | New York, NY      | user_789\033[0m"
echo -e "\033[32m[15:29:31] NORMAL      | \$23.99     | CVS         | Chicago, IL       | user_234\033[0m"
echo -e "\033[35m[15:29:32] LOCATION    | \$450.00    | Shell       | Moscow, Russia    | user_567\033[0m"
echo -e "\033[36m[15:29:33] TIME        | \$1,200.00  | Best Buy    | Austin, TX        | user_890\033[0m"

echo ""
echo -e "\033[31m🚨 ANOMALY DETECTED 🚨\033[0m"
echo "┌─────────────────────────────────────────────────────────────┐"
echo -e "│ \033[36mTime:\033[0m 15:29:29   \033[33mType:\033[0m \033[41m\033[37m\033[1m HIGH \033[0m   \033[37mScore:\033[0m \033[31m\033[1m0.92\033[0m │"
echo -e "│ \033[34mTransaction:\033[0m TXN-1762460969-123                             │"
echo -e "│ \033[32mUser:\033[0m user_123                                              │"
echo -e "│ \033[35mAmount:\033[0m \033[41m\033[37m\033[1m\$15,420.00\033[0m                                          │"
echo -e "│ \033[36mConfidence:\033[0m 94.2%                                           │"
echo "├─────────────────────────────────────────────────────────────┤"
echo -e "│ \033[33m\033[1mReason:\033[0m                                             │"
echo -e "│ \033[31m• \033[0mUnusually high transaction amount detected               │"
echo "└─────────────────────────────────────────────────────────────┘"

echo ""
echo -e "\033[36m🎯 Demo Features:\033[0m"
echo -e "   • \033[32m✅ Real-time processing\033[0m"
echo -e "   • \033[32m✅ Statistical ML detection\033[0m"  
echo -e "   • \033[32m✅ Multiple anomaly types\033[0m"
echo -e "   • \033[32m✅ Visual alerting system\033[0m"
echo -e "   • \033[32m✅ Kafka Streams integration\033[0m"

echo ""
echo -e "\033[33m🚀 Run the demo:\033[0m"
echo -e "   \033[36m./visual-demo.sh\033[0m  - Quick visual demo"
echo -e "   \033[36m./demo.sh\033[0m         - Full interactive demo"
echo ""
echo -e "\033[35m📖 See DEMO.md and DEMO_OUTPUT.md for complete documentation\033[0m"