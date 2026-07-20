package com.nsfs.aireply

import android.content.Context
import android.content.SharedPreferences

/**
 * 简单的配置持久化（SharedPreferences）。
 * 默认值已写死云端地址与 Token，用户一般无需改动。
 */
object Prefs {
    private const val NAME = "ai_reply_prefs"
    private const val KEY_URL = "server_url"
    private const val KEY_TOKEN = "token"
    private const val KEY_N = "n"

    private const val DEF_URL = "https://novelgrok.nsfs.cn"
    private const val DEF_TOKEN = "8104dbd8ac33919dcab4ea0f6070aeae6cb53af3"

    private fun sp(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getUrl(ctx: Context): String =
        sp(ctx).getString(KEY_URL, DEF_URL) ?: DEF_URL

    fun setUrl(ctx: Context, v: String) =
        sp(ctx).edit().putString(KEY_URL, v.ifBlank { DEF_URL }).apply()

    fun getToken(ctx: Context): String =
        sp(ctx).getString(KEY_TOKEN, DEF_TOKEN) ?: DEF_TOKEN

    fun setToken(ctx: Context, v: String) =
        sp(ctx).edit().putString(KEY_TOKEN, v).apply()

    fun getN(ctx: Context): Int = sp(ctx).getInt(KEY_N, 4).coerceIn(1, 6)

    fun setN(ctx: Context, v: Int) =
        sp(ctx).edit().putInt(KEY_N, v.coerceIn(1, 6)).apply()
}
