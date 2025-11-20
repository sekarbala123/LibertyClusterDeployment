# Liberty Cluster Members API Documentation

## Overview
This document describes the new JAX-RS REST API endpoint for querying Liberty Collective cluster members by cluster name.

## API Endpoint

### Get Cluster Members by Cluster Name

**Endpoint:** `GET /api/members/cluster`

**Description:** Retrieves information about all members of a specific Liberty Collective cluster, including their host information and HTTPS port.

**Query Parameters:**
- `clusterName` (required): The name of the cluster to query

**Example Request:**
```bash
curl -k "https://localhost:9443/liberty-cluster-app/api/members/cluster?clusterName=myCluster"
```

**Success Response (200 OK):**
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
          "mbeanObjectName": "WebSphere:feature=collectiveController,type=Runtime,name=server1.example.com,host=server1.example.com,server=member1,j2eeType=Application,name=myApp",
          "attributes": {
            "state": "STARTED",
            "deploymentDescriptor": "/path/to/app.ear"
          }
        },
        {
          "applicationName": "anotherApp",
          "j2eeType": "Application",
          "state": "STARTED",
          "mbeanObjectName": "WebSphere:feature=collectiveController,type=Runtime,name=server1.example.com,host=server1.example.com,server=member1,j2eeType=Application,name=anotherApp",
          "attributes": {
            "state": "STARTED"
          }
        }
      ]
    },
    {
      "serverName": "member2",
      "clusterName": "myCluster",
      "host": "192.168.1.101",
      "hostName": "server2.example.com",
      "httpsPort": "9445",
      "httpPort": "9082",
      "state": "STARTED",
      "userDir": "/opt/liberty/usr",
      "wlpInstallDir": "/opt/liberty/wlp",
      "applicationCount": 1,
      "applications": [
        {
          "applicationName": "myApp",
          "j2eeType": "Application",
          "state": "STARTED",
          "mbeanObjectName": "WebSphere:feature=collectiveController,type=Runtime,name=server2.example.com,host=server2.example.com,server=member2,j2eeType=Application,name=myApp",
          "attributes": {
            "state": "STARTED"
          }
        }
      ]
    }
  ],
  "timestamp": 1700000000000
}
```

**Error Response - Missing Parameter (400 Bad Request):**
```json
{
  "error": "Missing required parameter: clusterName",
  "usage": "GET /members/cluster?clusterName=<cluster-name>"
}
```

**Error Response - Cluster Not Found (404 Not Found):**
```json
{
  "clusterName": "nonExistentCluster",
  "memberCount": 0,
  "members": [],
  "message": "No members found for cluster: nonExistentCluster",
  "timestamp": 1700000000000
}
```

**Error Response - Not on Controller (500 Internal Server Error):**
```json
{
  "error": "This endpoint must be deployed on a Liberty Collective Controller",
  "mbean_not_found": "WebSphere:feature=collectiveController,type=CollectiveRepository,name=CollectiveRepository"
}
```

## Response Fields

### Member Object Fields:
- `serverName` (String): Name of the server/member
- `clusterName` (String): Name of the cluster this member belongs to
- `host` (String): IP address or hostname of the server
- `hostName` (String): Fully qualified domain name of the server
- `httpsPort` (String): HTTPS port number for secure connections
- `httpPort` (String, optional): HTTP port number if configured
- `state` (String): Current state of the server (e.g., STARTED, STOPPED)
- `userDir` (String, optional): Liberty user directory path
- `wlpInstallDir` (String, optional): Liberty installation directory path
- `applicationCount` (Integer): Number of applications deployed on this member
- `applications` (Array): List of application MBean information from the member server

### Application Object Fields (within each member):
- `applicationName` (String): Name of the deployed application
- `j2eeType` (String): J2EE type of the MBean (typically "Application")
- `state` (String, optional): Current state of the application (e.g., STARTED, STOPPED)
- `deploymentDescriptor` (String, optional): Path to the deployment descriptor
- `mbeanObjectName` (String): Full MBean ObjectName for reference
- `attributes` (Object): Map of all readable MBean attributes and their values

## Implementation Details

### Technology Stack:
- **JAX-RS 2.1** (Java API for RESTful Web Services)
- **Java EE 8**
- **IBM WebSphere Liberty** (Collective Controller feature)
- **JMX MBeans** for querying collective information

### Key Components:

1. **CollectiveMembersResource.java**
   - JAX-RS resource class with the `/members` base path
   - Contains the `getClusterMembers()` method for cluster queries
   - Uses JMX MBeans to query the collective controller

2. **JAXRSConfiguration.java**
   - JAX-RS application configuration
   - Defines the `/api` base path for all REST endpoints

3. **getApplicationMBeansFromMember() Helper Method**
   - Queries application MBeans from member servers through the controller
   - Retrieves application state, deployment information, and all available attributes
   - Handles multiple MBean patterns for comprehensive application discovery

### MBean Usage:
The API uses the following Liberty Collective MBeans:

**Server Information:**
- `WebSphere:feature=collectiveController,type=CollectiveRepository,name=CollectiveRepository` - Verifies controller status
- `WebSphere:feature=collectiveController,type=Server,*` - Queries server information

**Application Information (from member servers):**
- `WebSphere:feature=collectiveController,type=Runtime,name=<host>,host=<host>,server=<server>,*` - Runtime MBeans
- `WebSphere:feature=collectiveController,type=Runtime,name=<host>,*,j2eeType=Application,*` - Application MBeans

### Attributes Retrieved:

**Server Attributes:**
- `Cluster` - Cluster membership information
- `Host` - Server host IP address
- `HostName` - Server hostname
- `HttpsPort` - HTTPS port number
- `HttpPort` - HTTP port number (if available)
- `State` - Server state
- `UserDir` - Liberty user directory
- `WlpInstallDir` - Liberty installation directory

**Application Attributes (from member servers):**
- `state` - Application state (STARTED, STOPPED, etc.)
- `deploymentDescriptor` - Path to deployment descriptor
- All readable MBean attributes specific to each application

## Deployment

### Prerequisites:
1. IBM WebSphere Liberty Server with Collective Controller feature enabled
2. Application must be deployed on the collective controller server
3. Required Liberty features:
   - `collectiveController-1.0`
   - `jaxrs-2.1`
   - `javaee-8.0`

### Deployment Steps:

1. **Build the application:**
   ```bash
   mvn clean package
   ```

2. **Deploy to controller:**
   The EAR file is automatically copied to the controller's dropins directory:
   ```
   liberty-cluster-app-ear/target/liberty/wlp/usr/servers/controller/dropins/
   ```

3. **Start/Restart the controller server:**
   ```bash
   cd WLP/wlp/bin
   ./server start controller
   # or
   ./server restart controller
   ```

4. **Verify deployment:**
   Check the server logs for successful application deployment:
   ```bash
   tail -f WLP/wlp/usr/servers/controller/logs/messages.log
   ```

## Testing

### Using curl:
```bash
# Test with a valid cluster name
curl -k "https://localhost:9443/liberty-cluster-app/api/members/cluster?clusterName=myCluster"

