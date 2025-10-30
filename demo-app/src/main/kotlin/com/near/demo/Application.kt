package com.near.demo

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.near.jsonrpc.client.*
import org.slf4j.event.Level

@Serializable
data class RpcRequest(
    val method: String,
    val params: JsonElement? = null
)

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String
)

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080

    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        install(CallLogging) {
            level = Level.INFO
        }

        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }

        install(CORS) {
            anyHost()
            allowHeader(HttpHeaders.ContentType)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Options)
        }

        install(StatusPages) {
            exception<NearRpcException> { call, cause ->
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        error = "RPC_ERROR",
                        message = "Code ${cause.code}: ${cause.message}"
                    )
                )
            }
            exception<Throwable> { call, cause ->
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse(
                        error = "INTERNAL_ERROR",
                        message = cause.message ?: "Unknown error"
                    )
                )
            }
        }

        routing {
            configureRoutes()
        }
    }.start(wait = true)
}

fun Routing.configureRoutes() {
    // Serve static frontend
    get("/") {
        call.respondText(getIndexHtml(), ContentType.Text.Html)
    }

    // Health check
    get("/health") {
        call.respond(mapOf("status" to "ok", "library" to "near-kotlin-jsonrpc-client"))
    }

    // RPC endpoints for both testnet and mainnet
    post("/api/rpc/{network}") {
        val network = call.parameters["network"] ?: "testnet"
        val rpcUrl = when (network) {
            "mainnet" -> "https://rpc.mainnet.near.org"
            "testnet" -> "https://rpc.testnet.near.org"
            else -> {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_NETWORK", "Network must be mainnet or testnet"))
                return@post
            }
        }

        val request = call.receive<RpcRequest>()
        val client = NearJsonRpcClient(rpcUrl)

        val result = client.callRaw(request.method, request.params)

        call.respond(result)
    }

    // List available methods
    get("/api/methods") {
        call.respond(mapOf(
            "methods" to listOf(
                mapOf("name" to "status", "description" to "Get node status and network info"),
                mapOf("name" to "network_info", "description" to "Get network information"),
                mapOf("name" to "gas_price", "description" to "Get current gas price"),
                mapOf("name" to "block", "description" to "Query block information"),
                mapOf("name" to "chunk", "description" to "Query chunk information"),
                mapOf("name" to "tx", "description" to "Get transaction status"),
                mapOf("name" to "query", "description" to "Query accounts, contracts, etc."),
                mapOf("name" to "validators", "description" to "Get current validators"),
                mapOf("name" to "health", "description" to "Node health check"),
                mapOf("name" to "genesis_config", "description" to "Get genesis configuration"),
            )
        ))
    }
}

