#!/bin/bash
set -e
set -u
set -o pipefail

# Liberty Cluster Test Script
# This script tests all cluster members to verify they're running

echo "🧪 Testing Liberty Cluster..."
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to test endpoint
test_endpoint() {
    local name=$1
    local url=$2
    local is_https=$3
    local is_admin=$4
    
    echo -n "Testing $name... "
    
    # Use -k flag for HTTPS to ignore self-signed certificate warnings
    if [ "$is_https" = "true" ]; then
        response=$(curl -k -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null)
    else
        response=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null)
    fi
    
    # Admin Center returns 302 (redirect to login) which is valid
    if [ "$is_admin" = "true" ]; then
        if [ "$response" = "200" ] || [ "$response" = "302" ]; then
            echo -e "${GREEN}✅ OK${NC} (HTTP $response)"
            return 0
        else
            echo -e "${RED}❌ FAILED${NC} (HTTP $response)"
            return 1
        fi
    else
        if [ "$response" = "200" ]; then
            echo -e "${GREEN}✅ OK${NC} (HTTP $response)"
            return 0
        else
            echo -e "${RED}❌ FAILED${NC} (HTTP $response)"
            return 1
        fi
    fi
}

# Test all endpoints
echo "📍 Testing REST API Endpoints:"
echo ""

test_endpoint "Controller" "http://localhost:9080/liberty-cluster-app/api/cluster"
controller_status=$?

test_endpoint "Member 1  " "http://localhost:9081/liberty-cluster-app/api/cluster"
member1_status=$?

test_endpoint "Member 2  " "http://localhost:9082/liberty-cluster-app/api/cluster"
member2_status=$?

echo ""
echo "🔐 Testing Admin Center Endpoints:"
echo ""

test_endpoint "Controller Admin" "https://localhost:9443/adminCenter/" "true" "true"
controller_admin=$?

test_endpoint "Member 1 Admin  " "https://localhost:9444/adminCenter/" "true" "true"
member1_admin=$?

test_endpoint "Member 2 Admin  " "https://localhost:9445/adminCenter/" "true" "true"
member2_admin=$?

echo ""
echo "📊 Summary:"
echo ""

total_tests=6
passed_tests=0

[ $controller_status -eq 0 ] && ((passed_tests++))
[ $member1_status -eq 0 ] && ((passed_tests++))
[ $member2_status -eq 0 ] && ((passed_tests++))
[ $controller_admin -eq 0 ] && ((passed_tests++))
[ $member1_admin -eq 0 ] && ((passed_tests++))
[ $member2_admin -eq 0 ] && ((passed_tests++))

if [ $passed_tests -eq $total_tests ]; then
    echo -e "${GREEN}✅ All tests passed! ($passed_tests/$total_tests)${NC}"
    echo ""
    echo "🎉 Your Liberty cluster is running successfully!"
    echo ""
    echo "📍 Access Points:"
    echo "   Controller:  http://localhost:9080/liberty-cluster-app/api/cluster"
    echo "   Member 1:    http://localhost:9081/liberty-cluster-app/api/cluster"
    echo "   Member 2:    http://localhost:9082/liberty-cluster-app/api/cluster"
    echo ""
    echo "🔐 Admin Centers (admin/adminpwd):"
    echo "   Controller:  https://localhost:9443/adminCenter/"
    echo "   Member 1:    https://localhost:9444/adminCenter/"
    echo "   Member 2:    https://localhost:9445/adminCenter/"
    exit 0
else
    echo -e "${RED}❌ Some tests failed ($passed_tests/$total_tests passed)${NC}"
    echo ""
    echo "💡 Troubleshooting:"
    echo "   1. Make sure all servers are started"
    echo "   2. Wait 1-2 minutes for servers to fully start"
    echo "   3. Check server logs for errors"
    echo "   4. Verify ports are not in use by other applications"
    echo ""
    echo "📝 To start the cluster, run:"
    echo "   ./start-cluster.sh"
    exit 1
fi

# Made with Bob
