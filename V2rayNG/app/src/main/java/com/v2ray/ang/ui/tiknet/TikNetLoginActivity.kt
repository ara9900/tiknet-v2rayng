package com.v2ray.ang.ui.tiknet

import android.content.Intent
import android.os.Bundle
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.tiknet.TikNetSync
import com.v2ray.ang.tiknet.TikNetDevice
import com.v2ray.ang.tiknet.TikNetQrCredentials
import com.v2ray.ang.tiknet.TikNetQrLogin
import com.v2ray.ang.tiknet.TikNetQrLoginToken
import com.v2ray.ang.tiknet.TikNetQrSubscriptionLink
import com.v2ray.ang.tiknet.TikNetPublicConfig
import com.v2ray.ang.tiknet.TikNetPrefs
import com.v2ray.ang.tiknet.TikNetErrors
import com.v2ray.ang.tiknet.TikNetAppUpdateController
import com.v2ray.ang.tiknet.TikNetApi
import com.v2ray.ang.tiknet.TikNetApiException
import com.v2ray.ang.R
import com.v2ray.ang.ui.base.HelperBaseComponentActivity
import com.v2ray.ang.ui.compose.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val LoginBg = Color(0xFF0D0D0D)
private val LoginPrimary = Color(0xFF6366F1)
private val LoginCyan = Color(0xFF22D3EE)
private val LoginOnBg = Color(0xFFE8E8E8)
private val LoginMuted = Color(0xFF9E9E9E)
private val LoginDanger = Color(0xFFEF4444)
private val LoginField = Color(0x0AFFFFFF)

class TikNetLoginActivity : HelperBaseComponentActivity() {

    private val pendingLinkState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.setThemeMode("2")
        pendingLinkState.value = extractLink(intent)
        super.onCreate(savedInstanceState)
        // Must call super.onCreate before finish()/return or Android throws SuperNotCalledException.
        if (TikNetPrefs.isLoggedIn(this)) {
            goMain()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingLinkState.value = extractLink(intent)
    }

    @Composable
    override fun ScreenContent() {
        val pendingLink by pendingLinkState
        TikNetLoginScreen(
            savedUsername = TikNetPrefs.getUsername(this).orEmpty(),
            pendingLink = pendingLink,
            onPendingLinkConsumed = { pendingLinkState.value = null },
            onScanQr = { cb -> launchQRCodeScanner(cb) },
            onLoggedIn = { goMain() },
        )
    }

    private fun extractLink(intent: Intent?): String? {
        val data = intent?.data?.toString()?.trim().orEmpty()
        if (data.isNotEmpty() && TikNetQrLogin.isLoginDeepLink(data)) return data
        val extra = intent?.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
        if (extra.isNotEmpty() && TikNetQrLogin.isLoginDeepLink(extra)) return extra
        return null
    }

    private fun goMain() {
        startActivity(Intent(this, TikNetMainActivity::class.java))
        finish()
    }
}

