# QCL（Quanta Craft Launcher / 量子方块启动器）第三方组件与开源合规说明

## 本项目自身的许可
QCL 是基于 **HMCL-PE**（Hello Minecraft! Launcher Pocket Edition）重构的衍生作品。
HMCL-PE 以 **GNU GPL-3.0** 授权；但 QCL 链接了 **AGPL-3.0** 的 Terracotta，因此 **QCL 整体以 GNU AGPL-3.0 授权**，
完整协议全文见工程根目录的 `LICENSE`。

### 发布者的义务（AGPL-3.0）
1. **分发（发布 APK / 任何形式分发）时必须同时提供完整对应的源码**（可放到公开仓库并在下载页给出链接）。
2. 保留所有版权声明与许可声明（本文件与 `LICENSE` 必须随源码一起提供）。
3. 若修改了上游代码，需以 AGPL-3.0 兼容许可开放修改后的源码。
4. **网络条款（AGPL-3.0 §13）**：若把本软件作为网络服务提供给他人，须向网络用户提供源码。

## 第三方组件清单
| 组件 | 作者 / 来源 | 许可 | 用途 |
|---|---|---|---|
| HMCL-PE | Tungs（B 站 18115101） | GPL-3.0 | 本项目的前身与代码基础 |
| PojavLauncher | PojavLauncherTeam（已归档，后继 Amethyst-Android） | LGPL-3.0 | JVM 启动、LWJGL 移植、caciocavallo 等 |
| Boat（Cosine / 摆渡人后端） | Cosine 相关作者 | 见其仓库 | 另一套启动后端与 lwjgl2 移植 |
| Terracotta（陶瓷联机） | **BurningTNT** | **AGPL-3.0** | 多人联机（NAT 穿透 / 组网 / 房间邀请码）；源码见 `third_party_sources/Terracotta/` |
| LWJGL 2 / 3 | LWJGL 团队 | BSD-3-Clause | OpenGL / 输入 / 音频绑定 |
| GL4ES | ptitSeb 等 | MIT | OpenGL 2.x → OpenGL ES 1.1/2.0 转换层 |
| OpenAL Soft | kcat 等 | LGPL-2.0 | 音频 |
| OpenJDK / 移动端 JRE 构建 | OpenJDK、FCL 项目 | GPL-2.0 with Classpath Exception | Java 运行时 |
| **FoldCraftLauncher (FCL)** | **FCL-Team** | **GPL-3.0** | **部分实现参考/借用**：Java 运行时的选择与安装、authlib-injector 的引入与校验等（详见下方说明） |
| BMCLAPI | bangbang93 | 见其服务说明 | 国内下载镜像 |
| mcmod.cn（MC 百科） | mcmod.cn | 见其站点说明 | 百科链接 |
| Minecraft（游戏资源/贴图，仅作图标与界面参考） | Mojang / Microsoft | 专有资产，遵循 Minecraft EULA | 本项目不分发游戏本体 |

## ⚠️ 关于 FCL（FoldCraftLauncher）的代码/思路借用说明
QCL 在实现下列功能时，**参考并借用了 FCL（FoldCraftLauncher，FCL-Team）的开源代码与设计思路**：

1. **Java 运行时的选择与安装**：按版本所需 Java 大版本 + 设备实际架构选择并安装对应运行时。
2. **authlib-injector 的引入与校验**：注入前读取 jar 的 Manifest
   （校验 `Implementation-Title` / `Build-Number` / `Premain-Class`），校验不通过则不注入 `-javaagent`；
   以及把内置 jar 升级到与 Java 21+ 兼容的新版本。
3. **部分接口/流程设计**参考了 FCL 的对应实现。

FCL 以 **GPL-3.0** 授权。若你需要 FCL 的源码或其许可全文，请见其官方仓库
<https://github.com/FCL-Team/FoldCraftLauncher>。
在此对 FCL 的作者与社区致以诚挚谢意。

## 署名位置
启动器内「设置 → 关于」页已列出：BMCLAPI、MC 百科、原版项目（HMCL-PE / Tungs）、
本作作者（Rod123456）、**Terracotta / BurningTNT** 与 **FoldCraftLauncher（FCL-Team）**，
以及 AGPL-3.0 说明。

## 多人联机的使用限制（合规与免责）
多人联机功能仅限 **中国大陆地区** 使用；在境外使用本功能，本启动器不承担任何责任，
并可能带来法律风险，由使用者自行评估与承担。开启前会在启动器内弹出协议与免责声明并要求确认。

## ⚠️ Terracotta 是 AGPL-3.0（重要）
Terracotta（BurningTNT）实际许可是 **AGPL-3.0**（不是 GPL-3.0）。这意味着：
1. **分发必须提供对应源码** —— 其 Rust 源码已随本仓库提供在 `third_party_sources/Terracotta/`。
2. AGPL-3.0 有「网络条款」（§13）：若把该软件作为网络服务提供给他人使用，须向网络用户提供源码。
3. QCL 同时链接 GPL-3.0（HMCL-PE）与 AGPL-3.0（Terracotta），**合并作品按更严格的 AGPL-3.0 对待**；
   本项目 LICENSE 已升级为 AGPL-3.0。
