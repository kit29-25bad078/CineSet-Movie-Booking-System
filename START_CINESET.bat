@echo off
setlocal
cd /d "%~dp0"
call install-connector.bat
if errorlevel 1 (
  pause
  exit /b 1
)
if not exist out mkdir out
javac -cp "lib\*" -d out src\*.java
if errorlevel 1 (
  echo.
  echo COMPILATION FAILED.
  pause
  exit /b 
)
echo.
echo Starting CineSet server...
start "CineSet Server" cmd /k "cd /d "%~dp0" && java -cp "out;lib\*" MovieBookingServer"
timeout /t 2 /nobreak >nul
start "" "http://localhost:8080/"
echo.
echo CineSet is opening at http://localhost:8080/
echo Admin: http://localhost:8080/admin.html
endlocal
