package com.v2ray.ang.ui.tiknet

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CloudOff
import android.os.Build
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.tiknet.TikNetDevice
import com.v2ray.ang.tiknet.TikNetPrefs
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import android.content.Intent
import com.v2ray.ang.tiknet.TikNetEntitlementAlert
import com.v2ray.ang.tiknet.TikNetEntitlementKind
import com.v2ray.ang.tiknet.TikNetReferralInfo
import com.v2ray.ang.tiknet.TikNetReferralUi
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
import androidx.compose.material.icons.outlined.Widgets
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.v2ray.ang.R
import com.v2ray.ang.dto.AppInfo
import com.v2ray.ang.util.AppIconFetcher
import com.v2ray.ang.tiknet.TikNetAnnouncement
import com.v2ray.ang.tiknet.TikNetAppUpdateState
import com.v2ray.ang.tiknet.TikNetDiagItem
import com.v2ray.ang.tiknet.TikNetDiagStatus
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

    BackHandler(enabled = state.isUpdateBlocking) { }

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
                    TikNetTab.Account -> {
                        LaunchedEffect(Unit) {
                            viewModel.loadUser(silent = true)
                            viewModel.loadReferral()
                        }
                        AccountTab(
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
                            onReloadReferral = { viewModel.loadReferral(force = true) },
                            onAttachReferral = { viewModel.attachReferralCode(it) },
                            onSupportCopied = { viewModel.showMessage("اطلاعات پشتیبانی کپی شد") },
                            onIranDirectChange = { viewModel.setIranDirectEnabled(it) },
                            onWidgetModeChange = { viewModel.setWidgetMode(it) },
                            onWidgetServerChange = { viewModel.setWidgetServerGuid(it) },
                            onPinWidget = { viewModel.pinHomeWidget(com.v2ray.ang.tiknet.TikNetWidgetPin.Kind.Full) },
                            onPinCompactWidget = { viewModel.pinHomeWidget(com.v2ray.ang.tiknet.TikNetWidgetPin.Kind.Compact) },
                        )
                    }
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
                    onTogglePin = { viewModel.togglePinnedServer(it) },
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
                        fixing = state.diagnosticsFixing,
                        onRetry = { viewModel.runDiagnostics() },
                        onAutoFixAll = { viewModel.autoFixDiagnostics() },
                        onAutoFixItem = { viewModel.autoFixDiagItem(it) },
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

        AppUpdateOverlay(
            updateState = state.appUpdate,
            onDismiss = { viewModel.dismissOptionalUpdate() },
            onDownloadAndInstall = { viewModel.downloadAndInstallUpdate() },
        )
    }
}

