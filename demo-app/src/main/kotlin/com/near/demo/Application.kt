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

@Serializable
data class MethodInfo(
    val name: String,
    val desc: String,
    val params: String? = null
)

@Serializable
data class MethodsResponse(
    val categories: Map<String, List<MethodInfo>>,
    val total: Int
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
        call.respond(MethodsResponse(
            categories = mapOf(
                "Node & Network" to listOf(
                    MethodInfo("status", "Node status"),
                    MethodInfo("network_info", "Network info"),
                    MethodInfo("health", "Health check"),
                    MethodInfo("gas_price", "Current gas price", """[null]"""),
                    MethodInfo("client_config", "Client config"),
                ),
                "Blocks & Chunks" to listOf(
                    MethodInfo("block", "Block info", """{"finality":"final"}"""),
                    MethodInfo("chunk", "Chunk info", """{"chunk_id":"EBM2qg5cGr47EjMPtH88uvmXHDHqmWPzKaQadbWhdw22"}"""),
                    MethodInfo("changes", "State changes", """{"changes_type":"all_access_key_changes","account_ids":["test.near"],"block_id":17821130}"""),
                    MethodInfo("block_effects", "Block state changes", """{"block_id":17821130}"""),
                ),
                "Transactions" to listOf(
                    MethodInfo("tx", "TX status", """{"tx_hash":"6zgh2u9DqHHiXzdy9ouTP7oGky2T4nugqzqt9wJZwNFm","sender_account_id":"test.near"}"""),
                    MethodInfo("send_tx", "Send TX", """{"signed_tx_base64":"..."}"""),
                    MethodInfo("broadcast_tx_async", "Broadcast TX async", """{"signed_tx_base64":"..."}"""),
                    MethodInfo("broadcast_tx_commit", "Broadcast TX commit", """{"signed_tx_base64":"..."}"""),
                ),
                "Accounts & Query" to listOf(
                    MethodInfo("query", "Query state", """{"request_type":"view_account","finality":"final","account_id":"test.near"}"""),
                    MethodInfo("validators", "Validators", """[null]"""),
                ),
                "Light Client" to listOf(
                    MethodInfo("light_client_proof", "Light client proof", """{"type":"transaction","transaction_hash":"6zgh2u9DqHHiXzdy9ouTP7oGky2T4nugqzqt9wJZwNFm","sender_id":"test.near"}"""),
                    MethodInfo("next_light_client_block", "Next light client block", """{"last_block_hash":"4NfqDPZQJd2pffWNK2jVUrXfQxrQU6wyC7cWfTdPPpRj"}"""),
                    MethodInfo("maintenance_windows", "Maintenance windows", """{"account_id":"test.near"}"""),
                ),
                "Experimental" to listOf(
                    MethodInfo("EXPERIMENTAL_changes", "State changes (exp)", """{"changes_type":"all_access_key_changes","account_ids":["test.near"],"block_id":17821130}"""),
                    MethodInfo("EXPERIMENTAL_changes_in_block", "Changes in block (exp)", """{"block_id":17821130}"""),
                    MethodInfo("EXPERIMENTAL_genesis_config", "Genesis config (exp)"),
                    MethodInfo("EXPERIMENTAL_light_client_block_proof", "Light client block proof (exp)", """{"last_block_hash":"4NfqDPZQJd2pffWNK2jVUrXfQxrQU6wyC7cWfTdPPpRj"}"""),
                    MethodInfo("EXPERIMENTAL_light_client_proof", "Light client proof (exp)", """{"type":"transaction","transaction_hash":"...","sender_id":"test.near"}"""),
                    MethodInfo("EXPERIMENTAL_protocol_config", "Protocol config (exp)", """{"finality":"final"}"""),
                    MethodInfo("EXPERIMENTAL_validators_ordered", "Validators ordered (exp)", """{"block_id":17821130}"""),
                    MethodInfo("EXPERIMENTAL_tx_status", "TX status (exp)", """{"tx_hash":"6zgh2u9DqHHiXzdy9ouTP7oGky2T4nugqzqt9wJZwNFm","sender_account_id":"test.near"}"""),
                    MethodInfo("EXPERIMENTAL_receipt", "Receipt info (exp)", """{"receipt_id":"..."}"""),
                    MethodInfo("EXPERIMENTAL_congestion_level", "Congestion level (exp)"),
                    MethodInfo("EXPERIMENTAL_maintenance_windows", "Maintenance windows (exp)", """{"account_id":"test.near"}"""),
                    MethodInfo("EXPERIMENTAL_split_storage_info", "Split storage info (exp)"),
                ),
            ),
            total = 31
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
            grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
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
            word-wrap: break-word;
            overflow-wrap: break-word;
            hyphens: auto;
            font-size: 0.9em;
            line-height: 1.4;
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
            word-wrap: break-word;
            overflow-wrap: break-word;
            overflow-x: auto;
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
                <h2>2. Choose RPC Method <span style="color:#667eea; font-size:0.8em">(31 methods available)</span></h2>
                <div id="methods-container">Loading methods...</div>
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
        let methodExamples = {};

        // Load methods dynamically
        async function loadMethods() {
            try {
                const response = await fetch('/api/methods');
                const data = await response.json();

                const container = document.getElementById('methods-container');
                container.innerHTML = '';

                // Create sections for each category
                Object.entries(data.categories).forEach(([category, methods]) => {
                    const categorySection = document.createElement('div');
                    categorySection.style.marginBottom = '30px';

                    const categoryTitle = document.createElement('h3');
                    categoryTitle.textContent = category;
                    categoryTitle.style.color = '#667eea';
                    categoryTitle.style.fontSize = '1.2em';
                    categoryTitle.style.marginBottom = '10px';
                    categorySection.appendChild(categoryTitle);

                    const methodGrid = document.createElement('div');
                    methodGrid.className = 'method-grid';

                    methods.forEach(method => {
                        const btn = document.createElement('div');
                        btn.className = 'method-btn';
                        btn.dataset.method = method.name;
                        btn.textContent = method.name;
                        btn.title = method.desc;

                        // Store example params
                        if (method.params) {
                            methodExamples[method.name] = method.params;
                        }

                        btn.addEventListener('click', () => selectMethod(method.name));
                        methodGrid.appendChild(btn);
                    });

                    categorySection.appendChild(methodGrid);
                    container.appendChild(categorySection);
                });
            } catch (e) {
                console.error('Failed to load methods:', e);
            }
        }

        function selectMethod(method) {
            selectedMethod = method;
            const example = methodExamples[method];

            if (example) {
                document.getElementById('params').value = example;
                document.getElementById('example-params').innerHTML =
                    '<strong>Example:</strong> ' + example;
            } else {
                document.getElementById('params').value = '';
                document.getElementById('example-params').innerHTML =
                    '<strong>Note:</strong> This method requires no parameters';
            }

            // Visual feedback
            document.querySelectorAll('.method-btn').forEach(b => {
                b.style.background = '';
                b.style.color = '';
                b.style.borderColor = '';
            });
            event.target.style.background = '#667eea';
            event.target.style.color = 'white';
            event.target.style.borderColor = '#667eea';
        }

        // Network selection
        document.querySelectorAll('.network-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                document.querySelectorAll('.network-btn').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                selectedNetwork = btn.dataset.network;
            });
        });

        // Load methods on page load
        loadMethods();

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
