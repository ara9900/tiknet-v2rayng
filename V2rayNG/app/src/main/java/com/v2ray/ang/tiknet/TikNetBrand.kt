package com.v2ray.ang.tiknet

import android.content.Context
import com.google.gson.JsonParser

/** Reseller white-label build: assets/brand.json is written only by the whitelabel workflow. */
object TikNetBrand {
    @Volatile
    private var loaded = false

    @Volatile
    private var apiBase: String? = null

    fun apiBaseUrl(ctx: Context): String? {
        if (!loaded) {
            apiBase = runCatching {
                ctx.assets.open("brand.json").bufferedReader().use { reader ->
                    JsonParser.parseString(reader.readText()).asJsonObject.get("api_base_url")?.asString
                }
            }.getOrNull()?.trim()?.trimEnd('/')?.takeIf { it.startsWith("http") }
            loaded = true
        }
        return apiBase
    }

    fun isBranded(ctx: Context): Boolean = apiBaseUrl(ctx) != null
}
