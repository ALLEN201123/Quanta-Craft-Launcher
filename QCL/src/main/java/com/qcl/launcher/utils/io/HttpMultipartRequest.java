package com.qcl.launcher.utils.io;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

/* loaded from: classes2.dex */
public class HttpMultipartRequest implements Closeable {
    private final String boundary;
    private final String endl;
    private final ByteArrayOutputStream stream;
    private final HttpURLConnection urlConnection;

    public HttpMultipartRequest(HttpURLConnection httpURLConnection) throws IOException {
        String str = "*****" + System.currentTimeMillis() + "*****";
        this.boundary = str;
        this.endl = "\r\n";
        this.urlConnection = httpURLConnection;
        httpURLConnection.setDoOutput(true);
        httpURLConnection.setUseCaches(false);
        httpURLConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + str);
        this.stream = new ByteArrayOutputStream();
    }

    private void addLine(String str) throws IOException {
        this.stream.write(str.getBytes(StandardCharsets.UTF_8));
        this.stream.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    public HttpMultipartRequest file(String str, String str2, String str3, InputStream inputStream) throws IOException {
        addLine("--" + this.boundary);
        addLine(String.format("Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"", str, str2));
        addLine("Content-Type: " + str3);
        addLine("");
        IOUtils.copyTo(inputStream, this.stream);
        addLine("");
        return this;
    }

    public HttpMultipartRequest param(String str, String str2) throws IOException {
        addLine("--" + this.boundary);
        addLine(String.format("Content-Disposition: form-data; name=\"%s\"", str));
        addLine("");
        addLine(str2);
        return this;
    }

    @Override // java.io.Closeable, java.lang.AutoCloseable
    public void close() throws IOException {
        addLine("--" + this.boundary + "--");
        this.urlConnection.setRequestProperty("Content-Length", "" + this.stream.size());
        OutputStream outputStream = this.urlConnection.getOutputStream();
        try {
            IOUtils.write(this.stream.toByteArray(), outputStream);
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (Throwable th) {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }
}
