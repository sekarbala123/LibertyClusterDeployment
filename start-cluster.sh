#!/bin/bash

# Liberty Cluster Startup Script
# This script starts all cluster members in separate terminal windows

echo "🚀 Starting Liberty Cluster..."
echo ""

# Check if running on macOS or Linux
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    echo "📍 Detected macOS - Opening Terminal windows..."
    
    # Start controller
    osascript -e 'tell application "Terminal" to do script "cd \"'$(pwd)'/liberty-cluster-app-ear\" && mvn pre-integration-test liberty:run"'
    echo "✅ Controller starting on ports 9080/9443"
    
    sleep 2
    
    # Start member1
    osascript -e 'tell application "Terminal" to do script "cd \"'$(pwd)'/liberty-cluster-member1\" && mvn pre-integration-test liberty:run"'
    echo "✅ Member1 starting on ports 9081/9444"
    
    sleep 2
    
    # Start member2
    osascript -e 'tell application "Terminal" to do script "cd \"'$(pwd)'/liberty-cluster-member2\" && mvn pre-integration-test liberty:run"'
    echo "✅ Member2 starting on ports 9082/9445"
    
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # Linux
    echo "📍 Detected Linux - Opening terminal windows..."
    
    # Try different terminal emulators
    if command -v gnome-terminal &> /dev/null; then
        gnome-terminal -- bash -c "cd liberty-cluster-app-ear && mvn pre-integration-test liberty:run; exec bash"
        gnome-terminal -- bash -c "cd liberty-cluster-member1 && mvn pre-integration-test liberty:run; exec bash"
        gnome-terminal -- bash -c "cd liberty-cluster-member2 && mvn pre-integration-test liberty:run; exec bash"
    elif command -v xterm &> /dev/null; then
        xterm -e "cd liberty-cluster-app-ear && mvn pre-integration-test liberty:run" &
        xterm -e "cd liberty-cluster-member1 && mvn pre-integration-test liberty:run" &
        xterm -e "cd liberty-cluster-member2 && mvn pre-integration-test liberty:run" &
    else
        echo "⚠️  No supported terminal emulator found."
        echo "Please run these commands manually in separate terminals:"
        echo ""
        echo "Terminal 1: cd liberty-cluster-app-ear && mvn pre-integration-test liberty:run"
        echo "Terminal 2: cd liberty-cluster-member1 && mvn pre-integration-test liberty:run"
        echo "Terminal 3: cd liberty-cluster-member2 && mvn pre-integration-test liberty:run"
        exit 1
    fi
    
    echo "✅ Controller starting on ports 9080/9443"
    echo "✅ Member1 starting on ports 9081/9444"
    echo "✅ Member2 starting on ports 9082/9445"
else
    echo "⚠️  Unsupported OS: $OSTYPE"
    echo "Please run these commands manually in separate terminals:"
    echo ""
    echo "Terminal 1: cd liberty-cluster-app-ear && mvn pre-integration-test liberty:run"
    echo "Terminal 2: cd liberty-cluster-member1 && mvn pre-integration-test liberty:run"
    echo "Terminal 3: cd liberty-cluster-member2 && mvn pre-integration-test liberty:run"
    exit 1
fi

echo ""
echo "⏳ Waiting for servers to start (this may take 1-2 minutes)..."
echo ""
echo "📍 Access Points:"
echo "   Controller:  http://localhost:9080/liberty-cluster-app/api/cluster"
echo "   Member 1:    http://localhost:9081/liberty-cluster-app/api/cluster"
echo "   Member 2:    http://localhost:9082/liberty-cluster-app/api/cluster"
echo ""
echo "🔐 Admin Centers:"
echo "   Controller:  https://localhost:9443/adminCenter/ (admin/adminpwd)"
echo "   Member 1:    https://localhost:9444/adminCenter/ (admin/adminpwd)"
echo "   Member 2:    https://localhost:9445/adminCenter/ (admin/adminpwd)"
echo ""
echo "💡 To stop all servers, press Ctrl+C in each terminal window"
echo ""

# Made with Bob
