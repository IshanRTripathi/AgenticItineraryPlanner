# Stage 1: Build with Gradle
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle clean build -x test

# Stage 2: Run the app
FROM eclipse-temurin:17-jre
WORKDIR /app
# Copy only the Spring Boot fat JAR (not plain.jar)
COPY --from=build /app/build/libs/*SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
