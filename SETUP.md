# Liberty Cluster Deployment - Setup Guide

This guide provides step-by-step instructions for setting up and running the Liberty Cluster Deployment project for the first time.

## 📋 Prerequisites

Before you begin, ensure you have the following installed:

### Required Software

1. **Java Development Kit (JDK) 17 or later**
   - Download from: https://adoptium.net/ or https://www.oracle.com/java/technologies/downloads/
   - Verify installation:
     ```bash
     java -version
     # Should show version 17 or higher
     ```

2. **Apache Maven 3.6 or later**
   - Download from: https://maven.apache.org/download.cgi
   - Verify installation:
     ```bash
     mvn -version
     # Should show Maven 3.6 or higher
     ```

3. **Git** (for cloning the repository)
   - Download from: https://git-scm.com/downloads
   - Verify installation:
     ```bash
     git --version
     ```

### System Requirements

- **RAM**: Minimum 4GB, Recommended 8GB or more
- **Disk Space**: At least 2GB free space
- **Operating System**: Windows 10/11, macOS 10.14+, or Linux (Ubuntu 18.04+, RHEL 7+, etc.)
- **Network**: Internet connection required for first-time Maven dependency download

## 🚀 Step-by-Step Setup

### Step 1: Clone the Repository

```bash
# Clone the repository
git clone <repository-url>

# Navigate to the project directory
cd LibertyClusterDeployment

# Verify project structure
ls -la
```

**Expected Output:**
```
.gitattributes
.gitignore
pom.xml
README.md
SETUP.md
TODO.md
user-requirements.txt
liberty-cluster-app-ear/
liberty-cluster-app-war/
```

### Step 2: Configure Environment Variables (Optional)

For better control, you can set environment variables:

**Linux/macOS:**
```bash
export JAVA_HOME=/path/to/jdk-17
export MAVEN_OPTS="-Xmx1024m"
```

**Windows (Command Prompt):**
```cmd
set JAVA_HOME=C:\path\to\jdk-17
set MAVEN_OPTS=-Xmx1024m
```

**Windows (PowerShell):**
```powershell
$env:JAVA_HOME="C:\path\to\jdk-17"
$env:MAVEN_OPTS="-Xmx1024m"
```

### Step 3: Build the Project

This step will compile the code, run tests, and package the application.

```bash
# Clean and build the entire project
mvn clean install
```

**What happens during the build:**

1. **Dependency Resolution** (first time only, ~5-10 minutes)
   - Downloads Maven plugins
   - Downloads Jakarta EE 8 API dependencies
   - Downloads IBM WebSphere Liberty runtime (~200MB)

2. **Compilation**
   - Compiles Java source files
   - Validates JAX-RS resources

3. **Packaging**
   - Creates WAR file: `liberty-cluster-app-war/target/liberty-cluster-app-war-1.0-SNAPSHOT.war`
   - Creates EAR file: `liberty-cluster-app-ear/target/liberty-cluster-app-ear-1.0-SNAPSHOT.ear`

4. **Liberty Server Setup**
   - Extracts Liberty runtime to `liberty-cluster-app-ear/target/liberty/`
   - Creates server configuration in `target/liberty/wlp/usr/servers/controller/`

