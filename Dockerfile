FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# Copy project files
COPY --chown=gradle:gradle . /app

# Build the demo application
RUN bash scripts/fix_near_types.sh
RUN gradle :demo-app:shadowJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the built jar
COPY --from=build /app/demo-app/build/libs/near-demo.jar /app/app.jar

# Expose port
EXPOSE 8080

# Set environment
ENV PORT=8080

# Run the application
CMD ["java", "-jar", "/app/app.jar"]
