@echo off
set "JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d "%~dp0\.."
echo ==================================================================
echo Starting Standalone Mock SMTP Server on port 1025 (GreenMail)...
echo No Docker required! Emails sent by I-ITSM will display in console.
echo ==================================================================
.\mvnw.cmd org.codehaus.mojo:exec-maven-plugin:3.2.0:java "-Dexec.mainClass=com.iitsm.util.MockSmtpServer" "-Dexec.classpathScope=test"

