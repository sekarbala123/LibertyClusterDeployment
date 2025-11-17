# Liberty Cluster Deployment - Quick Reference Card

## 🚀 Quick Start (3 Commands)

```bash
mvn clean install                          # Build
cd liberty-cluster-app-ear && mvn liberty:dev  # Run
curl http://localhost:9080/liberty-cluster-app/api/cluster  # Test
```

## 📍 Access Points

| Service | URL | Credentials |
|---------|-----|-------------|
| REST API | http://localhost:9080/liberty-cluster-app/api/cluster | None |
| Admin Center | https://localhost:9443/adminCenter/ | admin / adminpwd |

## 🔧 Common Maven Commands

```bash
# Build
mvn clean install              # Full build
mvn clean install -DskipTests  # Skip tests
mvn clean package              # Package only

# Run
mvn liberty:dev                # Development mode (hot reload)
mvn liberty:run                # Production mode
mvn liberty:debug              # Debug mode (port 7777)
mvn liberty:stop               # Stop server

# Test
mvn test                       # Run tests
mvn verify                     # Run integration tests
```

## 📂 Project Structure

```
LibertyClusterDeployment/
├── pom.xml                    # Parent POM
├── README.md                  # Full documentation
├── SETUP.md                   # Setup guide
├── QUICK-REFERENCE.md         # This file
├── liberty-cluster-app-war/   # WAR module
│   └── src/main/java/com/example/liberty/cluster/
│       ├── ClusterInfoResource.java      # REST endpoint
│       └── JAXRSConfiguration.java       # JAX-RS config
└── liberty-cluster-app-ear/   # EAR module
    └── src/main/liberty/config/
        └── server.xml         # Liberty configuration
```

## 🌐 API Endpoints

### GET /api/cluster
Returns cluster information

```bash
# curl
curl http://localhost:9080/liberty-cluster-app/api/cluster

# PowerShell
Invoke-RestMethod -Uri http://localhost:9080/liberty-cluster-app/api/cluster

# Browser
http://localhost:9080/liberty-cluster-app/api/cluster
```

**Response:** `[]` (empty array if no clusters defined)

## 🔍 Troubleshooting

### Server won't start
```bash
# Check if port is in use
lsof -i :9080        # macOS/Linux
netstat -ano | findstr :9080  # Windows

# View logs
tail -f liberty-cluster-app-ear/target/liberty/wlp/usr/servers/controller/logs/messages.log
```

### Build fails
```bash
# Clean and rebuild
mvn clean install -U

# Clear Maven cache
rm -rf ~/.m2/repository
```

### Admin Center not accessible
1. Verify server is running: `mvn liberty:status`
2. Check credentials: `admin` / `adminpwd`
3. Accept self-signed certificate in browser

## 📝 Configuration Files

### server.xml
Location: `liberty-cluster-app-ear/src/main/liberty/config/server.xml`

Key settings:
- HTTP Port: 9080
- HTTPS Port: 9443
- Admin User: admin
- Admin Password: adminpwd

### pom.xml (EAR)
Location: `liberty-cluster-app-ear/pom.xml`

Key settings:
- Liberty Version: 22.0.0.1
- Server Name: controller
- License: Auto-accepted

## 🔐 Security

### Development (Current)
- Username: `admin`
- Password: `adminpwd`
- SSL: Self-signed certificate

### Production (Recommended)
- Use proper user registry
- Valid SSL certificates
- Environment variables for secrets
- HTTPS only

## 📊 Server Logs

```bash
# Messages log
tail -f target/liberty/wlp/usr/servers/controller/logs/messages.log

# Console log
tail -f target/liberty/wlp/usr/servers/controller/logs/console.log

# Trace log
tail -f target/liberty/wlp/usr/servers/controller/logs/trace.log

# Search for errors
grep ERROR target/liberty/wlp/usr/servers/controller/logs/messages.log
```

## 🎯 Development Workflow

1. **Make code changes** in `liberty-cluster-app-war/src/main/java/`
2. **Save file** - Liberty dev mode auto-detects changes
3. **Wait for reload** - Usually 2-5 seconds
4. **Test changes** - Use curl or browser
5. **Check logs** - Verify no errors

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=ClusterInfoResourceTest

# Skip tests
mvn clean install -DskipTests
```

## 📦 Packaging

```bash
# Create EAR file
mvn clean package

# Location
liberty-cluster-app-ear/target/liberty-cluster-app-ear-1.0-SNAPSHOT.ear

# Create server package
mvn liberty:package
```

## 🔄 Hot Reload (Dev Mode)

In `mvn liberty:dev` mode:
- **Enter** - Run tests
- **r** - Restart server
- **Ctrl+C** - Stop server

Changes auto-reload:
- ✅ Java files
- ✅ Resources
- ✅ Configuration files

## 🐛 Debug Mode

```bash
# Start in debug mode
cd liberty-cluster-app-ear
mvn liberty:debug

# Connect debugger to port 7777
```

**IDE Setup:**
- IntelliJ: Run → Edit Configurations → Remote JVM Debug → Port 7777
- Eclipse: Debug → Debug Configurations → Remote Java Application → Port 7777
- VS Code: Add launch.json configuration for port 7777

## 📈 Performance

### Resource Usage
- Memory: 512MB min, 2GB recommended
- Disk: ~500MB
- CPU: 1-2% idle

### Startup Time
- Cold start: 15-30 seconds
- Hot reload: 2-5 seconds

## 🔗 Useful Links

- [Full Documentation](README.md)
- [Setup Guide](SETUP.md)
- [Project Status](TODO.md)
- [IBM Liberty Docs](https://www.ibm.com/docs/en/was-liberty)

## 💡 Tips & Tricks

### Faster Builds
```bash
# Skip tests and documentation
mvn clean install -DskipTests -Dmaven.javadoc.skip=true
```

### Clean Everything
```bash
# Remove all build artifacts
mvn clean
rm -rf target/
```

### Check Server Status
```bash
cd liberty-cluster-app-ear/target/liberty/wlp/bin
./server status controller
```

### View Server Info
```bash
cd liberty-cluster-app-ear/target/liberty/wlp/bin
./server version
./server help
```

## 🎓 Learning Path

1. ✅ Follow [SETUP.md](SETUP.md) - Get it running
2. ✅ Read [README.md](README.md) - Understand the architecture
3. ✅ Test the API - Use curl examples
4. ✅ Explore Admin Center - Navigate the UI
5. ✅ Modify code - Try hot reload
6. ✅ Add features - Extend the application

## ⚡ Keyboard Shortcuts (Dev Mode)

| Key | Action |
|-----|--------|
| Enter | Run tests |
| r | Restart server |
| Ctrl+C | Stop server |

## 🎯 Common Tasks

### Change HTTP Port
Edit `server.xml`:
```xml
<httpEndpoint httpPort="9090" httpsPort="9453" />
```

### Change Admin Password
Edit `server.xml`:
```xml
<quickStartSecurity userName="admin" userPassword="newpassword" />
```

### Add JVM Options
Create `jvm.options`:
```
-Xms512m
-Xmx2048m
```

### Enable Debug Logging
Edit `server.xml`:
```xml
<logging traceSpecification="*=info:com.example.*=all" />
```

## 📞 Getting Help

1. Check [README.md Troubleshooting](README.md#troubleshooting)
2. Review server logs
3. Consult IBM Liberty documentation
4. Check [TODO.md](TODO.md) for known issues

---

**Quick Reference Version:** 1.0  
**Last Updated:** 2025-11-12  
**For detailed information, see [README.md](README.md)**