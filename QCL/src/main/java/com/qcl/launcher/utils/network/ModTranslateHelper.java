package com.qcl.launcher.utils.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.qcl.launcher.R;
import com.qcl.launcher.utils.io.NetworkUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 1.4.0 新增：模组介绍「一键翻译」。
 *
 * <p>用有道翻译的公开接口。实测（2026-10，PowerShell）：
 * <ul>
 *   <li>{@code https://fanyi.youdao.com/translate?&doctype=json&type=AUTO&i=...} —— 域名可访问，
 *       但该旧接口现在返回的是官网 SPA 的 HTML（Content-Type: text/html），不再是 JSON；</li>
 *   <li>{@code https://aidemo.youdao.com/trans?q=...&from=auto&to=zh-CHS} —— 返回 application/json，
 *       结构为 {@code {"errorCode":0,"query":"...","translation":["译文"]}}。</li>
 * </ul>
 * 因此本类以 aidemo 接口为主，同时保留对旧 fanyi 接口 {@code translateResult} 结构的兼容解析
 * （万一官方恢复旧接口也能用）。
 *
 * <p>长文本按句子边界分片（每片 ≤ {@link #MAX_CHUNK} 字符），逐片翻译后拼接；
 * 结果做内存缓存，同一文本只翻一次。回调切回主线程。
 */
public final class ModTranslateHelper {

    /** 单次请求的最大字符数。 */
    private static final int MAX_CHUNK = 1500;

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static final Map<String, String> CACHE = new HashMap<>();

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    private ModTranslateHelper() {
    }

    public interface Callback {
        void onSuccess(String result);

        void onFail(String reason);
    }

    /** 翻译入口：命中缓存直接回调，否则子线程分片翻译，回调切主线程。 */
    public static void translate(Context context, String text, Callback cb) {
        if (text == null || text.trim().isEmpty()) {
            MAIN.post(() -> cb.onFail(context.getString(R.string.translate_failed, "empty")));
            return;
        }
        final String source = text;
        String cached = CACHE.get(source);
        if (cached != null) {
            MAIN.post(() -> cb.onSuccess(cached));
            return;
        }
        new Thread(() -> {
            try {
                StringBuilder result = new StringBuilder();
                for (String chunk : split(source)) {
                    result.append(requestChunk(chunk));
                }
                String translated = result.toString();
                synchronized (CACHE) {
                    CACHE.put(source, translated);
                }
                MAIN.post(() -> cb.onSuccess(translated));
            } catch (Throwable t) {
                String reason = t.getMessage() == null ? t.toString() : t.getMessage();
                MAIN.post(() -> cb.onFail(reason));
            }
        }).start();
    }

    /** 按长度和句子边界分片。 */
    static List<String> split(String text) {
        List<String> chunks = new ArrayList<>();
        if (text.length() <= MAX_CHUNK) {
            chunks.add(text);
            return chunks;
        }
        String[] sentences = text.split("(?<=[.!?\\n。！？；;])");
        StringBuilder current = new StringBuilder();
        for (String sentence : sentences) {
            if (sentence.isEmpty()) {
                continue;
            }
            if (current.length() + sentence.length() > MAX_CHUNK && current.length() > 0) {
                chunks.add(current.toString());
                current.setLength(0);
            }
            if (sentence.length() > MAX_CHUNK) {
                if (current.length() > 0) {
                    chunks.add(current.toString());
                    current.setLength(0);
                }
                for (int i = 0; i < sentence.length(); i += MAX_CHUNK) {
                    chunks.add(sentence.substring(i, Math.min(sentence.length(), i + MAX_CHUNK)));
                }
            } else {
                current.append(sentence);
            }
        }
        if (current.length() > 0) {
            chunks.add(current.toString());
        }
        return chunks;
    }

    /** 单次请求：先试 aidemo 接口，失败再试旧 fanyi 接口。 */
    private static String requestChunk(String chunk) throws IOException {
        String encoded = NetworkUtils.encodeURL(chunk);

        String aidemo = httpGet("https://aidemo.youdao.com/trans?q=" + encoded + "&from=auto&to=zh-CHS");
        String parsed = parseAidemo(aidemo);
        if (parsed != null) {
            return parsed;
        }

        String legacy = httpGet("https://fanyi.youdao.com/translate?doctype=json&type=AUTO&i=" + encoded);
        parsed = parseLegacy(legacy);
        if (parsed != null) {
            return parsed;
        }
        throw new IOException("翻译接口无有效返回");
    }

    private static String httpGet(String url) throws IOException {
        Request request = new Request.Builder().url(url).build();
        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("HTTP " + response.code());
            }
            return response.body().string();
        }
    }

    /** 解析 aidemo 接口：{"errorCode":0,"translation":["..."]}。 */
    private static String parseAidemo(String body) {
        if (body == null || !body.trim().startsWith("{")) {
            return null;
        }
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonArray translation = root.getAsJsonArray("translation");
            if (translation == null || translation.size() == 0) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            for (JsonElement element : translation) {
                sb.append(element.getAsString());
            }
            return sb.length() == 0 ? null : sb.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    /** 兼容解析旧 fanyi 接口：{"translateResult":[[{"tgt":"..."}]]}。 */
    private static String parseLegacy(String body) {
        if (body == null || !body.trim().startsWith("{")) {
            return null;
        }
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonArray translateResult = root.getAsJsonArray("translateResult");
            if (translateResult == null) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            for (JsonElement segment : translateResult) {
                JsonArray items = segment.getAsJsonArray();
                for (JsonElement item : items) {
                    JsonObject object = item.getAsJsonObject();
                    if (object.has("tgt")) {
                        sb.append(object.get("tgt").getAsString());
                    }
                }
            }
            return sb.length() == 0 ? null : sb.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 「一键翻译 / 显示原文」切换按钮。翻译中显示「翻译中…」并禁用按钮；
     * 失败 Toast 原因并恢复原文；切回原文瞬时完成。
     */
    public static class Toggle {

        private final Context context;
        private final TextView target;
        private final Button button;
        private final String original;

        private String translated;
        private boolean showingTranslated;
        private boolean loading;

        public Toggle(Context context, TextView target, Button button) {
            this.context = context;
            this.target = target;
            this.button = button;
            this.original = target.getText() == null ? "" : target.getText().toString();
            if (button == null) {
                return;
            }
            if (original.trim().isEmpty()) {
                button.setVisibility(View.GONE);
                return;
            }
            button.setVisibility(View.VISIBLE);
            button.setText(R.string.translate_button_one_click);
            button.setOnClickListener(v -> onClick());
        }

        private void onClick() {
            if (loading) {
                return;
            }
            if (showingTranslated) {
                showOriginal();
                return;
            }
            if (translated != null) {
                showTranslated();
                return;
            }
            loading = true;
            button.setEnabled(false);
            button.setText(R.string.translate_loading);
            ModTranslateHelper.translate(context, original, new Callback() {
                @Override
                public void onSuccess(String result) {
                    loading = false;
                    button.setEnabled(true);
                    translated = result;
                    showTranslated();
                }

                @Override
                public void onFail(String reason) {
                    loading = false;
                    button.setEnabled(true);
                    button.setText(R.string.translate_button_one_click);
                    Toast.makeText(context,
                            context.getString(R.string.translate_failed, reason), Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void showTranslated() {
            target.setText(translated);
            button.setText(R.string.translate_button_original);
            showingTranslated = true;
        }

        private void showOriginal() {
            target.setText(original);
            button.setText(R.string.translate_button_one_click);
            showingTranslated = false;
        }
    }
}