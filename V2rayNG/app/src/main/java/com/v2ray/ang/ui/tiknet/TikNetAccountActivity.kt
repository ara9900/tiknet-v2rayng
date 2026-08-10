package com.v2ray.ang.ui.tiknet

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.v2ray.ang.R
import com.v2ray.ang.tiknet.TikNetApi
import com.v2ray.ang.tiknet.TikNetCatalogServer
import com.v2ray.ang.tiknet.TikNetPrefs
import com.v2ray.ang.tiknet.TikNetSync
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TikNetAccountActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!TikNetPrefs.isLoggedIn(this)) {
            startActivity(Intent(this, TikNetLoginActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_tiknet_account)

        val info = findViewById<TextView>(R.id.tiknet_account_info)
        val status = findViewById<TextView>(R.id.tiknet_account_status)
        val progress = findViewById<ProgressBar>(R.id.tiknet_account_progress)
        val syncBtn = findViewById<Button>(R.id.tiknet_sync_btn)
        val catalogBtn = findViewById<Button>(R.id.tiknet_catalog_btn)
        val logoutBtn = findViewById<Button>(R.id.tiknet_logout_btn)

        fun setBusy(busy: Boolean) {
            progress.visibility = if (busy) View.VISIBLE else View.GONE
            syncBtn.isEnabled = !busy
            catalogBtn.isEnabled = !busy
            logoutBtn.isEnabled = !busy
        }

        lifecycleScope.launch {
            setBusy(true)
            try {
                val base = TikNetPrefs.getBaseUrl(this@TikNetAccountActivity)!!
                val token = TikNetPrefs.getAccessToken(this@TikNetAccountActivity)!!
                val me = withContext(Dispatchers.IO) { TikNetApi.getMe(base, token) }
                info.text = buildString {
                    appendLine("کاربر: ${me.fullName ?: me.username}")
                    appendLine("پلن: ${me.planName ?: "—"}")
                    appendLine("انقضا: ${me.expireDate ?: "—"}")
                    appendLine("روز باقی‌مانده: ${me.daysRemaining ?: "—"}")
                    appendLine("پنل: $base")
                }
            } catch (e: Exception) {
                info.text = getString(R.string.tiknet_account_load_failed, e.message ?: "")
            } finally {
                setBusy(false)
            }
        }

        syncBtn.setOnClickListener {
            lifecycleScope.launch {
                setBusy(true)
                status.text = getString(R.string.tiknet_syncing)
                try {
                    val n = withContext(Dispatchers.IO) {
                        TikNetSync.syncPersonalSubscription(this@TikNetAccountActivity)
                    }
                    status.text = getString(R.string.tiknet_sync_ok, n)
                } catch (e: Exception) {
                    status.text = e.message ?: getString(R.string.tiknet_sync_failed)
                } finally {
                    setBusy(false)
                }
            }
        }

        catalogBtn.setOnClickListener {
            lifecycleScope.launch {
                setBusy(true)
                try {
                    val base = TikNetPrefs.getBaseUrl(this@TikNetAccountActivity)!!
                    val token = TikNetPrefs.getAccessToken(this@TikNetAccountActivity)!!
                    val servers = withContext(Dispatchers.IO) { TikNetApi.getServerCatalog(base, token) }
                    setBusy(false)
                    if (servers.isEmpty()) {
                        status.text = getString(R.string.tiknet_catalog_empty)
                        return@launch
                    }
                    showCatalogPicker(servers, status)
                } catch (e: Exception) {
                    setBusy(false)
                    status.text = e.message
                }
            }
        }

        logoutBtn.setOnClickListener {
            TikNetPrefs.clearSession(this)
            startActivity(Intent(this, TikNetLoginActivity::class.java))
            finish()
        }
    }

    private fun showCatalogPicker(servers: List<TikNetCatalogServer>, status: TextView) {
        val labels = servers.map { s ->
            val cc = s.countryCode?.uppercase()?.let { "[$it] " } ?: ""
            "$cc${s.name}"
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.tiknet_catalog_btn)
            .setItems(labels) { _, which ->
                val server = servers[which]
                lifecycleScope.launch {
                    status.text = getString(R.string.tiknet_syncing)
                    try {
                        val n = withContext(Dispatchers.IO) {
                            TikNetSync.importCatalogServer(
                                this@TikNetAccountActivity,
                                server.id,
                                server.name,
                            )
                        }
                        status.text = getString(R.string.tiknet_catalog_imported, n, server.name)
                    } catch (e: Exception) {
                        status.text = e.message
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
