package com.qcl.launcher.utils;

import java.io.IOException;

/* loaded from: classes2.dex */
public final class Hex {
    private static final char[] DIGITS_LOWER = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static byte[] decodeHex(String str) throws IOException {
        char[] charArray = str.toCharArray();
        int length = charArray.length;
        if ((length & 1) != 0) {
            throw new IOException("Odd number of characters.");
        }
        byte[] bArr = new byte[length >> 1];
        int i = 0;
        int i2 = 0;
        while (i < length) {
            int digit = toDigit(charArray[i], i) << 4;
            int i3 = i + 1;
            int digit2 = digit | toDigit(charArray[i3], i3);
            i = i3 + 1;
            bArr[i2] = (byte) (digit2 & 255);
            i2++;
        }
        return bArr;
    }

    public static String encodeHex(byte[] bArr) {
        int length = bArr.length;
        char[] cArr = new char[length << 1];
        int i = 0;
        for (int i2 = 0; i2 < length; i2++) {
            int i3 = i + 1;
            char[] cArr2 = DIGITS_LOWER;
            cArr[i] = cArr2[(bArr[i2] & 240) >>> 4];
            i = i3 + 1;
            cArr[i3] = cArr2[bArr[i2] & 15];
        }
        return new String(cArr);
    }

    private static int toDigit(char c, int i) throws IOException {
        int digit = Character.digit(c, 16);
        if (digit != -1) {
            return digit;
        }
        throw new IOException("Illegal hexadecimal character " + c + " at index " + i);
    }

    private Hex() {
    }
}
