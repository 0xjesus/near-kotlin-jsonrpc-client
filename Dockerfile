FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# Copy project files
COPY --chown=gradle:gradle . /app

# Make scripts executable and install dependencies
RUN apt-get update && apt-get install -y python3 python3-pip && rm -rf /var/lib/apt/lists/*
RUN chmod +x gradlew 2>/dev/null || true
RUN chmod +x scripts/fix_near_types.sh 2>/dev/null || true

# Build the demo application with verbose output
RUN echo "=== Starting build process ===" && \
    bash scripts/fix_near_types.sh && \
    echo "=== Types fixed, building JAR ===" && \
    ./gradlew :demo-app:shadowJar --no-daemon --info && \
    echo "=== Build complete, verifying JAR ===" && \
    ls -lah demo-app/build/libs/ && \
    test -f demo-app/build/libs/near-demo.jar && \
    echo "=== JAR verified successfully ==="

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the built jar
COPY --from=build /app/demo-app/build/libs/near-demo.jar /app/app.jar

# Expose port
EXPOSE 8080

# Set environment
ENV PORT=8080
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1

# Run the application
CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
