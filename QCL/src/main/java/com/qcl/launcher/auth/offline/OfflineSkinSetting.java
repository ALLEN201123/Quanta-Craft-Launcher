package com.qcl.launcher.auth.offline;

import android.content.Context;
import com.qcl.launcher.auth.yggdrasil.TextureModel;

/* loaded from: classes2.dex */
public class OfflineSkinSetting implements Cloneable {
    public String capePath;
    public TextureModel model;
    public String server;
    public String skinPath;
    public int type;

    public OfflineSkinSetting(Context context) {
        this(0, TextureModel.STEVE, "", "", "");
    }

    public OfflineSkinSetting(int i, TextureModel textureModel, String str, String str2, String str3) {
        this.type = i;
        this.model = textureModel;
        this.skinPath = str;
        this.capePath = str2;
        this.server = str3;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
