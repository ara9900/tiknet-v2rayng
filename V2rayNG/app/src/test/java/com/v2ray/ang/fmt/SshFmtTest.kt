package com.v2ray.ang.fmt

import com.v2ray.ang.enums.EConfigType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SshFmtTest {
    @Test
    fun parsesPanelShareLink() {
        val cfg = SshFmt.parse("ssh://u1:secret@1.2.3.4:2096#SSH-TR")
        assertNotNull(cfg)
        assertEquals(EConfigType.SSH, cfg!!.configType)
        assertEquals("u1", cfg.username)
        assertEquals("secret", cfg.password)
        assertEquals("1.2.3.4", cfg.server)
        assertEquals("2096", cfg.serverPort)
        assertEquals("SSH-TR", cfg.remarks)
    }

    @Test
    fun defaultsPort22() {
        val cfg = SshFmt.parse("ssh://alice:secret@ssh.example.com#node")
        assertNotNull(cfg)
        assertEquals("22", cfg!!.serverPort)
        assertEquals("alice", cfg.username)
    }

    @Test
    fun rejectsMissingUser() {
        assertNull(SshFmt.parse("ssh://1.2.3.4:2096#x"))
    }
}
