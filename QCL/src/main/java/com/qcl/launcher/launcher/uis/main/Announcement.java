package com.qcl.launcher.launcher.uis.main;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

/**
 * ★ 1.5.0：主界面公告数据模型 —— 整体照搬 FCL 的
 * {@code FCL/src/main/java/com/tungsten/fcl/ui/main/Announcement.java}。
 *
 * <p>设计要点（与 FCL 保持一致，便于以后同步上游公告）：
 * <ul>
 *   <li>公告体是 JSON：{@code { "announcements": [ {...}, {...} ] }}</li>
 *   <li>单条公告字段：id / significant / outdated / minVersion / maxVersion /
 *       specificLang / title[] / date / content[]</li>
 *   <li>title 与 content 都是「多语言块数组」，每块 = {@code {"lang": "zh_CN", "text": "..."}}，
 *       匹配规则：先用设备的 {@code zh_CN} 形态，再用语言前缀（{@code zh}），最后退回第一条。</li>
 *   <li>「隐藏」= 把 id 写进 {@code SharedPreferences("launcher").ignore_announcement}，
 *       下次 {@link #shouldDisplay} 直接返回 false。</li>
 * </ul>
 *
 * <p>★ QCL 与 FCL 的差异：QCL 出网不一定稳，所以公告**优先读 assets 内置**
 * （{@code assets/announcement.json}），读取失败时静默不显示 —— 绝不因为公告拉不到
 * 就影响主界面可用性。
 */
public class Announcement {

    /** ★ FCL 同款：官方公告源（GitHub raw）。留作以后接远程用。 */
    public static final String ANNOUNCEMENT_URL =
            "https://raw.githubusercontent.com/FCL-Team/FCL-Repo/refs/heads/main/res/announcement_v2.txt";
    /** ★ FCL 同款：国内备用源（Gitee raw）。 */
    public static final String ANNOUNCEMENT_URL_CN =
            "https://gitee.com/fcl-team/FCL-Repo/raw/main/res/announcement_v2.txt";

    /** ★ QCL 自己内置的公告文件（放 assets 根目录）。 */
    public static final String ASSET_NAME = "announcement.json";

    /** 隐藏过的公告 id 存这里（FCL 用的同一个 key 名）。 */
    private static final String PREF_NAME = "launcher";
    private static final String PREF_KEY_IGNORE = "ignore_announcement";
    /**
     * ★ 1.5.0：隐藏时**同时**记住当时那条公告的「内容指纹」。
     *
     * <p>为什么需要：只记 id 的话，只要 id 不变，官方把公告内容改了，玩家也**永远看不到**。
     * <p>指纹变了 ⇒ 官方改过这条公告 ⇒ 视为**新公告**，隐藏失效、公告重新出现；
     * 玩家想再藏一次，需要重新点「隐藏」—— 这正是用户要求的行为。
     */
    private static final String PREF_KEY_IGNORE_SIG = "ignore_announcement_sig";

    private final int id;
    private final boolean significant;
    private final boolean outdated;
    private final int minVersion;
    private final int maxVersion;
    private final ArrayList<String> specificLang;
    private final ArrayList<Content> title;
    private final String date;
    private final ArrayList<Content> content;

    public Announcement(int id, boolean significant, boolean outdated,
                        int minVersion, int maxVersion, ArrayList<String> specificLang,
                        ArrayList<Content> title, String date, ArrayList<Content> content) {
        this.id = id;
        this.significant = significant;
        this.outdated = outdated;
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
        this.specificLang = specificLang;
        this.title = title;
        this.date = date;
        this.content = content;
    }

    public int getId() {
        return id;
    }

    public boolean isSignificant() {
        return significant;
    }

    public String getDate() {
        return date == null ? "" : date;
    }

    /** 按当前系统语言挑一条标题；挑不到就退回第一条；都没有就空串。 */
    public String getDisplayTitle(Context context) {
        return pick(title, context);
    }

    /** 按当前系统语言挑一条正文。 */
    public String getDisplayContent(Context context) {
        return pick(content, context);
    }

