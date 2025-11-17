#!/bin/bash
set -e
set -u
set -o pipefail

echo "Stopping all Liberty servers..."
pkill -f "ws-server.jar controller"
pkill -f "ws-server.jar member1"
pkill -f "ws-server.jar member2"
pkill -f "liberty:dev"
pkill -f "liberty:run"
sleep 2
echo "All Liberty servers stopped."

# Made with Bob
