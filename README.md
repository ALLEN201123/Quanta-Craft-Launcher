# QCL · Quanta Craft Launcher

**Quanta Craft Launcher (QCL, 量子方块启动器)** is an Android launcher for Minecraft: Java Edition,
rebuilt from [HMCL-PE](https://github.com/Tungs-HMCL/HMCL-PE) by Tungs.
Maintainer: **Rod123456** (bilibili UID 550905358).

**English summary:** QCL focuses on *ancient* Minecraft versions (pre-classic / classic / indev / infdev /
alpha / beta / RC) — 181 archived builds are listed and installed with dependencies and assets fetched
automatically. It also supports modern versions up to 26.x (Java 25). Two launch backends (Boat / Pojav),
automatic 32/64-bit runtime detection, dynamic memory clamping, a launch-log overlay, a crash screen, and
multiplayer powered by Terracotta (China mainland only). Licensed under **GNU GPL-3.0** — see
[`LICENSE`](LICENSE) and [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).

> 中文说明在下方。The Chinese documentation follows.

---


> 基于 [HMCL-PE](https://github.com/Tungs-HMCL/HMCL-PE)（Tungs）重构的 Android 版 Minecraft: Java Edition 启动器。
> 作者：**Rod123456**（bilibili UID 550905358）—— 喜欢研究 Minecraft 1.0 以前的所有远古版本，视频也主要围绕这些老版本。

---

## Features

- **Ancient versions, fully playable** — 181 archived builds (pre-classic / classic / indev / infdev / alpha / beta / RC) listed and installed automatically, including the version JSON, libraries and assets.
- **Automatic 32/64-bit runtime detection** with a manual override in Settings (Auto / 64-bit / 32-bit).
- **Safe memory allocation** — per-bitness defaults (2 GB on 64-bit, 1 GB on 32-bit), then dynamically clamped to the device's *currently available* memory so the VM can never fail to start.
- **Two launch backends** — Pojav (default) and Boat; long-press the launch button to switch renderers (GL4ES / VirGL / OpenGL ES 2.0-3.0 / Vulkan Zink).
- **Launch log overlay** — live JVM and game output, closes automatically when the game reaches its main menu.
- **Multiplayer (Terracotta)** — enabled from the launcher's main screen, used from the in-game floating menu; hosts share an invite code, guests join with it. **China mainland only.**
- **Crash screen** — full log on the left, error summary on the right, with Exit / Back-to-launcher buttons.
- **Downloads** — BMCLAPI mirror; Mods / Resource packs / Modpacks / **Shaders** / Worlds pages, Modrinth by default with CurseForge switchable.

## Version / Java mapping

| Minecraft | Java runtime bundled |
|---|---|
| ≤ 1.16.5 (incl. all ancient versions) | Java 8 (`default`, `8-arm`, `8-arm64`, `8-x86`, `8-x86_64`) |
| 1.17 – 1.20.4 | Java 17 (`JRE17`, `17-arm`, `17-arm64`, `17-x86`, `17-x86_64`) |
| 1.20.5 – 1.21.11 | Java 21 (`JRE21`, `21-*`) |
| 26.x | Java 25 (`25-*` in-repo; `JRE25` ships as a release attachment) |

## Build

```bash
export JAVA_HOME=/path/to/jdk-17
./gradlew :HMCLPE:assembleRelease
# output: HMCLPE/build/outputs/apk/release/HMCLPE-release.apk
```

Environment: Gradle 7.3.3 / JDK 17 / NDK 27.3.13750724 / compileSdk 34 / minSdk 26.
All Java runtimes are bundled in-repo **except `JRE25`** (its `lib/modules` exceeds GitHub's 100 MB file
limit) — download `JRE25-runtime.zip` from the v1.0.0 release and unzip it into
`HMCLPE/src/main/assets/app_runtime/java/JRE25/`.
Sign with your own keystore; the repository does not contain one.

## License

**GNU GPL-3.0** (inherited from HMCL-PE). Distributing this app requires providing the complete
corresponding source code. Third-party components and attributions: [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).

### Credits
HMCL-PE (Tungs) · PojavLauncher (PojavLauncherTeam) · Terracotta (BurningTNT) ·
LWJGL / GL4ES / OpenAL / OpenJDK · BMCLAPI (bangbang93)

## Disclaimer
No Minecraft game files are included. The multiplayer feature is restricted to mainland China.

---

## 简体中文说明

**Quanta Craft Launcher（量子方块启动器，QCL）** 是基于 HMCL-PE（作者 Tungs）重构的安卓版 Minecraft: Java Edition 启动器。
维护者：**Rod123456**（bilibili UID 550905358）。

- **远古版本全流程**：内置 181 条历史归档（pre-classic/classic/indev/infdev/alpha/beta/RC），点击安装，
  自动补齐 jar / 版本 json / 依赖库 / 资源；启动前自动检查修复缺失文件。
- **运行时位数**：自动检测（64 位设备自动用 64 位），可在 全局游戏设置 → 运行时位数 手动切换；默认内存按位数给（64 位 2G / 32 位 1G），并按当前剩余内存动态夹取。
- **双后端**：Pojav（默认）/ Boat；长按启动键切换渲染器。
- **启动日志悬浮窗**：默认开启，进游戏主界面自动关闭；Boat 后端同样支持（抓取 logcat jrelog）。
- **多人联机（Terracotta，陶瓷联机）**：主界面「多人联机」开启 → 游戏内悬浮窗「联机模块」创建/加入房间。**仅限中国大陆使用。**
- **崩溃界面**：左侧完整日志、右侧错误摘要，可退出或返回启动器。
- **下载**：BMCLAPI 镜像；模组/资源包/整合包/光影/世界，默认源 Modrinth，可切换 CurseForge。

**版本与 Java 对应**：≤1.16.5（含全部远古版本）→ Java 8；1.17–1.20.4 → Java 17；1.20.5–1.21.11 → Java 21；26.x → Java 25。

**许可**：GNU GPL-3.0（继承自 HMCL-PE），分发 APK 必须同时提供完整源码。第三方组件署名见 `THIRD_PARTY_NOTICES.md`。

**免责**：不含 Minecraft 本体与资源；多人联机仅限中国大陆地区使用。
