package com.qcl.launcher.launcher.mod.qclpack;

import android.os.AsyncTask;
import com.google.gson.JsonParseException;
import com.qcl.launcher.launcher.game.Version;
import com.qcl.launcher.launcher.mod.Modpack;
import com.qcl.launcher.launcher.mod.ModpackProvider;
import com.qcl.launcher.utils.gson.JsonUtils;
import com.qcl.launcher.utils.io.ZipTools;
import com.qcl.launcher.utils.string.StringUtils;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import org.apache.commons.compress.archivers.zip.ZipFile;

/**
 * 整合包格式提供器（zip 内有 {@code modpack.json} + {@code minecraft/pack.json}）。
 *
 * <p>★ 2026-10-06 改名：原类名带 "HMCL" 前缀（本工程由 HMCL-PE 系启动器演化而来），
 * 现统一改成 QCL 前缀；{@link #getName()} 也按用户要求改成 "QCL"，
 * **不再兼容旧的 "HMCL" 标识** —— 代价是已安装的旧整合包会因 type 不匹配而认不出来，
 * 玩家删掉旧的重下即可（用户已明确确认接受这一点）。
 */
public final class QclModpackProvider implements ModpackProvider {
    public static final QclModpackProvider INSTANCE = new QclModpackProvider();

    @Override // com.qcl.launcher.launcher.mod.ModpackProvider
    public String getName() {
        // ★ 2026-10-06：按用户要求直接改成 "QCL"（去掉 HMCL 品牌）。
        //   代价：已安装的旧整合包 type 不匹配 → 认不出来；玩家删掉旧的重新下载即可。
        return "QCL";
    }

    @Override // com.qcl.launcher.launcher.mod.ModpackProvider
    public Modpack readManifest(ZipFile zipFile, Path path, Charset charset) throws IOException, JsonParseException {
        Modpack encoding = ((QclModpack) JsonUtils.fromNonNullJson(ZipTools.readTextZipEntry(zipFile, "modpack.json"), QclModpack.class)).setEncoding(charset);
        Version version = (Version) JsonUtils.fromNonNullJson(ZipTools.readTextZipEntry(zipFile, "minecraft/pack.json"), Version.class);
        if (version.getJar() == null) {
            if (StringUtils.isBlank(encoding.getVersion())) {
                throw new JsonParseException("Cannot recognize the game version of modpack " + zipFile + ".");
            }
            encoding.setManifest(QclModpackManifest.INSTANCE);
        } else {
            encoding.setManifest(QclModpackManifest.INSTANCE).setGameVersion(version.getJar());
        }
        return encoding;
    }

    /* loaded from: classes2.dex */
    public static class QclModpack extends Modpack {
        @Override // com.qcl.launcher.launcher.mod.Modpack
        public AsyncTask getInstallTask(File file, String str) {
            return new QclModpackInstallTask(file, this, str);
        }
    }
}
