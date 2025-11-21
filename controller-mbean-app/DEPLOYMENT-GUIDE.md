# Deployment Guide - Controller MBean Query Application

Complete step-by-step guide for deploying the Controller MBean Query Application on WebSphere Liberty Collective Controller.

## 📋 Prerequisites

### 1. Liberty Collective Setup
- Liberty Collective Controller configured and running
- Cluster member servers joined to the collective
- Member servers have `liberty-cluster-member-app` deployed

### 2. Software Requirements
- Java 8 or higher
- Maven 3.6 or higher
- WebSphere Liberty 25.0.0.9 or higher

### 3. Required Liberty Features
The controller server must have these features enabled:
- `jaxrs-2.1`
- `jsonp-1.1`
- `cdi-2.0`
- `collectiveController-1.0`
- `restConnector-2.0`
- `ssl-1.0`

## 🔨 Build Instructions

### Step 1: Navigate to Project Directory
```bash
cd controller-mbean-app
```

### Step 2: Build the WAR File
```bash
mvn clean package
```

**Expected Output:**
```
[INFO] Building war: .../controller-mbean-app/target/controller-mbean-app.war
[INFO] BUILD SUCCESS
```

The WAR file will be created at: `target/controller-mbean-app.war`

### Step 3: Verify Build
```bash
ls -lh target/controller-mbean-app.war
```

## 🚀 Deployment Options

### Option 1: Using Liberty Maven Plugin (Recommended for Development)

This option automatically installs Liberty, configures the server, and deploys the application.

```bash
# Run the application
mvn liberty:run

# Or start in background
mvn liberty:start

# Stop the server
mvn liberty:stop
```

**Access the application:**
- Web UI: http://localhost:9080/controller-mbean-app/
- API: http://localhost:9080/controller-mbean-app/api/counters

### Option 2: Manual Deployment to Existing Controller

#### Step 2.1: Copy WAR to Controller
```bash
# Copy to dropins directory (auto-deploy)
cp target/controller-mbean-app.war \
   <LIBERTY_HOME>/usr/servers/controller/dropins/

# OR copy to apps directory (requires server.xml configuration)
cp target/controller-mbean-app.war \
   <LIBERTY_HOME>/usr/servers/controller/apps/
```

#### Step 2.2: Update server.xml (if using apps directory)
Add to `<LIBERTY_HOME>/usr/servers/controller/server.xml`:

```xml
<webApplication id="controller-mbean-app" 
                location="controller-mbean-app.war" 
                name="controller-mbean-app"
                contextRoot="/controller-mbean-app">
    <classloader delegation="parentLast" />
</webApplication>
```

#### Step 2.3: Start/Restart Controller
```bash
# Start the controller
<LIBERTY_HOME>/bin/server start controller

# Or restart if already running
<LIBERTY_HOME>/bin/server stop controller
<LIBERTY_HOME>/bin/server start controller
```

### Option 3: Using Admin Center (GUI)

1. Access Liberty Admin Center: https://localhost:9443/adminCenter
2. Login with admin credentials (admin/admin)
3. Navigate to **Server Config** → **Applications**
4. Click **Add** → **Upload Application**
5. Select `controller-mbean-app.war`
6. Set context root to `/controller-mbean-app`
7. Click **Deploy**

## ✅ Verification Steps

### Step 1: Check Server Logs
```bash
tail -f <LIBERTY_HOME>/usr/servers/controller/logs/messages.log
```

**Look for:**
```
[AUDIT   ] CWWKT0016I: Web application available (default_host): http://localhost:9080/controller-mbean-app/
[AUDIT   ] CWWKZ0001I: Application controller-mbean-app started
```

### Step 2: Test Web UI
```bash
# Open in browser
open http://localhost:9080/controller-mbean-app/

# Or use curl
curl http://localhost:9080/controller-mbean-app/
```

### Step 3: Test API Endpoints

#### Test 1: List Members
```bash
curl http://localhost:9080/controller-mbean-app/api/counters/members | jq
```

**Expected Response:**
```json
{
  "memberCount": 2,
  "members": [
    {
      "serverName": "member1",
      "hostName": "localhost",
      "state": "STARTED"
    },
    {
      "serverName": "member2",
      "hostName": "localhost",
      "state": "STARTED"
    }
  ],
  "timestamp": 1700000000000
}
```

#### Test 2: Query All Counters
```bash
curl http://localhost:9080/controller-mbean-app/api/counters | jq
```