**Expected Output (Success):**
```
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for liberty-cluster-app-parent 1.0-SNAPSHOT:
[INFO] 
[INFO] liberty-cluster-app-parent ......................... SUCCESS [  0.123 s]
[INFO] liberty-cluster-app-war ............................ SUCCESS [  2.456 s]
[INFO] liberty-cluster-app-ear ............................ SUCCESS [  5.789 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### Step 4: Start the Liberty Server

Navigate to the EAR module and start the server in development mode:

```bash
cd liberty-cluster-app-ear
mvn liberty:dev
```

**What happens during startup:**

1. **Server Initialization**
   - Creates server instance named "controller"
   - Loads server configuration from `src/main/liberty/config/server.xml`
   - Initializes features: javaee-8.0, collectiveController-1.0, adminCenter-1.0

2. **Application Deployment**
   - Deploys the EAR file
   - Starts the WAR module
   - Initializes JAX-RS application

3. **Ready State**
   - HTTP endpoint available on port 9080
   - HTTPS endpoint available on port 9443
   - Admin Center accessible

**Expected Output (Server Started):**
```
[INFO] CWWKF0011I: The controller server is ready to run a smarter planet.
[INFO] CWWKT0016I: Web application available (default_host): http://localhost:9080/liberty-cluster-app/
[INFO] CWWKZ0001I: Application liberty-cluster-app started in X.XXX seconds.
```

**Development Mode Features:**
- **Hot Reload**: Changes to Java files are automatically detected and reloaded
- **Interactive Console**: 
  - Press `Enter` to run tests
  - Press `r` to restart the server
  - Press `Ctrl+C` to stop the server

### Step 5: Verify the Installation

#### 5.1 Test the REST API

Open a new terminal (keep the server running) and test the API:

**Using curl (Linux/macOS/Windows with Git Bash):**
```bash
curl http://localhost:9080/liberty-cluster-app/api/cluster
```

**Using PowerShell (Windows):**
```powershell
Invoke-RestMethod -Uri http://localhost:9080/liberty-cluster-app/api/cluster
```

**Using a Web Browser:**
Navigate to: http://localhost:9080/liberty-cluster-app/api/cluster

**Expected Response:**
```json
[]
```
*Note: Empty array is correct - no clusters are defined yet.*

#### 5.2 Access the Admin Center

1. Open your web browser
2. Navigate to: https://localhost:9443/adminCenter/
3. **Accept the security warning** (self-signed certificate)
   - Chrome: Click "Advanced" → "Proceed to localhost (unsafe)"
   - Firefox: Click "Advanced" → "Accept the Risk and Continue"
   - Safari: Click "Show Details" → "visit this website"
4. Login with credentials:
   - **Username**: `admin`
   - **Password**: `adminpwd`

**What you should see:**
- Dashboard with server status
- List of deployed applications
- Server configuration options
- Collective management tools

#### 5.3 Check Server Logs

View the server logs to ensure everything is running correctly:

```bash
# View messages log
cat liberty-cluster-app-ear/target/liberty/wlp/usr/servers/controller/logs/messages.log

# Tail the log (follow new entries)
tail -f liberty-cluster-app-ear/target/liberty/wlp/usr/servers/controller/logs/messages.log
```

**Look for these success messages:**
- `CWWKF0011I: The controller server is ready to run a smarter planet.`
- `CWWKZ0001I: Application liberty-cluster-app started`
- `CWWKT0016I: Web application available`

### Step 6: Stop the Server

To stop the server running in development mode:

1. Go to the terminal where `mvn liberty:dev` is running
2. Press `Ctrl+C`
3. Wait for graceful shutdown

**Alternative (if server is running in background):**
```bash
cd liberty-cluster-app-ear
mvn liberty:stop
```

## 🔧 Configuration

### Customizing Server Ports

If ports 9080 or 9443 are already in use, modify `server.xml`:

```xml
<httpEndpoint id="defaultHttpEndpoint"
              host="*"
              httpPort="9090"
              httpsPort="9453" />
```

### Changing Admin Credentials

Modify `server.xml`:

```xml
<quickStartSecurity userName="myadmin" userPassword="mypassword" />
```

### Adjusting JVM Memory

Create or edit `liberty-cluster-app-ear/src/main/liberty/config/jvm.options`:

```
-Xms512m
-Xmx2048m
```

## 🧪 Testing the Setup

### Run Unit Tests

```bash
# Run tests for all modules
mvn test

# Run tests for specific module
cd liberty-cluster-app-war
mvn test
```

### Manual Testing Checklist

- [ ] Server starts without errors
- [ ] REST API responds at http://localhost:9080/liberty-cluster-app/api/cluster
- [ ] Admin Center accessible at https://localhost:9443/adminCenter/
- [ ] Can login to Admin Center with admin/adminpwd
- [ ] Application appears in Admin Center's application list
- [ ] Server logs show no ERROR or SEVERE messages

## 🐛 Common Setup Issues

### Issue 1: Maven Build Fails - "Cannot resolve dependencies"

**Cause:** Network issues or Maven repository problems

**Solution:**
```bash
# Clear Maven cache
rm -rf ~/.m2/repository

