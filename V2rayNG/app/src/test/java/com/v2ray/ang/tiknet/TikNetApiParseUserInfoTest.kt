package com.v2ray.ang.tiknet

import com.google.gson.JsonParser
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TikNetApiParseUserInfoTest {

    @Test
    fun parseUserInfoWithBrandSupportTelegramNull() {
        val root = JsonParser.parseString(
            """
            {
              "username": "alice",
              "has_subscription": true,
              "support_telegram": null,
              "brand": { "support_telegram": null }
            }
            """.trimIndent(),
        ).asJsonObject

        val user = TikNetApi.parseUserInfo(root)

        assertEquals("alice", user.username)
        assertTrue(user.hasSubscription)
        assertNull(user.supportTelegram)
    }

    @Test
    fun parseUserInfoHasSubscriptionFalse() {
        val root = JsonParser.parseString(
            """
            {
              "username": "bob",
              "has_subscription": false,
              "is_expired": true
            }
            """.trimIndent(),
        ).asJsonObject

        val user = TikNetApi.parseUserInfo(root)

        assertEquals("bob", user.username)
        assertFalse(user.hasSubscription)
        assertTrue(user.isExpired == true)
    }
}
