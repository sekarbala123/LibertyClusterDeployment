# Liberty Cluster Counter Application Documentation

## Overview

This document describes the counter application deployed on Liberty Cluster members and the collector REST API that retrieves counter values from all members.

## Architecture

### Components

1. **Member Application** (`liberty-cluster-member-app`)
   - Deployed on each cluster member (member1, member2)
   - Exposes REST API for counter operations
   - Registers an MBean to expose counter metrics
   - Increments counter on each API request

2. **Collector Application** (`liberty-cluster-app-war`)
   - Deployed on the collective controller
   - Queries collective controller MBeans to discover members
   - Retrieves counter MBean values from all members
   - Provides aggregated view of counter metrics

## Member Application

### REST API Endpoints

Base URL: `http://localhost:<port>/member-app/api`

#### 1. Increment Counter
```
GET /counter/increment
```

Increments the counter and returns current value.

**Response:**
```json
{
  "memberName": "member1",
  "counter": 5,
  "totalRequests": 5,
  "message": "Counter incremented successfully"
}
```

#### 2. Get Counter Value
```
GET /counter
```

Returns current counter value without incrementing.

**Response:**
```json
{
  "memberName": "member1",
  "counter": 5,
  "totalRequests": 5
}
```

#### 3. Reset Counter
```
POST /counter/reset
```

Resets the counter to zero.

**Response:**
```json
{
  "memberName": "member1",
  "counter": 0,
  "message": "Counter reset successfully"
}
```

### MBean Details

**MBean Name:** `com.example.liberty.member:type=Counter`

**Attributes:**
- `Counter` (Long): Current counter value
- `TotalRequests` (Long): Total number of requests
- `MemberName` (String): Name of the member server

**Operations:**
- `resetCounter()`: Resets the counter to zero

### Member Ports

- **Member1:**
  - HTTP: 9081
  - HTTPS: 9444
  
- **Member2:**
  - HTTP: 9082
  - HTTPS: 9445

## Collector Application

### REST API Endpoints

Base URL: `https://localhost:9443/liberty-cluster-app-war-1.0-SNAPSHOT/api`

#### 1. Get All Member Counters
```
GET /mbeans/counters
GET /mbeans/counters?clusterName=<cluster-name>
```

Retrieves counter values from all members or members in a specific cluster.

**Response:**
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
      "counter": 10,
      "totalRequests": 10,
      "status": "success",
      "timestamp": 1700000000000
    },
    {
      "serverName": "member2",
      "hostName": "localhost",
      "mbeanMemberName": "member2",
      "counter": 15,
      "totalRequests": 15,
      "status": "success",
      "timestamp": 1700000000000
    }
  ],
  "timestamp": 1700000000000
}
```

#### 2. Get Specific Member Counter
```
GET /mbeans/counters/{serverName}
```

Retrieves counter value from a specific member.

**Example:**
```
GET /mbeans/counters/member1
```

**Response:**
```json
{
  "serverName": "member1",
  "hostName": "localhost",
  "mbeanMemberName": "member1",
  "counter": 10,
  "totalRequests": 10,
  "status": "success",
  "mbeanObjectName": "WebSphere:feature=collectiveController,type=Runtime,...",
  "timestamp": 1700000000000
}
```

#### 3. Get Available Members
```
GET /mbeans/members
GET /mbeans/members?clusterName=<cluster-name>
```

Lists all available cluster members.

**Response:**
```json
{
  "clusterName": "all",
  "memberCount": 2,
  "members": [
    {
      "serverName": "member1",
      "hostName": "localhost",
      "httpsPort": "9444",
      "state": "STARTED"
    },
    {
      "serverName": "member2",
      "hostName": "localhost",
      "httpsPort": "9445",
      "state": "STARTED"
    }
  ],
  "timestamp": 1700000000000
}
```

## Testing the Application

### 1. Test Member Application Directly

**Increment counter on member1:**
```bash
curl http://localhost:9081/member-app/api/counter/increment
```

**Get counter value:**
```bash
curl http://localhost:9081/member-app/api/counter
```

**Reset counter:**
```bash
curl -X POST http://localhost:9081/member-app/api/counter/reset
```

### 2. Test Collector Application

**Get all member counters:**
```bash
curl -k -u admin:admin123! https://localhost:9443/liberty-cluster-app-war-1.0-SNAPSHOT/api/mbeans/counters
```

**Get specific member counter:**
```bash
curl -k -u admin:admin123! https://localhost:9443/liberty-cluster-app-war-1.0-SNAPSHOT/api/mbeans/counters/member1
```

**Get available members:**
```bash
curl -k -u admin:admin123! https://localhost:9443/liberty-cluster-app-war-1.0-SNAPSHOT/api/mbeans/members
```

### 3. Test Scenario

1. **Increment counters on both members:**
   ```bash
   # Member1 - increment 5 times
   for i in {1..5}; do curl http://localhost:9081/member-app/api/counter/increment; done
   
   # Member2 - increment 3 times
   for i in {1..3}; do curl http://localhost:9082/member-app/api/counter/increment; done
   ```

2. **Query all counters from collector:**
   ```bash
   curl -k -u admin:admin123! https://localhost:9443/liberty-cluster-app-war-1.0-SNAPSHOT/api/mbeans/counters
   ```

3. **Expected result:**
   - Member1 counter: 5
   - Member2 counter: 3

## Implementation Details

### Member Application Components

1. **CounterMBean Interface**
   - Defines MBean contract
   - Exposes counter attributes and operations

2. **Counter Class**
   - Implements CounterMBean
   - Uses AtomicLong for thread-safe counter operations
   - Stores member name for identification

3. **MBeanManager**
   - Singleton EJB with @Startup
   - Registers Counter MBean on application startup
   - Unregisters MBean on application shutdown
   - Uses platform MBean server

4. **CounterResource**
   - JAX-RS resource for REST API
   - Injects MBeanManager via EJB
   - Provides endpoints for counter operations

5. **MemberApplication**
   - JAX-RS application configuration
   - Maps API to `/api` path

### Collector Application Components

1. **MemberMBeanResource**
   - Queries collective controller MBeans
   - Discovers cluster members dynamically
   - Retrieves counter MBean values through collective controller
   - Provides aggregated view of all member counters

### MBean Access Pattern

The collector accesses member MBeans through the collective controller's MBean routing:

```
WebSphere:feature=collectiveController,type=Runtime,
  name=<host>,host=<host>,server=<server>,type=Counter,*
