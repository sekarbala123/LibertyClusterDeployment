# Liberty Cluster API - Quick Reference

## Endpoints

### 1. List All Clusters
```
GET /api/members/clusters
```
Uses ClusterManager MBean `listClusterNames()` operation.

**Response:**
```json
{
  "clusterCount": 2,
  "clusters": ["cluster1", "cluster2"],
  "timestamp": 1700000000000
}
```

### 2. Get Cluster Members
```
GET /api/members/cluster?clusterName=<name>
```
Uses ClusterManager MBean `listMembers(clusterName)` operation to get member names, then queries Server MBeans for details and application MBeans from each member.

**Response:**
```json
{
  "clusterName": "myCluster",
  "memberCount": 2,
  "members": [
    {
      "serverName": "member1",
      "clusterName": "myCluster",
      "host": "192.168.1.100",
      "hostName": "server1.example.com",
      "httpsPort": "9444",
      "httpPort": "9081",
      "state": "STARTED",
      "userDir": "/opt/liberty/usr",
      "wlpInstallDir": "/opt/liberty/wlp",
      "applicationCount": 2,
      "applications": [
        {
          "applicationName": "myApp",
          "j2eeType": "Application",
          "state": "STARTED",
          "mbeanObjectName": "...",
          "attributes": {...}
        }
      ]
    }
  ],
  "timestamp": 1700000000000
}
```

## Implementation Details

### ClusterManager MBean Operations:
```java
ObjectName clusterMgr = new ObjectName("WebSphere:feature=collectiveController,type=ClusterManager,name=ClusterManager");

// List all clusters
Collection<String> clusters = (Collection<String>) mbs.invoke(clusterMgr, "listClusterNames", null, null);

// List members of a cluster
Collection<String> members = (Collection<String>) mbs.invoke(clusterMgr, "listMembers", 
    new Object[] {clusterName}, new String[] {String.class.getName()});
```

### Server MBean Query:
```java
ObjectName serverMBean = new ObjectName("WebSphere:feature=collectiveController,type=Server,name=" + memberName);
// Attributes: Host, HostName, HttpsPort, HttpPort, State, UserDir, WlpInstallDir
```

### Application MBean Query:
```java
String pattern = "WebSphere:feature=collectiveController,type=Runtime,name=<host>,*,j2eeType=Application,*";
// Queries application MBeans from member servers through controller
```

## Testing

```bash
# List all clusters
curl -k "https://localhost:9443/liberty-cluster-app/api/members/clusters"

# Get cluster members with application MBeans
curl -k "https://localhost:9443/liberty-cluster-app/api/members/cluster?clusterName=myCluster"
```

## Deployment

Deploy to controller's dropins:
```
liberty-cluster-app-ear/target/liberty-cluster-app-ear-1.0-SNAPSHOT.ear