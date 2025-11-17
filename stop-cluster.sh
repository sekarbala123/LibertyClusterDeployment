#!/bin/bash

# Liberty Cluster Stop Script
# This script stops all cluster members using appropriate methods for each server type

echo "🛑 Stopping Liberty Cluster..."
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to stop a Liberty server by port (for controller)
stop_server_by_port() {
    local server_name=$1
    local port=$2
    
    echo -n "Stopping $server_name (port $port)... "
    
    # Find process using the port
    PID=$(lsof -ti :$port 2>/dev/null)
    
    if [ ! -z "$PID" ]; then
        # Try graceful shutdown first (SIGTERM)
        kill $PID 2>/dev/null
        
        # Wait up to 10 seconds for graceful shutdown
        for i in {1..10}; do
            if ! lsof -ti :$port >/dev/null 2>&1; then
                echo -e "${GREEN}✅ Stopped${NC}"
                return 0
            fi
            sleep 1
        done
        
        # Force kill if still running (SIGKILL)
        if lsof -ti :$port >/dev/null 2>&1; then
            echo -n "(forcing) "
            kill -9 $PID 2>/dev/null
            sleep 1
            
            if ! lsof -ti :$port >/dev/null 2>&1; then
                echo -e "${GREEN}✅ Force stopped${NC}"
            else
                echo -e "${RED}❌ Failed to stop${NC}"
            fi
        fi
    else
        echo -e "${YELLOW}⚠️  Not running${NC}"
    fi
}

# Function to stop a Liberty server using server command
stop_server_by_command() {
    local server_name=$1
    local server_path=$2
    
    echo -n "Stopping $server_name using server command... "
    
    if [ -f "$server_path/server" ]; then
        # Try to stop using server command
        cd "$server_path"
        ./server stop $server_name >/dev/null 2>&1
        
        # Wait up to 10 seconds for shutdown
        for i in {1..10}; do
            if ! ./server status $server_name 2>&1 | grep -q "is running"; then
                echo -e "${GREEN}✅ Stopped${NC}"
                cd - >/dev/null
                return 0
            fi
            sleep 1
        done
        
        echo -e "${YELLOW}⚠️  May still be running${NC}"
        cd - >/dev/null
    else
        echo -e "${YELLOW}⚠️  Server command not found${NC}"
    fi
}

# Stop members first (reverse order of startup)
echo "📍 Stopping Cluster Members:"
echo ""

# Try to stop member2 using server command first
stop_server_by_command "member2" "liberty-cluster-member2/target/liberty/wlp/bin"
# Fallback to port-based stop if needed
if lsof -ti :9082 >/dev/null 2>&1; then
    stop_server_by_port "Member 2 (fallback)" "9082"
fi

# Try to stop member1 using server command first
stop_server_by_command "member1" "liberty-cluster-member1/target/liberty/wlp/bin"
# Fallback to port-based stop if needed
if lsof -ti :9081 >/dev/null 2>&1; then
    stop_server_by_port "Member 1 (fallback)" "9081"
fi

echo ""
echo "📍 Stopping Controller:"
echo ""

# Controller is stopped by port (Maven process)
stop_server_by_port "Controller" "9080"

echo ""
echo "🔍 Verifying all servers stopped..."
echo ""

# Check if any Liberty/Java processes are still running
LIBERTY_PIDS=$(ps aux | grep -E 'liberty|wlp|liberty-maven-plugin' | grep java | grep -v grep | grep -v stop-cluster | awk '{print $2}')

if [ -z "$LIBERTY_PIDS" ]; then
    echo -e "${GREEN}✅ All Liberty servers stopped successfully!${NC}"
else
    echo -e "${YELLOW}⚠️  Some Liberty processes still running:${NC}"
    ps aux | grep -E 'liberty|wlp' | grep java | grep -v grep | grep -v stop-cluster
    echo ""
    echo "💡 To force kill remaining processes, run:"
    echo "   kill -9 $LIBERTY_PIDS"
fi

echo ""
echo "📊 Port Status:"
echo ""

# Check ports
for port in 9080 9081 9082 9443 9444 9445; do
    if lsof -ti :$port >/dev/null 2>&1; then
        echo -e "   Port $port: ${RED}❌ Still in use${NC}"
    else
        echo -e "   Port $port: ${GREEN}✅ Available${NC}"
    fi
done

echo ""
echo "💡 To start the cluster again, run:"
echo "   ./start-cluster.sh"
echo ""

# Made with Bob