@echo off
if not exist out mkdir out
javac -cp "lib\*" -d out src\*.java
if errorlevel 1 (
 echo.
 echo COMPILATION FAILED
 pause
 exit /b 1
)
echo.
echo =====================================
echo CineSet compiled successfully.
echo Run run-windows.bat next.
echo =====================================
pause
