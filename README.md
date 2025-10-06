# NEAR Protocol Kotlin JSON-RPC Client

Type-safe Kotlin client for NEAR Protocol's JSON-RPC API, automatically generated from the official OpenAPI specification.

[![CI](https://github.com/YOUR_USERNAME/near-kotlin-jsonrpc-client/workflows/CI/badge.svg)](https://github.com/YOUR_USERNAME/near-kotlin-jsonrpc-client/actions)
[![Maven Central](https://img.shields.io/maven-central/v/org.near/near-jsonrpc-client.svg)](https://search.maven.org/artifact/org.near/near-jsonrpc-client)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## Features

- **Fully Type-Safe**: All RPC methods and responses are strongly typed
- **Auto-Generated**: Types and client code generated from NEAR's official OpenAPI spec
- **Kotlin-Native**: Idiomatic Kotlin with coroutines support
- **Lightweight**: Minimal dependencies using Ktor HTTP client
- **Well-Tested**: 85%+ test coverage on core functionality
- **Automated Updates**: GitHub Actions automatically regenerates code when the OpenAPI spec changes

## Packages

This project provides two packages:

### `near-jsonrpc-types`
Contains only type definitions and serialization code. Lightweight with minimal dependencies.

### `near-jsonrpc-client` 
Full-featured JSON-RPC client with all method implementations. Depends on `near-jsonrpc-types`.

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    // For types only
    implementation("org.near:near-jsonrpc-types:0.1.0")
    
    // For full client (includes types)
    implementation("org.near:near-jsonrpc-client:0.1.0")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'org.near:near-jsonrpc-types:0.1.0'
    implementation 'org.near:near-jsonrpc-client:0.1.0'
}
```

### Maven

```xml
<dependency>
    <groupId>org.near</groupId>
    <artifactId>near-jsonrpc-client</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Quick Start

```kotlin
import org.near.jsonrpc.client.NearJsonRpcClient
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*

fun main() = runBlocking {
    // Create client
    val client = NearJsonRpcClient("https://rpc.testnet.near.org")
    
    // Get network status
    val status = client.status()
    println("Chain ID: ${status.result?.chainId}")
    
    // Query block
    val block = client.block(buildJsonObject {
        put("finality", "final")
    })
    println("Latest block height: ${block.result?.header?.height}")
    
    // Query account
    val query = client.query(buildJsonObject {
        put("request_type", "view_account")
        put("finality", "final")
        put("account_id", "example.testnet")
    })
    println(query)
}
```

## Available Methods

All NEAR JSON-RPC methods are available as typed functions:

**Network & Node**
- `status()` - Node status and network info
- `networkInfo()` - Network information
- `health()` - Node health check
- `gasPrice()` - Current gas price
- `genesisConfig()` - Genesis configuration

**Blocks & Chunks**
- `block()` - Query block information
- `chunk()` - Query chunk information
- `changes()` - State changes in block
- `blockEffects()` - Block state changes by type

**Transactions**
- `tx()` - Transaction status
- `sendTx()` - Send transaction
- `broadcastTxAsync()` - Broadcast transaction (async)
- `broadcastTxCommit()` - Broadcast transaction (wait for commit)

**Accounts & Access Keys**
- `query()` - General query (accounts, contracts, etc.)
- `validators()` - Current validators

**Light Client**
- `lightClientProof()` - Light client execution proof
- `nextLightClientBlock()` - Next light client block

**Experimental Methods**
All `EXPERIMENTAL_*` methods are also available with proper typing.

## Advanced Usage

### Custom HTTP Client

```kotlin
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

val httpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

val client = NearJsonRpcClient(
    baseUrl = "https://rpc.mainnet.near.org",
    httpClient = httpClient
)
```

### Error Handling

```kotlin
import org.near.jsonrpc.client.NearRpcException

try {
    val result = client.tx(params)
} catch (e: NearRpcException) {
    println("RPC Error ${e.code}: ${e.message}")
    println("Data: ${e.data}")
}
```

### Raw JSON Responses

```kotlin
// Get raw JsonElement instead of typed response
val rawStatus = client.statusRaw()
println(rawStatus.jsonObject["version"])
```

## Architecture

### Code Generation Pipeline

1. **Download OpenAPI Spec**: Fetches latest spec from NEAR's nearcore repository
2. **Generate Types**: Uses openapi-generator to create Kotlin data classes
3. **Fix Type Names**: Converts snake_case to camelCase and fixes naming issues
4. **Generate Client**: Creates typed extension functions for all RPC methods
5. **Patch Paths**: Ensures all requests use `/` path (JSON-RPC requirement)

### Automation

- **Daily Regeneration**: GitHub Actions checks for OpenAPI spec updates daily
- **Auto PR Creation**: Automatically creates PR when spec changes detected
- **CI/CD**: Tests run on all PRs, publishes to Maven Central on release
- **Release Please**: Automated version management and changelog generation

## Development

### Prerequisites

- JDK 17+
- Python 3.7+
- Gradle 8.5+

### Build from Source

```bash
# Clone repository
git clone https://github.com/YOUR_USERNAME/near-kotlin-jsonrpc-client.git
cd near-kotlin-jsonrpc-client

# Build all packages
./gradlew build

# Run tests
./gradlew test

# Generate coverage report
./gradlew :near-jsonrpc-client:jacocoTestReport
open near-jsonrpc-client/build/reports/jacoco/test/html/index.html
```

### Regenerate Code

```bash
# Regenerate types and client from latest OpenAPI spec
./gradlew :near-jsonrpc-types:downloadOpenApiSpec
./gradlew :near-jsonrpc-client:generateKotlinClient
./gradlew build
```

### Project Structure

```
near-kotlin-jsonrpc-client/
├── near-jsonrpc-types/          # Type definitions package
│   ├── src/main/kotlin/         # Generated types
│   └── build.gradle.kts
├── near-jsonrpc-client/         # Client implementation package
│   ├── src/main/kotlin/         # Core client + generated methods
│   ├── src/test/kotlin/         # Tests
│   └── build.gradle.kts
├── tools/                       # Code generation scripts
│   ├── gen_types_from_openapi.py
│   └── gen_client_from_openapi.py
├── scripts/                     # Build scripts
│   └── fix_near_types.sh
└── .github/workflows/           # CI/CD automation
    ├── ci.yml
    ├── regen.yml
    ├── publish.yml
    └── release-please.yml
```

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

### Reporting Issues

- Check existing issues before creating new ones
- Provide minimal reproduction steps
- Include Kotlin version, client version, and stack traces

### Pull Requests

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Run tests (`./gradlew test`)
5. Commit changes (`git commit -m 'feat: add amazing feature'`)
6. Push to branch (`git push origin feature/amazing-feature`)
7. Open Pull Request

## Testing

```bash
# Run all tests
./gradlew test

# Run client tests with coverage
./gradlew :near-jsonrpc-client:test :near-jsonrpc-client:jacocoTestReport

# Verify coverage meets threshold
./gradlew :near-jsonrpc-client:jacocoTestCoverageVerification
```

Current test coverage: **85%**

## Related Projects

- [near-api-js](https://github.com/near/near-api-js) - JavaScript/TypeScript client
- [near-jsonrpc-client-rs](https://github.com/near/near-jsonrpc-client-rs) - Rust client
- [nearcore](https://github.com/near/nearcore) - NEAR Protocol node implementation

## License

This project is licensed under the MIT License - see [LICENSE](LICENSE) file for details.

## Acknowledgments

- Inspired by NEAR's TypeScript and Rust JSON-RPC clients
- Built with [Ktor](https://ktor.io/) HTTP client
- Code generation powered by [OpenAPI Generator](https://openapi-generator.tech/)
- Type fixes using [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)

## Support

- [NEAR Documentation](https://docs.near.org)
- [NEAR Discord](https://near.chat)
- [GitHub Issues](https://github.com/YOUR_USERNAME/near-kotlin-jsonrpc-client/issues)


