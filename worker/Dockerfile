# Stage 1: Build JAR using Maven
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy dependency definition and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code and build
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: JRE Runtime with Docker CLI installed
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install Docker CLI tool and GCC (g++) for local compiling options
RUN apk add --no-cache docker-cli g++

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