#### Test 3: Query Specific Member
```bash
curl http://localhost:9080/controller-mbean-app/api/counters/member1 | jq
```

## 🔧 Configuration

### Server Configuration

The application includes a pre-configured `server.xml` in `src/main/liberty/config/server.xml`.

**Key Configuration Elements:**

```xml
<!-- HTTP Endpoints -->
<httpEndpoint id="defaultHttpEndpoint"
              httpPort="9080"
              httpsPort="9443"
              host="*" />

<!-- Application -->
<webApplication id="controller-mbean-app" 
                location="controller-mbean-app.war" 
                name="controller-mbean-app"
                contextRoot="/controller-mbean-app">
    <classloader delegation="parentLast" />
</webApplication>

<!-- Collective Controller -->
<collectiveController>
    <hostAuthInfo host="*" user="admin" password="admin" />
</collectiveController>

<!-- REST Connector -->
<restConnector>
    <quickStartSecurity userName="admin" userPassword="admin" />
</restConnector>
```

### Customizing Ports

To change the HTTP/HTTPS ports, update `pom.xml`:

```xml
<properties>
    <liberty.var.default.http.port>9080</liberty.var.default.http.port>
    <liberty.var.default.https.port>9443</liberty.var.default.https.port>
</properties>
```

### Customizing Context Root

To change the context root, update `server.xml`:

```xml
<webApplication id="controller-mbean-app" 
                location="controller-mbean-app.war" 
                contextRoot="/my-custom-path">
```

## 🔐 Security Configuration

### Default Credentials
- **Username**: admin
- **Password**: admin

⚠️ **WARNING**: Change these credentials for production deployments!

### Updating Credentials

#### Option 1: Update server.xml
```xml
<basicRegistry id="basic" realm="BasicRealm">
    <user name="myuser" password="mypassword" />
</basicRegistry>

<administrator-role>
    <user>myuser</user>
</administrator-role>
```

#### Option 2: Use LDAP
```xml
<ldapRegistry id="ldap" realm="LdapRealm"
              host="ldap.example.com"
              port="389"
              baseDN="ou=users,dc=example,dc=com">
</ldapRegistry>
```

### SSL/TLS Configuration

For production, configure proper SSL certificates:

```xml
<keyStore id="defaultKeyStore" 
          location="key.p12" 
          type="PKCS12" 
          password="{xor}Lz4sLCgwLTs=" />

<ssl id="defaultSSLConfig" 
     keyStoreRef="defaultKeyStore" 
     trustDefaultCerts="false" />
```

## 🧪 Complete Deployment Test

Run this script to verify the complete deployment:

```bash
#!/bin/bash

echo "=== Controller MBean App Deployment Test ==="
echo ""

# Configuration
CONTROLLER_URL="http://localhost:9080/controller-mbean-app"
MEMBER1_URL="http://localhost:9081/member-app"
MEMBER2_URL="http://localhost:9082/member-app"

# Test 1: Check if controller app is accessible
echo "1. Testing controller application..."
if curl -s -f -o /dev/null "$CONTROLLER_URL"; then
    echo "✓ Controller application is accessible"
else
    echo "✗ Controller application is NOT accessible"
    exit 1
fi

# Test 2: Check if member apps are accessible
echo "2. Testing member applications..."
if curl -s -f -o /dev/null "$MEMBER1_URL/api/counter"; then
    echo "✓ Member1 application is accessible"
else
    echo "✗ Member1 application is NOT accessible"
fi

if curl -s -f -o /dev/null "$MEMBER2_URL/api/counter"; then
    echo "✓ Member2 application is accessible"
else
    echo "✗ Member2 application is NOT accessible"
fi

# Test 3: List members
echo "3. Listing cluster members..."
MEMBERS=$(curl -s "$CONTROLLER_URL/api/counters/members")
MEMBER_COUNT=$(echo $MEMBERS | jq -r '.memberCount')
echo "Found $MEMBER_COUNT members"

# Test 4: Increment counters
echo "4. Incrementing counters on members..."
for i in {1..3}; do
    curl -s "$MEMBER1_URL/api/counter/increment" > /dev/null
    curl -s "$MEMBER2_URL/api/counter/increment" > /dev/null
done
echo "✓ Counters incremented"

# Test 5: Query counters from controller
echo "5. Querying counters from controller..."
COUNTERS=$(curl -s "$CONTROLLER_URL/api/counters")
SUCCESS_COUNT=$(echo $COUNTERS | jq -r '.successCount')
echo "Successfully queried $SUCCESS_COUNT members"

# Test 6: Display results
echo ""
echo "=== Counter Values ==="
echo $COUNTERS | jq '.members[] | {serverName, counter, status}'

echo ""
echo "=== Deployment Test Complete ==="
```

