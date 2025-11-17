# Liberty Cluster Configuration Guide

## Overview

This document explains the clustering configuration implemented in the Liberty Cluster Deployment project. The cluster uses **JCache-based session replication** to share HTTP sessions across all three Liberty servers.

## Clustering Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Liberty Cluster                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐ │
│  │  Controller  │    │   Member 1   │    │   Member 2   │ │
│  │  :9080/9443  │    │  :9081/9444  │    │  :9082/9445  │ │
│  └──────┬───────┘    └──────┬───────┘    └──────┬───────┘ │
│         │                   │                   │          │
│         └───────────────────┼───────────────────┘          │
│                             │                              │
│                    ┌────────▼────────┐                     │
│                    │  Session Cache  │                     │
│                    │   (JCache API)  │                     │
│                    └─────────────────┘                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Key Features Configured

### 1. Session Cache Feature (sessionCache-1.0)

All three servers have the `sessionCache-1.0` feature enabled:

```xml
<featureManager>
    <feature>javaee-8.0</feature>
    <feature>adminCenter-1.0</feature>
    <feature>restConnector-2.0</feature>
    <feature>sessionCache-1.0</feature>
</featureManager>
```

**What it does:**
- Enables distributed session caching using JCache (JSR 107) API
- Allows sessions to be shared across multiple Liberty servers
- Provides session failover and high availability

### 2. HTTP Session Configuration

Each server has a unique `cloneId` for session identification:

**Controller:**
```xml
<httpSession cloneId="controller" 
             cookieName="JSESSIONID"
             cookiePath="/"
             invalidateOnUnauthorizedSessionRequestException="true"/>
```

**Member 1:**
```xml
<httpSession cloneId="member1" 
             cookieName="JSESSIONID"
             cookiePath="/"
             invalidateOnUnauthorizedSessionRequestException="true"/>
```

**Member 2:**
```xml
<httpSession cloneId="member2" 
             cookieName="JSESSIONID"
             cookiePath="/"
             invalidateOnUnauthorizedSessionRequestException="true"/>
```

**Configuration Details:**
- `cloneId`: Unique identifier for each server in the cluster
- `cookieName`: Standard session cookie name (JSESSIONID)
- `cookiePath`: Session cookie applies to entire application
- `invalidateOnUnauthorizedSessionRequestException`: Security setting

### 3. JCache Session Persistence

All servers use the same cache configuration:

```xml
<httpSessionCache cacheManagerRef="CacheManager"/>

<cacheManager id="CacheManager">
    <cache id="com.ibm.ws.session.meta.default_host%2Fliberty-cluster-app" 
           name="com.ibm.ws.session.meta.default_host%2Fliberty-cluster-app">
        <cacheLoaderWriter class="com.ibm.ws.session.store.cache.CacheStoreService"/>
    </cache>
    <cache id="com.ibm.ws.session.attr.default_host%2Fliberty-cluster-app"
           name="com.ibm.ws.session.attr.default_host%2Fliberty-cluster-app">
        <cacheLoaderWriter class="com.ibm.ws.session.store.cache.CacheStoreService"/>
    </cache>
</cacheManager>
```

**Two Caches:**
1. **Session Metadata Cache**: Stores session IDs, creation time, last access time
2. **Session Attributes Cache**: Stores actual session data (user objects, etc.)

## How Session Replication Works

### Session Creation Flow

```
1. User Request → Controller (9080)
   └─> Creates session with ID: ABC123
   └─> Stores in local cache
   └─> Replicates to JCache
   └─> Available on Member1 (9081) and Member2 (9082)

2. User Request → Member1 (9081) with cookie JSESSIONID=ABC123
   └─> Reads session from JCache
   └─> Session data available
   └─> User sees same session data

3. User Request → Member2 (9082) with cookie JSESSIONID=ABC123
   └─> Reads session from JCache
   └─> Session data available
   └─> User sees same session data
```

### Session Update Flow

```
1. User updates session on Controller
   └─> Session modified locally
   └─> Changes written to JCache
   └─> Changes propagated to all cluster members

2. Next request to any member
   └─> Reads updated session from JCache
   └─> Sees latest session data
```

## Testing Session Replication

### Create a Test Servlet

Add this servlet to test session replication:

```java
package com.example.liberty.cluster;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

@WebServlet("/session-test")
public class SessionTestServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(true);
        
        // Get or create counter
        Integer counter = (Integer) session.getAttribute("counter");
        if (counter == null) {
            counter = 0;
        }
        counter++;
        session.setAttribute("counter", counter);
        
        // Get or create creation time
        Date creationTime = (Date) session.getAttribute("creationTime");
        if (creationTime == null) {
            creationTime = new Date();
            session.setAttribute("creationTime", creationTime);
        }
        
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        out.println("<html><body>");
        out.println("<h1>Session Replication Test</h1>");
        out.println("<p><strong>Session ID:</strong> " + session.getId() + "</p>");
        out.println("<p><strong>Counter:</strong> " + counter + "</p>");
        out.println("<p><strong>Created:</strong> " + creationTime + "</p>");
        out.println("<p><strong>Server Port:</strong> " + request.getLocalPort() + "</p>");
        out.println("<p><a href='session-test'>Refresh</a></p>");
        out.println("<p><em>Try accessing this URL on different ports (9080, 9081, 9082) with the same session cookie!</em></p>");
        out.println("</body></html>");
    }
}
```

