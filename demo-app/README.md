# NEAR Kotlin JSON-RPC Client - Demo Application

Interactive web playground demonstrating the **near-kotlin-jsonrpc-client** library in action.

## Features

- 🌐 **Interactive Web UI**: Test all RPC methods from your browser
- 🔄 **Network Switching**: Toggle between Testnet and Mainnet
- 📝 **Live Examples**: Pre-filled parameter examples for each method
- ⚡ **Real-time Responses**: See actual NEAR RPC responses instantly
- 🎨 **Beautiful UI**: Modern, gradient-based design

## Available Methods

The demo supports the following NEAR RPC methods:

- `status` - Node status and network info
- `network_info` - Network information
- `gas_price` - Current gas price
- `block` - Query block information
- `validators` - Current validators
- `genesis_config` - Genesis configuration
- `tx` - Transaction status
- `query` - Query accounts, contracts, etc.

## Running Locally

### Prerequisites

- JDK 17+
- Gradle 8.5+
- Python 3.7+ (for code generation)

### Build and Run

#### Option 1: Using Build Script (Recommended)

```bash
# Build everything
./build-demo.sh

# Run the application
./start-demo.sh

# Or run directly
java -jar demo-app/build/libs/near-demo.jar
```

#### Option 2: Using Gradle Directly

```bash
# From repository root
bash scripts/fix_near_types.sh
./gradlew :demo-app:shadowJar

# Run the application
java -jar demo-app/build/libs/near-demo.jar

# Or run with Gradle (slower)
./gradlew :demo-app:run
```

The application will start on `http://localhost:8080`

### Quick Test

```bash
# Test health endpoint
curl http://localhost:8080/health

# Test RPC call
curl -X POST http://localhost:8080/api/rpc/testnet \
  -H "Content-Type: application/json" \
  -d '{"method":"status","params":null}'
```

## Deployment

### Railway

[![Deploy on Railway](https://railway.app/button.svg)](https://railway.app/new/template)

1. Fork this repository
2. Connect to Railway
3. Railway will automatically detect the `Procfile`
4. Deploy!

### Render

1. Create new Web Service
2. Connect your repository
3. Build command: `./gradlew :demo-app:shadowJar`
4. Start command: `java -jar demo-app/build/libs/near-demo.jar`

### Docker

```bash
# Build image
docker build -t near-kotlin-demo .

# Run container
docker run -p 8080:8080 near-kotlin-demo
```

### Fly.io

```bash
# Install flyctl
curl -L https://fly.io/install.sh | sh

# Launch app
fly launch

# Deploy
fly deploy
```

## API Endpoints

### POST `/api/rpc/{network}`

Execute NEAR RPC method.

**Parameters:**
- `network` - `testnet` or `mainnet`

**Body:**
```json
{
  "method": "status",
  "params": null
}
```

**Example:**
```bash
curl -X POST http://localhost:8080/api/rpc/testnet \
  -H "Content-Type: application/json" \
  -d '{"method": "status", "params": null}'
```

### GET `/api/methods`

List all available methods.

### GET `/health`

Health check endpoint.

## Environment Variables

- `PORT` - Server port (default: 8080)

## Architecture

```
┌─────────────────┐
│   Web Browser   │
│   (Frontend)    │
└────────┬────────┘
         │
         │ HTTP
         │
┌────────▼────────┐
│  Ktor Server    │
│  (Backend API)  │
└────────┬────────┘
         │
         │ Uses
         │
┌────────▼─────────────────┐
│ near-jsonrpc-client lib  │
│ (Your Library)           │
└────────┬─────────────────┘
         │
         │ JSON-RPC
         │
┌────────▼────────┐
│  NEAR Protocol  │
│   RPC Nodes     │
└─────────────────┘
```

## Technology Stack

- **Backend**: Ktor (Kotlin)
- **Frontend**: Vanilla HTML/CSS/JavaScript
- **HTTP Client**: Ktor Client (from library)
- **Serialization**: kotlinx.serialization
- **Deployment**: Railway/Render/Docker

## Screenshots

### Main Interface
![Demo Interface](screenshots/demo.png)

The UI includes:
- Network selector (Testnet/Mainnet)
- Method selection grid
- JSON parameter input
- Live response viewer

## Adding New Methods

To add support for new RPC methods:

1. Add the method case in `Application.kt`:
```kotlin
when (request.method) {
    // ... existing methods
    "your_method" -> client.yourMethod(request.params)
}
```

2. Add to the method list in `/api/methods` endpoint

3. Add button to frontend HTML

## License

MIT License - Same as the main library

## Support

For issues with the demo app, please open an issue on the main repository:
https://github.com/0xjesus/near-kotlin-jsonrpc-client/issues
