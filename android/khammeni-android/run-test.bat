@echo off
chcp 65001 >nul
set CP=%TEMP%\khammeni-test\out;%TEMP%\nanohttpd.jar
java -Dstdout.encoding=UTF-8 -Dfile.encoding=UTF-8 -cp "%CP%" TestServer > "%TEMP%\test-out.txt" 2>&1
echo EXIT=%ERRORLEVEL%