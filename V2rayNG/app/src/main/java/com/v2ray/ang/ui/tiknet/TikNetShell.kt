package com.v2ray.ang.ui.tiknet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material.icons.outlined.Troubleshoot
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.dto.AppInfo
import com.v2ray.ang.tiknet.TikNetAnnouncement
import com.v2ray.ang.tiknet.TikNetFaqItem
import com.v2ray.ang.tiknet.TikNetJalali
import com.v2ray.ang.tiknet.TikNetMessages
import com.v2ray.ang.tiknet.TikNetNotificationItem
import com.v2ray.ang.tiknet.TikNetUserInfo

private val TikBg = Color(0xFF0D0D0D)
private val TikSurface = Color(0xFF1E1E1E)
private val TikSurface2 = Color(0xFF252525)
private val TikPrimary = Color(0xFF6366F1)
private val TikConnected = Color(0xFF22C55E)
private val TikConnecting = Color(0xFFF59E0B)
private val TikMuted = Color(0xFF9E9E9E)
private val TikOnBg = Color(0xFFE8E8E8)
private val TikBorder = Color(0x14FFFFFF)
private val TikDanger = Color(0xFFEF4444)
private val TikWarn = Color(0xFFEAB308)
private val TikOrange = Color(0xFFF97316)

private enum class TikNetTab { Connect, Details, Filter, Account }

private enum class AccountSheet { None, Notifications, Faq, Diagnostics }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TikNetShell(
    state: TikNetMainUiState,
    viewModel: TikNetMainViewModel,
    onToggleConnect: () -> Unit,
    onSelectServer: (String) -> Unit,
    onSmartMode: () -> Unit,
    onPingAll: () -> Unit,
    onSync: () -> Unit,
    onLogout: () -> Unit,
    onFilterChangedRestart: () -> Unit,
) {
    var tab by remember { mutableStateOf(TikNetTab.Connect) }
    var showServerSheet by remember { mutableStateOf(false) }
    var showLogout by remember { mutableStateOf(false) }
    var accountSheet by remember { mutableStateOf(AccountSheet.None) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.syncMessage) {
        val msg = state.syncMessage ?: return@LaunchedEffect
        snackbar.showSnackbar(msg)
        viewModel.clearSyncMessage()
    }

    // Force Latin digits (0-9): Persian locale fonts otherwise reshape ASCII digits to ۰-۹ and they look tiny.
    val latinDigitStyle = LocalTextStyle.current.copy(localeList = LocaleList(Locale("en")))
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalTextStyle provides latinDigitStyle,
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(TikBg),
            containerColor = TikBg,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                TikNetBottomNav(tab = tab, onSelect = { tab = it })
            },
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(TikBg),
            ) {
                when (tab) {
                    TikNetTab.Connect -> ConnectTab(
                        state = state,
                        onToggleConnect = onToggleConnect,
                        onOpenServers = { showServerSheet = true },
                        onOpenNotifications = {
                            accountSheet = AccountSheet.Notifications
                            viewModel.loadNotifications()
                        },
                    )
                    TikNetTab.Details -> DetailsTab(state = state)
                    TikNetTab.Filter -> FilterTab(
                        state = state,
                        viewModel = viewModel,
                        onFilterChangedRestart = onFilterChangedRestart,
                    )
                    TikNetTab.Account -> AccountTab(
                        state = state,
                        onSync = onSync,
                        onLogoutClick = { showLogout = true },
                        onOpenNotifications = {
                            accountSheet = AccountSheet.Notifications
                            viewModel.loadNotifications()
                        },
                        onOpenFaq = {
                            accountSheet = AccountSheet.Faq
                            viewModel.loadFaq()
                        },
                        onOpenDiagnostics = {
                            accountSheet = AccountSheet.Diagnostics
                            viewModel.runDiagnostics()
                        },
                    )
                }
            }
        }

        if (state.showSplash) {
            TikNetLaunchSplash(onFinished = { viewModel.dismissSplash() })
        }

        if (showServerSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showServerSheet = false },
                sheetState = sheetState,
                containerColor = TikSurface,
                contentColor = TikOnBg,
                dragHandle = {
                    Box(
                        Modifier
                            .padding(vertical = 10.dp)
                            .size(width = 40.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(TikMuted.copy(alpha = 0.4f)),
                    )
                },
            ) {
                ServerPickerSheet(
                    state = state,
                    onSmartMode = {
                        onSmartMode()
                        showServerSheet = false
                    },
                    onSelectServer = { guid ->
                        onSelectServer(guid)
                        showServerSheet = false
                    },
                    onPingAll = onPingAll,
                    onClose = { showServerSheet = false },
                )
            }
        }

        when (accountSheet) {
            AccountSheet.Notifications -> {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { accountSheet = AccountSheet.None },
                    sheetState = sheetState,
                    containerColor = TikSurface,
                    contentColor = TikOnBg,
                ) {
                    NotificationsSheet(
                        items = state.notifications,
                        loading = state.notificationsLoading,
                        onMarkRead = { viewModel.markNotificationRead(it) },
                        onClose = { accountSheet = AccountSheet.None },
                    )
                }
            }
            AccountSheet.Faq -> {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { accountSheet = AccountSheet.None },
                    sheetState = sheetState,
                    containerColor = TikSurface,
                    contentColor = TikOnBg,
                ) {
                    FaqSheet(
                        items = state.faq,
                        loading = state.faqLoading,
                        onClose = { accountSheet = AccountSheet.None },
                    )
                }
            }
            AccountSheet.Diagnostics -> {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { accountSheet = AccountSheet.None },
                    sheetState = sheetState,
                    containerColor = TikSurface,
                    contentColor = TikOnBg,
                ) {
                    DiagnosticsSheet(
                        items = state.diagnostics,
                        loading = state.diagnosticsLoading,
                        onRetry = { viewModel.runDiagnostics() },
                        onOpenSettings = { viewModel.openSettingsTarget(it) },
                        onClose = { accountSheet = AccountSheet.None },
                    )
                }
            }
            AccountSheet.None -> Unit
        }

        if (showLogout) {
            AlertDialog(
                onDismissRequest = { showLogout = false },
                containerColor = TikSurface,
                titleContentColor = TikOnBg,
                textContentColor = TikMuted,
                title = { Text("خروج از حساب") },
                text = { Text("آیا مطمئن هستید که می‌خواهید از حساب خارج شوید؟") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogout = false
                            onLogout()
                        },
                    ) {
                        Text("خروج", color = TikDanger)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogout = false }) {
                        Text("انصراف", color = TikMuted)
                    }
                },
            )
        }
    }
}

