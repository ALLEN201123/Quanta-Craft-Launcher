package com.qcl.launcher.launcher.game;

import com.google.gson.JsonParseException;
import com.qcl.launcher.utils.DigestUtils;
import com.qcl.launcher.utils.Hex;
import com.qcl.launcher.utils.gson.tools.Validation;
import com.qcl.launcher.utils.string.StringUtils;
import java.io.IOException;
import java.nio.file.Path;

/* loaded from: classes2.dex */
public final class AssetObject implements Validation {
    private final String hash;
    private final long size;

    public AssetObject() {
        this("", 0L);
    }

    public AssetObject(String str, long j) {
        this.hash = str;
        this.size = j;
    }

    public String getHash() {
        return this.hash;
    }

    public long getSize() {
        return this.size;
    }

    public String getLocation() {
        return this.hash.substring(0, 2) + "/" + this.hash;
    }

    @Override // com.qcl.launcher.utils.gson.tools.Validation
    public void validate() throws JsonParseException {
        if (StringUtils.isBlank(this.hash) || this.hash.length() < 2) {
            throw new JsonParseException("AssetObject hash cannot be blank.");
        }
    }

    public boolean validateChecksum(Path path, boolean z) throws IOException {
        return this.hash == null ? z : Hex.encodeHex(DigestUtils.digest("SHA-1", path)).equalsIgnoreCase(this.hash);
    }
}
