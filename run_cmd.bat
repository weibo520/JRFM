@echo off
chcp 437 > nul
title Command Line Interface

echo Starting Command Line Interface...
echo ===============================
echo.

REM 编译代码
javac -encoding UTF-8 -d build src/server/*.java
javac -encoding UTF-8 -d build src/client/*.java

REM 启动服务器
start "Server" cmd /k "cd /d %~dp0build && java server.FileServer 8888 ../files"

REM 等待服务器启动
timeout /t 2 > nul

REM 启动命令行客户端
cmd /k "cd /d %~dp0build && java client.CommandLineInterface localhost 8888" 