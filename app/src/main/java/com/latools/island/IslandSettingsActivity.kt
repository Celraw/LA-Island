/*
 * LA 灵动岛插件设置页。
 *
 * 页面结构按 SmartIsland 原版：Studio / Position / Demo / Settings 四个主 Tab。
 * 仅把 UI 组件替换为 Miuix；引擎和数据仍来自 SmartIsland（GPL-3.0）。
 * 插件没有桌面入口，只能由官方签名的 LA Tools 主应用通过 signature 权限启动。
 */

package com.latools.island

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.IslandEvent
import com.agupta07505.smartisland.service.SmartIslandNotificationListenerService
import com.agupta07505.smartisland.service.SmartIslandOverlayService
import com.agupta07505.smartisland.util.DemoEvents
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Stopwatch
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import javax.inject.Inject

@AndroidEntryPoint
class IslandSettingsActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SmartIslandSettingsRepository
    @Inject lateinit var notificationRepository: INotificationRepository
    private var resumeTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val caller = callingPackage
        if (caller != null && caller != "com.latools.app") {
            finish()
            return
        }
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(
                initialValue = SmartIslandSettings.Default,
            )
            @Suppress("UNUSED_EXPRESSION")
            resumeTick
            MiuixTheme {
                IslandStudio(
                    settings = settings,
                    repository = settingsRepository,
                    notificationRepository = notificationRepository,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTick++
    }
}

@Composable
private fun IslandStudio(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    notificationRepository: INotificationRepository,
) {
    var tab by remember { mutableIntStateOf(0) }
    val items = listOf(
        NavigationItem("工作台", MiuixIcons.GridView),
        NavigationItem("位置", MiuixIcons.Tune),
        NavigationItem("演示", MiuixIcons.Stopwatch),
        NavigationItem("设置", MiuixIcons.Settings),
    )
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "LA 灵动岛",
                color = MiuixTheme.colorScheme.surface,
            )
        },
        bottomBar = {
            NavigationBar(mode = NavigationBarDisplayMode.IconAndText) {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = item.icon,
                        label = item.label,
                    )
                }
            }
        },
        containerColor = MiuixTheme.colorScheme.background,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> StudioPage(settings, repository)
                1 -> PositionPage(settings, repository)
                2 -> DemoPage(settings, notificationRepository)
                else -> SettingsPage(settings, repository)
            }
        }
    }
}

