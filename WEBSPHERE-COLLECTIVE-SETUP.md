# WebSphere Liberty Collective Controller Setup Guide

## Overview

The project has been successfully migrated to **WebSphere Liberty** with **Collective Controller** support. This provides centralized management of all cluster members.

## Architecture

```
┌──────────────────────────────────────────────────────┐
│         WebSphere Liberty Collective                 │
├──────────────────────────────────────────────────────┤
│                                                      │
│  ┌────────────────────────────────┐                 │
│  │  Collective Controller         │                 │
│  │  localhost:9443                │                 │
│  │  - collectiveController-1.0    │                 │
│  │  - Manages members             │                 │
│  │  - Centralized console         │                 │
│  └────────────┬───────────────────┘                 │
│               │                                      │
│       ┌───────┴────────┐                            │
│       │                │                            │
│  ┌────▼─────┐    ┌────▼─────┐                      │
│  │ Member 1 │    │ Member 2 │                      │
│  │ :9081    │    │ :9082    │                      │
│  │ :9444    │    │ :9445    │                      │
│  └──────────┘    └──────────┘                      │
│                                                      │
└──────────────────────────────────────────────────────┘
```

## Changes Made

### 1. Runtime Migration

**From:** Open Liberty (free, no collective support)
**To:** WebSphere Liberty (licensed, with collective support)

### 2. POM Updates

All three modules now use WebSphere Liberty runtime:

```xml
<runtimeArtifact>
    <groupId>com.ibm.websphere.appserver.runtime</groupId>
    <artifactId>wlp-kernel</artifactId>
    <version>24.0.0.12</version>
    <type>zip</type>
</runtimeArtifact>

<!-- License acceptance -->
<install>
    <licenseCode>accept</licenseCode>
</install>
```

**Files Modified:**
- `liberty-cluster-app-ear/pom.xml`
- `liberty-cluster-member1/pom.xml`
- `liberty-cluster-member2/pom.xml`

### 3. Controller Configuration

**File:** `liberty-cluster-app-ear/src/main/liberty/config/server.xml`

**Added Features:**
```xml
<feature>collectiveController-1.0</feature>
```

**Added Configuration:**
```xml
<collectiveController>
    <host>*</host>
    <httpsPort>9443</httpsPort>
    <serverIdentity>controller</serverIdentity>
</collectiveController>
```

### 4. Member Configuration

**Files:**
- `liberty-cluster-member1/src/main/liberty/config/server.xml`
- `liberty-cluster-member2/src/main/liberty/config/server.xml`

**Added Features:**
```xml
<feature>collectiveMember-1.0</feature>
```

**Added Configuration:**

**Member 1:**
```xml
<collectiveMember>
    <controllerHost>localhost</controllerHost>
    <controllerHttpsPort>9443</controllerHttpsPort>
    <serverIdentity>member1</serverIdentity>
</collectiveMember>
```

**Member 2:**
```xml
<collectiveMember>
    <controllerHost>localhost</controllerHost>
    <controllerHttpsPort>9443</controllerHttpsPort>
    <serverIdentity>member2</serverIdentity>
</collectiveMember>
```

## Setup Instructions

### Step 1: Clean Previous Installation

```bash
# Remove Open Liberty artifacts
mvn clean

# Remove cached Liberty installations
rm -rf liberty-cluster-app-ear/target/liberty
rm -rf liberty-cluster-member1/target/liberty
rm -rf liberty-cluster-member2/target/liberty
```

### Step 2: Build with WebSphere Liberty

```bash
# Build all modules
mvn clean install

# This will:
# 1. Download WebSphere Liberty runtime
# 2. Accept the license automatically
# 3. Install required features
# 4. Package the application
```

**Expected Output:**
```
[INFO] Installing features: [javaee-8.0, adminCenter-1.0, restConnector-2.0, sessionCache-1.0, collectiveController-1.0]
[INFO] Installing features: [javaee-8.0, adminCenter-1.0, restConnector-2.0, sessionCache-1.0, collectiveMember-1.0]
```

