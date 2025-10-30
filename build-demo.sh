#!/bin/bash
set -e

echo "🔧 Building NEAR Kotlin JSON-RPC Demo..."

# Step 1: Fix types
echo "📝 Step 1/3: Fixing NEAR types..."
bash scripts/fix_near_types.sh

# Step 2: Build demo JAR
echo "🏗️ Step 2/3: Building demo application..."
./gradlew :demo-app:shadowJar --no-daemon

# Step 3: Verify JAR exists
echo "✅ Step 3/3: Verifying build..."
if [ -f "demo-app/build/libs/near-demo.jar" ]; then
    echo "✨ Build successful! JAR created at: demo-app/build/libs/near-demo.jar"
    ls -lh demo-app/build/libs/near-demo.jar
else
    echo "❌ Error: JAR file not found!"
    exit 1
fi

echo "🎉 Demo app is ready to deploy!"
