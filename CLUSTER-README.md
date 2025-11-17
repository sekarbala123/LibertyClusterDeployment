# Liberty Cluster - Quick Start Guide

## 🎯 What You Have

A complete **3-server Open Liberty cluster** ready to run:

- **Controller** (Port 9080/9443)
- **Member 1** (Port 9081/9444)
- **Member 2** (Port 9082/9445)

---

## 🚀 Quick Start (3 Steps)

### Step 1: Build the Project

```bash
mvn clean install
```

### Step 2: Start the Cluster

**macOS/Linux:**
```bash
./start-cluster.sh
```

**Windows:**
```cmd
start-cluster.bat
```

**Or manually in 3 separate terminals:**
```bash
# Terminal 1
cd liberty-cluster-app-ear && mvn liberty:dev

# Terminal 2
cd liberty-cluster-member1 && mvn liberty:dev

# Terminal 3
cd liberty-cluster-member2 && mvn liberty:dev
```

### Step 3: Test the Cluster

```bash
./test-cluster.sh
```

Or test manually:
```bash
curl http://localhost:9080/liberty-cluster-app/api/cluster
curl http://localhost:9081/liberty-cluster-app/api/cluster
curl http://localhost:9082/liberty-cluster-app/api/cluster
```

---

## 📍 Access Points

### REST API Endpoints

| Server | URL |
|--------|-----|
| Controller | http://localhost:9080/liberty-cluster-app/api/cluster |
| Member 1 | http://localhost:9081/liberty-cluster-app/api/cluster |
| Member 2 | http://localhost:9082/liberty-cluster-app/api/cluster |

### Admin Centers

| Server | URL | Credentials |
|--------|-----|-------------|
| Controller | https://localhost:9443/adminCenter/ | admin / adminpwd |
| Member 1 | https://localhost:9444/adminCenter/ | admin / adminpwd |
| Member 2 | https://localhost:9445/adminCenter/ | admin / adminpwd |

---

## 📂 Project Structure

```
LibertyClusterDeployment/
├── liberty-cluster-app-war/          # WAR module (application code)
├── liberty-cluster-app-ear/          # EAR module (controller server)
├── liberty-cluster-member1/          # Cluster member 1
├── liberty-cluster-member2/          # Cluster member 2
├── start-cluster.sh                  # Startup script (macOS/Linux)
├── start-cluster.bat                 # Startup script (Windows)
├── test-cluster.sh                   # Test script
└── CLUSTER-SETUP-GUIDE.md           # Detailed setup guide
```

---

## 🔧 Configuration

### Server Ports

| Server | HTTP | HTTPS | Admin Center |
|--------|------|-------|--------------|
| Controller | 9080 | 9443 | https://localhost:9443/adminCenter/ |
| Member 1 | 9081 | 9444 | https://localhost:9444/adminCenter/ |
| Member 2 | 9082 | 9445 | https://localhost:9445/adminCenter/ |

### Changing Ports

Edit the respective `server.xml` file:

**Controller:** `liberty-cluster-app-ear/src/main/liberty/config/server.xml`
**Member 1:** `liberty-cluster-member1/src/main/liberty/config/server.xml`
**Member 2:** `liberty-cluster-member2/src/main/liberty/config/server.xml`

```xml
<httpEndpoint id="defaultHttpEndpoint"
              host="*"
              httpPort="YOUR_HTTP_PORT"
              httpsPort="YOUR_HTTPS_PORT" />
```

---

## 🧪 Testing

### Test All Servers

```bash
./test-cluster.sh
```

### Test Individual Servers

```bash
# Controller
curl http://localhost:9080/liberty-cluster-app/api/cluster

# Member 1
curl http://localhost:9081/liberty-cluster-app/api/cluster

# Member 2
curl http://localhost:9082/liberty-cluster-app/api/cluster
```

### Expected Response

Each server should return JSON with server information:

```json
{
  "serverName": "Open Liberty Server",
  "javaVersion": "24.0.1",
  "osName": "Mac OS X",
  "availableProcessors": 8,
  "freeMemory": 123456789,
  "totalMemory": 234567890,
  "maxMemory": 345678901,
  "mbeanCount": 42,
  "message": "Open Liberty server running successfully"
}
```

---

## 🛑 Stopping the Cluster

