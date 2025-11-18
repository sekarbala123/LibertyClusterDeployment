# Liberty Cluster Member Startup Fix

## Problem Summary

The cluster member servers (member1 and member2) were failing to start with the error:
```
CWWKM2173E: Failed to install application from project. The project packaging type is not supported.
```

## Root Cause

The member modules were using `<packaging>pom</packaging>` which is incompatible with the `liberty:dev` goal. The Liberty Maven Plugin expects either `war`, `ear`, or `liberty-assembly` packaging types when using `liberty:dev`.

## Solution Implemented

### 1. Updated Member POMs (member1 and member2)

Added two key configurations:

#### A. Maven Dependency Plugin
Copies the EAR file from the controller module to the member's target/apps directory:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-dependency-plugin</artifactId>
    <executions>
        <execution>
            <id>copy-ear</id>
            <phase>pre-integration-test</phase>
            <goals>
                <goal>copy</goal>
            </goals>
            <configuration>
                <artifactItems>
                    <artifactItem>
                        <groupId>com.example</groupId>
                        <artifactId>liberty-cluster-app-ear</artifactId>
                        <version>${project.version}</version>
                        <type>ear</type>
                        <outputDirectory>${project.build.directory}/apps</outputDirectory>
                    </artifactItem>
                </artifactItems>
            </configuration>
        </execution>
    </executions>
</plugin>
```

#### B. Liberty Maven Plugin Configuration
Configured to create the server and install features during the pre-integration-test phase:

```xml
<plugin>
    <groupId>io.openliberty.tools</groupId>
    <artifactId>liberty-maven-plugin</artifactId>
    <version>3.3.4</version>
    <executions>
        <execution>
            <id>install-server</id>
            <phase>pre-integration-test</phase>
            <goals>
                <goal>create</goal>
                <goal>install-feature</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <serverName>member1</serverName> <!-- or member2 -->
        <deployPackages>dependencies</deployPackages>
        <runtimeArtifact>
                                <groupId>com.ibm.websphere.appserver.runtime</groupId>
                                <artifactId>wlp-kernel</artifactId>
                                <version>24.0.0.12</version>            <type>zip</type>
        </runtimeArtifact>
        <bootstrapProperties>
            <app.location>${project.build.directory}/apps/liberty-cluster-app-ear-${project.version}.ear</app.location>
        </bootstrapProperties>
    </configuration>
</plugin>
```

### 2. Updated Startup Scripts

Changed member startup commands from `liberty:dev` to `mvn pre-integration-test liberty:run`:

**Before:**
```bash
cd liberty-cluster-member1 && mvn liberty:dev
```

**After:**
```bash
cd liberty-cluster-member1 && mvn pre-integration-test liberty:run
```

This ensures:
1. The EAR is copied to target/apps (pre-integration-test phase)
2. The server is created and features are installed
3. The server starts with the deployed application

## How to Start the Cluster

### Option 1: Using Startup Scripts (Recommended)

#### On macOS/Linux:
```bash
./start-cluster.sh
```

#### On Windows:
```cmd
start-cluster.bat
```

The scripts will:
- Open separate terminal windows for each server
- Start the controller with `mvn liberty:dev`
- Start member1 and member2 with `mvn pre-integration-test liberty:run`
- Display access URLs for all servers

### Option 2: Manual Startup

Open three separate terminal windows:

**Terminal 1 - Controller:**
```bash
cd liberty-cluster-app-ear
mvn liberty:dev
```

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

### Option 3: Prepare Members First (Alternative)

If you want to prepare the members before starting:

```bash
# Prepare both members (copy EAR and create servers)
mvn pre-integration-test -pl liberty-cluster-member1,liberty-cluster-member2

# Then start each in separate terminals
cd liberty-cluster-member1 && mvn liberty:run
cd liberty-cluster-member2 && mvn liberty:run
```

## Verification

Once all servers are running, verify they're accessible:

### REST API Endpoints:
```bash
# Controller
curl http://localhost:9080/liberty-cluster-app/api/cluster

# Member 1
curl http://localhost:9081/liberty-cluster-app/api/cluster

# Member 2
curl http://localhost:9082/liberty-cluster-app/api/cluster
```

### Admin Centers:
- Controller: https://localhost:9443/adminCenter/ (admin/admin)
- Member 1: https://localhost:9444/adminCenter/ (admin/admin)
- Member 2: https://localhost:9445/adminCenter/ (admin/admin)

## Key Differences: liberty:dev vs liberty:run

### liberty:dev (Controller)
- **Hot reload**: Automatically detects and reloads code changes
- **Interactive**: Keeps terminal open with dev mode features
- **Best for**: Active development on the controller

### liberty:run (Members)
- **Standard mode**: Runs the server without hot reload
- **Simpler**: Just starts and runs the server
- **Best for**: Member servers that deploy pre-built EAR files

## Technical Details

### Why POM Packaging?

Member modules use POM packaging because they:
1. Don't build their own artifacts (WAR/EAR)
2. Deploy the EAR built by the controller module
3. Only need to configure and run a Liberty server

### Deployment Flow

```
1. Build Phase (mvn clean install)
   └─> liberty-cluster-app-war builds WAR
   └─> liberty-cluster-app-ear packages WAR into EAR

2. Pre-Integration-Test Phase (member modules)
   └─> maven-dependency-plugin copies EAR to target/apps
   └─> liberty-maven-plugin creates server
   └─> liberty-maven-plugin installs features

3. Run Phase (liberty:run)
   └─> Server starts
   └─> Deploys EAR from target/apps
   └─> Application becomes available
```

### Bootstrap Properties

The `app.location` bootstrap property tells Liberty where to find the EAR:

```xml
<bootstrapProperties>
    <app.location>${project.build.directory}/apps/liberty-cluster-app-ear-${project.version}.ear</app.location>
</bootstrapProperties>
```

This is referenced in server.xml:
```xml
<application location="${app.location}" />
```

## Troubleshooting

### Issue: "Cannot find EAR file"
**Solution:** Run `mvn pre-integration-test` first to copy the EAR

### Issue: "Server already exists"
**Solution:** Clean the target directory:
```bash
mvn clean
```

### Issue: "Port already in use"
**Solution:** Check if another server is running on the same port:
```bash
# macOS/Linux
lsof -i :9081
lsof -i :9082

# Windows
netstat -ano | findstr :9081
netstat -ano | findstr :9082
```

### Issue: "Features not installed"
**Solution:** The pre-integration-test phase installs features automatically. If needed, run:
```bash
mvn liberty:install-feature -pl liberty-cluster-member1
```

## Summary

The fix enables member servers to:
1. ✅ Use POM packaging (appropriate for deployment-only modules)
2. ✅ Copy and deploy the controller's EAR file
3. ✅ Start successfully with `liberty:run`
4. ✅ Run independently on different ports
5. ✅ Form a functional cluster with the controller

All three servers (controller + 2 members) now start successfully and serve the same application on different ports!