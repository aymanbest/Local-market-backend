#!/bin/bash
echo "Building the application with Maven..."
mvn clean package -DskipTests

echo "Extracting dependencies..."
mvn dependency:copy-dependencies

echo "Starting the application..."
java -cp "target/*.jar:target/dependency/*" -Dloader.main=com.localmarket.main.LocalMarketApplication org.springframework.boot.loader.PropertiesLauncher 