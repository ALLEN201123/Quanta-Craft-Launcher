<p align="center">
  <img src="icon.png" width="160" alt="QCL 图标"/>
</p>

# QCL · Quanta Craft Launcher

> **目前手机启动器唯一支持下载所有考古社区已归档远古版本的；也是唯一给「全远古版本」做好中文的启动器。**

**Quanta Craft Launcher (QCL, 量子方块启动器)** is an Android launcher for Minecraft: Java Edition.
Since 1.1.1 the launch / render / input / Java-runtime pipeline is **self-built**, with
[FoldCraftLauncher (FCL)](https://github.com/FCL-Team/FoldCraftLauncher) as the reference;
versions 1.1.0 and earlier were based on [HMCL-PE](https://github.com/Tungs-HMCL/HMCL-PE).
Maintainer: **Rod123456** (bilibili UID 550905358).

**English summary:** QCL focuses on *ancient* Minecraft versions (pre-classic / classic / indev / infdev /
alpha / beta / RC) — **the only Android launcher that currently supports downloading every *archived* ancient
version** (181 archived builds; builds the community never archived are not downloadable) — and the only one that ships Chinese patches for **all** ancient versions. Each archived build
is installed with its dependencies and assets fetched automatically. It also supports modern
versions up to 26.x (Java 25). Pojav launch backend with native SDL3 windowing,
automatic 32/64-bit runtime detection, dynamic memory clamping, a launch-log overlay, a crash screen, and
multiplayer powered by Terracotta (China mainland only). Licensed under **GNU GPL-3.0** — see
[`LICENSE`](LICENSE) and [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).

> 中文说明在下方。The Chinese documentation follows.

---


> Android 版 Minecraft: Java Edition 启动器。**1.1.1 起启动链路 / 输入桥 / Java 运行时链路自研**，
> 图形侧**窗口、Surface、EGL 上下文、帧节奏**也自研（**渲染器本身当前仍是第三方库**，见下文「渲染器」一节）；
> 以 [FCL（FoldCraftLauncher）](https://github.com/FCL-Team/FoldCraftLauncher) 为参照；
> **1.1.0 及更早版本基于 [HMCL-PE](https://github.com/Tungs-HMCL/HMCL-PE)**（Tungs），其代码核心已在 1.1.1 中移除。
> 作者：**Rod123456**（bilibili UID 550905358）—— 喜欢研究 Minecraft 1.0 以前的所有远古版本，视频也主要围绕这些老版本。
>
> **目前手机启动器唯一支持下载所有考古社区已归档远古版本的；也是唯一给「全远古版本」做好中文的启动器。**

---

## 这是什么

QCL 是一个安卓平台的 Minecraft Java 版启动器，界面按自己的「QCL 灰色半透明」风格重做，重点解决
**远古版本（pre-classic / classic / indev / infdev / alpha / beta / RC）能不能装、能不能启动**的问题，
同时支持到最新的 26.x（2026 年官方改历年命名）。

## 主要特性

### 启动链路
- **32/64 位运行时自动检测**：跟随设备与应用实际 ABI 自动选择（可在设置里手动强制）
- **内存安全**：按运行时位数给默认值（64 位 2GB / 32 位 1GB），并按**当前剩余内存**动态夹取，避免 VM 初始化失败把启动器一起带走
- **启动后端**：Pojav（1.1.1 起原生 SDL3 图形窗口，支持 26.3+）。
  ⚠️ **Boat 后端已移除** —— 1.1.x 起源码、资源、manifest 声明都已删除，启动器里也没有切换入口了。
- **长按启动键**可切换渲染器（Krypton Wrapper / Holy-GL4ES / Zink / VirGL / Freedreno / VGPU / MobileGlues；默认 Krypton，全版本通吃）
- **启动日志悬浮窗**：默认开启，实时显示 JVM 与游戏输出，进入游戏主界面自动关闭（可在游戏内悬浮窗开关）

### 渲染器
QCL 自己写的是**窗口 / Surface / EGL 上下文 / 输入桥 / 帧节奏**这一层；
真正把 OpenGL 调用翻译成设备 GLES 的**渲染器本身，目前全部是第三方库**（长按启动键切换）：

| 名称 | 库 | 说明 |
|---|---|---|
| **Krypton Wrapper**（默认） | `libng_gl4es.so` | OpenGL 3.1+，全版本通吃；官方支持到 26.3-snapshot-3 |
| **Holy-GL4ES** | `libgl4es_114.so` | OpenGL 2.1，老版本更稳 |
| **Zink** | `libglxshim.so` | OpenGL 4.6（Mesa zink on Vulkan），支持到 26.3-snapshot-3 |
| **VirGL** | `libOSMesa_81.so` | 软渲染（VirGLRenderer） |
| **Freedreno** | `libOSMesa_8.so` | OpenGL 4.6，仅高通 adreno616-a660 |
| **VGPU** | `libvgpu.so` | OpenGL 2.1+ |
| **MobileGlues**（外部插件） | `libmobileglues.so` | 需另装官方插件 APK；26.3 正式版待其官方支持 |

**自研渲染器（进行中）**
- QCL 计划**自研一个自己的渲染器**，并**明确不以 MobileGlues 为模仿对象、不复制其实现思路**。
- 起因（如实记录）：我们此前向 `MobileGL-Dev/MobileGlues` 提交过 PR（#59，26.3 崩溃的空指针保护），
  被维护者驳回并要求「修根因、别再提补丁式 PR」，随后该问题由官方自行处理。
  由此我们决定**不再向该仓库提交任何东西**，兼容性只做到「**能识别、能加载它**」，
  一切以其自身实现为参照，QCL 走自己的路。
- **进度：尚未发布，因此渲染器列表里暂时没有这一项** —— 不做装上却不能用的假选项。

### 远古版本
- **手机启动器里，目前唯一支持下载全部「已归档」远古版本的版本** —— 内置 **181 条历史归档**
  （pre-classic / classic / indev / infdev / alpha / beta / RC 几乎全覆盖，来自考古社区归档），点击即可安装
  - ⚠️ 说明：社区没有归档的版本（当年未公开、或归档站已失传）同样无法下载 —— 归档里没有的，这里也没有
- 安装时自动补齐 **jar / 版本 json / 依赖库 / 资源文件**，完成后提示安装成功
- 启动前自动检查并补齐缺失文件（json 损坏也能用模板重建）
- 远古版本（LWJGL2 时代）**补齐了 paulscode SoundSystem 音频链**，进游戏不再无声
- **全远古版本中文**：**classic / indev / infdev / alpha / beta** 全系列都做了中文补丁（界面 + 中文字体），
  是目前唯一做到这一点的安卓启动器（仅 6 个连 ESC 菜单都没有的最早原型版未翻译）
- **安卓设备上唯一支持 ModLoader 和 Babric 的启动器**（1.2.3）：
  正式版老版本（1.2.5 ~ 1.6.2）和部分远古版本（Beta / Alpha）可以下载 ModLoader，
  b1.7.3 可以用 Babric —— 别的安卓启动器没有这两样
- **ModLoader（Risugami）**：安装页可直接勾选随本体一起装（1.2.3），
  **版本设置 → 自动安装里也能给已装好的版本就地装/卸**（1.2.5）
  覆盖 1.2.5 ~ 1.6.2 及一串 Beta / Alpha（Alpha 官方只有 .rar，已转 zip 放镜像）；
  b1.7.3 用的是社区的 ModloaderFix 版（原版在现代 Java 下会卡初始化）
- **Babric**（b1.7.3 专用的 Fabric 分支）：一键安装（1.2.3），版本设置 → 自动安装 同样可就地装/卸（1.2.5），仅支持 b1.7.3
- **音效音乐不会因为装了加载器而丢**（1.2.5）：ModLoader / Babric 会自动打开「不检查游戏文件」，
  以前这个开关连音频下载一起跳过 → 远古版本没声音；现在音频照常补，且补不下也不拦启动
- **版本图标**：装了哪个加载器就显示哪个的 logo（1.2.5 起三个页面完全一致，FCL 同款）；
  版本设置里的铅笔可打开图标选择器，MultiMC 风格内置图标 + 自定义图片并存

### 下载
- 国内默认走 **BMCLAPI** 镜像；Mod / 资源包 / 整合包 / **光影** / 世界分页浏览
- **Modrinth / CurseForge 走国内 MCIM 镜像**（接口、图标、模组文件；镜像不通自动回退官方，1.2.7）
- 列表图标有内存+磁盘缓存，拉不到就放弃而不是一直转圈（1.2.7）
- 默认下载源为 **Modrinth**（资源包 / 整合包 / 光影 / 世界均支持切换 CurseForge）
- **支持 Prism / MultiMC 整合包导入**（1.2.3）：导入时自动补下游戏本体，jarmods / patch 全套处理
- **改 class 的老式模组自动注入本体 jar**（1.2.3）：下载后自动识别，有元数据的加载器模组照常放
  mods/；撞了别的模组的 class 会弹窗问继续还是取消；版本设置里有 **Class 查看器**，可查看/删除
- 模组卡片会按你当前版本装的加载器标注「不支持你当前的版本」（1.2.3）
- 排序默认按**下载量**，版本筛选默认**你正在玩的版本**且筛选真的生效（1.2.5 修），搜索旁有刷新按钮（1.2.3）
- 模组详情页第一组就是**适配你当前版本**的那一组，点一下直接进下载框（1.2.5）
- 版本列表覆盖 26.3 → a1.2.0_02（114 个正式版，1.2.3 重建）

### 账号
- **微软账号登录**：可在启动器内添加、切换多个微软账号
- 离线账号：默认**史蒂夫**（宽臂），支持导入自己的 PNG 皮肤
- 头像跟随皮肤变化（导入皮肤取脸部 / 史蒂夫 / 艾利克斯）
- 3D 人物待机与走路动画

### 皮肤与披风（不用跳转官网）
- **微软账号可直接在启动器里换皮**：3D 预览、选本地图片上传、一键重置
- 皮肤模型可在经典（Steve）/ 苗条（Alex）之间切换，也会自动识别当前皮肤属于哪种
- 披风列表：查看 / 激活 / 隐藏

### 版本更新
- 启动时自动检查更新，弹窗显示更新说明，可在启动器内直接下载安装
- **更新包按设备架构自动挑对应的那个**（照 FCL 的做法），不会下错架构
- 也可以选择跳 GitHub 或网盘手动下载；「忽略此更新」会记住该版本号，之后不再提示
- ⚠️ 更新检查读的是仓库 **`main` 分支**上的 `launcher_version.json`，
  不是源码里那份同名文件（那份只是留个格式参考）
- 🇨🇳 国内额外有一个 **Gitee 镜像**（`gitee.com/allne201123/qcl-repo`，照 FCL 的做法）。
  GitHub raw 和 jsDelivr 在国内经常连不上，这个镜像就是给国内用户用的。
  ⚠️ 所以发版时**两处都要更新**（GitHub main 分支 + Gitee 仓库），别只改一处

### 多人联机（Terracotta / 陶瓷联机）
- 基于 [Terracotta](https://github.com/burningtnt/Terracotta)（BurningTNT）
- **游戏外**（启动器主界面 →「多人联机」）开启；**游戏内**（悬浮窗 →「联机模块」）创建/加入房间
- 房主获得邀请码自动复制，访客填入邀请码后获得服务器地址
- ⚠️ **仅限中国大陆地区使用**，境外使用本启动器不承担责任，且可能带来法律风险

### 其它
- 崩溃时弹出**崩溃界面**（左完整日志 / 右错误摘要 / 退出 / 返回启动器），不再直接闪退回主界面
- 版本隔离默认开启，玩家可自行关闭
- **持续性能模式**：manifest 里声明了 `appCategory="game"` + `isGame="true"`（照 FCL），
  让系统游戏助手（vivo 游戏魔盒 / 华为游戏助手等）把 QCL 认成游戏，可呼出系统侧边栏；
  游戏菜单里也有开关，默认开，进游戏会调 `setSustainedPerformanceMode`

## 版本历史与更新说明

**每个版本改了什么，都在 GitHub Releases 里，README 不逐版记录：**

- 全部版本：https://github.com/ALLEN201123/Quanta-Craft-Launcher/releases
- 当前版本：**1.4.3**（versionCode 343）

> 建议先按下面这张表判断自己关心的问题是在哪一版修的，直接查对应 Release 的说明：
>
> | 关注点 | 去看 |
> |---|---|
> | 整合包导入 / ModLoader / Babric / 改 class 模组注入与冲突 / Class 查看器 | 1.2.3 |
> | 图标选择器 / 加载器图标（装哪个显示哪个）/ 下载页五页功能统一 | 1.2.4 |
| 模组页整页误标「不支持你当前的版本」/ 列表图标和版本设置不一致 / Babric 留下 0B 文件 | 1.2.5 |
| 手机收不到更新提示（某个镜像源落后一版就把更新挡住了） | 1.2.6 |
| 下载页图标一直白/加载不出来、Modrinth 与 CurseForge 国内访问慢 | 1.2.7 |
| 1.2.6 也收不到更新提示（各 CDN 缓存刷新不同步）/ 更新包在国内下不动 | 1.2.8 |
| 动态背景切后台回来不动 / 下载页详情页加载慢 / 图标加载卡顿 | 1.2.9 |
| **视角偏移彻底修复（关菜单/关背包后视角猛偏、进游戏第一次滑动就大偏、鼠标状态反馈延迟 250 毫秒）** / **实验室：种子查询器补 26.x、数据包制作按目标版本自动取 pack_format 并修 1.21+ 目录名与新写法、物品配方查询改读游戏本体全量配方（121→1175 条）** / **版本列表按正式版·远古版分组排序** / NeoForge 安装页补 26.x | 1.4.3 |
| **大厅 / 实验室（6 项工具）/ NeoForge 在线安装 / 自研皮肤库（搜索·收藏·模型识别·3D 预览）/ 模组翻译 / 整合包导出** / **老格式皮肤头顶黑块修复** / Alex 细手臂误判修复 / 皮肤库面板配色统一 | 1.4.1 |
| **indev/部分原型版本（in-/pc-）无法进入游戏修复** / **classic 全系与部分 indev 画面铺满全屏（applet 容器尺寸动态化）** / 版本设置页自动注入参数提示 | 1.4.0 |
| **独立设置版本切换渲染器不生效** / 设置文件缺失启动闪退防护 / **Alpha F3 调试屏幕汉化** / Alpha 界面补漏（世界类型、空世界槽位「空」等 70+ 条） | 1.3.9 |
| **infdev 全系列 27 版本 / indev 全系列 23 版本 / Classic 26 个有菜单版本 全中文**（无菜单的原型版 6 个不翻译）/ 修复 Sodium 崩溃与小地图闪烁 / 修复模组前置重复下载 / 修复 Classic 无法启动（launchwrapper 1.6） | 1.3.8 |
| **Alpha 全系列（42 个版本）中文补丁** / Beta 补齐 b1.2_02-dev 与 b1.4-1507 / 切页面 3D 人物残留修复 / 新增「主界面显示账号人物」开关 / 默认背景改经典 | 1.3.7 |
| Beta 全版本（b1.0~b1.9）中文补齐 / 4 个特殊版本（试玩版、b1.8-pre1、b1.9-pre）中文空白乱码修复 / 试玩版主菜单汉化 | 1.3.6 |
| 远古版本选项按钮「音乐: 关」等值部分乱码/重叠 | 1.3.5 |
| 远古版本装了加载器后没声音（「不检查游戏文件」曾把音频一起跳过） | 1.2.5 |
| 下载页「游戏版本」默认值不对、筛选没生效 / 模组详情页「加载版本列表失败」 | 1.2.5 |
> | 收不到更新提示 / 想参与开发 | 1.2.2 |
> | clone 下来构建不了 / 令牌被写进日志 | 1.2.1 |
> | 应用内「更新」按钮下错包 | 1.2.0 |
> | infdev 划屏转视角转不动 | 1.1.9 |
> | infdev 进不去 / 远古版本启动 | 1.1.7 / 1.1.8 |
> | 远古版本（JRE8）报 UnsupportedClassVersionError | 1.1.6 |
> | 微软账号登录、本地换皮、更新弹窗 | 1.1.5 |
> | 系统游戏助手不认 QCL | 1.1.4 |
> | MobileGlues（MG）装了没反应 | 1.1.3 |
> | 高版本进世界崩溃 / 远古版本无声 | 1.1.2 |
> | 远古版本无声 | 1.1.1 |
> | 老版本（Java 8）启动即崩、高版本触屏点不动 | 1.1.0 |


### 1.3.5（versionCode 335 · 2026-09-28）

**修复远古版本选项界面「标签: 值」按钮显示错乱** ✓

**症状**：b1.6 ~ 1.0、b1.9 全系的选项界面里，凡「标签 + 冒号 + 值」格式的按钮
（音乐 / 音效 / 鼠标反转 / 灵敏度 / 视场角 / 难度），冒号、空格、数字、百分号等
**西文字符被画成乱字形**，并与标签重叠；纯中文按钮与纯英文界面一直正常。

**根因**：中文补丁的字体渲染器把「中文字形」与「西文字形」放在两张纹理上。绘制时西文字符
先攒进队列、遇到中文字符才真正送显；而中文字符的显示列表内部会绑定自己的点阵纹理。
于是排在中文后面的西文字符（比如「音乐: 关」的冒号）在**错误的纹理**上取字形 → 乱码。

**修复**：每次把西文队列送显前重新绑定西文字体纹理。涉及全部 9 个补丁字体类
（b1.6 系 `se`、b1.7.3 系 `sj`、b1.8 系 `kh`、1.0 `abe`、b1.9 全系 `lf/ls/mc/mb/mf`）。
期间曾因字节码插入顺序错误导致 b1.7.3 启动 VerifyError，已修正并实机复测。

**附带**：补丁标记（`.cnpack_lang`）加上启动器 versionCode —— **启动器升级后自动为
已安装的远古版本重新注入最新补丁**，不再需要删除版本重装。

**已验证**：b1.9-pre5 选项界面（音乐: 100% / 音效: 100% / 鼠标反转: 关 / 灵敏度: 100% /
视场角: 极小 / 难度: 普通）与 b1.7.3 选项界面全部正常；1.20.6 高版本回归无影响。


### 1.3.4（versionCode 334 · 2026-09-28）

**远古版本界面文本「按版本适配」** ✓

各版本游戏自己用的英文就不一样，之前所有版本共用一份中文表，导致低版本会显示新版本的说法。逐个版本核对后按版本适配：

| 版本 | 草 | 石台阶 | 模组 / 材质包 |
|---|---|---|---|
| b1.6 系、b1.7.3 系（含改名版）| 草 | 石台阶 | 模组与材质包 |
| b1.8 / b1.8.1 / b1.9-pre2~pre5 | 草方块 | 圆石台阶 | 模组与材质包 |
| 1.0 / b1.9-pre6 | 草方块 | 圆石台阶 | 材质包 |

做法是**一份主表 + 只列差异键的小覆盖**（`lang_over/over_b16.lang`、`over_b18.lang`），注入时按行合并 —— 不用维护多份完整语言文件。
依据：逐个版本对比原版语言文件，键集合是递增的、旧键都保留，真正含义变过的只有上面这几个。

**补全全部未翻译文本（共 49 处）** ✓

- 游戏内：粒子效果（粒子效果 / 全部 / 减少 / 最少）、高草三态（灌木 / 草 / 蕨）、龙蛋、床提示
- 游戏内成就：1.0 全部成就（钻石！/ 我们需要再深入些 / 见鬼去吧 / 与火共舞 / 本地酿造厂 / 结束了？/ 结束了。/ 附魔师 / 赶尽杀绝 / 图书管理员）+「狙击手的对决」
- 启动器：关于页说明、分类名（诅咒 / 模组加载器 / 体验优化）、鼠标键位（左键 / 右键 / 中键）、检查更新失败等提示

**修复一个注入缺陷** ✓

`1.0.0-rc1` / `1.0.0-rc2-1649` / `1.0.0-rc2-1656` 这些**候选版**曾被误判为正式 1.0，往它们的游戏文件里写入了本不该有的类。现改为精确识别版本号；`b1.8-pre2` 这类预发布版也不再被误处理。

**外观**：新用户默认背景改为「经典图片」，动态轮播改由「网络」选项承载。

### 1.3.3（versionCode 333 · 2026-09-27）

**中文界面新增 b1.9 全系列** ✓

本版把 **b1.9 全系 5 个版本**的中文补丁做通了（此前这些版本完全没有中文）。
这 5 个版本的 FontRenderer 混淆名**逐版不同**，必须逐版对应：

| 版本 | FontRenderer 混淆名 |
|---|---|
| b1.9-pre2 | `lf` |
| b1.9-pre3-1402 | `ls` |
| b1.9-pre4-1415 | `mc` |
| b1.9-pre5 | `mb` |
| b1.9-pre6 | `mf` |

**实机验证** ✓：pre2 / pre5 / pre6 主菜单五个按钮全部正常显示中文（单人游戏 / 多人游戏 / 模组与材质包 / 选项 / 退出游戏），字号与英文协调、居中正确。

**本版修掉的问题** ✓

1. **启动崩溃 `NoClassDefFoundError: co`** —— 注入时 `isPatchedEntry` 漏排除 b1.9/b1.6 分支，把游戏自己的原生类当补丁类删掉了。
2. **中文一个字都不显示** —— 高度算法 `fh = (gw >>> 4) - 1` 在中文（`glyph_sizes` 值为 `0x0F`）时算出 **-1**，字形退化成 1 像素横条。
3. **每个字右边多一条竖线** —— 绘制宽度用了整整一格（16px），采到了隔壁格的像素（原版用 `7.99` 就是防这个）。
4. **字大得离谱、挤成一团** —— 原版字体纹理是 128×128、每格 8×8，中文点阵是 256×256、每格 16×16，**必须缩一半**画成 8×8。
5. **改小后只剩偏旁** —— UV 跟着屏幕尺寸一起减半了。**UV 跨度要恒取整格 16，只有屏幕跨度才是 7.99**。
6. **「选项」按钮旁出现乱字符** —— `menu.options=选项...` 的 3 个 ASCII 句点落在原版纹理的空白格上，采到错误像素。已改为中文省略号（走 CJK 字形页）。
7. **FOV 选项显示成 `options.fov` 键名** —— 语言文件缺 `options.fov` / `options.fov.min`，已补（视场角 / 极小）。

**1.3.5 已修复上面遗留的「值部分重叠」**：根因是中英混排时西文字符在中文点阵纹理上取字形（详见下方 1.3.5 段）。
- b1.9-pre3-1402 / pre4-1415 未做实机启动验证（pre2 / pre5 / pre6 已验证，三者共用同一套补丁逻辑）。

### 1.3.2（versionCode 332 · 2026-09-27）

**中文界面新增 b1.6 全系列** ✓

| 系列 | 版本 | FontRenderer 混淆名 |
|---|---|---|
| **b1.6 系** | b1.6 / b1.6.1 / b1.6.2 / b1.6.3 / b1.6.4 / b1.6.5 / b1.6.6 | `se` |

这 **7 个版本的 `se.class` 字节完全相同**（SHA1 `d5ff012e31b3`）→ **一份补丁通用** ✓

**实机验证** ✓：b1.6.6 主菜单全中文（`单人游戏` / `多人游戏` / `模组与材质包` / `选项...` / `退出游戏`）

**目前支持中文的远古版本**

| 系列 | 版本 | 混淆名 |
|---|---|---|
| b1.7.3 系 | b1.7.3 / b1.7.3ml / b1.7.3bc … | `sj` + `co` + QclLangScreen |
| b1.8 系 | b1.8 / b1.8.1 | `kh` |
| **b1.6 系** | **b1.6 / b1.6.1 ~ b1.6.6** | **`se`（本版新增）** |
| 1.0 正式版 | 1.0 / 1.0.0 | `abe` |
| **b1.9 全系（1.3.3 新增）** | **b1.9-pre2 / pre3-1402 / pre4-1415 / pre5 / pre6** | **`lf` / `ls` / `mc` / `mb` / `mf`（逐版不同）** |

**本版摸清的关键机制**（供后续扩版本参考）✓
- **`font.txt` 不是"字宽表"，它就是「字符表本身」**：
  b1.7.3 的 `ChatAllowedCharacters`、b1.9/1.0 的 `SharedConstants` **都是启动时读 `/font.txt` 拼接出字符表**。
  所以注入我们的 `font.txt`（12241 字节、含 4064 个字符含中文）就等于**换掉了整个字符表** ——
  **绝不能为了"保留原版英文"而不注入它**，那样字符表只剩 144 个 ASCII，中文查不到 → 界面全空白。
- **`isPatchedEntry` 的跳过名单必须带版本**：混淆名每版重新分配（同名不同物）——
  例：`sj` 在 b1.7.3 是 FontRenderer，在 b1.9-pre6 里却是别的原生类，误跳过会 `NoClassDefFoundError`。

**说明**
- 首次启动会在初始化完成后自动注入中文（**不用重装游戏**）✓
- 急着玩的话：在启动器里切换一次语言，或重启一次启动器让它补上 ✓

### 1.3.1（versionCode 331 · 2026-09-24）

**中文界面扩到更多远古版本** ✓
- 新增 **b1.8 / b1.8.1**（两版 FontRenderer 字节完全相同，一份补丁通用）
- 新增 **1.0 正式版**（Mojang 的 id 是 `1.0`，RetroMCP 里叫 `1.0.0`）
- 加上原有 **b1.7.3 系**，目前共 4 个版本是完整中文界面

**实现方式（本版改用正确路线）** ✓
- 用 **RetroMCP 反编译「该版本自己的源码」**再打补丁，不是拿 b1.7.3 的补丁跨版本重映射 ——
  后者会因目标版映射表缺 b1.7.3 的 MCP 方法名而残留未映射符号，运行时直接 `NoSuchMethodError` 冻结
- 每个补丁都过双重校验：① `func_` 残留必须为 0 ② 与原版逐方法对比必须**全覆盖**

**修复的严重问题** ✓
- **混淆名「同名不同物」导致原生类被误删**：同一个文件名在不同版本是完全不同的类
  （`sj` 在 b1.7.3 是 FontRenderer，在 b1.8 是 WorldGenCactus；`co` 在 b1.7.3 是 GuiOptions，在 b1.8 是 BlockStone）。
  旧逻辑按文件名无条件跳过，会把目标版本自己的原生类删掉 → `NoClassDefFoundError` → 游戏起不来。现在跳过逻辑带版本判断。
- **语言文件不再整目录跳过**（保住 1.1 的 `languages.txt`，否则 1.1 语言管理器会 NPE 冻结）
- **「不检查游戏文件」只对支持中文的版本自动开**，不再动其他版本的设置
- **legacy（1.6 以前）资源索引里的占位条目（`READ_ME_I_AM_VERY_IMPORTANT` 等）在任何源都是 404**，
  以前会让整批资源下载判失败；现在跳过不判失败（HMCL 同款做法）

### 1.3.0（versionCode 330 · 2026-09-23）

**远古版本（b1.7.3 系）自带中文** ✓
- 字模用的是 **Mojang 官方中文点阵**（1.5.2 的 `glyph_XX.png` + `glyph_sizes.bin` 原样搬）
- 渲染照官方做法：**16×16 字形只画 8px 高** → 中英等高、不溢出、不糊
- 界面文本全量翻译（341 + 71 条）
- 语言只有两项：**简体中文 / English** —— 可在「启动器设置 → 远古版本汉化语言」改，也能在游戏里的**选项 → 语言…** 改（照 1.5.2 的入口做的，选中后立刻生效）

**自动注入（不用重装游戏）** ✓
- 下载页 / 归档列表安装完即打；**早就装好的版本**，启动器开起来会自己补 ✓
- 版本识别**优先读版本 json 里的真 id**（玩家把目录改名也认得出 ✓）
- 远古版本装完自动打开「不检查游戏文件」→ 不会再覆盖你改过的本体、不会再自动重下音乐音效 ✓

**捆绑的 bug 修复**：点取消还在后台下载 / Finalizer 关 ZipFile 报 EIO 导致崩溃 / 下载页切版本后仍提示不支持 / 模组详情页图标一直白 ✓

## 构建

```bash
export JAVA_HOME=/path/to/jdk-17
./gradlew :QCL:assembleRelease
# 产物：QCL/build/outputs/apk/release/QCL-release.apk
```

- 环境：Gradle 7.3.3 / JDK 17 / NDK 27.3.13750724 / compileSdk 34 / minSdk 26
- 签名：请**自备** keystore（仓库不包含签名密钥），在 `QCL/build.gradle` 中配置

### 运行库（已内置）
`QCL/src/main/assets/app_runtime/java/` 已包含全部常用 Java 运行时，**克隆仓库即可完整构建，不需要再从任何 Release 附件补包**。

每个运行时目录里都是与 FCL 同款的架构分卷（`bin-<架构>.tar.xz` + `universal.tar.xz` + `version`）：

| 仓库目录 | Java 运行时 | 架构分卷 | 目录体积 | 设备端解压到 |
|---|---|---|---|---|
| `jre8/` | Java 8 | arm / arm64 / x86 / x86_64 + universal | 27.3 MB | `default` |
| `jre17/` | Java 17 | arm / arm64 / x86 / x86_64 + universal | 38.3 MB | `JRE17` |
| `jre21/` | Java 21 | arm / arm64 / x86 / x86_64 + universal | 46.3 MB | `JRE21` |
| `jre25/` | Java 25 | arm / arm64 / x86_64 + universal（**无 x86**） | 49.0 MB | `JRE25` |

- 四个运行时的内容与 FCL 的 `FCL/src/main/jreAssets/app_runtime/java/` **逐字节一致**（23 个文件中 22 个 md5 完全相同；唯一差异是 `jre8/version` 的版本计数 `13` → `14`，QCL 用它触发设备端重新解压）。
- **单文件最大 32.8 MB**（`jre25/universal.tar.xz`），远低于 GitHub 单文件 100 MB 硬上限，所以 **23 个文件全部已入库**。
- 早先那个 121 MB 的 `lib/modules` 单文件已不存在 —— 它现在被压缩进 `universal.tar.xz`（32.8 MB）。**`JRE25-runtime.zip` 附件已作废，不需要了。**
- Java 25 没有 x86 分卷（上游未提供）；在 x86 设备上安装器会直接跳过 Java 25。

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
