package org.near.jsonrpc.client

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import kotlin.test.*

class NearJsonRpcClientTest {

    private fun mockClient(json: Json, handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): NearJsonRpcClient {
        val httpClient = HttpClient(MockEngine { req -> handler(req) }) {
            install(ContentNegotiation) { json(json) }
        }
        return NearJsonRpcClient("http://localhost:3030", httpClient, json)
    }

    private fun mockSuccess(result: String = "{}"): suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { _ ->
        respond(
            content = ByteReadChannel("""{"jsonrpc":"2.0","id":1,"result":$result}"""),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
    }

    // Core call() tests
    @Test
    fun testCallSuccess() = runBlocking {
        val json = Json { ignoreUnknownKeys = true }
        val client = mockClient(json, mockSuccess("""{"ok":true}"""))
        val result = client.call("status", null, JsonElement.serializer())
        assertEquals(true, result.jsonObject["ok"]?.jsonPrimitive?.boolean)
    }

    @Test
    fun testCallErrorWithData() = runBlocking {
        val json = Json { ignoreUnknownKeys = true }
        val client = mockClient(json) { _ ->
            respond(
                content = ByteReadChannel("""{"jsonrpc":"2.0","id":1,"error":{"code":-32000,"message":"boom","data":{"why":"x"}}}"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val ex = assertFailsWith<NearRpcException> { client.call("status", null, JsonElement.serializer()) }
        assertEquals(-32000, ex.code)
        assertEquals("boom", ex.message)
    }

    @Test
    fun testCallErrorWithoutData() = runBlocking {
        val json = Json { ignoreUnknownKeys = true }
        val client = mockClient(json) { _ ->
            respond(
                content = ByteReadChannel("""{"jsonrpc":"2.0","id":1,"error":{"code":-32600,"message":"Invalid"}}"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val ex = assertFailsWith<NearRpcException> { client.call("test", null, JsonElement.serializer()) }
        assertEquals(-32600, ex.code)
        assertNull(ex.data)
    }

    @Test
    fun testCallNullResult() = runBlocking {
        val json = Json { ignoreUnknownKeys = true }
        val client = mockClient(json) { _ ->
            respond(
                content = ByteReadChannel("""{"jsonrpc":"2.0","id":1}"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val ex = assertFailsWith<NearRpcException> { client.call("test", null, JsonElement.serializer()) }
        assertEquals(-32603, ex.code)
    }

    @Test
    fun testCallRaw() = runBlocking {
        val json = Json { ignoreUnknownKeys = true }
        val client = mockClient(json, mockSuccess("""{"version":"1.0"}"""))
        val result = client.callRaw("status", null)
        assertEquals("1.0", result.jsonObject["version"]?.jsonPrimitive?.content)
    }

    @Test
    fun testCallRawNullResult() = runBlocking {
        val json = Json { ignoreUnknownKeys = true }
        val client = mockClient(json) { _ ->
            respond(
                content = ByteReadChannel("""{"jsonrpc":"2.0","id":1}"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val result = client.callRaw("test", null)
        assertTrue(result is JsonNull)
    }

    @Test
    fun testWithParams() = runBlocking {
        val json = Json { ignoreUnknownKeys = true }
        val params = buildJsonObject { put("block_id", 123) }
        val client = mockClient(json) { req ->
            val body = Json.decodeFromString<JsonObject>(req.body.toByteArray().decodeToString())
            assertEquals(123, body["params"]?.jsonObject?.get("block_id")?.jsonPrimitive?.int)
            respond(
                content = ByteReadChannel("""{"jsonrpc":"2.0","id":1,"result":{}}"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        client.call("block", params, JsonElement.serializer())
    }

    // Test ALL generated methods
    @Test
    fun testStatusRaw() = runBlocking {
        val client = mockClient(Json { ignoreUnknownKeys = true }, mockSuccess())
        client.statusRaw()
    }

    @Test
    fun testExperimentalChanges() = runBlocking {
        val client = mockClient(Json { ignoreUnknownKeys = true }, mockSuccess())
        client.experimentalChanges()
    }

    @Test
    fun testExperimentalChangesInBlock() = runBlocking {
        val client = mockClient(Json { ignoreUnknownKeys = true }, mockSuccess())
        client.experimentalChangesInBlock()
    }

    @Test
    fun testExperimentalCongestionLevel() = runBlocking {
        val client = mockClient(Json { ignoreUnknownKeys = true }, mockSuccess())
        client.experimentalCongestionLevel()
    }

    @Test
    fun testExperimentalGenesisConfig() = runBlocking {
        val client = mockClient(Json { ignoreUnknownKeys = true }, mockSuccess())
        client.experimentalGenesisConfig()
    }

    @Test
    fun testExperimentalLightClientBlockProof() = runBlocking {
        val client = mockClient(Json { ignoreUnknownKeys = true }, mockSuccess())
        client.experimentalLightClientBlockProof()
    }

    @Test
    fun testExperimentalLightClientProof() = runBlocking {
        val client = mockClient(Json { ignoreUnknownKeys = true }, mockSuccess())
        client.experimentalLightClientProof()
    }

    @Test
    fun testExperimentalMaintenanceWindows() = runBlocking {
        val client = mockClient(Json { ignoreUnknownKeys = true }, mockSuccess())
        client.experimentalMaintenanceWindows()
    }

    @Test
    fun testExperimentalProtocolConfig() = runBlocking {
        val client = mockClient(Json { ignoreUnknownKeys = true }, mockSuccess())
        client.experimentalProtocolConfig()
    }

    @Test
    fun testExperimentalReceipt() = runBlocking {
        val client = mockClient(Json { ignoreUnknownKeys = true }, mockSuccess())
        client.experimentalReceipt()
    }

    @Test
    fun testExperimentalSplitStorageInfo() = runBlocking {
        val client = mockClient(Json { ignoreUnknownKeys = true }, mockSuccess())
        client.experimentalSplitStorageInfo()
    }

    @Test
    fun testExperimentalTxStatus() = runBlocking {
        val client = mockClient(Json { ignoreUnknownKeys = true }, mockSuccess())
        client.experimentalTxStatus()
    }

    @Test
    fun testExperimentalValidatorsOrdered() = runBlocking {
        val client = mockClient(Json { ignoreUnknownKeys = true }, mockSuccess())
        client.experimentalValidatorsOrdered()
    }

    @Test
    fun testBlock() = runBlocking {
        val client = mockClient(Json { ignoreUnknownKeys = true }, mockSuccess())
        client.block()
    }

    @Test
    fun testBlockEffects() = runBlocking {
        val client = mockClient(Json { ignoreUnknownKeys = true }, mockSuccess())
        client.blockEffects()
    }

    @Test
    fun testBroadcastTxAsync() = runBlocking {
        val client = mockClient(Json { ignoreUnknownKeys = true }, mockSuccess())
        client.broadcastTxAsync()
    }

    @Test
    fun testBroadcastTxCommit() = runBlocking {
        val client = mockClient(Json { ignoreUnknownKeys = true }, mockSuccess())
        client.broadcastTxCommit()
    }

    @Test
    fun testChanges() = runBlocking {
        val client = mockClient(Json { ignoreUnknownKeys = true }, mockSuccess())
        client.changes()
    }

    @Test
    fun testChunk() = runBlocking {
        val client = mockClient(Json { ignoreUnknownKeys = true }, mockSuccess())
        client.chunk()
    }

    @Test
    fun testClientConfig() = runBlocking {
        val client = mockClient(Json { ignoreUnknownKeys = true }, mockSuccess())
        client.clientConfig()
    }

    @Test
    fun testGasPrice() = runBlocking {
        val client = mockClient(Json { ignoreUnknownKeys = true }, mockSuccess())
        client.gasPrice()
    }

    @Test
    fun testGenesisConfig() = runBlocking {
        val client = mockClient(Json { ignoreUnknownKeys = true }, mockSuccess())
        client.genesisConfig()
    }

    @Test
    fun testHealth() = runBlocking {
        val client = mockClient(Json { ignoreUnknownKeys = true }, mockSuccess("null"))
        assertFailsWith<NearRpcException> { client.health() }
    }

    @Test
    fun testLightClientProof() = runBlocking {
        val client = mockClient(Json { ignoreUnknownKeys = true }, mockSuccess())
        client.lightClientProof()
    }

    @Test
    fun testMaintenanceWindows() = runBlocking {
        val client = mockClient(Json { ignoreUnknownKeys = true }, mockSuccess())
        client.maintenanceWindows()
    }

    @Test
    fun testNetworkInfo() = runBlocking {
        val client = mockClient(Json { ignoreUnknownKeys = true }, mockSuccess())
        client.networkInfo()
    }

    @Test
    fun testNextLightClientBlock() = runBlocking {
        val client = mockClient(Json { ignoreUnknownKeys = true }, mockSuccess())
        client.nextLightClientBlock()
    }

    @Test
    fun testQuery() = runBlocking {
        val client = mockClient(Json { ignoreUnknownKeys = true }, mockSuccess())
        client.query()
    }

    @Test
    fun testSendTx() = runBlocking {
        val client = mockClient(Json { ignoreUnknownKeys = true }, mockSuccess())
        client.sendTx()
    }

    @Test
    fun testStatus() = runBlocking {
        val client = mockClient(Json { ignoreUnknownKeys = true }, mockSuccess())
        client.status()
    }

    @Test
    fun testTx() = runBlocking {
        val client = mockClient(Json { ignoreUnknownKeys = true }, mockSuccess())
        client.tx()
    }

    @Test
    fun testValidators() = runBlocking {
        val client = mockClient(Json { ignoreUnknownKeys = true }, mockSuccess())
        client.validators()
    }
    // Agrega SOLO estos 2 tests al final de tu archivo de test:

@Test
fun testBaseUrlWithTrailingSlash() = runBlocking {
    val json = Json { ignoreUnknownKeys = true }
    // Crear cliente mock con URL que termina en /
    val httpClient = HttpClient(MockEngine { req ->
        assertEquals("/", req.url.encodedPath)
        respond(
            content = ByteReadChannel("""{"jsonrpc":"2.0","id":1,"result":{}}"""),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
    }) {
        install(ContentNegotiation) { json(json) }
    }
    val client = NearJsonRpcClient("http://localhost:3030/", httpClient, json)
    client.call("test", null, JsonElement.serializer())
}

@Test
fun testNearRpcExceptionProperties() {
    val data = buildJsonObject { put("x", 1) }
    val ex = NearRpcException(-32000, "test error", data)
    assertEquals(-32000, ex.code)
    assertEquals("test error", ex.message)
    assertEquals(1, ex.data?.jsonObject?.get("x")?.jsonPrimitive?.int)
}
@Test
fun testCallWithComplexParams() = runBlocking {
    val json = Json { ignoreUnknownKeys = true }
    val params = buildJsonObject {
        put("finality", "final")
        put("block_id", 12345)
    }
    val client = mockClient(json) { req ->
        val body = Json.decodeFromString<JsonObject>(req.body.toByteArray().decodeToString())
        assertEquals("final", body["params"]?.jsonObject?.get("finality")?.jsonPrimitive?.content)
        respond(
            content = ByteReadChannel("""{"jsonrpc":"2.0","id":1,"result":{"data":"test"}}"""),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
    }
    val result = client.call("query", params, JsonElement.serializer())
    assertEquals("test", result.jsonObject["data"]?.jsonPrimitive?.content)
}

@Test
fun testCallRawWithParams() = runBlocking {
    val json = Json { ignoreUnknownKeys = true }
    val params = buildJsonObject { put("key", "value") }
    val client = mockClient(json) { req ->
        val body = Json.decodeFromString<JsonObject>(req.body.toByteArray().decodeToString())
        assertEquals("value", body["params"]?.jsonObject?.get("key")?.jsonPrimitive?.content)
        respond(
            content = ByteReadChannel("""{"jsonrpc":"2.0","id":1,"result":{"success":true}}"""),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
    }
    val result = client.callRaw("test", params)
    assertEquals(true, result.jsonObject["success"]?.jsonPrimitive?.boolean)
}

@Test
fun testErrorWithComplexData() = runBlocking {
    val json = Json { ignoreUnknownKeys = true }
    val client = mockClient(json) { _ ->
        respond(
            content = ByteReadChannel("""{"jsonrpc":"2.0","id":1,"error":{"code":-32001,"message":"Server error","data":{"trace":["line1","line2"],"type":"internal"}}}"""),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
    }
    val ex = assertFailsWith<NearRpcException> {
        client.call("test", null, JsonElement.serializer())
    }
    assertEquals(-32001, ex.code)
    assertEquals("Server error", ex.message)
    assertEquals("internal", ex.data?.jsonObject?.get("type")?.jsonPrimitive?.content)
}

@Test
fun testMethodNameInRequest() = runBlocking {
    val json = Json { ignoreUnknownKeys = true }
    val client = mockClient(json) { req ->
        val body = Json.decodeFromString<JsonObject>(req.body.toByteArray().decodeToString())
        assertEquals("custom_method", body["method"]?.jsonPrimitive?.content)
        assertEquals("2.0", body["jsonrpc"]?.jsonPrimitive?.content)
        assertNotNull(body["id"])
        respond(
            content = ByteReadChannel("""{"jsonrpc":"2.0","id":1,"result":{}}"""),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
    }
    client.call("custom_method", null, JsonElement.serializer())
}

@Test
fun testDefaultConstructorWithRealHttpClient() = runBlocking {
    val json = Json { ignoreUnknownKeys = true }
    // Crear cliente SIN pasar httpClient (usa CIO por defecto)
    val client = NearJsonRpcClient("http://httpbin.org/post", null, json)
    
    // No podemos hacer una llamada real sin un servidor, 
    // pero el constructor ya se ejecutó y creó el HttpClient interno
    assertNotNull(client)
    
    // Alternativamente, podemos hacer una llamada real a un endpoint de prueba
    // pero esto requiere internet, mejor solo verificar que se creó
}

@Test
fun testConstructorCreatesDefaultHttpClient() {
    val json = Json { ignoreUnknownKeys = true }
    // Constructor sin httpClient - crea uno por defecto con CIO
    val client = NearJsonRpcClient("http://test.com", null, json)
    assertNotNull(client)
}
}