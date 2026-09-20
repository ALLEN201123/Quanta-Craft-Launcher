# QCL 更新说明

> ⚠️ **这份文件停留在 1.0.0，已不再逐版维护。**
>
> 每个版本实际改了什么，一律以 **GitHub Releases** 为准：
> https://github.com/ALLEN201123/Quanta-Craft-Launcher/releases
>
> 下面是首个公开版本（1.0.0）的说明，仅作历史留存。

---

# QCL 1.0.0（Quanta Craft Launcher / 量子方块启动器）

首个公开版本。基于 HMCL-PE（Tungs）重构，重点解决**远古版本能不能装、能不能启动**。

## 本次亮点
- **远古版本全打通**：内置 181 条历史归档（pre-classic / classic / indev / infdev / alpha / beta / RC），
  点击即可安装；安装时自动补齐 jar / 版本 json / 依赖库 / 资源，完成后提示安装成功
- **启动链路加固**：运行时 32/64 位自动检测（可手动切换）、内存按位数给默认值并按当前剩余内存动态夹取，
  不再出现"VM 初始化失败把启动器一起带走"的白屏
- **启动日志悬浮窗**：默认开启，实时显示 JVM 与游戏日志，进游戏主界面自动关闭
- **多人联机（Terracotta）**：游戏外开启、游戏内使用；房主拿邀请码、访客填邀请码即加入
- **崩溃界面**：崩溃时弹出（左侧完整日志 / 右侧错误摘要 / 退出 / 返回启动器），不再直接闪退回主界面
- **长按启动键**切换渲染器；QCL 灰色半透明界面；命令方块图标

## 安装
- 直装 APK 即可（applicationId `com.qcl.launcher`，可与 HMCL-PE 共存）
- Android 8.0+（minSdk 26）

## 使用提醒
- 使用 **Pojav 后端**。⚠️ **Boat 后端已在 1.1.x 里彻底移除**（源码、资源、manifest 声明都删了），
  启动器里没有切换入口，也不再支持切换 —— 早期版本里写过「Boat 后端保留（可切换）」，那句话已经过时。
- 内存：64 位运行时默认 2GB、32 位默认 1GB；32 位地址空间实测上限约 1GB，填更大只会被自动夹取
- 多人联机**仅限中国大陆地区**使用

## 开源
- 本项目以 **GNU GPL-3.0** 授权（继承 HMCL-PE），分发本 APK 时必须同时提供完整源码
- 第三方组件许可与署名见 `THIRD_PARTY_NOTICES.md`

## 特别鸣谢

本项目的 Java 运行时方案、authlib-injector 用法等部分实现，**参考并借用了 FCL（FoldCraftLauncher）的开源代码与思路**，在此特别致谢。

| 项目 | 作者 | 用途 |
|---|---|---|
| HMCL-PE | Tungs（bilibili 18115101） | 本项目的前身 |
| PojavLauncher | PojavLauncherTeam / Amethyst-Android | JVM 启动与 LWJGL 移植 |
| Terracotta | BurningTNT | 多人联机 |
| LWJGL / GL4ES / OpenAL / OpenJDK | 各自作者 | 图形、音频与运行时 |
| BMCLAPI | bangbang93 | 国内下载镜像 |
| FoldCraftLauncher (FCL) | FCL-Team | 部分实现参考/借用（Java 运行时方案、authlib-injector 用法等） |
