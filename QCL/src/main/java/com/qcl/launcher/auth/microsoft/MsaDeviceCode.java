package com.qcl.launcher.auth.microsoft;

import android.util.Log;

import net.kdt.pojavlaunch.utils.Tools;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ★ 2026-10-06：微软账号**设备码（Device Code）登录**。
 *
 * <p><b>为什么要它</b>：原来的登录是内嵌 {@code WebView} 打开微软授权页，
 * 在部分机型 / 国内网络下 WebView 会被微软拦（提示"此浏览器不安全"或直接空白），
 * 玩家根本登不上。设备码流程不依赖 WebView：
 * <ol>
 *   <li>向微软要一个 8 位码（形如 {@code K7M9-QW2P}）和一个网址；</li>
 *   <li>提示玩家用**手机浏览器或另一台设备**打开那个网址、输入码并确认；</li>
 *   <li>启动器在后台轮询，玩家确认后立刻拿到令牌。</li>
 * </ol>
 *
 * <p><b>端点与参数照抄 FCL</b>（{@code fclcore/auth/OAuth.java}，已核对）：
 * <pre>
 *   取码  POST https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode
 *         client_id=00000000402b5328  scope=service::user.auth.xboxlive.com::MBI_SSL
 *   换令牌 POST https://login.microsoftonline.com/consumers/oauth2/v2.0/token
 *         client_id=...  grant_type=urn:ietf:params:oauth:grant-type:device_code
 *         device_code=&lt;上一步的 device_code&gt;
 * </pre>
 * 拿到 {@code refresh_token} 后交给既有的 {@link Msa}，
 * 它会继续走 XBL → XSTS → Minecraft 那条久经考验的路。
 */
public final class MsaDeviceCode {

    private static final String TAG = "MicroAuth";

    /**
     * ★★★★★ 2026-10-06 最终修正：设备码必须走**老式 Live 通道**！
     *
     * <p><b>两次踩坑记录（别再犯）</b>：
     * <ol>
     *   <li>先照抄 WebView 的 {@code 00000000402b5328} + 新式 AAD 端点
     *       {@code login.microsoftonline.com/consumers/oauth2/v2.0/devicecode}
     *       → <b>AADSTS700016</b>（"应用不存在于此目录"），取码直接失败。</li>
     *   <li>再换成第三方启动器常用的公开 AAD 客户端
     *       {@code 389b1b32-…} + scope {@code XboxLive.signin offline_access}
     *       → <b>同样 AADSTS700016</b>。</li>
     * </ol>
     *
     * <p><b>真相（OCL 时代实测记录在记忆文件 2026-10-01.md 里）</b>：
     * <pre>
     *   新式 login.microsoftonline.com/consumers/oauth2/v2.0/devicecode  → ❌ AADSTS700016
     *   老式 login.live.com/oauth20_connect.srf（设备码）                → ✅ HTTP 200 + user_code
     *   老式 login.live.com/oauth20_token.srf（轮询/刷新）               → ✅ authorization_pending
     * </pre>
     * 对照实验证明：**换任何公开的新式 ID 打新式端点都是 AADSTS700016** ——
     * 两套通道根本不通用，不是 ID 特殊。
     *
     * <p>所以本类只用老通道，参数也照老通道的规矩：
     * <ul>
     *   <li>取码要额外带 {@code response_type=device_code}</li>
     *   <li>换令牌/刷新要带 {@code redirect_url}（★ 不是 redirect_uri）</li>
     *   <li>scope 用 {@code service::user.auth.xboxlive.com::MBI_SSL}</li>
     * </ul>
     * 客户端 ID 沿用 WebView 那个（同一套 Live 应用，账号互通）。
     */
    public static final String CLIENT_ID = "00000000402b5328";
    private static final String SCOPE = "service::user.auth.xboxlive.com::MBI_SSL";

    /** 老通道：申请设备码。 */
    private static final String DEVICE_CODE_URL = "https://login.live.com/oauth20_connect.srf";
    /** 老通道：换令牌 / 刷新。 */
    private static final String TOKEN_URL = "https://login.live.com/oauth20_token.srf";
    /** 老通道换令牌要带这个（注意是 redirect_**url**，不是 redirect_uri）。 */
    private static final String REDIRECT_URL = "https://login.live.com/oauth20_desktop.srf";

    /**
     * ★★★★★ 2026-10-06：**带预填验证码**的授权页地址。
     *
     * <p>OCL 时代实测出来的写法（记录在记忆文件 2026-09-30 / 2026-10-01.md）：
     * <pre>
     *   login.live.com/oauth20_remoteconnect.srf?otc=&lt;user_code&gt;
     * </pre>
     * 打开后微软页面里的验证码**已经填好**，玩家不用手输也不用粘贴，
     * 直接点"继续/允许"就行 —— 这就是 FCL/OCL 那种"自动跳转"的体验。
     *
     * <p>注意：新式 AAD 通道不接受 {@code otc} 自动填充，
     * 但**老 Live 通道认**，这也是必须走老通道的又一个理由。
     */
    public static String buildPrefilledUrl(String userCode) {
        if (userCode == null || userCode.isEmpty()) {
            return "https://www.microsoft.com/link";
        }
        return "https://login.live.com/oauth20_remoteconnect.srf?otc=" + userCode;
    }

    /** 取码结果。 */
    public static final class DeviceCode {
        public String deviceCode;
        public String userCode;
        public String verificationUri;
        /** 两次轮询之间要等多久（毫秒）。 */
        public long intervalMs = 5000L;
        /** 这个码多久后失效（毫秒）。 */
        public long expiresInMs = 900000L;
    }

