@echo off
echo Building the application with Maven...
call mvn clean package -DskipTests

echo Starting the application...
java -cp "target/*.jar;target/dependency/*" -Dloader.main=com.localmarket.main.LocalMarketApplication org.springframework.boot.loader.PropertiesLauncher

pause 