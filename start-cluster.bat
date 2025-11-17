@echo off
REM Liberty Cluster Startup Script for Windows
REM This script starts all cluster members in separate command prompt windows

echo.
echo 🚀 Starting Liberty Cluster...
echo.

REM Start controller
start "Liberty Controller" cmd /k "cd liberty-cluster-app-ear && mvn liberty:dev"
echo ✅ Controller starting on ports 9080/9443
timeout /t 2 /nobreak >nul

REM Start member1
start "Liberty Member 1" cmd /k "cd liberty-cluster-member1 && mvn pre-integration-test liberty:run"
echo ✅ Member1 starting on ports 9081/9444
timeout /t 2 /nobreak >nul

REM Start member2
start "Liberty Member 2" cmd /k "cd liberty-cluster-member2 && mvn pre-integration-test liberty:run"
echo ✅ Member2 starting on ports 9082/9445

echo.
echo ⏳ Waiting for servers to start (this may take 1-2 minutes)...
echo.
echo 📍 Access Points:
echo    Controller:  http://localhost:9080/liberty-cluster-app/api/cluster
echo    Member 1:    http://localhost:9081/liberty-cluster-app/api/cluster
echo    Member 2:    http://localhost:9082/liberty-cluster-app/api/cluster
echo.
echo 🔐 Admin Centers:
echo    Controller:  https://localhost:9443/adminCenter/ (admin/adminpwd)
echo    Member 1:    https://localhost:9444/adminCenter/ (admin/adminpwd)
echo    Member 2:    https://localhost:9445/adminCenter/ (admin/adminpwd)
echo.
echo 💡 To stop all servers, press Ctrl+C in each command prompt window
echo.
pause

@REM Made with Bob