    private MsaDeviceCode() {
    }

    /**
     * 第一步：向微软申请设备码。
     *
     * @return 设备码信息（含给玩家看的 8 位码与网址）
     * @throws Exception 网络失败或微软返回错误
     */
    public static DeviceCode requestDeviceCode() throws Exception {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("client_id", CLIENT_ID);
        form.put("scope", SCOPE);
        // ★ 老通道必须带这个，否则微软不认是设备码请求
        form.put("response_type", "device_code");

        String body = postForm(DEVICE_CODE_URL, form);
        JSONObject jo = new JSONObject(body);
        DeviceCode dc = new DeviceCode();
        dc.deviceCode = jo.getString("device_code");
        dc.userCode = jo.getString("user_code");
        // 老通道返回 verification_uri，新通道有的返回 verification_url —— 两种都兼容
        dc.verificationUri = jo.optString("verification_uri",
                jo.optString("verification_url", "https://www.microsoft.com/link"));
        if (jo.has("interval")) {
            dc.intervalMs = Math.max(1000L, jo.getLong("interval") * 1000L);
        }
        if (jo.has("expires_in")) {
            dc.expiresInMs = jo.getLong("expires_in") * 1000L;
        }
        Log.i(TAG, "已申请设备码（内容不打印），有效期 " + (dc.expiresInMs / 1000) + " 秒");
        return dc;
    }

    /** 轮询结果状态。 */
    public enum PollResult { PENDING, SLOW_DOWN, SUCCESS }

    /** 轮询一次的结果。 */
    public static final class PollOutcome {
        public PollResult result;
        /** 仅在 SUCCESS 时非空：可直接交给 Msa(refresh=true, refreshToken) 的刷新令牌。 */
        public String refreshToken;
    }

    /**
     * 第二步：轮询一次，看玩家确认了没有。
     *
     * <p>职责边界：本方法**只查一次**并立刻返回，不 sleep、不循环 ——
     * 循环交给界面层去做（这样界面能随时取消、能更新提示，也不会卡住线程）。
     *
     * @param deviceCode 第一步拿到的 device_code
     * @return PENDING（还没确认）/ SLOW_DOWN（轮太快了，下次要等久点）/ SUCCESS（拿到令牌）
     */
    public static PollOutcome pollOnce(String deviceCode) throws Exception {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("client_id", CLIENT_ID);
        form.put("grant_type", "urn:ietf:params:oauth:grant-type:device_code");
        form.put("device_code", deviceCode);
        // ★ 老通道要求带 redirect_url（注意是 url，不是 uri）
        form.put("redirect_url", REDIRECT_URL);

        PollOutcome out = new PollOutcome();
        String body;
        try {
            body = postForm(TOKEN_URL, form);
        } catch (DeviceCodePendingException pending) {
            // 微软用 400 + error 字段表示"还没确认"，这是**正常流程**不是错误
            out.result = pending.slowDown ? PollResult.SLOW_DOWN : PollResult.PENDING;
            return out;
        }
        JSONObject jo = new JSONObject(body);
        out.result = PollResult.SUCCESS;
        out.refreshToken = jo.getString("refresh_token");
        Log.i(TAG, "设备码确认成功，已拿到刷新令牌（内容不打印）");
        return out;
    }

    /** 内部用：微软说"还没好"时抛它，由 pollOnce 翻译成 PENDING / SLOW_DOWN。 */
    private static final class DeviceCodePendingException extends Exception {
        final boolean slowDown;

        DeviceCodePendingException(boolean slowDown) {
            this.slowDown = slowDown;
        }
    }

    /** POST 表单；把微软的 authorization_pending / slow_down 转成内部异常。 */
    private static String postForm(String urlStr, Map<String, String> form) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : form.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(URLEncoder.encode(e.getKey(), "UTF-8"));
            sb.append('=');
            sb.append(URLEncoder.encode(e.getValue(), "UTF-8"));
        }
        byte[] bytes = sb.toString().getBytes("UTF-8");

        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("charset", "utf-8");
        conn.setRequestProperty("Content-Length", Integer.toString(bytes.length));
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestMethod("POST");
        conn.setUseCaches(false);
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.connect();
        try (OutputStream wr = conn.getOutputStream()) {
            wr.write(bytes);
        }

        int code = conn.getResponseCode();
        if (code >= 200 && code < 300) {
            return Tools.read(conn.getInputStream());
        }
        // 出错时读错误体，识别"等待中"这类正常状态
        String err = Tools.read(conn.getErrorStream());
        if (err != null) {
            if (err.contains("authorization_pending")) {
                throw new DeviceCodePendingException(false);
            }
            if (err.contains("slow_down")) {
                throw new DeviceCodePendingException(true);
            }
        }
        throw new RuntimeException(httpErrorText(code));
    }

    /** 把 HTTP 错误码翻成给玩家看的中文提示（与 Msa 里的风格一致）。 */
    private static String httpErrorText(int code) {
        if (code == 503 || code == 502 || code == 504) {
            return "登录超时：微软服务器繁忙或网络不稳定，请稍后重试。";
        }
        if (code == 401 || code == 403) {
            return "登录失败：授权被拒绝，请重新获取设备码。";
        }
        if (code == 400) {
            return "设备码已失效，请重新获取。";
        }
        return "登录失败（错误码 " + code + "），请稍后重试。";
    }
}
