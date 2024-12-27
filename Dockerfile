# Stage 1: Build the Spring Boot application
FROM gradle:8.11.1-jdk17 AS build

# Set the working directory
WORKDIR /app

# Copy the project files
COPY . .

# Build the Spring Boot application
RUN gradle build -x test

# Stage 2: Run the Spring Boot application
FROM eclipse-temurin:17-jdk-alpine AS runtime

# Set the working directory
WORKDIR /app

# Copy the built application from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose the application port
EXPOSE 8080

# Command to run the application with the Datadog Java Agent
CMD ["java", "-jar", "app.jar"]
