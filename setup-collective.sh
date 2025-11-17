#!/bin/bash

# Script to set up Liberty collective controller

echo "🔧 Building the project..."
mvn clean install
if [ $? -ne 0 ]; then
    echo "❌ Maven build failed"
    exit 1
fi
echo "✅ Project built successfully"
echo ""

echo "🔧 Setting up Liberty Collective Controller..."
echo ""

# Setup the collective controller
echo "📍 Creating collective controller..."
cd liberty-cluster-app-ear/target/liberty/wlp/bin
./collective create controller --keystorePassword=Liberty

if [ $? -eq 0 ]; then
    echo "✅ Collective controller created successfully"
else
    echo "❌ Failed to create collective controller"
    exit 1
fi

cd ../../../../..

echo ""
echo "✅ Collective controller setup completed"
echo ""
echo "💡 Now you can start the controller using: 'cd liberty-cluster-app-ear && mvn liberty:run'"
echo "💡 Then, you can join members using: ./join-members.sh"
echo ""

# Made with Bob