@Composable
private fun TikNetBottomNav(tab: TikNetTab, onSelect: (TikNetTab) -> Unit) {
    val items = listOf(
        Triple(TikNetTab.Connect, Icons.Outlined.Shield, "اتصال"),
        Triple(TikNetTab.Details, Icons.Outlined.Analytics, "جزئیات"),
        Triple(TikNetTab.Filter, Icons.Outlined.Apps, "فیلتر اپ‌ها"),
        Triple(TikNetTab.Account, Icons.Outlined.Person, "حساب من"),
    )
    Row(
        Modifier
            .fillMaxWidth()
            .background(TikSurface)
            .border(width = 1.dp, color = TikBorder)
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { (t, icon, label) ->
            val selected = tab == t
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelect(t) }
                    .background(if (selected) TikPrimary.copy(alpha = 0.18f) else Color.Transparent)
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selected) TikPrimary.copy(alpha = 0.28f) else Color.Transparent)
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = label,
                        tint = if (selected) TikPrimary else TikMuted,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    label,
                    color = if (selected) TikOnBg else TikMuted,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/* ───────────────────────── Connect ───────────────────────── */

@Composable
private fun ConnectTab(
    state: TikNetMainUiState,
    onToggleConnect: () -> Unit,
    onOpenServers: () -> Unit,
    onOpenNotifications: () -> Unit,
) {
    val statusColor = statusColor(state)
    val statusLabel = statusLabel(state)
    val statusHint = statusHint(state)
    val busy = state.phase == TikNetConnPhase.Connecting ||
        state.phase == TikNetConnPhase.Disconnecting ||
        state.smartPicking ||
        state.busy

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            // RTL: CenterEnd = physical left (بالا سمت چپ)
            Box(
                Modifier.align(Alignment.CenterEnd),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(onClick = onOpenNotifications) {
                    Icon(
                        Icons.Outlined.Notifications,
                        contentDescription = "اعلان‌ها",
                        tint = TikOnBg,
                    )
                }
                if (state.unreadCount > 0) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 8.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(TikDanger),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            TikNetJalali.toLatinDigits(
                                if (state.unreadCount > 99) "99+" else state.unreadCount.toString(),
                            ),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
            }
            Text(
                "اتصال امن",
                color = TikOnBg,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center),
            )
            Text(
                TikNetJalali.toPersianDigits(state.appVersion),
                color = TikMuted,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp),
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            StatusHeroCard(
                statusLabel = statusLabel,
                statusHint = statusHint,
                statusColor = statusColor,
                serverTitle = state.selectedTitle,
                connected = state.phase == TikNetConnPhase.Connected && !state.smartPicking,
                busy = busy,
            )
            Spacer(Modifier.height(16.dp))
            ServerSelectorCard(
                title = state.selectedTitle,
                smartMode = state.smartMode,
                onClick = onOpenServers,
            )
            Spacer(Modifier.height(16.dp))
            AnnouncementBanner(state.announcement)
            state.error?.takeIf { it.isNotBlank() }?.let { err ->
                Spacer(Modifier.height(12.dp))
                AlertGlass(icon = Icons.Outlined.WarningAmber, color = TikDanger, text = err)
            }
            Spacer(Modifier.height(28.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ConnectPowerButton(
                    connected = state.phase == TikNetConnPhase.Connected && !state.smartPicking,
                    busy = busy,
                    onClick = onToggleConnect,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                when {
                    busy -> "برای لغو، دوباره دکمه را بزنید"
                    state.phase == TikNetConnPhase.Connected -> "برای قطع اتصال بزنید"
                    else -> "برای اتصال به سرور انتخابی بزنید"
                },
                color = TikMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StatusHeroCard(
    statusLabel: String,
    statusHint: String,
    statusColor: Color,
    serverTitle: String,
    connected: Boolean,
    busy: Boolean,
) {
    if (busy) {
        val infinite = rememberInfiniteTransition(label = "statusGlow")
        val glow by infinite.animateFloat(
            initialValue = 0.22f,
            targetValue = 0.55f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "glowAlpha",
        )
        val borderPulse by infinite.animateFloat(
            initialValue = 1.2f,
            targetValue = 2.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "borderW",
        )
        StatusHeroCardBody(
            statusLabel, statusHint, statusColor, serverTitle,
            connected = connected,
            busy = true,
            glow = glow,
            borderPulse = borderPulse,
        )
    } else {
        StatusHeroCardBody(
            statusLabel,
            statusHint,
            statusColor,
            serverTitle,
            connected = connected,
            busy = false,
            glow = if (connected) 0.42f else 0.28f,
            borderPulse = if (connected) 1.6f else 1.4f,
        )
    }
}

@Composable
private fun StatusHeroCardBody(
    statusLabel: String,
    statusHint: String,
    statusColor: Color,
    serverTitle: String,
    connected: Boolean,
    busy: Boolean,
    glow: Float,
    borderPulse: Float,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(statusColor.copy(alpha = 0.18f), TikSurface),
                ),
            )
            .border(
                width = borderPulse.dp,
                brush = Brush.linearGradient(
                    listOf(
                        statusColor.copy(alpha = glow),
                        statusColor.copy(alpha = glow * 0.45f),
                        statusColor.copy(alpha = glow),
                    ),
                ),
                shape = RoundedCornerShape(20.dp),
            )
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = when {
                        connected -> Icons.Outlined.VerifiedUser
                        busy -> Icons.Outlined.Sync
                        else -> Icons.Outlined.Shield
                    },
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(30.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    statusLabel,
                    color = statusColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(statusHint, color = TikMuted, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "مقصد: $serverTitle",
                    color = TikOnBg,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ServerSelectorCard(
    title: String,
    smartMode: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TikSurface)
            .border(1.dp, TikBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(TikPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (smartMode) Icons.Outlined.Speed else Icons.Outlined.Dns,
                contentDescription = null,
                tint = TikPrimary,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("سرور", color = TikMuted, fontSize = 12.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                title,
                color = TikOnBg,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(Icons.Outlined.ExpandMore, contentDescription = null, tint = TikMuted)
    }
}

@Composable
private fun AnnouncementBanner(announcement: TikNetAnnouncement?) {
    if (announcement == null || !announcement.show || announcement.text.isBlank()) return
    val color = when (announcement.type.lowercase()) {
        "warning" -> TikWarn
        "error" -> TikDanger
        "success" -> TikConnected
        else -> TikPrimary
    }
    AlertGlass(icon = Icons.Outlined.Campaign, color = color, text = announcement.text)
}

@Composable
private fun AlertGlass(icon: ImageVector, color: Color, text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, color = TikOnBg, fontSize = 14.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ConnectPowerButton(
    connected: Boolean,
    busy: Boolean,
    onClick: () -> Unit,
) {
    val color = if (connected) TikConnected else TikPrimary
    if (busy) {
        val infinite = rememberInfiniteTransition(label = "powerPulse")
        val pulse by infinite.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.72f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulse",
        )
        val scale by infinite.animateFloat(
            initialValue = 1f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "scale",
        )
        ConnectPowerButtonBody(color, connected, busy = true, pulse, scale, onClick)
    } else {
        ConnectPowerButtonBody(
            color = color,
            connected = connected,
            busy = false,
            pulse = if (connected) 0.45f else 0.35f,
            scale = 1f,
            onClick = onClick,
        )
    }
}

@Composable
private fun ConnectPowerButtonBody(
    color: Color,
    connected: Boolean,
    busy: Boolean,
    pulse: Float,
    scale: Float,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(210.dp)
            .scale(scale)
            .drawBehind {
                drawCircle(
                    color = color.copy(alpha = pulse * 0.32f),
                    radius = 96.dp.toPx(),
                )
                drawCircle(
                    color = color.copy(alpha = pulse * 0.16f),
                    radius = 112.dp.toPx(),
                )
            },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(168.dp)
                .clip(CircleShape)
                .background(color)
                .clickable(onClick = onClick),
        ) {
            if (busy) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(48.dp),
                )
            } else {
                Icon(
                    imageVector = if (connected) Icons.Outlined.PowerSettingsNew else Icons.Outlined.Shield,
                    contentDescription = if (connected) "قطع اتصال" else "اتصال",
                    tint = Color.White,
                    modifier = Modifier.size(56.dp),
                )
            }
        }
    }
}

@Composable
private fun ServerPickerSheet(
    state: TikNetMainUiState,
    onSmartMode: () -> Unit,
    onSelectServer: (String) -> Unit,
    onPingAll: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .padding(bottom = 16.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("انتخاب سرور", color = TikOnBg, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    when {
                        state.smartPicking -> "در حال انتخاب بهترین سرور…"
                        state.phase == TikNetConnPhase.Connected ->
                            "متصل هستید — تغییر سرور ممکن است اتصال را عوض کند"
                        else -> "اتصال هوشمند یا یک سرور را انتخاب کنید"
                    },
                    color = TikMuted,
                    fontSize = 13.sp,
                )
            }
            IconButton(onClick = onPingAll, enabled = !state.isPinging) {
                if (state.isPinging) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = TikPrimary)
                } else {
                    Icon(Icons.Outlined.Speed, contentDescription = "پینگ", tint = TikOnBg)
                }
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, contentDescription = "بستن", tint = TikMuted)
            }
        }
        HorizontalDivider(color = TikBorder)
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                ServerRow(
                    flagEmoji = "⚡",
                    title = "اتصال هوشمند",
                    subtitle = "بهترین سرور بر اساس پینگ",
                    selected = state.smartMode,
                    pingMs = null,
                    onClick = onSmartMode,
                    highlight = true,
                )
            }
            items(state.servers, key = { it.guid }) { server ->
                ServerRow(
                    flagEmoji = flagFromRemarks(server.remarks),
                    title = server.remarks,
                    subtitle = server.protocolLabel,
                    selected = !state.smartMode && state.selectedGuid == server.guid,
                    pingMs = server.pingMs,
                    onClick = { onSelectServer(server.guid) },
                )
            }
            if (state.servers.isEmpty()) {
                item {
                    Text(
                        "سروری موجود نیست. از حساب من اشتراک را بروزرسانی کنید.",
                        color = TikMuted,
                        modifier = Modifier.padding(24.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun ServerRow(
    flagEmoji: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    pingMs: Long?,
    onClick: () -> Unit,
    highlight: Boolean = false,
) {
    val border = when {
        selected -> TikPrimary
        highlight -> TikPrimary.copy(alpha = 0.35f)
        else -> TikBorder
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) TikPrimary.copy(alpha = 0.12f) else TikSurface2)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(TikPrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(flagEmoji, fontSize = 18.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TikOnBg, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = TikMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (pingMs != null) {
            PingChip(pingMs)
        } else if (highlight) {
            Icon(Icons.Outlined.Speed, contentDescription = null, tint = TikPrimary, modifier = Modifier.size(18.dp))
        }
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = TikPrimary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun PingChip(ms: Long) {
    val color = latencyColor(ms)
    val label = TikNetJalali.toPersianDigits("$ms ms")
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Speed, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

/* ───────────────────────── Details ───────────────────────── */

@Composable
private fun DetailsTab(state: TikNetMainUiState) {
    val connected = state.phase == TikNetConnPhase.Connected
    val statusColor = statusColor(state)
    val uptime = if (connected) {
        TikNetJalali.formatUptime(state.connectedAtMs, state.uptimeTick.takeIf { it > 0 } ?: System.currentTimeMillis())
    } else {
        "—"
    }
    val selected = state.servers.firstOrNull { it.guid == state.selectedGuid }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
    ) {
        Text(
            "جزئیات اتصال",
            color = TikOnBg,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            textAlign = TextAlign.Center,
        )

        DetailCard(title = "وضعیت") {
            DetailRow(Icons.Outlined.VerifiedUser, "وضعیت", statusLabel(state), statusColor)
            ThinDivider()
            DetailRow(Icons.Outlined.Timelapse, "مدت اتصال", uptime)
        }
        Spacer(Modifier.height(16.dp))

        DetailCard(title = "سرور و مسیر") {
            DetailRow(Icons.Outlined.Dns, "سرور انتخابی", state.selectedTitle.ifBlank { "—" })
            ThinDivider()
            DetailRow(Icons.Outlined.Route, "پروتکل", selected?.protocolLabel?.ifBlank { "—" } ?: "—")
            ThinDivider()
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Speed, contentDescription = null, tint = TikMuted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                Text("پینگ سرور", color = TikMuted, fontSize = 14.sp, modifier = Modifier.weight(1f))
                val ping = selected?.pingMs
                if (ping != null) PingChip(ping) else Text("—", color = TikOnBg, fontWeight = FontWeight.Medium)
            }
            if (state.currentDelayText.isNotBlank()) {
                ThinDivider()
                DetailRow(
                    Icons.Outlined.Speed,
                    "تأخیر هسته",
                    TikNetMessages.coreDelay(state.currentDelayText),
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        DetailCard(title = "آی‌پی خروجی") {
            val (flag, ipText) = TikNetMessages.formatExitIp(
                state.exitIpText.takeIf { it.isNotBlank() },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (state.exitIpText.isBlank() && !connected) "🌐" else flag.ifBlank { "🌐" },
                    fontSize = 28.sp,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("آی‌پی خروجی", color = TikMuted, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        when {
                            state.exitIpText.isBlank() && connected -> "در حال دریافت…"
                            state.exitIpText.isBlank() -> "—"
                            else -> ipText
                        },
                        color = TikOnBg,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        DetailCard(title = "سرعت لحظه‌ای") {
            Row {
                MetricTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.ArrowDownward,
                    iconColor = TikConnected,
                    label = "دانلود",
                    value = if (connected) TikNetJalali.toPersianDigits(TikNetJalali.formatSpeed(state.downlinkSpeed)) else "—",
                )
                Spacer(Modifier.width(12.dp))
                MetricTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.ArrowUpward,
                    iconColor = TikPrimary,
                    label = "آپلود",
                    value = if (connected) TikNetJalali.toPersianDigits(TikNetJalali.formatSpeed(state.uplinkSpeed)) else "—",
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        DetailCard(title = "ترافیک این نشست") {
            DetailRow(
                Icons.Outlined.ArrowDownward,
                "حجم دانلود",
                if (connected) TikNetJalali.toPersianDigits(TikNetJalali.formatSize(state.sessionDown)) else "—",
            )
            ThinDivider()
            DetailRow(
                Icons.Outlined.ArrowUpward,
                "حجم آپلود",
                if (connected) TikNetJalali.toPersianDigits(TikNetJalali.formatSize(state.sessionUp)) else "—",
            )
            ThinDivider()
            DetailRow(
                Icons.Outlined.SwapVert,
                "مجموع نشست",
                if (connected) {
                    TikNetJalali.toPersianDigits(TikNetJalali.formatSize(state.sessionUp + state.sessionDown))
                } else {
                    "—"
                },
            )
        }
        Spacer(Modifier.height(16.dp))

        DetailCard(title = "هسته") {
            DetailRow(Icons.Outlined.Memory, "وضعیت هسته", if (connected) "فعال" else "متوقف")
            ThinDivider()
            DetailRow(Icons.Outlined.Timelapse, "مدت اتصال", uptime)
            ThinDivider()
            DetailRow(
                Icons.Outlined.VpnKey,
                "حالت",
                if (state.smartMode) "اتصال هوشمند" else "دستی",
            )
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
            .border(1.dp, TikBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text(title, color = TikOnBg, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String, valueColor: Color = TikOnBg) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = TikMuted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, color = TikMuted, fontSize = 13.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            value,
            color = valueColor,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MetricTile(
    modifier: Modifier,
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(TikSurface2)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = TikMuted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(value, color = TikOnBg, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
private fun ThinDivider() {
    HorizontalDivider(Modifier.padding(vertical = 10.dp), color = TikBorder)
}

/* ───────────────────────── Filter ───────────────────────── */

@Composable
private fun FilterTab(
    state: TikNetMainUiState,
    viewModel: TikNetMainViewModel,
    onFilterChangedRestart: () -> Unit,
) {
    LaunchedEffect(Unit) {
        viewModel.ensureAppsLoaded()
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(TikBg),
    ) {
        Text(
            "فیلتر اپ‌ها",
            color = TikOnBg,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            textAlign = TextAlign.Center,
        )

        if (state.filterLoading && state.filterApps.isEmpty()) {
            FilterLoadingView()
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(TikSurface2)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("فیلتر اپ (Split Tunnel)", color = TikOnBg, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (state.filterEnabled) {
                                "فقط اپ‌های روشن‌شده از VPN استفاده می‌کنند."
                            } else {
                                "خاموش = همه اپ‌ها از VPN استفاده می‌کنند."
                            },
                            color = TikMuted,
                            fontSize = 12.sp,
                        )
                    }
                    Switch(
                        checked = state.filterEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.setFilterEnabled(enabled)
                            onFilterChangedRestart()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = TikPrimary,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = TikMuted.copy(alpha = 0.4f),
                        ),
                    )
                }
                Spacer(Modifier.height(12.dp))
                SearchField(
                    value = state.filterQuery,
                    onValueChange = { viewModel.setFilterQuery(it) },
                    hint = "جستجو بین اپ‌ها",
                )
            }

            val apps = remember(state.filterApps, state.filterQuery, state.filterSelected) {
                viewModel.filteredApps()
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(apps, key = { it.packageName }) { app ->
                    AppFilterRow(
                        app = app,
                        checked = state.filterSelected.contains(app.packageName),
                        enabled = state.filterEnabled,
                        onToggle = {
                            viewModel.toggleFilterApp(app.packageName)
                            if (state.filterEnabled) onFilterChangedRestart()
                        },
                    )
                }
                if (apps.isEmpty() && !state.filterLoading) {
                    item {
                        Text(
                            "اپلیکی یافت نشد",
                            color = TikMuted,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterLoadingView() {
    val infinite = rememberInfiniteTransition(label = "filterLoad")
    val angle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweep",
    )

    Column(
        Modifier
            .fillMaxSize()
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
            Canvas(
                Modifier
                    .size(56.dp)
                    .graphicsLayer { rotationZ = angle },
            ) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            TikPrimary,
                            TikPrimary.copy(alpha = 0.15f),
                            TikConnected.copy(alpha = 0.5f),
                            TikPrimary,
                        ),
                    ),
                    startAngle = -90f,
                    sweepAngle = 300f,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(TikSurface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Shield, contentDescription = null, tint = TikPrimary, modifier = Modifier.size(26.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("در حال بارگذاری اپ‌ها", color = TikOnBg, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "لیست برنامه‌های نصب‌شده روی گوشی در حال آماده‌سازی است…",
            color = TikMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
        Spacer(Modifier.height(28.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(8) { SkeletonAppRow() }
        }
    }
}

@Composable
private fun SkeletonAppRow() {
    val infinite = rememberInfiniteTransition(label = "skel")
    val alpha by infinite.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "a",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TikSurface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(TikMuted.copy(alpha = alpha)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Box(
                Modifier
                    .fillMaxWidth(0.55f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(TikMuted.copy(alpha = alpha)),
            )
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth(0.35f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(TikMuted.copy(alpha = alpha * 0.7f)),
            )
        }
        Box(
            Modifier
                .width(42.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(TikMuted.copy(alpha = alpha * 0.6f)),
        )
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit, hint: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TikSurface2)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = TikMuted)
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = TikOnBg, fontSize = 15.sp),
            cursorBrush = SolidColor(TikPrimary),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(hint, color = TikMuted.copy(alpha = 0.7f), fontSize = 15.sp)
                }
                inner()
            },
        )
    }
}

@Composable
private fun AppFilterRow(
    app: AppInfo,
    checked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TikSurface)
            .border(1.dp, TikBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(TikPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                app.appName.take(1).ifEmpty { "?" },
                color = TikPrimary,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(app.appName, color = TikOnBg, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(app.packageName, color = TikMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = TikPrimary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = TikMuted.copy(alpha = 0.35f),
                disabledCheckedTrackColor = TikPrimary.copy(alpha = 0.35f),
            ),
        )
    }
}

/* ───────────────────────── Account ───────────────────────── */

@Composable
private fun AccountTab(
    state: TikNetMainUiState,
    onSync: () -> Unit,
    onLogoutClick: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenFaq: () -> Unit,
    onOpenDiagnostics: () -> Unit,
) {
    val user = state.user
    val expired = user?.isExpired == true || (user?.hasSubscription == false)
    val active = user != null && user.hasSubscription && user.isExpired != true

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(TikPrimary.copy(alpha = 0.35f), TikBg),
                    ),
                )
                .padding(top = 20.dp, bottom = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("حساب من", color = TikOnBg, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))
                Box(
                    Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(TikPrimary, TikPrimary.copy(alpha = 0.55f))),
                        )
                        .border(2.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        (user?.username ?: "?").take(1).uppercase(),
                        color = Color.White,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    user?.fullName?.takeIf { it.isNotBlank() } ?: user?.username ?: "—",
                    color = TikOnBg,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (!user?.username.isNullOrBlank() && !user?.fullName.isNullOrBlank()) {
                    Text("@${user!!.username}", color = TikMuted, fontSize = 13.sp)
                }
                Spacer(Modifier.height(10.dp))
                StatusBadge(expired = expired, active = active, hasSub = user?.hasSubscription == true)
            }
        }

        Column(Modifier.padding(horizontal = 20.dp)) {
            Row {
                StatTile(
                    Modifier.weight(1f),
                    Icons.Outlined.WorkspacePremium,
                    "پلن",
                    user?.planName?.takeIf { it.isNotBlank() } ?: "—",
                )
                Spacer(Modifier.width(10.dp))
                StatTile(
                    Modifier.weight(1f),
                    Icons.Outlined.Event,
                    "انقضا",
                    TikNetJalali.formatExpire(user?.expireDate),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row {
                StatTile(
                    Modifier.weight(1f),
                    Icons.Outlined.Timelapse,
                    "روز باقی‌مانده",
                    user?.daysRemaining?.let { TikNetJalali.toPersianDigits(it.toString()) } ?: "—",
                )
                Spacer(Modifier.width(10.dp))
                // Keep grid balanced without last-sync field
                StatTile(
                    Modifier.weight(1f),
                    Icons.Outlined.Info,
                    "وضعیت",
                    when {
                        expired -> "منقضی"
                        active -> "فعال"
                        else -> "—"
                    },
                )
            }

            val hasTraffic = (user?.trafficUsedBytes ?: 0) > 0 || (user?.trafficLimitBytes ?: 0) > 0
            if (hasTraffic) {
                Spacer(Modifier.height(10.dp))
                TrafficCard(user)
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onSync,
                enabled = !state.busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TikPrimary, contentColor = Color.White),
            ) {
                if (state.busy) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White)
                    Spacer(Modifier.width(10.dp))
                    Text("در حال بروزرسانی…")
                } else {
                    Icon(Icons.Outlined.Sync, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("بروزرسانی اشتراک")
                }
            }

            val uriHandler = LocalUriHandler.current
            val shopUrl = state.shopUrl
            if (!shopUrl.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { runCatching { uriHandler.openUri(shopUrl) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TikConnected,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.Outlined.ShoppingCart, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(state.shopLabel?.takeIf { it.isNotBlank() } ?: "خرید و تمدید")
                }
            }
            val tg = state.telegramSupport
            if (!tg.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        val url = when {
                            tg.startsWith("http") -> tg
                            tg.startsWith("tg:") -> tg
                            tg.startsWith("@") -> "https://t.me/${tg.removePrefix("@")}"
                            else -> "https://t.me/$tg"
                        }
                        runCatching { uriHandler.openUri(url) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, TikPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TikPrimary),
                ) {
                    Icon(Icons.Outlined.Chat, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("پشتیبانی")
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("خدمات", color = TikOnBg, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(TikSurface)
                    .border(1.dp, TikBorder, RoundedCornerShape(14.dp)),
            ) {
                ServiceTile(Icons.Outlined.Notifications, "اعلان‌ها", onOpenNotifications)
                HorizontalDivider(color = TikBorder, modifier = Modifier.padding(start = 56.dp))
                ServiceTile(Icons.Outlined.HelpOutline, "راهنما و سوالات", onOpenFaq)
                HorizontalDivider(color = TikBorder, modifier = Modifier.padding(start = 56.dp))
                ServiceTile(Icons.Outlined.Troubleshoot, "عیب‌یابی اینترنت گوشی", onOpenDiagnostics)
            }

            Spacer(Modifier.height(20.dp))
            OutlinedButton(
                onClick = onLogoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, TikDanger),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TikDanger),
            ) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("خروج از حساب")
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "نسخه ${TikNetJalali.toPersianDigits(state.appVersion)}",
                color = TikMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StatusBadge(expired: Boolean, active: Boolean, hasSub: Boolean) {
    val (color, label, icon) = when {
        expired -> Triple(TikDanger, "منقضی", Icons.Outlined.ErrorOutline)
        !hasSub -> Triple(TikWarn, "بدون سرویس", Icons.Outlined.Info)
        active -> Triple(TikConnected, "فعال", Icons.Outlined.CheckCircle)
        else -> Triple(TikMuted, "—", Icons.Outlined.Info)
    }
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = color, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

@Composable
private fun StatTile(modifier: Modifier, icon: ImageVector, label: String, value: String) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(TikSurface)
            .border(1.dp, TikBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = TikPrimary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = TikMuted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            value,
            color = TikOnBg,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TrafficCard(user: TikNetUserInfo?) {
    val used = user?.trafficUsedBytes
    val limit = user?.trafficLimitBytes
    val label = TikNetJalali.formatTraffic(used, limit)
    val ratio = TikNetJalali.trafficRatio(used, limit)
    val barColor = if (ratio >= 0.9f) TikDanger else TikPrimary

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(TikSurface)
            .border(1.dp, TikBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Text("مصرف حجم", color = TikMuted, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            TikNetJalali.toPersianDigits(label),
            color = TikOnBg,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )
        if ((limit ?: 0) > 0) {
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = barColor,
                trackColor = TikBorder,
            )
        }
    }
}

@Composable
private fun ServiceTile(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(TikPrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = TikPrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(label, color = TikOnBg, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.ExpandMore, contentDescription = null, tint = TikMuted)
    }
}

/* ───────────────────────── Account sheets ───────────────────────── */

@Composable
private fun NotificationsSheet(
    items: List<TikNetNotificationItem>,
    loading: Boolean,
    onMarkRead: (Int) -> Unit,
    onClose: () -> Unit,
) {
    var expandedId by remember { mutableStateOf<Int?>(null) }
    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f)
            .padding(bottom = 16.dp),
    ) {
        SheetHeader("اعلان‌ها", onClose)
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TikPrimary)
            }
            items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("اعلان جدیدی ندارید.", color = TikMuted)
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { it.id }) { n ->
                    val open = expandedId == n.id
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (n.read) TikSurface2 else TikPrimary.copy(alpha = 0.1f))
                            .border(1.dp, TikBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                expandedId = if (open) null else n.id
                                if (!n.read) onMarkRead(n.id)
                            }
                            .padding(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Notifications,
                                contentDescription = null,
                                tint = if (n.read) TikMuted else TikPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                n.title.ifBlank { "اعلان" },
                                color = TikOnBg,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                contentDescription = null,
                                tint = TikMuted,
                            )
                        }
                        if (open && n.body.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(n.body, color = TikMuted, fontSize = 13.sp, lineHeight = 20.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FaqSheet(
    items: List<TikNetFaqItem>,
    loading: Boolean,
    onClose: () -> Unit,
) {
    var expandedId by remember { mutableStateOf<Int?>(null) }
    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f)
            .padding(bottom = 16.dp),
    ) {
        SheetHeader("راهنما و سوالات", onClose)
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TikPrimary)
            }
            items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("موردی یافت نشد.", color = TikMuted)
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { it.id }) { faq ->
                    val open = expandedId == faq.id
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(TikSurface2)
                            .border(1.dp, TikBorder, RoundedCornerShape(12.dp))
                            .clickable { expandedId = if (open) null else faq.id }
                            .padding(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.HelpOutline, contentDescription = null, tint = TikPrimary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                faq.question,
                                color = TikOnBg,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                contentDescription = null,
                                tint = TikMuted,
                            )
                        }
                        if (open) {
                            Spacer(Modifier.height(8.dp))
                            Text(faq.answer, color = TikMuted, fontSize = 13.sp, lineHeight = 20.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsSheet(
    items: List<TikNetDiagItem>,
    loading: Boolean,
    onRetry: () -> Unit,
    onOpenSettings: (String?) -> Unit,
    onClose: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f)
            .padding(bottom = 16.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("عیب‌یابی اینترنت گوشی", color = TikOnBg, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = onRetry, enabled = !loading) {
                Icon(Icons.Outlined.Refresh, contentDescription = "بررسی مجدد", tint = TikOnBg)
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, contentDescription = "بستن", tint = TikMuted)
            }
        }
        HorizontalDivider(color = TikBorder)
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = TikPrimary)
                    Spacer(Modifier.height(12.dp))
                    Text("در حال بررسی…", color = TikMuted)
                }
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { it.title }) { item ->
                    val color = if (item.ok) TikConnected else TikDanger
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(TikSurface2)
                            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .clickable { onOpenSettings(item.settingsAction) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (item.ok) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = color,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title, color = TikOnBg, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(2.dp))
                            Text(item.detail, color = TikMuted, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetHeader(title: String, onClose: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = TikOnBg, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
        IconButton(onClick = onClose) {
            Icon(Icons.Outlined.Close, contentDescription = "بستن", tint = TikMuted)
        }
    }
    HorizontalDivider(color = TikBorder)
}

/* ───────────────────────── helpers ───────────────────────── */

private fun statusColor(state: TikNetMainUiState): Color = when {
    state.smartPicking -> TikConnecting
    state.phase == TikNetConnPhase.Connected -> TikConnected
    state.phase == TikNetConnPhase.Connecting || state.phase == TikNetConnPhase.Disconnecting -> TikConnecting
    else -> TikMuted
}

private fun statusLabel(state: TikNetMainUiState): String = when {
    state.smartPicking -> "انتخاب بهترین سرور…"
    state.phase == TikNetConnPhase.Connected -> "متصل به اینترنت"
    state.phase == TikNetConnPhase.Connecting -> "در حال اتصال…"
    state.phase == TikNetConnPhase.Disconnecting -> "در حال قطع…"
    else -> "قطع شده"
}

private fun statusHint(state: TikNetMainUiState): String = when {
    state.smartPicking -> "پینگ سرورها در حال اندازه‌گیری است"
    state.phase == TikNetConnPhase.Connected -> "ترافیک شما از طریق VPN عبور می‌کند"
    state.phase == TikNetConnPhase.Connecting -> "لطفاً چند ثانیه صبر کنید"
    state.phase == TikNetConnPhase.Disconnecting -> "در حال قطع اتصال"
    else -> "برای اتصال، دکمه پایین را بزنید"
}

private fun latencyColor(ms: Long): Color = when {
    ms <= 0 -> TikConnected
    ms <= 120 -> TikConnected
    ms <= 250 -> TikWarn
    else -> TikOrange
}

private fun flagFromRemarks(remarks: String): String {
    val emoji = remarks.firstOrNull { Character.getType(it) == Character.OTHER_SYMBOL.toInt() || it.code > 0x1F1E0 }
    if (emoji != null && remarks.any { it.code in 0x1F1E6..0x1F1FF }) {
        val flags = Regex("""[\uD83C][\uDDE6-\uDDFF][\uD83C][\uDDE6-\uDDFF]""").find(remarks)?.value
        if (flags != null) return flags
    }
    val lower = remarks.lowercase()
    return when {
        "آلمان" in remarks || "germany" in lower || "de" == lower.take(2) -> "🇩🇪"
        "هلند" in remarks || "netherlands" in lower || "nl" in lower -> "🇳🇱"
        "انگلیس" in remarks || "britain" in lower || "uk" in lower || "gb" in lower -> "🇬🇧"
        "آمریکا" in remarks || "usa" in lower || "united states" in lower -> "🇺🇸"
        "فرانسه" in remarks || "france" in lower -> "🇫🇷"
        "ترکیه" in remarks || "turkey" in lower || "türkiye" in lower -> "🇹🇷"
        "امارات" in remarks || "uae" in lower || "dubai" in lower -> "🇦🇪"
        "کانادا" in remarks || "canada" in lower -> "🇨🇦"
        "سوئد" in remarks || "sweden" in lower -> "🇸🇪"
        "فنلاند" in remarks || "finland" in lower -> "🇫🇮"
        "ژاپن" in remarks || "japan" in lower -> "🇯🇵"
        "سنگاپور" in remarks || "singapore" in lower -> "🇸🇬"
        else -> "🌐"
    }
}

@Composable
private fun TikNetLaunchSplash(onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val infinite = rememberInfiniteTransition(label = "splash")
    val pulse by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shieldPulse",
    )
    val ringAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ring",
    )

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2100, easing = LinearEasing),
        )
        onFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(TikBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        scaleX = pulse
                        scaleY = pulse
                    },
            ) {
                Canvas(
                    Modifier
                        .size(120.dp)
                        .graphicsLayer { rotationZ = ringAngle },
                ) {
                    drawArc(
                        color = TikPrimary.copy(alpha = 0.55f),
                        startAngle = -90f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
                Box(
                    Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(TikPrimary.copy(alpha = 0.35f), TikSurface),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = TikPrimary,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            Text(
                "TikNet",
                color = TikOnBg,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "مسیری مطمئن به دنیای اینترنت",
                color = TikMuted,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(36.dp))
            LinearProgressIndicator(
                progress = { progress.value },
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = TikPrimary,
                trackColor = TikBorder,
            )
        }
    }
}
