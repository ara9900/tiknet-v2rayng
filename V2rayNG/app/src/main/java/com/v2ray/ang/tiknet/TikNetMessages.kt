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

    /** Parse exit-IP probe like "(DE) 1.2.3.4" into `1.2.3.4-آلمان-🇩🇪`. */
    fun formatDestination(raw: String?, remarksFallback: String? = null): String {
        if (!raw.isNullOrBlank()) {
            val m = Regex("""\(([A-Za-z]{2})\)\s*(.+)""").find(raw.trim())
            if (m != null) {
                val code = m.groupValues[1].uppercase()
                val ip = m.groupValues[2].trim()
                val flag = flagForCountryCode(code)
                val name = countryNameFa(code)
                return "$ip-$name-$flag"
            }
            val ipOnly = Regex("""(\d{1,3}(?:\.\d{1,3}){3})""").find(raw)?.value
            if (ipOnly != null) {
                val fromRemarks = countryFromRemarks(remarksFallback)
                return "$ipOnly-${fromRemarks.first}-${fromRemarks.second}"
            }
            return raw.trim()
        }
        val fromRemarks = countryFromRemarks(remarksFallback)
        if (fromRemarks.first == "—" && fromRemarks.second == "🌐") return "—"
        return "—-${fromRemarks.first}-${fromRemarks.second}"
    }

    fun countryNameFa(code: String): String = when (code.uppercase()) {
        "DE" -> "آلمان"
        "NL" -> "هلند"
        "GB", "UK" -> "انگلیس"
        "US" -> "آمریکا"
        "FR" -> "فرانسه"
        "TR" -> "ترکیه"
        "AE" -> "امارات"
        "FI" -> "فنلاند"
        "SE" -> "سوئد"
        "NO" -> "نروژ"
        "IT" -> "ایتالیا"
        "ES" -> "اسپانیا"
        "CA" -> "کانادا"
        "JP" -> "ژاپن"
        "SG" -> "سنگاپور"
        "IN" -> "هند"
        "IR" -> "ایران"
        else -> code.uppercase()
    }

    private fun countryFromRemarks(remarks: String?): Pair<String, String> {
        val r = remarks.orEmpty()
        if (r.isBlank()) return "—" to "🌐"
        val lower = r.lowercase()
        val flag = Regex("""[\uD83C][\uDDE6-\uDDFF][\uD83C][\uDDE6-\uDDFF]""").find(r)?.value ?: "🌐"
        val name = when {
            "آلمان" in r || "germany" in lower -> "آلمان"
            "هلند" in r || "netherlands" in lower -> "هلند"
            "انگلیس" in r || "britain" in lower || "united kingdom" in lower -> "انگلیس"
            "آمریکا" in r || "usa" in lower || "united states" in lower -> "آمریکا"
            "فرانسه" in r || "france" in lower -> "فرانسه"
            "ترکیه" in r || "turkey" in lower || "türkiye" in lower -> "ترکیه"
            "امارات" in r || "uae" in lower || "dubai" in lower -> "امارات"
            else -> r.replace(Regex("""[\uD83C][\uDDE6-\uDDFF][\uD83C][\uDDE6-\uDDFF]"""), "")
                .trim()
                .substringBefore(" ")
                .ifBlank { "—" }
        }
        return name to flag
    }
}
