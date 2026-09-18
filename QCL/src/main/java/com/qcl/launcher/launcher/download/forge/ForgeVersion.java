package com.qcl.launcher.launcher.download.forge;

import com.google.gson.JsonParseException;
import com.qcl.launcher.utils.gson.tools.Validation;
import java.util.Collections;
import java.util.List;

/* loaded from: classes2.dex */
public class ForgeVersion implements Validation {
    private final String branch;
    private final int build;
    private final List<File> files;
    private final String mcversion;
    private final String modified;
    private final String version;

    public ForgeVersion() {
        this(null, 0, "", null, "", Collections.emptyList());
    }

    public ForgeVersion(String str, int i, String str2, String str3, String str4, List<File> list) {
        this.branch = str;
        this.build = i;
        this.mcversion = str2;
        this.modified = str3;
        this.version = str4;
        this.files = list;
    }

    public String getBranch() {
        return this.branch;
    }

    public int getBuild() {
        return this.build;
    }

    public String getGameVersion() {
        return this.mcversion;
    }

    public String getModified() {
        return this.modified;
    }

    public String getVersion() {
        return this.version;
    }

    public List<File> getFiles() {
        return this.files;
    }

    @Override // com.qcl.launcher.utils.gson.tools.Validation
    public void validate() throws JsonParseException {
        if (this.files == null) {
            throw new JsonParseException("ForgeVersion files cannot be null");
        }
        if (this.version == null) {
            throw new JsonParseException("ForgeVersion version cannot be null");
        }
        if (this.mcversion == null) {
            throw new JsonParseException("ForgeVersion mcversion cannot be null");
        }
    }

    /* loaded from: classes2.dex */
    public static final class File {
        private final String category;
        private final String format;
        private final String hash;

        public File() {
            this("", "", "");
        }

        public File(String str, String str2, String str3) {
            this.format = str;
            this.category = str2;
            this.hash = str3;
        }

        public String getFormat() {
            return this.format;
        }

        public String getCategory() {
            return this.category;
        }

        public String getHash() {
            return this.hash;
        }
    }
}
