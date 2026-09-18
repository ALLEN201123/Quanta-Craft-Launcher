package com.qcl.launcher.launcher.mod.curse;

import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.qcl.launcher.utils.gson.tools.Validation;
import com.qcl.launcher.utils.io.NetworkUtils;
import java.net.URL;
import java.util.Objects;

/* loaded from: classes2.dex */
public final class CurseManifestFile implements Validation {

    @SerializedName("fileID")
    private final int fileID;

    @SerializedName("fileName")
    private final String fileName;

    @SerializedName("projectID")
    private final int projectID;

    @SerializedName("required")
    private final boolean required;

    @SerializedName("url")
    private final String url;

    public CurseManifestFile() {
        this(0, 0, null, null, true);
    }

    public CurseManifestFile(int i, int i2, String str, String str2, boolean z) {
        this.projectID = i;
        this.fileID = i2;
        this.fileName = str;
        this.url = str2;
        this.required = z;
    }

    public int getProjectID() {
        return this.projectID;
    }

    public int getFileID() {
        return this.fileID;
    }

    public String getFileName() {
        return this.fileName;
    }

    public boolean isRequired() {
        return this.required;
    }

    @Override // com.qcl.launcher.utils.gson.tools.Validation
    public void validate() throws JsonParseException {
        if (this.projectID == 0 || this.fileID == 0) {
            throw new JsonParseException("Missing Project ID or File ID.");
        }
    }

    public URL getUrl() {
        String str = this.url;
        if (str == null) {
            if (this.fileName != null) {
                return NetworkUtils.toURL(NetworkUtils.encodeLocation(String.format("https://edge.forgecdn.net/files/%d/%d/%s", Integer.valueOf(this.fileID / 1000), Integer.valueOf(this.fileID % 1000), this.fileName)));
            }
            return null;
        }
        return NetworkUtils.toURL(NetworkUtils.encodeLocation(str));
    }

    public CurseManifestFile withFileName(String str) {
        return new CurseManifestFile(this.projectID, this.fileID, str, this.url, this.required);
    }

    public CurseManifestFile withURL(String str) {
        return new CurseManifestFile(this.projectID, this.fileID, this.fileName, str, this.required);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CurseManifestFile curseManifestFile = (CurseManifestFile) obj;
        return this.projectID == curseManifestFile.projectID && this.fileID == curseManifestFile.fileID;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.projectID), Integer.valueOf(this.fileID));
    }
}
