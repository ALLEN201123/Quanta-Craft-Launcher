package com.qcl.launcher.launcher.mod;

import android.os.AsyncTask;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes2.dex */
public abstract class Modpack {
    private String author;
    private String description;
    private transient Charset encoding;
    private String gameVersion;
    private ModpackManifest manifest;
    private String name;
    private String version;

    public abstract AsyncTask getInstallTask(File file, String str);

    public Modpack() {
        this("", null, null, null, null, null, null);
    }

    public Modpack(String str, String str2, String str3, String str4, String str5, Charset charset, ModpackManifest modpackManifest) {
        this.name = str;
        this.author = str2;
        this.version = str3;
        this.gameVersion = str4;
        this.description = str5;
        this.encoding = charset;
        this.manifest = modpackManifest;
    }

    public String getName() {
        return this.name;
    }

    public Modpack setName(String str) {
        this.name = str;
        return this;
    }

    public String getAuthor() {
        return this.author;
    }

    public Modpack setAuthor(String str) {
        this.author = str;
        return this;
    }

    public String getVersion() {
        return this.version;
    }

    public Modpack setVersion(String str) {
        this.version = str;
        return this;
    }

    public String getGameVersion() {
        return this.gameVersion;
    }

    public Modpack setGameVersion(String str) {
        this.gameVersion = str;
        return this;
    }

    public String getDescription() {
        return this.description;
    }

    public Modpack setDescription(String str) {
        this.description = str;
        return this;
    }

    public Charset getEncoding() {
        return this.encoding;
    }

    public Modpack setEncoding(Charset charset) {
        this.encoding = charset;
        return this;
    }

    public ModpackManifest getManifest() {
        return this.manifest;
    }

    public Modpack setManifest(ModpackManifest modpackManifest) {
        this.manifest = modpackManifest;
        return this;
    }

    public static boolean acceptFile(String str, List<String> list, List<String> list2) {
        if (str.isEmpty()) {
            return true;
        }
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            if (str.equals(it.next())) {
                return false;
            }
        }
        if (list2 == null || list2.isEmpty()) {
            return true;
        }
        Iterator<String> it2 = list2.iterator();
        while (it2.hasNext()) {
            if (str.equals(it2.next())) {
                return true;
            }
        }
        return false;
    }
}
