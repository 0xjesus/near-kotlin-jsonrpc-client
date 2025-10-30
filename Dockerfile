FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# Copy project files
COPY --chown=gradle:gradle . /app

# Make scripts executable
RUN chmod +x gradlew scripts/fix_near_types.sh 2>/dev/null || true

# Build the demo application
RUN bash scripts/fix_near_types.sh || echo "Fix types script completed"
RUN gradle :demo-app:shadowJar --no-daemon || ./gradlew :demo-app:shadowJar --no-daemon

# Verify the JAR was built
RUN ls -la demo-app/build/libs/ && test -f demo-app/build/libs/near-demo.jar

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
