package com.qcl.launcher.utils.io;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * ★ 1.2.3：把 jar 的 class「覆盖进」目标 jar。
 *
 * 【为什么需要它】
 * 「文件覆盖式安装」是远古版本（b1.7.3 时代）装模组/加载器的**标准做法**：
 * 把补丁 jar 里的 class 直接塞进 minecraft.jar。启动器这边有两个地方都要用：
 *  1. Risugami's ModLoader（{@link com.qcl.launcher.launcher.download.modloader.ModLoaderInstallTask}）
 *  2. MultiMC / Prism 整合包的 `jarmods/`（{@link com.qcl.launcher.launcher.mod.multimc.MultiMCModpackInstallTask}）
 *     —— MultiMC 的 jarmod 语义就是「把 jar 合并进 minecraft.jar」，
 *        比如 b1.7.3 的 Cursed Fabric 包，Intermediary Mappings 就是个 jarmod，
 *        **不合并的话加载器根本没生效，点启动必崩**。
 *
 * 【两个必须做对的细节】
 * 1. **必须丢掉 META-INF**：原版 minecraft.jar 是签过名的，里面有
 *    `META-INF/MOJANG_C.*` 与 `*.SF`/`*.RSA`。改过内容后签名必然失效，JVM 会抛
 *    `SecurityException: signer information does not match ...`。
 *    远古时代的官方装法就明确写着「Delete META-INF folder in the jar」。
 * 2. **先写临时文件再替换 + 备份**：直接就地改 jar，中途失败会把玩家的游戏本体写坏。
 *    所以流程是：读全部条目 → 覆盖 → 写 .tmp → 原文件改名备份 → 替换 → 删备份。
 */
public final class JarMerger {

    private JarMerger() {
    }

    /**
     * 把 {@code sourceJars} 里的所有条目覆盖进 {@code targetJar}。
     * 目标 jar 里已有的同名条目会被覆盖，其余原样保留；META-INF 全部丢弃。
     *
     * @return 实际写入的条目数（来自 sourceJars 的）
     */
    public static int mergeInto(File targetJar, Iterable<File> sourceJars) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();

        if (!targetJar.isFile()) {
            throw new IOException("目标 jar 不存在：" + targetJar);
        }

        // 1) 读原 jar（跳过签名相关的 META-INF）
        int stripped = 0;
        try (ZipFile jar = new ZipFile(targetJar)) {
            java.util.Enumeration<? extends ZipEntry> it = jar.entries();
            while (it.hasMoreElements()) {
                ZipEntry e = it.nextElement();
                if (e.isDirectory()) {
                    continue;
                }
                if (e.getName().toUpperCase().startsWith("META-INF/")) {
                    stripped++;
                    continue;
                }
                entries.put(e.getName(), readAll(jar.getInputStream(e)));
            }
        }
        if (stripped > 0) {
            android.util.Log.i("JarMerger", "已移除 " + stripped
                    + " 个 META-INF 签名条目（改过 jar 后签名必然失效）");
        }

        // 2) 用来源 jar 覆盖
        int injected = 0;
        for (File src : sourceJars) {
            if (src == null || !src.isFile()) {
                continue;
            }
            try (ZipFile z = new ZipFile(src)) {
                java.util.Enumeration<? extends ZipEntry> it = z.entries();
                while (it.hasMoreElements()) {
                    ZipEntry e = it.nextElement();
                    if (e.isDirectory()) {
                        continue;
                    }
                    String name = e.getName();
                    // 来源里的 META-INF 同样丢掉，否则又会把签名信息带回来
                    if (name.toUpperCase().startsWith("META-INF/")) {
                        continue;
                    }
                    entries.put(name, readAll(z.getInputStream(e)));
                    injected++;
                }
            }
        }
        if (injected == 0) {
            return 0;   // 没什么可合的，不动原文件
        }

        // 3) 写临时文件
        File tmp = new File(targetJar.getParentFile(), targetJar.getName() + ".merge.tmp");
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(tmp))) {
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                ZipEntry ze = new ZipEntry(e.getKey());
                ze.setTime(0L);   // 固定时间戳，保证结果可复现
                out.putNextEntry(ze);
                out.write(e.getValue());
                out.closeEntry();
            }
        }

        // 4) 备份 → 替换（失败则回滚，绝不让玩家丢游戏本体）
        File backup = new File(targetJar.getParentFile(), targetJar.getName() + ".merge.bak");
        //noinspection ResultOfMethodCallIgnored
        backup.delete();
        if (!targetJar.renameTo(backup)) {
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
            throw new IOException("无法备份原 jar：" + targetJar);
        }
        if (!tmp.renameTo(targetJar)) {
            //noinspection ResultOfMethodCallIgnored
            backup.renameTo(targetJar);   // 恢复
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
            throw new IOException("写入 jar 失败，已恢复原文件：" + targetJar);
        }
        //noinspection ResultOfMethodCallIgnored
        backup.delete();
        return injected;
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }
}
