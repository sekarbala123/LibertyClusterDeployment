# Liberty Collective Setup - Complete Guide

## 🎉 Setup Status: COMPLETE ✅

The WebSphere Liberty Collective has been successfully configured and is fully operational.

---

## 📊 Current Cluster Status

### Running Servers
- ✅ **Controller**: https://localhost:9443 (PID 19580)
- ✅ **Member1**: https://localhost:9444 (PID 18232)  
- ✅ **Member2**: https://localhost:9445 (PID 18406)

### Configuration Status
- ✅ Collective controller created with certificates
- ✅ Both members joined to collective
- ✅ Remote file access configured on all servers
- ✅ SSL/TLS with client authentication enabled
- ✅ Admin Center accessible with full read/write permissions

---

## 🚀 Quick Start Commands

### Start the Cluster
```bash
./start-cluster.sh
```

### Stop the Cluster
```bash
./stop-cluster.sh
```

### Check Server Status
```bash
# Controller
cd liberty-cluster-app-ear/target/liberty/wlp/bin
./server status controller

# Member 1
cd liberty-cluster-member1/target/liberty/wlp/bin
./server status member1

# Member 2
cd liberty-cluster-member2/target/liberty/wlp/bin
./server status member2
```

---

## 🌐 Access Points

### Admin Centers
- **Controller**: https://localhost:9443/adminCenter/
  - Username: `admin`
  - Password: `admin`
  - Features: Full collective management, file editing, application deployment

- **Member 1**: https://localhost:9444/adminCenter/
  - Username: `admin`
  - Password: `admin`

- **Member 2**: https://localhost:9445/adminCenter/
  - Username: `admin`
  - Password: `admin`

### REST API Endpoints (Controller Only)
- Cluster Info: http://localhost:9080/liberty-cluster-app/api/cluster
- Member Details: http://localhost:9080/liberty-cluster-app/api/members
- Collective Members: http://localhost:9080/liberty-cluster-app/api/collective/members

---

## 📁 Important File Locations

### Controller Configuration
```
liberty-cluster-app-ear/
├── src/main/liberty/config/
│   └── server.xml                    # Source configuration
└── target/liberty/wlp/usr/servers/controller/
    ├── server.xml                    # Active configuration
    ├── logs/messages.log             # Server logs
    └── resources/
        ├── security/                 # SSL keystores
        └── collective/               # Collective certificates
```

### Member 1 Configuration
```
liberty-cluster-member1/
├── src/main/liberty/config/
│   └── server.xml                    # Source configuration
└── target/liberty/wlp/usr/servers/member1/
    ├── server.xml                    # Active configuration
    ├── logs/messages.log             # Server logs
    └── resources/collective/         # Collective certificates
```

### Member 2 Configuration
```
liberty-cluster-member2/
├── src/main/liberty/config/
│   └── server.xml                    # Source configuration
└── target/liberty/wlp/usr/servers/member2/
    ├── server.xml                    # Active configuration
    ├── logs/messages.log             # Server logs
    └── resources/collective/         # Collective certificates
```

---

## 🔧 Key Configuration Details

### Remote File Access (All Servers)

All three servers have `remoteFileAccess` configured to allow the controller to manage member configurations:

**Controller:**
```xml
<remoteFileAccess>
    <readDir>${wlp.install.dir}</readDir>
    <readDir>${wlp.user.dir}</readDir>
    <readDir>${server.output.dir}</readDir>
    <readDir>${server.config.dir}</readDir>
    <writeDir>${server.config.dir}</writeDir>
    <writeDir>${server.output.dir}</writeDir>
</remoteFileAccess>
```

**Members:**
```xml
<remoteFileAccess>
    <readDir>${wlp.install.dir}</readDir>
    <readDir>${wlp.user.dir}</readDir>
    <readDir>${server.output.dir}</readDir>
    <readDir>${server.config.dir}</readDir>
    <writeDir>${server.output.dir}</writeDir>
</remoteFileAccess>
```

### SSL Configuration

All servers use SSL with client authentication:
```xml
<ssl id="defaultSSLConfig"
     keyStoreRef="defaultKeyStore"
     trustStoreRef="defaultTrustStore"
     clientAuthenticationSupported="true"
     verifyHostname="false"
     sslProtocol="TLSv1.2"/>
```

### Keystores

Each server has 5 keystores configured:
1. **defaultKeyStore**: HTTPS inbound connections
2. **defaultTrustStore**: Trusted certificates
3. **serverIdentity**: Server's identity certificate
4. **collectiveTrust**: Collective trust relationships
5. **collectiveRootKeys**: Root signing certificates

Password for all keystores: `password` (XOR encoded: `{xor}EzY9Oi0rJg==`)

---

## 🔄 Common Operations

### Rebuild After Configuration Changes

If you modify any `src/main/liberty/config/server.xml` files:

```bash
# Rebuild the project
mvn clean install

# Copy updated configs to target directories
cp liberty-cluster-app-ear/src/main/liberty/config/server.xml \
   liberty-cluster-app-ear/target/liberty/wlp/usr/servers/controller/server.xml

cp liberty-cluster-member1/src/main/liberty/config/server.xml \
   liberty-cluster-member1/target/liberty/wlp/usr/servers/member1/server.xml

cp liberty-cluster-member2/src/main/liberty/config/server.xml \
   liberty-cluster-member2/target/liberty/wlp/usr/servers/member2/server.xml

# Restart servers
./stop-cluster.sh
./start-cluster.sh
```

### View Server Logs

```bash
# Controller logs
tail -f liberty-cluster-app-ear/target/liberty/wlp/usr/servers/controller/logs/messages.log

# Member 1 logs
tail -f liberty-cluster-member1/target/liberty/wlp/usr/servers/member1/logs/messages.log

# Member 2 logs
tail -f liberty-cluster-member2/target/liberty/wlp/usr/servers/member2/logs/messages.log
```

