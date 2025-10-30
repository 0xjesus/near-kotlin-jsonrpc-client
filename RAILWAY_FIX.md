# Railway Deployment - FIXED

## ⚠️ IMPORTANT: Manual Configuration Required

Railway needs manual configuration. Follow these EXACT steps:

## 🔧 Step 1: Configure Build Method

In Railway Dashboard → Settings → Build:

### Option A: Use Dockerfile (RECOMMENDED)

1. **Builder**: Select `Dockerfile`
2. **Dockerfile Path**: `Dockerfile`
3. **Build Command**: Leave empty (Dockerfile handles it)

### Option B: Use Nixpacks (FALLBACK)

If Dockerfile fails:

1. **Builder**: Select `Nixpacks`
2. **Build Command**:
   ```bash
   bash scripts/fix_near_types.sh && ./gradlew :demo-app:shadowJar --no-daemon
   ```
3. **Install Command**:
   ```bash
   apt-get update && apt-get install -y python3
   ```

## 🚀 Step 2: Configure Start Command

In Railway Dashboard → Settings → Deploy:

### For Dockerfile:
```bash
java -Xmx512m -jar /app/app.jar
```

### For Nixpacks:
```bash
bash run-demo.sh
```

## 📝 Step 3: Set Environment Variables

Add these in Railway Dashboard → Variables:

```
PORT=8080
JAVA_OPTS=-Xmx512m -Xms256m
```

## ✅ Step 4: Deploy

1. Click "Deploy" or push to GitHub
2. Wait 5-7 minutes for build
3. Check logs for "Application started"

## 🐛 If Still Failing

### Debug Steps:

1. **Check Build Logs**:
   - Look for "=== JAR verified successfully ===" (Dockerfile)
   - Or "JAR built successfully" (Nixpacks)

2. **If JAR not building**:
   - Go to Settings → General
   - Change Root Directory to: `/`
   - Redeploy

3. **If Python errors**:
   - Ensure Python 3 is installed in build phase
   - Check that `scripts/fix_near_types.sh` runs

4. **If still fails - Nuclear Option**:
   ```bash
   # In Railway Settings → Deploy → Start Command:
   bash -c "bash scripts/fix_near_types.sh && ./gradlew :demo-app:shadowJar --no-daemon && java -Xmx512m -jar demo-app/build/libs/near-demo.jar"
   ```

   This builds AND runs in one command (slower startup but guaranteed to work)

## 🎯 Verification

Once deployed, test:

```bash
# Health check
curl https://your-app.railway.app/health

# RPC call
curl -X POST https://your-app.railway.app/api/rpc/testnet \
  -H "Content-Type: application/json" \
  -d '{"method":"status","params":null}'
```

## 📞 Still Having Issues?

The `run-demo.sh` script is a failsafe that:
- Checks if JAR exists
- Builds it if missing
- Starts the app

If Dockerfile doesn't work, switch to Nixpacks with `run-demo.sh` start command.

## 🔍 Understanding the Problem

Railway was failing because:
1. ❌ Nixpacks wasn't running build commands properly
2. ❌ JAR file wasn't being created during build phase
3. ❌ Start command tried to run non-existent JAR

Now we have:
1. ✅ Dockerfile builds JAR reliably (Option A)
2. ✅ run-demo.sh builds JAR at startup if needed (Option B)
3. ✅ Multiple fallback options

## 🚀 Expected Build Output

You should see:
```
=== Starting build process ===
=== Types fixed, building JAR ===
BUILD SUCCESSFUL in 3m 45s
=== Build complete, verifying JAR ===
-rw-r--r-- 1 gradle gradle 21M near-demo.jar
=== JAR verified successfully ===
```

Then on startup:
```
🚀 Starting application on port 8080...
Application started in 0.078 seconds
Responding at http://0.0.0.0:8080
```

---

**If you see these messages, your deployment is successful! 🎉**