```

This pattern allows the controller to route MBean requests to the appropriate member server.

## Configuration

### Member Server Configuration

Each member server requires:

1. **Features:**
   - `collectiveMember-1.0`
   - `jaxrs-2.1`
   - `servlet-4.0`
   - `cdi-2.0`
   - `ejb-3.2`
   - `jsonp-1.1`
   - `jsonb-1.0`
   - `restConnector-2.0`

2. **Application Deployment:**
   ```xml
   <application id="member-app" 
                name="member-app" 
                location="${shared.app.dir}/member-app.war"
                type="war">
       <classloader delegation="parentLast" />
   </application>
   ```

3. **Member Identification:**
   ```xml
   <variable name="member.name" value="member1" />
   ```

### Controller Server Configuration

The controller requires:
- `collectiveController-1.0`
- `restConnector-2.0`
- All Java EE 8 features for the collector application

## Troubleshooting

### Counter MBean Not Found

If the collector reports "mbean_not_found":

1. **Verify member application is deployed:**
   ```bash
   curl http://localhost:9081/member-app/api/counter
   ```

2. **Check member server logs:**
   ```bash
   tail -f liberty-cluster-member1/target/liberty/wlp/usr/servers/member1/logs/messages.log
   ```

3. **Verify MBean registration:**
   Look for log message: "Successfully registered MBean: com.example.liberty.member:type=Counter"

### Connection Errors

1. **Verify member servers are running:**
   ```bash
   ./liberty-cluster-member1/target/liberty/wlp/bin/server status member1
   ./liberty-cluster-member2/target/liberty/wlp/bin/server status member2
   ```

2. **Check collective membership:**
   ```bash
   curl -k -u admin:admin123! https://localhost:9443/liberty-cluster-app-war-1.0-SNAPSHOT/api/members
   ```

### Authentication Issues

Ensure you're using correct credentials:
- Username: `admin`
- Password: `admin123!`

## Building and Deployment

### Build All Applications

```bash
mvn clean package
```

### Build Member Application Only

```bash
cd liberty-cluster-member-app
mvn clean package
```

### Deploy Member Application

```bash
cp liberty-cluster-member-app/target/member-app.war \
   liberty-cluster-member1/target/liberty/wlp/usr/shared/apps/

cp liberty-cluster-member-app/target/member-app.war \
   liberty-cluster-member2/target/liberty/wlp/usr/shared/apps/
```

### Restart Servers

```bash
# Restart member1
./liberty-cluster-member1/target/liberty/wlp/bin/server stop member1
./liberty-cluster-member1/target/liberty/wlp/bin/server start member1

# Restart member2
./liberty-cluster-member2/target/liberty/wlp/bin/server stop member2
./liberty-cluster-member2/target/liberty/wlp/bin/server start member2
```

## Security Considerations

1. **MBean Access:** MBeans are accessed through the collective controller's secure REST connector
2. **Authentication:** All collector API calls require basic authentication
3. **SSL/TLS:** HTTPS is used for secure communication
4. **Authorization:** Only users with administrator role can access MBeans

## Performance Considerations

1. **Thread Safety:** Counter uses AtomicLong for lock-free thread-safe operations
2. **MBean Registration:** Performed once at application startup
3. **Collective Queries:** Collector queries are routed through the controller's MBean infrastructure
4. **Caching:** Consider implementing caching for frequently accessed counter values

## Future Enhancements

1. **Metrics Export:** Export counter metrics to monitoring systems (Prometheus, Grafana)
2. **Historical Data:** Store counter history in a database
3. **Alerts:** Configure alerts for counter thresholds
4. **Dashboard:** Create a web dashboard for real-time counter visualization
5. **Cluster-wide Operations:** Add endpoints to reset all counters simultaneously

## References

- [IBM WebSphere Liberty Collective Documentation](https://www.ibm.com/docs/en/was-liberty/base?topic=liberty-collectives)
- [Java Management Extensions (JMX) Guide](https://docs.oracle.com/javase/tutorial/jmx/)
- [JAX-RS 2.1 Specification](https://jakarta.ee/specifications/restful-ws/2.1/)
- [EJB 3.2 Specification](https://jakarta.ee/specifications/enterprise-beans/3.2/)