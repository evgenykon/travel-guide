package com.evgenykon.travelguide.auth

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.evgenykon.travelguide.network.ExchangeCodeRequest
import com.evgenykon.travelguide.network.OpenRouterApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URLEncoder

class OpenRouterOAuth(private val api: OpenRouterApi) {

    sealed interface AuthResult {
        data class Success(val key: String) : AuthResult
        data class Failure(val message: String) : AuthResult
    }

    suspend fun authorize(
        context: Context,
        appLabel: String = "Travel Guide"
    ): AuthResult = withContext(Dispatchers.IO) {
        val verifier = Pkce.generateVerifier()
        val challenge = Pkce.challenge(verifier)
        val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        try {
            server.soTimeout = CALLBACK_TIMEOUT_MS
            val callback = "http://127.0.0.1:${server.localPort}/callback"
            val authUrl = buildString {
                append("https://openrouter.ai/auth?")
                append("callback_url=").append(URLEncoder.encode(callback, "UTF-8"))
                append("&code_challenge=").append(challenge)
                append("&code_challenge_method=S256")
                append("&key_label=").append(URLEncoder.encode(appLabel, "UTF-8"))
            }
            withContext(Dispatchers.Main) { openBrowser(context, authUrl) }
            val code = acceptCode(server)
                ?: return@withContext AuthResult.Failure("Не удалось получить код авторизации")
            val response = api.exchangeCode(
                ExchangeCodeRequest(code = code, codeVerifier = verifier)
            )
            AuthResult.Success(response.key)
        } catch (e: Exception) {
            AuthResult.Failure(e.message ?: "Ошибка авторизации OpenRouter")
        } finally {
            runCatching { server.close() }
        }
    }

    private fun openBrowser(context: Context, url: String) {
        try {
            CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
        } catch (e: ActivityNotFoundException) {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    private fun acceptCode(server: ServerSocket): String? {
        server.accept().use { socket ->
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
            val requestLine = reader.readLine() ?: return null
            val path = requestLine.split(" ").getOrNull(1) ?: return null
            val code = Uri.parse("http://127.0.0.1$path").getQueryParameter("code")

            val body = """
                <!doctype html>
                <html>
                <head><meta charset="utf-8"><title>Travel Guide</title></head>
                <body style="font-family:sans-serif;text-align:center;padding-top:15%">
                <h2>Авторизация завершена</h2>
                <p>Можно закрыть эту вкладку и вернуться в приложение.</p>
                </body>
                </html>
            """.trimIndent()
            val bytes = body.toByteArray(Charsets.UTF_8)
            val response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n" + body
            socket.getOutputStream().write(response.toByteArray(Charsets.UTF_8))
            socket.getOutputStream().flush()
            return code
        }
    }

    private companion object {
        const val CALLBACK_TIMEOUT_MS = 180_000
    }
}
