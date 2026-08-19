package com.v2ray.ang.ui.tiknet

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.v2ray.ang.AngApplication
import com.v2ray.ang.AppConfig
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.dto.AppInfo
import com.v2ray.ang.dto.TestServiceMessage
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SpeedtestManager
import com.v2ray.ang.helper.MessageHelper
import com.v2ray.ang.tiknet.TikNetAnnouncement
import com.v2ray.ang.tiknet.TikNetApi
import com.v2ray.ang.tiknet.TikNetApiException
import com.v2ray.ang.tiknet.TikNetAppUpdateController
import com.v2ray.ang.tiknet.TikNetAppUpdateState
import com.v2ray.ang.tiknet.TikNetBootstrap
import com.v2ray.ang.tiknet.TikNetDevice
import com.v2ray.ang.tiknet.TikNetDiagItem
import com.v2ray.ang.tiknet.TikNetDiagnostics
import com.v2ray.ang.tiknet.TikNetEntitlementAlert
import com.v2ray.ang.tiknet.TikNetEntitlementAlerts
import com.v2ray.ang.tiknet.TikNetFaqItem
import com.v2ray.ang.tiknet.TikNetNotificationItem
import com.v2ray.ang.tiknet.TikNetErrors
import com.v2ray.ang.tiknet.TikNetPrefs
import com.v2ray.ang.tiknet.TikNetPingCache
import com.v2ray.ang.tiknet.TikNetReferralInfo
import com.v2ray.ang.tiknet.TikNetSession
import com.v2ray.ang.tiknet.TikNetSync
import com.v2ray.ang.tiknet.TikNetUsageHistory
import com.v2ray.ang.tiknet.TikNetUserInfo
import com.v2ray.ang.ui.main.MainRepository
import com.v2ray.ang.ui.main.MainServiceEvent
import com.v2ray.ang.util.AppManagerUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import com.v2ray.ang.core.CoreServiceManager

enum class TikNetConnPhase {
    Disconnected,
    Connecting,
    Connected,
    Disconnecting,
}

data class TikNetServerItem(
    val guid: String,
    val remarks: String,
    val protocolLabel: String,
    val pingMs: Long?,
)

data class TikNetMainUiState(
    val phase: TikNetConnPhase = TikNetConnPhase.Disconnected,
    val smartPicking: Boolean = false,
    val smartMode: Boolean = true,
    val selectedGuid: String? = null,
    val selectedTitle: String = "اتصال هوشمند",
    val servers: List<TikNetServerItem> = emptyList(),
    val isPinging: Boolean = false,
    val user: TikNetUserInfo? = null,
    val busy: Boolean = false,
    val syncMessage: String? = null,
    /** True until first /me (or cache) attempt finishes — avoids false «بدون سرویس». */
    val userLoading: Boolean = true,
    val error: String? = null,
    val currentDelayText: String = "",
    val exitIpText: String = "",
    val uplinkSpeed: Long = 0,
    val downlinkSpeed: Long = 0,
    val sessionUp: Long = 0,
    val sessionDown: Long = 0,
    val connectedAtMs: Long? = null,
    val uptimeTick: Long = 0,
    val announcement: TikNetAnnouncement? = null,
    val appVersion: String = BuildConfig.VERSION_NAME,
    // filter
    val filterEnabled: Boolean = false,
    val filterLoading: Boolean = false,
    val filterApps: List<AppInfo> = emptyList(),
    val filterSelected: Set<String> = emptySet(),
    val filterQuery: String = "",
    // account sheets data
    val notifications: List<TikNetNotificationItem> = emptyList(),
    val notificationsLoading: Boolean = false,
    val faq: List<TikNetFaqItem> = emptyList(),
    val faqLoading: Boolean = false,
    val diagnostics: List<TikNetDiagItem> = emptyList(),
    val diagnosticsLoading: Boolean = false,
    val diagnosticsFixing: Boolean = false,
    val unreadCount: Int = 0,
    val shopUrl: String? = null,
    val shopLabel: String? = null,
    val telegramSupport: String? = null,
    val showSplash: Boolean = true,
    val appUpdate: TikNetAppUpdateState = TikNetAppUpdateState.Idle,
    val entitlementAlert: TikNetEntitlementAlert? = null,
    val referral: TikNetReferralInfo? = null,
    val referralLoading: Boolean = false,
    val referralError: String? = null,
    val referralDisabled: Boolean = false,
    val referralAttaching: Boolean = false,
    /** True when showing cached profile because panel /me failed. */
    val profileOffline: Boolean = false,
    val pinnedServers: Set<String> = emptySet(),
    val iranDirectEnabled: Boolean = true,
    val widgetMode: String = TikNetPrefs.WIDGET_MODE_CURRENT,
    val widgetServerGuid: String? = null,
    val reconnectOnNetwork: Boolean = true,
    val usageHistory: TikNetUsageHistory? = null,
    val usageDays: Int = 14,
    val usageLoading: Boolean = false,
    val usageMissing: Boolean = false,
    val sessions: List<TikNetSession> = emptyList(),
    val sessionsLoading: Boolean = false,
    val sessionsError: String? = null,
    val sessionRevokingId: Int? = null,
) {
    val isUpdateBlocking: Boolean
        get() = when (val update = appUpdate) {
            is TikNetAppUpdateState.Available -> update.info.force
            is TikNetAppUpdateState.Downloading -> update.info.force
            is TikNetAppUpdateState.Error -> update.info?.force == true
            else -> false
        }
}

sealed class TikNetUiEvent {
    data object StartVpn : TikNetUiEvent()
    data object RestartVpn : TikNetUiEvent()
    data class Toast(val message: String) : TikNetUiEvent()
}

