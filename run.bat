@echo off
echo Building 
call mvn clean compile
if %ERRORLEVEL% neq 0 (
    echo Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)

call mvn javafx:run
pause
