#!/bin/bash
set -e

JAR_PATH="demo-app/build/libs/near-demo.jar"

echo "🚀 Starting NEAR Kotlin Demo..."

# Check if JAR exists
if [ ! -f "$JAR_PATH" ]; then
    echo "❌ JAR not found at $JAR_PATH"
    echo "🔨 Building JAR now..."

    # Build the JAR
    bash scripts/fix_near_types.sh || echo "Fix types completed"
    ./gradlew :demo-app:shadowJar --no-daemon

    # Verify it was created
    if [ ! -f "$JAR_PATH" ]; then
        echo "❌ Build failed - JAR still not found!"
        exit 1
    fi

    echo "✅ JAR built successfully"
fi

echo "✅ JAR found at $JAR_PATH"
echo "📊 JAR size: $(du -h $JAR_PATH | cut -f1)"

# Get PORT from environment or default
PORT="${PORT:-8080}"
JAVA_OPTS="${JAVA_OPTS:--Xmx512m -Xms256m}"

echo "🚀 Starting application on port $PORT..."
echo "🔧 Java options: $JAVA_OPTS"

# Start the app
exec java $JAVA_OPTS -jar "$JAR_PATH"
