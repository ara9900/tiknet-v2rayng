package com.v2ray.ang.tiknet

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlin.math.max
import kotlin.math.min

data class TikNetReferralRewardSpec(
    val type: String = "traffic_gb",
    val amount: Double = 0.0,
) {
    val labelFa: String
        get() {
            val n = if (amount % 1.0 == 0.0) amount.toInt().toString() else amount.toString()
            return when (type) {
                "days" -> "$n روز"
                else -> "$n گیگ"
            }
        }
}

data class TikNetReferralStats(
    val invitedCount: Int = 0,
    val rewardedCount: Int = 0,
    val pendingCount: Int = 0,
)

data class TikNetReferralMilestone(
    val invitesRequired: Int = 0,
    val trafficGb: Double = 0.0,
    val bonusDays: Int = 0,
    val title: String? = null,
) {
    val rewardCaptionFa: String
        get() {
            val parts = mutableListOf<String>()
            if (trafficGb > 0) {
                val n = if (trafficGb % 1.0 == 0.0) trafficGb.toInt().toString() else trafficGb.toString()
                parts += "$n گیگ"
            }
            if (bonusDays > 0) parts += "$bonusDays روز"
            return if (parts.isEmpty()) "" else "جایزه: ${parts.joinToString(" + ")}"
        }
}

data class TikNetReferralProgress(
    val rewardedCount: Int = 0,
    val currentMilestoneIndex: Int = 0,
    val currentTarget: Int = 0,
    val currentLabel: String? = null,
    val progressRatio: Double = 0.0,
    val rewardCaption: String? = null,
    val completedAll: Boolean = false,
    val nextMilestone: TikNetReferralMilestone? = null,
)

data class TikNetReferralInfo(
    val enabled: Boolean = true,
    val referralCode: String = "",
    val shareUrl: String? = null,
    val shareText: String? = null,
    val canAttachReferrer: Boolean = false,
    val attachedReferrerCode: String? = null,
    val stats: TikNetReferralStats = TikNetReferralStats(),
    val referrerReward: TikNetReferralRewardSpec = TikNetReferralRewardSpec(),
    val inviteeReward: TikNetReferralRewardSpec = TikNetReferralRewardSpec(),
    val progress: TikNetReferralProgress? = null,
    val milestones: List<TikNetReferralMilestone> = emptyList(),
)

data class TikNetReferralAttachResult(
    val ok: Boolean = true,
    val attachedReferrerCode: String? = null,
)

object TikNetReferralParser {
    fun parse(text: String): TikNetReferralInfo {
        val root = JsonParser.parseString(text).asJsonObject
        val rewards = root.getAsJsonObject("rewards") ?: JsonObject()
        val milestonesRaw = root.get("milestones")
            ?: root.get("referral_milestones")
            ?: rewards.get("milestones")
        val milestones = milestonesRaw?.asJsonArrayOrNull()
            ?.mapNotNull { el ->
                val o = el.asObjectOrNull() ?: return@mapNotNull null
                val m = parseMilestone(o)
                m.takeIf { it.invitesRequired > 0 }
            }
            .orEmpty()
        val progressRaw = root.get("progress") ?: root.get("milestone_progress")
        val enabled = bool(root, "referral_enabled", "enabled", "is_enabled") ?: true
        return TikNetReferralInfo(
            enabled = enabled,
            referralCode = str(root, "referral_code").orEmpty(),
            shareUrl = str(root, "share_url"),
            shareText = str(root, "share_text"),
            canAttachReferrer = bool(root, "can_attach_referrer") ?: false,
            attachedReferrerCode = str(root, "attached_referrer_code"),
            stats = parseStats(root.getAsJsonObject("stats")),
            referrerReward = parseReward(rewards.getAsJsonObject("referrer_on_first_purchase")),
            inviteeReward = parseReward(rewards.getAsJsonObject("invitee_on_first_purchase")),
            progress = progressRaw?.asObjectOrNull()?.let { parseProgress(it) },
            milestones = milestones,
        )
    }

    fun parseAttach(text: String, fallbackCode: String): TikNetReferralAttachResult {
        if (text.isBlank()) {
            return TikNetReferralAttachResult(ok = true, attachedReferrerCode = fallbackCode)
        }
        val root = runCatching { JsonParser.parseString(text).asJsonObject }.getOrNull()
            ?: return TikNetReferralAttachResult(ok = true, attachedReferrerCode = fallbackCode)
        return TikNetReferralAttachResult(
            ok = bool(root, "ok") ?: true,
            attachedReferrerCode = str(root, "attached_referrer_code") ?: fallbackCode,
        )
    }

    private fun parseStats(o: JsonObject?): TikNetReferralStats {
        if (o == null) return TikNetReferralStats()
        return TikNetReferralStats(
            invitedCount = int(o, "invited_count") ?: 0,
            rewardedCount = int(o, "rewarded_count") ?: 0,
            pendingCount = int(o, "pending_count") ?: 0,
        )
    }

    private fun parseReward(o: JsonObject?): TikNetReferralRewardSpec {
        if (o == null) return TikNetReferralRewardSpec()
        return TikNetReferralRewardSpec(
            type = str(o, "type") ?: "traffic_gb",
            amount = num(o, "amount") ?: 0.0,
        )
    }

