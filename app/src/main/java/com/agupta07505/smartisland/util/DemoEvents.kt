/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.model.IslandEvent
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import com.agupta07505.smartisland.model.TransferKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Sample cards for the Demo tab, posted straight into the notification repository; nothing touches the system shade.
 * Demo keys all start with [KEY_PREFIX].
 *
 * Demo cards follow the same rules as real ones: an event switched off on the Home tab is not shown, and short
 * events (plug-in, Wi-Fi, ...) leave by themselves after their usual time.
 */
object DemoEvents {
    const val KEY_PREFIX = "demo_"
    private const val NOOP_ACTION = "com.agupta07505.smartisland.action.DEMO_NOOP"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val dismissJobs = ConcurrentHashMap<String, Job>()

    /** Events that have a sample card. Face ID is an animation, not a card, so it still lists here and returns none. */
    val available: List<IslandEvent> = IslandEvent.entries.toList()

    fun isDemoKey(key: String): Boolean = key.startsWith(KEY_PREFIX)

    /** Posts the sample card for [event], replacing the previous sample of the same event. */
    fun show(
        context: Context,
        repo: INotificationRepository,
        event: IslandEvent,
        autoExpand: Boolean = true,
        batteryWarningMs: Long = IslandTimings.BATTERY_WARNING_CARD_MS
    ) {
        // The Face ID scan is not a card: the Demo tab starts it directly (see DemoSection).
        if (event == IslandEvent.FACE_UNLOCK) return
        val card = build(context, event) ?: return
        dismissJobs.remove(card.key)?.cancel()
        repo.postNotification(card, autoExpand = autoExpand)
        val lifetime = lifetimeMs(event, batteryWarningMs)
        if (lifetime > 0L) {
            dismissJobs[card.key] = scope.launch {
                delay(lifetime)
                repo.removeNotification(card.key)
                dismissJobs.remove(card.key)
            }
        }
    }

    fun clear(repo: INotificationRepository, event: IslandEvent) {
        val key = keyFor(event)
        dismissJobs.remove(key)?.cancel()
        repo.removeNotification(key)
    }

    fun clearAll(repo: INotificationRepository) {
        dismissJobs.values.forEach { it.cancel() }
        dismissJobs.clear()
        repo.notifications.value.filter { isDemoKey(it.key) }.forEach { repo.removeNotification(it.key) }
    }

    fun keyFor(event: IslandEvent): String = KEY_PREFIX + event.id

    internal fun lifetimeMs(
        event: IslandEvent,
        batteryWarningMs: Long = IslandTimings.BATTERY_WARNING_CARD_MS
    ): Long = when (event) {
        IslandEvent.PLUGGED_IN -> IslandTimings.PLUGGED_IN_CARD_MS
        IslandEvent.PLUGGED_OUT -> IslandTimings.UNPLUGGED_CARD_MS
        IslandEvent.LOW_BATTERY, IslandEvent.BATTERY_SAVER, IslandEvent.FULLY_CHARGED -> batteryWarningMs
        IslandEvent.WIFI, IslandEvent.VPN, IslandEvent.USB_CONNECTED, IslandEvent.HEADPHONES_CONNECTED ->
            IslandTimings.CONNECTIVITY_CARD_MS
        IslandEvent.WIFI_DISCONNECTED, IslandEvent.VPN_DISCONNECTED,
        IslandEvent.USB_DISCONNECTED, IslandEvent.HEADPHONES_DISCONNECTED -> IslandTimings.DISCONNECT_CARD_MS
        else -> 0L
    }

