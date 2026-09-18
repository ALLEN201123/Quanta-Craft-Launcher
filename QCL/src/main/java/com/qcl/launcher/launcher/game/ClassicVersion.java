package com.qcl.launcher.launcher.game;

import java.io.File;
import java.util.Arrays;
import java.util.Date;

/* loaded from: classes2.dex */
public class ClassicVersion extends Version {
    public ClassicVersion() {
        super(true, "Classic", null, null, "${auth_player_name} ${auth_session} --workDir ${game_directory}", null, "net.minecraft.client.Minecraft", null, null, null, null, null, null, Arrays.asList(new ClassicLibrary("lwjgl"), new ClassicLibrary("jinput"), new ClassicLibrary("lwjgl_util")), null, null, null, ReleaseType.UNKNOWN, new Date(), new Date(), 0, false, false, null);
    }

    /* loaded from: classes2.dex */
    private static class ClassicLibrary extends Library {
        public ClassicLibrary(String str) {
            super(new Artifact("", "", ""), null, new LibrariesDownloadInfo(new LibraryDownloadInfo("bin/" + str + ".jar"), null), null, null, null, null, null, null);
        }
    }

    public static boolean hasClassicVersion(File file) {
        File file2 = new File(file, "bin");
        return file2.exists() && new File(file2, "lwjgl.jar").exists() && new File(file2, "jinput.jar").exists() && new File(file2, "lwjgl_util.jar").exists();
    }
}
