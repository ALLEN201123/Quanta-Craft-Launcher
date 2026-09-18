package com.qcl.launcher.auth.offline;

import com.google.gson.annotations.SerializedName;
import com.qcl.launcher.auth.yggdrasil.TextureModel;
import com.qcl.launcher.utils.string.StringUtils;

/* loaded from: classes2.dex */
public class SkinJson {
    private final String cape;
    private final String elytra;
    private final String skin;

    @SerializedName(alternate = {"skins"}, value = "textures")
    private final TextureJson textures;
    private final String username;

    public SkinJson(String str, String str2, String str3, String str4, TextureJson textureJson) {
        this.username = str;
        this.skin = str2;
        this.cape = str3;
        this.elytra = str4;
        this.textures = textureJson;
    }

    public boolean hasSkin() {
        return StringUtils.isNotBlank(this.username);
    }

    public TextureModel getModel() {
        TextureJson textureJson = this.textures;
        if (textureJson != null && textureJson.slim != null) {
            return TextureModel.ALEX;
        }
        TextureJson textureJson2 = this.textures;
        if (textureJson2 == null || textureJson2.defaultSkin == null) {
            return null;
        }
        return TextureModel.STEVE;
    }

    public String getAlexModelHash() {
        TextureJson textureJson = this.textures;
        if (textureJson == null || textureJson.slim == null) {
            return null;
        }
        return this.textures.slim;
    }

    public String getSteveModelHash() {
        TextureJson textureJson = this.textures;
        if (textureJson == null || textureJson.defaultSkin == null) {
            return this.skin;
        }
        return this.textures.defaultSkin;
    }

    public String getHash() {
        TextureModel model = getModel();
        if (model == TextureModel.ALEX) {
            return getAlexModelHash();
        }
        if (model == TextureModel.STEVE) {
            return getSteveModelHash();
        }
        return null;
    }

    public String getCapeHash() {
        TextureJson textureJson = this.textures;
        if (textureJson == null || textureJson.cape == null) {
            return this.cape;
        }
        return this.textures.cape;
    }

    /* loaded from: classes2.dex */
    public static class TextureJson {
        private final String cape;

        @SerializedName("default")
        private final String defaultSkin;
        private final String elytra;
        private final String slim;

        public TextureJson(String str, String str2, String str3, String str4) {
            this.defaultSkin = str;
            this.slim = str2;
            this.cape = str3;
            this.elytra = str4;
        }
    }
}
