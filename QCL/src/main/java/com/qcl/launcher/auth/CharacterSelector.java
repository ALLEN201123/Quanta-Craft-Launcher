package com.qcl.launcher.auth;

import com.qcl.launcher.auth.yggdrasil.GameProfile;
import com.qcl.launcher.auth.yggdrasil.YggdrasilService;
import java.util.List;

/* loaded from: classes2.dex */
public interface CharacterSelector {
    GameProfile select(YggdrasilService yggdrasilService, List<GameProfile> list) throws NoSelectedCharacterException;
}
