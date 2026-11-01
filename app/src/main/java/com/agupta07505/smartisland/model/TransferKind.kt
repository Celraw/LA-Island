/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * What a progress card (IslandMode.DownloadUpload) is doing. Each kind has its own Home switch and wording on the
 * card; `pointsUp` picks the arrow direction of the transfer glyph.
 */
enum class TransferKind(val pointsUp: Boolean, val badge: String, val fallbackTitle: String) {
    DOWNLOAD(false, "下载中", "正在下载文件"),
    UPLOAD(true, "上传中", "正在上传文件"),
    BACKUP(true, "备份中", "正在备份"),
    COPY_MOVE(false, "复制中", "正在复制文件"),
    DELETE(false, "删除中", "正在删除文件"),
    INSTALL(false, "安装中", "正在安装应用")
}

