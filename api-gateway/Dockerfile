FROM maven:3.9.9-eclipse-temurin-21 AS build

# Set the working directory
WORKDIR /app

# Copia as libs
COPY libs/common-reactive-exception /libs/common-reactive-exception
RUN mvn -f /libs/common-reactive-exception/pom.xml clean install -DskipTests

# Copy the pom.xml file
COPY api-gateway/pom.xml .

# Copy the source code
COPY api-gateway/src ./src

# Build the application
RUN mvn clean package

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-jammy

# Set the working directory
WORKDIR /app

# Copy the built jar file from the build stage (the name comes from pom.xml)
COPY --from=build /app/target/*.jar app.jar

# Command to run the application
ENTRYPOINT ["java","-jar","app.jar"]