class TikNetMainViewModel(
    application: Application,
    private val repository: MainRepository,
) : AndroidViewModel(application) {

    private val _ui = MutableStateFlow(TikNetMainUiState())
    val ui: StateFlow<TikNetMainUiState> = _ui.asStateFlow()

    private val _events = MutableSharedFlow<TikNetUiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<TikNetUiEvent> = _events.asSharedFlow()

    private var pendingSmartConnect = false
    private var pendingSmartSwitch = false
    private var lastSmartConnectAtMs = 0L
    private var uptimeJob: Job? = null
    private var appsLoaded = false
    private var iranRoutingJob: Job? = null
    private val iranRoutingSeq = AtomicInteger(0)
    private val iranRoutingLock = Any()
    private var networkReconnectAttempts = 0

    init {
        // Enable live speed notifications → traffic broadcast for details
        MmkvManager.encodeSettings(AppConfig.PREF_SPEED_ENABLED, true)
        val cached = TikNetPrefs.getCachedProfile(application)
        val cachedOrUsername = cached
            ?: TikNetPrefs.getUsername(application)
                ?.takeIf { it.isNotBlank() }
                ?.let { TikNetUserInfo(username = it) }
        if (cachedOrUsername != null) {
            val alert = TikNetEntitlementAlerts.evaluate(cachedOrUsername)
            _ui.update {
                it.copy(
                    user = cachedOrUsername,
                    userLoading = false,
                    entitlementAlert = alert,
                    pinnedServers = TikNetPrefs.getPinnedServers(application),
                    iranDirectEnabled = TikNetPrefs.isIranDirectEnabled(application),
                    widgetMode = TikNetPrefs.getWidgetMode(application),
                    widgetServerGuid = TikNetPrefs.getWidgetServerGuid(application),
                    reconnectOnNetwork = TikNetPrefs.isReconnectOnNetworkEnabled(application),
                )
            }
        } else {
            _ui.update {
                it.copy(
                    pinnedServers = TikNetPrefs.getPinnedServers(application),
                    iranDirectEnabled = TikNetPrefs.isIranDirectEnabled(application),
                    widgetMode = TikNetPrefs.getWidgetMode(application),
                    widgetServerGuid = TikNetPrefs.getWidgetServerGuid(application),
                    reconnectOnNetwork = TikNetPrefs.isReconnectOnNetworkEnabled(application),
                )
            }
        }
        refreshServers()
        observeService()
        MessageHelper.sendMsg2Service(application, AppConfig.MSG_REGISTER_CLIENT, "")
        viewModelScope.launch(Dispatchers.IO) {
            TikNetBootstrap.applyDefaults(getApplication())
            if (TikNetPrefs.isIranDirectEnabled(getApplication())) {
                TikNetBootstrap.refreshGeoAssets(getApplication())
            }
        }
        viewModelScope.launch { loadUser(silent = true) }
        viewModelScope.launch { loadAnnouncement() }
        viewModelScope.launch { checkForAppUpdate() }
        viewModelScope.launch(Dispatchers.IO) { TikNetDevice.registerIfLoggedIn(getApplication()) }
        loadUnreadCount()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { TikNetSync.syncPersonalSubscription(getApplication()) }
            withContext(Dispatchers.Main) {
                refreshServers()
                // Avoid auto real-ping storm on every launch (heavy on high-core phones).
            }
        }
    }

    private fun observeService() {
        viewModelScope.launch {
            repository.mainServiceEvent.collect { event ->
                when (event) {
                    MainServiceEvent.StateRunning,
                    MainServiceEvent.StateStartSuccess -> {
                        TikNetPingCache.clearFailover(getApplication())
                        TikNetPrefs.setWantConnected(getApplication(), true)
                        networkReconnectAttempts = 0
                        _ui.update {
                            it.copy(
                                phase = TikNetConnPhase.Connected,
                                busy = false,
                                smartPicking = false,
                                connectedAtMs = it.connectedAtMs ?: System.currentTimeMillis(),
                                sessionUp = 0,
                                sessionDown = 0,
                            )
                        }
                        refreshSelected()
                        startUptimeTicker()
                        MessageHelper.sendMsg2Service(
                            getApplication(),
                            AppConfig.MSG_MEASURE_DELAY,
                            "",
                        )
                        viewModelScope.launch(Dispatchers.IO) {
                            delay(1200)
                            val ip = runCatching { SpeedtestManager.getRemoteIPInfo() }.getOrNull()
                            if (!ip.isNullOrBlank()) {
                                _ui.update { it.copy(exitIpText = ip) }
                            }
                        }
                    }

                    MainServiceEvent.StateNotRunning,
                    MainServiceEvent.StateStopSuccess -> {
                        stopUptimeTicker()
                        TikNetPingCache.clearFailover(getApplication())
                        _ui.update {
                            it.copy(
                                phase = TikNetConnPhase.Disconnected,
                                busy = false,
                                smartPicking = false,
                                currentDelayText = "",
                                exitIpText = "",
                                uplinkSpeed = 0,
                                downlinkSpeed = 0,
                                connectedAtMs = null,
                            )
                        }
                    }

                    is MainServiceEvent.StateStartFailure -> {
                        val ctx = getApplication<Application>()
                        if (_ui.value.smartMode && !TikNetPingCache.isFailoverUsed(ctx) && !pendingSmartConnect) {
                            // Cached best failed on this network → one forced re-ping + reconnect.
                            TikNetPingCache.markFailoverUsed(ctx)
                            TikNetPingCache.invalidate(ctx)
                            _ui.update {
                                it.copy(
                                    error = null,
                                    syncMessage = "پینگ تازه به‌خاطر تغییر شبکه…",
                                )
                            }
                            lastSmartConnectAtMs = 0L
                            startSmartPingThenConnect()
                        } else {
                            _ui.update {
                                it.copy(
                                    phase = TikNetConnPhase.Disconnected,
                                    busy = false,
                                    smartPicking = false,
                                    error = event.errorMessage.ifBlank { "اتصال ناموفق" },
                                )
                            }
                        }
                    }

                    is MainServiceEvent.MeasureDelaySuccess -> {
                        val lines = event.content.lines().map { it.trim() }.filter { it.isNotEmpty() }
                        val delayLine = lines.firstOrNull().orEmpty()
                        val ipLine = lines.firstOrNull { it.startsWith("(") && it.contains(")") }.orEmpty()
                        _ui.update {
                            it.copy(
                                currentDelayText = delayLine,
                                exitIpText = ipLine.ifBlank { it.exitIpText },
                            )
                        }
                    }

                    is MainServiceEvent.TrafficUpdate -> {
                        val p = event.content.split("|")
                        if (p.size >= 4) {
                            val rateUp = p[0].toLongOrNull() ?: 0L
                            val rateDown = p[1].toLongOrNull() ?: 0L
                            val dUp = p[2].toLongOrNull() ?: 0L
                            val dDown = p[3].toLongOrNull() ?: 0L
                            _ui.update {
                                it.copy(
                                    uplinkSpeed = rateUp,
                                    downlinkSpeed = rateDown,
                                    sessionUp = it.sessionUp + dUp,
                                    sessionDown = it.sessionDown + dDown,
                                )
                            }
                        }
                    }

                    is MainServiceEvent.MeasureConfigNotify -> {
                        // keep pinging flag
                        _ui.update { it.copy(isPinging = true) }
                    }

                    MainServiceEvent.MeasureConfigSuccess -> {
                        // Per-server ping result — refresh list only; do not connect yet.
                        refreshServers()
                    }

                    is MainServiceEvent.MeasureConfigFinish -> {
                        refreshServers()
                        _ui.update { it.copy(isPinging = false) }
                        when {
                            pendingSmartConnect -> {
                                pendingSmartConnect = false
                                finishSmartPickAndConnect()
                            }
                            pendingSmartSwitch -> {
                                pendingSmartSwitch = false
                                val best = pickBestGuid()
                                if (best != null && best != _ui.value.selectedGuid) {
                                    TikNetPingCache.rememberSuccessfulBatch(getApplication())
                                    selectServer(best, smartLabel = true)
                                    _events.tryEmit(TikNetUiEvent.RestartVpn)
                                } else {
                                    _ui.update { it.copy(smartPicking = false) }
                                }
                            }
                            else -> {
                                // Manual / list ping — refresh cache TTL when results are usable.
                                TikNetPingCache.rememberSuccessfulBatch(getApplication())
                            }
                        }
                    }

                    else -> Unit
                }
            }
        }
    }

    private fun startUptimeTicker() {
        uptimeJob?.cancel()
        uptimeJob = viewModelScope.launch {
            while (isActive) {
                _ui.update { it.copy(uptimeTick = System.currentTimeMillis()) }
                delay(5_000)
            }
        }
    }

    private fun stopUptimeTicker() {
        uptimeJob?.cancel()
        uptimeJob = null
    }

    fun checkForAppUpdate() {
        viewModelScope.launch {
            _ui.update { it.copy(appUpdate = TikNetAppUpdateState.Checking) }
            val state = withContext(Dispatchers.IO) {
                TikNetAppUpdateController.check(getApplication())
            }
            _ui.update { it.copy(appUpdate = state) }
        }
    }

    fun dismissOptionalUpdate() {
        val update = _ui.value.appUpdate
        val info = (update as? TikNetAppUpdateState.Available)?.info ?: return
        if (info.force) return
        _ui.update { it.copy(appUpdate = TikNetAppUpdateState.UpToDate) }
    }

    fun downloadAndInstallUpdate() {
        val current = _ui.value.appUpdate
        val info = when (current) {
            is TikNetAppUpdateState.Available -> current.info
            is TikNetAppUpdateState.Error -> current.info ?: return
            is TikNetAppUpdateState.Downloading -> return
            else -> return
        }

        viewModelScope.launch {
            _ui.update { it.copy(appUpdate = TikNetAppUpdateState.Downloading(info, 0)) }
            try {
                withContext(Dispatchers.IO) {
                    TikNetAppUpdateController.downloadAndInstall(
                        ctx = getApplication(),
                        info = info,
                    ) { progress ->
                        _ui.update { state ->
                            val update = state.appUpdate
                            if (update is TikNetAppUpdateState.Downloading &&
                                update.info.versionCode == info.versionCode
                            ) {
                                state.copy(appUpdate = TikNetAppUpdateState.Downloading(info, progress))
                            } else {
                                state
                            }
                        }
                    }
                }
                _ui.update { it.copy(appUpdate = TikNetAppUpdateState.Available(info)) }
            } catch (e: Exception) {
                _ui.update { it.copy(appUpdate = TikNetAppUpdateState.Error(TikNetErrors.message(e), info)) }
            }
        }
    }

    fun refreshServers() {
        val guids = linkedSetOf<String>()
        guids.addAll(MmkvManager.decodeServerList(TikNetPrefs.TIKNET_SUB_GUID))
        MmkvManager.decodeSubsList().forEach { subId ->
            if (subId.startsWith("tiknet-")) {
                guids.addAll(MmkvManager.decodeServerList(subId))
            }
        }
        if (guids.isEmpty()) guids.addAll(MmkvManager.decodeAllServerList())
        val selected = MmkvManager.getSelectServer()
        val items = guids.mapNotNull { guid ->
            val cfg = MmkvManager.decodeServerConfig(guid) ?: return@mapNotNull null
            val aff = MmkvManager.decodeServerAffiliationInfo(guid)
            val ping = aff?.testDelayMillis?.takeIf { it > 0 && it < 65000 }
            TikNetServerItem(
                guid = guid,
                remarks = cfg.remarks.ifBlank { "سرور" },
                protocolLabel = protocolLabel(cfg),
                pingMs = ping,
            )
        }
        val pinned = TikNetPrefs.getPinnedServers(getApplication())
        val sorted = items.sortedWith(
            compareByDescending<TikNetServerItem> { pinned.contains(it.guid) }
                .thenBy {
                    val p = it.pingMs
                    if (p == null || p <= 0 || p >= 65000) Long.MAX_VALUE else p
                },
        )
        val selectedItem = sorted.firstOrNull { it.guid == selected }
        _ui.update {
            it.copy(
                servers = sorted,
                pinnedServers = pinned,
                selectedGuid = selectedItem?.guid ?: selected,
                selectedTitle = when {
                    it.smartMode -> "اتصال هوشمند"
                    selectedItem != null -> selectedItem.remarks
                    else -> "انتخاب سرور"
                },
            )
        }
    }

    fun togglePinnedServer(guid: String) {
        val pinned = TikNetPrefs.togglePinnedServer(getApplication(), guid)
        _ui.update { it.copy(pinnedServers = pinned) }
        refreshServers()
    }

    fun setIranDirectEnabled(enabled: Boolean) {
        val seq = iranRoutingSeq.incrementAndGet()
        // Optimistic UI — switch must flip immediately.
        _ui.update {
            it.copy(
                iranDirectEnabled = enabled,
                syncMessage = if (enabled) "در حال اعمال مسیریابی ایران…" else "در حال غیرفعال‌سازی…",
            )
        }
        // Cancel in-flight toggle UI continuation; lock below prevents stale IO overwrite.
        iranRoutingJob?.cancel()
        iranRoutingJob = viewModelScope.launch {
            try {
                val applied = withContext(Dispatchers.IO) {
                    synchronized(iranRoutingLock) {
                        if (seq != iranRoutingSeq.get()) {
                            return@withContext TikNetPrefs.isIranDirectEnabled(getApplication())
                        }
                        TikNetBootstrap.setIranDirectRouting(getApplication(), enabled)
                        TikNetPrefs.isIranDirectEnabled(getApplication())
                    }
                }
                ensureActive()
                if (seq != iranRoutingSeq.get()) return@launch
                _ui.update {
                    it.copy(
                        iranDirectEnabled = applied,
                        syncMessage = if (applied) {
                            "مسیریابی ایران و لوکال فعال شد"
                        } else {
                            "مسیریابی ایران خاموش شد (فقط لوکال مستقیم می‌ماند)"
                        },
                    )
                }
                if (_ui.value.phase == TikNetConnPhase.Connected) {
                    _events.tryEmit(TikNetUiEvent.RestartVpn)
                }
                if (applied) {
                    launch(Dispatchers.IO) {
                        val ctx = getApplication<Application>()
                        if (TikNetBootstrap.needsGeoAssets(ctx)) {
                            withContext(Dispatchers.Main) {
                                if (seq == iranRoutingSeq.get()) {
                                    _ui.update { it.copy(syncMessage = "در حال دریافت فایل‌های مسیریابی…") }
                                }
                            }
                            TikNetBootstrap.refreshGeoAssets(ctx)
                            withContext(Dispatchers.Main) {
                                if (seq != iranRoutingSeq.get() || !TikNetPrefs.isIranDirectEnabled(ctx)) {
                                    return@withContext
                                }
                                _ui.update { it.copy(syncMessage = "فایل‌های مسیریابی آماده شد") }
                                // Geo files are required for WHITE_IRAN domain/IP matches —
                                // restart again so the running core reloads with assets present.
                                if (_ui.value.phase == TikNetConnPhase.Connected) {
                                    _events.tryEmit(TikNetUiEvent.RestartVpn)
                                }
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (seq != iranRoutingSeq.get()) return@launch
                android.util.Log.e("TikNet", "setIranDirectEnabled failed: ${e.message}", e)
                val reverted = !enabled
                withContext(Dispatchers.IO) {
                    synchronized(iranRoutingLock) {
                        if (seq == iranRoutingSeq.get()) {
                            runCatching { TikNetBootstrap.setIranDirectRouting(getApplication(), reverted) }
                        }
                    }
                }
                _ui.update {
                    it.copy(
                        iranDirectEnabled = TikNetPrefs.isIranDirectEnabled(getApplication()),
                        syncMessage = TikNetErrors.message(e, "تغییر مسیریابی ناموفق بود"),
                    )
                }
            }
        }
    }

    fun setWidgetMode(mode: String) {
        TikNetPrefs.setWidgetMode(getApplication(), mode)
        _ui.update { it.copy(widgetMode = mode) }
        com.v2ray.ang.receiver.WidgetProvider.requestUpdate(getApplication())
    }

    fun setWidgetServerGuid(guid: String?) {
        TikNetPrefs.setWidgetServerGuid(getApplication(), guid)
        _ui.update { it.copy(widgetServerGuid = guid) }
        if (!guid.isNullOrBlank()) {
            TikNetPrefs.setWidgetMode(getApplication(), TikNetPrefs.WIDGET_MODE_FIXED)
            _ui.update { it.copy(widgetMode = TikNetPrefs.WIDGET_MODE_FIXED) }
        }
        com.v2ray.ang.receiver.WidgetProvider.requestUpdate(getApplication())
    }

    fun pinHomeWidget(kind: com.v2ray.ang.tiknet.TikNetWidgetPin.Kind = com.v2ray.ang.tiknet.TikNetWidgetPin.Kind.Full) {
        val ctx = getApplication<Application>()
        if (!com.v2ray.ang.tiknet.TikNetWidgetPin.isSupported(ctx)) {
            showMessage("لانچر دستگاه از افزودن ویجت داخل اپ پشتیبانی نمی‌کند. از لیست ویجت‌ها دستی اضافه کنید.")
            return
        }
        val ok = com.v2ray.ang.tiknet.TikNetWidgetPin.requestPin(ctx, kind)
        showMessage(
            if (ok) "اضافه کردن ویجت را روی صفحهٔ اصلی تأیید کنید"
            else "لانچر دستگاه از افزودن ویجت داخل اپ پشتیبانی نمی‌کند. از لیست ویجت‌ها دستی اضافه کنید.",
        )
    }

    fun refreshSelected() {
        val guid = MmkvManager.getSelectServer()
        val cfg = guid?.let { MmkvManager.decodeServerConfig(guid) }
        _ui.update {
            it.copy(
                selectedGuid = guid,
                // Smart mode must keep the picker label as «اتصال هوشمند», never the node name.
                selectedTitle = if (it.smartMode) {
                    "اتصال هوشمند"
                } else {
                    cfg?.remarks?.ifBlank { "سرور" } ?: it.selectedTitle
                },
            )
        }
    }

    fun selectServer(guid: String, smartLabel: Boolean = false) {
        MmkvManager.setSelectServer(guid)
        val cfg = MmkvManager.decodeServerConfig(guid)
        _ui.update {
            it.copy(
                smartMode = smartLabel,
                selectedGuid = guid,
                selectedTitle = if (smartLabel) "اتصال هوشمند"
                else cfg?.remarks?.ifBlank { "سرور" } ?: "سرور",
            )
        }
        refreshServers()
    }

    fun enableSmartMode() {
        _ui.update { it.copy(smartMode = true, selectedTitle = "اتصال هوشمند") }
    }

    fun pingAllServers() {
        val guids = _ui.value.servers.map { it.guid }
        if (guids.isEmpty()) {
            refreshServers()
        }
        val list = _ui.value.servers.map { it.guid }.ifEmpty {
            MmkvManager.decodeServerList(TikNetPrefs.TIKNET_SUB_GUID)
        }
        if (list.isEmpty()) return
        _ui.update { it.copy(isPinging = true) }
        MessageHelper.sendMsg2TestService(
            getApplication(),
            TestServiceMessage(
                key = AppConfig.MSG_MEASURE_CONFIG_START,
                subscriptionId = TikNetPrefs.TIKNET_SUB_GUID,
                serverGuids = list,
                onlyTcp = false,
            ),
        )
    }

    private fun pickBestGuid(): String? {
        refreshServers()
        return _ui.value.servers
            .filter { it.pingMs != null && it.pingMs > 0 && it.pingMs < 65000 }
            .minByOrNull { it.pingMs!! }
            ?.guid
            ?: _ui.value.servers.firstOrNull()?.guid
    }

    private fun startSmartPingThenConnect() {
        _ui.update {
            it.copy(
                phase = TikNetConnPhase.Connecting,
                smartPicking = true,
                busy = true,
                selectedTitle = "اتصال هوشمند",
            )
        }
        pendingSmartConnect = true
        pingAllServers()
        viewModelScope.launch {
            delay(12_000)
            if (pendingSmartConnect) {
                pendingSmartConnect = false
                finishSmartPickAndConnect()
            }
        }
    }

    private fun finishSmartPickAndConnect() {
        val best = pickBestGuid()
        if (best != null) {
            TikNetPingCache.rememberSuccessfulBatch(getApplication())
            selectServer(best, smartLabel = true)
            _events.tryEmit(TikNetUiEvent.StartVpn)
        } else {
            _ui.update {
                it.copy(
                    smartPicking = false,
                    phase = TikNetConnPhase.Disconnected,
                    busy = false,
                    error = "سروری با پینگ معتبر پیدا نشد",
                )
            }
        }
    }

    private fun connectSmartFromCache() {
        val best = pickBestGuid()
        if (best == null) {
            startSmartPingThenConnect()
            return
        }
        _ui.update {
            it.copy(
                phase = TikNetConnPhase.Connecting,
                smartPicking = false,
                busy = true,
                selectedTitle = "اتصال هوشمند",
                syncMessage = "وصل سریع با پینگ ذخیره‌شده…",
            )
        }
        selectServer(best, smartLabel = true)
        markConnecting()
        _events.tryEmit(TikNetUiEvent.StartVpn)
    }

    /** Power button: smart → cache or ping-then-connect; manual → connect selected. */
    fun requestConnect() {
        _ui.update { it.copy(error = null) }
        if (_ui.value.smartMode) {
            val now = System.currentTimeMillis()
            if (pendingSmartConnect || _ui.value.smartPicking) return
            if (now - lastSmartConnectAtMs < 1_200L) return
            lastSmartConnectAtMs = now
            val ctx = getApplication<Application>()
            if (TikNetPingCache.isFresh(ctx)) {
                connectSmartFromCache()
            } else {
                startSmartPingThenConnect()
            }
        } else {
            if (!ensureServerSelected()) {
                _ui.update { it.copy(error = "ابتدا سرور را انتخاب کنید یا اشتراک را بروزرسانی کنید") }
                syncSubscription()
                return
            }
            markConnecting()
            _events.tryEmit(TikNetUiEvent.StartVpn)
        }
    }

    fun cancelConnectAttempt() {
        pendingSmartConnect = false
        pendingSmartSwitch = false
        lastSmartConnectAtMs = System.currentTimeMillis()
        TikNetPrefs.setWantConnected(getApplication(), false)
        runCatching {
            MessageHelper.sendMsg2TestService(
                getApplication(),
                TestServiceMessage(key = AppConfig.MSG_MEASURE_CONFIG_CANCEL),
            )
        }
        TikNetPrefs.setWidgetConnecting(getApplication(), false)
        TikNetPrefs.setWidgetSmartPending(getApplication(), false)
        com.v2ray.ang.tiknet.TikNetWidgetConnect.clearSmartPending(getApplication())
        com.v2ray.ang.receiver.WidgetProvider.requestUpdate(getApplication())
        _ui.update {
            it.copy(
                phase = TikNetConnPhase.Disconnected,
                smartPicking = false,
                busy = false,
                isPinging = false,
                error = null,
                selectedTitle = if (it.smartMode) "اتصال هوشمند" else it.selectedTitle,
            )
        }
    }

    fun markConnecting() {
        TikNetPrefs.setWidgetConnecting(getApplication(), true)
        com.v2ray.ang.receiver.WidgetProvider.requestUpdate(getApplication())
        _ui.update {
            it.copy(phase = TikNetConnPhase.Connecting, busy = true, error = null)
        }
    }

    fun markDisconnecting() {
        pendingSmartConnect = false
        pendingSmartSwitch = false
        TikNetPrefs.setWantConnected(getApplication(), false)
        _ui.update {
            it.copy(phase = TikNetConnPhase.Disconnecting, busy = true, error = null, smartPicking = false)
        }
    }

    fun ensureServerSelected(): Boolean {
        val guid = MmkvManager.getSelectServer()
        if (!guid.isNullOrBlank() && MmkvManager.decodeServerConfig(guid) != null) return true
        val first = _ui.value.servers.firstOrNull() ?: run {
            refreshServers()
            _ui.value.servers.firstOrNull()
        } ?: return false
        MmkvManager.setSelectServer(first.guid)
        refreshServers()
        return true
    }

    fun syncSubscription() {
        viewModelScope.launch {
            _ui.update { it.copy(busy = true, syncMessage = "در حال همگام‌سازی…") }
            try {
                val n = withContext(Dispatchers.IO) {
                    TikNetSync.syncPersonalSubscription(getApplication())
                }
                refreshServers()
                pingAllServers()
                _ui.update { it.copy(busy = false, syncMessage = "$n کانفیگ وارد شد") }
            } catch (e: Exception) {
                _ui.update {
                    it.copy(busy = false, syncMessage = TikNetErrors.message(e, "همگام‌سازی ناموفق"))
                }
            } finally {
                // Always refresh account profile, even when subscription import fails.
                loadUser(silent = true)
            }
        }
    }

    fun loadUser(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _ui.update { it.copy(busy = true) }
            try {
                val ctx = getApplication<Application>()
                val base = TikNetPrefs.getBaseUrl(ctx)
                val token = TikNetPrefs.getAccessToken(ctx)
                if (base.isNullOrBlank() || token.isNullOrBlank()) {
                    android.util.Log.w("TikNet", "loadUser: missing base/token")
                    _ui.update { it.copy(busy = false, userLoading = false) }
                    return@launch
                }
                val (me, supportTg) = withContext(Dispatchers.IO) { TikNetApi.enrichMe(base, token) }
                val publicCfg = withContext(Dispatchers.IO) {
                    runCatching { TikNetApi.getPublicConfig(base) }.getOrNull()
                }
                TikNetPrefs.saveCachedProfile(ctx, me)
                if (TikNetPrefs.getUsername(ctx).isNullOrBlank() && me.username.isNotBlank()) {
                    TikNetPrefs.saveSession(ctx, base, token, me.username)
                }
                val alert = TikNetEntitlementAlerts.evaluate(me)
                withContext(Dispatchers.IO) {
                    TikNetEntitlementAlerts.maybeNotify(ctx, alert)
                    runCatching { TikNetDevice.registerIfLoggedIn(ctx) }
                }
                // Panel may disable shop via public-config and/or /me.shop_enabled.
                val shopAllowedByMe = me.shopEnabled != false
                val showShop = publicCfg?.showShop == true && shopAllowedByMe
                _ui.update {
                    it.copy(
                        user = me,
                        busy = false,
                        userLoading = false,
                        error = null,
                        profileOffline = false,
                        telegramSupport = supportTg ?: me.supportTelegram,
                        shopUrl = if (showShop) publicCfg?.shopUrl else null,
                        shopLabel = if (showShop) publicCfg?.shopLabel else null,
                        entitlementAlert = alert,
                    )
                }
                loadUsageHistory()
            } catch (e: Exception) {
                android.util.Log.e("TikNet", "loadUser failed: ${e.message}", e)
                val cached = TikNetPrefs.getCachedProfile(getApplication())
                val uname = TikNetPrefs.getUsername(getApplication())
                val fallback = cached
                    ?: _ui.value.user
                    ?: uname?.takeIf { it.isNotBlank() }?.let { TikNetUserInfo(username = it) }
                val alert = TikNetEntitlementAlerts.evaluate(fallback)
                _ui.update {
                    it.copy(
                        user = fallback,
                        busy = false,
                        userLoading = false,
                        profileOffline = fallback != null,
                        telegramSupport = cached?.supportTelegram ?: it.telegramSupport,
                        error = if (!silent) TikNetErrors.message(e, "خطا در دریافت اطلاعات حساب") else it.error,
                        entitlementAlert = alert,
                    )
                }
            }
        }
    }

    fun loadAnnouncement() {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val ann = withContext(Dispatchers.IO) {
                TikNetApi.getAnnouncement(TikNetPrefs.getBaseUrl(ctx), TikNetPrefs.getAccessToken(ctx))
            }
            _ui.update { it.copy(announcement = ann) }
        }
    }

    fun dismissSplash() {
        _ui.update { it.copy(showSplash = false) }
    }

    fun loadUnreadCount() {
        viewModelScope.launch {
            try {
                val ctx = getApplication<Application>()
                val base = TikNetPrefs.getBaseUrl(ctx) ?: return@launch
                val token = TikNetPrefs.getAccessToken(ctx) ?: return@launch
                val (_, unread) = withContext(Dispatchers.IO) {
                    TikNetApi.getNotifications(base, token)
                }
                _ui.update { it.copy(unreadCount = unread) }
            } catch (_: Exception) {
                // keep previous count
            }
        }
    }

    // ---- App filter ----
    fun ensureAppsLoaded(force: Boolean = false) {
        if (!force && (appsLoaded || _ui.value.filterLoading)) return
        viewModelScope.launch {
            val started = System.currentTimeMillis()
            _ui.update { it.copy(filterLoading = true) }
            try {
                val apps = withContext(Dispatchers.IO) {
                    AppManagerUtil.loadNetworkAppList(getApplication())
                        .filter { it.packageName != getApplication<Application>().packageName }
                        .sortedBy { it.appName.lowercase() }
                }
                val selected = MmkvManager.decodeSettingsStringSet(AppConfig.PREF_PER_APP_PROXY_SET)?.toSet()
                    ?: emptySet()
                appsLoaded = true
                val wait = 700 - (System.currentTimeMillis() - started)
                if (wait > 0) delay(wait)
                _ui.update {
                    it.copy(
                        filterLoading = false,
                        filterApps = apps,
                        filterSelected = selected,
                        filterEnabled = MmkvManager.decodeSettingsBool(AppConfig.PREF_PER_APP_PROXY, false),
                    )
                }
            } catch (e: Exception) {
                appsLoaded = false
                _ui.update { it.copy(filterLoading = false, filterApps = emptyList()) }
            }
        }
    }

    fun setFilterEnabled(enabled: Boolean) {
        MmkvManager.encodeSettings(AppConfig.PREF_PER_APP_PROXY, enabled)
        _ui.update { it.copy(filterEnabled = enabled) }
    }

    fun setFilterQuery(q: String) {
        _ui.update { it.copy(filterQuery = q) }
    }

    fun toggleFilterApp(packageName: String) {
        val cur = _ui.value.filterSelected.toMutableSet()
        if (!cur.add(packageName)) cur.remove(packageName)
        MmkvManager.encodeSettings(AppConfig.PREF_PER_APP_PROXY_SET, cur.toMutableSet())
        _ui.update { it.copy(filterSelected = cur) }
    }

    fun filteredApps(): List<AppInfo> {
        val q = _ui.value.filterQuery.trim()
        val all = _ui.value.filterApps
        val filtered = if (q.isEmpty()) {
            all
        } else {
            all.filter {
                it.appName.contains(q, ignoreCase = true) || it.packageName.contains(q, ignoreCase = true)
            }
        }
        return if (_ui.value.filterEnabled) {
            filtered.sortedWith(
                compareByDescending<AppInfo> { _ui.value.filterSelected.contains(it.packageName) }
                    .thenBy { it.appName.lowercase() },
            )
        } else {
            filtered
        }
    }

    // ---- Account services ----
    fun loadNotifications() {
        viewModelScope.launch {
            _ui.update { it.copy(notificationsLoading = true) }
            try {
                val ctx = getApplication<Application>()
                val base = TikNetPrefs.getBaseUrl(ctx)!!
                val token = TikNetPrefs.getAccessToken(ctx)!!
                val (list, unread) = withContext(Dispatchers.IO) { TikNetApi.getNotifications(base, token) }
                _ui.update {
                    it.copy(
                        notifications = list,
                        notificationsLoading = false,
                        unreadCount = unread,
                    )
                }
            } catch (e: Exception) {
                _ui.update {
                    it.copy(notificationsLoading = false, syncMessage = TikNetErrors.message(e, "خطا در اعلان‌ها"))
                }
            }
        }
    }

    fun markNotificationRead(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val ctx = getApplication<Application>()
                TikNetApi.markNotificationRead(
                    TikNetPrefs.getBaseUrl(ctx)!!,
                    TikNetPrefs.getAccessToken(ctx)!!,
                    id,
                )
            }
            withContext(Dispatchers.Main) {
                _ui.update {
                    val updated = it.notifications.map { n ->
                        if (n.id == id) n.copy(read = true) else n
                    }
                    it.copy(
                        notifications = updated,
                        unreadCount = updated.count { n -> !n.read },
                    )
                }
            }
        }
    }

    fun loadFaq() {
        viewModelScope.launch {
            _ui.update { it.copy(faqLoading = true) }
            try {
                val ctx = getApplication<Application>()
                val base = TikNetPrefs.getBaseUrl(ctx) ?: TikNetApi.resolveBaseUrl(ctx)
                val list = withContext(Dispatchers.IO) { TikNetApi.getFaq(base) }
                _ui.update { it.copy(faq = list, faqLoading = false) }
            } catch (e: Exception) {
                _ui.update {
                    it.copy(
                        faqLoading = false,
                        faq = emptyList(),
                        syncMessage = TikNetErrors.message(e, "خطا در راهنما و سوالات"),
                    )
                }
            }
        }
    }

    fun loadReferral(force: Boolean = false) {
        if (!force && (_ui.value.referralLoading || _ui.value.referral != null || _ui.value.referralDisabled)) {
            return
        }
        viewModelScope.launch {
            _ui.update {
                it.copy(referralLoading = true, referralError = null, referralDisabled = false)
            }
            try {
                val ctx = getApplication<Application>()
                val base = TikNetPrefs.getBaseUrl(ctx)
                val token = TikNetPrefs.getAccessToken(ctx)
                if (base.isNullOrBlank() || token.isNullOrBlank()) {
                    _ui.update { it.copy(referralLoading = false, referralDisabled = true) }
                    return@launch
                }
                val info = withContext(Dispatchers.IO) { TikNetApi.getReferral(base, token) }
                if (!info.enabled) {
                    _ui.update {
                        it.copy(referralLoading = false, referral = null, referralDisabled = true)
                    }
                    return@launch
                }
                _ui.update {
                    it.copy(referralLoading = false, referral = info, referralError = null, referralDisabled = false)
                }
            } catch (e: Exception) {
                val code = (e as? TikNetApiException)?.statusCode
                if (code == 403 || code == 404 || code == 501) {
                    _ui.update {
                        it.copy(referralLoading = false, referralDisabled = true, referral = null)
                    }
                } else {
                    _ui.update {
                        it.copy(
                            referralLoading = false,
                            referralError = TikNetErrors.message(e, "خطا در دریافت اطلاعات معرف"),
                        )
                    }
                }
            }
        }
    }

    fun attachReferralCode(code: String) {
        val trimmed = code.trim()
        if (trimmed.isEmpty() || _ui.value.referralAttaching) return
        viewModelScope.launch {
            _ui.update { it.copy(referralAttaching = true) }
            try {
                val ctx = getApplication<Application>()
                val base = TikNetPrefs.getBaseUrl(ctx)!!
                val token = TikNetPrefs.getAccessToken(ctx)!!
                withContext(Dispatchers.IO) { TikNetApi.attachReferral(base, token, trimmed) }
                _ui.update {
                    it.copy(
                        referralAttaching = false,
                        syncMessage = "کد معرف ثبت شد.",
                    )
                }
                loadReferral(force = true)
            } catch (e: Exception) {
                _ui.update {
                    it.copy(
                        referralAttaching = false,
                        syncMessage = TikNetErrors.message(e, "ثبت کد معرف ناموفق"),
                    )
                }
            }
        }
    }

    fun runDiagnostics() {
        viewModelScope.launch {
            _ui.update { it.copy(diagnosticsLoading = true) }
            val vpnActive = isTikNetVpnActive()
            val items = withContext(Dispatchers.IO) {
                TikNetDiagnostics.collect(getApplication(), vpnActive)
            }
            _ui.update { it.copy(diagnostics = items, diagnosticsLoading = false) }
        }
    }

    fun autoFixDiagnostics() {
        viewModelScope.launch {
            _ui.update { it.copy(diagnosticsFixing = true) }
            val current = _ui.value.diagnostics.ifEmpty {
                withContext(Dispatchers.IO) {
                    TikNetDiagnostics.collect(getApplication(), isTikNetVpnActive())
                }
            }

            val messages = mutableListOf<String>()
            val failOrWarn = current.filter {
                it.status == com.v2ray.ang.tiknet.TikNetDiagStatus.Fail ||
                    it.status == com.v2ray.ang.tiknet.TikNetDiagStatus.Warn
            }

            // Battery exemption (system dialog)
            current.firstOrNull { it.id == "battery" && it.autoFix == "battery" }?.let {
                messages += TikNetDiagnostics.autoFixOne(
                    getApplication(), it,
                    onRestartVpn = {},
                    onSync = {},
                )
            }

            // Routing / geo (only refresh Iran geo assets when that mode is on)
            withContext(Dispatchers.IO) {
                runCatching {
                    TikNetBootstrap.applyDefaults(getApplication())
                    if (TikNetPrefs.isIranDirectEnabled(getApplication())) {
                        TikNetBootstrap.refreshGeoAssets(getApplication())
                    }
                    messages += "مسیریابی / geo بررسی شد"
                }
            }

            // Sync subscription when connectivity looks broken
            if (failOrWarn.any { it.id in setOf("http", "dns", "network") }) {
                withContext(Dispatchers.IO) {
                    runCatching {
                        TikNetSync.syncPersonalSubscription(getApplication())
                        messages += "همگام‌سازی اشتراک انجام شد"
                        withContext(Dispatchers.Main) { refreshServers() }
                    }
                }
            }

            // Restart / start VPN when needed
            if (failOrWarn.any { it.id in setOf("http", "dns", "network", "core") } ||
                current.any { it.autoFix == "restart_vpn" }
            ) {
                if (_ui.value.phase == TikNetConnPhase.Connected) {
                    _events.tryEmit(TikNetUiEvent.RestartVpn)
                    messages += "اتصال VPN راه‌اندازی مجدد شد"
                } else {
                    _events.tryEmit(TikNetUiEvent.StartVpn)
                    messages += "اتصال VPN شروع شد"
                }
            }

            // Open first settings page that needs manual change
            val manual = failOrWarn.firstOrNull {
                it.id in setOf("airplane", "auto_time", "private_dns", "always_on")
            }
            if (manual != null) {
                TikNetDiagnostics.openSettings(getApplication(), manual.settingsAction)
                messages += "تنظیمات «${manual.title}» باز شد"
            }

            if (messages.isEmpty()) messages += "مورد قابل رفع خودکار نبود"

            delay(1500)
            val items = withContext(Dispatchers.IO) {
                TikNetDiagnostics.collect(getApplication(), isTikNetVpnActive())
            }
            _ui.update {
                it.copy(
                    diagnostics = items,
                    diagnosticsFixing = false,
                    syncMessage = messages.joinToString(" · "),
                )
            }
        }
    }

    fun autoFixDiagItem(item: TikNetDiagItem) {
        viewModelScope.launch {
            _ui.update { it.copy(diagnosticsFixing = true) }
            val msg = TikNetDiagnostics.autoFixOne(
                ctx = getApplication(),
                item = item,
                onRestartVpn = {
                    if (_ui.value.phase == TikNetConnPhase.Connected) {
                        _events.tryEmit(TikNetUiEvent.RestartVpn)
                    } else {
                        _events.tryEmit(TikNetUiEvent.StartVpn)
                    }
                },
                onSync = { syncSubscription() },
            )
            delay(900)
            val items = withContext(Dispatchers.IO) {
                TikNetDiagnostics.collect(getApplication(), isTikNetVpnActive())
            }
            _ui.update {
                it.copy(
                    diagnostics = items,
                    diagnosticsFixing = false,
                    syncMessage = msg,
                )
            }
        }
    }

    fun openSettingsTarget(action: String?) {
        TikNetDiagnostics.openSettings(getApplication(), action)
    }

    fun loadUsageHistory(days: Int = _ui.value.usageDays) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val base = TikNetPrefs.getBaseUrl(ctx)
            val token = TikNetPrefs.getAccessToken(ctx)
            if (base.isNullOrBlank() || token.isNullOrBlank()) return@launch
            val clamped = days.coerceIn(7, 90)
            _ui.update { it.copy(usageLoading = true, usageDays = clamped) }
            try {
                val hist = withContext(Dispatchers.IO) {
                    TikNetApi.getUsageHistory(base, token, days = clamped)
                }
                _ui.update {
                    it.copy(usageHistory = hist, usageLoading = false, usageMissing = false)
                }
            } catch (e: TikNetApiException) {
                _ui.update {
                    it.copy(
                        usageLoading = false,
                        usageMissing = e.statusCode == 404,
                        usageHistory = if (e.statusCode == 404) null else it.usageHistory,
                    )
                }
            } catch (_: Exception) {
                _ui.update { it.copy(usageLoading = false) }
            }
        }
    }

    fun loadSessions() {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val base = TikNetPrefs.getBaseUrl(ctx)
            val token = TikNetPrefs.getAccessToken(ctx)
            if (base.isNullOrBlank() || token.isNullOrBlank()) return@launch
            _ui.update { it.copy(sessionsLoading = true, sessionsError = null) }
            try {
                val list = withContext(Dispatchers.IO) {
                    TikNetApi.listSessions(base, token)
                }
                _ui.update {
                    it.copy(sessions = list, sessionsLoading = false, sessionsError = null)
                }
            } catch (e: Exception) {
                _ui.update {
                    it.copy(
                        sessionsLoading = false,
                        sessionsError = TikNetErrors.message(e, "لیست نشست‌ها دریافت نشد"),
                    )
                }
            }
        }
    }

    fun revokeSession(sessionId: Int) {
        if (_ui.value.sessions.any { it.id == sessionId && it.isCurrent }) return
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val base = TikNetPrefs.getBaseUrl(ctx)
            val token = TikNetPrefs.getAccessToken(ctx)
            if (base.isNullOrBlank() || token.isNullOrBlank()) return@launch
            _ui.update { it.copy(sessionRevokingId = sessionId, sessionsError = null) }
            try {
                withContext(Dispatchers.IO) {
                    TikNetApi.revokeSession(base, token, sessionId)
                }
                _ui.update {
                    it.copy(
                        sessions = it.sessions.filter { s -> s.id != sessionId },
                        sessionRevokingId = null,
                        syncMessage = "نشست بسته شد",
                    )
                }
            } catch (e: Exception) {
                _ui.update {
                    it.copy(
                        sessionRevokingId = null,
                        sessionsError = TikNetErrors.message(e, "خروج از نشست ناموفق بود"),
                    )
                }
            }
        }
    }

    fun setReconnectOnNetworkEnabled(enabled: Boolean) {
        TikNetPrefs.setReconnectOnNetworkEnabled(getApplication(), enabled)
        _ui.update { it.copy(reconnectOnNetwork = enabled) }
    }

    fun onUnderlayNetworkAvailable() {
        val ctx = getApplication<Application>()
        if (!TikNetPrefs.isReconnectOnNetworkEnabled(ctx)) return
        if (!TikNetPrefs.isWantConnected(ctx)) return
        if (!TikNetPrefs.isLoggedIn(ctx)) return
        val user = _ui.value.user
        if (user?.isExpired == true || user?.hasSubscription == false) return
        val phase = _ui.value.phase
        if (phase == TikNetConnPhase.Connected ||
            phase == TikNetConnPhase.Connecting ||
            phase == TikNetConnPhase.Disconnecting
        ) {
            return
        }
        if (runCatching { CoreServiceManager.isRunning() }.getOrDefault(false)) return
        if (networkReconnectAttempts >= 2) return
        networkReconnectAttempts += 1
        _ui.update { it.copy(syncMessage = "شبکه عوض شد؛ دوباره وصل می‌شود…") }
        requestConnect()
    }

    fun onUnderlayNetworkLost() {
        networkReconnectAttempts = 0
    }

    private fun isTikNetVpnActive(): Boolean {
        return _ui.value.phase == TikNetConnPhase.Connected ||
            runCatching { CoreServiceManager.isRunning() }.getOrElse { false }
    }

    fun logout() {
        TikNetPrefs.setWantConnected(getApplication(), false)
        TikNetPrefs.clearSession(getApplication())
    }

    fun clearSyncMessage() {
        _ui.update { it.copy(syncMessage = null) }
    }

    fun showMessage(message: String) {
        _ui.update { it.copy(syncMessage = message) }
    }

    override fun onCleared() {
        stopUptimeTicker()
        repository.close()
        super.onCleared()
    }

    companion object {
        fun protocolLabel(cfg: ProfileItem): String {
            val type = cfg.configType.name
            val net = cfg.network?.takeIf { it.isNotBlank() }
            val sec = cfg.security?.takeIf { it.isNotBlank() }
            return listOfNotNull(type, net, sec).joinToString(" / ")
        }

        class Factory(
            private val app: AngApplication,
        ) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TikNetMainViewModel(app, MainRepository(app)) as T
            }
        }
    }
}
