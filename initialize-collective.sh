#!/bin/bash

# Liberty Collective Initialization Script
# This script sets up a complete Liberty Collective with controller and member servers

set -e  # Exit on error

echo "=========================================="
echo "Liberty Collective Initialization"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
CONTROLLER_DIR="liberty-cluster-app-ear/target/liberty/wlp"
MEMBER1_DIR="liberty-cluster-member1/target/liberty/wlp"
MEMBER2_DIR="liberty-cluster-member2/target/liberty/wlp"

CONTROLLER_SERVER="controller"
MEMBER1_SERVER="member1"
MEMBER2_SERVER="member2"

ADMIN_USER="admin"
ADMIN_PASSWORD="admin123!"
KEYSTORE_PASSWORD="admin123!"

CONTROLLER_HOST="localhost"
CONTROLLER_HTTPS_PORT="9443"

# Step 1: Build the application
echo -e "${YELLOW}Step 1: Building the application...${NC}"
mvn clean install
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Build successful${NC}"
else
    echo -e "${RED}✗ Build failed${NC}"
    exit 1
fi
echo ""

# Step 2: Stop any running servers
echo -e "${YELLOW}Step 2: Stopping any running servers...${NC}"
pkill -f "liberty:run" 2>/dev/null || true
pkill -f "wlp/bin/server" 2>/dev/null || true
sleep 2
echo -e "${GREEN}✓ All servers stopped${NC}"
echo ""

# Step 3: Clean up existing collective configuration
echo -e "${YELLOW}Step 3: Cleaning up existing collective configuration...${NC}"
rm -rf ${CONTROLLER_DIR}/usr/servers/${CONTROLLER_SERVER}/collective 2>/dev/null || true
rm -rf ${MEMBER1_DIR}/usr/servers/${MEMBER1_SERVER}/collective 2>/dev/null || true
rm -rf ${MEMBER2_DIR}/usr/servers/${MEMBER2_SERVER}/collective 2>/dev/null || true
echo -e "${GREEN}✓ Cleanup complete${NC}"
echo ""

# Step 4: Start controller server
echo -e "${YELLOW}Step 4: Starting controller server...${NC}"
cd ${CONTROLLER_DIR}/bin
./server start ${CONTROLLER_SERVER}
echo "Waiting for controller to fully start (30 seconds)..."
sleep 30
echo -e "${GREEN}✓ Controller server started${NC}"
cd - > /dev/null
echo ""

# Step 5: Create the collective
echo -e "${YELLOW}Step 5: Creating the collective...${NC}"
cd ${CONTROLLER_DIR}/bin
./collective create ${CONTROLLER_SERVER} \
    --keystorePassword=${KEYSTORE_PASSWORD} \
    --createConfigFile=${CONTROLLER_DIR}/usr/servers/${CONTROLLER_SERVER}/collective-create.xml

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Collective created successfully${NC}"
else
    echo -e "${RED}✗ Failed to create collective${NC}"
    exit 1
fi
cd - > /dev/null
echo ""

# Step 6: Restart controller to apply collective configuration
echo -e "${YELLOW}Step 6: Restarting controller server...${NC}"
cd ${CONTROLLER_DIR}/bin
./server stop ${CONTROLLER_SERVER}
sleep 5
./server start ${CONTROLLER_SERVER}
echo "Waiting for controller to fully restart with collective configuration (45 seconds)..."
sleep 45

# Verify HTTPS port is accessible
echo "Verifying controller HTTPS port 9443 is accessible..."
for i in {1..10}; do
    if nc -z localhost 9443 2>/dev/null; then
        echo -e "${GREEN}✓ Controller HTTPS port is accessible${NC}"
        break
    fi
    echo "Attempt $i: Port not ready, waiting 5 more seconds..."
    sleep 5
done

echo -e "${GREEN}✓ Controller restarted${NC}"
cd - > /dev/null
echo ""

# Step 7: Start member1 server
echo -e "${YELLOW}Step 7: Starting member1 server...${NC}"
cd ${MEMBER1_DIR}/bin
./server start ${MEMBER1_SERVER}
sleep 10
echo -e "${GREEN}✓ Member1 server started${NC}"
cd - > /dev/null
echo ""

