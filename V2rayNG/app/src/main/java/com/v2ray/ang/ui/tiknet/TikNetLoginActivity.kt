package com.v2ray.ang.ui.tiknet

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.v2ray.ang.R
import com.v2ray.ang.tiknet.TikNetApi
import com.v2ray.ang.tiknet.TikNetApiException
import com.v2ray.ang.tiknet.TikNetPrefs
import com.v2ray.ang.tiknet.TikNetSync
import androidx.appcompat.app.AppCompatActivity
import com.v2ray.ang.ui.main.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TikNetLoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (TikNetPrefs.isLoggedIn(this)) {
            goMain()
            return
        }
        setContentView(R.layout.activity_tiknet_login)

        val username = findViewById<EditText>(R.id.tiknet_username)
        val password = findViewById<EditText>(R.id.tiknet_password)
        val btn = findViewById<Button>(R.id.tiknet_login_btn)
        val progress = findViewById<ProgressBar>(R.id.tiknet_progress)
        val error = findViewById<TextView>(R.id.tiknet_error)

        btn.setOnClickListener {
            val u = username.text?.toString()?.trim().orEmpty()
            val p = password.text?.toString().orEmpty()
            if (u.isEmpty() || p.isEmpty()) {
                error.visibility = View.VISIBLE
                error.text = getString(R.string.tiknet_login_empty)
                return@setOnClickListener
            }
            btn.isEnabled = false
            progress.visibility = View.VISIBLE
            error.visibility = View.GONE
            lifecycleScope.launch {
                try {
                    val base = withContext(Dispatchers.IO) { TikNetApi.resolveBaseUrl(this@TikNetLoginActivity) }
                    val login = withContext(Dispatchers.IO) { TikNetApi.login(base, u, p) }
                    if (login.accessToken.isBlank()) throw TikNetApiException("empty token")
                    TikNetPrefs.saveSession(this@TikNetLoginActivity, base, login.accessToken, u)
                    withContext(Dispatchers.IO) {
                        runCatching { TikNetSync.syncPersonalSubscription(this@TikNetLoginActivity) }
                    }
                    goMain()
                } catch (e: Exception) {
                    error.visibility = View.VISIBLE
                    error.text = e.message ?: getString(R.string.tiknet_login_failed)
                    btn.isEnabled = true
                    progress.visibility = View.GONE
                }
            }
        }
    }

    private fun goMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
