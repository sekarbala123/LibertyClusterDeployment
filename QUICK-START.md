# Liberty Collective - Quick Start Guide

## ⚡ Essential Commands

### Start Everything
```bash
./start-cluster.sh
```

### Stop Everything
```bash
./stop-cluster.sh
```

### Check Status
```bash
# All servers
ps aux | grep liberty

# Specific ports
lsof -i :9080  # Controller HTTP
lsof -i :9443  # Controller HTTPS
lsof -i :9444  # Member1 HTTPS
lsof -i :9445  # Member2 HTTPS
```

---

## 🌐 Access URLs

### Admin Centers
| Server | URL | Credentials |
|--------|-----|-------------|
| Controller | https://localhost:9443/adminCenter/ | admin / adminpwd |
| Member 1 | https://localhost:9444/adminCenter/ | admin / adminpwd |
| Member 2 | https://localhost:9445/adminCenter/ | admin / adminpwd |

### REST APIs (Controller Only)
```bash
# Cluster info
curl http://localhost:9080/liberty-cluster-app/api/cluster

# Member details
curl http://localhost:9080/liberty-cluster-app/api/members

# Collective members
curl http://localhost:9080/liberty-cluster-app/api/collective/members
```

---

## 📋 Server Details

| Server | HTTP | HTTPS | Location |
|--------|------|-------|----------|
| Controller | 9080 | 9443 | liberty-cluster-app-ear/target/liberty/wlp/usr/servers/controller |
| Member 1 | 9081 | 9444 | liberty-cluster-member1/target/liberty/wlp/usr/servers/member1 |
| Member 2 | 9082 | 9445 | liberty-cluster-member2/target/liberty/wlp/usr/servers/member2 |

---

## 🔧 Individual Server Control

### Controller
```bash
cd liberty-cluster-app-ear/target/liberty/wlp/bin

# Start
./server start controller

# Stop
./server stop controller

# Status
./server status controller

# Logs
tail -f ../usr/servers/controller/logs/messages.log
```

### Member 1
```bash
cd liberty-cluster-member1/target/liberty/wlp/bin

# Start
./server start member1

# Stop
./server stop member1

# Status
./server status member1

# Logs
tail -f ../usr/servers/member1/logs/messages.log
```

### Member 2
```bash
cd liberty-cluster-member2/target/liberty/wlp/bin

# Start
./server start member2

# Stop
./server stop member2

# Status
./server status member2

# Logs
tail -f ../usr/servers/member2/logs/messages.log
```

---

## 🔄 After Configuration Changes

```bash
# 1. Stop all servers
./stop-cluster.sh

# 2. Rebuild
mvn clean install

# 3. Copy configs (if you edited src files)
cp liberty-cluster-app-ear/src/main/liberty/config/server.xml \
   liberty-cluster-app-ear/target/liberty/wlp/usr/servers/controller/server.xml

cp liberty-cluster-member1/src/main/liberty/config/server.xml \
   liberty-cluster-member1/target/liberty/wlp/usr/servers/member1/server.xml

cp liberty-cluster-member2/src/main/liberty/config/server.xml \
   liberty-cluster-member2/target/liberty/wlp/usr/servers/member2/server.xml

# 4. Start all servers
./start-cluster.sh
```

---

## 🐛 Quick Troubleshooting

### Port Already in Use
```bash
# Find and kill process
lsof -i :9080
kill -9 <PID>
```

### View All Logs at Once
```bash
# Terminal 1
tail -f liberty-cluster-app-ear/target/liberty/wlp/usr/servers/controller/logs/messages.log

# Terminal 2
tail -f liberty-cluster-member1/target/liberty/wlp/usr/servers/member1/logs/messages.log

# Terminal 3
tail -f liberty-cluster-member2/target/liberty/wlp/usr/servers/member2/logs/messages.log
```

### Check Collective Status
```bash
# Member connection status
grep "CWWKX8055I" liberty-cluster-member1/target/liberty/wlp/usr/servers/member1/logs/messages.log
grep "CWWKX8055I" liberty-cluster-member2/target/liberty/wlp/usr/servers/member2/logs/messages.log

# Remote file access status
grep "CWWKX7912I" liberty-cluster-app-ear/target/liberty/wlp/usr/servers/controller/logs/messages.log
```

### Force Clean Restart
```bash
# Stop everything
./stop-cluster.sh

# Kill any remaining processes
pkill -9 -f liberty

# Clean build
mvn clean install

# Start fresh
./start-cluster.sh
```

---

## 📚 Documentation Files

- **COLLECTIVE-SETUP-COMPLETE.md** - Complete setup guide with all details
- **README.md** - Project overview and API documentation
- **SETUP.md** - Detailed setup instructions
- **CLUSTER-SETUP-GUIDE.md** - Step-by-step cluster configuration

---

## ✅ Success Indicators

When everything is working, you should see:

**Controller logs:**
```
CWWKF0011I: The controller server is ready to run a smarter planet
CWWKX7912I: The FileServiceMXBean attribute ReadList was successfully updated
```

**Member logs:**
```
CWWKF0011I: The member1 server is ready to run a smarter planet
CWWKX8055I: The collective member has established a connection to the collective controller
CWWKX8116I: The server STARTED state was successfully published to the collective repository
```

---

## 🎯 Current Status

✅ Collective controller configured  
✅ 2 members joined to collective  
✅ Remote file access enabled on all servers  
✅ SSL/TLS configured  
✅ Admin Center accessible  
✅ REST APIs operational  

**All systems operational!** 🚀

---

*For detailed information, see COLLECTIVE-SETUP-COMPLETE.md*