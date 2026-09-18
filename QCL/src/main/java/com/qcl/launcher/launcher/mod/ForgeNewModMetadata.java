package com.qcl.launcher.launcher.mod;

import com.google.gson.JsonParseException;
import com.moandjiezana.toml.Toml;
import com.qcl.launcher.launcher.mod.LocalModFile;
import com.qcl.launcher.utils.io.ZipTools;
import com.qcl.launcher.utils.string.StringUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/* loaded from: classes2.dex */
public final class ForgeNewModMetadata {
    private final String license;
    private final String loaderVersion;
    private final String logoFile;
    private final String modLoader;
    private final List<Mod> mods;

    public ForgeNewModMetadata() {
        this("", "", "", "", Collections.emptyList());
    }

    public ForgeNewModMetadata(String str, String str2, String str3, String str4, List<Mod> list) {
        this.modLoader = str;
        this.loaderVersion = str2;
        this.logoFile = str3;
        this.license = str4;
        this.mods = list;
    }

    public String getModLoader() {
        return this.modLoader;
    }

    public String getLoaderVersion() {
        return this.loaderVersion;
    }

    public String getLogoFile() {
        return this.logoFile;
    }

    public String getLicense() {
        return this.license;
    }

    public List<Mod> getMods() {
        return this.mods;
    }

    /* loaded from: classes2.dex */
    public static class Mod {
        private final String authors;
        private final String description;
        private final String displayName;
        private final String displayURL;
        private final String modId;
        private final String side;
        private final String version;

        public Mod() {
            this("", "", "", "", "", "", "");
        }

        public Mod(String str, String str2, String str3, String str4, String str5, String str6, String str7) {
            this.modId = str;
            this.version = str2;
            this.displayName = str3;
            this.side = str4;
            this.displayURL = str5;
            this.authors = str6;
            this.description = str7;
        }

        public String getModId() {
            return this.modId;
        }

        public String getVersion() {
            return this.version;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public String getSide() {
            return this.side;
        }

        public String getDisplayURL() {
            return this.displayURL;
        }

        public String getAuthors() {
            return this.authors;
        }

        public String getDescription() {
            return this.description;
        }
    }

    public static LocalModFile fromFile(ModManager modManager, Path path) throws IOException, JsonParseException {
        String readNormalMeta = ZipTools.readNormalMeta(path.toString(), "META-INF/mods.toml");
        InputStream fileInputStream = ZipTools.getFileInputStream(path.toString(), "META-INF/MANIFEST.MF");
        if (StringUtils.isBlank(readNormalMeta)) {
            throw new IOException("File " + path + " is not a Forge1.13+ mod.");
        }
        ForgeNewModMetadata forgeNewModMetadata = (ForgeNewModMetadata) new Toml().read(readNormalMeta).to(ForgeNewModMetadata.class);
        if (forgeNewModMetadata == null || forgeNewModMetadata.getMods().isEmpty()) {
            throw new IOException("Mod " + path + " `mods.toml` is malformed..");
        }
        Mod mod = forgeNewModMetadata.getMods().get(0);
        String str = "";
        if (fileInputStream != null) {
            try {
                str = new Manifest(fileInputStream).getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            } catch (IOException e) {
                e.printStackTrace();
            }
            fileInputStream.close();
        }
        return new LocalModFile(modManager, modManager.getLocalMod(mod.getModId(), ModLoaderType.FORGE), path, mod.getDisplayName(), new LocalModFile.Description(mod.getDescription()), mod.getAuthors(), mod.getVersion().replace("${file.jarVersion}", str), "", mod.getDisplayURL(), forgeNewModMetadata.getLogoFile());
    }
}
