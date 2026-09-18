package com.qcl.launcher.launcher.mod;

import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.qcl.launcher.launcher.mod.LocalModFile;
import com.qcl.launcher.utils.gson.JsonUtils;
import com.qcl.launcher.utils.io.ZipTools;
import com.qcl.launcher.utils.string.StringUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/* loaded from: classes2.dex */
public final class ForgeOldModMetadata {
    private final String author;
    private final String[] authorList;
    private final String[] authors;
    private final String credits;
    private final String description;
    private final String logoFile;
    private final String mcversion;

    @SerializedName("modid")
    private final String modId;
    private final String name;
    private final String updateUrl;
    private final String url;
    private final String version;

    public ForgeOldModMetadata() {
        this("", "", "", "", "", "", "", "", "", "", new String[0], new String[0]);
    }

    public ForgeOldModMetadata(String str, String str2, String str3, String str4, String str5, String str6, String str7, String str8, String str9, String str10, String[] strArr, String[] strArr2) {
        this.modId = str;
        this.name = str2;
        this.description = str3;
        this.author = str4;
        this.version = str5;
        this.logoFile = str6;
        this.mcversion = str7;
        this.url = str8;
        this.updateUrl = str9;
        this.credits = str10;
        this.authorList = strArr;
        this.authors = strArr2;
    }

    public String getModId() {
        return this.modId;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public String getAuthor() {
        return this.author;
    }

    public String getVersion() {
        return this.version;
    }

    public String getLogoFile() {
        return this.logoFile;
    }

    public String getGameVersion() {
        return this.mcversion;
    }

    public String getUrl() {
        return this.url;
    }

    public String getUpdateUrl() {
        return this.updateUrl;
    }

    public String getCredits() {
        return this.credits;
    }

    public String[] getAuthorList() {
        return this.authorList;
    }

    public String[] getAuthors() {
        return this.authors;
    }

    public static LocalModFile fromFile(ModManager modManager, Path path) throws IOException, JsonParseException {
        String readNormalMeta = ZipTools.readNormalMeta(path.toString(), "mcmod.info");
        if (StringUtils.isBlank(readNormalMeta)) {
            throw new IOException("File " + path + " is not a Forge mod.");
        }
        List list = (List) JsonUtils.GSON.fromJson(readNormalMeta, new TypeToken<List<ForgeOldModMetadata>>() { // from class: com.qcl.launcher.launcher.mod.ForgeOldModMetadata.1
        }.getType());
        if (list == null || list.isEmpty()) {
            throw new IOException("Mod " + path + " `mcmod.info` is malformed..");
        }
        ForgeOldModMetadata forgeOldModMetadata = (ForgeOldModMetadata) list.get(0);
        String author = forgeOldModMetadata.getAuthor();
        if (StringUtils.isBlank(author) && forgeOldModMetadata.getAuthors().length > 0) {
            author = String.join(", ", forgeOldModMetadata.getAuthors());
        }
        if (StringUtils.isBlank(author) && forgeOldModMetadata.getAuthorList().length > 0) {
            author = String.join(", ", forgeOldModMetadata.getAuthorList());
        }
        if (StringUtils.isBlank(author)) {
            author = forgeOldModMetadata.getCredits();
        }
        return new LocalModFile(modManager, modManager.getLocalMod(forgeOldModMetadata.getModId(), ModLoaderType.FORGE), path, forgeOldModMetadata.getName(), new LocalModFile.Description(forgeOldModMetadata.getDescription()), author, forgeOldModMetadata.getVersion(), forgeOldModMetadata.getGameVersion(), StringUtils.isBlank(forgeOldModMetadata.getUrl()) ? forgeOldModMetadata.getUpdateUrl() : forgeOldModMetadata.url, forgeOldModMetadata.getLogoFile());
    }
}
