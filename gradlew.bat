@echo off
where gradle >nul 2>nul
if errorlevel 1 (
  echo Gradle is not installed or not available on PATH. Install it first or use the GitHub Actions workflow.
  exit /b 1
)

gradle %*
