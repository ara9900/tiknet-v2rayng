package com.v2ray.ang.fmt

import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.extension.idnHost
import com.v2ray.ang.extension.isNotNullEmpty
import com.v2ray.ang.util.Utils
import java.net.URI

object SshFmt : FmtBase() {
    /**
     * Parses an ssh://user:pass@host:port#remark URI into a ProfileItem.
     * Used as a VPN tunnel (local SOCKS via SSH), not interactive shell login.
     */
    fun parse(str: String): ProfileItem? {
        val config = ProfileItem.create(EConfigType.SSH)

        val uri = URI(Utils.fixIllegalUrl(str))
        if (uri.idnHost.isEmpty()) return null

        config.remarks = Utils.decodeURIComponent(uri.fragment.orEmpty()).let { it.ifEmpty { "SSH" } }
        config.server = uri.idnHost
        config.serverPort = if (uri.port > 0) uri.port.toString() else "22"

        val userInfo = uri.userInfo
        if (!userInfo.isNullOrEmpty()) {
            val parts = if (userInfo.contains(":")) {
                userInfo.split(":", limit = 2)
            } else {
                Utils.decode(userInfo).split(":", limit = 2)
            }
            config.username = Utils.decodeURIComponent(parts.first())
            config.password = if (parts.size > 1) Utils.decodeURIComponent(parts.last()) else ""
        }

        if (config.username.isNullOrBlank()) return null
        return config
    }

    fun toUri(config: ProfileItem): String {
        val user = config.username.orEmpty()
        val pass = config.password.orEmpty()
        val pw = if (user.isNotNullEmpty()) "$user:$pass" else ":"
        return toUri(config, Utils.encode(pw, true), null)
    }
}
