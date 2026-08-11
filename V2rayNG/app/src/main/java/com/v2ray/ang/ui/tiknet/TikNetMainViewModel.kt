package com.v2ray.ang.ui.tiknet

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.v2ray.ang.AngApplication
import com.v2ray.ang.AppConfig
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.helper.MessageHelper
import com.v2ray.ang.tiknet.TikNetApi
import com.v2ray.ang.tiknet.TikNetPrefs
import com.v2ray.ang.tiknet.TikNetSync
import com.v2ray.ang.tiknet.TikNetUserInfo
import com.v2ray.ang.ui.main.MainRepository
import com.v2ray.ang.ui.main.MainServiceEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val statusMessage: String = "",
    val selectedGuid: String? = null,
    val selectedTitle: String = "انتخاب سرور",
    val servers: List<TikNetServerItem> = emptyList(),
    val user: TikNetUserInfo? = null,
    val busy: Boolean = false,
    val syncMessage: String? = null,
    val error: String? = null,
    val currentDelayText: String = "",
    val appVersion: String = BuildConfig.VERSION_NAME,
)

class TikNetMainViewModel(
    application: Application,
    private val repository: MainRepository,
) : AndroidViewModel(application) {

    private val _ui = MutableStateFlow(TikNetMainUiState())
    val ui: StateFlow<TikNetMainUiState> = _ui.asStateFlow()

    init {
        refreshServers()
        observeService()
        MessageHelper.sendMsg2Service(application, AppConfig.MSG_REGISTER_CLIENT, "")
        viewModelScope.launch { loadUser(silent = true) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { TikNetSync.syncPersonalSubscription(getApplication()) }
            withContext(Dispatchers.Main) { refreshServers() }
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
                                statusMessage = "",
                                busy = false,
                            )
                        }
                        refreshSelected()
                        MessageHelper.sendMsg2Service(
                            getApplication(),
                            AppConfig.MSG_MEASURE_DELAY,
                            "",
                        )
                    }

                    MainServiceEvent.StateNotRunning,
                    MainServiceEvent.StateStopSuccess -> {
                        _ui.update {
                            it.copy(
                                phase = TikNetConnPhase.Disconnected,
                                statusMessage = "",
                                busy = false,
                                currentDelayText = "",
                            )
                        }
                    }

                    is MainServiceEvent.StateStartFailure -> {
                        _ui.update {
                            it.copy(
                                phase = TikNetConnPhase.Disconnected,
                                busy = false,
                                error = event.errorMessage.ifBlank { "اتصال ناموفق" },
                            )
                        }
                    }

                    is MainServiceEvent.MeasureDelaySuccess -> {
                        _ui.update { it.copy(currentDelayText = event.content) }
                    }

                    else -> Unit
                }
            }
        }
    }

    fun refreshServers() {
        val guids = linkedSetOf<String>()
        guids.addAll(MmkvManager.decodeServerList(TikNetPrefs.TIKNET_SUB_GUID))
        // Include any catalog imports
        MmkvManager.decodeSubsList().forEach { subId ->
            if (subId.startsWith("tiknet-")) {
                guids.addAll(MmkvManager.decodeServerList(subId))
            }
        }
        if (guids.isEmpty()) {
            guids.addAll(MmkvManager.decodeAllServerList())
        }
        val selected = MmkvManager.getSelectServer()
        val items = guids.mapNotNull { guid ->
            val cfg = MmkvManager.decodeServerConfig(guid) ?: return@mapNotNull null
            val aff = MmkvManager.decodeServerAffiliationInfo(guid)
            val ping = aff?.testDelayMillis?.takeIf { it > 0 }
            TikNetServerItem(
                guid = guid,
                remarks = cfg.remarks.ifBlank { "سرور" },
                protocolLabel = protocolLabel(cfg),
                pingMs = ping,
            )
        }
        val selectedItem = items.firstOrNull { it.guid == selected }
            ?: items.firstOrNull()
        if (selectedItem != null && selected != selectedItem.guid) {
            MmkvManager.setSelectServer(selectedItem.guid)
        }
        _ui.update {
            it.copy(
                servers = items,
                selectedGuid = selectedItem?.guid,
                selectedTitle = selectedItem?.remarks ?: "انتخاب سرور",
            )
        }
    }

    private fun refreshSelected() {
        val guid = MmkvManager.getSelectServer()
        val cfg = guid?.let { MmkvManager.decodeServerConfig(it) }
        _ui.update {
            it.copy(
                selectedGuid = guid,
                selectedTitle = cfg?.remarks?.ifBlank { "سرور" } ?: "انتخاب سرور",
            )
        }
    }

    fun selectServer(guid: String) {
        MmkvManager.setSelectServer(guid)
        refreshServers()
    }

    fun markConnecting() {
        _ui.update {
            it.copy(phase = TikNetConnPhase.Connecting, busy = true, error = null)
        }
    }

    fun markDisconnecting() {
        _ui.update {
            it.copy(phase = TikNetConnPhase.Disconnecting, busy = true, error = null)
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
                _ui.update {
                    it.copy(busy = false, syncMessage = "$n کانفیگ وارد شد")
                }
                loadUser(silent = true)
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
                val me = withContext(Dispatchers.IO) { TikNetApi.getMe(base, token) }
                TikNetPrefs.saveCachedProfile(ctx, me)
                _ui.update { it.copy(user = me, busy = false, error = null) }
            } catch (e: Exception) {
                val cached = TikNetPrefs.getCachedProfile(getApplication())
                _ui.update {
                    it.copy(
                        user = cached ?: it.user,
                        busy = false,
                        error = if (silent) it.error else (e.message ?: "خطا در خواندن حساب"),
                    )
                }
            }
        }
    }

    fun clearSyncMessage() {
        _ui.update { it.copy(syncMessage = null) }
    }

    fun logout() {
        TikNetPrefs.clearSession(getApplication())
    }

    override fun onCleared() {
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
