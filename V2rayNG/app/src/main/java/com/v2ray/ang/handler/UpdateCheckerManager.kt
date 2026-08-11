package com.v2ray.ang.handler

import com.v2ray.ang.dto.CheckUpdateResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object UpdateCheckerManager {
    /**
     * Stock GitHub / v2rayNG update checks are disabled for TikNet.
     * In-app updates must come from the panel (`/api/customer/app-update`) only.
     */
    suspend fun checkForUpdate(includePreRelease: Boolean = false): CheckUpdateResult =
        withContext(Dispatchers.IO) {
            CheckUpdateResult(hasUpdate = false)
        }
}
