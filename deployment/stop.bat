@echo off
echo ========================================
echo   Finance Platform - App stop
echo ========================================
echo.

REM Поиск процесса Java, запустившего наш JAR
echo [1/2] Search fo app task...

for /f "tokens=2" %%a in ('tasklist ^| findstr /i "java" ^| findstr /i "finance-platform"') do (
    set PID=%%a
)

if defined PID (
    echo [2/2] App stop withPID: %PID%
    taskkill /F /PID %PID%
    if errorlevel 1 (
        echo [ERROR] The process wasn't stopped.
    ) else (
        echo [SUCCESS] The application has been stopped.
    )
) else (
    echo [ИНФО] Started app wasn't found.
)

echo.
pause