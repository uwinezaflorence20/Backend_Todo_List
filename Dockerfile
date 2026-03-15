# Stage 1: Build the application using Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies first to cache them
RUN mvn dependency:go-offline -B
# Copy the source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the application in a minimal JRE container
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Copy the built jar file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port 8080
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]