    private fun noop(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(NOOP_ACTION).setPackage(context.packageName),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun build(context: Context, event: IslandEvent): IslandNotification? {
        val now = System.currentTimeMillis()
        val key = keyFor(event)
        val pi = noop(context)
        fun button(title: String) = IslandNotificationAction(title = title, pendingIntent = pi)
        fun battery(category: String, title: String, text: String) = IslandNotification(
            key = key, packageName = "com.android.systemui", appName = "系统",
            title = title, text = text, category = category, mode = IslandMode.Battery, timeMillis = now
        )
        fun transfer(
            kind: TransferKind, pkg: String, app: String, title: String, text: String,
            progress: Int, detail: String
        ) = IslandNotification(
            key = key, packageName = pkg, appName = app, title = title, text = text,
            progress = progress, progressMax = if (progress > 0) 100 else 0,
            mode = IslandMode.DownloadUpload, transferKind = kind, transferDetail = detail, timeMillis = now
        )

        return when (event) {
            IslandEvent.PLUGGED_IN -> battery("battery_plugged_in", "已连接电源", "78%")
            IslandEvent.PLUGGED_OUT -> battery("battery_plugged_out", "已断开电源", "78%")
            IslandEvent.FULLY_CHARGED -> battery("battery_full", "已充满", "100%")
            IslandEvent.LOW_BATTERY -> battery("battery_low", "电量低", "15%")
            IslandEvent.BATTERY_SAVER -> battery("battery_saver", "省电模式", "18%")

            IslandEvent.WIFI -> IslandNotification(
                key = key, packageName = "com.android.systemui", appName = "Wi-Fi",
                title = "家庭 Wi-Fi", text = "已连接", category = "wifi",
                mode = IslandMode.Bluetooth, timeMillis = now
            )
            IslandEvent.VPN -> IslandNotification(
                key = key, packageName = "com.android.systemui", appName = "VPN",
                title = "VPN 已连接", text = "德国法兰克福", category = "vpn",
                mode = IslandMode.Bluetooth, timeMillis = now
            )
            IslandEvent.WIFI_DISCONNECTED -> IslandNotification(
                key = key, packageName = "com.android.systemui", appName = "Wi-Fi",
                title = "家庭 Wi-Fi", text = "已断开", category = "wifi_disconnected",
                mode = IslandMode.Bluetooth, timeMillis = now
            )
            IslandEvent.VPN_DISCONNECTED -> IslandNotification(
                key = key, packageName = "com.android.systemui", appName = "VPN",
                title = "VPN 已断开", text = "流量未受保护", category = "vpn_disconnected",
                mode = IslandMode.Bluetooth, timeMillis = now
            )
            IslandEvent.USB_CONNECTED -> IslandNotification(
                key = key, packageName = "com.android.systemui", appName = "USB",
                title = "USB 设备", text = "已连接", category = "usb_connected",
                mode = IslandMode.Bluetooth, timeMillis = now
            )
            IslandEvent.USB_DISCONNECTED -> IslandNotification(
                key = key, packageName = "com.android.systemui", appName = "USB",
                title = "USB 设备", text = "已断开", category = "usb_disconnected",
                mode = IslandMode.Bluetooth, timeMillis = now
            )
            IslandEvent.HEADPHONES_CONNECTED -> IslandNotification(
                key = key, packageName = "com.android.systemui", appName = "耳机",
                title = "有线耳机", text = "已连接", category = "headphones_connected",
                mode = IslandMode.Bluetooth, timeMillis = now
            )
            IslandEvent.HEADPHONES_DISCONNECTED -> IslandNotification(
                key = key, packageName = "com.android.systemui", appName = "耳机",
                title = "有线耳机", text = "已断开", category = "headphones_disconnected",
                mode = IslandMode.Bluetooth, timeMillis = now
            )
            IslandEvent.BLUETOOTH -> IslandNotification(
                key = key, packageName = "com.android.bluetooth", appName = "蓝牙",
                title = "无线耳机", text = "已连接 · 82%", progress = 82, progressMax = 100,
                mode = IslandMode.Bluetooth, timeMillis = now
            )
            IslandEvent.HOTSPOT -> IslandNotification(
                key = key, packageName = "com.android.systemui", appName = "热点",
                title = "热点已开启", text = "已连接 2 台设备",
                actionIntents = listOf(button("关闭")),
                mode = IslandMode.Hotspot, timeMillis = now
            )

            IslandEvent.CALL -> IslandNotification(
                key = key, packageName = "com.google.android.dialer", appName = "电话",
                title = "联系人", text = "来电",
                actionIntents = listOf(button("拒接"), button("接听")),
                mode = IslandMode.IncomingCall, timeMillis = now
            )
            IslandEvent.MUSIC -> IslandNotification(
                key = key, packageName = "com.spotify.music", appName = "音乐",
                title = "Midnight City", text = "M83",
                actionIntents = listOf(button("上一首"), button("暂停"), button("下一首")),
                mediaPositionMs = 92_000L, mediaDurationMs = 243_000L, mediaIsPlaying = true,
                mode = IslandMode.Music, timeMillis = now
            )
            IslandEvent.NOTIFICATION -> IslandNotification(
                key = key, packageName = "com.google.android.apps.messaging", appName = "短信",
                title = "联系人", text = "今晚还见面吗？",
                bigText = "今晚还见面吗？我订了 8 点的位子，方便的话回我一声。",
                mode = IslandMode.Notification, timeMillis = now
            )
            IslandEvent.NAVIGATION -> IslandNotification(
                key = key, packageName = "com.google.android.apps.maps", appName = "地图",
                title = "前方路口左转", text = "250 米 · 12 分钟 · 4.2 公里",
                mode = IslandMode.Navigation, timeMillis = now
            )
            IslandEvent.LIVE_ACTIVITY -> IslandNotification(
                key = key, packageName = "com.demo.delivery", appName = "外卖",
                title = "订单即将送达", text = "骑手还有 5 分钟",
                progress = 60, progressMax = 100,
                mode = IslandMode.LiveActivity, timeMillis = now
            )

            IslandEvent.DOWNLOAD -> transfer(
                TransferKind.DOWNLOAD, "com.android.chrome", "Chrome",
                "holiday-video.mp4", "62% • 4.2 MB/s", 62, "4.2 MB/s · 剩余 3 分钟"
            )
            IslandEvent.UPLOAD -> transfer(
                TransferKind.UPLOAD, "com.google.android.apps.photos", "Photos",
                "Uploading 24 photos", "35% • 1.8 MB/s", 35, "1.8 MB/s · 剩余 6 分钟"
            )
            IslandEvent.BACKUP -> transfer(
                TransferKind.BACKUP, "com.whatsapp", "WhatsApp",
                "Backing up chats", "48%", 48, ""
            )
            IslandEvent.FILE_COPY_MOVE -> transfer(
                TransferKind.COPY_MOVE, "com.google.android.apps.nbu.files", "Files",
                "Copying 12 items", "71%", 71, ""
            )
            IslandEvent.FILE_DELETE -> transfer(
                TransferKind.DELETE, "com.google.android.apps.nbu.files", "Files",
                "Deleting 30 files", "Please wait", 0, ""
            )
            IslandEvent.APP_INSTALL -> transfer(
                TransferKind.INSTALL, "com.android.vending", "Play Store",
                "Installing Maps", "55%", 55, ""
            )

            IslandEvent.FLASHLIGHT -> IslandNotification(
                key = key, packageName = "com.android.systemui", appName = "手电筒",
                title = "手电筒已开启", text = "点按关闭",
                actionIntents = listOf(button("关闭")),
                mode = IslandMode.Flashlight, timeMillis = now
            )
            IslandEvent.SCREEN_RECORDING -> IslandNotification(
                key = key, packageName = "com.android.systemui", appName = "录屏",
                title = "录屏中", text = "录制中 · 00:12",
                actionIntents = listOf(button("暂停"), button("停止")),
                mode = IslandMode.ScreenRecording, timeMillis = now - 12_000L
            )
            IslandEvent.AUDIO_RECORDING -> IslandNotification(
                key = key, packageName = "com.google.android.apps.recorder", appName = "录音机",
                title = "录音中", text = "录制中 · 00:08",
                actionIntents = listOf(button("暂停"), button("停止")),
                mode = IslandMode.AudioRecording, timeMillis = now - 8_000L
            )
            IslandEvent.TIMER -> IslandNotification(
                key = key, packageName = "com.google.android.deskclock", appName = "时钟",
                title = "计时器", text = "05:00",
                actionIntents = listOf(button("暂停"), button("停止")),
                mode = IslandMode.Timer, timeMillis = now + 300_000L
            )
            IslandEvent.STOPWATCH -> IslandNotification(
                key = key, packageName = "com.google.android.deskclock", appName = "时钟",
                title = "秒表", text = "运行中",
                actionIntents = listOf(button("计次"), button("暂停"), button("重置")),
                mode = IslandMode.Stopwatch, timeMillis = now - 45_000L
            )

            IslandEvent.FACE_UNLOCK -> null
        }
    }
}


