# MBean Counter API Guide

## Overview

This guide explains how to use the JAX-RS REST API to query counter MBean details from cluster members through the Liberty Collective Controller.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Liberty Collective Controller             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  liberty-cluster-app-war (Controller Application)     │  │
│  │  - REST API Endpoints                                 │  │
│  │  - MBean Query via Collective Controller              │  │
│  │  - Base URL: http://localhost:9080/liberty-cluster-app│  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ JMX/MBean Query
                            ▼
┌─────────────────────────────────────────────────────────────┐
│              Cluster Members (member1, member2)              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  liberty-cluster-member-app (Member Application)      │  │
│  │  - Counter MBean (com.example.liberty.member:type=   │  │
│  │    Counter)                                           │  │
│  │  - REST API for local counter operations             │  │
│  │  - Base URL: http://localhost:9081/member-app        │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## Applications

### 1. Controller Application (liberty-cluster-app-war)
- **Deployment**: Liberty Collective Controller
- **Purpose**: Query MBeans from cluster members
- **Context Root**: `/liberty-cluster-app`
- **API Base Path**: `/api`

### 2. Member Application (liberty-cluster-member-app)
- **Deployment**: Cluster Member Servers
- **Purpose**: Expose Counter MBean and local REST API
- **Context Root**: `/member-app`
- **API Base Path**: `/api`
- **MBean Name**: `com.example.liberty.member:type=Counter`

## REST API Endpoints

### Controller Endpoints (Query Members)

#### 1. Get All Member Counters
Query counter values from all cluster members.

**Endpoint**: `GET /api/mbeans/counters`

**Query Parameters**:
- `clusterName` (optional): Filter by cluster name

**Example Request**:
```bash
# Get counters from all members
curl http://localhost:9080/liberty-cluster-app/api/mbeans/counters

# Get counters from specific cluster
curl http://localhost:9080/liberty-cluster-app/api/mbeans/counters?clusterName=myCluster
```

**Example Response**:
```json
{
  "clusterName": "all",
  "totalMembers": 2,
  "successCount": 2,
  "errorCount": 0,
  "members": [
    {
      "serverName": "member1",
      "hostName": "localhost",
      "mbeanMemberName": "member1",
      "counter": 42,
      "totalRequests": 42,
      "status": "success",
      "mbeanObjectName": "WebSphere:feature=collectiveController,type=Runtime,...",
      "timestamp": 1700000000000
    },
    {
      "serverName": "member2",
      "hostName": "localhost",
      "mbeanMemberName": "member2",
      "counter": 35,
      "totalRequests": 35,
      "status": "success",
      "mbeanObjectName": "WebSphere:feature=collectiveController,type=Runtime,...",
      "timestamp": 1700000000000
    }
  ],
  "timestamp": 1700000000000
}
```

#### 2. Get Specific Member Counter
Query counter value from a specific cluster member.

**Endpoint**: `GET /api/mbeans/counters/{serverName}`

**Path Parameters**:
- `serverName`: Name of the cluster member server

**Example Request**:
```bash
curl http://localhost:9080/liberty-cluster-app/api/mbeans/counters/member1
```

**Example Response**:
```json
{
  "serverName": "member1",
  "hostName": "localhost",
  "mbeanMemberName": "member1",
  "counter": 42,
  "totalRequests": 42,
  "status": "success",
  "mbeanObjectName": "WebSphere:feature=collectiveController,type=Runtime,...",
  "timestamp": 1700000000000
}
```

#### 3. List Available Members
Get list of all cluster members.

**Endpoint**: `GET /api/mbeans/members`

**Query Parameters**:
- `clusterName` (optional): Filter by cluster name

**Example Request**:
```bash
curl http://localhost:9080/liberty-cluster-app/api/mbeans/members
```

**Example Response**:
```json
{
  "clusterName": "all",
  "memberCount": 2,
  "members": [
    {
      "serverName": "member1",
      "hostName": "localhost",
      "httpsPort": "9443",
      "state": "STARTED",
      "clusterName": "myCluster"
    },
    {
      "serverName": "member2",
      "hostName": "localhost",
      "httpsPort": "9444",
      "state": "STARTED",
      "clusterName": "myCluster"
    }
  ],
  "timestamp": 1700000000000
}
```

### Member Endpoints (Local Operations)

These endpoints are called directly on the member servers.

#### 1. Increment Counter
Increment the counter and return current value.

**Endpoint**: `GET /api/counter/increment`

**Example Request**:
```bash
# On member1 (port 9081)
curl http://localhost:9081/member-app/api/counter/increment

# On member2 (port 9082)
curl http://localhost:9082/member-app/api/counter/increment
```

**Example Response**:
```json
{
  "memberName": "member1",
  "counter": 43,
  "totalRequests": 43,
  "message": "Counter incremented successfully"
}
```

#### 2. Get Counter Value
Get current counter value without incrementing.

**Endpoint**: `GET /api/counter`

**Example Request**:
```bash
curl http://localhost:9081/member-app/api/counter
```

**Example Response**:
```json
{
  "memberName": "member1",
  "counter": 43,
  "totalRequests": 43
}
```

#### 3. Reset Counter
Reset counter to zero.

**Endpoint**: `POST /api/counter/reset`

**Example Request**:
```bash
curl -X POST http://localhost:9081/member-app/api/counter/reset
```

**Example Response**:
```json
{
  "memberName": "member1",
  "counter": 0,
  "message": "Counter reset successfully"
}
```

## MBean Details

### Counter MBean Interface

**MBean Name**: `com.example.liberty.member:type=Counter`

**Attributes**:
- `Counter` (long): Current counter value
- `TotalRequests` (long): Total number of requests
- `MemberName` (String): Name of the cluster member

**Operations**:
- `resetCounter()`: Reset counter to zero

