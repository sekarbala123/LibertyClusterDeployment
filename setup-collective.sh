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

echo "💡 Now you can create the collective manually."
echo ""

# Made with Bob