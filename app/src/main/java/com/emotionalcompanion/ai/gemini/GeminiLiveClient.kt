package com.emotionalcompanion.ai.gemini

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/** Low-level connection events. Nothing here ever contains the API key. */
sealed interface LiveSignal {
    data object Open : LiveSignal
    data class Message(val text: String) : LiveSignal
    data class Closed(val code: Int, val reason: String) : LiveSignal
    data class Failed(val failure: TransportFailure) : LiveSignal
}

sealed interface TransportFailure {
    data object NoInternet : TransportFailure
    data object Timeout : TransportFailure
    /** The WebSocket handshake got an HTTP error (e.g. 400, 403, 429). */
    data class Http(val code: Int) : TransportFailure
    data object Network : TransportFailure
}

class LiveConnection internal constructor(private val socket: WebSocket) {
    fun send(text: String): Boolean = socket.send(text)
    fun close() {
        socket.close(1000, null)
    }
    fun cancel() = socket.cancel()
}

/** OkHttp WebSocket wrapper for the Gemini Live endpoint. No logging interceptor, by design. */
class GeminiLiveClient(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // streaming: no read timeout
        .pingInterval(20, TimeUnit.SECONDS)
        .build(),
) {

    fun connect(apiKey: String, onSignal: (LiveSignal) -> Unit): LiveConnection {
        val url = LiveProtocol.ENDPOINT + "?key=" + URLEncoder.encode(apiKey, "UTF-8")
        val request = Request.Builder().url(url).build()
        val socket = http.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) = onSignal(LiveSignal.Open)

                override fun onMessage(webSocket: WebSocket, text: String) = onSignal(LiveSignal.Message(text))

                // The server may send JSON in binary frames.
                override fun onMessage(webSocket: WebSocket, bytes: ByteString) =
                    onSignal(LiveSignal.Message(bytes.utf8()))

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(1000, null)
                    onSignal(LiveSignal.Closed(code, reason))
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    val failure = when {
                        response != null -> TransportFailure.Http(response.code)
                        t is UnknownHostException -> TransportFailure.NoInternet
                        t is SocketTimeoutException -> TransportFailure.Timeout
                        t is IOException -> TransportFailure.Network
                        else -> TransportFailure.Network
                    }
                    response?.close()
                    onSignal(LiveSignal.Failed(failure))
                }
            },
        )
        return LiveConnection(socket)
    }
}
