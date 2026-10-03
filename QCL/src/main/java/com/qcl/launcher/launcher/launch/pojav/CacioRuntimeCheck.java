package com.qcl.launcher.launcher.launch.pojav;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * ★ 1.4.6：远古版本运行环境（caciocavallo）**启动前自检**。
 *
 * <p>背景：cacio 那套文件的解包只在 {@code RuntimeInstallActivity} 里做，
 * 而它仅在 {@code KEY_READY=false} 或 app/runtime 版本号变化时才启动 ——
 * 所以**文件被删/装了一半/解包中断**时，缓存标记还在 → 再也不会补，
 * 结果 {@code Tools.getCacioJavaArgs()} 拼出空的 {@code -Xbootclasspath/p}，
 * JVM 直接 {@code Unrecognized option: -Xbootclasspath/p} 起不来（这个坑踩过两次）。
 *
 * <p>这里在启动游戏前做一次**极轻量**检查：关键文件缺任何一个，就从 APK assets 重新解出来，
 * 并把属主/权限交给应用自己（用 APK 自带 assets，不依赖网络）。
 */
public final class CacioRuntimeCheck {

    /** cacio 目录里必须存在的文件（缺一即视为不完整）。 */
    private static final String[] REQUIRED = {
            "ResConfHack.jar",
            "cacio-shared-1.10-SNAPSHOT.jar",
            "cacio-androidnw-1.10-SNAPSHOT.jar",
            "QCL_CACIO_PATCH_V2.txt",
    };
    private static final String ASSET_DIR = "app_runtime/caciocavallo";

    private CacioRuntimeCheck() {
    }

    /**
     * 确保运行环境完整；不完整就重新解包。
     *
     * @return true = 做了修复；false = 本来就完整或修复失败
     */
    public static boolean ensure(android.content.Context context, String targetDir) {
        try {
            File dir = new File(targetDir);
            boolean missing = false;
            for (String name : REQUIRED) {
                File f = new File(dir, name);
                if (!f.isFile() || f.length() <= 0L) {
                    missing = true;
                    break;
                }
            }
            if (!missing) {
                return false;
            }
            // 清掉可能残留的半成品，避免新旧混在一起
            deleteRecursively(dir);
            if (!dir.exists() && !dir.mkdirs()) {
                return false;
            }
            String[] names = context.getAssets().list(ASSET_DIR);
            if (names == null || names.length == 0) {
                return false;
            }
            for (String name : names) {
                File out = new File(dir, name);
                File parent = out.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                try (InputStream in = context.getAssets().open(ASSET_DIR + "/" + name);
                     FileOutputStream os = new FileOutputStream(out)) {
                    byte[] buf = new byte[65536];
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        os.write(buf, 0, n);
                    }
                }
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static void deleteRecursively(File f) {
        try {
            if (f.isDirectory()) {
                File[] kids = f.listFiles();
                if (kids != null) {
                    for (File k : kids) {
                        deleteRecursively(k);
                    }
                }
            }
            f.delete();
        } catch (Throwable ignored) {
        }
    }
}
