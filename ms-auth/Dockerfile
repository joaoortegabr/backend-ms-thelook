# Estágio 1: Build das Dependências Comuns
FROM maven:3.9.9-eclipse-temurin-21 AS base-build
WORKDIR /libs

# Copia e instala as libs locais primeiro (Camada de Cache)
# Isso só rodará novamente se os arquivos nas pastas libs mudarem
COPY libs/common-exception ./common-exception
RUN mvn -f ./common-exception/pom.xml clean install -DskipTests

COPY libs/common ./common
RUN mvn -f ./common/pom.xml clean install -DskipTests

# Estágio 2: Build do Microsserviço
FROM base-build AS app-build
WORKDIR /app

# Copia apenas o pom.xml primeiro para baixar as dependências do Maven Central
# Isso evita baixar a internet toda vez que você mudar uma linha de código Java
COPY ms-auth/pom.xml .
RUN mvn dependency:resolve-plugins dependency:resolve -B

# Agora copia o código fonte e gera o jar
COPY ms-auth/src ./src
RUN mvn clean package -DskipTests -B

# Estágio 3: Runtime (Final)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=app-build /app/target/*.jar app.jar

# Configurações para rodar com Java 21 e Virtual Threads de forma otimizada
ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:+ZGenerational", "-jar", "app.jar"]