### If Started with Scripts

Press `Ctrl+C` in each terminal window

### If Started Manually

```bash
# In each terminal where mvn liberty:dev is running
Press Ctrl+C

# Or use Maven command
cd liberty-cluster-app-ear && mvn liberty:stop
cd liberty-cluster-member1 && mvn liberty:stop
cd liberty-cluster-member2 && mvn liberty:stop
```

---

## 🔍 Troubleshooting

### Issue: Port Already in Use

```bash
# Find process using port
lsof -i :9080

# Kill process
kill -9 <PID>
```

### Issue: Server Won't Start

1. Check if port is available
2. Review logs: `target/liberty/wlp/usr/servers/*/logs/messages.log`
3. Clean and rebuild: `mvn clean install`

### Issue: Application Not Deployed

1. Verify EAR file exists in `target/` directory
2. Check server logs for deployment errors
3. Ensure `mvn clean install` completed successfully

### View Server Logs

```bash
# Controller
tail -f liberty-cluster-app-ear/target/liberty/wlp/usr/servers/controller/logs/messages.log

# Member 1
tail -f liberty-cluster-member1/target/liberty/wlp/usr/servers/member1/logs/messages.log

# Member 2
tail -f liberty-cluster-member2/target/liberty/wlp/usr/servers/member2/logs/messages.log
```

---

## 📊 Monitoring

### Check Server Status

```bash
# Using curl
curl -s http://localhost:9080/liberty-cluster-app/api/cluster | jq

# Check all servers
for port in 9080 9081 9082; do
    echo "Server on port $port:"
    curl -s http://localhost:$port/liberty-cluster-app/api/cluster | jq .message
    echo ""
done
```

### Admin Center Monitoring

1. Access Admin Center for each server
2. Navigate to "Explore" → "Servers"
3. View metrics, logs, and configuration

---

## 🔐 Security

### Default Credentials

- **Username:** `admin`
- **Password:** `adminpwd`

### Changing Credentials

Edit `server.xml` for each server:

```xml
<quickStartSecurity userName="newuser" userPassword="newpassword" />
```

### Production Security

For production deployment:
1. Use proper user registry (LDAP, Active Directory)
2. Configure valid SSL certificates
3. Enable HTTPS only
4. Use environment variables for secrets
5. Enable security auditing

See [CLUSTER-SETUP-GUIDE.md](CLUSTER-SETUP-GUIDE.md) for detailed security configuration.

---

## 📚 Additional Documentation

- **[CLUSTER-SETUP-GUIDE.md](CLUSTER-SETUP-GUIDE.md)** - Comprehensive setup guide with advanced configurations
- **[README.md](README.md)** - Main project documentation
- **[SETUP.md](SETUP.md)** - First-time setup instructions
- **[QUICK-REFERENCE.md](QUICK-REFERENCE.md)** - Command reference

---

## 💡 Tips

### Development Mode Features

When running with `mvn liberty:dev`:
- **Press Enter** - Run tests
- **Type 'r' + Enter** - Restart server
- **Ctrl+C** - Stop server
- **Hot Reload** - Code changes auto-reload

### Adding More Members

To add more cluster members:

1. Copy `liberty-cluster-member2` directory
2. Rename to `liberty-cluster-member3`
3. Update ports in `server.xml` (e.g., 9083/9446)
4. Update `cloneId` in `server.xml`
5. Add module to parent `pom.xml`
6. Build and run

---

## 🎯 Next Steps

1. ✅ **Test the cluster** - Run `./test-cluster.sh`
2. ✅ **Explore Admin Center** - Login and navigate the UI
3. ✅ **Try hot reload** - Make code changes and see them reload
4. ✅ **Add load balancer** - See [CLUSTER-SETUP-GUIDE.md](CLUSTER-SETUP-GUIDE.md)
5. ✅ **Configure session replication** - For stateful applications

---

## 🆘 Getting Help

1. Check server logs
2. Review [CLUSTER-SETUP-GUIDE.md](CLUSTER-SETUP-GUIDE.md)
3. See [Troubleshooting](#troubleshooting) section above
4. Check [Open Liberty Documentation](https://openliberty.io/docs/)

---

**🎉 Your Liberty cluster is ready to use!**

Start it with `./start-cluster.sh` and test with `./test-cluster.sh`