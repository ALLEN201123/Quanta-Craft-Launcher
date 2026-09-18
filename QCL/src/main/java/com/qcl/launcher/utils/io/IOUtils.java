package com.qcl.launcher.utils.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

/* loaded from: classes2.dex */
public final class IOUtils {
    public static final int DEFAULT_BUFFER_SIZE = 8192;

    private IOUtils() {
    }

    public static byte[] readFullyWithoutClosing(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        copyTo(inputStream, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    public static ByteArrayOutputStream readFully(InputStream inputStream) throws IOException {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            copyTo(inputStream, byteArrayOutputStream);
            if (inputStream != null) {
                inputStream.close();
            }
            return byteArrayOutputStream;
        } catch (Throwable th) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    public static byte[] readFullyAsByteArray(InputStream inputStream) throws IOException {
        return readFully(inputStream).toByteArray();
    }

    public static String readFullyAsString(InputStream inputStream) throws IOException {
        return readFully(inputStream).toString();
    }

    public static String readFullyAsString(InputStream inputStream, Charset charset) throws IOException {
        return readFully(inputStream).toString(charset.name());
    }

    public static void write(String str, OutputStream outputStream) throws IOException {
        write(str.getBytes(), outputStream);
    }

    public static void write(byte[] bArr, OutputStream outputStream) throws IOException {
        copyTo(new ByteArrayInputStream(bArr), outputStream);
    }

    public static void copyTo(InputStream inputStream, OutputStream outputStream) throws IOException {
        copyTo(inputStream, outputStream, new byte[8192]);
    }

    public static void copyTo(InputStream inputStream, OutputStream outputStream, byte[] bArr) throws IOException {
        while (true) {
            int read = inputStream.read(bArr);
            if (read == -1) {
                return;
            } else {
                outputStream.write(bArr, 0, read);
            }
        }
    }
}