# Retry build with force update
mvn clean install -U
```

### Issue 2: Port Already in Use

**Error:** `Address already in use: bind`

**Solution:**
```bash
# Find process using port 9080 (Linux/macOS)
lsof -i :9080

# Find process using port 9080 (Windows)
netstat -ano | findstr :9080

# Kill the process or change the port in server.xml
```

### Issue 3: Java Version Mismatch

**Error:** `Unsupported class file major version`

**Solution:**
```bash
# Check Java version
java -version

# Ensure JAVA_HOME points to JDK 17+
echo $JAVA_HOME  # Linux/macOS
echo %JAVA_HOME%  # Windows

# Update JAVA_HOME if needed
export JAVA_HOME=/path/to/jdk-17  # Linux/macOS
set JAVA_HOME=C:\path\to\jdk-17   # Windows
```

### Issue 4: Liberty Download Fails

**Error:** `Failed to download WebSphere Liberty`

**Solution:**
1. Check internet connection
2. Verify Maven can access IBM repositories
3. Try manual download and local installation:
   ```bash
   mvn dependency:get \
     -Dartifact=com.ibm.websphere.appserver.runtime:wlp-javaee8:22.0.0.1:zip
   ```

### Issue 5: Admin Center Shows "Not Authorized"

**Cause:** Incorrect credentials or security configuration

**Solution:**
1. Verify credentials in `server.xml`: `admin` / `adminpwd`
2. Clear browser cache and cookies
3. Try incognito/private browsing mode
4. Check server logs for authentication errors

## 📚 Next Steps

After successful setup:

1. **Explore the Admin Center**
   - Navigate through different sections
   - View server metrics and logs
   - Explore collective management features

2. **Test the REST API**
   - Try different HTTP clients (curl, Postman, browser)
   - Examine the response format
   - Check error handling

3. **Set Up Additional Cluster Members**
   - Follow the [Cluster Setup](README.md#cluster-setup) section in README.md
   - Create and configure member servers
   - Join members to the collective

4. **Customize the Application**
   - Modify the REST endpoint
   - Add new features
   - Implement additional services

5. **Learn About Liberty Features**
   - Read IBM WebSphere Liberty documentation
   - Explore available features
   - Understand collective architecture

## 🔗 Useful Commands Reference

### Maven Commands

```bash
# Clean build
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Run in development mode
mvn liberty:dev

# Run in production mode
mvn liberty:run

# Stop server
mvn liberty:stop

# Create server package
mvn liberty:package

# Run tests only
mvn test

# Update dependencies
mvn clean install -U
```

### Server Management

```bash
# Start server (from Liberty bin directory)
./server start controller

# Stop server
./server stop controller

# Check server status
./server status controller

# View server logs
./server dump controller

# Package server
./server package controller --include=usr
```

### Debugging

```bash
# Start in debug mode
mvn liberty:debug

# View real-time logs
tail -f target/liberty/wlp/usr/servers/controller/logs/messages.log

# Check for errors
grep ERROR target/liberty/wlp/usr/servers/controller/logs/messages.log
```

## 📞 Getting Help

If you encounter issues not covered in this guide:

1. **Check the logs**: `target/liberty/wlp/usr/servers/controller/logs/messages.log`
2. **Review the [Troubleshooting](README.md#troubleshooting) section** in README.md
3. **Consult IBM Documentation**: https://www.ibm.com/docs/en/was-liberty
4. **Check Maven output** for detailed error messages

## ✅ Setup Verification Checklist

Use this checklist to verify your setup is complete:

- [ ] JDK 17+ installed and verified
- [ ] Maven 3.6+ installed and verified
- [ ] Project cloned successfully
- [ ] `mvn clean install` completes without errors
- [ ] Server starts with `mvn liberty:dev`
- [ ] REST API accessible at http://localhost:9080/liberty-cluster-app/api/cluster
- [ ] Admin Center accessible at https://localhost:9443/adminCenter/
- [ ] Can login to Admin Center
- [ ] Application shows as "Started" in Admin Center
- [ ] No ERROR messages in server logs
- [ ] Can stop server gracefully

---

**Congratulations!** 🎉 Your Liberty Cluster Deployment is now set up and running.

For more information, see the main [README.md](README.md) file.