package com.v2ray.ang.ui.tiknet

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.AngApplication
import com.v2ray.ang.core.LauncherManager
import com.v2ray.ang.enums.PermissionType
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.tiknet.TikNetPrefs
import com.v2ray.ang.ui.base.HelperBaseComponentActivity
import com.v2ray.ang.ui.compose.ThemeManager
import kotlinx.coroutines.flow.collectLatest

class TikNetMainActivity : HelperBaseComponentActivity() {

    private val viewModel: TikNetMainViewModel by viewModels {
        TikNetMainViewModel.Companion.Factory(application as AngApplication)
    }

    private val requestVpnPermission =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                viewModel.markConnecting()
                LauncherManager.startService(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!TikNetPrefs.isLoggedIn(this)) {
            startActivity(Intent(this, TikNetLoginActivity::class.java))
            finish()
            return
        }
        ThemeManager.setThemeMode("2")
        super.onCreate(savedInstanceState)
        checkAndRequestPermission(PermissionType.POST_NOTIFICATIONS) {}
    }

    @Composable
    override fun ScreenContent() {
        BackHandler { moveTaskToBack(false) }
        val state by viewModel.ui.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.events.collectLatest { event ->
                when (event) {
                    TikNetUiEvent.StartVpn -> startVpnOrService()
                    is TikNetUiEvent.Toast -> {
                        // temporarily reuse syncMessage for toast text on account/connect
                        viewModel.ui.value.let {
                            // no-op: toast messages also set error/sync in requestConnect
                        }
                    }
                }
            }
        }

        TikNetShell(
            state = state,
            viewModel = viewModel,
            onToggleConnect = { toggleConnection() },
            onSelectServer = { guid ->
                val wasConnected = state.phase == TikNetConnPhase.Connected
                viewModel.selectServer(guid)
                if (wasConnected) {
                    viewModel.markConnecting()
                    LauncherManager.stopService(this)
                    window.decorView.postDelayed({ startVpnOrService() }, 450)
                }
            },
            onSmartMode = { viewModel.enableSmartMode() },
            onPingAll = { viewModel.pingAllServers() },
            onSync = { viewModel.syncSubscription() },
            onLogout = {
                if (state.phase == TikNetConnPhase.Connected) {
                    LauncherManager.stopService(this)
                }
                viewModel.logout()
                startActivity(Intent(this, TikNetLoginActivity::class.java))
                finish()
            },
            onFilterChangedRestart = {
                if (state.phase == TikNetConnPhase.Connected) {
                    viewModel.markConnecting()
                    LauncherManager.stopService(this)
                    window.decorView.postDelayed({ startVpnOrService() }, 450)
                }
            },
        )
    }

    private fun toggleConnection() {
        when (viewModel.ui.value.phase) {
            TikNetConnPhase.Connected -> {
                viewModel.markDisconnecting()
                LauncherManager.stopService(this)
            }
            TikNetConnPhase.Disconnected -> {
                viewModel.requestConnect()
            }
            else -> Unit
        }
    }

    private fun startVpnOrService() {
        viewModel.markConnecting()
        if (SettingsManager.isVpnMode()) {
            val intent = VpnService.prepare(this)
            if (intent == null) {
                LauncherManager.startService(this)
            } else {
                requestVpnPermission.launch(intent)
            }
        } else {
            LauncherManager.startService(this)
        }
    }
}