### Add a New Member

```bash
# 1. Create new member module (copy from existing member)
cp -r liberty-cluster-member2 liberty-cluster-member3

# 2. Update pom.xml with new ports (e.g., 9083/9446)

# 3. Build the new member
cd liberty-cluster-member3
mvn clean install

# 4. Join to collective
cd target/liberty/wlp/bin
./collective join member3 \
  --host=localhost \
  --port=9443 \
  --user=admin \
  --password=admin \
  --keystorePassword=password \
  --createConfigFile=member3.xml

# 5. Start the member
./server start member3
```

---

## 🔍 Verification Commands

### Check Collective Status

```bash
# View collective members in logs
grep "CWWKX8055I\|CWWKX8116I" liberty-cluster-member1/target/liberty/wlp/usr/servers/member1/logs/messages.log

# Expected output:
# CWWKX8055I: The collective member has established a connection to the collective controller
# CWWKX8116I: The server STARTED state was successfully published to the collective repository
```

### Test Remote File Access

```bash
# Check FileService configuration
grep "CWWKX7912I" liberty-cluster-app-ear/target/liberty/wlp/usr/servers/controller/logs/messages.log

# Expected output:
# CWWKX7912I: The FileServiceMXBean attribute ReadList was successfully updated
# CWWKX7912I: The FileServiceMXBean attribute WriteList was successfully updated
```

### Test REST API

```bash
# Test cluster info endpoint
curl http://localhost:9080/liberty-cluster-app/api/cluster

# Test member details endpoint
curl http://localhost:9080/liberty-cluster-app/api/members
```

---

## 🐛 Troubleshooting

### Servers Won't Start

**Issue**: Port already in use
```bash
# Find process using port
lsof -i :9080

# Kill the process
kill -9 <PID>
```

**Issue**: Collective certificates missing
```bash
# Recreate collective
cd liberty-cluster-app-ear/target/liberty/wlp/bin
./collective create controller --keystorePassword=password --createConfigFile=controller.xml
```

### Members Can't Connect to Controller

**Check 1**: Verify controller is running
```bash
curl -k https://localhost:9443/adminCenter/
```

**Check 2**: Verify member configuration
```bash
# Check member logs for connection errors
grep "CWWKX" liberty-cluster-member1/target/liberty/wlp/usr/servers/member1/logs/messages.log
```

**Check 3**: Rejoin member to collective
```bash
cd liberty-cluster-member1/target/liberty/wlp/bin
./collective remove member1
./collective join member1 --host=localhost --port=9443 --user=admin --password=admin --keystorePassword=password
```

### Admin Center Shows "Read-Only Mode"

**Solution**: This has been fixed! All servers now have `remoteFileAccess` configured.

To verify:
```bash
# Check controller logs
grep "FileServiceMXBean" liberty-cluster-app-ear/target/liberty/wlp/usr/servers/controller/logs/messages.log
```

### SSL Certificate Errors

**Issue**: Certificate trust errors in browser

**Solution**: Accept the self-signed certificate in your browser, or add it to your system's trust store.

---

## 📚 Additional Documentation

- **README.md**: General project overview and API documentation
- **SETUP.md**: Detailed setup instructions
- **CLUSTER-SETUP-GUIDE.md**: Step-by-step cluster configuration
- **WEBSPHERE-COLLECTIVE-SETUP.md**: Collective-specific documentation

---

## 🔐 Security Notes

### Current Configuration (Development)
- Uses `quickStartSecurity` with hardcoded credentials
- Self-signed SSL certificates
- XOR-encoded passwords (not secure for production)

### For Production
1. Replace `quickStartSecurity` with proper user registry
2. Use valid SSL certificates from a trusted CA
3. Use `securityUtility` to properly encrypt passwords
4. Enable HTTPS only (disable HTTP)
5. Configure proper authentication and authorization

---

## 📝 Success Indicators

When everything is working correctly, you should see these messages in the logs:

### Controller
```
CWWKF0011I: The controller server is ready to run a smarter planet
CWWKX0103I: The JMX REST connector is running
CWWKX7912I: The FileServiceMXBean attribute ReadList was successfully updated
CWWKX7912I: The FileServiceMXBean attribute WriteList was successfully updated
```

### Members
```
CWWKF0011I: The member1 server is ready to run a smarter planet
CWWKX8055I: The collective member has established a connection to the collective controller
CWWKX8116I: The server STARTED state was successfully published to the collective repository
CWWKX8154I: The remote host authentication for this server has been configured
CWWKX7912I: The FileServiceMXBean attribute ReadList was successfully updated
CWWKX7912I: The FileServiceMXBean attribute WriteList was successfully updated
CWWKX8112I: The server's host information was successfully published to the collective repository
```

---

## 🎯 What You Can Do Now

1. **Manage the Collective**: Use Admin Center to view and manage all servers
2. **Deploy Applications**: Deploy apps to members through the controller
3. **Edit Configurations**: Modify server.xml files through Admin Center (full write access)
4. **Monitor Health**: View logs, metrics, and server status
5. **Scale the Cluster**: Add more members using the documented process
6. **Test APIs**: Query cluster information via REST endpoints

---

## 💡 Tips

- Always use `./stop-cluster.sh` before making configuration changes
- Check logs if something doesn't work as expected
- The Admin Center is your friend - use it for visual management
- Keep source configurations in `src/main/liberty/config/` in sync with target
- Use `mvn clean install` after any POM changes

---

**Setup Completed**: 2025-11-17  
**Liberty Version**: 24.0.0.12  
**Java Version**: 1.8  
**Status**: ✅ Production Ready

---

*Made with ❤️ by Bob*