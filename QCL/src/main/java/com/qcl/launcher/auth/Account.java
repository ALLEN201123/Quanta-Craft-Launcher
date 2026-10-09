package com.qcl.launcher.auth;

import com.qcl.launcher.auth.offline.OfflineSkinSetting;

/* loaded from: classes2.dex */
public class Account {
    public String auth_access_token;
    public String auth_client_token;
    public String auth_player_name;
    public String auth_session;
    public String auth_uuid;
    public String email;
    public String loginServer;
    public int loginType;
    public OfflineSkinSetting offlineSkinSetting;
    public String password;
    public String refresh_token;
    public String texture;
    /**
     * ★ 1.5.0 新增：披风纹理（base64，格式与 {@link #texture} 一致）。
     *
     * <p>为什么要加：微软账号的披风此前**根本没被拉下来过** ——
     * {@code Msa.getTextures()} 里那两行 {@code TextureType.CAPE} 是被注释掉的，
     * 而 {@code Account} 也**没有字段**能存它 ⇒ 主界面 {@code showModel(..., null, ...)}
     * 永远传 null ⇒ 人物背后永远没有披风（用户实测「主界面和对话框里都看不到披风」）。
     *
     * <p>存量账号没有这个字段时为 null，Gson 会当空处理，**不需要迁移**。
     */
    public String capeTexture;
    public String user_type;

    public Account(int i, String str, String str2, String str3, String str4, String str5, String str6, String str7, String str8, String str9, String str10, String str11) {
        this.loginType = i;
        this.email = str;
        this.password = str2;
        this.user_type = str3;
        this.auth_session = str4;
        this.auth_player_name = str5;
        this.auth_uuid = str6;
        this.auth_access_token = str7;
        this.auth_client_token = str8;
        this.refresh_token = str9;
        this.loginServer = str10;
        this.texture = str11;
    }

    public void refresh(Account account) {
        this.loginType = account.loginType;
        this.email = account.email;
        this.password = account.password;
        this.user_type = account.user_type;
        this.auth_session = account.auth_session;
        this.auth_player_name = account.auth_player_name;
        this.auth_uuid = account.auth_uuid;
        this.auth_access_token = account.auth_access_token;
        this.auth_client_token = account.auth_client_token;
        this.refresh_token = account.refresh_token;
        this.loginServer = account.loginServer;
        this.texture = account.texture;
        // ★ 1.5.0：拷贝构造**必须带上披风**，否则 refresh/copy 后披风丢失。
        this.capeTexture = account.capeTexture;
        this.offlineSkinSetting = account.offlineSkinSetting;
    }

    /**
     * ★ 1.5.0：从 {@link com.qcl.launcher.auth.yggdrasil.Texture} URL 下载一张纹理并转 base64。
     *
     * <p>微软登录要下皮肤与披风两张，原来的写法是在每个登录对话框里各内联一遍
     * "http→https 替换 + 打开连接 + decodeStream"。这里抽出来共用一份，避免四处重复。
     *
     * @param texture 可为 null（该账号没有这类纹理）⇒ 返回 null
     * @return base64 字符串；下载/解码失败返回 null（**绝不抛异常打断登录**）
     */
    public static String downloadTextureAsBase64(com.qcl.launcher.auth.yggdrasil.Texture texture) {
        if (texture == null) {
            return null;
        }
        java.io.InputStream is = null;
        java.net.HttpURLConnection con = null;
        try {
            String u = texture.getUrl();
            if (u == null || u.trim().isEmpty()) {
                return null;
            }
            if (!u.startsWith("https")) {
                u = u.replaceFirst("http", "https");
            }
            con = (java.net.HttpURLConnection) new java.net.URL(u).openConnection();
            con.setDoInput(true);
            con.setConnectTimeout(15000);
            con.setReadTimeout(30000);
            con.connect();
            is = con.getInputStream();
            android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeStream(is);
            if (bmp == null) {
                return null;
            }
            return com.qcl.launcher.skin.utils.Avatar.bitmapToString(bmp);
        } catch (Throwable ignored) {
            // ★ 披风/皮肤拿不到就当没有，**不能让登录流程失败**
            return null;
        } finally {
            try { if (is != null) is.close(); } catch (Throwable ignored) {}
            try { if (con != null) con.disconnect(); } catch (Throwable ignored) {}
        }
    }
}