# Test without cluster name (should return 400)
curl -k "https://localhost:9443/liberty-cluster-app/api/members/cluster"

# Test with non-existent cluster (should return 404)
curl -k "https://localhost:9443/liberty-cluster-app/api/members/cluster?clusterName=nonExistent"
```

### Using a REST client:
- **URL:** `https://localhost:9443/liberty-cluster-app/api/members/cluster?clusterName=myCluster`
- **Method:** GET
- **Headers:** Accept: application/json

## Additional Endpoints

The application also includes these existing endpoints:

1. **GET /api/members** - Get all collective members
2. **GET /api/members/detailed** - Get detailed member information
3. **GET /api/members/repo-operations** - Get repository operations
4. **GET /api/members/mbeans** - Get all WebSphere MBeans

## Security Considerations

1. **HTTPS Required:** The API should be accessed over HTTPS in production
2. **Authentication:** Configure Liberty security features for authentication
3. **Authorization:** Restrict access to authorized users only
4. **SSL/TLS:** Ensure proper SSL/TLS configuration on the controller

## Troubleshooting

### Common Issues:

1. **"This endpoint must be deployed on a Liberty Collective Controller"**
   - Ensure the application is deployed on the controller server, not a member
   - Verify `collectiveController-1.0` feature is enabled

2. **"No members found for cluster"**
   - Verify the cluster name is correct
   - Check that servers are properly joined to the cluster
   - Ensure servers have the `Cluster` attribute set

3. **Connection refused**
   - Verify the controller server is running
   - Check the HTTPS port configuration
   - Ensure firewall rules allow access

## Key Features

### Application MBean Querying
The API now includes the ability to query application MBean states from member servers through the collective controller. This provides:

1. **Real-time Application Status** - Get current state of applications on each member
2. **Deployment Information** - Access deployment descriptors and configuration
3. **Comprehensive Attributes** - All readable MBean attributes are included
4. **Centralized Monitoring** - Query all member applications from the controller

### How It Works
1. The controller maintains connections to all member servers
2. When querying a cluster, the API:
   - Identifies all members in the specified cluster
   - For each member, queries its application MBeans through the controller
   - Retrieves application state, deployment info, and all available attributes
   - Aggregates the information in the response

### MBean Patterns Used
The implementation queries multiple MBean patterns to ensure comprehensive application discovery:
- Runtime MBeans for general member information
- Application-specific MBeans with j2eeType=Application
- All readable attributes from discovered MBeans

## Future Enhancements

Potential improvements for the API:
1. Add filtering by server state (STARTED, STOPPED, etc.)
2. Add filtering by application state
3. Include cluster health metrics and application performance data
4. Add pagination for large clusters
5. Support for multiple cluster queries in a single request
6. WebSocket support for real-time cluster and application updates
7. Add application-specific operations (start, stop, restart)
8. Include JVM metrics and thread pool information

## Contact & Support

For issues or questions about this API, please refer to:
- IBM WebSphere Liberty documentation
- Liberty Collective documentation
- Project repository

---
**Version:** 1.0.0  
**Last Updated:** 2025-11-19  
**Author:** Bob IDE