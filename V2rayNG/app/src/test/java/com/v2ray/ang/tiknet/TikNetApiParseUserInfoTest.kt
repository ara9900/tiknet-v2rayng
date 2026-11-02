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
        assertEquals(TikNetProduct.KIND_VPN, user.productKind)
    }

    @Test
    fun parseUserInfoProductKindGameDns() {
        val root = JsonParser.parseString(
            """
            {
              "username": "gamer",
              "has_subscription": true,
              "product_kind": "game_dns",
              "plan_name": "DNS گیم ۳۰ روزه"
            }
            """.trimIndent(),
        ).asJsonObject

        val user = TikNetApi.parseUserInfo(root)

        assertEquals(TikNetProduct.KIND_GAME_DNS, user.productKind)
        assertTrue(TikNetProduct.isGameDns(user))
    }

    @Test
    fun filterSubscriptionKeepsOnlyWireGuardForGameDns() {
        val mixed = """
            vless://uuid@host:443?security=reality#vpn
            wireguard://key@1.2.3.4:24028?allowedips=1.1.1.1/32#game
            vmess://abc
        """.trimIndent()
        val filtered = TikNetProduct.filterSubscriptionPayload(mixed, TikNetProduct.KIND_GAME_DNS)
        assertTrue(filtered.contains("wireguard://"))
        assertFalse(filtered.contains("vless://"))
        assertFalse(filtered.contains("vmess://"))
        assertEquals(mixed, TikNetProduct.filterSubscriptionPayload(mixed, TikNetProduct.KIND_VPN))
    }
}
