package com.qcl.launcher.launcher.game;

import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.qcl.launcher.utils.DigestUtils;
import com.qcl.launcher.utils.Hex;
import com.qcl.launcher.utils.gson.tools.TolerableValidationException;
import com.qcl.launcher.utils.gson.tools.Validation;
import com.qcl.launcher.utils.string.StringUtils;
import com.qcl.launcher.utils.string.ToStringBuilder;
import java.io.IOException;
import java.nio.file.Path;

/* loaded from: classes2.dex */
public class DownloadInfo implements Validation {

    @SerializedName("sha1")
    private final String sha1;

    @SerializedName("size")
    private final int size;

    @SerializedName("url")
    private final String url;

    public DownloadInfo() {
        this("");
    }

    public DownloadInfo(String str) {
        this(str, null);
    }

    public DownloadInfo(String str, String str2) {
        this(str, str2, 0);
    }

    public DownloadInfo(String str, String str2, int i) {
        this.url = str;
        this.sha1 = str2;
        this.size = i;
    }

    public String getUrl() {
        return this.url;
    }

    public String getSha1() {
        if ("invalid".equals(this.sha1)) {
            return null;
        }
        return this.sha1;
    }

    public int getSize() {
        return this.size;
    }

    public String toString() {
        return new ToStringBuilder(this).append("url", this.url).append("sha1", this.sha1).append("size", Integer.valueOf(this.size)).toString();
    }

    @Override // com.qcl.launcher.utils.gson.tools.Validation
    public void validate() throws JsonParseException, TolerableValidationException {
        if (StringUtils.isBlank(this.url)) {
            throw new TolerableValidationException();
        }
    }

    public boolean validateChecksum(Path path, boolean z) throws IOException {
        return getSha1() == null ? z : Hex.encodeHex(DigestUtils.digest("SHA-1", path)).equalsIgnoreCase(getSha1());
    }
}
