@echo off
setlocal

set "JAVA_HOME=%~dp0.tools\jdk25\jdk-25.0.3+9"
set "PATH=%JAVA_HOME%\bin;%PATH%"

call "%~dp0gradlew.bat" build --no-daemon %*
