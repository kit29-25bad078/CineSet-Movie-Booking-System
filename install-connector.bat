@echo off
setlocal
if not exist lib mkdir lib
if exist "lib\mysql-connector-j-9.4.0.jar" (
  echo MySQL Connector/J already installed.
  exit /b 0
)
echo Downloading MySQL Connector/J 9.4.0...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$u='https://repo.maven.apache.org/maven2/com/mysql/mysql-connector-j/9.4.0/mysql-connector-j-9.4.0.jar'; Invoke-WebRequest -Uri $u -OutFile 'lib\mysql-connector-j-9.4.0.jar'"
if errorlevel 1 (
  echo.
  echo DOWNLOAD FAILED.
  echo If internet is unavailable, download MySQL Connector/J 9.4.0 manually and place the JAR inside the lib folder.
  exit /b 1
)
echo MySQL Connector/J installed successfully.
endlocal
