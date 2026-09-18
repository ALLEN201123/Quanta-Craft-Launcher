# QCL · Quanta Craft Launcher

> **目前手机启动器唯一支持下载所有考古社区已归档远古版本的。**

**Quanta Craft Launcher (QCL, 量子方块启动器)** is an Android launcher for Minecraft: Java Edition.
Since 1.1.1 the launch / render / input / Java-runtime pipeline is **self-built**, with
[FoldCraftLauncher (FCL)](https://github.com/FCL-Team/FoldCraftLauncher) as the reference;
versions 1.1.0 and earlier were based on [HMCL-PE](https://github.com/Tungs-HMCL/HMCL-PE).
Maintainer: **Rod123456** (bilibili UID 550905358).

**English summary:** QCL focuses on *ancient* Minecraft versions (pre-classic / classic / indev / infdev /
alpha / beta / RC) — **the only Android launcher that currently supports downloading every *archived* ancient
version** (181 archived builds; builds the community never archived are not downloadable). Each archived build
is installed with its dependencies and assets fetched automatically. It also supports modern
versions up to 26.x (Java 25). Pojav launch backend with native SDL3 windowing,
automatic 32/64-bit runtime detection, dynamic memory clamping, a launch-log overlay, a crash screen, and
multiplayer powered by Terracotta (China mainland only). Licensed under **GNU GPL-3.0** — see
[`LICENSE`](LICENSE) and [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).

> 中文说明在下方。The Chinese documentation follows.

---


> Android 版 Minecraft: Java Edition 启动器。**1.1.1 起启动 / 渲染 / 输入 / Java 运行时链路全部自研**，
> 以 [FCL（FoldCraftLauncher）](https://github.com/FCL-Team/FoldCraftLauncher) 为参照；
> **1.1.0 及更早版本基于 [HMCL-PE](https://github.com/Tungs-HMCL/HMCL-PE)**（Tungs），其代码核心已在 1.1.1 中移除。
> 作者：**Rod123456**（bilibili UID 550905358）—— 喜欢研究 Minecraft 1.0 以前的所有远古版本，视频也主要围绕这些老版本。
>
> **目前手机启动器唯一支持下载所有考古社区已归档远古版本的。**

---

## 这是什么

QCL 是一个安卓平台的 Minecraft Java 版启动器，界面按自己的「QCL 灰色半透明」风格重做，重点解决
**远古版本（pre-classic / classic / indev / infdev / alpha / beta / RC）能不能装、能不能启动**的问题，
同时支持到最新的 26.x（2026 年官方改历年命名）。

## 主要特性

### 启动链路
- **32/64 位运行时自动检测**：跟随设备与应用实际 ABI 自动选择（可在设置里手动强制）
- **内存安全**：按运行时位数给默认值（64 位 2GB / 32 位 1GB），并按**当前剩余内存**动态夹取，避免 VM 初始化失败把启动器一起带走
- **启动后端**：Pojav（1.1.1 起原生 SDL3 图形窗口，支持 26.3+）
- **长按启动键**可切换渲染器（Krypton Wrapper / Holy-GL4ES / Zink / VirGL / Freedreno / VGPU / MobileGlues；默认 Krypton，全版本通吃）
- **启动日志悬浮窗**：默认开启，实时显示 JVM 与游戏输出，进入游戏主界面自动关闭（可在游戏内悬浮窗开关）

### 远古版本
- **手机启动器里，目前唯一支持下载全部「已归档」远古版本的版本** —— 内置 **181 条历史归档**
  （pre-classic / classic / indev / infdev / alpha / beta / RC 几乎全覆盖，来自考古社区归档），点击即可安装
  - ⚠️ 说明：社区没有归档的版本（当年未公开、或归档站已失传）同样无法下载 —— 归档里没有的，这里也没有
- 安装时自动补齐 **jar / 版本 json / 依赖库 / 资源文件**，完成后提示安装成功
- 启动前自动检查并补齐缺失文件（json 损坏也能用模板重建）
- 远古版本（LWJGL2 时代）**补齐了 paulscode SoundSystem 音频链**，进游戏不再无声

### 下载
- 国内默认走 **BMCLAPI** 镜像；Mod / 资源包 / 整合包 / **光影** / 世界分页浏览
- 默认下载源为 **Modrinth**（资源包 / 整合包 / 光影 / 世界均支持切换 CurseForge）

### 账号与皮肤
- 离线账号默认**史蒂夫**（宽臂），支持导入自己的 PNG 皮肤
- 头像跟随皮肤变化（导入皮肤取脸部 / 史蒂夫 / 艾利克斯）
- 3D 人物待机与走路动画

### 多人联机（Terracotta / 陶瓷联机）
- 基于 [Terracotta](https://github.com/burningtnt/Terracotta)（BurningTNT）
- **游戏外**（启动器主界面 →「多人联机」）开启；**游戏内**（悬浮窗 →「联机模块」）创建/加入房间
- 房主获得邀请码自动复制，访客填入邀请码后获得服务器地址
- ⚠️ **仅限中国大陆地区使用**，境外使用本启动器不承担责任，且可能带来法律风险

### 其它
- 崩溃时弹出**崩溃界面**（左完整日志 / 右错误摘要 / 退出 / 返回启动器），不再直接闪退回主界面
- 版本隔离默认开启，玩家可自行关闭

## 构建

```bash
export JAVA_HOME=/path/to/jdk-17
./gradlew :QCL:assembleRelease
# 产物：QCL/build/outputs/apk/release/QCL-release.apk
```

- 环境：Gradle 7.3.3 / JDK 17 / NDK 27.3.13750724 / compileSdk 34 / minSdk 26
- 签名：请**自备** keystore（仓库不包含签名密钥），在 `QCL/build.gradle` 中配置

### 运行库（已内置）
`QCL/src/main/assets/app_runtime/java/` 已包含全部常用 Java 运行时，克隆即可完整构建：

- Java 8：`default`、`8-arm`、`8-arm64`、`8-x86`、`8-x86_64`
- Java 17：`JRE17`、`17-arm`、`17-arm64`、`17-x86`、`17-x86_64`
- Java 21：`JRE21`、`21-arm`、`21-arm64`、`21-x86`、`21-x86_64`
- Java 25：`25-arm`、`25-arm64`、`25-x86_64`

唯一例外是 **JRE25**：它的 `lib/modules` 单文件 121MB，超过 GitHub 100MB 硬上限、无法入库。
请从 Release 附件下载 `JRE25-runtime.zip`，解压到 `QCL/src/main/assets/app_runtime/java/JRE25/` 即可。

## 开源许可

- 本项目以 **GNU GPL-3.0** 授权，全文见 [`LICENSE`](LICENSE)。发布（包括 APK）时必须同时提供完整对应源码。
- 第三方组件与各自许可、署名见 `THIRD_PARTY_NOTICES.md`

### 特别鸣谢

1.1.1 起，QCL 的启动 / 渲染 / 输入 / Java 运行时等核心链路，**参照并借用了 FCL（FoldCraftLauncher）的开源代码与设计思路**自研实现，在此特别致谢。

| 项目 | 作者 | 用途 |
|---|---|---|
| HMCL-PE | Tungs（bilibili 18115101） | 本项目的历史来源（1.1.0 及更早版本的代码基础） |
| PojavLauncher | PojavLauncherTeam / Amethyst-Android | JVM 启动与 LWJGL 移植 |
| Terracotta | BurningTNT | 多人联机 |
| LWJGL / GL4ES / OpenAL / OpenJDK | 各自作者 | 图形、音频与运行时 |
| BMCLAPI | bangbang93 | 国内下载镜像 |
| **FoldCraftLauncher (FCL)** | **[FCL-Team](https://github.com/FCL-Team/FoldCraftLauncher)** | **1.1.1 起启动 / 渲染 / 输入 / 运行时方案的参照** |

> ⚠️ 关于 FCL：QCL 在启动 / 渲染 / 输入 / Java 运行时等核心链路的实现上，
> **参照并借用了 FCL 的开源代码与设计思路**。FCL 以 GPL-3.0 授权，其源码与许可详见其官方仓库。
> 在此对其作者与社区致以诚挚谢意。

## 免责声明
本项目不包含 Minecraft 游戏本体与资源，使用需自备正版账号（或自建离线账号）。
多人联机功能仅限中国大陆地区使用。