### MBean Implementation

The Counter MBean is automatically registered on application startup via the `MBeanManager` singleton EJB.

**Key Classes**:
1. `CounterMBean` - MBean interface
2. `Counter` - MBean implementation with AtomicLong counter
3. `MBeanManager` - Singleton EJB that registers/unregisters MBean
4. `CounterResource` - REST API for counter operations

## Testing Workflow

### Step 1: Start the Cluster
```bash
# Start controller and members
./start-cluster.sh
```

### Step 2: Verify Member Applications
```bash
# Check member1
curl http://localhost:9081/member-app/api/counter

# Check member2
curl http://localhost:9082/member-app/api/counter
```

### Step 3: Increment Counters on Members
```bash
# Increment member1 counter 5 times
for i in {1..5}; do
  curl http://localhost:9081/member-app/api/counter/increment
done

# Increment member2 counter 3 times
for i in {1..3}; do
  curl http://localhost:9082/member-app/api/counter/increment
done
```

### Step 4: Query Counters from Controller
```bash
# Get all member counters via controller
curl http://localhost:9080/liberty-cluster-app/api/mbeans/counters | jq

# Get specific member counter
curl http://localhost:9080/liberty-cluster-app/api/mbeans/counters/member1 | jq
```

### Step 5: List Available Members
```bash
curl http://localhost:9080/liberty-cluster-app/api/mbeans/members | jq
```

## Complete Test Script

```bash
#!/bin/bash

echo "=== Testing MBean Counter API ==="
echo ""

# 1. Check member applications
echo "1. Checking member applications..."
echo "Member1:"
curl -s http://localhost:9081/member-app/api/counter | jq
echo ""
echo "Member2:"
curl -s http://localhost:9082/member-app/api/counter | jq
echo ""

# 2. Increment counters
echo "2. Incrementing counters..."
echo "Incrementing member1 counter 10 times..."
for i in {1..10}; do
  curl -s http://localhost:9081/member-app/api/counter/increment > /dev/null
done
echo "Done"

echo "Incrementing member2 counter 7 times..."
for i in {1..7}; do
  curl -s http://localhost:9082/member-app/api/counter/increment > /dev/null
done
echo "Done"
echo ""

# 3. Query via controller
echo "3. Querying counters via controller..."
curl -s http://localhost:9080/liberty-cluster-app/api/mbeans/counters | jq
echo ""

# 4. Query specific member
echo "4. Querying specific member (member1)..."
curl -s http://localhost:9080/liberty-cluster-app/api/mbeans/counters/member1 | jq
echo ""

# 5. List members
echo "5. Listing available members..."
curl -s http://localhost:9080/liberty-cluster-app/api/mbeans/members | jq
echo ""

# 6. Reset counters
echo "6. Resetting counters..."
curl -s -X POST http://localhost:9081/member-app/api/counter/reset | jq
curl -s -X POST http://localhost:9082/member-app/api/counter/reset | jq
echo ""

# 7. Verify reset
echo "7. Verifying reset via controller..."
curl -s http://localhost:9080/liberty-cluster-app/api/mbeans/counters | jq
echo ""

echo "=== Test Complete ==="
```

## Troubleshooting

### Issue: MBean Not Found

**Symptom**: Response shows `"status": "mbean_not_found"`

**Solutions**:
1. Verify member application is deployed and running
2. Check member server logs for MBean registration
3. Ensure `MBeanManager` singleton is starting correctly
4. Verify member server has `ejb-3.2` feature enabled

**Check MBean Registration**:
```bash
# Check member server logs
tail -f liberty-cluster-member1/target/liberty/wlp/usr/servers/member1/logs/messages.log | grep MBean
```

### Issue: Controller Cannot Connect to Members

**Symptom**: Connection errors or timeouts

**Solutions**:
1. Verify collective is properly configured
2. Check member servers are joined to collective
3. Verify network connectivity between controller and members
4. Check firewall settings

**Verify Collective Status**:
```bash
# List collective members
./liberty-cluster-app-ear/target/liberty/wlp/bin/collective list \
  --host=localhost --port=9443 --user=admin --password=admin
```

### Issue: Authentication Errors

**Symptom**: 401 Unauthorized or authentication failures

**Solutions**:
1. Verify admin credentials in server.xml
2. Check keystore and truststore configuration
3. Ensure SSL certificates are properly configured

## Performance Considerations

1. **Caching**: Consider caching member information to reduce collective queries
2. **Timeouts**: Set appropriate JMX connection timeouts
3. **Batch Queries**: Query multiple members in parallel for better performance
4. **Connection Pooling**: Reuse JMX connections when possible

## Security Considerations

1. **Authentication**: Always use secure credentials
2. **SSL/TLS**: Enable HTTPS for production deployments
3. **Authorization**: Implement role-based access control
4. **Audit Logging**: Enable audit logging for MBean access

## Additional Resources

- [WebSphere Liberty Collective Documentation](https://www.ibm.com/docs/en/was-liberty/base?topic=liberty-collectives)
- [JMX in Liberty](https://www.ibm.com/docs/en/was-liberty/base?topic=liberty-monitoring-jmx)
- [JAX-RS 2.1 Specification](https://jcp.org/en/jsr/detail?id=370)

## Summary

This application demonstrates:
- ✅ JAX-RS REST API on Liberty Collective Controller
- ✅ Counter MBean on cluster member servers
- ✅ Remote MBean query from controller to members
- ✅ J2EE 8 features (JAX-RS 2.1, EJB 3.2, JMX)
- ✅ WebSphere Liberty Collective integration
- ✅ Cluster member discovery and monitoring

The controller application queries MBeans from cluster members through the Liberty Collective Controller's MBean routing capabilities, providing a centralized monitoring solution.