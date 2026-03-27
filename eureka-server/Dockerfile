FROM maven:3.9.9-eclipse-temurin-21 AS build

# Set the working directory
WORKDIR /app

# Copy the pom.xml file
COPY eureka-server/pom.xml .

# Copy the source code
COPY eureka-server/src ./src

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