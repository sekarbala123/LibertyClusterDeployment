# Controller MBean Query Application

A simple JAX-RS WAR application for querying Counter MBeans from Liberty cluster members through the Collective Controller.

## 📋 Overview

This application provides REST API endpoints to query the Counter MBean deployed in `liberty-cluster-member-app` on cluster member servers. It uses the Liberty Collective Controller's MBean routing capabilities to access member MBeans remotely.

## 🏗️ Architecture

```
Controller Server (Port 9080)
├── controller-mbean-app.war
│   └── REST API Endpoints
│       ├── GET /api/counters (all members)
│       ├── GET /api/counters/{serverName} (specific member)
│       └── GET /api/counters/members (list members)
│
└── Queries via JMX ──────────────────┐
                                       │
                                       ▼
Member Servers (Ports 9081, 9082)
├── liberty-cluster-member-app.war
│   └── Counter MBean
│       ├── Counter (long)
│       ├── TotalRequests (long)
│       └── MemberName (String)
```

## 🚀 Quick Start

### Prerequisites

1. Liberty Collective Controller configured and running
2. Cluster member servers joined to the collective
3. `liberty-cluster-member-app` deployed on member servers

### Build the Application

```bash
cd controller-mbean-app
mvn clean package
```

This will create `target/controller-mbean-app.war`

### Deploy to Controller

#### Option 1: Using Liberty Maven Plugin

```bash
mvn liberty:run
```

#### Option 2: Manual Deployment

1. Copy the WAR file to the controller's dropins directory:
```bash
cp target/controller-mbean-app.war <LIBERTY_HOME>/usr/servers/controller/dropins/
```

2. Start the controller server:
```bash
<LIBERTY_HOME>/bin/server start controller
```

### Access the Application

- **Web UI**: http://localhost:9080/controller-mbean-app/
- **API Base**: http://localhost:9080/controller-mbean-app/api/

## 📡 API Endpoints

### 1. Get All Member Counters

Query counter values from all cluster members.

**Request:**
```bash
GET /api/counters
```

**Example:**
```bash
curl http://localhost:9080/controller-mbean-app/api/counters | jq
```

**Response:**
```json
{
  "totalMembers": 2,
  "successCount": 2,
  "errorCount": 0,
  "members": [
    {
      "serverName": "member1",
      "hostName": "localhost",
      "memberName": "member1",
      "counter": 42,
      "totalRequests": 42,
      "status": "success",
      "mbeanObjectName": "WebSphere:feature=collectiveController,type=Runtime,...",
      "timestamp": 1700000000000
    },
    {
      "serverName": "member2",
      "hostName": "localhost",
      "memberName": "member2",
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

### 2. Get Specific Member Counter

Query counter value from a specific cluster member.

**Request:**
```bash
GET /api/counters/{serverName}
```

**Example:**
```bash
curl http://localhost:9080/controller-mbean-app/api/counters/member1 | jq
```

**Response:**
```json
{
  "serverName": "member1",
  "hostName": "localhost",
  "memberName": "member1",
  "counter": 42,
  "totalRequests": 42,
  "status": "success",
  "mbeanObjectName": "WebSphere:feature=collectiveController,type=Runtime,...",
  "timestamp": 1700000000000
}
```

### 3. List Available Members

Get list of all cluster members in the collective.

**Request:**
```bash
GET /api/counters/members
```

**Example:**
```bash
curl http://localhost:9080/controller-mbean-app/api/counters/members | jq
```

**Response:**
```json
{
  "memberCount": 2,
  "members": [
    {
      "serverName": "member1",
      "hostName": "localhost",
      "state": "STARTED",
      "clusterName": "myCluster"
    },
    {
      "serverName": "member2",
      "hostName": "localhost",
      "state": "STARTED",
      "clusterName": "myCluster"
    }
  ],
  "timestamp": 1700000000000
}
```

## 🧪 Testing

### Complete Test Workflow

```bash
# 1. Increment counters on member servers
curl http://localhost:9081/member-app/api/counter/increment
curl http://localhost:9081/member-app/api/counter/increment
curl http://localhost:9082/member-app/api/counter/increment

# 2. Query all counters from controller
curl http://localhost:9080/controller-mbean-app/api/counters | jq

# 3. Query specific member
curl http://localhost:9080/controller-mbean-app/api/counters/member1 | jq

# 4. List all members
curl http://localhost:9080/controller-mbean-app/api/counters/members | jq
```

### Automated Test Script

```bash
#!/bin/bash

echo "Testing Controller MBean Query Application"

# Test 1: List members
echo "1. Listing cluster members..."
curl -s http://localhost:9080/controller-mbean-app/api/counters/members | jq