@Composable
private fun TikNetLoginScreen(
    savedUsername: String,
    pendingLink: String?,
    onPendingLinkConsumed: () -> Unit,
    onScanQr: ((String?) -> Unit) -> Unit,
    onLoggedIn: () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    var username by remember { mutableStateOf(savedUsername) }
    var password by remember { mutableStateOf("") }
    var obscure by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var panelReady by remember { mutableStateOf(false) }
    var panelReachable by remember { mutableStateOf(true) }
    var resolvedBase by remember { mutableStateOf<String?>(null) }
    var shop by remember { mutableStateOf(TikNetPublicConfig()) }

    suspend fun finishLogin(base: String, accessToken: String, userHint: String?) {
        TikNetPrefs.saveSession(context, base, accessToken, userHint)
        runCatching {
            val (me, _) = TikNetApi.enrichMe(base, accessToken)
            TikNetPrefs.saveCachedProfile(context, me)
            if (userHint.isNullOrBlank() && me.username.isNotBlank()) {
                TikNetPrefs.saveSession(context, base, accessToken, me.username)
            }
        }
        runCatching { TikNetSync.syncPersonalSubscription(context) }
        runCatching { TikNetDevice.registerIfLoggedIn(context) }
        try {
            withContext(Dispatchers.IO) {
                TikNetAppUpdateController.check(context)
            }
        } catch (_: Exception) {
            // Best-effort only; login must continue even if update lookup fails.
        }
        withContext(Dispatchers.Main) { onLoggedIn() }
    }

    fun runAuth(block: suspend () -> Unit) {
        scope.launch {
            loading = true
            error = null
            try {
                withContext(Dispatchers.IO) { block() }
            } catch (e: Exception) {
                error = TikNetErrors.message(e, "ورود ناموفق")
                loading = false
            }
        }
    }

    fun doPasswordLogin() {
        val u = username.trim()
        val p = password
        if (u.isEmpty() || p.isEmpty()) {
            error = "نام کاربری و رمز عبور را وارد کنید."
            return
        }
        runAuth {
            val base = resolvedBase ?: TikNetApi.resolveBaseUrl(context)
            val login = TikNetApi.login(base, u, p)
            if (login.accessToken.isBlank()) throw TikNetApiException("empty token")
            finishLogin(base, login.accessToken, u)
        }
    }

    fun consumePayload(raw: String) {
        when (val payload = TikNetQrLogin.parse(raw)) {
            null -> error = "کد QR ورود نامعتبر است."
            is TikNetQrSubscriptionLink ->
                error = "این QR برای ورود نیست. لطفاً QR ورود حساب را اسکن کنید."
            is TikNetQrLoginToken -> runAuth {
                val base = payload.panelUrl?.trim()?.trimEnd('/')
                    ?: resolvedBase
                    ?: TikNetApi.resolveBaseUrl(context)
                val login = TikNetApi.loginWithToken(base, payload.token)
                if (login.accessToken.isBlank()) throw TikNetApiException("empty token")
                finishLogin(base, login.accessToken, null)
            }
            is TikNetQrCredentials -> {
                username = payload.username
                password = payload.password
                runAuth {
                    val base = payload.panelUrl?.trim()?.trimEnd('/')
                        ?: resolvedBase
                        ?: TikNetApi.resolveBaseUrl(context)
                    val login = TikNetApi.login(base, payload.username, payload.password)
                    if (login.accessToken.isBlank()) throw TikNetApiException("empty token")
                    finishLogin(base, login.accessToken, payload.username)
                }
            }
        }
    }

    fun doQrLogin() {
        if (loading) return
        onScanQr { raw ->
            if (!raw.isNullOrBlank()) consumePayload(raw)
        }
    }

    LaunchedEffect(Unit) {
        try {
            val base = withContext(Dispatchers.IO) { TikNetApi.resolveBaseUrl(context) }
            resolvedBase = base
            panelReachable = base.isNotBlank()
            if (base.isNotBlank()) {
                shop = withContext(Dispatchers.IO) {
                    runCatching { TikNetApi.getPublicConfig(base) }.getOrDefault(TikNetPublicConfig())
                }
            }
        } catch (_: Exception) {
            panelReachable = false
        } finally {
            panelReady = true
        }
    }

    LaunchedEffect(pendingLink, panelReady, loading) {
        val link = pendingLink ?: return@LaunchedEffect
        if (!panelReady || loading) return@LaunchedEffect
        onPendingLinkConsumed()
        consumePayload(link)
    }

    val formEnabled = panelReady && panelReachable && !loading
    val pulse = rememberInfiniteTransition(label = "loginPulse")
    val glow by pulse.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(LoginBg),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(LoginPrimary.copy(alpha = 0.34f), Color.Transparent),
                    ),
                ),
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .fillMaxSize(0.85f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(LoginCyan.copy(alpha = 0.14f), Color.Transparent),
                    ),
                ),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                    ),
                ),
        )

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(118.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                LoginPrimary.copy(alpha = 0.25f + glow * 0.2f),
                                Color.White.copy(alpha = 0.06f),
                            ),
                        ),
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                    .padding(18.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.tiknet_shield),
                    contentDescription = "TikNet",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "TikNet",
                color = LoginOnBg,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text("ورود امن به حساب شما", color = LoginMuted, fontSize = 14.sp)
            Spacer(Modifier.height(28.dp))

            when {
                !panelReady -> {
                    CircularProgressIndicator(color = LoginPrimary, modifier = Modifier.padding(28.dp))
                }
                !panelReachable -> {
                    GlassCard {
                        Text(
                            "اتصال به پنل برقرار نشد. اتصال اینترنت را بررسی کنید.",
                            color = LoginDanger,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                else -> {
                    GlassCard {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            enabled = formEnabled,
                            singleLine = true,
                            label = { Text("نام کاربری") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Person, contentDescription = null, tint = LoginMuted)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            colors = loginFieldColors(),
                            shape = RoundedCornerShape(14.dp),
                        )
                        Spacer(Modifier.height(14.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            enabled = formEnabled,
                            singleLine = true,
                            label = { Text("رمز عبور") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Lock, contentDescription = null, tint = LoginMuted)
                            },
                            trailingIcon = {
                                IconButton(onClick = { obscure = !obscure }) {
                                    Icon(
                                        if (obscure) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                        contentDescription = null,
                                        tint = LoginMuted,
                                    )
                                }
                            },
                            visualTransformation = if (obscure) {
                                PasswordVisualTransformation()
                            } else {
                                VisualTransformation.None
                            },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { if (formEnabled) doPasswordLogin() },
                            ),
                            colors = loginFieldColors(),
                            shape = RoundedCornerShape(14.dp),
                        )
                        if (!error.isNullOrBlank()) {
                            Spacer(Modifier.height(14.dp))
                            Text(
                                error!!,
                                color = LoginDanger,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Spacer(Modifier.height(22.dp))
                        Button(
                            onClick = { doPasswordLogin() },
                            enabled = formEnabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LoginPrimary,
                                contentColor = Color.White,
                                disabledContainerColor = LoginPrimary.copy(alpha = 0.4f),
                            ),
                        ) {
                            if (loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.4.dp,
                                )
                            } else {
                                Text("ورود", fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { doQrLogin() },
                            enabled = formEnabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, LoginPrimary.copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LoginPrimary),
                        ) {
                            Icon(Icons.Outlined.QrCodeScanner, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("ورود با QR")
                        }
                        if (shop.shopEnabled && !shop.shopUrl.isNullOrBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { runCatching { uriHandler.openUri(shop.shopUrl!!) } },
                                enabled = formEnabled,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LoginPrimary.copy(alpha = 0.22f),
                                    contentColor = LoginOnBg,
                                ),
                            ) {
                                Icon(Icons.Outlined.ShoppingBag, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text(shop.shopLabel?.takeIf { it.isNotBlank() } ?: "خرید و تمدید")
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GlassCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
            .padding(horizontal = 18.dp, vertical = 20.dp),
    ) {
        content()
    }
}

@Composable
private fun loginFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = LoginOnBg,
    unfocusedTextColor = LoginOnBg,
    disabledTextColor = LoginMuted,
    focusedBorderColor = LoginPrimary,
    unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
    disabledBorderColor = Color.White.copy(alpha = 0.05f),
    focusedLabelColor = LoginPrimary,
    unfocusedLabelColor = LoginMuted,
    cursorColor = LoginPrimary,
    focusedContainerColor = LoginField,
    unfocusedContainerColor = LoginField,
    disabledContainerColor = LoginField,
)
