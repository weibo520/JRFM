@echo off
chcp 65001 > nul
title Java远程文件管理系统

echo Java远程文件管理系统启动脚本
echo ============================
echo.

REM 关闭可能正在运行的Java进程
taskkill /f /im java.exe >nul 2>&1

REM 创建必要的目录
if not exist build mkdir build
if not exist files mkdir files

REM 编译代码
echo 正在编译代码...
javac -encoding UTF-8 -d build src/server/*.java
javac -encoding UTF-8 -d build src/client/*.java
echo 编译完成！
echo.

REM 选择要启动的组件
echo 请选择要启动的组件:
echo [1] 服务器 + 图形界面客户端
echo [2] 服务器 + 命令行客户端
echo [3] 仅启动服务器
echo [4] 仅启动图形界面客户端
echo [5] 仅启动命令行客户端
echo.

set /p choice=请选择 [1-5]: 

REM 根据选择启动相应组件
if "%choice%"=="1" (
    echo 启动服务器...
    start "服务器" cmd /k "cd /d %~dp0build && java server.FileServer 8888 ../files"
    timeout /t 2 > nul
    echo 启动图形界面客户端...
    start "图形界面客户端" cmd /k "cd /d %~dp0build && java client.GUI localhost 8888"
) else if "%choice%"=="2" (
    echo 启动服务器...
    start "服务器" cmd /k "cd /d %~dp0build && java server.FileServer 8888 ../files"
    timeout /t 2 > nul
    echo 启动命令行客户端...
    start "命令行客户端" cmd /k "cd /d %~dp0build && java client.CommandLineInterface localhost 8888"
) else if "%choice%"=="3" (
    echo 启动服务器...
    start "服务器" cmd /k "cd /d %~dp0build && java server.FileServer 8888 ../files"
) else if "%choice%"=="4" (
    echo 启动图形界面客户端...
    start "图形界面客户端" cmd /k "cd /d %~dp0build && java client.GUI localhost 8888"
) else if "%choice%"=="5" (
    echo 启动命令行客户端...
    start "命令行客户端" cmd /k "cd /d %~dp0build && java client.CommandLineInterface localhost 8888"
) else (
    echo 无效选择！
    pause
    exit /b 1
)

echo.
echo 系统已启动！
echo.
pause 