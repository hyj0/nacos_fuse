@echo off
REM Nacos FUSE 启动脚本 (Windows) - 支持多服务器配置

echo ========================================
echo Nacos FUSE File System - Build and Run
echo ========================================
echo.

REM 检查 Maven
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Maven not found in PATH
    echo Please install Maven and add it to PATH
    echo Download from: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

REM 编译项目
echo [INFO] Building project...
call mvn clean package -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Build failed
    pause
    exit /b 1
)

echo.
echo [SUCCESS] Build completed
echo.

REM 设置默认参数
set CONFIG_FILE=config.yaml
set MOUNT_POINT=N:

REM 如果提供了参数，使用参数
if not "%~1"=="" set CONFIG_FILE=%~1
if not "%~2"=="" set MOUNT_POINT=%~2

echo ========================================
echo Starting Nacos FUSE (Multi-Server)...
echo Config File: %CONFIG_FILE%
echo Mount Point: %MOUNT_POINT%
echo ========================================
echo.
echo IMPORTANT: Make sure WinFsp is installed!
echo Download from: https://winfsp.dev/rel/
echo.

REM 运行程序
java -jar target\nacos_fuse-1.0-SNAPSHOT.jar %CONFIG_FILE% %MOUNT_POINT%

pause
