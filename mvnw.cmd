@echo off
setlocal EnableDelayedExpansion

set "SCRIPT_DIR=%~dp0"
set "WRAPPER_PROPS=!SCRIPT_DIR!.mvn\wrapper\maven-wrapper.properties"
set "DIST_URL="

if not exist "!WRAPPER_PROPS!" (
    echo [mvnw] ERROR: !WRAPPER_PROPS! not found 1>&2
    exit /b 1
)

for /f "usebackq tokens=1,* delims==" %%A in ("!WRAPPER_PROPS!") do (
    if /i "%%A"=="distributionUrl" set "DIST_URL=%%B"
)

if "!DIST_URL!"=="" (
    echo [mvnw] ERROR: distributionUrl not found in !WRAPPER_PROPS! 1>&2
    exit /b 1
)

for %%F in (!DIST_URL!) do set "DIST_FILENAME=%%~nxF"
set "DIST_NAME=!DIST_FILENAME:-bin.zip=!"

set "DIST_CACHE=%USERPROFILE%\.m2\wrapper\dists\!DIST_NAME!"
set "MVN_CMD=!DIST_CACHE!\!DIST_NAME!\bin\mvn.cmd"

if not exist "!MVN_CMD!" (
    echo [mvnw] Maven !DIST_NAME! not found in cache. Downloading...
    if not exist "!DIST_CACHE!" md "!DIST_CACHE!"
    powershell -noprofile -ExecutionPolicy Bypass -Command "& { $url='!DIST_URL!'; $zip='!DIST_CACHE!\!DIST_FILENAME!'; Invoke-WebRequest $url -OutFile $zip; Expand-Archive $zip -DestinationPath '!DIST_CACHE!' -Force; Remove-Item $zip }"
    if !ERRORLEVEL! neq 0 (
        echo [mvnw] ERROR: Download or extraction failed 1>&2
        exit /b 1
    )
    echo [mvnw] Maven downloaded to !DIST_CACHE!
)

"!MVN_CMD!" %*
exit /b %ERRORLEVEL%
