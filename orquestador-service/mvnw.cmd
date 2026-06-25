@ECHO OFF
SETLOCAL

SET "BASE_DIR=%~dp0"
IF "%MAVEN_USER_HOME%"=="" SET "MAVEN_USER_HOME=%USERPROFILE%\.m2"

SET "DISTS_DIR=%MAVEN_USER_HOME%\wrapper\dists"
SET "MAVEN_HOME=%DISTS_DIR%\apache-maven-3.9.9"

IF NOT EXIST "%MAVEN_HOME%\bin\mvn.cmd" (
  ECHO Downloading Apache Maven 3.9.9...
  IF NOT EXIST "%DISTS_DIR%" MKDIR "%DISTS_DIR%"
  powershell -NoProfile -NonInteractive -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip' -OutFile '%DISTS_DIR%\apache-maven-3.9.9-bin.zip'"
  powershell -NoProfile -NonInteractive -Command "Expand-Archive -Path '%DISTS_DIR%\apache-maven-3.9.9-bin.zip' -DestinationPath '%DISTS_DIR%' -Force"
  DEL /F /Q "%DISTS_DIR%\apache-maven-3.9.9-bin.zip"
  ECHO Done.
)

"%MAVEN_HOME%\bin\mvn.cmd" %*
ENDLOCAL
