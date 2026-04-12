FROM maven:3.9.9-eclipse-temurin-21 AS base-build

FROM base-build AS app-build
WORKDIR /app

# Copy the pom.xml file
COPY eureka-server/pom.xml .
RUN mvn dependency:resolve-plugins dependency:resolve -B

# Copy the source code
COPY eureka-server/src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-jammy

# Set the working directory
WORKDIR /app

# Copy the built jar file from the build stage (the name comes from pom.xml)
COPY --from=app-build /app/target/*.jar app.jar

# Configurações para rodar com Java 21 e Virtual Threads de forma otimizada
ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:+ZGenerational", "-jar", "app.jar"]