@Composable
private fun StudioPage(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val overlay = Settings.canDrawOverlays(context)
    val accessibility = isAccessibilityServiceEnabled(context)
    val listener = isNotificationListenerEnabled(context)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card {
                Column(Modifier.padding(vertical = 8.dp)) {
                    BasicComponent(
                        title = "工作台",
                        summary = if (settings.enabled) "灵动岛已开启" else "灵动岛已关闭",
                    )
                    SwitchPreference(
                        title = "启用灵动岛",
                        summary = if (settings.enabled) "运行中" else "关闭时所有事件都不显示",
                        checked = settings.enabled,
                        onCheckedChange = { scope.launch { repository.setEnabled(it) } },
                    )
                    if (!overlay || !accessibility) {
                        BasicComponent(
                            title = "缺少权限",
                            summary = "需要悬浮窗和无障碍服务才能显示",
                        )
                    }
                }
            }
        }

        item { SectionTitle("权限") }
        item {
            Card {
                Column(Modifier.padding(vertical = 8.dp)) {
                    PermissionLine("悬浮窗", overlay) {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}"),
                            ),
                        )
                    }
                    PermissionLine("无障碍服务", accessibility) {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                    PermissionLine("通知使用权", listener) {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                }
            }
        }

        item { SectionTitle("事件开关") }
        items(EVENT_GROUPS, key = { it.first }) { (group, events) ->
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        group,
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    events.forEach { event ->
                        SwitchPreference(
                            title = eventTitle(event),
                            summary = eventSummary(event),
                            checked = settings.isEventEnabled(event.id),
                            onCheckedChange = { enabled ->
                                scope.launch { repository.setEventEnabled(event.id, enabled) }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PositionPage(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
) {
    val scope = rememberCoroutineScope()
    var landscape by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        TextButton(
                            text = "竖屏",
                            onClick = { landscape = false },
                        )
                        TextButton(
                            text = "横屏",
                            onClick = { landscape = true },
                        )
                    }
                    PreviewPill(
                        width = if (landscape) settings.landscapeWidth else settings.width,
                        height = if (landscape) settings.landscapeHeight else settings.height,
                        corner = if (landscape) settings.landscapeCornerRadius else settings.cornerRadius,
                        colorLong = settings.pillColor,
                    )
                    SliderRow(
                        "宽度",
                        if (landscape) settings.landscapeWidth else settings.width,
                        60f..220f,
                        "dp",
                    ) { value ->
                        scope.launch {
                            if (landscape) {
                                repository.setLandscapePillGeometry(
                                    value,
                                    settings.landscapeHeight,
                                    settings.landscapeXOffset,
                                    settings.landscapeYOffset,
                                )
                            } else {
                                repository.setWidth(value)
                            }
                        }
                    }
                    SliderRow(
                        "高度",
                        if (landscape) settings.landscapeHeight else settings.height,
                        24f..80f,
                        "dp",
                    ) { value ->
                        scope.launch {
                            if (landscape) {
                                repository.setLandscapePillGeometry(
                                    settings.landscapeWidth,
                                    value,
                                    settings.landscapeXOffset,
                                    settings.landscapeYOffset,
                                )
                            } else {
                                repository.setHeight(value)
                            }
                        }
                    }
                    if (!landscape) {
                        SliderRow("圆角", settings.cornerRadius, 0f..40f, "dp") {
                            scope.launch { repository.setCornerRadius(it) }
                        }
                    }
                    SliderRow(
                        "不透明度",
                        if (landscape) settings.landscapeOpacity else settings.opacity,
                        0.2f..1f,
                        "",
                    ) { value ->
                        scope.launch {
                            if (landscape) repository.setLandscapeOpacity(value) else repository.setOpacity(value)
                        }
                    }
                    SliderRow(
                        "水平偏移",
                        if (landscape) settings.landscapeXOffset else settings.xOffset,
                        -100f..100f,
                        "dp",
                    ) { value ->
                        scope.launch {
                            if (landscape) {
                                repository.setLandscapePillGeometry(
                                    settings.landscapeWidth,
                                    settings.landscapeHeight,
                                    value,
                                    settings.landscapeYOffset,
                                )
                            } else {
                                repository.setXOffset(value)
                            }
                        }
                    }
                    SliderRow(
                        "垂直偏移",
                        if (landscape) settings.landscapeYOffset else settings.yOffset,
                        0f..100f,
                        "dp",
                    ) { value ->
                        scope.launch {
                            if (landscape) {
                                repository.setLandscapePillGeometry(
                                    settings.landscapeWidth,
                                    settings.landscapeHeight,
                                    settings.landscapeXOffset,
                                    value,
                                )
                            } else {
                                repository.setYOffset(value)
                            }
                        }
                    }
                }
            }
        }

        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "行为",
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                    SwitchPreference(
                        title = "阴影",
                        summary = "悬浮岛下方投影",
                        checked = settings.enableShadow,
                        onCheckedChange = { scope.launch { repository.setEnableShadow(it) } },
                    )
                    SwitchPreference(
                        title = "闲置时隐藏",
                        summary = "没有活动时收起胶囊",
                        checked = settings.hideWhenIdle,
                        onCheckedChange = { scope.launch { repository.setHideWhenIdle(it) } },
                    )
                    SwitchPreference(
                        title = "横屏显示",
                        summary = "横屏时也显示灵动岛",
                        checked = settings.showInLandscape,
                        onCheckedChange = { scope.launch { repository.setShowInLandscape(it) } },
                    )
                }
            }
        }

        item { SectionTitle("动画与合并") }
        item {
            Card {
                Column(Modifier.padding(vertical = 8.dp)) {
                    WindowDropdownPreference(
                        title = "动画速度",
                        summary = "当前：" + when (
                            if (landscape) settings.animSpeedLandscape else settings.animSpeedPortrait
                        ) {
                            "slow" -> "慢"
                            "fast" -> "快"
                            else -> "标准"
                        },
                        items = listOf("慢", "标准", "快"),
                        selectedIndex = when (
                            if (landscape) settings.animSpeedLandscape else settings.animSpeedPortrait
                        ) {
                            "slow" -> 0
                            "fast" -> 2
                            else -> 1
                        },
                        onSelectedIndexChange = { index ->
                            val speed = when (index) {
                                0 -> "slow"
                                2 -> "fast"
                                else -> "relaxed"
                            }
                            scope.launch {
                                if (landscape) repository.setAnimSpeedLandscape(speed)
                                else repository.setAnimSpeedPortrait(speed)
                            }
                        },
                    )
                    SwitchPreference(
                        title = "浮动窗口",
                        summary = "允许灵动岛使用浮动窗口模式",
                        checked = settings.floatingWindowEnabled,
                        onCheckedChange = { scope.launch { repository.setFloatingWindowEnabled(it) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun DemoPage(
    settings: SmartIslandSettings,
    notificationRepository: INotificationRepository,
) {
    val context = LocalContext.current
    val cards by notificationRepository.notifications.collectAsStateWithLifecycle()
    val showing = cards.map { it.key }.toSet()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "演示",
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "点击「演示」在灵动岛上放一张示例卡片；短事件会自动消失。",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { DemoEvents.clearAll(notificationRepository) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("清空所有 Demo 卡片")
                    }
                }
            }
        }
        items(DemoEvents.available, key = { it.id }) { event ->
            Card {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            eventTitle(event),
                            style = MiuixTheme.textStyles.body1,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                        Text(
                            eventSummary(event),
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        )
                    }
                    TextButton(
                        text = if (DemoEvents.keyFor(event) in showing) "清除" else "演示",
                        onClick = {
                            if (DemoEvents.keyFor(event) in showing) {
                                DemoEvents.clear(notificationRepository, event)
                            } else {
                                DemoEvents.show(
                                    context,
                                    notificationRepository,
                                    event,
                                    autoExpand = true,
                                    batteryWarningMs = settings.batteryWarningMs.toLong(),
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsPage(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SectionTitle("权限") }
        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    PermissionLine("悬浮窗", Settings.canDrawOverlays(context)) {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}"),
                            ),
                        )
                    }
                    PermissionLine("无障碍服务", isAccessibilityServiceEnabled(context)) {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                    PermissionLine("通知使用权", isNotificationListenerEnabled(context)) {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                }
            }
        }

        item { SectionTitle("外观") }
        item {
            Card {
                Column(Modifier.padding(vertical = 8.dp)) {
                    WindowDropdownPreference(
                        title = "主题模式",
                        summary = when (settings.themeMode) {
                            "light" -> "浅色"
                            "dark" -> "深色"
                            else -> "跟随系统"
                        },
                        items = listOf("跟随系统", "浅色", "深色"),
                        selectedIndex = when (settings.themeMode) {
                            "light" -> 1
                            "dark" -> 2
                            else -> 0
                        },
                        onSelectedIndexChange = { index ->
                            scope.launch {
                                repository.setThemeMode(
                                    when (index) {
                                        1 -> "light"
                                        2 -> "dark"
                                        else -> "system"
                                    },
                                )
                            }
                        },
                    )
                    SwitchPreference(
                        title = "Material You 取色",
                        summary = "从壁纸取色（Android 12+）",
                        checked = settings.materialYou,
                        onCheckedChange = { scope.launch { repository.setMaterialYou(it) } },
                    )
                    WindowDropdownPreference(
                        title = "强调色",
                        summary = "选中颜色",
                        items = ACCENT_NAMES,
                        selectedIndex = ACCENT_COLORS.indexOf(settings.accentColor).coerceAtLeast(0),
                        onSelectedIndexChange = { index ->
                            ACCENT_COLORS.getOrNull(index)?.let { color ->
                                scope.launch { repository.setAccentColor(color) }
                            }
                        },
                    )
                }
            }
        }

        item { SectionTitle("通知") }
        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    SwitchPreference(
                        title = "通知上岛",
                        checked = settings.notificationsEnabled,
                        onCheckedChange = { scope.launch { repository.setNotificationsEnabled(it) } },
                    )
                    SwitchPreference(
                        title = "隐藏通知栏",
                        checked = settings.hideFromNotificationShade,
                        onCheckedChange = { scope.launch { repository.setHideFromNotificationShade(it) } },
                    )
                    SwitchPreference(
                        title = "显示通知操作",
                        checked = settings.showNotificationActions,
                        onCheckedChange = { scope.launch { repository.setShowNotificationActions(it) } },
                    )
                    SwitchPreference(
                        title = "锁屏显示",
                        checked = settings.showOnLockScreen,
                        onCheckedChange = { scope.launch { repository.setShowOnLockScreen(it) } },
                    )
                }
            }
        }

        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "LA 灵动岛插件 v1.0",
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "引擎：SmartIsland · GPL-3.0\n页面：LA Miuix\n入口：仅限 LA Tools",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionLine(label: String, granted: Boolean, onClick: () -> Unit) {
    ArrowPreference(
        title = label,
        summary = if (granted) "已授权" else "未授权，点按去授权",
        onClick = onClick,
    )
}

@Composable
private fun SliderRow(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    suffix: String,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.padding(vertical = 2.dp)) {
        Text(
            "$title：${"%.1f".format(value)}$suffix",
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceSecondary,
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            steps = 0,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun PreviewPill(width: Float, height: Float, corner: Float, colorLong: Long) {
    Box(
        modifier = Modifier.fillMaxWidth().height(90.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width((width.coerceIn(60f, 220f) * 1.2f).dp)
                .height((height.coerceIn(24f, 80f) * 1.2f).dp)
                .background(
                    Color(colorLong),
                    RoundedCornerShape((corner * 1.2f).dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text("09:41", color = Color.White, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MiuixTheme.textStyles.footnote1,
        color = MiuixTheme.colorScheme.onSurfaceSecondary,
        modifier = Modifier.padding(start = 4.dp),
    )
}

private val EVENT_GROUPS: List<Pair<String, List<IslandEvent>>> by lazy {
    IslandEvent.entries.groupBy { event ->
        when (event.group) {
            "Power" -> "电源"
            "Connectivity" -> "连接"
            "Activities" -> "活动"
            else -> event.group
        }
    }.toList()
}

private val ACCENT_COLORS = listOf(
    0xFF0F766EL, 0xFF1D4ED8L, 0xFF6D28D9L, 0xFFBE123CL, 0xFFB45309L, 0xFF0F172AL,
)

private val ACCENT_NAMES = listOf("青绿", "蓝色", "紫色", "红色", "橙色", "深灰")

private fun eventTitle(event: IslandEvent): String = when (event) {
    IslandEvent.PLUGGED_IN -> "连接电源"
    IslandEvent.PLUGGED_OUT -> "断开电源"
    IslandEvent.FULLY_CHARGED -> "充满电"
    IslandEvent.LOW_BATTERY -> "电量低"
    IslandEvent.BATTERY_SAVER -> "省电模式"
    IslandEvent.WIFI -> "Wi-Fi 连接"
    IslandEvent.VPN -> "VPN 连接"
    IslandEvent.WIFI_DISCONNECTED -> "Wi-Fi 断开"
    IslandEvent.VPN_DISCONNECTED -> "VPN 断开"
    IslandEvent.BLUETOOTH -> "蓝牙连接"
    IslandEvent.USB_CONNECTED -> "USB 连接"
    IslandEvent.USB_DISCONNECTED -> "USB 断开"
    IslandEvent.HEADPHONES_CONNECTED -> "耳机连接"
    IslandEvent.HEADPHONES_DISCONNECTED -> "耳机断开"
    IslandEvent.HOTSPOT -> "个人热点"
    IslandEvent.CALL -> "来电"
    IslandEvent.MUSIC -> "音乐播放"
    IslandEvent.NOTIFICATION -> "应用通知"
    IslandEvent.NAVIGATION -> "导航"
    IslandEvent.LIVE_ACTIVITY -> "实时活动"
    IslandEvent.DOWNLOAD -> "下载"
    IslandEvent.UPLOAD -> "上传"
    IslandEvent.BACKUP -> "备份与恢复"
    IslandEvent.FILE_COPY_MOVE -> "复制 / 移动文件"
    IslandEvent.FILE_DELETE -> "删除文件"
    IslandEvent.APP_INSTALL -> "安装应用"
    IslandEvent.FLASHLIGHT -> "手电筒"
    IslandEvent.SCREEN_RECORDING -> "录屏"
    IslandEvent.AUDIO_RECORDING -> "录音"
    IslandEvent.TIMER -> "计时器"
    IslandEvent.STOPWATCH -> "秒表"
    IslandEvent.FACE_UNLOCK -> "人脸解锁"
    else -> event.shortLabel
}

private fun eventSummary(event: IslandEvent): String = when (event) {
    IslandEvent.CALL -> "来电始终显示，建议保持开启"
    IslandEvent.MUSIC -> "显示歌曲和播放控制"
    IslandEvent.NAVIGATION -> "显示转向和距离"
    else -> "允许该事件出现在灵动岛"
}

private fun isAccessibilityServiceEnabled(context: android.content.Context): Boolean {
    val expected = ComponentName(context, SmartIslandOverlayService::class.java)
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ).orEmpty()
    return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
}

private fun isNotificationListenerEnabled(context: android.content.Context): Boolean {
    val expected = ComponentName(context, SmartIslandNotificationListenerService::class.java)
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners",
    ).orEmpty()
    return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
}

