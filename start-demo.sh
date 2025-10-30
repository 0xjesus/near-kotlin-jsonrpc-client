#!/bin/bash
set -e

# Get PORT from environment or default to 8080
PORT="${PORT:-8080}"

# Java options
JAVA_OPTS="${JAVA_OPTS:--Xmx512m -Xms256m}"

echo "🚀 Starting NEAR Kotlin JSON-RPC Demo..."
echo "📍 Port: $PORT"
echo "🔧 Java Options: $JAVA_OPTS"

# Check if JAR exists
JAR_PATH="demo-app/build/libs/near-demo.jar"
if [ ! -f "$JAR_PATH" ]; then
    echo "❌ Error: $JAR_PATH not found!"
    echo "Please run ./build-demo.sh first"
    exit 1
fi

# Start the application
exec java $JAVA_OPTS -jar "$JAR_PATH"
