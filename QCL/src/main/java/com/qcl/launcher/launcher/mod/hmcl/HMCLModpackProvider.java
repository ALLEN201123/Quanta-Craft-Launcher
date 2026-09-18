package com.qcl.launcher.launcher.mod.hmcl;

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

/* loaded from: classes2.dex */
public final class HMCLModpackProvider implements ModpackProvider {
    public static final HMCLModpackProvider INSTANCE = new HMCLModpackProvider();

    @Override // com.qcl.launcher.launcher.mod.ModpackProvider
    public String getName() {
        return "HMCL";
    }

    @Override // com.qcl.launcher.launcher.mod.ModpackProvider
    public Modpack readManifest(ZipFile zipFile, Path path, Charset charset) throws IOException, JsonParseException {
        Modpack encoding = ((HMCLModpack) JsonUtils.fromNonNullJson(ZipTools.readTextZipEntry(zipFile, "modpack.json"), HMCLModpack.class)).setEncoding(charset);
        Version version = (Version) JsonUtils.fromNonNullJson(ZipTools.readTextZipEntry(zipFile, "minecraft/pack.json"), Version.class);
        if (version.getJar() == null) {
            if (StringUtils.isBlank(encoding.getVersion())) {
                throw new JsonParseException("Cannot recognize the game version of modpack " + zipFile + ".");
            }
            encoding.setManifest(HMCLModpackManifest.INSTANCE);
        } else {
            encoding.setManifest(HMCLModpackManifest.INSTANCE).setGameVersion(version.getJar());
        }
        return encoding;
    }

    /* loaded from: classes2.dex */
    public static class HMCLModpack extends Modpack {
        @Override // com.qcl.launcher.launcher.mod.Modpack
        public AsyncTask getInstallTask(File file, String str) {
            return new HMCLModpackInstallTask(file, this, str);
        }
    }
}
