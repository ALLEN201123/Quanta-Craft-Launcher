package com.qcl.launcher.auth.offline;

import com.qcl.launcher.auth.yggdrasil.TextureModel;

/* loaded from: classes2.dex */
public class LoadedSkin {
    private final Texture cape;
    private final TextureModel model;
    private final Texture skin;

    public LoadedSkin(TextureModel textureModel, Texture texture, Texture texture2) {
        this.model = textureModel;
        this.skin = texture;
        this.cape = texture2;
    }

    public TextureModel getModel() {
        return this.model;
    }

    public Texture getSkin() {
        return this.skin;
    }

    public Texture getCape() {
        return this.cape;
    }
}
