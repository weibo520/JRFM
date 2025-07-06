@echo off
chcp 437 > nul
title File Management System

echo File Management System
echo =====================
echo.

REM Kill any running Java processes
taskkill /f /im java.exe >nul 2>&1

REM Create necessary directories
if not exist build mkdir build
if not exist files mkdir files

REM Compile code
echo Compiling code...
javac -encoding UTF-8 -d build src/server/*.java
javac -encoding UTF-8 -d build src/client/*.java
echo Compilation complete!
echo.

REM Start server
echo Starting server...
start "Server" cmd /k "cd /d %~dp0build && java server.FileServer 8888 ../files"

REM Wait for server to start
echo Waiting for server to start...
timeout /t 3 > nul

REM Start GUI client
echo Starting GUI client...
start "GUI Client" cmd /k "cd /d %~dp0build && java client.GUI localhost 8888"

REM Start Command Line client
echo Starting Command Line client...
start "Command Line Client" cmd /k "cd /d %~dp0build && java client.CommandLineInterface localhost 8888"

echo.
echo All components started!
echo.
pause 