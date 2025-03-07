@echo off
echo Setting up clean database mode...
set SPRING_JPA_HIBERNATE_DDL_AUTO=create

echo Building the application with Maven...
call mvn clean package -DskipTests

echo Extracting dependencies...
call mvn dependency:copy-dependencies

echo Starting the application with clean database...
java -cp "target/*.jar;target/dependency/*" -Dloader.main=com.localmarket.main.LocalMarketApplication -Dspring.jpa.hibernate.ddl-auto=create org.springframework.boot.loader.PropertiesLauncher

pause 