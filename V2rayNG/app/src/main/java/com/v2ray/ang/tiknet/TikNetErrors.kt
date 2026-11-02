package com.v2ray.ang.tiknet

import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.io.IOException

object TikNetErrors {
    fun message(error: Throwable?, fallback: String = "خطا نامشخص"): String {
        if (error == null) return fallback

        val chain = generateSequence(error) { it.cause }.toList()
        chain.firstOrNull { it is TikNetApiException }?.let { api ->
            val ex = api as TikNetApiException
            return when {
                ex.forcesLogout && ex.code == "account_removed" ->
                    ex.message?.takeIf { it.isNotBlank() } ?: "حساب حذف شده است؛ دوباره ثبت‌نام یا وارد شوید"
                ex.forcesLogout ->
                    ex.message?.takeIf { it.isNotBlank() } ?: "نشست لغو شده؛ دوباره وارد شوید"
                ex.statusCode == 401 ->
                    "اتصال به حساب برقرار نشد؛ چند لحظه دیگر تلاش کنید"
                ex.statusCode == 403 -> "دسترسی مجاز نیست"
                else -> ex.message?.takeIf { it.isNotBlank() } ?: fallback
            }
        }

        chain.firstOrNull { it is UnknownHostException || it is SocketTimeoutException || it is IOException }?.let {
            return "اتصال به سرور ممکن نیست"
        }

        return error.message?.takeIf { it.isNotBlank() } ?: fallback
    }
}
