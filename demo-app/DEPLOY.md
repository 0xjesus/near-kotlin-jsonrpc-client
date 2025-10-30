# Deployment Guide - NEAR Kotlin JSON-RPC Demo

Quick guide to deploy the demo application to various platforms.

## 🚀 Quick Deploy Options

### Option 1: Railway (Recommended)

1. **Fork this repository**
2. **Visit [Railway](https://railway.app)**
3. **Click "New Project" → "Deploy from GitHub repo"**
4. **Select your forked repository**
5. **Railway will automatically detect the configuration and deploy**

That's it! Your app will be live in ~2 minutes at `https://your-app.railway.app`

### Option 2: Render

1. **Fork this repository**
2. **Visit [Render](https://render.com)**
3. **Click "New" → "Web Service"**
4. **Connect your GitHub repository**
5. **Configure:**
   - **Build Command**: `./gradlew :demo-app:shadowJar`
   - **Start Command**: `java -jar demo-app/build/libs/near-demo.jar`
   - **Environment**: Select Docker
6. **Click "Create Web Service"**

Your app will be live at `https://your-app.onrender.com`

### Option 3: Fly.io

```bash
# Install flyctl
curl -L https://fly.io/install.sh | sh

# Navigate to project
cd near-kotlin-jsonrpc-client

# Launch app (will create fly.toml)
fly launch --name near-kotlin-demo

# Deploy
fly deploy
```

Your app will be live at `https://near-kotlin-demo.fly.dev`

### Option 4: Docker on Any Platform

```bash
# Build image
docker build -t near-kotlin-demo .

# Run locally
docker run -p 8080:8080 near-kotlin-demo

# Push to registry
docker tag near-kotlin-demo your-registry/near-kotlin-demo
docker push your-registry/near-kotlin-demo
```

Deploy the Docker image to:
- AWS ECS
- Google Cloud Run
- Azure Container Instances
- DigitalOcean App Platform
- Any Kubernetes cluster

## 🔧 Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | `8080` | Port the server listens on |

## 📊 Resource Requirements

- **RAM**: 512 MB minimum
- **CPU**: 0.5 vCPU minimum
- **Disk**: 200 MB
- **Network**: Requires outbound HTTPS to NEAR RPC nodes

## ✅ Health Check

All platforms should use:
- **Path**: `/health`
- **Expected Response**: `{"status":"ok"}`
- **Port**: Same as `PORT` env var

## 🔥 Free Tier Recommendations

| Platform | Free Tier | Best For |
|----------|-----------|----------|
| **Railway** | 500 hours/month | Easiest setup |
| **Render** | 750 hours/month | Most generous free tier |
| **Fly.io** | 3 VMs free | Best performance |
| **Heroku** | Deprecated | Not recommended |

## 🛠️ Build Configuration

### Railway
Uses `railway.json` (already configured):
```json
{
  "build": {
    "buildCommand": "./gradlew :demo-app:shadowJar"
  },
  "deploy": {
    "startCommand": "java -jar demo-app/build/libs/near-demo.jar"
  }
}
```

### Render
Uses `render.yaml` (already configured)

### Docker
Uses `Dockerfile` (already configured)

### Procfile
For Heroku-compatible platforms:
```
web: java -jar demo-app/build/libs/near-demo.jar
```

## 🐛 Troubleshooting

### Build Fails

**Problem**: Gradle build fails
**Solution**: Ensure JDK 17+ is available in build environment

**Problem**: Out of memory during build
**Solution**: Add `GRADLE_OPTS=-Xmx2048m` environment variable

### Runtime Fails

**Problem**: App crashes on startup
**Solution**: Check logs for port binding issues, ensure PORT env var is set

**Problem**: RPC calls timeout
**Solution**: Ensure outbound HTTPS is allowed in firewall rules

### Performance Issues

**Problem**: Slow response times
**Solution**:
- Increase RAM allocation to 1GB
- Use a region closer to NEAR RPC nodes (us-west or eu-west)

## 📝 Post-Deployment

After deployment:

1. **Test the health endpoint**: `curl https://your-app.com/health`
2. **Test an RPC call**:
   ```bash
   curl -X POST https://your-app.com/api/rpc/testnet \
     -H "Content-Type: application/json" \
     -d '{"method":"status","params":null}'
   ```
3. **Visit the web UI**: Open `https://your-app.com` in your browser

## 🔗 Updating Your Deployment

Most platforms auto-deploy on git push:

```bash
git add .
git commit -m "Update demo app"
git push origin main
```

Platform will automatically rebuild and redeploy.

## 💡 Tips

- **Custom Domain**: All platforms support custom domains
- **HTTPS**: Automatically provided by all platforms
- **Logs**: Check platform dashboard for application logs
- **Monitoring**: Set up uptime monitoring with UptimeRobot or similar
- **Scaling**: Free tiers auto-sleep after inactivity, upgrade for always-on

## 📞 Support

For deployment issues:
- Check platform-specific documentation
- Open an issue on GitHub
- Join NEAR Developer Discord

## 🎉 Share Your Deployment

Once deployed, add your demo URL to the main README.md by opening a PR!
