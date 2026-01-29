@echo off
setlocal

REM Set JAVA_HOME if not set
if "%JAVA_HOME%"=="" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-25"
)

REM Set Maven wrapper path
set "MVN_CMD=%~dp0mvnw.cmd"

REM Run the project using Maven
echo Building and running the project...
call "%MVN_CMD%" clean javafx:run

pause
