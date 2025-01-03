# Stage 1: Build Stage
FROM eclipse-temurin:21 AS build

WORKDIR /app

# Copy necessary files from the root
COPY ../gradlew ./gradlew
COPY ../gradle ./gradle
COPY ../settings.gradle.kts ./

# Copy the subproject into the container
COPY ./configservice/ ./configservice/
COPY ./shared/ ./shared/

# Build only the subproject
RUN ./gradlew :configservice:bootJar

# Stage 2: Runtime Stage
FROM eclipse-temurin:21

WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/configservice/build/libs/*.jar ./app.jar

EXPOSE 8080

# Set the default command to run the JAR
ENTRYPOINT ["java", "-jar", "app.jar"]
