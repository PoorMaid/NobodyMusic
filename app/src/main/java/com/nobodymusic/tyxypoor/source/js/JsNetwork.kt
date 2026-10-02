package com.nobodymusic.tyxypoor.source.js

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

interface JsNetwork {
    fun request(url: String, optionsJson: String, callback: (String, Int, String?) -> Unit)
}

class OkHttpJsNetwork(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) : JsNetwork {

    override fun request(url: String, optionsJson: String, callback: (String, Int, String?) -> Unit) {
        Thread {
            try {
                val opt = if (optionsJson.isBlank()) JSONObject() else JSONObject(optionsJson)
                val method = opt.optString("method", "GET").uppercase()
                val headersObj = opt.optJSONObject("headers")
                val builder = Request.Builder().url(url)
                headersObj?.keys()?.forEach { k ->
                    builder.header(k, headersObj.optString(k))
                }
                builder.header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                when (method) {
                    "POST" -> {
                        val bodyStr = when {
                            opt.has("body") -> opt.optString("body")
                            opt.has("form") -> opt.optJSONObject("form")?.let { f ->
                                f.keys().asSequence().joinToString("&") { k ->
                                    "$k=${java.net.URLEncoder.encode(f.optString(k), "UTF-8")}"
                                }
                            } ?: ""
                            else -> ""
                        }
                        builder.post(bodyStr.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                    }
                    else -> builder.get()
                }
                client.newCall(builder.build()).execute().use { resp ->
                    callback(resp.body?.string() ?: "", resp.code, null)
                }
            } catch (t: Throwable) {
                callback("", 0, t.message ?: "network error")
            }
        }.start()
    }
}