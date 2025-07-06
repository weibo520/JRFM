@echo off
chcp 65001 > nul
title Java远程文件管理系统 - 命令行客户端

echo Java远程文件管理系统 - 命令行客户端
echo ====================================
echo.

REM 创建必要的目录
if not exist build mkdir build

REM 编译客户端代码
echo 正在编译客户端代码...
javac -encoding UTF-8 -d build src/client/*.java
echo 编译完成！
echo.

REM 启动命令行客户端
echo 启动命令行客户端...
cd build
java client.CommandLineInterface localhost 8888

echo.
pause 