# Step 8: Join member1 to collective
echo -e "${YELLOW}Step 8: Joining member1 to collective...${NC}"
cd ${MEMBER1_DIR}/bin
./collective join ${MEMBER1_SERVER} \
    --host=${CONTROLLER_HOST} \
    --port=${CONTROLLER_HTTPS_PORT} \
    --user=${ADMIN_USER} \
    --password=${ADMIN_PASSWORD} \
    --keystorePassword=${KEYSTORE_PASSWORD} \
    --createConfigFile=${MEMBER1_DIR}/usr/servers/${MEMBER1_SERVER}/collective-join.xml

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Member1 joined successfully${NC}"
else
    echo -e "${RED}✗ Failed to join member1${NC}"
    exit 1
fi
cd - > /dev/null
echo ""

# Step 9: Restart member1
echo -e "${YELLOW}Step 9: Restarting member1 server...${NC}"
cd ${MEMBER1_DIR}/bin
./server stop ${MEMBER1_SERVER}
sleep 5
./server start ${MEMBER1_SERVER}
sleep 10
echo -e "${GREEN}✓ Member1 restarted${NC}"
cd - > /dev/null
echo ""

# Step 10: Start member2 server
echo -e "${YELLOW}Step 10: Starting member2 server...${NC}"
cd ${MEMBER2_DIR}/bin
./server start ${MEMBER2_SERVER}
sleep 10
echo -e "${GREEN}✓ Member2 server started${NC}"
cd - > /dev/null
echo ""

# Step 11: Join member2 to collective
echo -e "${YELLOW}Step 11: Joining member2 to collective...${NC}"
cd ${MEMBER2_DIR}/bin
./collective join ${MEMBER2_SERVER} \
    --host=${CONTROLLER_HOST} \
    --port=${CONTROLLER_HTTPS_PORT} \
    --user=${ADMIN_USER} \
    --password=${ADMIN_PASSWORD} \
    --keystorePassword=${KEYSTORE_PASSWORD} \
    --createConfigFile=${MEMBER2_DIR}/usr/servers/${MEMBER2_SERVER}/collective-join.xml

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Member2 joined successfully${NC}"
else
    echo -e "${RED}✗ Failed to join member2${NC}"
    exit 1
fi
cd - > /dev/null
echo ""

# Step 12: Restart member2
echo -e "${YELLOW}Step 12: Restarting member2 server...${NC}"
cd ${MEMBER2_DIR}/bin
./server stop ${MEMBER2_SERVER}
sleep 5
./server start ${MEMBER2_SERVER}
sleep 10
echo -e "${GREEN}✓ Member2 restarted${NC}"
cd - > /dev/null
echo ""

# Step 13: Verify collective status
echo -e "${YELLOW}Step 13: Verifying collective status...${NC}"
echo ""
echo "Controller Server Status:"
cd ${CONTROLLER_DIR}/bin
./server status ${CONTROLLER_SERVER}
cd - > /dev/null
echo ""

echo "Member1 Server Status:"
cd ${MEMBER1_DIR}/bin
./server status ${MEMBER1_SERVER}
cd - > /dev/null
echo ""

echo "Member2 Server Status:"
cd ${MEMBER2_DIR}/bin
./server status ${MEMBER2_SERVER}
cd - > /dev/null
echo ""

# Step 14: Display access information
echo -e "${GREEN}=========================================="
echo "Collective Setup Complete!"
echo "==========================================${NC}"
echo ""
echo "Controller Server:"
echo "  - HTTP:  http://localhost:9080"
echo "  - HTTPS: https://localhost:9443"
echo "  - Admin Console: https://localhost:9443/adminCenter"
echo ""
echo "Member1 Server:"
echo "  - HTTP:  http://localhost:9081"
echo "  - HTTPS: https://localhost:9444"
echo ""
echo "Member2 Server:"
echo "  - HTTP:  http://localhost:9082"
echo "  - HTTPS: https://localhost:9445"
echo ""
echo "REST API Endpoints:"
echo "  - Cluster Info: http://localhost:9080/liberty-cluster-app/api/cluster"
echo "  - Members Info: http://localhost:9080/liberty-cluster-app/api/members"
echo ""
echo "Credentials:"
echo "  - Username: ${ADMIN_USER}"
echo "  - Password: ${ADMIN_PASSWORD}"
echo ""
echo -e "${YELLOW}Testing the /api/members endpoint...${NC}"
sleep 5
curl -s http://localhost:9080/liberty-cluster-app/api/members | python3 -m json.tool || echo "Endpoint not ready yet, please wait a moment and try again"
echo ""
echo -e "${GREEN}Setup complete! You can now test the collective.${NC}"

# Made with Bob
