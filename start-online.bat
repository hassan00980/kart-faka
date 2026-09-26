@echo off
rem ============================================================
rem  كرت فكة — شغّل اللعبة أونلاين (دبل كليك على الملف ده وبس)
rem ============================================================
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-online.ps1"
if errorlevel 1 (
  echo.
  echo حصل خطأ — راجع الرسالة اللي فوق وافتح الملف start-online.ps1
  pause
)