fun getIndexHtml() = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>NEAR Kotlin JSON-RPC Client Demo</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            padding: 20px;
        }
        .container {
            max-width: 1200px;
            margin: 0 auto;
            background: white;
            border-radius: 16px;
            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
            overflow: hidden;
        }
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 40px;
            text-align: center;
        }
        .header h1 { font-size: 2.5em; margin-bottom: 10px; }
        .header p { font-size: 1.1em; opacity: 0.9; }
        .content { padding: 40px; }
        .section { margin-bottom: 30px; }
        .section h2 { color: #667eea; margin-bottom: 15px; font-size: 1.5em; }

        .network-selector {
            display: flex;
            gap: 10px;
            margin-bottom: 20px;
        }
        .network-btn {
            flex: 1;
            padding: 12px;
            border: 2px solid #667eea;
            background: white;
            color: #667eea;
            border-radius: 8px;
            cursor: pointer;
            font-size: 1em;
            font-weight: 600;
            transition: all 0.3s;
        }
        .network-btn.active {
            background: #667eea;
            color: white;
        }

        .method-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
            gap: 15px;
            margin-bottom: 30px;
        }
        .method-btn {
            padding: 15px;
            background: #f7fafc;
            border: 2px solid #e2e8f0;
            border-radius: 8px;
            cursor: pointer;
            transition: all 0.3s;
            text-align: center;
            font-weight: 500;
        }
        .method-btn:hover {
            background: #667eea;
            color: white;
            border-color: #667eea;
            transform: translateY(-2px);
            box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
        }

        .params-section {
            margin: 20px 0;
        }
        textarea {
            width: 100%;
            min-height: 100px;
            padding: 15px;
            border: 2px solid #e2e8f0;
            border-radius: 8px;
            font-family: 'Courier New', monospace;
            font-size: 14px;
            resize: vertical;
        }

        .execute-btn {
            width: 100%;
            padding: 15px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border: none;
            border-radius: 8px;
            font-size: 1.1em;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.3s;
        }
        .execute-btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 20px rgba(102, 126, 234, 0.4);
        }
        .execute-btn:disabled {
            background: #cbd5e0;
            cursor: not-allowed;
            transform: none;
        }

        .result-box {
            margin-top: 20px;
            padding: 20px;
            background: #1a202c;
            color: #68d391;
            border-radius: 8px;
            font-family: 'Courier New', monospace;
            font-size: 13px;
            max-height: 500px;
            overflow: auto;
            white-space: pre-wrap;
            word-wrap: break-word;
        }
        .error { color: #fc8181; }
        .loading {
            text-align: center;
            padding: 20px;
            color: #667eea;
        }

        .example-params {
            background: #edf2f7;
            padding: 10px;
            border-radius: 6px;
            margin-top: 10px;
            font-size: 0.9em;
        }
        .example-params strong { color: #667eea; }

        .footer {
            text-align: center;
            padding: 30px;
            background: #f7fafc;
            color: #718096;
        }
        .footer a {
            color: #667eea;
            text-decoration: none;
            font-weight: 600;
        }
        .footer a:hover { text-decoration: underline; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🚀 NEAR Kotlin JSON-RPC Client</h1>
            <p>Interactive Demo Playground</p>
            <p style="font-size: 0.9em; margin-top: 10px;">Type-safe Kotlin client for NEAR Protocol</p>
        </div>

        <div class="content">
            <div class="section">
                <h2>1. Select Network</h2>
                <div class="network-selector">
                    <button class="network-btn active" data-network="testnet">Testnet</button>
                    <button class="network-btn" data-network="mainnet">Mainnet</button>
                </div>
            </div>

            <div class="section">
                <h2>2. Choose RPC Method</h2>
                <div class="method-grid">
                    <div class="method-btn" data-method="status">status</div>
                    <div class="method-btn" data-method="network_info">network_info</div>
                    <div class="method-btn" data-method="gas_price">gas_price</div>
                    <div class="method-btn" data-method="block">block</div>
                    <div class="method-btn" data-method="validators">validators</div>
                    <div class="method-btn" data-method="genesis_config">genesis_config</div>
                </div>
            </div>

            <div class="section params-section">
                <h2>3. Parameters (JSON)</h2>
                <textarea id="params" placeholder='{"finality": "final"}'></textarea>
                <div class="example-params" id="example-params"></div>
            </div>

            <div class="section">
                <button class="execute-btn" id="execute">Execute Request</button>
            </div>

            <div class="section" id="result-section" style="display: none;">
                <h2>Response</h2>
                <div class="result-box" id="result"></div>
            </div>
        </div>

        <div class="footer">
            <p>Built with <strong>near-kotlin-jsonrpc-client</strong></p>
            <p style="margin-top: 10px;">
                <a href="https://github.com/0xjesus/near-kotlin-jsonrpc-client" target="_blank">View on GitHub</a>
            </p>
        </div>
    </div>

    <script>
        let selectedNetwork = 'testnet';
        let selectedMethod = null;

        const examples = {
            'status': null,
            'network_info': null,
            'gas_price': null,
            'genesis_config': null,
            'validators': null,
            'block': '{"finality": "final"}',
        };

        // Network selection
        document.querySelectorAll('.network-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                document.querySelectorAll('.network-btn').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                selectedNetwork = btn.dataset.network;
            });
        });

        // Method selection
        document.querySelectorAll('.method-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                selectedMethod = btn.dataset.method;
                const example = examples[selectedMethod];
                if (example) {
                    document.getElementById('params').value = example;
                    document.getElementById('example-params').innerHTML =
                        '<strong>Example:</strong> ' + example;
                } else {
                    document.getElementById('params').value = '';
                    document.getElementById('example-params').innerHTML =
                        '<strong>Note:</strong> This method requires no parameters';
                }
                btn.style.background = '#667eea';
                btn.style.color = 'white';
                setTimeout(() => {
                    btn.style.background = '';
                    btn.style.color = '';
                }, 200);
            });
        });

        // Execute request
        document.getElementById('execute').addEventListener('click', async () => {
            if (!selectedMethod) {
                alert('Please select a method first');
                return;
            }

            const paramsText = document.getElementById('params').value.trim();
            let params = null;

            if (paramsText) {
                try {
                    params = JSON.parse(paramsText);
                } catch (e) {
                    alert('Invalid JSON in parameters: ' + e.message);
                    return;
                }
            }

            const resultSection = document.getElementById('result-section');
            const resultBox = document.getElementById('result');
            const executeBtn = document.getElementById('execute');

            resultSection.style.display = 'block';
            resultBox.className = 'result-box loading';
            resultBox.textContent = 'Loading...';
            executeBtn.disabled = true;

            try {
                const response = await fetch(`/api/rpc/${'$'}{selectedNetwork}`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        method: selectedMethod,
                        params: params
                    })
                });

                const data = await response.json();
                resultBox.className = 'result-box';
                resultBox.textContent = JSON.stringify(data, null, 2);
            } catch (e) {
                resultBox.className = 'result-box error';
                resultBox.textContent = 'Error: ' + e.message;
            } finally {
                executeBtn.disabled = false;
            }
        });
    </script>
</body>
</html>
""".trimIndent()
