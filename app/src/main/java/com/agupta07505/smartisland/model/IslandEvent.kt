/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Every activity the island can show, each with its own switch. `id` is persisted in
 * SmartIslandSettings.disabledEvents; `label` is the long description (Demo tab), `shortLabel` fits a Home tile.
 */
enum class IslandEvent(val id: String, val label: String, val shortLabel: String, val group: String) {
    PLUGGED_IN("battery_plugged_in", "连接电源（短卡片）", "连接电源", "Power"),
    PLUGGED_OUT("battery_plugged_out", "断开电源", "断开电源", "Power"),
    FULLY_CHARGED("battery_full", "已充满", "已充满", "Power"),
    LOW_BATTERY("battery_low", "电量低", "电量低", "Power"),
    BATTERY_SAVER("battery_saver", "省电模式", "省电", "Power"),

    WIFI("wifi", "Wi-Fi 已连接（显示网络名）", "Wi-Fi", "Connectivity"),
    VPN("vpn", "VPN 已连接（显示位置）", "VPN", "Connectivity"),
    WIFI_DISCONNECTED("wifi_disconnected", "Wi-Fi 已断开", "Wi-Fi 断开", "Connectivity"),
    VPN_DISCONNECTED("vpn_disconnected", "VPN 已断开（流量未受保护）", "VPN 断开", "Connectivity"),
    BLUETOOTH("bluetooth", "蓝牙已连接", "蓝牙", "Connectivity"),
    USB_CONNECTED("usb_connected", "USB 设备已连接", "USB 连接", "Connectivity"),
    USB_DISCONNECTED("usb_disconnected", "USB 设备已断开", "USB 断开", "Connectivity"),
    HEADPHONES_CONNECTED("headphones_connected", "有线耳机已连接", "耳机连接", "Connectivity"),
    HEADPHONES_DISCONNECTED("headphones_disconnected", "有线耳机已断开", "耳机断开", "Connectivity"),
    HOTSPOT("hotspot", "个人热点", "热点", "Connectivity"),

    CALL("call", "来电与通话中", "来电", "Activities"),
    MUSIC("music", "音乐播放器", "音乐", "Activities"),
    NOTIFICATION("notification", "应用通知", "通知", "Activities"),
    NAVIGATION("navigation", "导航", "导航", "Activities"),
    LIVE_ACTIVITY("live_activity", "实时活动", "实时", "Activities"),
    DOWNLOAD("download", "下载", "下载", "Activities"),
    UPLOAD("upload", "上传", "上传", "Activities"),
    BACKUP("backup", "备份与恢复", "备份", "Activities"),
    FILE_COPY_MOVE("file_copy_move", "文件复制与移动", "复制/移动", "Activities"),
    FILE_DELETE("file_delete", "文件删除", "删除", "Activities"),
    APP_INSTALL("app_install", "应用安装与更新", "安装", "Activities"),
    FLASHLIGHT("flashlight", "手电筒", "手电筒", "Activities"),
    SCREEN_RECORDING("screen_recording", "录屏", "录屏", "Activities"),
    AUDIO_RECORDING("audio_recording", "录音", "录音", "Activities"),
    TIMER("timer", "计时器", "计时器", "Activities"),
    STOPWATCH("stopwatch", "秒表", "秒表", "Activities"),
    // drawn by ui/FaceIdLayer.kt over the cutout, not as a card
    FACE_UNLOCK("face_unlock", "人脸解锁动画", "人脸解锁", "Activities");

    /**
     * True when the event has its own auto-expand switch on the Home tab. Calls always open the island, so they have
     * none.
     */
    val canSwitchAutoExpand: Boolean get() = this != CALL && this != FACE_UNLOCK

    /**
     * True when the event has its own "hide on the lock screen" switch. Calls never do: a ringing call must stay
     * reachable.
     */
    val canHideOnLockScreen: Boolean get() = this != CALL && this != FACE_UNLOCK

    companion object {
        /** Event for real app notifications (not system broadcasts). */
        fun forListenerMode(mode: IslandMode, transferKind: TransferKind = TransferKind.DOWNLOAD): IslandEvent? = when (mode) {
            IslandMode.Notification -> NOTIFICATION
            IslandMode.IncomingCall -> CALL
            IslandMode.Music -> MUSIC
            IslandMode.LiveActivity -> LIVE_ACTIVITY
            IslandMode.Navigation -> NAVIGATION
            IslandMode.DownloadUpload -> forTransfer(transferKind)
            IslandMode.Hotspot -> HOTSPOT
            IslandMode.ScreenRecording -> SCREEN_RECORDING
            IslandMode.AudioRecording -> AUDIO_RECORDING
            IslandMode.Timer -> TIMER
            IslandMode.Stopwatch -> STOPWATCH
            else -> null
        }

        fun forTransfer(kind: TransferKind): IslandEvent = when (kind) {
            TransferKind.DOWNLOAD -> DOWNLOAD
            TransferKind.UPLOAD -> UPLOAD
            TransferKind.BACKUP -> BACKUP
            TransferKind.COPY_MOVE -> FILE_COPY_MOVE
            TransferKind.DELETE -> FILE_DELETE
            TransferKind.INSTALL -> APP_INSTALL
        }

        /**
         * Event a posted island notification belongs to, for filtering against the user's switches; null for modes
         * that are never gated (Empty).
         */
        fun forNotification(n: IslandNotification): IslandEvent? = when (n.mode) {
            IslandMode.Empty -> null
            IslandMode.Notification -> NOTIFICATION
            IslandMode.IncomingCall -> CALL
            IslandMode.Music -> MUSIC
            IslandMode.Battery -> when (n.category) {
                "battery_plugged_in" -> PLUGGED_IN
                "battery_plugged_out" -> PLUGGED_OUT
                "battery_full" -> FULLY_CHARGED
                "battery_saver" -> BATTERY_SAVER
                "battery_low" -> LOW_BATTERY
                else -> PLUGGED_IN
            }
            IslandMode.LiveActivity -> LIVE_ACTIVITY
            IslandMode.Navigation -> NAVIGATION
            IslandMode.DownloadUpload ->
                if (n.transferKind == TransferKind.DOWNLOAD && n.key.contains("upload", ignoreCase = true)) {
                    UPLOAD
                } else {
                    forTransfer(n.transferKind)
                }
            IslandMode.Hotspot -> HOTSPOT
            IslandMode.Bluetooth -> when (n.category) {
                "wifi" -> WIFI
                "vpn" -> VPN
                "wifi_disconnected" -> WIFI_DISCONNECTED
                "vpn_disconnected" -> VPN_DISCONNECTED
                "usb_connected" -> USB_CONNECTED
                "usb_disconnected" -> USB_DISCONNECTED
                "headphones_connected" -> HEADPHONES_CONNECTED
                "headphones_disconnected" -> HEADPHONES_DISCONNECTED
                else -> BLUETOOTH
            }
            IslandMode.Flashlight -> FLASHLIGHT
            IslandMode.ScreenRecording -> SCREEN_RECORDING
            IslandMode.AudioRecording -> AUDIO_RECORDING
            IslandMode.Timer -> TIMER
            IslandMode.Stopwatch -> STOPWATCH
        }
    }
}
