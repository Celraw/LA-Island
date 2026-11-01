# LA Island（LA 灵动岛插件）

LA Tools 的独立灵动岛插件，基于 [Smart Island](https://github.com/agupta07505/SmartIsland)（GPL-3.0）修改。

## 简介

- 引擎：Smart Island 的悬浮胶囊、通知、媒体、来电、实时活动等能力；
- 设置页：LA 的 Miuix 页面（`com.latools.island.IslandSettingsActivity`）；
- 包名：`com.latools.island`；
- 没有桌面入口，只能由同一官方签名的 LA Tools 主应用启动；
- 安装包由 LA Tools 的云端组件分发获取，不内置在主应用中。

## 许可证与来源

本项目按 **GNU GPL v3** 发布，完整文本见 [LICENSE](LICENSE)。

上游项目：

- Smart Island：<https://github.com/agupta07505/SmartIsland>
- 作者：Animesh Gupta
- 许可证：GPL-3.0

本项目保留了上游源码中的版权与许可证声明；修改说明、第三方字体许可见
[NOTICE.md](NOTICE.md) 和 [THIRD_PARTY_FONTS.md](THIRD_PARTY_FONTS.md)。

## 构建

需要 JDK 17、Android SDK 37。

```bash
# 在项目根目录创建 keystore.properties（不要提交）
cat > keystore.properties <<'EOF'
storeFile=/path/to/release.jks
storePassword=...
keyAlias=...
keyPassword=...
EOF

./gradlew assembleRelease
```

产物：

```text
app/build/outputs/apk/release/app-release.apk
```

Release 开启 R8 混淆与资源压缩。签名证书、`keystore.properties`、
`local.properties` 和构建产物都不会提交到仓库。

## 安装

插件不提供桌面图标，需先安装 LA Tools，再从：

```text
首页 → 实用工具 → 系统工具 → 灵动岛
```

进入。首次使用需在设置页授予悬浮窗、无障碍服务和通知使用权。

## 版本

- versionName：`1.0`
- versionCode：`1`
