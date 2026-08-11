package com.v2ray.ang.tiknet

/**
 * Map core / English delay & ping messages to short Persian copy for the UI.
 */
object TikNetMessages {
    fun coreDelay(raw: String?): String {
        if (raw.isNullOrBlank()) return "—"
        val t = raw.trim()
        val lower = t.lowercase()

        // Success patterns: "... 123 ms" / "available, 123"
        Regex("""(\d+)\s*ms""", RegexOption.IGNORE_CASE).find(t)?.groupValues?.getOrNull(1)?.let { ms ->
            return "تأخیر: ${TikNetJalali.toPersianDigits(ms)} میلی‌ثانیه"
        }
        Regex("""(\d+)""").find(t)?.groupValues?.getOrNull(1)?.let { ms ->
            if ("available" in lower || "موفق" in t || "success" in lower) {
                return "تأخیر: ${TikNetJalali.toPersianDigits(ms)} میلی‌ثانیه"
            }
        }

        return when {
            "deadline" in lower || "timeout" in lower || "timed out" in lower ->
                "زمان پاسخ تمام شد — سرور جواب نداد"
            "connection test failed" in lower || "failed" in lower ->
                "تست اتصال ناموفق بود"
            "context canceled" in lower || "cancelled" in lower || "canceled" in lower ->
                "تست اتصال لغو شد"
            "unreachable" in lower || "no route" in lower ->
                "مسیر به سرور در دسترس نیست"
            "refused" in lower ->
                "اتصال از سمت سرور رد شد"
            "network is unreachable" in lower ->
                "شبکه در دسترس نیست"
            else -> {
                // Strip noisy English prefixes, keep short
                val cleaned = t
                    .replace(Regex("""(?i)connection test (failed|error)[:\s]*"""), "")
                    .replace(Regex("""(?i)error[:\s]*"""), "")
                    .trim()
                if (cleaned.length > 80 || cleaned.any { it in 'A'..'Z' || it in 'a'..'z' }) {
                    "خطا در تست اتصال"
                } else cleaned.ifBlank { "خطا در تست اتصال" }
            }
        }
    }

    fun flagForCountryCode(code: String?): String {
        val cc = code?.trim()?.uppercase().orEmpty()
        if (cc.length != 2) return "🌐"
        // Regional indicator symbols
        val a = Character.codePointAt(cc, 0) - 0x41 + 0x1F1E6
        val b = Character.codePointAt(cc, 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(a)) + String(Character.toChars(b))
    }

    /** Parse "(GB) 1.2.3.4" or similar into flag + text. */
    fun formatExitIp(raw: String?): Pair<String, String> {
        if (raw.isNullOrBlank()) return "" to "—"
        val m = Regex("""\(([A-Za-z]{2})\)\s*(.+)""").find(raw.trim())
        return if (m != null) {
            val code = m.groupValues[1]
            val ip = m.groupValues[2].trim()
            flagForCountryCode(code) to "$code · $ip"
        } else {
            "🌐" to raw.trim()
        }
    }
}