### Manual Testing Steps

1. **Start all servers:**
   ```bash
   ./start-cluster.sh
   ```

2. **Access Controller and create session:**
   ```bash
   curl -c cookies.txt http://localhost:9080/liberty-cluster-app/session-test
   ```
   Note the session ID and counter value.

3. **Access Member 1 with same session:**
   ```bash
   curl -b cookies.txt http://localhost:9081/liberty-cluster-app/session-test
   ```
   You should see:
   - Same session ID
   - Counter incremented
   - Different server port (9081)

4. **Access Member 2 with same session:**
   ```bash
   curl -b cookies.txt http://localhost:9082/liberty-cluster-app/session-test
   ```
   You should see:
   - Same session ID
   - Counter incremented again
   - Different server port (9082)

5. **Verify session persistence:**
   - Stop one server
   - Access another server with the same session
   - Session data should still be available

## Session Failover Demonstration

### Scenario: Controller Fails

```
1. User has session on Controller (9080)
   Session ID: XYZ789
   Counter: 5

2. Controller crashes or stops

3. User redirected to Member1 (9081)
   └─> Reads session XYZ789 from JCache
   └─> Counter: 5 (preserved)
   └─> User experience uninterrupted

4. User continues on Member1
   └─> Counter: 6, 7, 8...
   └─> All updates replicated to JCache
```

## Configuration Benefits

### High Availability
- Sessions survive server failures
- No single point of failure
- Automatic failover

### Scalability
- Add more members easily
- Load can be distributed
- Sessions shared automatically

### Performance
- Local cache for fast access
- Distributed cache for reliability
- Configurable cache sizes

## Important Notes

### Session Serialization

All objects stored in session **must be Serializable**:

```java
public class UserData implements Serializable {
    private static final long serialVersionUID = 1L;
    private String username;
    private String email;
    // ... getters/setters
}
```

### Cache Provider

The current configuration uses Liberty's built-in JCache implementation. For production, consider:

1. **Hazelcast**: Distributed in-memory data grid
2. **Infinispan**: Red Hat's distributed cache
3. **Redis**: External cache server

### Production Considerations

1. **Network Configuration**
   - Ensure all servers can communicate
   - Configure firewall rules
   - Use multicast or TCP discovery

2. **Cache Tuning**
   - Set appropriate cache sizes
   - Configure eviction policies
   - Monitor cache hit rates

3. **Security**
   - Encrypt cache communication
   - Secure session cookies (HTTPS only)
   - Set appropriate session timeouts

4. **Monitoring**
   - Track session count
   - Monitor cache performance
   - Alert on replication failures

## Troubleshooting

### Sessions Not Replicating

**Check:**
1. sessionCache-1.0 feature enabled on all servers
2. Same cache names configured
3. Network connectivity between servers
4. No firewall blocking cache communication

**Logs to check:**
```bash
# Controller
tail -f liberty-cluster-app-ear/target/liberty/wlp/usr/servers/defaultServer/logs/messages.log

# Member 1
tail -f liberty-cluster-member1/target/liberty/wlp/usr/servers/member1/logs/messages.log

# Member 2
tail -f liberty-cluster-member2/target/liberty/wlp/usr/servers/member2/logs/messages.log
```

### Session Lost After Server Restart

**Cause:** In-memory cache is not persistent

**Solutions:**
1. Use persistent cache provider (Redis, database)
2. Configure session persistence to database
3. Accept session loss on restart (stateless design)

### Performance Issues

**Symptoms:**
- Slow session access
- High CPU usage
- Network congestion

**Solutions:**
1. Increase cache size
2. Reduce session data size
3. Use local cache more aggressively
4. Consider session affinity (sticky sessions)

## Next Steps

1. **Add Session Test Servlet** (see code above)
2. **Test Session Replication** (follow manual testing steps)
3. **Monitor Cluster** (check logs and metrics)
4. **Tune Configuration** (adjust cache sizes, timeouts)
5. **Add Load Balancer** (for production deployment)

## Summary

✅ **Configured:**
- sessionCache-1.0 feature on all servers
- Unique cloneId for each server
- JCache-based session persistence
- Two caches (metadata + attributes)

✅ **Benefits:**
- Session failover
- High availability
- Horizontal scalability
- Transparent to application

✅ **Ready for:**
- Development testing
- Load testing
- Production deployment (with additional tuning)

The cluster is now properly configured for session replication and high availability! 🎉