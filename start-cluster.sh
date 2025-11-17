#!/bin/bash

# Liberty Cluster Startup Script
# This script starts the member servers

echo "🚀 Starting Liberty Cluster Members..."
echo ""

# Check if running on macOS or Linux
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    echo "📍 Detected macOS - Opening Terminal windows..."
    
    # Start member1 using direct server command (no application)
    if [ -d "liberty-cluster-member1/target/liberty/wlp/bin" ]; then
        osascript -e 'tell application "Terminal" to do script "cd \"'$(pwd)'/liberty-cluster-member1/target/liberty/wlp/bin\" && ./server start member1 && tail -f ../usr/servers/member1/logs/messages.log"'
        echo "✅ Member1 starting on ports 9081/9444 (collective member only)"
    else
        echo "⚠️  member1 build directory not found. Skipping."
    fi
    
    sleep 2
    
    # Start member2 using direct server command (no application)
    if [ -d "liberty-cluster-member2/target/liberty/wlp/bin" ]; then
        osascript -e 'tell application "Terminal" to do script "cd \"'$(pwd)'/liberty-cluster-member2/target/liberty/wlp/bin\" && ./server start member2 && tail -f ../usr/servers/member2/logs/messages.log"'
        echo "✅ Member2 starting on ports 9082/9445 (collective member only)"
    else
        echo "⚠️  member2 build directory not found. Skipping."
    fi
    
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # Linux
    echo "📍 Detected Linux - Opening terminal windows..."
    
    # Try different terminal emulators
    if command -v gnome-terminal &> /dev/null; then
        if [ -d "liberty-cluster-member1/target/liberty/wlp/bin" ]; then
            gnome-terminal -- bash -c "cd liberty-cluster-member1/target/liberty/wlp/bin && ./server start member1 && tail -f ../usr/servers/member1/logs/messages.log; exec bash"
        fi
        sleep 2
        if [ -d "liberty-cluster-member2/target/liberty/wlp/bin" ]; then
            gnome-terminal -- bash -c "cd liberty-cluster-member2/target/liberty/wlp/bin && ./server start member2 && tail -f ../usr/servers/member2/logs/messages.log; exec bash"
        fi
    elif command -v xterm &> /dev/null; then
        if [ -d "liberty-cluster-member1/target/liberty/wlp/bin" ]; then
            xterm -e "cd liberty-cluster-member1/target/liberty/wlp/bin && ./server start member1 && tail -f ../usr/servers/member1/logs/messages.log" &
        fi
        sleep 2
        if [ -d "liberty-cluster-member2/target/liberty/wlp/bin" ]; then
            xterm -e "cd liberty-cluster-member2/target/liberty/wlp/bin && ./server start member2 && tail -f ../usr/servers/member2/logs/messages.log" &
        fi
    else
        echo "⚠️  No supported terminal emulator found."
        echo "Please run these commands manually in separate terminals:"
        echo ""
        echo "Terminal 1: cd liberty-cluster-member1/target/liberty/wlp/bin && ./server start member1 && tail -f ../usr/servers/member1/logs/messages.log"
        echo "Terminal 2: cd liberty-cluster-member2/target/liberty/wlp/bin && ./server start member2 && tail -f ../usr/servers/member2/logs/messages.log"
        exit 1
    fi
    
    echo "✅ Member1 starting on ports 9081/9444 (collective member only)"
    echo "✅ Member2 starting on ports 9082/9445 (collective member only)"
else
    echo "⚠️  Unsupported OS: $OSTYPE"
    echo "Please run these commands manually in separate terminals:"
    echo ""
    echo "Terminal 1: cd liberty-cluster-member1/target/liberty/wlp/bin && ./server start member1 && tail -f ../usr/servers/member1/logs/messages.log"
    echo "Terminal 2: cd liberty-cluster-member2/target/liberty/wlp/bin && ./server start member2 && tail -f ../usr/servers/member2/logs/messages.log"
    exit 1
fi

echo ""
echo "⏳ Waiting for servers to start (this may take 1-2 minutes)..."
echo ""
echo "💡 To stop all servers, run: ./stop-cluster.sh"
echo ""

# Made with Bob
