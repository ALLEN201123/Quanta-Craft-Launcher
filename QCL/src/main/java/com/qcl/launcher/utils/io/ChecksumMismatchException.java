package com.qcl.launcher.utils.io;

import com.qcl.launcher.utils.DigestUtils;
import com.qcl.launcher.utils.Hex;
import java.io.IOException;
import java.nio.file.Path;

/* loaded from: classes2.dex */
public class ChecksumMismatchException extends IOException {
    private final String actualChecksum;
    private final String algorithm;
    private final String expectedChecksum;

    public ChecksumMismatchException(String str, String str2, String str3) {
        super("Incorrect checksum (" + str + "), expected: " + str2 + ", actual: " + str3);
        this.algorithm = str;
        this.expectedChecksum = str2;
        this.actualChecksum = str3;
    }

    public String getAlgorithm() {
        return this.algorithm;
    }

    public String getExpectedChecksum() {
        return this.expectedChecksum;
    }

    public String getActualChecksum() {
        return this.actualChecksum;
    }

    public static void verifyChecksum(Path path, String str, String str2) throws IOException {
        String encodeHex = Hex.encodeHex(DigestUtils.digest(str, path));
        if (!encodeHex.equalsIgnoreCase(str2)) {
            throw new ChecksumMismatchException(str, str2, encodeHex);
        }
    }
}
