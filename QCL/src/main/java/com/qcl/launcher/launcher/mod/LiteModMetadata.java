package com.qcl.launcher.launcher.mod;

import com.google.gson.JsonParseException;
import com.qcl.launcher.launcher.mod.LocalModFile;
import com.qcl.launcher.utils.gson.JsonUtils;
import com.qcl.launcher.utils.io.IOUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/* loaded from: classes2.dex */
public final class LiteModMetadata {
    private final String author;
    private final String checkUpdateUrl;
    private final String[] classTransformerClasses;
    private final String description;
    private final String mcversion;
    private final String modpackName;
    private final String modpackVersion;
    private final String name;
    private final String revision;
    private final String updateURI;
    private final String version;

    public LiteModMetadata() {
        this("", "", "", "", "", new String[]{""}, "", "", "", "", "");
    }

    public LiteModMetadata(String str, String str2, String str3, String str4, String str5, String[] strArr, String str6, String str7, String str8, String str9, String str10) {
        this.name = str;
        this.version = str2;
        this.mcversion = str3;
        this.revision = str4;
        this.author = str5;
        this.classTransformerClasses = strArr;
        this.description = str6;
        this.modpackName = str7;
        this.modpackVersion = str8;
        this.checkUpdateUrl = str9;
        this.updateURI = str10;
    }

    public String getName() {
        return this.name;
    }

    public String getVersion() {
        return this.version;
    }

    public String getGameVersion() {
        return this.mcversion;
    }

    public String getRevision() {
        return this.revision;
    }

    public String getAuthor() {
        return this.author;
    }

    public String[] getClassTransformerClasses() {
        return this.classTransformerClasses;
    }

    public String getDescription() {
        return this.description;
    }

    public String getModpackName() {
        return this.modpackName;
    }

    public String getModpackVersion() {
        return this.modpackVersion;
    }

    public String getCheckUpdateUrl() {
        return this.checkUpdateUrl;
    }

    public String getUpdateURI() {
        return this.updateURI;
    }

    public static LocalModFile fromFile(ModManager modManager, Path path) throws IOException, JsonParseException {
        ZipFile zipFile = new ZipFile(path.toFile());
        try {
            ZipEntry entry = zipFile.getEntry("litemod.json");
            if (entry == null) {
                throw new IOException("File " + path + "is not a LiteLoader mod.");
            }
            LiteModMetadata liteModMetadata = (LiteModMetadata) JsonUtils.GSON.fromJson(IOUtils.readFullyAsString(zipFile.getInputStream(entry)), LiteModMetadata.class);
            if (liteModMetadata == null) {
                throw new IOException("Mod " + path + " `litemod.json` is malformed.");
            }
            LocalModFile localModFile = new LocalModFile(modManager, modManager.getLocalMod(liteModMetadata.getName(), ModLoaderType.LITE_LOADER), path, liteModMetadata.getName(), new LocalModFile.Description(liteModMetadata.getDescription()), liteModMetadata.getAuthor(), liteModMetadata.getVersion(), liteModMetadata.getGameVersion(), liteModMetadata.getUpdateURI(), "");
            zipFile.close();
            return localModFile;
        } catch (Throwable th) {
            try {
                zipFile.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }
}
