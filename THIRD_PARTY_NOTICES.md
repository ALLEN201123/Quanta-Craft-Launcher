# QCL（Quanta Craft Launcher / 量子方块启动器）第三方组件与开源合规说明

## 本项目自身的许可
QCL 是基于 **HMCL-PE**（Hello Minecraft! Launcher Pocket Edition）重构的衍生作品。
HMCL-PE 以 **GNU GPL-3.0** 授权，因此 **QCL 同样以 GNU GPL-3.0（或更新版本）授权**，
完整协议全文见工程根目录的 `LICENSE`。

### 发布者的义务（GPL-3.0）
1. **分发（发布 APK / 任何形式分发）时必须同时提供完整对应的源码**（可放到公开仓库并在下载页给出链接）。
2. 保留所有版权声明与许可声明（本文件与 `LICENSE` 必须随源码一起提供）。
3. 若修改了上游代码，需以 GPL-3.0 兼容许可开放修改后的源码。
4. 网络提供服务时不强制公开源码（GPL-3.0 本身不含 AGPL 的网络条款），但本项目选择一并公开以便社区协作。

## 第三方组件清单
| 组件 | 作者 / 来源 | 许可 | 用途 |
|---|---|---|---|
| HMCL-PE | Tungs（B 站 18115101） | GPL-3.0 | 本项目的前身与代码基础 |
| PojavLauncher | PojavLauncherTeam（已归档，后继 Amethyst-Android） | LGPL-3.0 | JVM 启动、LWJGL 移植、caciocavallo 等 |
| Boat（Cosine / 摆渡人后端） | Cosine 相关作者 | 见其仓库 | 另一套启动后端与 lwjgl2 移植 |
| Terracotta（陶瓷联机） | **BurningTNT** | GPL-3.0 | 多人联机（NAT 穿透 / 组网 / 房间邀请码） |
| LWJGL 2 / 3 | LWJGL 团队 | BSD-3-Clause | OpenGL / 输入 / 音频绑定 |
| GL4ES | ptitSeb 等 | MIT | OpenGL 2.x → OpenGL ES 1.1/2.0 转换层 |
| OpenAL Soft | kcat 等 | LGPL-2.0 | 音频 |
| OpenJDK / 移动端 JRE 构建 | OpenJDK、FCL 项目 | GPL-2.0 with Classpath Exception | Java 运行时 |
| BMCLAPI | bangbang93 | 见其服务说明 | 国内下载镜像 |
| mcmod.cn（MC 百科） | mcmod.cn | 见其站点说明 | 百科链接 |
| Minecraft（游戏资源/贴图，仅作图标与界面参考） | Mojang / Microsoft | 专有资产，遵循 Minecraft EULA | 本项目不分发游戏本体 |

## 署名位置
启动器内「设置 → 关于」页已列出：BMCLAPI、MC 百科、原版项目（HMCL-PE / Tungs）、
本作作者（Rod123456）、以及 **Terracotta / BurningTNT** 与 GPL-3.0 说明。

## 多人联机的使用限制（合规与免责）
多人联机功能仅限 **中国大陆地区** 使用；在境外使用本功能，本启动器不承担任何责任，
并可能带来法律风险，由使用者自行评估与承担。开启前会在启动器内弹出协议与免责声明并要求确认。
