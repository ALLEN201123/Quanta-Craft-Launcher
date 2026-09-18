package com.qcl.launcher.launcher.mod.mcbbs;

import com.google.gson.JsonParseException;
import com.qcl.launcher.launcher.mod.Modpack;
import com.qcl.launcher.launcher.mod.ModpackProvider;
import com.qcl.launcher.utils.gson.JsonUtils;
import com.qcl.launcher.utils.io.IOUtils;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

/* loaded from: classes2.dex */
public final class McbbsModpackProvider implements ModpackProvider {
    public static final McbbsModpackProvider INSTANCE = new McbbsModpackProvider();

    @Override // com.qcl.launcher.launcher.mod.ModpackProvider
    public String getName() {
        return "Mcbbs";
    }

    private static Modpack fromManifestFile(String str, Charset charset) throws IOException, JsonParseException {
        return ((McbbsModpackManifest) JsonUtils.fromNonNullJson(str, McbbsModpackManifest.class)).toModpack(charset);
    }

    @Override // com.qcl.launcher.launcher.mod.ModpackProvider
    public Modpack readManifest(ZipFile zipFile, Path path, Charset charset) throws IOException, JsonParseException {
        ZipArchiveEntry entry = zipFile.getEntry("mcbbs.packmeta");
        if (entry != null) {
            return fromManifestFile(IOUtils.readFullyAsString(zipFile.getInputStream(entry)), charset);
        }
        ZipArchiveEntry entry2 = zipFile.getEntry("manifest.json");
        if (entry2 != null) {
            return fromManifestFile(IOUtils.readFullyAsString(zipFile.getInputStream(entry2)), charset);
        }
        throw new IOException("`mcbbs.packmeta` or `manifest.json` cannot be found");
    }
}