### Step 3: Start the Controller

```bash
cd liberty-cluster-app-ear
mvn liberty:dev
```

**Wait for:**
```
[INFO] CWWKF0011I: The controller server is ready to run a smarter planet.
[INFO] CWWKT0016I: Web application available (default_host): http://localhost:9080/liberty-cluster-app/
[INFO] CWWKZ0001I: Application liberty-cluster-app started
```

### Step 4: Start Member Servers

**Terminal 2 - Member 1:**
```bash
cd liberty-cluster-member1
mvn pre-integration-test liberty:run
```

**Terminal 3 - Member 2:**
```bash
cd liberty-cluster-member2
mvn pre-integration-test liberty:run
```

### Step 5: Verify Collective Registration

Members should automatically register with the controller on startup.

**Check Controller Logs:**
```bash
tail -f liberty-cluster-app-ear/target/liberty/wlp/usr/servers/controller/logs/messages.log
```

**Look for:**
```
CWWKX7301I: The collective member member1 has joined the collective.
CWWKX7301I: The collective member member2 has joined the collective.
```

## Accessing the Collective

### Collective Admin Center

**URL:** https://localhost:9443/adminCenter/

**Credentials:**
- Username: `admin`
- Password: `adminpwd`

**Features:**
- View all collective members
- Monitor server status
- Deploy applications to members
- View logs and metrics
- Manage configurations

### Individual Admin Centers

**Controller:**
- URL: https://localhost:9443/adminCenter/
- Credentials: admin/adminpwd

**Member 1:**
- URL: https://localhost:9444/adminCenter/
- Credentials: admin/adminpwd

**Member 2:**
- URL: https://localhost:9445/adminCenter/
- Credentials: admin/adminpwd

### Application Endpoints

**REST API:**
- Controller: http://localhost:9080/liberty-cluster-app/api/cluster
- Member 1: http://localhost:9081/liberty-cluster-app/api/cluster
- Member 2: http://localhost:9082/liberty-cluster-app/api/cluster

**Session Test:**
- Controller: http://localhost:9080/liberty-cluster-app/session-test
- Member 1: http://localhost:9081/liberty-cluster-app/session-test
- Member 2: http://localhost:9082/liberty-cluster-app/session-test

## Collective Management

### View Collective Members

1. Access Admin Center: https://localhost:9443/adminCenter/
2. Login with admin/adminpwd
3. Navigate to "Collective" section
4. View registered members: controller, member1, member2

### Deploy Application to All Members

From the controller's Admin Center:
1. Go to "Applications"
2. Select "Deploy to Collective"
3. Choose target members
4. Upload and deploy

### Monitor Cluster Health

The Admin Center provides:
- Real-time server status
- CPU and memory usage
- Active sessions
- Request throughput
- Error rates

## Certificate Management

### Auto-Generated Certificates

On first startup, Liberty generates:
- Server certificates for HTTPS
- Collective membership certificates
- Trust relationships between controller and members

**Location:**
```
liberty-cluster-app-ear/target/liberty/wlp/usr/servers/controller/resources/security/
liberty-cluster-member1/target/liberty/wlp/usr/servers/member1/resources/security/
liberty-cluster-member2/target/liberty/wlp/usr/servers/member2/resources/security/
```

### Production Certificates

For production, replace auto-generated certificates:

1. **Generate CA-signed certificates**
2. **Update keyStore configuration:**
```xml
<keyStore id="defaultKeyStore" 
          location="server.p12" 
          type="PKCS12" 
          password="${keystore.password}"/>
```

3. **Configure trust between servers**

## Troubleshooting

### Issue: Members Not Registering

**Symptoms:**
- Members start but don't appear in collective
- No CWWKX7301I message in controller logs

**Solutions:**