Save as `test-deployment.sh` and run:
```bash
chmod +x test-deployment.sh
./test-deployment.sh
```

## 🐛 Troubleshooting

### Issue 1: Application Not Starting

**Check logs:**
```bash
tail -100 <LIBERTY_HOME>/usr/servers/controller/logs/messages.log
```

**Common causes:**
- Missing required features in server.xml
- Port conflicts (9080/9443 already in use)
- Incorrect file permissions

**Solution:**
```bash
# Check if ports are in use
lsof -i :9080
lsof -i :9443

# Fix permissions
chmod -R 755 <LIBERTY_HOME>/usr/servers/controller
```

### Issue 2: MBean Not Found

**Symptom:** API returns `"status": "mbean_not_found"`

**Verify member app deployment:**
```bash
# Check if member app is deployed
curl http://localhost:9081/member-app/api/counter

# Check member server logs
tail -f <LIBERTY_HOME>/usr/servers/member1/logs/messages.log | grep MBean
```

**Solution:**
1. Ensure `liberty-cluster-member-app` is deployed on members
2. Verify MBean registration in member logs
3. Restart member servers if needed

### Issue 3: No Members Found

**Symptom:** API returns `"memberCount": 0`

**Verify collective status:**
```bash
<LIBERTY_HOME>/bin/collective list \
  --host=localhost --port=9443 \
  --user=admin --password=admin
```

**Solution:**
1. Ensure member servers are joined to collective
2. Verify collective controller is running
3. Check network connectivity

### Issue 4: Authentication Errors

**Symptom:** 401 Unauthorized errors

**Solution:**
1. Verify credentials in server.xml
2. Check REST connector configuration
3. Ensure admin user has administrator-role

## 📊 Monitoring

### Application Logs
```bash
# Real-time monitoring
tail -f <LIBERTY_HOME>/usr/servers/controller/logs/messages.log

# Filter for application logs
tail -f <LIBERTY_HOME>/usr/servers/controller/logs/messages.log | grep "com.example.controller"
```

### JMX Monitoring
```bash
# Connect with JConsole
jconsole localhost:9443

# Or use REST API
curl -k -u admin:admin \
  https://localhost:9443/IBMJMXConnectorREST/mbeans/WebSphere:*
```

## 🔄 Updating the Application

### Step 1: Build New Version
```bash
mvn clean package
```

### Step 2: Stop Application
```bash
<LIBERTY_HOME>/bin/server stop controller
```

### Step 3: Replace WAR File
```bash
cp target/controller-mbean-app.war \
   <LIBERTY_HOME>/usr/servers/controller/dropins/
```

### Step 4: Start Application
```bash
<LIBERTY_HOME>/bin/server start controller
```

## 📚 Additional Resources

- [Liberty Server Configuration](https://www.ibm.com/docs/en/was-liberty/base?topic=liberty-server-configuration-overview)
- [Liberty Collective Setup](https://www.ibm.com/docs/en/was-liberty/base?topic=liberty-setting-up-collective)
- [Liberty Maven Plugin](https://github.com/OpenLiberty/ci.maven)

## ✅ Deployment Checklist

- [ ] Liberty Collective Controller is configured
- [ ] Member servers are joined to collective
- [ ] Member application (`liberty-cluster-member-app`) is deployed
- [ ] Controller application is built (`mvn clean package`)
- [ ] WAR file is deployed to controller
- [ ] Server is started/restarted
- [ ] Application is accessible (Web UI test)
- [ ] API endpoints are working (curl tests)
- [ ] MBeans are queryable from controller
- [ ] Logs show no errors
- [ ] Security credentials are updated (for production)

## 🎉 Success Criteria

Your deployment is successful when:

1. ✅ Web UI is accessible at http://localhost:9080/controller-mbean-app/
2. ✅ API returns member list with correct count
3. ✅ Counter values can be queried from all members
4. ✅ No errors in server logs
5. ✅ All test endpoints return valid JSON responses

---

**Need Help?** Check the main [README.md](README.md) or refer to IBM WebSphere Liberty documentation.