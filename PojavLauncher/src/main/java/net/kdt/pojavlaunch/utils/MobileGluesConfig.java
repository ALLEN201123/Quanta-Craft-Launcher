package net.kdt.pojavlaunch.utils;

import android.content.Context;

import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * ★★★★★ 1.5.8：**MobileGlues 运行时配置写入**（照搬 FCL 的
 * {@code FCLauncher.writeMobileGluesConfig}）。
 *
 * <h3>为什么必须做这件事（26.3 启动崩溃的最后一环）</h3>
 *
 * <p>实测日志（26.3 + MG，Android 16 / aarch64）：
 * <pre>
 *   MG_DIR_PATH = /sdcard/MG            ← MG 默认读这里
 *   [MobileGlues] Setting: ignoreError = 0    ← 编译默认值，不是我们要的 1
 *
 *   java.lang.ArithmeticException: / by zero
 *       at java.lang.Math.floorDiv(Math.java:1476)
 *       at net.minecraft.util.Mth.positiveCeilDiv(Mth.java:815)
 *       at net.minecraft.client.renderer.DynamicGpuDataStorageMapped.&lt;init&gt;(:32)
 *       at com.mojang.blaze3d.systems.RenderSystem.initRenderer(:246)
 * </pre>
 *
 * <p>因果链：
 * <ol>
 *   <li>MG 官方 release note 明确写：<b>26.3-snapshot-3 及以后必须开启
 *       "Ignore shader/program error"</b>（即 {@code ignoreError=1}）</li>
 *   <li>QCL 从来没给 MG 写过配置 ⇒ MG 用编译默认值 {@code ignoreError=0}</li>
 *   <li>不吞 GL 错误 ⇒ MG 的 GL 实现不返回
 *       {@code GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT} ⇒
 *       {@code DeviceLimits.minUniformOffsetAlignment()} 返回 <b>0</b></li>
 *   <li>{@code Mth.roundToward(x, 0)} → {@code floorDiv(x, 0)} → 除零崩溃</li>
 * </ol>
 *
 * <h3>照 FCL 的两个关键做法</h3>
 * <ol>
 *   <li><b>不共用 {@code /sdcard/MG}</b> —— 改用 app 私有目录
 *       {@code <files>/mobileglues/}，并通过 {@code MG_DIR_PATH} 指过去。
 *       （Android 11+ 分区存储下，往 /sdcard/MG 写文件本来就可能失败。）</li>
 *   <li><b>只覆盖显式配置过的键</b> —— 已有 config.json 里 MG 自己写的键
 *       （以及它自管的未知键）全部保留，避免把上游默认值钉死。</li>
 * </ol>
 *
 * <p>★ 本类**不引第三方 JSON 库**：QCL 自带 org.json（Android 平台自带）。
 */
public final class MobileGluesConfig {

    /** MG 私有配置目录名（在 {@code context.getFilesDir()} 下）。 */
    private static final String DIR_NAME = "mobileglues";

    /** MG 的配置文件。 */
    private static final String FILE_NAME = "config.json";

    /**
     * 默认要写进 config.json 的键值。
     *
     * <p>当前只有一项，但它就是 26.3 能不能启动的关键：
     * {@code ignoreError = 1}（MG 官方要求 26.3-snapshot-3+ 必须开）。
     *
     * <p>以后要做「齿轮配置界面」时，把用户选的值合并进这个 map 即可。
     */
    private static final String[][] DEFAULTS = new String[][]{
            // ★★★ 关键修正：MG 读的键名是 **enableNoError**，不是 ignoreError。
            //
            //   MG 源码（MobileGlues-cpp/config/settings.cpp）：
            //     NoErrorConfig noErrorConfig =
            //         success ? static_cast<NoErrorConfig>(config_get_int("enableNoError"))
            //                 : NoErrorConfig::Auto;
            //     switch (noErrorConfig) {
            //     case Level1: global_settings.ignore_error = IgnoreErrorLevel::Partial; break;
            //     case Level2: global_settings.ignore_error = IgnoreErrorLevel::Full;    break;
            //     case Disable: default:
            //                  global_settings.ignore_error = IgnoreErrorLevel::None;    break;
            //     }
            //
            //   日志里的 "[MobileGlues] Setting: ignoreError = 0" 是
            //   **内部枚举 ignore_error 的值**（0=None），不是配置键名。
            //   写 "ignoreError":1 完全无效（MG 根本不看这个键）。
            //
            //   值：1 = Partial（部分忽略），2 = Full（完全忽略），0 = None。
            //   26.3 需要至少 Partial（官方 release note 说 "Ignore shader/program error"
            //   must be enabled），这里取 2（Full）最稳妥——宁可多吞一些
            //   无害错误，也不要因为一个小错误崩掉整个渲染初始化。
            {"enableNoError", "2"},
    };

    private MobileGluesConfig() {
    }

    /**
     * MG 的私有配置目录（不存在时**不**自动创建，由 {@link #ensureConfig} 建）。
     */
    public static File dirOf(Context context) {
        return new File(context.getFilesDir(), DIR_NAME);
    }

    /**
     * 确保 {@code <files>/mobileglues/config.json} 存在且含必要键。
     *
     * @return 配置目录（调用方用它设 {@code MG_DIR_PATH}）；创建失败返回 null
     */
    public static File ensureConfig(Context context) {
        if (context == null) {
            return null;
        }
        File dir = dirOf(context);
        try {
            if (!dir.isDirectory() && !dir.mkdirs()) {
                android.util.Log.w("jrelog", "[MG] 无法创建配置目录: " + dir);
                return null;
            }
            File cfg = new File(dir, FILE_NAME);

            // ① 读旧配置（存在且能解析就保留，解析失败就重头来）
            JSONObject json = new JSONObject();
            String oldText = null;
            if (cfg.isFile()) {
                try {
                    oldText = new String(readAll(cfg), StandardCharsets.UTF_8);
                    json = new JSONObject(oldText);
                } catch (Throwable t) {
                    json = new JSONObject();
                }
            }

            // ② 只覆盖我们显式管的键（其余原样保留，交给 MG 自管）
            boolean changed = false;
            for (String[] kv : DEFAULTS) {
                String key = kv[0];
                int want;
                try {
                    want = Integer.parseInt(kv[1]);
                } catch (Throwable t) {
                    continue;
                }
                int cur = json.optInt(key, Integer.MIN_VALUE);
                if (cur != want) {
                    json.put(key, want);
                    changed = true;
                }
            }

            // ③ 内容有变化才写（避免每次启动都动磁盘）
            String newText = json.toString();
            if (!changed && oldText != null && newText.equals(oldText)) {
                android.util.Log.i("jrelog", "[MG] config.json 已是最新: " + cfg);
                return dir;
            }
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(cfg);
                out.write(newText.getBytes(StandardCharsets.UTF_8));
                out.flush();
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (Throwable ignored) {
                    }
                }
            }
            android.util.Log.i("jrelog", "[MG] 已写入 config.json: " + newText);
            return dir;
        } catch (Throwable t) {
            android.util.Log.w("jrelog", "[MG] 写配置失败（不影响启动）: " + t);
            // 目录可能建好了，仍然返回它 —— MG 会自己补默认值
            return dir.isDirectory() ? dir : null;
        }
    }

    private static byte[] readAll(File f) throws IOException {
        byte[] data = new byte[(int) f.length()];
        DataInputStream in = null;
        try {
            in = new DataInputStream(new FileInputStream(f));
            in.readFully(data);
            return data;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }
}
