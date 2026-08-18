package com.v2ray.ang.tiknet

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Jalali date helpers. Digits stay Latin (0-9) for readability in the TikNet UI.
 */
object TikNetJalali {
    private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    /** Kept for call-site compatibility — returns Latin digits (readable). */
    fun toPersianDigits(s: String): String = toLatinDigits(s)

    fun toLatinDigits(s: String): String = buildString {
        for (c in s) {
            val idx = persianDigits.indexOf(c)
            append(if (idx >= 0) ('0' + idx) else c)
        }
    }

    /** Accepts ISO-8601, yyyy-MM-dd, or already-Shamsi-looking strings. */
    fun formatExpire(raw: String?): String {
        if (raw.isNullOrBlank()) return "—"
        val trimmed = toLatinDigits(raw.trim())
        // Already looks like jalali yyyy/mm/dd
        if (trimmed.matches(Regex("""\d{4}/\d{1,2}/\d{1,2}"""))) {
            return trimmed
        }
        val millis = parseToMillis(trimmed) ?: return trimmed
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
        val gY = cal.get(Calendar.YEAR)
        val gM = cal.get(Calendar.MONTH) + 1
        val gD = cal.get(Calendar.DAY_OF_MONTH)
        val (jy, jm, jd) = gregorianToJalali(gY, gM, gD)
        return String.format(Locale.US, "%04d/%02d/%02d", jy, jm, jd)
    }

    fun formatGb(value: Double): String {
        if (value < 0.01) return String.format(Locale.US, "%.0f MB", value * 1024.0)
        return String.format(Locale.US, "%.2f GB", value)
    }

    /** Short Jalali label for chart X-axis, e.g. 05/28 */
    fun formatChartAxisDate(raw: String?): String {
        if (raw.isNullOrBlank()) return "—"
        val full = formatExpire(raw)
        val parts = full.split('/')
        return if (parts.size == 3) "${parts[1]}/${parts[2]}" else full.take(8)
    }

    fun formatTraffic(used: Long?, limit: Long?): String {
        fun one(b: Long): String {
            val gb = 1024.0 * 1024.0 * 1024.0
            val mb = 1024.0 * 1024.0
            return when {
                b >= gb -> String.format(Locale.US, "%.1f GB", b / gb)
                b >= mb -> String.format(Locale.US, "%.0f MB", b / mb)
                else -> String.format(Locale.US, "%.0f KB", b / 1024.0)
            }
        }
        val u = used ?: 0L
        val l = limit ?: 0L
        if (u <= 0 && l <= 0) return "—"
        return if (l > 0) "${one(u)} / ${one(l)}" else one(u)
    }

    fun trafficRatio(used: Long?, limit: Long?): Float {
        val u = used ?: 0L
        val l = limit ?: 0L
        if (l <= 0) return 0f
        return (u.toDouble() / l.toDouble()).toFloat().coerceIn(0f, 1f)
    }

    fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0) return "0 B/s"
        val kb = 1024.0
        val mb = kb * 1024
        return when {
            bytesPerSec >= mb -> String.format(Locale.US, "%.1f MB/s", bytesPerSec / mb)
            bytesPerSec >= kb -> String.format(Locale.US, "%.0f KB/s", bytesPerSec / kb)
            else -> "$bytesPerSec B/s"
        }
    }

    fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024
        return when {
            bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
            bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
            bytes >= kb -> String.format(Locale.US, "%.0f KB", bytes / kb)
            else -> "$bytes B"
        }
    }

    fun formatUptime(connectedAtMs: Long?, nowMs: Long = System.currentTimeMillis()): String {
        if (connectedAtMs == null || connectedAtMs <= 0) return "—"
        val sec = ((nowMs - connectedAtMs) / 1000).coerceAtLeast(0)
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        val raw = if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%d:%02d", m, s)
        return raw
    }

    private fun parseToMillis(raw: String): Long? {
        return runCatching {
            // epoch seconds / millis
            raw.toLongOrNull()?.let { v ->
                return if (v < 10_000_000_000L) v * 1000 else v
            }
            val iso = raw.replace(' ', 'T')
            java.time.Instant.parse(
                when {
                    iso.endsWith("Z") || iso.contains('+') -> iso
                    iso.length == 10 -> "${iso}T00:00:00Z"
                    else -> "${iso}Z"
                }
            ).toEpochMilli()
        }.getOrNull() ?: runCatching {
            val p = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(raw.take(10))
            p?.time
        }.getOrNull()
    }

    /** Algorithm from common civil calendar conversion. */
    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
        val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) +
            ((gy2 + 399) / 400) + gd + gdm[gm - 1]
        var jy = -1595 + (33 * (days / 12053))
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + days / 31
            jd = 1 + days % 31
        } else {
            jm = 7 + (days - 186) / 30
            jd = 1 + (days - 186) % 30
        }
        return Triple(jy, jm, jd)
    }
}
