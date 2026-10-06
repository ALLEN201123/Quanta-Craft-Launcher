# 远古版本中文：硬编码翻译 key 补丁

## 背景

远古版本（b1.6 ~ 1.0 / indev / infdev）**没有现在的 `.lang` 语言文件机制**，
界面文案有三种来源：

1. **英文字面量直接硬编码在 class 常量池里** —— 如 `Game over!`、`Select world`。
   处理方式：字节码替换（`StrTranslate`），映射表 `en2zh3.tsv`。
2. **翻译 key 硬编码在常量池里**，运行时交给语言表查表 —— 如
   `deathScreen.respawn`、`options.renderDistance.normal`、`gui.toMenu`。
   **这些 key 查不到就是直接显示 key 本身（所以界面全是英文）**。
   本目录的工作就是把这批 key 补进映射表，让 `StrTranslate` 把 key 本身换成中文。
3. 崩溃日志 / 存档错误信息 —— **不翻**（玩家看不到，翻了还会破坏日志可读性）。

## 关键文件

| 文件 | 说明 |
|---|---|
| `en2zh3.tsv` | 主映射表（英→中），1117 条。`StrTranslate` 的输入 |
| `legacy_keys.tsv` | 扫描出来的**全部硬编码翻译 key** + 出现在哪些版本（脚本产物，可重生成） |
| `legacy_keys_zh.tsv` | 这些 key 的中文对照（已并入 `en2zh3.tsv`） |
| `extract_keys.py` | 从已装 jar 里扫出所有硬编码翻译 key |
| `repack.py` | 把翻译产物**覆盖回原 jar**（★ 只挑 `.class`） |
| `mkasset.py` | 生成 `cn_a1v_<版本>_tr.jar` 资产（★ 只含**字符串真变了**的 class） |
| `scan_final.py` | 诊断脚本：把残留字符串分成 KEY / UI / WORD 三类 |

## ★ 两个必须遵守的打包铁律

1. **`repack.py` 只能挑 `.class`**。
   StrTranslate 的产物目录会混入整个 jar 的资源（png/ogg/txt，b1.7.3 有 87 个）。
   整目录塞回去会**用英文原文资源覆盖掉 QCL 注入的官方中文点阵
   `font/glyph_XX.png`** → 中文全变方块。

2. **`mkasset.py` 只收「字符串常量池真的变了」的 class**。
   翻译产物里每个被处理过的 class 都会重写字节码，但只有部分真换了字符串。
   与既有 120 个 asset jar 的规则保持一致（1.0 那个只 52 个，产物有 99 个）。

## 重跑流程（补翻某个远古版本）

```bash
ASM=<gradle 缓存里的 asm-9.6.jar>
PY="C:/Users/123/.workbuddy/binaries/python/versions/3.13.12/python.exe"

# 0) 先把该版本的**原版** jar 放到 _qcl_test/orig_<版本>.jar
#    （设备上的 <版本>.jar.orig 就是纯原版备份）
# 1) 翻译（产物目录会混资源，正常）
"$JAVA_HOME/bin/java" -cp "$ASM;." StrTranslate \
    orig_<版本>.jar tr_<版本> en2zh3.tsv
# 2) 生成资产 jar（只含变化 class）
"$PY" mkasset.py orig_<版本>.jar tr_<版本> asset_<版本>.jar
# 3) 放进 assets，文件名规则：版本名里非字母数字全换 _
cp asset_<版本>.jar \
   /d/QCL-build/QCL/src/main/assets/cn_a1v_<版本名_下划线化>_tr.jar
```

★ 改完 assets 记得清运行时提取缓存，否则不重新注入：
`QCL/build/intermediates/{assets,merged_assets,compressed_assets}`

## 覆盖范围（本次实测过的）

| 版本 | 替换数 | 说明 |
|---|---|---|
| b1.6.6 | 376 | |
| b1.7.3 | 379 | |
| 1.0 | 510 | 之前只有 124，本次补上 294 个 key |
| b1.9-pre6 | 509 | |

**其余版本（Alpha/Beta 全系、indev/infdev）尚未用扩充后的映射表重跑**，
需要时按上面流程补跑 —— 它们的 key 基本都在 `legacy_keys.tsv` 里。
