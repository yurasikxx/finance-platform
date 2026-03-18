@echo off
echo ========================================
echo   Finance Platform - Application Starter
echo ========================================
echo.

REM Проверка наличия Java
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java is not found. Install JDK 21.
    pause
    exit /b 1
)

REM Проверка наличия JAR-файла
if not exist "finance-platform.jar" (
    echo [ERROR] File finance-platform.jar is not found!
    pause
    exit /b 1
)

echo [1/3] Starting application...
echo.

REM Запуск приложения с параметрами БД
java -jar finance-platform.jar ^
  --spring.datasource.url=jdbc:postgresql://localhost:5432/finance_platform ^
  --spring.datasource.username=postgres ^
  --spring.datasource.password=postgres

REM Если произошла ошибка
if errorlevel 1 (
    echo.
    echo [ERROR] The application stopped with an error.
    pause
)

pause