package com.nsfs.aireply

import android.os.Handler
import android.os.Looper
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 直连云端 /suggest，用系统网络栈发 HTTPS 请求，不依赖任何外部二进制（curl 等）。
 * 返回格式：每行 "风格|文本"（fmt=lines）。
 */
object ApiClient {

    /**
     * @param cb 主线程回调：(候选列表, 错误信息)。二者必有一为空。
     */
    fun fetch(
        context: String,
        serverUrl: String,
        token: String,
        n: Int,
        cb: (List<Pair<String, String>>?, String?) -> Unit
    ) {
        Thread {
            try {
                val base = serverUrl.trimEnd('/')
                val url = buildString {
                    append(base)
                    append("/suggest?fmt=lines")
                    append("&token=").append(URLEncoder.encode(token, "UTF-8"))
                    append("&n=").append(n)
                    append("&context=").append(URLEncoder.encode(context, "UTF-8"))
                }
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 15000
                conn.readTimeout = 30000

                val code = conn.responseCode
                if (code != 200) {
                    val err = try {
                        conn.errorStream?.bufferedReader()?.readText()
                    } catch (_: Exception) { null }
                    conn.disconnect()
                    Handler(Looper.getMainLooper()).post {
                        cb(null, "服务端错误 HTTP $code ${err ?: ""}".trim())
                    }
                    return@Thread
                }

                val body = conn.inputStream.bufferedReader().readText()
                conn.disconnect()

                val list = body.lineSequence()
                    .mapNotNull { line ->
                        val idx = line.indexOf('|')
                        if (idx <= 0) null
                        else Pair(line.substring(0, idx).trim(), line.substring(idx + 1).trim())
                    }
                    .filter { it.second.isNotEmpty() }
                    .toList()

                Handler(Looper.getMainLooper()).post { cb(list, null) }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    cb(null, "网络错误：${e.javaClass.simpleName} ${e.message ?: ""}".trim())
                }
            }
        }.start()
    }
}
