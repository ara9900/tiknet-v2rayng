package com.v2ray.ang.tiknet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TikNetQrLoginTest {

    @Test
    fun parseJsonCredentials() {
        val payload = TikNetQrLogin.parse(
            """{"username":"alice","password":"secret","panel_url":"https://panel.example"}""",
        )

        assertTrue(payload is TikNetQrCredentials)
        payload as TikNetQrCredentials
        assertEquals("alice", payload.username)
        assertEquals("secret", payload.password)
        assertEquals("https://panel.example", payload.panelUrl)
    }

    @Test
    fun parseJsonToken() {
        val payload = TikNetQrLogin.parse(
            """{"token":"abc123","panel_url":"https://panel.example"}""",
        )

        assertTrue(payload is TikNetQrLoginToken)
        payload as TikNetQrLoginToken
        assertEquals("abc123", payload.token)
        assertEquals("https://panel.example", payload.panelUrl)
    }

    @Test
    fun parseTikNetDeepLink() {
        val payload = TikNetQrLogin.parse("tiknet://login?token=abc123&panel=https%3A%2F%2Fpanel.example")

        assertTrue(payload is TikNetQrLoginToken)
        payload as TikNetQrLoginToken
        assertEquals("abc123", payload.token)
        assertEquals("https://panel.example", payload.panelUrl)
        assertTrue(TikNetQrLogin.isLoginDeepLink("tiknet://login?token=abc123"))
    }

    @Test
    fun rejectSubscriptionLink() {
        val payload = TikNetQrLogin.parse("https://panel.example/subscription")

        assertTrue(payload is TikNetQrSubscriptionLink)
        payload as TikNetQrSubscriptionLink
        assertEquals("https://panel.example/subscription", payload.url)
        assertFalse(TikNetQrLogin.isLoginDeepLink("https://panel.example/subscription"))
    }
}
