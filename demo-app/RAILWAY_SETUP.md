# Railway Deployment Guide

Quick guide to deploy on Railway with screenshots and troubleshooting.

## 🚀 Step-by-Step Deployment

### 1. Prerequisites

- GitHub account with this repository forked
- Railway account (sign up at [railway.app](https://railway.app))
- Link Railway to your GitHub account

### 2. Deploy to Railway

1. **Visit Railway Dashboard**
   - Go to [railway.app/dashboard](https://railway.app/dashboard)
   - Click "New Project"

2. **Deploy from GitHub**
   - Select "Deploy from GitHub repo"
   - Choose `near-kotlin-jsonrpc-client`
   - Railway will automatically detect the configuration

3. **Wait for Build**
   - Railway will:
     - Detect `nixpacks.toml` configuration
     - Run the build command: `bash scripts/fix_near_types.sh && ./gradlew :demo-app:shadowJar`
     - Create the JAR file
     - Start the application
   - Build time: ~3-5 minutes

4. **Get Your URL**
   - Once deployed, Railway provides a public URL
   - Click "Settings" → "Generate Domain"
   - Your app will be at: `https://your-app.up.railway.app`

## ⚙️ Configuration

Railway automatically detects:
- ✅ **Build**: Uses `nixpacks.toml`
- ✅ **Start Command**: `java -Xmx512m -jar demo-app/build/libs/near-demo.jar`
- ✅ **Port**: Automatically set via `PORT` env variable
- ✅ **Health Check**: `/health` endpoint

### Environment Variables

Railway automatically sets:
- `PORT` - Set by Railway (usually 8080)
- `RAILWAY_ENVIRONMENT` - Production/staging identifier

No additional configuration needed!

## 🐛 Common Issues

### Issue 1: "Unable to access jarfile"

**Symptom**: Error during start: `Unable to access jarfile demo-app/build/libs/near-demo.jar`

**Cause**: Build didn't complete successfully

**Solution**:
1. Check build logs in Railway dashboard
2. Look for Python 3 or JDK 17 issues
3. The `nixpacks.toml` should handle this, but you can manually add:
   - Settings → Variables → Add `NIXPACKS_PYTHON_VERSION=3.11`
   - Redeploy

### Issue 2: Build Timeout

**Symptom**: Build exceeds time limit

**Solution**:
1. Railway free tier has 5-minute build limit
2. Our build takes 3-5 minutes, should be fine
3. If timeout occurs:
   - Try redeploying
   - Or upgrade to Hobby plan ($5/month)

### Issue 3: Memory Issues

**Symptom**: `OutOfMemoryError` or crashes

**Solution**:
1. Free tier has 512MB RAM
2. We set `Xmx512m` to stay within limits
3. If issues persist:
   - Settings → Variables → Add `JAVA_OPTS=-Xmx400m -Xms200m`
   - Redeploy

### Issue 4: Slow First Request

**Symptom**: First request takes 10-30 seconds

**Cause**: Free tier apps sleep after inactivity

**Solution**:
- This is normal for free tier
- Subsequent requests are fast
- For always-on: upgrade to Hobby plan

## 📊 Railway Dashboard Features

### Logs
- Click "View Logs" to see real-time application logs
- Useful for debugging RPC calls
- Shows all HTTP requests

### Metrics
- CPU usage
- Memory usage
- Network traffic
- Request count

### Variables
- Manage environment variables
- Add secrets (future use)
- Railway automatically restarts on changes

## 💰 Pricing

**Free Tier** (Trial):
- $5 free credit
- Good for testing
- Apps sleep after inactivity
- 500 hours/month

**Hobby Tier** ($5/month):
- Always-on
- More resources
- Better performance
- Recommended for production

## 🔧 Advanced Configuration

### Custom Domain

1. Settings → Domains
2. Add custom domain
3. Configure DNS (CNAME record)
4. Railway handles SSL automatically

### Monitoring

1. Add health check monitoring:
   - Use UptimeRobot (free)
   - Monitor: `https://your-app.up.railway.app/health`
   - Get alerts if app goes down

### Scaling

Railway autoscales within plan limits:
- CPU: Scales automatically
- Memory: Set via JAVA_OPTS
- Instances: Single instance on free tier

## ✅ Verify Deployment

After deployment, test:

```bash
# Replace with your Railway URL
export APP_URL="https://your-app.up.railway.app"

# Test health
curl $APP_URL/health

# Test RPC
curl -X POST $APP_URL/api/rpc/testnet \
  -H "Content-Type: application/json" \
  -d '{"method":"status","params":null}'
```

Expected responses:
- Health: `{"status":"ok","library":"near-kotlin-jsonrpc-client"}`
- RPC: JSON with NEAR network status

## 🎉 Success!

Once deployed:
1. ✅ Update README.md with your Railway URL
2. ✅ Share in NEAR community channels
3. ✅ Include in bounty submission

## 📞 Support

- **Railway Docs**: [docs.railway.app](https://docs.railway.app)
- **Railway Discord**: [discord.gg/railway](https://discord.gg/railway)
- **This Project**: Open GitHub issue

## 🚀 Next Steps

- Add custom domain
- Set up monitoring
- Share with NEAR community
- Consider upgrading for always-on
