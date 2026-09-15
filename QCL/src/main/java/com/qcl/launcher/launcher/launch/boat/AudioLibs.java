package com.qcl.launcher.launcher.launch.boat;

import com.qcl.launcher.manifest.AppManifest;

/**
 * 远古版本（LWJGL2 时代）音频依赖。
 *
 * <p>Minecraft 1.6 之前使用 paulscode SoundSystem 播放音频，运行时需要：
 * <ul>
 *   <li>soundsystem —— SoundSystem 本体</li>
 *   <li>libraryjavasound —— 纯 Java 后端（javax.sound），**不需要 OpenAL 原生库**</li>
 *   <li>librarylwjglopenal —— OpenAL 后端（设备有 libopenal.so.1 时才可用）</li>
 *   <li>codecjorbis / codecwav —— ogg / wav 解码</li>
 *   <li>jinput / jutils —— SoundSystem 的可选依赖（缺失时 SoundSystem 初始化会抛 NoClassDefFoundError）</li>
 * </ul>
 *
 * <p>上游 HMCL-PE 的 Boat 后端 classpath 只拼了 lwjgl / lwjgl_util，
 * 因此远古版本进游戏后**完全无声**（日志里能看到 SoundSystem 的 ClassNotFound）。
 * 这里把这些 jar 补进 classpath。
 *
 * <p>jar 位于 {@code assets/app_runtime/boat/lwjgl-2/audio/}，
 * 通过 AssetsUtils 释放到 {@code BOAT_LIB_DIR/lwjgl-2/audio/}。
 */
public final class AudioLibs {

    /** jar 文件名，顺序即 classpath 顺序。libraryjavasound 放在 OpenAL 之后，优先走 OpenAL，失败再退回纯 Java。 */
    private static final String[] JARS = {
            "soundsystem-20120107.jar",
            "librarylwjglopenal-20100824.jar",
            "libraryjavasound-20101123.jar",
            "codecjorbis-20101023.jar",
            "codecwav-20101023.jar",
            "jinput-2.0.5.jar",
            "jutils-1.0.0.jar"
    };

    private AudioLibs() {
    }

    /** 是否把音频 jar 加进 classpath。可通过 -Dqcl.disableAudio=true 临时关掉用于排查。 */
    private static boolean enabled() {
        return !"true".equalsIgnoreCase(System.getProperty("qcl.disableAudio"));
    }

    /**
     * 返回以 ':' 结尾的 classpath 片段（后面直接拼 version.getClassPath(...)），
     * 关闭或目录缺失时返回空串，保证向后兼容。
     */
    public static String classPath() {
        if (!enabled()) return "";
        StringBuilder sb = new StringBuilder();
        for (String jar : JARS) {
            String path = AppManifest.BOAT_LIB_DIR + "/lwjgl-2/audio/" + jar;
            if (!new java.io.File(path).isFile()) continue;
            sb.append(path).append(':');
        }
        return sb.toString();
    }
}