@Composable
private fun AppUpdateOverlay(
    updateState: TikNetAppUpdateState,
    onDismiss: () -> Unit,
    onDownloadAndInstall: () -> Unit,
) {
    val info = when (updateState) {
        is TikNetAppUpdateState.Available -> updateState.info
        is TikNetAppUpdateState.Downloading -> updateState.info
        is TikNetAppUpdateState.Error -> updateState.info
        else -> null
    } ?: return

    val force = info.force
    val title = if (force) "بروزرسانی اجباری" else "نسخه جدید"
    val buttonLabel = when (updateState) {
        is TikNetAppUpdateState.Error -> "تلاش دوباره"
        is TikNetAppUpdateState.Downloading -> "در حال دانلود…"
        else -> "دانلود و نصب"
    }

    AlertDialog(
        onDismissRequest = { if (!force) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !force,
            dismissOnClickOutside = !force,
        ),
        containerColor = TikSurface,
        titleContentColor = TikOnBg,
        textContentColor = TikMuted,
        title = { Text(title) },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    "نسخه ${info.versionName.ifBlank { TikNetJalali.toPersianDigits(info.versionCode.toString()) }}",
                    color = TikOnBg,
                    fontWeight = FontWeight.SemiBold,
                )
                if (info.changelog.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(info.changelog, color = TikMuted, lineHeight = 20.sp)
                }
                if (updateState is TikNetAppUpdateState.Downloading) {
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { updateState.progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = TikPrimary,
                        trackColor = TikBorder,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "در حال دانلود: ${TikNetJalali.toPersianDigits(updateState.progress.toString())}%",
                        color = TikMuted,
                        fontSize = 13.sp,
                    )
                }
                if (updateState is TikNetAppUpdateState.Error) {
                    Spacer(Modifier.height(10.dp))
                    Text(updateState.message, color = TikDanger)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDownloadAndInstall,
                enabled = updateState !is TikNetAppUpdateState.Downloading,
            ) {
                Text(buttonLabel, color = TikPrimary)
            }
        },
        dismissButton = {
            if (!force) {
                TextButton(onClick = onDismiss) {
                    Text("بعداً", color = TikMuted)
                }
            }
        },
    )
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
                destinationLine = destinationLine(state),
                connected = state.phase == TikNetConnPhase.Connected && !state.smartPicking,
                busy = busy,
            )
            Spacer(Modifier.height(16.dp))
            ServerSelectorCard(
                title = if (state.smartMode) "اتصال هوشمند" else state.selectedTitle,
                smartMode = state.smartMode,
                onClick = onOpenServers,
            )
            Spacer(Modifier.height(16.dp))
            AnnouncementBanner(state.announcement)
            state.entitlementAlert?.let { alert ->
                Spacer(Modifier.height(12.dp))
                EntitlementBanner(alert = alert, shopUrl = state.shopUrl)
            }
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
    destinationLine: String,
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
            statusLabel, statusHint, statusColor, destinationLine,
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
            destinationLine,
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
    destinationLine: String,
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
                    "مقصد: $destinationLine",
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
private fun EntitlementBanner(alert: TikNetEntitlementAlert, shopUrl: String?) {
    val uriHandler = LocalUriHandler.current
    val color = when (alert.kind) {
        TikNetEntitlementKind.Expired -> TikDanger
        TikNetEntitlementKind.ExpiringSoon -> if (alert.severe) TikDanger else TikOrange
        TikNetEntitlementKind.LowTraffic -> TikWarn
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = color)
            Spacer(Modifier.width(10.dp))
            Text(alert.message, color = TikOnBg, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f))
        }
        if (!shopUrl.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = { runCatching { uriHandler.openUri(shopUrl) } },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
            ) {
                Text("خرید و تمدید", color = color, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
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
    onTogglePin: (String) -> Unit,
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
                    pinned = state.pinnedServers.contains(server.guid),
                    onClick = { onSelectServer(server.guid) },
                    onTogglePin = { onTogglePin(server.guid) },
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
    pinned: Boolean = false,
    onTogglePin: (() -> Unit)? = null,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = TikOnBg, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (pinned) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Outlined.PushPin, contentDescription = null, tint = TikWarn, modifier = Modifier.size(14.dp))
                }
            }
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = TikMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (onTogglePin != null) {
            IconButton(
                onClick = onTogglePin,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    Icons.Outlined.PushPin,
                    contentDescription = if (pinned) "برداشتن پین" else "پین سرور",
                    tint = if (pinned) TikWarn else TikMuted,
                    modifier = Modifier.size(18.dp),
                )
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
    val (flag, ipText) = TikNetMessages.formatExitIp(
        state.exitIpText.takeIf { it.isNotBlank() },
    )
    val exitValue = when {
        state.exitIpText.isBlank() && connected -> "در حال دریافت…"
        state.exitIpText.isBlank() -> "—"
        else -> ipText
    }
    val sessionTotal = state.sessionUp + state.sessionDown
    val downShare = if (sessionTotal > 0) state.sessionDown.toFloat() / sessionTotal.toFloat() else 0.5f

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .padding(bottom = 20.dp),
    ) {
        Text("جزئیات اتصال", color = TikOnBg, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("وضعیت تونل، مسیر و مصرف این نشست", color = TikMuted, fontSize = 13.sp)

        Spacer(Modifier.height(16.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(TikSurface)
                .border(1.dp, statusColor.copy(alpha = 0.32f), RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.16f))
                    .border(1.dp, statusColor.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(statusLabel(state), color = statusColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text(statusHint(state), color = TikMuted, fontSize = 12.sp, lineHeight = 16.sp)
            }
        }

        Spacer(Modifier.height(18.dp))
        AccountSectionLabel("وضعیت", "اتصال و مدت نشست")
        DetailCard {
            DetailRow(Icons.Outlined.VerifiedUser, "وضعیت", statusLabel(state), statusColor)
            ThinDivider()
            DetailRow(Icons.Outlined.Timelapse, "مدت اتصال", uptime)
        }

        Spacer(Modifier.height(18.dp))
        AccountSectionLabel("سرور و مسیر", "نود انتخابی و تأخیر")
        DetailCard {
            DetailRow(Icons.Outlined.Dns, "سرور انتخابی", state.selectedTitle.ifBlank { "—" })
            ThinDivider()
            DetailRow(Icons.Outlined.Route, "پروتکل", selected?.protocolLabel?.ifBlank { "—" } ?: "—")
            ThinDivider()
            DetailRow(
                icon = Icons.Outlined.Speed,
                label = "پینگ سرور",
                value = selected?.pingMs?.let { TikNetJalali.toPersianDigits("$it ms") } ?: "—",
                trailing = {
                    val ping = selected?.pingMs
                    if (ping != null) PingChip(ping) else Text("—", color = TikOnBg, fontWeight = FontWeight.Medium)
                },
            )
            if (state.currentDelayText.isNotBlank()) {
                ThinDivider()
                DetailRow(
                    Icons.Outlined.Speed,
                    "تأخیر هسته",
                    TikNetMessages.coreDelay(state.currentDelayText),
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        AccountSectionLabel("آی‌پی خروجی", "مقصد ترافیک بعد از تونل")
        DetailCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(TikSurface2)
                        .border(1.dp, TikBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (state.exitIpText.isBlank() && !connected) "🌐" else flag.ifBlank { "🌐" },
                        fontSize = 26.sp,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("آی‌پی خروجی", color = TikMuted, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        exitValue,
                        color = TikOnBg,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                    if (connected && state.exitIpText.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            destinationLine(state),
                            color = TikMuted,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        AccountSectionLabel("سرعت لحظه‌ای", "دانلود و آپلود زنده")
        DetailCard {
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

        Spacer(Modifier.height(18.dp))
        AccountSectionLabel("ترافیک این نشست", "حجم از زمان وصل شدن")
        DetailCard {
            if (connected && sessionTotal > 0) {
                Row(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp))) {
                    Box(Modifier.weight(downShare.coerceIn(0.08f, 0.92f)).fillMaxSize().background(TikConnected))
                    Box(Modifier.weight((1f - downShare).coerceIn(0.08f, 0.92f)).fillMaxSize().background(TikPrimary))
                }
                Spacer(Modifier.height(12.dp))
            }
            DetailRow(
                Icons.Outlined.ArrowDownward,
                "حجم دانلود",
                if (connected) TikNetJalali.toPersianDigits(TikNetJalali.formatSize(state.sessionDown)) else "—",
                TikConnected,
            )
            ThinDivider()
            DetailRow(
                Icons.Outlined.ArrowUpward,
                "حجم آپلود",
                if (connected) TikNetJalali.toPersianDigits(TikNetJalali.formatSize(state.sessionUp)) else "—",
                TikPrimary,
            )
            ThinDivider()
            DetailRow(
                Icons.Outlined.SwapVert,
                "مجموع نشست",
                if (connected) {
                    TikNetJalali.toPersianDigits(TikNetJalali.formatSize(sessionTotal))
                } else {
                    "—"
                },
            )
        }

        Spacer(Modifier.height(18.dp))
        AccountSectionLabel("هسته", "موتور VPN و حالت انتخاب سرور")
        DetailCard {
            DetailRow(Icons.Outlined.Memory, "وضعیت هسته", if (connected) "فعال" else "متوقف", if (connected) TikConnected else TikMuted)
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
private fun DetailCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(TikSurface)
            .border(1.dp, TikBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = TikOnBg,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(TikSurface2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = TikMuted, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(label, color = TikMuted, fontSize = 13.sp, modifier = Modifier.weight(1f))
        if (trailing != null) {
            trailing()
        } else {
            Text(
                value,
                color = valueColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                textAlign = TextAlign.End,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.15f),
            )
        }
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
            .clip(RoundedCornerShape(16.dp))
            .background(TikSurface2)
            .border(1.dp, iconColor.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(label, color = TikMuted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(10.dp))
        Text(value, color = TikOnBg, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1)
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
                if (state.filterEnabled) {
                    val pinned = remember(state.filterApps, state.filterSelected) {
                        state.filterApps.filter { state.filterSelected.contains(it.packageName) }
                    }
                    if (pinned.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "اپ‌های روشن‌شده (${TikNetJalali.toPersianDigits(pinned.size.toString())})",
                            color = TikOnBg,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(pinned, key = { "pin-${it.packageName}" }) { app ->
                                AppFilterPinnedChip(
                                    app = app,
                                    onClick = {
                                        viewModel.toggleFilterApp(app.packageName)
                                        onFilterChangedRestart()
                                    },
                                )
                            }
                        }
                    }
                }
            }

            val apps = remember(state.filterApps, state.filterQuery, state.filterSelected, state.filterEnabled) {
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
private fun AppFilterPinnedChip(app: AppInfo, onClick: () -> Unit) {
    val context = LocalContext.current
    val iconRequest = remember(app.packageName) {
        ImageRequest.Builder(context)
            .data("appicon:${app.packageName}")
            .fetcherFactory(AppIconFetcher.Factory(context))
            .build()
    }
    Row(
        Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(TikSurface)
            .border(1.dp, TikPrimary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = iconRequest,
            contentDescription = app.appName,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Fit,
            error = painterResource(R.drawable.ic_image_24dp),
            fallback = painterResource(R.drawable.ic_image_24dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            app.appName,
            color = TikOnBg,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
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
    val context = LocalContext.current
    val iconRequest = remember(app.packageName) {
        ImageRequest.Builder(context)
            .data("appicon:${app.packageName}")
            .fetcherFactory(AppIconFetcher.Factory(context))
            .build()
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TikSurface)
            .border(1.dp, TikBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = iconRequest,
            contentDescription = app.appName,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Fit,
            error = painterResource(R.drawable.ic_image_24dp),
            fallback = painterResource(R.drawable.ic_image_24dp),
        )
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
    onReloadReferral: () -> Unit,
    onAttachReferral: (String) -> Unit,
    onSupportCopied: () -> Unit = {},
    onIranDirectChange: (Boolean) -> Unit = {},
    onWidgetModeChange: (String) -> Unit = {},
    onWidgetServerChange: (String?) -> Unit = {},
    onPinWidget: () -> Unit = {},
    onPinCompactWidget: () -> Unit = {},
) {
    val user = state.user
    val expired = user?.isExpired == true
    val hasSub = user?.hasSubscription == true
    val active = user != null && hasSub && user.isExpired != true
    val displayName = user?.fullName?.takeIf { it.isNotBlank() } ?: user?.username ?: "—"
    val accent = when {
        state.userLoading && user == null -> TikMuted
        expired && hasSub -> TikDanger
        !hasSub && user != null -> TikWarn
        active -> TikConnected
        else -> TikPrimary
    }
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val shopUrl = state.shopUrl
    fun supportTicketText(): String {
        val u = state.user?.username ?: TikNetPrefs.getUsername(context) ?: "—"
        val device = runCatching { TikNetDevice.getOrCreateDeviceId(context).take(8) }.getOrDefault("—")
        return buildString {
            appendLine("TikNet پشتیبانی")
            appendLine("user: @$u")
            appendLine("version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("android: ${Build.VERSION.RELEASE} / SDK ${Build.VERSION.SDK_INT}")
            appendLine("model: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("device: $device")
            if (state.profileOffline) appendLine("profile: offline-cache")
        }.trim()
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .padding(bottom = 20.dp),
    ) {
        Text("حساب من", color = TikOnBg, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("پروفایل، اشتراک و تنظیمات دستگاه", color = TikMuted, fontSize = 13.sp)

        Spacer(Modifier.height(16.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(TikSurface)
                .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.55f))))
                    .border(2.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    displayName.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    displayName,
                    color = TikOnBg,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!user?.username.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text("@${user!!.username}", color = TikMuted, fontSize = 13.sp, maxLines = 1)
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusBadge(
                        expired = expired,
                        active = active,
                        hasSub = hasSub,
                        loading = state.userLoading && user == null,
                    )
                    if (state.profileOffline) OfflineBadge()
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        AccountSectionLabel("اشتراک", "وضعیت پلن و مصرف")
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(TikSurface)
                .border(1.dp, TikBorder, RoundedCornerShape(20.dp))
                .padding(14.dp),
        ) {
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
                StatTile(
                    Modifier.weight(1f),
                    Icons.Outlined.VerifiedUser,
                    "وضعیت",
                    when {
                        expired -> "منقضی"
                        active -> "فعال"
                        user != null && !hasSub -> "بدون سرویس"
                        else -> "—"
                    },
                )
            }

            val hasTraffic = (user?.trafficUsedBytes ?: 0) > 0 || (user?.trafficLimitBytes ?: 0) > 0
            if (hasTraffic) {
                Spacer(Modifier.height(10.dp))
                TrafficCard(user)
            }

            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onSync,
                enabled = !state.busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TikPrimary, contentColor = Color.White),
            ) {
                if (state.busy) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("در حال بروزرسانی…", fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(Icons.Outlined.Sync, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("بروزرسانی اشتراک", fontWeight = FontWeight.SemiBold)
                }
            }
            if (!shopUrl.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { runCatching { uriHandler.openUri(shopUrl) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (expired || !hasSub) TikOrange else TikConnected,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.Outlined.ShoppingCart, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        state.shopLabel?.takeIf { it.isNotBlank() } ?: "خرید و تمدید",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        val tg = state.telegramSupport
        if (!tg.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    clipboard.setText(AnnotatedString(supportTicketText()))
                    onSupportCopied()
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
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, TikPrimary.copy(alpha = 0.45f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TikOnBg),
            ) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(TikPrimary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Chat, contentDescription = null, tint = TikPrimary, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text("پشتیبانی (با کپی مشخصات)", fontWeight = FontWeight.SemiBold)
            }
        }

        if (!state.referralDisabled) {
            Spacer(Modifier.height(22.dp))
            AccountSectionLabel("معرف", "دعوت دوستان و دریافت جایزه")
            ReferralSection(
                state = state,
                onReload = onReloadReferral,
                onAttach = onAttachReferral,
            )
        }

        Spacer(Modifier.height(22.dp))
        AccountSectionLabel("تنظیمات اتصال", "مسیریابی ایران و ویجت خانه")
        ConnectionSettingsCard(
            state = state,
            onIranDirectChange = onIranDirectChange,
            onWidgetModeChange = onWidgetModeChange,
            onWidgetServerChange = onWidgetServerChange,
            onPinWidget = onPinWidget,
            onPinCompactWidget = onPinCompactWidget,
        )

        Spacer(Modifier.height(22.dp))
        AccountSectionLabel("خدمات", "اعلان‌ها، راهنما و عیب‌یابی")
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(TikSurface)
                .border(1.dp, TikBorder, RoundedCornerShape(20.dp)),
        ) {
            ServiceTile(
                icon = Icons.Outlined.Notifications,
                label = "اعلان‌ها",
                onClick = onOpenNotifications,
                badge = state.unreadCount,
            )
            HorizontalDivider(color = TikBorder, modifier = Modifier.padding(start = 60.dp))
            ServiceTile(Icons.Outlined.HelpOutline, "راهنما و سوالات", onOpenFaq)
            HorizontalDivider(color = TikBorder, modifier = Modifier.padding(start = 60.dp))
            ServiceTile(Icons.Outlined.Troubleshoot, "عیب‌یابی اینترنت گوشی", onOpenDiagnostics)
            HorizontalDivider(color = TikBorder, modifier = Modifier.padding(start = 60.dp))
            ServiceTile(Icons.Outlined.ContentCopy, "کپی اطلاعات پشتیبانی") {
                clipboard.setText(AnnotatedString(supportTicketText()))
                onSupportCopied()
            }
        }

        Spacer(Modifier.height(18.dp))
        OutlinedButton(
            onClick = onLogoutClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, TikDanger.copy(alpha = 0.55f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TikDanger),
        ) {
            Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("خروج از حساب", fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(14.dp))
        Text(
            "TikNet  ·  نسخه ${TikNetJalali.toPersianDigits(state.appVersion)}",
            color = TikMuted.copy(alpha = 0.85f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AccountSectionLabel(title: String, hint: String? = null) {
    Column(Modifier.padding(bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TikPrimary),
            )
            Spacer(Modifier.width(8.dp))
            Text(title, color = TikOnBg, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        if (!hint.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(hint, color = TikMuted, fontSize = 12.sp, modifier = Modifier.padding(start = 11.dp))
        }
    }
}

@Composable
private fun ReferralSection(
    state: TikNetMainUiState,
    onReload: () -> Unit,
    onAttach: (String) -> Unit,
) {
    if (state.referralDisabled) return
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var attachCode by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TikSurface)
            .border(1.dp, TikBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        when {
            state.referralLoading && state.referral == null -> {
                Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TikPrimary, modifier = Modifier.size(24.dp))
                }
            }
            state.referralError != null && state.referral == null -> {
                Text(state.referralError ?: "", color = TikMuted, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onReload) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null, tint = TikPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("تلاش دوباره", color = TikPrimary)
                }
            }
            else -> {
                val info = state.referral
                Text(
                    "دوستان را دعوت کنید؛ با تکمیل هر مرحله جایزه بگیرید.",
                    color = TikMuted,
                    fontSize = 13.sp,
                )
                if (info != null && (info.referrerReward.amount > 0 || info.inviteeReward.amount > 0)) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "پاداش شما: ${info.referrerReward.labelFa} · پاداش دوست: ${info.inviteeReward.labelFa}",
                        color = TikPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text("کد معرف شما", color = TikMuted, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TikSurface2)
                            .border(1.dp, TikBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Text(
                            info?.referralCode?.takeIf { it.isNotBlank() } ?: "—",
                            color = TikOnBg,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val code = info?.referralCode.orEmpty()
                            if (code.isNotBlank()) {
                                clipboard.setText(AnnotatedString(code))
                            }
                        },
                        enabled = !info?.referralCode.isNullOrBlank(),
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "کپی", tint = TikPrimary)
                    }
                    IconButton(
                        onClick = {
                            val body = info?.let { TikNetReferralUi.shareBody(it) }.orEmpty()
                            if (body.isBlank()) return@IconButton
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "دعوت به تیک‌نت")
                                putExtra(Intent.EXTRA_TEXT, body)
                            }
                            context.startActivity(Intent.createChooser(send, "اشتراک‌گذاری"))
                        },
                        enabled = !info?.referralCode.isNullOrBlank(),
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = "اشتراک‌گذاری", tint = TikPrimary)
                    }
                }
                if (info != null) {
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReferralStatChip(Modifier.weight(1f), "دعوت‌ها", TikNetJalali.toPersianDigits(info.stats.invitedCount.toString()))
                        ReferralStatChip(Modifier.weight(1f), "پاداش‌خورده", TikNetJalali.toPersianDigits(info.stats.rewardedCount.toString()))
                        ReferralStatChip(Modifier.weight(1f), "در انتظار", TikNetJalali.toPersianDigits(info.stats.pendingCount.toString()))
                    }
                    Spacer(Modifier.height(14.dp))
                    ReferralProgressBox(info)
                    val attached = info.attachedReferrerCode?.trim().orEmpty()
                    if (attached.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Text("معرف شما: $attached", color = TikOnBg, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    } else if (info.canAttachReferrer) {
                        Spacer(Modifier.height(14.dp))
                        Text("کد معرف دارید؟", color = TikMuted, fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = attachCode,
                                onValueChange = { attachCode = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                placeholder = { Text("کد معرف", color = TikMuted) },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { onAttach(attachCode) }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TikPrimary,
                                    unfocusedBorderColor = TikBorder,
                                    focusedTextColor = TikOnBg,
                                    unfocusedTextColor = TikOnBg,
                                    cursorColor = TikPrimary,
                                ),
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { onAttach(attachCode) },
                                enabled = !state.referralAttaching && attachCode.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = TikPrimary, contentColor = Color.White),
                            ) {
                                if (state.referralAttaching) {
                                    CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text("ثبت")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReferralStatChip(modifier: Modifier, label: String, value: String) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(TikSurface2)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = TikOnBg, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(2.dp))
        Text(label, color = TikMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ReferralProgressBox(info: TikNetReferralInfo) {
    if (TikNetReferralUi.completedAll(info)) {
        val rewarded = info.progress?.rewardedCount ?: info.stats.rewardedCount
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(TikConnected.copy(alpha = 0.12f))
                .border(1.dp, TikConnected.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .padding(12.dp),
        ) {
            Text(
                "همه مراحل دعوت تکمیل شد. تعداد دعوت‌های موفق: ${TikNetJalali.toPersianDigits(rewarded.toString())}",
                color = TikOnBg,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
        }
        return
    }
    val label = TikNetJalali.toPersianDigits(TikNetReferralUi.progressLabel(info))
    val ratio = TikNetReferralUi.progressRatio(info).toFloat()
    val caption = TikNetReferralUi.rewardCaption(info)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TikSurface2)
            .border(1.dp, TikBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Text(
            "تعداد کاربر دعوت‌شده با کد شما: $label",
            color = TikOnBg,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(8.dp)),
            color = TikPrimary,
            trackColor = TikBorder,
        )
        Spacer(Modifier.height(10.dp))
        Text(caption, color = TikPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ConnectionSettingsCard(
    state: TikNetMainUiState,
    onIranDirectChange: (Boolean) -> Unit,
    onWidgetModeChange: (String) -> Unit,
    onWidgetServerChange: (String?) -> Unit,
    onPinWidget: () -> Unit = {},
    onPinCompactWidget: () -> Unit = {},
) {
    Column(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(TikSurface)
                .border(1.dp, TikBorder, RoundedCornerShape(14.dp))
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "مسیریابی ایران+لوکال به صورت مستقیم",
                        color = TikOnBg,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "با روشن بودن این گزینه، برای همراه‌بانک‌ها، اپ‌های ایرانی و سایت‌های داخلی دیگر لازم نیست فیلتر/VPN تیک‌نت را خاموش کنید.",
                        color = TikMuted,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                    )
                }
                Switch(
                    checked = state.iranDirectEnabled,
                    onCheckedChange = onIranDirectChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = TikPrimary,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = TikMuted.copy(alpha = 0.35f),
                    ),
                )
            }
        }

        // Clear break so widget settings read as a separate concern.
        Spacer(Modifier.height(26.dp))
        HorizontalDivider(thickness = 1.dp, color = TikBorder)
        Spacer(Modifier.height(26.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(TikSurface)
                .border(1.dp, TikBorder, RoundedCornerShape(14.dp))
                .padding(14.dp),
        ) {
            Text("ویجت خانه", color = TikOnBg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "وقتی از ویجت وصل می‌کنید، از کدام سرور استفاده شود.",
                color = TikMuted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
            Spacer(Modifier.height(10.dp))
            WidgetModeOption(
                selected = state.widgetMode == TikNetPrefs.WIDGET_MODE_CURRENT,
                title = "سرور انتخاب‌شده فعلی",
                subtitle = "همان سروری که در اپ انتخاب کرده‌اید",
                onClick = { onWidgetModeChange(TikNetPrefs.WIDGET_MODE_CURRENT) },
            )
            Spacer(Modifier.height(8.dp))
            WidgetModeOption(
                selected = state.widgetMode == TikNetPrefs.WIDGET_MODE_SMART,
                title = "اتصال هوشمند",
                subtitle = "بهترین سرور بر اساس آخرین پینگ ذخیره‌شده",
                onClick = { onWidgetModeChange(TikNetPrefs.WIDGET_MODE_SMART) },
            )
            Spacer(Modifier.height(8.dp))
            WidgetModeOption(
                selected = state.widgetMode == TikNetPrefs.WIDGET_MODE_FIXED,
                title = "سرور ثابت",
                subtitle = "همیشه یک سرور مشخص",
                onClick = { onWidgetModeChange(TikNetPrefs.WIDGET_MODE_FIXED) },
            )
            if (state.widgetMode == TikNetPrefs.WIDGET_MODE_FIXED) {
                Spacer(Modifier.height(10.dp))
                if (state.servers.isEmpty()) {
                    Text("سروری برای انتخاب نیست. اول اشتراک را بروزرسانی کنید.", color = TikMuted, fontSize = 12.sp)
                } else {
                    state.servers.take(12).forEach { server ->
                        val selected = state.widgetServerGuid == server.guid
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) TikPrimary.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable { onWidgetServerChange(server.guid) }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "${flagFromRemarks(server.remarks)} ${server.remarks}",
                                color = TikOnBg,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (selected) {
                                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = TikPrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    if (state.servers.size > 12) {
                        Text(
                            "و ${TikNetJalali.toPersianDigits((state.servers.size - 12).toString())} سرور دیگر…",
                            color = TikMuted,
                            fontSize = 11.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = onPinWidget,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, TikPrimary.copy(alpha = 0.55f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TikPrimary),
            ) {
                Icon(Icons.Outlined.Widgets, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("افزودن ویجت به صفحه اصلی", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "ویجت کامل اتصال تیک‌نت را روی صفحهٔ اصلی دستگاه می‌گذارد",
                color = TikMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onPinCompactWidget,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, TikPrimary.copy(alpha = 0.35f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TikOnBg),
            ) {
                Icon(Icons.Outlined.PushPin, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("افزودن ویجت کوچک (فقط آیکون)", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "یک ویجت خیلی کوچک فقط با آیکون، بدون متن و جزئیات",
                color = TikMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
        }
    }
}

@Composable
private fun WidgetModeOption(
    selected: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, if (selected) TikPrimary else TikBorder, RoundedCornerShape(12.dp))
            .background(if (selected) TikPrimary.copy(alpha = 0.1f) else TikSurface2)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(18.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) TikPrimary else TikMuted, CircleShape)
                .padding(3.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(TikPrimary))
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TikOnBg, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(subtitle, color = TikMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun OfflineBadge() {
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(TikMuted.copy(alpha = 0.15f))
            .border(1.dp, TikMuted.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = TikMuted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("آفلاین", color = TikMuted, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

@Composable
private fun StatusBadge(expired: Boolean, active: Boolean, hasSub: Boolean, loading: Boolean = false) {
    val (color, label, icon) = when {
        loading -> Triple(TikMuted, "در حال بارگذاری…", Icons.Outlined.Info)
        expired && hasSub -> Triple(TikDanger, "منقضی", Icons.Outlined.ErrorOutline)
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
            .background(TikSurface2)
            .border(1.dp, TikBorder, RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(TikPrimary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = TikPrimary, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(7.dp))
            Text(label, color = TikMuted, fontSize = 11.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            value,
            color = TikOnBg,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
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
    val barColor = when {
        ratio >= 0.9f -> TikDanger
        ratio >= 0.7f -> TikOrange
        else -> TikPrimary
    }
    val percent = TikNetJalali.toPersianDigits(((ratio * 100f).toInt().coerceIn(0, 100)).toString())

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(TikSurface2)
            .border(1.dp, TikBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("مصرف حجم", color = TikMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
            if ((limit ?: 0) > 0) {
                Text("$percent٪", color = barColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            TikNetJalali.toPersianDigits(label),
            color = TikOnBg,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
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
                trackColor = Color.White.copy(alpha = 0.08f),
            )
        }
    }
}

@Composable
private fun ServiceTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    badge: Int = 0,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(TikPrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = TikPrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(label, color = TikOnBg, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        if (badge > 0) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(TikDanger)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            ) {
                Text(
                    TikNetJalali.toLatinDigits(if (badge > 99) "99+" else badge.toString()),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
            contentDescription = null,
            tint = TikMuted,
        )
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
    fixing: Boolean,
    onRetry: () -> Unit,
    onAutoFixAll: () -> Unit,
    onAutoFixItem: (TikNetDiagItem) -> Unit,
    onOpenSettings: (String?) -> Unit,
    onClose: () -> Unit,
) {
    val busy = loading || fixing
    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.88f)
            .padding(bottom = 16.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "عیب‌یابی اینترنت گوشی",
                color = TikOnBg,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRetry, enabled = !busy) {
                Icon(Icons.Outlined.Refresh, contentDescription = "بررسی مجدد", tint = TikOnBg)
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, contentDescription = "بستن", tint = TikMuted)
            }
        }
        Text(
            "مواردی که معمولاً اینترنت یا VPN را خراب می‌کنند بررسی می‌شوند.",
            color = TikMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onAutoFixAll,
            enabled = !busy && items.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TikPrimary, contentColor = Color.White),
            shape = RoundedCornerShape(12.dp),
        ) {
            if (fixing) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text("در حال رفع خودکار…")
            } else {
                Icon(Icons.Outlined.Troubleshoot, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("رفع خودکار مشکلات")
            }
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = TikBorder)
        when {
            loading && items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                items(items, key = { it.id }) { item ->
                    val color = when (item.status) {
                        TikNetDiagStatus.Ok -> TikConnected
                        TikNetDiagStatus.Fail -> TikDanger
                        TikNetDiagStatus.Warn -> TikOrange
                        TikNetDiagStatus.Info -> TikMuted
                    }
                    val icon = when (item.status) {
                        TikNetDiagStatus.Ok -> Icons.Outlined.CheckCircle
                        TikNetDiagStatus.Fail -> Icons.Outlined.ErrorOutline
                        TikNetDiagStatus.Warn -> Icons.Outlined.WarningAmber
                        TikNetDiagStatus.Info -> Icons.Outlined.Info
                    }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(TikSurface2)
                            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(icon, contentDescription = null, tint = color)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.title, color = TikOnBg, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(2.dp))
                                Text(item.detail, color = TikMuted, fontSize = 13.sp, lineHeight = 18.sp)
                            }
                        }
                        if (item.settingsAction != null || item.autoFix != null) {
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (item.autoFix != null &&
                                    (item.status == TikNetDiagStatus.Fail || item.status == TikNetDiagStatus.Warn)
                                ) {
                                    TextButton(
                                        onClick = { onAutoFixItem(item) },
                                        enabled = !busy,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    ) {
                                        Text("رفع خودکار", color = TikPrimary, fontSize = 13.sp)
                                    }
                                }
                                if (item.settingsAction != null) {
                                    TextButton(
                                        onClick = { onOpenSettings(item.settingsAction) },
                                        enabled = !busy,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    ) {
                                        Text("باز کردن تنظیمات", color = TikMuted, fontSize = 13.sp)
                                    }
                                }
                            }
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
    state.smartPicking -> "در حال یافتن سریع‌ترین سرور…"
    state.phase == TikNetConnPhase.Connected -> "متصل به اینترنت"
    state.phase == TikNetConnPhase.Connecting -> "در حال اتصال…"
    state.phase == TikNetConnPhase.Disconnecting -> "در حال قطع…"
    else -> "قطع شده"
}

private fun statusHint(state: TikNetMainUiState): String = when {
    state.smartPicking -> "پینگ همهٔ سرورها گرفته می‌شود و بهترین انتخاب می‌شود"
    state.phase == TikNetConnPhase.Connected -> "ترافیک شما از طریق VPN عبور می‌کند"
    state.phase == TikNetConnPhase.Connecting -> "لطفاً چند ثانیه صبر کنید"
    state.phase == TikNetConnPhase.Disconnecting -> "در حال قطع اتصال"
    else -> "برای اتصال، دکمه پایین را بزنید"
}

private fun destinationLine(state: TikNetMainUiState): String {
    if (state.smartPicking) return "در حال یافتن…"
    val connected = state.phase == TikNetConnPhase.Connected
    if (!connected) return "—"
    if (state.exitIpText.isBlank()) return "در حال دریافت…"
    val remarks = state.servers.firstOrNull { it.guid == state.selectedGuid }?.remarks
    return TikNetMessages.formatDestination(state.exitIpText, remarks)
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
                    Image(
                        painter = painterResource(R.drawable.tiknet_shield),
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .padding(6.dp),
                        contentScale = ContentScale.Fit,
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
