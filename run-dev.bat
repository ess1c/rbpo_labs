@echo off
echo Starting application in development mode (no SSL, H2 database)...
call mvnw.cmd clean compile
call mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
pause

