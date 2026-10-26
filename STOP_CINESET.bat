@echo off
taskkill /FI "WINDOWTITLE eq CineSet Server" /T /F >nul 2>&1
echo CineSet server stopped.
pause