# Test 2: Increment counters
echo "2. Incrementing counters on members..."
for i in {1..5}; do
  curl -s http://localhost:9081/member-app/api/counter/increment > /dev/null
  curl -s http://localhost:9082/member-app/api/counter/increment > /dev/null
done

# Test 3: Query all counters
echo "3. Querying all member counters..."
curl -s http://localhost:9080/controller-mbean-app/api/counters | jq

# Test 4: Query specific member
echo "4. Querying member1 counter..."
curl -s http://localhost:9080/controller-mbean-app/api/counters/member1 | jq

echo "Test complete!"
```

## 🔧 Configuration

### Server Configuration (server.xml)

The application requires the following Liberty features:

```xml
<featureManager>
    <feature>jaxrs-2.1</feature>
    <feature>jsonp-1.1</feature>
    <feature>cdi-2.0</feature>
    <feature>collectiveController-1.0</feature>
    <feature>restConnector-2.0</feature>
    <feature>ssl-1.0</feature>
</featureManager>
```

### Application Configuration

- **Context Root**: `/controller-mbean-app`
- **API Base Path**: `/api`
- **HTTP Port**: 9080
- **HTTPS Port**: 9443

## 📊 Counter MBean Details

### MBean Name
```
com.example.liberty.member:type=Counter
```

### Attributes
- `Counter` (long) - Current counter value
- `TotalRequests` (long) - Total number of requests processed
- `MemberName` (String) - Name of the cluster member

### Operations
- `resetCounter()` - Reset counter to zero

## 🔍 How It Works

1. **MBean Registration**: The Counter MBean is registered on each member server by the `MBeanManager` singleton EJB in `liberty-cluster-member-app`

2. **Collective Routing**: The Liberty Collective Controller provides MBean routing capabilities that allow querying member MBeans remotely

3. **MBean Pattern**: Member MBeans are accessed through the controller using the pattern:
   ```
   WebSphere:feature=collectiveController,type=Runtime,name=<host>,host=<host>,server=<server>,type=Counter,*
   ```

4. **REST API**: The JAX-RS resource queries the MBean through the platform MBeanServer and returns JSON responses

## 🐛 Troubleshooting

### Issue: MBean Not Found

**Symptom**: Response shows `"status": "mbean_not_found"`

**Solutions**:
1. Verify `liberty-cluster-member-app` is deployed and running on member servers
2. Check member server logs for MBean registration messages
3. Ensure member servers are properly joined to the collective
4. Verify the Counter MBean is registered:
   ```bash
   # Check member server logs
   tail -f <LIBERTY_HOME>/usr/servers/member1/logs/messages.log | grep MBean
   ```

### Issue: No Members Found

**Symptom**: Response shows `"memberCount": 0`

**Solutions**:
1. Verify member servers are joined to the collective
2. Check collective controller status
3. Verify member servers are running

**Check Collective Status**:
```bash
<LIBERTY_HOME>/bin/collective list \
  --host=localhost --port=9443 \
  --user=admin --password=admin
```

### Issue: Connection Errors

**Symptom**: Connection refused or timeout errors

**Solutions**:
1. Verify controller server is running
2. Check firewall settings
3. Verify ports are not blocked
4. Check server.xml configuration

## 📁 Project Structure

```
controller-mbean-app/
├── pom.xml
├── README.md
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── example/
        │           └── controller/
        │               ├── RestApplication.java
        │               └── MemberCounterResource.java
        ├── liberty/
        │   └── config/
        │       └── server.xml
        └── webapp/
            ├── WEB-INF/
            │   └── web.xml
            └── index.html
```

## 🔐 Security

- Basic authentication with admin/admin credentials (change for production)
- SSL/TLS enabled for secure communication
- REST connector secured with quickStartSecurity

**Production Recommendations**:
1. Use LDAP or other enterprise authentication
2. Configure proper SSL certificates
3. Enable audit logging
4. Implement role-based access control

## 📚 Related Documentation

- [Liberty Collective Documentation](https://www.ibm.com/docs/en/was-liberty/base?topic=liberty-collectives)
- [JAX-RS 2.1 Specification](https://jcp.org/en/jsr/detail?id=370)
- [JMX in Liberty](https://www.ibm.com/docs/en/was-liberty/base?topic=liberty-monitoring-jmx)

## 🤝 Integration with Member Application

This application works in conjunction with `liberty-cluster-member-app`:

1. **Member App** (`liberty-cluster-member-app`):
   - Deploys on cluster member servers
   - Registers Counter MBean
   - Provides local REST API for counter operations

2. **Controller App** (`controller-mbean-app`):
   - Deploys on collective controller
   - Queries member MBeans remotely
   - Provides centralized monitoring API

## 📝 License

This is a sample application for demonstration purposes.

## 👥 Support

For issues or questions, refer to the project documentation or IBM WebSphere Liberty support resources.