#!/bin/bash

# Liberty Cluster Startup Script
# This script starts the controller and member servers
# Architecture: Controller deploys the REST application, members are collective members only

echo "🚀 Starting Liberty Cluster..."
echo ""

# Check if running on macOS or Linux
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    echo "📍 Detected macOS - Opening Terminal windows..."
    
    # Start controller with Maven (deploys REST application)
    osascript -e 'tell application "Terminal" to do script "cd \"'$(pwd)'/liberty-cluster-app-ear\" && mvn liberty:run"'
    echo "✅ Controller starting on ports 9080/9443 (with REST application)"
    
    sleep 5
    
    # Start member1 using direct server command (no application)
    osascript -e 'tell application "Terminal" to do script "cd \"'$(pwd)'/liberty-cluster-member1/target/liberty/wlp/bin\" && ./server start member1 && tail -f ../usr/servers/member1/logs/messages.log"'
    echo "✅ Member1 starting on ports 9081/9444 (collective member only)"
    
    sleep 2
    
    # Start member2 using direct server command (no application)
    osascript -e 'tell application "Terminal" to do script "cd \"'$(pwd)'/liberty-cluster-member2/target/liberty/wlp/bin\" && ./server start member2 && tail -f ../usr/servers/member2/logs/messages.log"'
    echo "✅ Member2 starting on ports 9082/9445 (collective member only)"
    
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # Linux
    echo "📍 Detected Linux - Opening terminal windows..."
    
    # Try different terminal emulators
    if command -v gnome-terminal &> /dev/null; then
        gnome-terminal -- bash -c "cd liberty-cluster-app-ear && mvn liberty:run; exec bash"
        sleep 5
        gnome-terminal -- bash -c "cd liberty-cluster-member1/target/liberty/wlp/bin && ./server start member1 && tail -f ../usr/servers/member1/logs/messages.log; exec bash"
        sleep 2
        gnome-terminal -- bash -c "cd liberty-cluster-member2/target/liberty/wlp/bin && ./server start member2 && tail -f ../usr/servers/member2/logs/messages.log; exec bash"
    elif command -v xterm &> /dev/null; then
        xterm -e "cd liberty-cluster-app-ear && mvn liberty:run" &
        sleep 5
        xterm -e "cd liberty-cluster-member1/target/liberty/wlp/bin && ./server start member1 && tail -f ../usr/servers/member1/logs/messages.log" &
        sleep 2
        xterm -e "cd liberty-cluster-member2/target/liberty/wlp/bin && ./server start member2 && tail -f ../usr/servers/member2/logs/messages.log" &
    else
        echo "⚠️  No supported terminal emulator found."
        echo "Please run these commands manually in separate terminals:"
        echo ""
        echo "Terminal 1: cd liberty-cluster-app-ear && mvn liberty:run"
        echo "Terminal 2: cd liberty-cluster-member1/target/liberty/wlp/bin && ./server start member1 && tail -f ../usr/servers/member1/logs/messages.log"
        echo "Terminal 3: cd liberty-cluster-member2/target/liberty/wlp/bin && ./server start member2 && tail -f ../usr/servers/member2/logs/messages.log"
        exit 1
    fi
    
    echo "✅ Controller starting on ports 9080/9443 (with REST application)"
    echo "✅ Member1 starting on ports 9081/9444 (collective member only)"
    echo "✅ Member2 starting on ports 9082/9445 (collective member only)"
else
    echo "⚠️  Unsupported OS: $OSTYPE"
    echo "Please run these commands manually in separate terminals:"
    echo ""
    echo "Terminal 1: cd liberty-cluster-app-ear && mvn liberty:run"
    echo "Terminal 2: cd liberty-cluster-member1/target/liberty/wlp/bin && ./server start member1 && tail -f ../usr/servers/member1/logs/messages.log"
    echo "Terminal 3: cd liberty-cluster-member2/target/liberty/wlp/bin && ./server start member2 && tail -f ../usr/servers/member2/logs/messages.log"
    exit 1
fi

echo ""
echo "⏳ Waiting for servers to start (this may take 1-2 minutes)..."
echo ""
echo "📍 REST API Endpoints (Controller only):"
echo "   Cluster Info:    http://localhost:9080/liberty-cluster-app/api/cluster"
echo "   Members List:    http://localhost:9080/liberty-cluster-app/api/members"
echo "   Members MBeans:  http://localhost:9080/liberty-cluster-app/api/members/mbeans"
echo ""
echo "🔐 Admin Centers:"
echo "   Controller:  https://localhost:9443/adminCenter/ (admin/adminpwd)"
echo "   Member 1:    https://localhost:9444/adminCenter/ (admin/adminpwd)"
echo "   Member 2:    https://localhost:9445/adminCenter/ (admin/adminpwd)"
echo ""
echo "💡 Architecture Notes:"
echo "   - Controller: Runs REST application + collective controller"
echo "   - Members: Collective members only (no application deployment)"
echo "   - Query collective members via: http://localhost:9080/liberty-cluster-app/api/members"
echo ""
echo "💡 To stop all servers, run: ./stop-cluster.sh"
echo ""

# Made with Bob