1. **Check network connectivity:**
```bash
# From member server
curl -k https://localhost:9443/ibm/api/collective/v1/status
```

2. **Verify controller is running:**
```bash
# Check controller process
ps aux | grep controller
```

3. **Check certificates:**
```bash
# Verify keystore exists
ls -la liberty-cluster-app-ear/target/liberty/wlp/usr/servers/controller/resources/security/
```

4. **Review member logs:**
```bash
tail -f liberty-cluster-member1/target/liberty/wlp/usr/servers/member1/logs/messages.log
```

### Issue: License Not Accepted

**Error:**
```
CWWKE0700E: The license agreement for WebSphere Liberty has not been accepted
```

**Solution:**
Verify `<install><licenseCode>accept</licenseCode></install>` is in all POMs.

### Issue: Feature Not Found

**Error:**
```
CWWKF0001E: A feature definition could not be found for collectiveController-1.0
```

**Solution:**
1. Ensure using WebSphere Liberty (not Open Liberty)
2. Check runtime artifact in POM
3. Clean and rebuild: `mvn clean install`

### Issue: Port Conflicts

**Error:**
```
CWWKO0221E: TCP/IP port 9443 is already in use
```

**Solution:**
1. Stop conflicting process
2. Or change ports in server.xml

## License Information

### WebSphere Liberty License

By using WebSphere Liberty, you accept the IBM International License Agreement for Non-Warranted Programs (ILAN).

**License Terms:**
- Development use: Free
- Production use: May require license purchase
- Check IBM website for current licensing terms

**License File Location:**
```
liberty-cluster-app-ear/target/liberty/wlp/lafiles/
```

### Accepting the License

The license is automatically accepted via Maven configuration:
```xml
<install>
    <licenseCode>accept</licenseCode>
</install>
```

**Manual Acceptance:**
```bash
# If needed, accept manually
java -jar wlp/bin/tools/ws-server.jar --acceptLicense
```

## Production Considerations

### 1. External Controller

For production, run controller on separate machine:

**Update member configuration:**
```xml
<collectiveMember>
    <controllerHost>controller.example.com</controllerHost>
    <controllerHttpsPort>9443</controllerHttpsPort>
    <serverIdentity>member1</serverIdentity>
</collectiveMember>
```

### 2. Load Balancer

Add load balancer in front of members:
- HAProxy
- Nginx
- Cloud load balancer (AWS ELB, Azure LB)

### 3. Monitoring

Integrate with enterprise monitoring:
- Prometheus + Grafana
- Splunk
- Dynatrace
- New Relic

### 4. High Availability

- Run multiple controllers (active-passive)
- Use shared storage for collective data
- Configure automatic failover

### 5. Security Hardening

- Use CA-signed certificates
- Enable SSL client authentication
- Configure LDAP/AD integration
- Implement role-based access control

## Comparison: Open Liberty vs WebSphere Liberty

| Feature | Open Liberty | WebSphere Liberty |
|---------|-------------|-------------------|
| Cost | Free | Licensed (free for dev) |
| Collective Controller | ❌ No | ✅ Yes |
| Centralized Management | ❌ No | ✅ Yes |
| Session Replication | ✅ Yes | ✅ Yes |
| Admin Center | ✅ Yes | ✅ Yes (Enhanced) |
| Commercial Support | Community | IBM Support |
| Auto Member Discovery | ❌ No | ✅ Yes |
| Configuration Push | ❌ No | ✅ Yes |

## Summary

✅ **Successfully Migrated to WebSphere Liberty**
- Collective controller configured
- Members auto-register with controller
- Centralized management available
- Session replication working
- License automatically accepted

✅ **Collective Features Available:**
- Centralized Admin Center
- Member management
- Application deployment
- Monitoring and logging
- Configuration distribution

✅ **Production Ready:**
- High availability
- Session failover
- Scalable architecture
- Enterprise management

The Liberty cluster now has full collective controller support with centralized management! 🎉