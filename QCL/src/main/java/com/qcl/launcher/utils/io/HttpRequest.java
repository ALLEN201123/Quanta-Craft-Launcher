/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonParseException
 */
package com.qcl.launcher.utils.io;

import com.google.gson.JsonParseException;
import com.qcl.launcher.task.Schedulers;
import com.qcl.launcher.utils.Lang;
import com.qcl.launcher.utils.Pair;
import com.qcl.launcher.utils.function.ExceptionalBiConsumer;
import com.qcl.launcher.utils.gson.JsonUtils;
import com.qcl.launcher.utils.io.IOUtils;
import com.qcl.launcher.utils.io.NetworkUtils;
import com.qcl.launcher.utils.io.ResponseCodeException;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public abstract class HttpRequest {
    protected final String url;
    protected final String method;
    protected final Map<String, String> headers = new HashMap<String, String>();
    protected ExceptionalBiConsumer<URL, Integer, IOException> responseCodeTester;
    protected final Set<Integer> toleratedHttpCodes = new HashSet<Integer>();
    protected boolean ignoreHttpCode;

    private HttpRequest(String url, String method) {
        this.url = url;
        this.method = method;
    }

    public HttpRequest accept(String contentType) {
        return this.header("Accept", contentType);
    }

    public HttpRequest authorization(String token) {
        return this.header("Authorization", token);
    }

    public HttpRequest authorization(String tokenType, String tokenString) {
        return this.authorization(tokenType + " " + tokenString);
    }

    public HttpRequest authorization(Authorization authorization) {
        return this.authorization(authorization.getTokenType(), authorization.getAccessToken());
    }

    public HttpRequest header(String key, String value) {
        this.headers.put(key, value);
        return this;
    }

    public HttpRequest ignoreHttpCode() {
        this.ignoreHttpCode = true;
        return this;
    }

    public abstract String getString() throws IOException;

    public CompletableFuture<String> getStringAsync() {
        return CompletableFuture.supplyAsync(Lang.wrap(this::getString), Schedulers.io());
    }

    public <T> T getJson(Class<T> typeOfT) throws IOException, JsonParseException {
        return JsonUtils.fromNonNullJson(this.getString(), typeOfT);
    }

    public <T> T getJson(Type type) throws IOException, JsonParseException {
        return JsonUtils.fromNonNullJson(this.getString(), type);
    }

    public <T> CompletableFuture<T> getJsonAsync(Class<T> typeOfT) {
        return this.getStringAsync().thenApplyAsync(jsonString -> JsonUtils.fromNonNullJson(jsonString, typeOfT));
    }

    public <T> CompletableFuture<T> getJsonAsync(Type type) {
        return this.getStringAsync().thenApplyAsync(jsonString -> JsonUtils.fromNonNullJson(jsonString, type));
    }

    public HttpRequest filter(ExceptionalBiConsumer<URL, Integer, IOException> responseCodeTester) {
        this.responseCodeTester = responseCodeTester;
        return this;
    }

    public HttpRequest ignoreHttpErrorCode(int code) {
        this.toleratedHttpCodes.add(code);
        return this;
    }

    public HttpURLConnection createConnection() throws IOException {
        return createConnection(this.url);
    }

    /**
     * ★ 1.2.7：按指定地址建连接。
     * 为什么需要重载：{@code this.url} 是 **final**（1.2.7 想改它做镜像重试时编译不过），
     * 所以走镜像时用这个重载把镜像地址传进来，而不是去改字段。
     */
    public HttpURLConnection createConnection(String urlString) throws IOException {
        HttpURLConnection con = NetworkUtils.createHttpConnection(new URL(urlString));
        con.setRequestMethod(this.method);
        for (Map.Entry<String, String> entry : this.headers.entrySet()) {
            con.setRequestProperty(entry.getKey(), entry.getValue());
        }
        // ★ 1.2.8：镜像那次用**更短的超时** —— 万一镜像自己卡住，7 秒就放弃并回退官方，
        //   不会让用户盯着「加载中」干等（默认 15 秒在这里太久了）。
        if (urlString != null && urlString.startsWith(MirrorUtils.MCIM)) {
            con.setConnectTimeout(5000);
            con.setReadTimeout(7000);
        }
        return con;
    }

    public static HttpGetRequest GET(String url) {
        return new HttpGetRequest(url);
    }

    @SafeVarargs
    public static HttpGetRequest GET(String url, Pair<String, String> ... query) {
        return HttpRequest.GET(NetworkUtils.withQuery(url, Lang.mapOf(query)));
    }

    public static HttpPostRequest POST(String url) throws MalformedURLException {
        return new HttpPostRequest(url);
    }

    public static interface Authorization {
        public String getTokenType();

        public String getAccessToken();
    }

    public static class HttpGetRequest
    extends HttpRequest {
        public HttpGetRequest(String url) {
            super(url, "GET");
        }

        @Override
        public String getString() throws IOException {
            // ★★★ 1.2.7：国内先走 MCIM 镜像（api.modrinth.com / api.curseforge.com），
            //   镜像不通**自动回退官方**（路径结构完全一致，只是换域名）。
            //   官方 API 在国内时通时不通，这是「列表/详情页加载半天出不来」的主因之一。
            String original = this.url;
            String mirrored = MirrorUtils.rewriteApi(original);
            if (mirrored != null && !mirrored.equals(original)) {
                try {
                    HttpURLConnection mcon = NetworkUtils.resolveConnection(this.createConnection(mirrored));
                    return IOUtils.readFullyAsString(mcon.getInputStream(), StandardCharsets.UTF_8);
                }
                catch (IOException e) {
                    // 镜像这次不行 → 回官方重试一次
                }
            }
            HttpURLConnection con = NetworkUtils.resolveConnection(this.createConnection());
            return IOUtils.readFullyAsString(con.getInputStream(), StandardCharsets.UTF_8);
        }
    }

    public static final class HttpPostRequest
    extends HttpRequest {
        private byte[] bytes;

        public HttpPostRequest(String url) {
            super(url, "POST");
        }

        public HttpPostRequest contentType(String contentType) {
            this.headers.put("Content-Type", contentType);
            return this;
        }

        public HttpPostRequest json(Object payload) throws JsonParseException {
            return this.string(payload instanceof String ? (String)payload : JsonUtils.GSON.toJson(payload), "application/json");
        }

        public HttpPostRequest form(Map<String, String> params) {
            return this.string(NetworkUtils.withQuery("", params), "application/x-www-form-urlencoded");
        }

        @SafeVarargs
        public final HttpPostRequest form(Pair<String, String> ... params) {
            return this.form(Lang.mapOf(params));
        }

        public HttpPostRequest string(String payload, String contentType) {
            this.bytes = payload.getBytes(StandardCharsets.UTF_8);
            this.header("Content-Length", "" + this.bytes.length);
            this.contentType(contentType + "; charset=utf-8");
            return this;
        }

        @Override
        public String getString() throws IOException {
            HttpURLConnection con = this.createConnection();
            con.setDoOutput(true);
            try (OutputStream os = con.getOutputStream();){
                os.write(this.bytes);
            }
            if (this.responseCodeTester != null) {
                this.responseCodeTester.accept(new URL(this.url), con.getResponseCode());
            } else if (con.getResponseCode() / 100 != 2 && !this.ignoreHttpCode && !this.toleratedHttpCodes.contains(con.getResponseCode())) {
                String data = NetworkUtils.readData(con);
                throw new ResponseCodeException(new URL(this.url), con.getResponseCode(), data);
            }
            return NetworkUtils.readData(con);
        }
    }
}