    private fun parseMilestone(o: JsonObject): TikNetReferralMilestone {
        return TikNetReferralMilestone(
            invitesRequired = int(o, "invites_required", "target", "invites") ?: 0,
            trafficGb = num(o, "traffic_gb", "traffic") ?: 0.0,
            bonusDays = int(o, "bonus_days", "days") ?: 0,
            title = str(o, "title"),
        )
    }

    private fun parseProgress(o: JsonObject): TikNetReferralProgress {
        val next = o.get("next_milestone")?.asObjectOrNull()?.let { parseMilestone(it) }
        val ratio = (num(o, "progress_ratio") ?: 0.0).coerceIn(0.0, 1.0)
        return TikNetReferralProgress(
            rewardedCount = int(o, "rewarded_count") ?: 0,
            currentMilestoneIndex = int(o, "current_milestone_index") ?: 0,
            currentTarget = int(o, "current_target", "target")
                ?: next?.invitesRequired
                ?: 0,
            currentLabel = str(o, "current_label", "label"),
            progressRatio = ratio,
            rewardCaption = str(o, "reward_caption", "prize_caption"),
            completedAll = bool(o, "completed_all") ?: false,
            nextMilestone = next,
        )
    }

    private fun str(o: JsonObject, vararg keys: String): String? {
        for (k in keys) {
            val el = o.get(k) ?: continue
            if (el.isJsonNull || !el.isJsonPrimitive) continue
            val v = el.asString?.trim().orEmpty()
            if (v.isNotEmpty()) return v
        }
        return null
    }

    private fun bool(o: JsonObject, vararg keys: String): Boolean? {
        for (k in keys) {
            val el = o.get(k) ?: continue
            if (el.isJsonNull || !el.isJsonPrimitive) continue
            if (el.asJsonPrimitive.isBoolean) return el.asBoolean
        }
        return null
    }

    private fun int(o: JsonObject, vararg keys: String): Int? {
        for (k in keys) {
            val el = o.get(k) ?: continue
            if (el.isJsonNull || !el.isJsonPrimitive) continue
            val p = el.asJsonPrimitive
            if (p.isNumber) return p.asInt
            if (p.isString) return p.asString.toIntOrNull()
        }
        return null
    }

    private fun num(o: JsonObject, vararg keys: String): Double? {
        for (k in keys) {
            val el = o.get(k) ?: continue
            if (el.isJsonNull || !el.isJsonPrimitive) continue
            val p = el.asJsonPrimitive
            if (p.isNumber) return p.asDouble
            if (p.isString) return p.asString.toDoubleOrNull()
        }
        return null
    }

    private fun JsonElement.asObjectOrNull(): JsonObject? =
        takeIf { it.isJsonObject }?.asJsonObject

    private fun JsonElement.asJsonArrayOrNull() =
        takeIf { it.isJsonArray }?.asJsonArray
}

/** UI helpers for progress display (mirrors Flutter). */
object TikNetReferralUi {
    fun progressRatio(info: TikNetReferralInfo): Double {
        val progress = info.progress
        val rewarded = progress?.rewardedCount ?: info.stats.rewardedCount
        val target = progressTarget(info)
        if (progress != null) return progress.progressRatio.coerceIn(0.0, 1.0)
        if (target <= 0) return 0.0
        return min(1.0, max(0.0, rewarded.toDouble() / target))
    }

    fun progressTarget(info: TikNetReferralInfo): Int {
        val progress = info.progress
        val candidates = listOf(
            progress?.currentTarget ?: 0,
            progress?.nextMilestone?.invitesRequired ?: 0,
            info.milestones.firstOrNull()?.invitesRequired ?: 0,
        )
        return candidates.firstOrNull { it > 0 } ?: 5
    }

    fun progressLabel(info: TikNetReferralInfo): String {
        val progress = info.progress
        val rewarded = progress?.rewardedCount ?: info.stats.rewardedCount
        val target = progressTarget(info)
        val fromApi = progress?.currentLabel?.trim().orEmpty()
        if (fromApi.isNotEmpty()) return fromApi
        val display = min(rewarded, target)
        return "$display از $target"
    }

    fun rewardCaption(info: TikNetReferralInfo): String {
        val progress = info.progress
        progress?.rewardCaption?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        progress?.nextMilestone?.rewardCaptionFa?.takeIf { it.isNotEmpty() }?.let { return it }
        info.milestones.firstOrNull()?.rewardCaptionFa?.takeIf { it.isNotEmpty() }?.let { return it }
        return "جایزه پس از تکمیل این مرحله از پنل اعمال می‌شود"
    }

    fun completedAll(info: TikNetReferralInfo): Boolean {
        val progress = info.progress
        if (progress?.completedAll == true) return true
        val rewarded = progress?.rewardedCount ?: info.stats.rewardedCount
        val last = info.milestones.lastOrNull() ?: return false
        return last.invitesRequired > 0 && rewarded >= last.invitesRequired
    }

    fun shareBody(info: TikNetReferralInfo): String {
        val custom = info.shareText?.trim().orEmpty()
        if (custom.isNotEmpty()) return custom
        val lines = mutableListOf("با کد معرف من در تیک‌نت ثبت‌نام/خرید کن و پاداش بگیر:")
        if (info.referralCode.isNotBlank()) lines += "کد: ${info.referralCode}"
        info.shareUrl?.trim()?.takeIf { it.isNotEmpty() }?.let { lines += it }
        return lines.joinToString("\n")
    }
}
