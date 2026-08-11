package com.v2ray.ang.ui.tiknet

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.PowerManager
import android.provider.Settings
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
import com.v2ray.ang.tiknet.TikNetBootstrap
import com.v2ray.ang.tiknet.TikNetFaqItem
import com.v2ray.ang.tiknet.TikNetNotificationItem
import com.v2ray.ang.tiknet.TikNetPrefs
import com.v2ray.ang.tiknet.TikNetSync
import com.v2ray.ang.tiknet.TikNetUserInfo
import com.v2ray.ang.ui.main.MainRepository
import com.v2ray.ang.ui.main.MainServiceEvent
import com.v2ray.ang.util.AppManagerUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

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

data class TikNetDiagItem(
    val title: String,
    val detail: String,
    val ok: Boolean,
    val settingsAction: String? = null,
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
    val unreadCount: Int = 0,
    val shopUrl: String? = null,
    val shopLabel: String? = null,
    val telegramSupport: String? = null,
    val showSplash: Boolean = true,
)

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
    private var uptimeJob: Job? = null
    private var appsLoaded = false

    init {
        // Enable live speed notifications → traffic broadcast for details
        MmkvManager.encodeSettings(AppConfig.PREF_SPEED_ENABLED, true)
        refreshServers()
        observeService()
        MessageHelper.sendMsg2Service(application, AppConfig.MSG_REGISTER_CLIENT, "")
        viewModelScope.launch(Dispatchers.IO) {
            TikNetBootstrap.applyDefaults(getApplication())
            TikNetBootstrap.refreshGeoAssets(getApplication())
        }
        viewModelScope.launch { loadUser(silent = true) }
        viewModelScope.launch { loadAnnouncement() }
        loadUnreadCount()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val ctx = getApplication<Application>()
                val base = TikNetPrefs.getBaseUrl(ctx) ?: TikNetApi.resolveBaseUrl(ctx)
                TikNetApi.getAppUpdate(base)
            }
        }
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
                        _ui.update {
                            it.copy(
                                phase = TikNetConnPhase.Disconnected,
                                busy = false,
                                smartPicking = false,
                                error = event.errorMessage.ifBlank { "اتصال ناموفق" },
                            )
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

                    is MainServiceEvent.MeasureConfigFinish,
                    MainServiceEvent.MeasureConfigSuccess -> {
                        refreshServers()
                        _ui.update { it.copy(isPinging = false) }
                        if (pendingSmartConnect) {
                            pendingSmartConnect = false
                            val best = pickBestGuid()
                            if (best != null) {
                                selectServer(best, smartLabel = true)
                                _events.tryEmit(TikNetUiEvent.StartVpn)
                            } else {
                                _ui.update {
                                    it.copy(
                                        smartPicking = false,
                                        phase = TikNetConnPhase.Disconnected,
                                        error = "سروری با پینگ معتبر پیدا نشد",
                                    )
                                }
                            }
                        }
                        if (pendingSmartSwitch) {
                            pendingSmartSwitch = false
                            val best = pickBestGuid()
                            if (best != null && best != _ui.value.selectedGuid) {
                                selectServer(best, smartLabel = true)
                                _events.tryEmit(TikNetUiEvent.RestartVpn)
                            } else {
                                _ui.update { it.copy(smartPicking = false) }
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
                delay(1000)
            }
        }
    }

    private fun stopUptimeTicker() {
        uptimeJob?.cancel()
        uptimeJob = null
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
        val sorted = items.sortedWith(compareBy<TikNetServerItem> {
            val p = it.pingMs
            if (p == null || p <= 0 || p >= 65000) Long.MAX_VALUE else p
        })
        val selectedItem = sorted.firstOrNull { it.guid == selected }
        _ui.update {
            it.copy(
                servers = sorted,
                selectedGuid = selectedItem?.guid ?: selected,
                selectedTitle = when {
                    it.smartMode && selectedItem == null -> "اتصال هوشمند"
                    selectedItem != null -> selectedItem.remarks
                    else -> "انتخاب سرور"
                },
            )
        }
    }

    private fun refreshSelected() {
        val guid = MmkvManager.getSelectServer()
        val cfg = guid?.let { MmkvManager.decodeServerConfig(it) }
        _ui.update {
            it.copy(
                selectedGuid = guid,
                selectedTitle = cfg?.remarks?.ifBlank { "سرور" } ?: it.selectedTitle,
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
                selectedTitle = if (smartLabel) "اتصال هوشمند · ${cfg?.remarks.orEmpty()}"
                else cfg?.remarks?.ifBlank { "سرور" } ?: "سرور",
            )
        }
        refreshServers()
    }

    fun enableSmartMode() {
        _ui.update { it.copy(smartMode = true, selectedTitle = "اتصال هوشمند") }
        if (_ui.value.phase == TikNetConnPhase.Connected) {
            pendingSmartSwitch = true
            _ui.update { it.copy(smartPicking = true) }
            pingAllServers()
        }
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

    /** Power button: smart → ping then connect; manual → connect selected. */
    fun requestConnect() {
        _ui.update { it.copy(error = null) }
        if (_ui.value.smartMode) {
            _ui.update {
                it.copy(
                    phase = TikNetConnPhase.Connecting,
                    smartPicking = true,
                    busy = true,
                )
            }
            pendingSmartConnect = true
            pingAllServers()
            // Fallback if ping service never finishes
            viewModelScope.launch {
                delay(12_000)
                if (pendingSmartConnect) {
                    pendingSmartConnect = false
                    val best = pickBestGuid()
                    if (best != null) {
                        selectServer(best, smartLabel = true)
                        _events.tryEmit(TikNetUiEvent.StartVpn)
                    } else {
                        _ui.update {
                            it.copy(
                                smartPicking = false,
                                phase = TikNetConnPhase.Disconnected,
                                busy = false,
                                error = "سروری آماده نیست",
                            )
                        }
                    }
                }
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

    fun markConnecting() {
        _ui.update {
            it.copy(phase = TikNetConnPhase.Connecting, busy = true, error = null)
        }
    }

    fun markDisconnecting() {
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
                loadUser(silent = true)
                _ui.update { it.copy(busy = false, syncMessage = "$n کانفیگ وارد شد") }
            } catch (e: Exception) {
                _ui.update {
                    it.copy(busy = false, syncMessage = e.message ?: "همگام‌سازی ناموفق")
                }
            }
        }
    }

    fun loadUser(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _ui.update { it.copy(busy = true) }
            try {
                val ctx = getApplication<Application>()
                val base = TikNetPrefs.getBaseUrl(ctx) ?: return@launch
                val token = TikNetPrefs.getAccessToken(ctx) ?: return@launch
                val (me, supportTg) = withContext(Dispatchers.IO) { TikNetApi.enrichMe(base, token) }
                val publicCfg = withContext(Dispatchers.IO) {
                    runCatching { TikNetApi.getPublicConfig(base) }.getOrNull()
                }
                TikNetPrefs.saveCachedProfile(ctx, me)
                _ui.update {
                    it.copy(
                        user = me,
                        busy = false,
                        error = null,
                        telegramSupport = supportTg ?: me.supportTelegram,
                        shopUrl = if (publicCfg?.shopEnabled == true) publicCfg.shopUrl else null,
                        shopLabel = publicCfg?.shopLabel,
                    )
                }
            } catch (e: Exception) {
                val cached = TikNetPrefs.getCachedProfile(getApplication())
                _ui.update {
                    it.copy(
                        user = cached ?: it.user,
                        busy = false,
                        telegramSupport = cached?.supportTelegram ?: it.telegramSupport,
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
        if (q.isEmpty()) return all
        return all.filter {
            it.appName.contains(q, ignoreCase = true) || it.packageName.contains(q, ignoreCase = true)
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
                    it.copy(notificationsLoading = false, syncMessage = e.message ?: "خطا در اعلان‌ها")
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
                _ui.update { it.copy(faqLoading = false, faq = emptyList()) }
            }
        }
    }

    fun runDiagnostics() {
        viewModelScope.launch {
            _ui.update { it.copy(diagnosticsLoading = true, diagnostics = emptyList()) }
            val items = withContext(Dispatchers.IO) { collectDiagnostics(getApplication()) }
            _ui.update { it.copy(diagnostics = items, diagnosticsLoading = false) }
        }
    }

    fun openSettingsTarget(action: String?) {
        val ctx = getApplication<Application>()
        val intent = when (action) {
            "date" -> Intent(Settings.ACTION_DATE_SETTINGS)
            "vpn" -> Intent("android.net.vpn.SETTINGS")
            "wireless" -> Intent(Settings.ACTION_WIRELESS_SETTINGS)
            "airplane" -> Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
            "battery" -> Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            "private_dns" -> Intent(Settings.ACTION_WIRELESS_SETTINGS)
            else -> Intent(Settings.ACTION_SETTINGS)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { ctx.startActivity(intent) }
    }

    fun logout() {
        TikNetPrefs.clearSession(getApplication())
    }

    fun clearSyncMessage() {
        _ui.update { it.copy(syncMessage = null) }
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

        private fun collectDiagnostics(ctx: Context): List<TikNetDiagItem> {
            val list = mutableListOf<TikNetDiagItem>()
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val net = cm.activeNetwork
            val caps = net?.let { cm.getNetworkCapabilities(it) }

            val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            val validated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
            val wifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            val cell = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
            val transportLabel = when {
                wifi -> "وای‌فای"
                cell -> "داده موبایل"
                caps == null -> "بدون شبکه"
                else -> "سایر"
            }
            list += TikNetDiagItem(
                "نوع شبکه",
                transportLabel,
                hasInternet,
                "wireless",
            )
            list += TikNetDiagItem(
                "شبکه فعال",
                if (hasInternet) "اتصال اینترنت برقرار است" else "اینترنت در دسترس نیست",
                hasInternet,
                "wireless",
            )
            list += TikNetDiagItem(
                "اعتبارسنجی اتصال",
                if (validated) "سیستم اتصال را معتبر می‌داند" else "اتصال معتبرسازی نشده",
                validated || !hasInternet,
                "wireless",
            )

            val airplane = Settings.Global.getInt(ctx.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
            list += TikNetDiagItem(
                "حالت هواپیما",
                if (airplane) "روشن است — خاموشش کنید" else "خاموش",
                !airplane,
                "airplane",
            )

            val vpn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
            list += TikNetDiagItem(
                "VPN فعال",
                if (vpn) "یک VPN (احتمالاً همین اپ یا دیگری) فعال است" else "VPN فعالی دیده نمی‌شود",
                true,
                "vpn",
            )

            val privateDnsMode = runCatching {
                Settings.Global.getString(ctx.contentResolver, "private_dns_mode")
            }.getOrNull().orEmpty()
            val privateDnsHost = runCatching {
                Settings.Global.getString(ctx.contentResolver, "private_dns_specifier")
            }.getOrNull().orEmpty()
            if (privateDnsMode.isNotBlank()) {
                val dnsDetail = when (privateDnsMode.lowercase()) {
                    "off" -> "خاموش"
                    "opportunistic" -> "خودکار (opportunistic)"
                    "hostname" -> "خصوصی: ${privateDnsHost.ifBlank { "—" }}"
                    else -> privateDnsMode
                }
                list += TikNetDiagItem(
                    "DNS خصوصی",
                    dnsDetail,
                    privateDnsMode.lowercase() != "hostname" || privateDnsHost.isNotBlank(),
                    "private_dns",
                )
            }

            val pm = ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val ignoringBattery = pm?.isIgnoringBatteryOptimizations(ctx.packageName) == true
            list += TikNetDiagItem(
                "بهینه‌سازی باتری",
                if (ignoringBattery) {
                    "محدودیت باتری برای TikNet برداشته شده"
                } else {
                    "ممکن است سیستم اتصال پس‌زمینه را محدود کند — در تنظیمات بررسی کنید"
                },
                ignoringBattery,
                "battery",
            )

            val hosts = listOf("www.gstatic.com", "dns.google", "one.one.one.one", "cloudflare.com")
            val dnsResults = hosts.map { host ->
                host to runCatching {
                    InetAddress.getByName(host)
                    true
                }.getOrDefault(false)
            }
            val dnsOkCount = dnsResults.count { it.second }
            list += TikNetDiagItem(
                "DNS (چند میزبان)",
                if (dnsOkCount == hosts.size) {
                    "رزولوشن همه میزبان‌ها موفق"
                } else {
                    "موفق: $dnsOkCount از ${hosts.size} — ${dnsResults.filter { !it.second }.joinToString { it.first }}"
                },
                dnsOkCount > 0,
                "wireless",
            )

            val httpOk = runCatching {
                val c = (URL("https://www.gstatic.com/generate_204").openConnection() as HttpURLConnection)
                c.connectTimeout = 5000
                c.readTimeout = 5000
                c.instanceFollowRedirects = false
                c.requestMethod = "GET"
                c.connect()
                val code = c.responseCode
                c.disconnect()
                code == 204 || code in 200..399
            }.getOrDefault(false)
            list += TikNetDiagItem(
                "HTTP 204",
                if (httpOk) "پاسخ موفق از generate_204" else "عدم دسترسی به generate_204",
                httpOk,
                "wireless",
            )

            val autoTime = Settings.Global.getInt(ctx.contentResolver, Settings.Global.AUTO_TIME, 0) == 1
            list += TikNetDiagItem(
                "زمان خودکار",
                if (autoTime) "فعال" else "غیرفعال — ممکن است TLS مشکل داشته باشد",
                autoTime,
                "date",
            )
            return list
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
