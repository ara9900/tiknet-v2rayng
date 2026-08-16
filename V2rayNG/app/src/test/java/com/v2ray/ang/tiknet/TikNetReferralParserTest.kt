package com.v2ray.ang.tiknet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TikNetReferralParserTest {
    @Test
    fun parsesReferralPayload() {
        val json = """
            {
              "enabled": true,
              "referral_code": "ABC123",
              "share_url": "https://t.me/tiknetbot",
              "can_attach_referrer": true,
              "stats": {"invited_count": 2, "rewarded_count": 1, "pending_count": 1},
              "rewards": {
                "referrer_on_first_purchase": {"type": "traffic_gb", "amount": 5},
                "invitee_on_first_purchase": {"type": "days", "amount": 3}
              },
              "milestones": [{"invites_required": 3, "traffic_gb": 10, "bonus_days": 0}],
              "progress": {
                "rewarded_count": 1,
                "current_target": 3,
                "progress_ratio": 0.33,
                "completed_all": false
              }
            }
        """.trimIndent()
        val info = TikNetReferralParser.parse(json)
        assertTrue(info.enabled)
        assertEquals("ABC123", info.referralCode)
        assertTrue(info.canAttachReferrer)
        assertEquals(2, info.stats.invitedCount)
        assertEquals("5 گیگ", info.referrerReward.labelFa)
        assertEquals("3 روز", info.inviteeReward.labelFa)
        assertEquals(1, info.milestones.size)
        assertEquals(3, info.progress?.currentTarget)
        assertFalse(TikNetReferralUi.completedAll(info))
        assertTrue(TikNetReferralUi.shareBody(info).contains("ABC123"))
    }

    @Test
    fun hidesWhenDisabledFlag() {
        val info = TikNetReferralParser.parse("""{"enabled": false, "referral_code": "X"}""")
        assertFalse(info.enabled)
    }
}

class TikNetEntitlementAlertsTest {
    @Test
    fun detectsExpiringSoon() {
        val alert = TikNetEntitlementAlerts.evaluate(
            TikNetUserInfo(
                username = "u",
                hasSubscription = true,
                isExpired = false,
                daysRemaining = 2,
            ),
        )
        assertEquals(TikNetEntitlementKind.ExpiringSoon, alert?.kind)
    }

    @Test
    fun detectsLowTraffic() {
        val alert = TikNetEntitlementAlerts.evaluate(
            TikNetUserInfo(
                username = "u",
                hasSubscription = true,
                isExpired = false,
                daysRemaining = 20,
                trafficUsedBytes = 90L * 1024 * 1024 * 1024,
                trafficLimitBytes = 100L * 1024 * 1024 * 1024,
            ),
        )
        assertEquals(TikNetEntitlementKind.LowTraffic, alert?.kind)
    }
}
