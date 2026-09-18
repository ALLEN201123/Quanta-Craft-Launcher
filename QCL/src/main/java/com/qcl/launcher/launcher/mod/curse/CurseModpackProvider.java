package com.qcl.launcher.launcher.mod.curse;

import android.os.AsyncTask;
import com.google.gson.JsonParseException;
import com.qcl.launcher.launcher.mod.Modpack;
import com.qcl.launcher.launcher.mod.ModpackProvider;
import com.qcl.launcher.utils.gson.JsonUtils;
import com.qcl.launcher.utils.io.IOUtils;
import com.qcl.launcher.utils.io.ZipTools;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

/* loaded from: classes2.dex */
public final class CurseModpackProvider implements ModpackProvider {
    public static final CurseModpackProvider INSTANCE = new CurseModpackProvider();

    @Override // com.qcl.launcher.launcher.mod.ModpackProvider
    public String getName() {
        return "Curse";
    }

    @Override // com.qcl.launcher.launcher.mod.ModpackProvider
    public Modpack readManifest(ZipFile zipFile, Path path, Charset charset) throws IOException, JsonParseException {
        final CurseManifest curseManifest = (CurseManifest) JsonUtils.fromNonNullJson(ZipTools.readTextZipEntry(zipFile, "manifest.json"), CurseManifest.class);
        String str = "No description";
        try {
            ZipArchiveEntry entry = zipFile.getEntry("modlist.html");
            if (entry != null) {
                str = IOUtils.readFullyAsString(zipFile.getInputStream(entry));
            }
        } catch (Throwable unused) {
        }
        return new Modpack(curseManifest.getName(), curseManifest.getAuthor(), curseManifest.getVersion(), curseManifest.getMinecraft().getGameVersion(), str, charset, curseManifest) { // from class: com.qcl.launcher.launcher.mod.curse.CurseModpackProvider.1
            @Override // com.qcl.launcher.launcher.mod.Modpack
            public AsyncTask getInstallTask(File file, String str2) {
                return new CurseInstallTask(file, this, curseManifest, str2);
            }
        };
    }
}
