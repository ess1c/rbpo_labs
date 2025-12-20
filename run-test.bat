@echo off
echo Starting application in test mode (no SSL, H2 database)...
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=test
pause

