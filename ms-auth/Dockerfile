FROM maven:3.9.9-eclipse-temurin-21 AS build

# Set the working directory
WORKDIR /app

# Copia as libs
COPY libs/common-exception /libs/common-exception
RUN mvn -f /libs/common-exception/pom.xml clean install -DskipTests

COPY libs/common-security /libs/common-security
RUN mvn -f /libs/common-security/pom.xml clean install -DskipTests

# Copy the pom.xml file
COPY ms-auth/pom.xml .

# Copy the source code
COPY ms-auth/src ./src

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-jammy

# Set the working directory
WORKDIR /app

# Copy the built jar file from the build stage (the name comes from pom.xml)
COPY --from=build /app/target/*.jar app.jar

# Command to run the application
ENTRYPOINT ["java","-jar","app.jar"]