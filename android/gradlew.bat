@rem Gradle startup script for Windows.
@rem Usage:  gradlew.bat <task>      e.g.  gradlew.bat assembleDebug
@if "%DEBUG%"=="" @echo off
setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.\
set APP_BASE_NAME=%~n0
set CLASSPATH=%DIRNAME%gradle\wrapper\gradle-wrapper.jar

if not exist "%CLASSPATH%" (
    echo.
    echo ERROR: gradle\wrapper\gradle-wrapper.jar is missing.
    echo.
    echo Run this once from the project root to download it:
    echo     powershell -ExecutionPolicy Bypass -File tools\get-gradle-wrapper.ps1
    echo.
    exit /b 1
)

if defined JAVA_HOME (
    set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
    set "JAVA_EXE=java.exe"
)

set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

set GRADLE_EXIT_CODE=%ERRORLEVEL%
endlocal & exit /b %GRADLE_EXIT_CODE%
