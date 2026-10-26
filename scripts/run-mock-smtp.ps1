$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
Set-Location "$PSScriptRoot\.."

Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host "Starting Standalone Mock SMTP Server on port 1025 (GreenMail)..." -ForegroundColor Cyan
Write-Host "No Docker required! Emails sent by I-ITSM will display in console." -ForegroundColor Green
Write-Host "==================================================================" -ForegroundColor Cyan

.\mvnw.cmd org.codehaus.mojo:exec-maven-plugin:3.2.0:java "-Dexec.mainClass=com.iitsm.util.MockSmtpServer" "-Dexec.classpathScope=test"

