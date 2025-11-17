#!/bin/bash

# Script to join Liberty collective members to the controller

echo "🔗 Joining Liberty Collective Members..."
echo ""

# Check if controller is running
if ! nc -z localhost 9443; then
    echo "❌ Controller is not running on port 9443. Please start the controller first."
    exit 1
fi

# Get the base directory
BASE_DIR=$(pwd)

# Join member1
if [ -d "$BASE_DIR/liberty-cluster-member1/target/liberty/wlp/bin" ]; then
    echo "📍 Joining member1 to collective..."
    cd "$BASE_DIR/liberty-cluster-member1/target/liberty/wlp/bin"
    ./collective join member1 --host=localhost --port=9443 --user=admin --password=adminpwd --keystorePassword=Liberty --autoAcceptCertificates --hostName=localhost --rpcUserHome="$HOME" --useCollectiveSSHKey=true

    if [ $? -eq 0 ]; then
        echo "✅ Member1 joined successfully"
    else
        echo "❌ Failed to join member1"
    fi
else
    echo "⚠️  member1 build directory not found. Skipping."
fi

echo ""

# Join member2
if [ -d "$BASE_DIR/liberty-cluster-member2/target/liberty/wlp/bin" ]; then
    echo "📍 Joining member2 to collective..."
    cd "$BASE_DIR/liberty-cluster-member2/target/liberty/wlp/bin"
    ./collective join member2 --host=localhost --port=9443 --user=admin --password=adminpwd --keystorePassword=Liberty --autoAcceptCertificates --hostName=localhost --rpcUserHome="$HOME" --useCollectiveSSHKey=true

    if [ $? -eq 0 ]; then
        echo "✅ Member2 joined successfully"
    else
        echo "❌ Failed to join member2"
    fi
else
    echo "⚠️  member2 build directory not found. Skipping."
fi

cd $BASE_DIR

echo ""
echo "✅ Collective member join process completed"
echo ""
echo "💡 You can now query members via:"
echo "   curl http://localhost:9080/liberty-cluster-app/api/members"
echo ""

# Made with Bob