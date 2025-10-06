package org.near.jsonrpc.client

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
private data class RpcReq(val jsonrpc: String = "2.0", val id: Long, val method: String, val params: JsonElement? = null)
@Serializable
private data class RpcErr(val code: Int, val message: String, val data: JsonElement? = null)
@Serializable
private data class RpcRes<R>(val jsonrpc: String, val id: Long? = null, val result: R? = null, val error: RpcErr? = null)

class NearRpcException(val code: Int, override val message: String, val data: JsonElement? = null): RuntimeException(message)

class NearJsonRpcClient(
    private val baseUrl: String,
    httpClient: HttpClient? = null,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true; isLenient = true }
) {
    private val client = httpClient ?: HttpClient(CIO) { install(ContentNegotiation) { json(this@NearJsonRpcClient.json) } }

    suspend fun <R> call(method: String, params: JsonElement?, deserializer: KSerializer<R>): R {
        val req = RpcReq(id = System.currentTimeMillis(), method = method, params = params)
        val text = client.post(baseUrl.trimEnd('/')) {
            url { encodedPath = "/" }
            contentType(ContentType.Application.Json)
            setBody(req)
        }.bodyAsText()
        val decoded = json.decodeFromString(RpcRes.serializer(deserializer), text)
        if (decoded.error != null) throw NearRpcException(decoded.error.code, decoded.error.message, decoded.error.data)
        if (decoded.result == null) throw NearRpcException(-32603, "Empty result", null)
        return decoded.result
    }

    suspend fun callRaw(method: String, params: JsonElement?): JsonElement {
        val req = RpcReq(id = System.currentTimeMillis(), method = method, params = params)
        val text = client.post(baseUrl.trimEnd('/')) {
            url { encodedPath = "/" }
            contentType(ContentType.Application.Json)
            setBody(req)
        }.bodyAsText()
        val e = json.parseToJsonElement(text).jsonObject
        return e["result"] ?: JsonNull
    }
}
