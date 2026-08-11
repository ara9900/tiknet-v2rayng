package com.v2ray.ang.ui.tiknet

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private val TikBg = Color(0xFF0D0D0D)
private val TikSurface = Color(0xFF1E1E1E)
private val TikSurface2 = Color(0xFF252525)
private val TikPrimary = Color(0xFF6366F1)
private val TikConnected = Color(0xFF22C55E)
private val TikConnecting = Color(0xFFF59E0B)
private val TikDisconnected = Color(0xFF9E9E9E)
private val TikOnBg = Color(0xFFE8E8E8)
private val TikMuted = Color(0xFF9E9E9E)
private val TikBorder = Color(0x14FFFFFF)
private val TikDanger = Color(0xFFEF4444)

enum class TikNetTab { Connect, Details, Filter, Account }

@Composable
fun TikNetShell(
    state: TikNetMainUiState,
    onToggleConnect: () -> Unit,
    onSelectServer: (String) -> Unit,
    onSync: () -> Unit,
    onRefreshUser: () -> Unit,
    onLogout: () -> Unit,
    filterContent: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        var tab by remember { mutableStateOf(TikNetTab.Connect) }
        Scaffold(
            containerColor = TikBg,
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF121212),
                    contentColor = TikOnBg,
                    tonalElevation = 0.dp,
                    modifier = Modifier.navigationBarsPadding(),
                ) {
                    val items = listOf(
                        Triple(TikNetTab.Connect, "اتصال", Icons.Outlined.Shield),
                        Triple(TikNetTab.Details, "جزئیات", Icons.Outlined.Analytics),
                        Triple(TikNetTab.Filter, "فیلتر اپ‌ها", Icons.Outlined.Apps),
                        Triple(TikNetTab.Account, "حساب من", Icons.Outlined.Person),
                    )
                    items.forEach { (t, label, icon) ->
                        val selected = tab == t
                        NavigationBarItem(
                            selected = selected,
                            onClick = { tab = t },
                            icon = {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (selected) TikPrimary.copy(alpha = 0.25f) else Color.Transparent)
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = label,
                                        tint = if (selected) TikPrimary else TikMuted,
                                    )
                                }
                            },
                            label = {
                                Text(
                                    label,
                                    fontSize = 11.sp,
                                    color = if (selected) TikOnBg else TikMuted,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent,
                                selectedIconColor = TikPrimary,
                                unselectedIconColor = TikMuted,
                                selectedTextColor = TikOnBg,
                                unselectedTextColor = TikMuted,
                            ),
                        )
                    }
                }
            },
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(TikBg),
            ) {
                when (tab) {
                    TikNetTab.Connect -> TikNetConnectionTab(
                        state = state,
                        onToggleConnect = onToggleConnect,
                        onSelectServer = onSelectServer,
                    )
                    TikNetTab.Details -> TikNetDetailsTab(state)
                    TikNetTab.Filter -> filterContent()
                    TikNetTab.Account -> TikNetAccountTab(
                        state = state,
                        onSync = onSync,
                        onRefreshUser = onRefreshUser,
                        onLogout = onLogout,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TikNetConnectionTab(
    state: TikNetMainUiState,
    onToggleConnect: () -> Unit,
    onSelectServer: (String) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    val statusColor by animateColorAsState(
        when (state.phase) {
            TikNetConnPhase.Connected -> TikConnected
            TikNetConnPhase.Connecting, TikNetConnPhase.Disconnecting -> TikConnecting
            TikNetConnPhase.Disconnected -> TikDisconnected
        },
        label = "status",
    )
    val statusLabel = when (state.phase) {
        TikNetConnPhase.Connected -> "متصل به اینترنت"
        TikNetConnPhase.Connecting -> "در حال اتصال…"
        TikNetConnPhase.Disconnecting -> "در حال قطع…"
        TikNetConnPhase.Disconnected -> "قطع شده"
    }
    val statusHint = when (state.phase) {
        TikNetConnPhase.Connected -> "ترافیک شما از طریق VPN عبور می‌کند"
        TikNetConnPhase.Connecting -> "لطفاً چند ثانیه صبر کنید"
        TikNetConnPhase.Disconnecting -> "در حال قطع اتصال"
        TikNetConnPhase.Disconnected -> "برای اتصال، دکمه پایین را بزنید"
    }
    val btnHint = when (state.phase) {
        TikNetConnPhase.Connected -> "برای قطع اتصال بزنید"
        TikNetConnPhase.Connecting, TikNetConnPhase.Disconnecting -> "لطفاً صبر کنید…"
        TikNetConnPhase.Disconnected -> "برای اتصال بزنید"
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "v${state.appVersion}",
                color = TikMuted,
                fontSize = 12.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "اتصال امن",
                color = TikOnBg,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(40.dp))
        }

        Spacer(Modifier.height(16.dp))

        // Status hero
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(2.dp, statusColor.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(statusColor.copy(alpha = 0.12f), TikSurface),
                    ),
                )
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(statusLabel, color = statusColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(statusHint, color = TikMuted, fontSize = 13.sp)
                    if (state.phase == TikNetConnPhase.Connected) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "مقصد: ${state.selectedTitle}",
                            color = TikOnBg,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        when (state.phase) {
                            TikNetConnPhase.Connected -> Icons.Outlined.VerifiedUser
                            TikNetConnPhase.Connecting, TikNetConnPhase.Disconnecting -> Icons.Outlined.Refresh
                            TikNetConnPhase.Disconnected -> Icons.Outlined.Shield
                        },
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Server picker card
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(TikSurface)
                .clickable { showPicker = true }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.SwapVert, contentDescription = null, tint = TikPrimary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("سرور انتخابی", color = TikMuted, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    state.selectedTitle,
                    color = TikOnBg,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("اشتراک", color = TikMuted, fontSize = 11.sp)
            }
            if (state.phase == TikNetConnPhase.Connected) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(TikConnected.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text("VPN روشن", color = TikConnected, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(8.dp))
            }
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TikPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Text("T", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Announcement
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, TikConnected.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                .background(TikSurface)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "تیکنت، مسیری مطمئن به دنیای اینترنت",
                color = TikOnBg,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
            )
            Text("📢", fontSize = 18.sp)
        }

        Spacer(Modifier.height(36.dp))

        // Power button
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val glow = when (state.phase) {
                TikNetConnPhase.Connected -> TikConnected
                TikNetConnPhase.Connecting, TikNetConnPhase.Disconnecting -> TikConnecting
                TikNetConnPhase.Disconnected -> TikPrimary
            }
            Box(
                Modifier
                    .size(168.dp)
                    .clip(CircleShape)
                    .background(glow.copy(alpha = 0.15f))
                    .clickable(enabled = state.phase != TikNetConnPhase.Connecting && state.phase != TikNetConnPhase.Disconnecting) {
                        onToggleConnect()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(132.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(listOf(glow.copy(alpha = 0.55f), glow.copy(alpha = 0.9f))),
                        )
                        .border(3.dp, glow.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    when (state.phase) {
                        TikNetConnPhase.Connecting, TikNetConnPhase.Disconnecting ->
                            CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(42.dp))
                        TikNetConnPhase.Connected ->
                            Icon(Icons.Outlined.PowerSettingsNew, null, tint = Color.White, modifier = Modifier.size(56.dp))
                        TikNetConnPhase.Disconnected ->
                            Icon(Icons.Outlined.Shield, null, tint = Color.White, modifier = Modifier.size(56.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(btnHint, color = TikMuted, fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

        if (!state.error.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(state.error!!, color = TikDanger, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showPicker) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPicker = false },
            sheetState = sheetState,
            containerColor = TikSurface,
        ) {
            Text(
                "انتخاب سرور",
                Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                color = TikOnBg,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(420.dp),
            ) {
                items(state.servers, key = { it.guid }) { server ->
                    val selected = server.guid == state.selectedGuid
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) TikPrimary.copy(alpha = 0.18f) else TikSurface2)
                            .border(
                                1.dp,
                                if (selected) TikPrimary else TikBorder,
                                RoundedCornerShape(14.dp),
                            )
                            .clickable {
                                onSelectServer(server.guid)
                                showPicker = false
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(flagForRemarks(server.remarks), fontSize = 22.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(server.remarks, color = TikOnBg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(server.protocolLabel, color = TikPrimary, fontSize = 12.sp)
                        }
                        val ping = server.pingMs
                        if (ping != null) {
                            Text("${ping}ms", color = TikConnected, fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TikNetDetailsTab(state: TikNetMainUiState) {
    val connected = state.phase == TikNetConnPhase.Connected
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("جزئیات اتصال", color = TikOnBg, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        DetailCard("وضعیت") {
            Text(
                if (connected) "متصل" else "قطع",
                color = if (connected) TikConnected else TikMuted,
                fontWeight = FontWeight.Bold,
            )
            if (state.currentDelayText.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text("تأخیر: ${state.currentDelayText}", color = TikMuted, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        DetailCard("سرور و مسیر") {
            Text(state.selectedTitle, color = TikOnBg)
            Spacer(Modifier.height(4.dp))
            val proto = state.servers.firstOrNull { it.guid == state.selectedGuid }?.protocolLabel.orEmpty()
            if (proto.isNotBlank()) Text(proto, color = TikPrimary, fontSize = 13.sp)
        }
        Spacer(Modifier.height(12.dp))
        DetailCard("هسته") {
            Text(
                if (connected) "در حال اجرا" else "متوقف",
                color = TikMuted,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text("موتور امن TikNet", color = TikMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun DetailCard(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TikSurface)
            .padding(16.dp),
    ) {
        Text(title, color = TikMuted, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
fun TikNetFilterTab(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onOpenAdvanced: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(20.dp),
    ) {
        Text(
            "فیلتر اپ‌ها",
            color = TikOnBg,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(TikSurface)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("فیلتر اپ (Split Tunnel)", color = TikOnBg, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "فقط اپ‌های انتخاب‌شده از VPN عبور کنند",
                    color = TikMuted,
                    fontSize = 12.sp,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = TikPrimary,
                    checkedThumbColor = Color.White,
                ),
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onOpenAdvanced,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TikPrimary),
        ) {
            Text("انتخاب اپ‌ها", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "با روشن بودن فیلتر، فقط اپ‌های انتخابی از تونل عبور می‌کنند.",
            color = TikMuted,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun TikNetAccountTab(
    state: TikNetMainUiState,
    onSync: () -> Unit,
    onRefreshUser: () -> Unit,
    onLogout: () -> Unit,
) {
    var confirmLogout by remember { mutableStateOf(false) }
    val user = state.user
    val name = user?.fullName?.takeIf { it.isNotBlank() } ?: user?.username ?: TikNetPrefsSafeUsername(state)
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "T"
    val active = user?.isExpired != true && (user?.hasSubscription == true || user != null)

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("حساب من", color = TikOnBg, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(listOf(TikPrimary.copy(alpha = 0.45f), TikSurface)),
                )
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(TikPrimary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(initial, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("نام کاربری", color = TikMuted, fontSize = 12.sp)
                    Text(user?.username ?: name, color = TikOnBg, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    if (!user?.fullName.isNullOrBlank() && user!!.fullName != user.username) {
                        Text(user.fullName!!, color = TikOnBg.copy(alpha = 0.85f), fontSize = 14.sp)
                    }
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (active) TikConnected.copy(alpha = 0.2f) else TikDanger.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (active) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber,
                            null,
                            tint = if (active) TikConnected else TikDanger,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (active) "فعال" else "منقضی",
                            color = if (active) TikConnected else TikDanger,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("پلن", user?.planName ?: "—", Modifier.weight(1f))
            StatCard("انقضا", user?.expireDate ?: "—", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("روز باقی‌مانده", user?.daysRemaining?.toString() ?: "—", Modifier.weight(1f))
            StatCard(
                "مصرف حجم",
                formatTraffic(user?.trafficUsedBytes),
                Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onSync,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TikPrimary),
        ) {
            if (state.busy) {
                CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Outlined.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("بروزرسانی اشتراک", fontWeight = FontWeight.Bold)
            }
        }
        if (!state.syncMessage.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(state.syncMessage!!, color = TikMuted, fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(22.dp))
        Text("خدمات", color = TikOnBg, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(TikSurface),
        ) {
            ServiceRow("اعلان‌ها")
            HorizontalDivider(color = TikBorder)
            ServiceRow("راهنما و سوالات")
            HorizontalDivider(color = TikBorder)
            ServiceRow("عیب‌یابی اینترنت گوشی")
        }

        Spacer(Modifier.height(20.dp))
        OutlinedButton(
            onClick = { confirmLogout = true },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, TikDanger.copy(alpha = 0.6f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TikDanger),
        ) {
            Icon(Icons.AutoMirrored.Outlined.Logout, null)
            Spacer(Modifier.width(8.dp))
            Text("خروج از حساب", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "TikNet v${state.appVersion}",
            color = TikMuted,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onRefreshUser, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("بروزرسانی اطلاعات", color = TikPrimary)
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("خروج از حساب") },
            text = { Text("آیا مطمئنید می‌خواهید خارج شوید؟") },
            confirmButton = {
                TextButton(onClick = { confirmLogout = false; onLogout() }) {
                    Text("خروج", color = TikDanger)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("انصراف") }
            },
            containerColor = TikSurface,
            titleContentColor = TikOnBg,
            textContentColor = TikMuted,
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(TikSurface)
            .padding(14.dp),
    ) {
        Text(label, color = TikMuted, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Text(value, color = TikOnBg, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ServiceRow(title: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = TikOnBg, modifier = Modifier.weight(1f))
        Text("‹", color = TikMuted, fontSize = 18.sp)
    }
}

@Composable
private fun TikNetPrefsSafeUsername(state: TikNetMainUiState): String =
    state.user?.username ?: "کاربر"

private fun formatTraffic(bytes: Long?): String {
    if (bytes == null || bytes <= 0) return "—"
    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
    return String.format(Locale.US, "%.1f GB", gb)
}

private fun flagForRemarks(remarks: String): String {
    val r = remarks.lowercase(Locale.US)
    return when {
        "germany" in r || "آلمان" in r || "(de)" in r -> "🇩🇪"
        "united kingdom" in r || "uk" in r || "britain" in r || "(gb)" in r -> "🇬🇧"
        "netherlands" in r || "holland" in r || "هلند" in r || "(nl)" in r -> "🇳🇱"
        "france" in r || "فرانسه" in r || "(fr)" in r -> "🇫🇷"
        "turkey" in r || "ترکیه" in r || "(tr)" in r -> "🇹🇷"
        "usa" in r || "united states" in r || "(us)" in r -> "🇺🇸"
        else -> "🌐"
    }
}