    private static String pick(ArrayList<Content> list, Context context) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        String full = currentLangTag(context);          // zh_CN
        String prefix = full.contains("_") ? full.substring(0, full.indexOf('_')) : full; // zh
        String fallback = null;
        for (Content c : list) {
            if (c == null || TextUtils.isEmpty(c.lang)) {
                if (fallback == null) {
                    fallback = c == null ? null : c.text;
                }
                continue;
            }
            if (c.lang.equalsIgnoreCase(full)) {
                return c.text == null ? "" : c.text;
            }
            if (c.lang.equalsIgnoreCase(prefix) && fallback == null) {
                fallback = c.text;
            }
        }
        if (fallback != null) {
            return fallback;
        }
        return list.get(0).text == null ? "" : list.get(0).text;
    }

    /** 设备语言标签，形如 {@code zh_CN} / {@code en_US}（FCL 用同一套匹配口径）。 */
    private static String currentLangTag(Context context) {
        Locale locale;
        try {
            locale = context.getResources().getConfiguration().locale;
        } catch (Throwable t) {
            locale = Locale.getDefault();
        }
        if (locale == null) {
            locale = Locale.getDefault();
        }
        String lang = locale.getLanguage() == null ? "" : locale.getLanguage();
        String country = locale.getCountry() == null ? "" : locale.getCountry();
        // 中文特别处理：TW/HK/MO 归到 zh_TW（与 FCL 语言包命名对齐）
        if ("zh".equalsIgnoreCase(lang)) {
            if ("TW".equalsIgnoreCase(country) || "HK".equalsIgnoreCase(country) || "MO".equalsIgnoreCase(country)) {
                return "zh_TW";
            }
            return "zh_CN";
        }
        if (country.isEmpty()) {
            return lang;
        }
        return lang + "_" + country;
    }

    /**
     * 这条公告**现在该不该显示**。条件与 FCL 一致：
     * <ol>
     *   <li>{@code outdated == false}</li>
     *   <li>{@code specificLang} 为空，或包含当前语言（完整 tag 或语言前缀）</li>
     *   <li>当前 versionCode 落在 [minVersion, maxVersion] 区间</li>
     *   <li>没被玩家隐藏过（SharedPreferences 里的 ignore_announcement != id）</li>
     * </ol>
     */
    public boolean shouldDisplay(Context context, int versionCode) {
        if (outdated) {
            return false;
        }
        if (specificLang != null && !specificLang.isEmpty()) {
            String full = currentLangTag(context);
            String prefix = full.contains("_") ? full.substring(0, full.indexOf('_')) : full;
            boolean hit = false;
            for (String s : specificLang) {
                if (s == null) {
                    continue;
                }
                if (s.equalsIgnoreCase(full) || s.equalsIgnoreCase(prefix)) {
                    hit = true;
                    break;
                }
            }
            if (!hit) {
                return false;
            }
        }
        if (versionCode < minVersion || versionCode > maxVersion) {
            return false;
        }
        if (getIgnoredId(context) != id) {
            return true;
        }
        // ★★★ 1.5.0：id 相同但**内容指纹不同** ⇒ 官方更新过这条公告 ⇒ 隐藏失效、重新出现。
        //   （老版本只比 id，会导致"公告更新了玩家却永远看不到"。）
        return !String.valueOf(contentSignature()).equals(getIgnoredSignature(context));
    }

    /**
     * 本条公告的**内容指纹**（语言无关）：把 date + 所有 title/content 文本块按顺序混进一个 hash。
     * 官方改了任何一句话 ⇒ 指纹变 ⇒ 玩家之前点的「隐藏」作废。
     */
    private int contentSignature() {
        int h = date == null ? 0 : date.hashCode();
        if (title != null) {
            for (Content c : title) {
                if (c != null && c.getText() != null) {
                    h = h * 31 + c.getText().hashCode();
                }
            }
        }
        if (content != null) {
            for (Content c : content) {
                if (c != null && c.getText() != null) {
                    h = h * 31 + c.getText().hashCode();
                }
            }
        }
        return h;
    }

    /** 玩家点「隐藏」→ 记住这条公告的 id **和内容指纹**，以后不再显示（直到官方更新它）。 */
    public void hide(Context context) {
        prefs(context).edit()
                .putInt(PREF_KEY_IGNORE, id)
                .putString(PREF_KEY_IGNORE_SIG, String.valueOf(contentSignature()))
                .apply();
    }

    private static int getIgnoredId(Context context) {
        return prefs(context).getInt(PREF_KEY_IGNORE, -1);
    }

    private static String getIgnoredSignature(Context context) {
        return prefs(context).getString(PREF_KEY_IGNORE_SIG, "");
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 从一段 JSON 文本解析出公告列表。格式不合法 / 空内容 → 返回空列表（**不抛异常**）。
     */
    public static ArrayList<Announcement> parseList(String json) {
        ArrayList<Announcement> result = new ArrayList<>();
        if (TextUtils.isEmpty(json)) {
            return result;
        }
        try {
            JSONObject root = new JSONObject(json);
            JSONArray arr = root.optJSONArray("announcements");
            if (arr == null) {
                return result;
            }
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) {
                    continue;
                }
                Announcement a = parseOne(o);
                if (a != null) {
                    result.add(a);
                }
            }
        } catch (Throwable ignored) {
            // 公告体坏了不能影响主界面
        }
        return result;
    }

    private static Announcement parseOne(JSONObject o) {
        try {
            int id = o.optInt("id", 0);
            boolean significant = o.optBoolean("significant", false);
            boolean outdated = o.optBoolean("outdated", false);
            int minVersion = o.optInt("minVersion", 0);
            int maxVersion = o.optInt("maxVersion", Integer.MAX_VALUE);
            String date = o.optString("date", "");

            ArrayList<String> langs = new ArrayList<>();
            JSONArray langArr = o.optJSONArray("specificLang");
            if (langArr != null) {
                for (int i = 0; i < langArr.length(); i++) {
                    String s = langArr.optString(i, null);
                    if (s != null) {
                        langs.add(s);
                    }
                }
            }

            ArrayList<Content> title = parseContents(o.optJSONArray("title"));
            ArrayList<Content> content = parseContents(o.optJSONArray("content"));
            if (title.isEmpty() && content.isEmpty()) {
                return null;
            }
            return new Announcement(id, significant, outdated, minVersion, maxVersion,
                    langs, title, date, content);
        } catch (Throwable t) {
            return null;
        }
    }

    private static ArrayList<Content> parseContents(JSONArray arr) {
        ArrayList<Content> list = new ArrayList<>();
        if (arr == null) {
            return list;
        }
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) {
                continue;
            }
            String lang = o.optString("lang", "");
            String text = o.optString("text", "");
            list.add(new Content(lang, text));
        }
        return list;
    }

    /** 一条「某语言的文本块」，FCL 里就叫 Content。 */
    public static final class Content {
        private final String lang;
        private final String text;

        public Content(String lang, String text) {
            this.lang = lang;
            this.text = text;
        }

        public String getLang() {
            return lang;
        }

        public String getText() {
            return text;
        }
    }
}
