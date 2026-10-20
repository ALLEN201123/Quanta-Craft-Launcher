package com.qcl.launcher.utils.net;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 * ★★★★★ 2026-10-20：**地区检测**（用于"仅中国大陆暂停下载/联机"）。
 *
 * <p>用户反馈：「ZL 启动器显示的是针对中国大陆法律法规，而不是外国人。
 * 你这不等于把外国人也针对了吗？」
 *
 * <p>所以合规封禁必须**按地区**执行：
 * <ul>
 *   <li>中国大陆 → 暂停下载页 / 陶瓷联机 / 安装游戏 等</li>
 *   <li>其它国家或地区 → **正常可用**</li>
 * </ul>
 *
 * <p>判断依据：**公网出口 IP 的归属国家**（不用 GPS / SIM，不需要额外权限）。
 * 依次尝试多个免费接口，任一返回可信结果即采用并**磁盘缓存 24 小时**，
 * 避免每次启动都发请求、也避免接口抖动。
 *
 * <p>★ 缓存未就绪（首次启动、或全部接口都失败）时的默认值：
 * <b>按中国大陆处理</b>（保守）—— 宁可让海外用户多看一次提示（提示里已写明
 * "如果你身处中国大陆以外却看到此提示，可能是地区检测异常，请重启"），
 * 也不要因为检测失败而在中国大陆把功能放开。
 */
public final class RegionChecker {

    /** 判定结果缓存（内存）。null = 还没算出来。 */
    private static volatile Boolean mainlandCache = null;

    /** 后台是否已经在查。 */
    private static volatile boolean fetching = false;

    private static final String PREF = "qcl_region";
    private static final String KEY_CN = "is_mainland";
    private static final String KEY_TIME = "checked_at";
    /** 缓存有效期：24 小时。 */
    private static final long TTL_MS = 24L * 60 * 60 * 1000;

    private RegionChecker() {
    }

    /**
     * 是否中国大陆。
     *
     * <p>★ 这是**同步**方法：优先读内存 → 读磁盘缓存 → 都没有就返回
     * {@code true}（保守），并在后台发起一次查询，下次调用就能拿到真实结果。
     */
    public static boolean isMainlandChina(Context context) {
        Boolean mem = mainlandCache;
        if (mem != null) {
            return mem;
        }
        // 磁盘缓存
        try {
            if (context != null) {
                SharedPreferences sp = context.getApplicationContext()
                        .getSharedPreferences(PREF, Context.MODE_PRIVATE);
                long at = sp.getLong(KEY_TIME, 0L);
                if (at > 0 && System.currentTimeMillis() - at < TTL_MS) {
                    boolean v = sp.getBoolean(KEY_CN, true);
                    mainlandCache = v;
                    return v;
                }
            }
        } catch (Throwable ignored) {
        }
        // 没缓存：后台查一次，本次先按中国大陆（保守）返回
        fetchAsync(context);
        return true;
    }

    /** 强制后台刷新（例如玩家在提示里点了"重试"）。 */
    public static void refreshAsync(Context context) {
        mainlandCache = null;
        fetchAsync(context);
    }

    private static void fetchAsync(final Context context) {
        if (fetching) {
            return;
        }
        fetching = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                Boolean result = null;
                try {
                    result = queryOnce("https://ipapi.co/json/", "\"country_code\"");
                } catch (Throwable ignored) {
                }
                if (result == null) {
                    try {
                        result = queryOnce("http://ip-api.com/json/?fields=countryCode", "\"countryCode\"");
                    } catch (Throwable ignored) {
                    }
                }
                if (result == null) {
                    try {
                        result = queryOnce("https://ipinfo.io/json", "\"country\"");
                    } catch (Throwable ignored) {
                    }
                }
                if (result != null) {
                    mainlandCache = result;
                    try {
                        if (context != null) {
                            context.getApplicationContext()
                                    .getSharedPreferences(PREF, Context.MODE_PRIVATE)
                                    .edit()
                                    .putBoolean(KEY_CN, result)
                                    .putLong(KEY_TIME, System.currentTimeMillis())
                                    .apply();
                        }
                    } catch (Throwable ignored) {
                    }
                }
                fetching = false;
            }
        }, "qcl-region").start();
    }

    /**
     * 调一次接口，解析出国家代码，判断是否 CN。
     *
     * @param fieldKey JSON 里国家代码的字段名（含引号）
     * @return true=中国大陆 / false=其它 / null=查询或解析失败
     */
    private static Boolean queryOnce(String url, String fieldKey) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "QCL-RegionCheck");
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            String body = sb.toString();
            int i = body.indexOf(fieldKey);
            if (i < 0) {
                return null;
            }
            int colon = body.indexOf(':', i);
            if (colon < 0) {
                return null;
            }
            int q1 = body.indexOf('"', colon + 1);
            if (q1 < 0) {
                return null;
            }
            int q2 = body.indexOf('"', q1 + 1);
            if (q2 < 0) {
                return null;
            }
            String cc = body.substring(q1 + 1, q2).trim().toUpperCase(Locale.ROOT);
            if (cc.length() != 2) {
                return null;
            }
            return "CN".equals(cc);
        } catch (Throwable t) {
            return null;
        } finally {
            try {
                if (conn != null) {
                    conn.disconnect();
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
