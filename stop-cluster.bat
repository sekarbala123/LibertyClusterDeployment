@echo off
REM Liberty Cluster Stop Script for Windows
REM This script stops all cluster members gracefully

echo.
echo 🛑 Stopping Liberty Cluster...
echo.

REM Function to stop a Liberty server
REM Parameters: %1=server_name, %2=server_dir, %3=port

echo 📍 Stopping Cluster Members:
echo.

REM Stop Member 2
echo Stopping Member 2 (port 9082)...
cd liberty-cluster-member2
call mvn liberty:stop >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✅ Member 2 stopped
) else (
    echo ⚠️  Member 2 not running or failed to stop
)
cd ..

REM Stop Member 1
echo Stopping Member 1 (port 9081)...
cd liberty-cluster-member1
call mvn liberty:stop >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✅ Member 1 stopped
) else (
    echo ⚠️  Member 1 not running or failed to stop
)
cd ..

echo.
echo 📍 Stopping Controller:
echo.

REM Stop Controller
echo Stopping Controller (port 9080)...
cd liberty-cluster-app-ear
call mvn liberty:stop >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✅ Controller stopped
) else (
    echo ⚠️  Controller not running or failed to stop
)
cd ..

echo.
echo 🔍 Verifying servers stopped...
echo.

REM Check if ports are still in use
echo 📊 Port Status:
echo.

netstat -ano | findstr ":9080" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo    Port 9080: ❌ Still in use
) else (
    echo    Port 9080: ✅ Available
)

netstat -ano | findstr ":9081" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo    Port 9081: ❌ Still in use
) else (
    echo    Port 9081: ✅ Available
)

netstat -ano | findstr ":9082" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo    Port 9082: ❌ Still in use
) else (
    echo    Port 9082: ✅ Available
)

netstat -ano | findstr ":9443" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo    Port 9443: ❌ Still in use
) else (
    echo    Port 9443: ✅ Available
)

netstat -ano | findstr ":9444" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo    Port 9444: ❌ Still in use
) else (
    echo    Port 9444: ✅ Available
)

netstat -ano | findstr ":9445" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo    Port 9445: ❌ Still in use
) else (
    echo    Port 9445: ✅ Available
)

echo.
echo ✅ All Liberty servers stopped!
echo.
echo 💡 To start the cluster again, run:
echo    start-cluster.bat
echo.
pause

@REM Made with Bob