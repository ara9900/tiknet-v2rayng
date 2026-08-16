package com.v2ray.ang.tiknet

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class TikNetPublicConfigParseTest {

    @Test
    fun telegramShopDisabledHidesButtonEvenWithUrl() {
        val cfg = TikNetApi.parsePublicConfigJson(
            """
            {
              "telegram_shop_enabled": false,
              "telegram_shop_url": "https://t.me/tik_net_bot?start=shop",
              "telegram_shop_label": "خرید از ربات تلگرام"
            }
            """.trimIndent(),
        )
        assertFalse(cfg.shopEnabled)
        assertFalse(cfg.showShop)
    }

    @Test
    fun telegramShopEnabledShowsButton() {
        val cfg = TikNetApi.parsePublicConfigJson(
            """
            {
              "telegram_shop_enabled": true,
              "telegram_shop_url": "https://t.me/tik_net_bot?start=shop",
              "telegram_shop_label": "خرید از ربات تلگرام"
            }
            """.trimIndent(),
        )
        assertTrue(cfg.shopEnabled)
        assertTrue(cfg.showShop)
        assertEquals("https://t.me/tik_net_bot?start=shop", cfg.shopUrl)
    }

    @Test
    fun telegramUrlWithoutFlagStaysHidden() {
        val cfg = TikNetApi.parsePublicConfigJson(
            """
            {
              "telegram_shop_url": "https://t.me/tik_net_bot?start=shop"
            }
            """.trimIndent(),
        )
        assertFalse(cfg.shopEnabled)
        assertFalse(cfg.showShop)
    }

    @Test
    fun explicitFalseBeatsOtherTrueFlags() {
        val cfg = TikNetApi.parsePublicConfigJson(
            """
            {
              "telegram_shop_enabled": false,
              "shop_enabled": true,
              "telegram_shop_url": "https://t.me/tik_net_bot?start=shop"
            }
            """.trimIndent(),
        )
        assertFalse(cfg.showShop)
    }

    @Test
    fun stringFalseIsRespected() {
        val cfg = TikNetApi.parsePublicConfigJson(
            """
            {
              "telegram_shop_enabled": "false",
              "telegram_shop_url": "https://t.me/tik_net_bot?start=shop"
            }
            """.trimIndent(),
        )
        assertFalse(cfg.showShop)
        assertNull(cfg.takeIf { it.showShop }?.shopUrl)
    }
}
