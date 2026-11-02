package com.v2ray.ang.tiknet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TikNetProductTest {
    @Test
    fun normalizeAliases() {
        assertEquals(TikNetProduct.KIND_GAME_DNS, TikNetProduct.normalize("game_dns"))
        assertEquals(TikNetProduct.KIND_GAME_DNS, TikNetProduct.normalize("DNS"))
        assertEquals(TikNetProduct.KIND_VPN, TikNetProduct.normalize(null))
        assertEquals(TikNetProduct.KIND_VPN, TikNetProduct.normalize("vpn"))
    }

    @Test
    fun dropsClashJsonInGameMode() {
        val clash = """{"proxies":[{"type":"vless"}]}"""
        assertEquals("", TikNetProduct.filterSubscriptionPayload(clash, "game_dns"))
        assertEquals(clash, TikNetProduct.filterSubscriptionPayload(clash, "vpn"))
    }

    @Test
    fun keepsWireGuardConf() {
        val conf = """
            [Interface]
            PrivateKey = aaa
            Address = 10.0.0.2/32
            DNS = 208.67.222.222
            MTU = 1300

            [Peer]
            PublicKey = bbb
            AllowedIPs = 1.1.1.1/32
            Endpoint = 1.2.3.4:24028
            PersistentKeepalive = 15
        """.trimIndent()
        val out = TikNetProduct.filterSubscriptionPayload(conf, "game_dns")
        assertTrue(out.contains("[Interface]"))
        assertTrue(out.contains("AllowedIPs"))
    }
}
