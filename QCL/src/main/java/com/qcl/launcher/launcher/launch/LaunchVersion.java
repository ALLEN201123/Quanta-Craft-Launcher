package com.qcl.launcher.launcher.launch;

import android.util.ArrayMap;
import android.util.Log;
import com.google.gson.Gson;
import com.qcl.launcher.launcher.setting.game.GameLaunchSetting;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* loaded from: classes2.dex */
public class LaunchVersion {
    public static String LAUNCHER_NAME = "QCL";
    public static String LAUNCHER_VERSION = "";
    private Map<String, String> SHAs;
    public Arguments arguments;
    public AssetsIndex assetIndex;
    public String assets;
    public HashMap<String, Download> downloads;
    public String id;
    public String inheritsFrom;
    public Library[] libraries;
    public String mainClass;
    public String minecraftArguments;
    public String minecraftPath;
    public int minimumLauncherVersion;
    public String releaseTime;
    public String time;
    public String type;

    public static void setLauncherIdentity(String str, String str2) {
        if (str != null && !str.isEmpty()) {
            LAUNCHER_NAME = str;
        }
        if (str2 != null) {
            LAUNCHER_VERSION = str2;
        }
    }

    /* loaded from: classes2.dex */
    public class AssetsIndex {
        public String id;
        public String sha1;
        public int size;
        public int totalSize;
        public String url;

        public AssetsIndex() {
        }
    }

    /* loaded from: classes2.dex */
    public class Download {
        public String path;
        public String sha1;
        public int size;
        public String url;

        public Download() {
        }
    }

    /* loaded from: classes2.dex */
    public class Library {
        public HashMap<String, Download> downloads;
        public String name;

        public Library() {
        }
    }

    /* loaded from: classes2.dex */
    public class Arguments {
        private Object[] game;
        private Object[] jvm;

        public Arguments() {
        }
    }

    public static LaunchVersion fromDirectory(File file) {
        try {
            LaunchVersion launchVersion = (LaunchVersion) new Gson().fromJson(new String(readAllBytes(new File(file, file.getName() + ".json")), "UTF-8"), LaunchVersion.class);
            if (new File(file, file.getName() + ".jar").exists()) {
                launchVersion.minecraftPath = new File(file, file.getName() + ".jar").getAbsolutePath();
            } else {
                launchVersion.minecraftPath = "";
            }
            String str = launchVersion.inheritsFrom;
            if (str == null || str.equals("")) {
                return launchVersion;
            }
            LaunchVersion fromDirectory = fromDirectory(new File(file.getParentFile(), launchVersion.inheritsFrom));
            AssetsIndex assetsIndex = launchVersion.assetIndex;
            if (assetsIndex != null) {
                fromDirectory.assetIndex = assetsIndex;
            }
            String str2 = launchVersion.assets;
            if (str2 != null && !str2.equals("")) {
                fromDirectory.assets = launchVersion.assets;
            }
            HashMap<String, Download> hashMap = launchVersion.downloads;
            if (hashMap != null && !hashMap.isEmpty()) {
                if (fromDirectory.downloads == null) {
                    fromDirectory.downloads = new HashMap<>();
                }
                for (Map.Entry<String, Download> entry : launchVersion.downloads.entrySet()) {
                    fromDirectory.downloads.put(entry.getKey(), entry.getValue());
                }
            }
            Library[] libraryArr = launchVersion.libraries;
            if (libraryArr != null && libraryArr.length > 0) {
                Library[] libraryArr2 = new Library[fromDirectory.libraries.length + libraryArr.length];
                int i = 0;
                for (Library library : libraryArr) {
                    libraryArr2[i] = library;
                    i++;
                }
                for (Library library2 : fromDirectory.libraries) {
                    libraryArr2[i] = library2;
                    i++;
                }
                fromDirectory.libraries = libraryArr2;
            }
            String str3 = launchVersion.mainClass;
            if (str3 != null && !str3.equals("")) {
                fromDirectory.mainClass = launchVersion.mainClass;
            }
            String str4 = launchVersion.minecraftArguments;
            if (str4 != null && !str4.equals("")) {
                fromDirectory.minecraftArguments = launchVersion.minecraftArguments;
            }
            int i2 = launchVersion.minimumLauncherVersion;
            if (i2 > fromDirectory.minimumLauncherVersion) {
                fromDirectory.minimumLauncherVersion = i2;
            }
            String str5 = launchVersion.releaseTime;
            if (str5 != null && !str5.equals("")) {
                fromDirectory.releaseTime = launchVersion.releaseTime;
            }
            String str6 = launchVersion.time;
            if (str6 != null && !str6.equals("")) {
                fromDirectory.time = launchVersion.time;
            }
            String str7 = launchVersion.type;
            if (str7 != null && !str7.equals("")) {
                fromDirectory.type = launchVersion.type;
            }
            String str8 = launchVersion.minecraftPath;
            if (str8 != null && !str8.equals("")) {
                fromDirectory.minecraftPath = launchVersion.minecraftPath;
            }
            if (fromDirectory.minimumLauncherVersion >= 21 && launchVersion.arguments.game != null && launchVersion.arguments.game.length > 0) {
                Object[] objArr = new Object[fromDirectory.arguments.game.length + launchVersion.arguments.game.length];
                int i3 = 0;
                for (Object obj : launchVersion.arguments.game) {
                    objArr[i3] = obj;
                    i3++;
                }
                for (Object obj2 : fromDirectory.arguments.game) {
                    objArr[i3] = obj2;
                    i3++;
                }
                fromDirectory.arguments.game = objArr;
            }
            return fromDirectory;
        } catch (UnsupportedEncodingException unused) {
            return null;
        }
    }

    private static byte[] readAllBytes(File file) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream((int) Math.max(1024L, file.length()));
                byte[] bArr = new byte[8192];
                while (true) {
                    int read = fileInputStream.read(bArr);
                    if (read <= 0) {
                        byte[] byteArray = byteArrayOutputStream.toByteArray();
                        fileInputStream.close();
                        return byteArray;
                    }
                    byteArrayOutputStream.write(bArr, 0, read);
                }
            } finally {
            }
        } catch (Throwable th) {
            th.printStackTrace();
            return new byte[0];
        }
    }

    public String getClassPath(String str, boolean z, boolean z2) {
        String str2 = str + "/libraries/";
        int i = 0;
        String str3 = "";
        for (Library library : this.libraries) {
            if (library.name != null && !library.name.equals("") && !library.name.contains("org.lwjgl") && !library.name.contains("natives") && (!z2 || !library.name.contains("java-objc-bridge"))) {
                Log.e("boat", library.name);
                String[] split = library.name.split(":");
                String str4 = split[0];
                String str5 = split[1];
                String str6 = split[2];
                String str7 = (((((("" + str2) + str4.replaceAll("\\.", "/")) + "/") + str5) + "/") + str6) + "/" + str5 + "-" + str6 + ".jar";
                Log.e("路径", str7);
                if (new File(str7).exists()) {
                    if (i > 0) {
                        str3 = str3 + ":";
                    }
                    str3 = str3 + str7;
                    i++;
                }
            }
        }
        String str8 = i > 0 ? ":" : "";
        if (z) {
            return str3 + str8 + this.minecraftPath;
        }
        return this.minecraftPath + str8 + str3;
    }

    /**
     * 把一个参数模板里的 ${...} 占位符替换掉。
     * ★★★ 1.4.9 整合包崩溃修复的核心：原实现是「先按空格 join 全部参数、再 split(" ")」，
     * 会把**含空格的单条参数**撕成几片。Fabulously Optimized 这类 Fabric 整合包的
     * arguments.jvm 里有 "-DFabricMcEmu= net.minecraft.client.main.Main "（值本身带空格，
     * 用来告诉 Fabric 要替换掉哪个主类），撕开后 net.minecraft.client.main.Main 变成一个
     * 裸的独立 argv 项，正好落在 JVM 判定「第一个非选项参数 = 主类」的位置之前 →
     * 后面的 -cp / 主类 全被当成它的命令行参数，classpath 没生效 →
     * "Could not find or load main class"（类名为空）。
     * 所以这里改成**按数组元素逐个渲染**，一个 json 元素 = 一个 argv 项，内部空格原样保留。
     *
     * @return 渲染后的参数；渲染结果为空串时返回 null（表示该参数作废）
     */
    private String renderArgument(String str, GameLaunchSetting s) {
        StringBuilder sb = new StringBuilder(str);
        boolean z = false;
        int i = 0;
        String str2 = "";
        for (int i2 = 0; i2 < sb.length(); i2++) {
            if (!z) {
                if (sb.charAt(i2) != '$') {
                    str2 = str2 + sb.charAt(i2);
                } else {
                    int i3 = i2 + 1;
                    if (i3 >= sb.length() || sb.charAt(i3) != '{') {
                        str2 = str2 + sb.charAt(i2);
                    } else {
                        z = true;
                        i = i2;
                    }
                }
            } else if (sb.charAt(i2) == '}') {
                str2 = str2 + renderPlaceholder(sb.substring(i + 2, i2), s);
                z = false;
            }
        }
        return str2.isEmpty() ? null : str2;
    }

    /** 占位符取值的统一入口；未识别的占位符返回空串（保持旧行为）。 */
    private String renderPlaceholder(String substring, GameLaunchSetting s) {
        if (substring.equals("version_name")) {
            return this.id == null ? "" : this.id;
        }
        if (substring.equals("launcher_name")) {
            return LAUNCHER_NAME;
        }
        if (substring.equals("launcher_version")) {
            return LAUNCHER_VERSION == null ? "" : LAUNCHER_VERSION;
        }
        if (substring.equals("version_type")) {
            return LAUNCHER_NAME;
        }
        if (substring.equals("assets_index_name")) {
            AssetsIndex assetsIndex = this.assetIndex;
            if (assetsIndex != null) {
                return assetsIndex.id == null ? "" : assetsIndex.id;
            }
            return this.assets == null ? "" : this.assets;
        }
        if (s == null) {
            return substring.equals("classpath_separator") ? ":" : "";
        }
        if (substring.equals("game_directory")) {
            return s.game_directory;
        }
        if (substring.equals("assets_root") || substring.equals("game_assets")) {
            return s.gameFileDirectory + "/assets";
        }
        if (substring.equals("user_properties")) {
            return "{}";
        }
        if (substring.equals("auth_player_name")) {
            return s.account.auth_player_name;
        }
        if (substring.equals("auth_session")) {
            return s.account.auth_session;
        }
        if (substring.equals("auth_uuid")) {
            return s.account.auth_uuid;
        }
        if (substring.equals("auth_access_token")) {
            return s.account.auth_access_token;
        }
        if (substring.equals("user_type")) {
            return s.account.user_type;
        }
        if (substring.equals("primary_jar_name")) {
            return new File(s.currentVersion).getName() + ".jar";
        }
        if (substring.equals("library_directory")) {
            return s.gameFileDirectory + "/libraries";
        }
        return substring.equals("classpath_separator") ? ":" : "";
    }

    public String[] getJVMArguments(GameLaunchSetting gameLaunchSetting) {
        ArrayList<String> out = new ArrayList<>();
        Arguments arguments = this.arguments;
        if (arguments == null || arguments.jvm == null) {
            return new String[0];
        }
        for (Object obj : arguments.jvm) {
            // 带 rules 的条目是对象而非字符串 —— 原实现也是直接忽略（移动端没有 features 上下文）
            if (!(obj instanceof String)) {
                continue;
            }
            String raw = (String) obj;
            // ★ 这三个由启动器自己提供，绝不能从 json 里取：
            //   -cp / ${classpath}      → PojavLauncher 用自己拼好的 classPath（含 lwjgl 桥接 jar）
            //   -Djava.library.path     → PojavLauncher 用算好的 natives 目录
            if (raw.startsWith("-Djava.library.path") || raw.startsWith("-cp") || raw.startsWith("${classpath}")) {
                continue;
            }
            String rendered = renderArgument(raw, gameLaunchSetting);
            if (rendered != null) {
                out.add(rendered);
            }
        }
        return out.toArray(new String[0]);
    }

    public String[] getMinecraftArguments(GameLaunchSetting gameLaunchSetting, boolean z) {
        ArrayList<String> out = new ArrayList<>();
        Arguments arguments = this.arguments;
        if (z) {
            // 高版本（1.13+）：arguments.game 是数组，逐元素渲染，元素内部空格原样保留
            if (arguments == null || arguments.game == null) {
                return new String[0];
            }
            for (Object obj : arguments.game) {
                // 带 rules 的条目是对象而非字符串 —— 原实现也是直接忽略（移动端无 features 上下文）
                if (!(obj instanceof String)) {
                    continue;
                }
                String rendered = renderArgument((String) obj, gameLaunchSetting);
                if (rendered != null) {
                    out.add(rendered);
                }
            }
            return out.toArray(new String[0]);
        }
        // 低版本：只有一个 minecraftArguments 整串（本身就按空格分隔），仍走逐元素渲染
        String mcArgs = this.minecraftArguments == null ? "" : this.minecraftArguments;
        String renderedMc = renderArgument(mcArgs, gameLaunchSetting);
        if (renderedMc != null) {
            for (String part : renderedMc.split(" ")) {
                if (!part.isEmpty()) {
                    out.add(part);
                }
            }
        }
        // 旧行为：低版本 json 里 arguments.game 的内容也会被追加（不加空格）
        if (arguments != null && arguments.game != null) {
            for (Object obj2 : arguments.game) {
                if (obj2 instanceof String) {
                    String rendered2 = renderArgument((String) obj2, gameLaunchSetting);
                    if (rendered2 != null) {
                        out.add(rendered2);
                    }
                }
            }
        }
        return out.toArray(new String[0]);
    }

    public List<String> getLibraries() {
        ArrayList arrayList = new ArrayList();
        for (Library library : this.libraries) {
            if (library.name != null && !library.name.equals("") && !library.name.contains("net.java.jinput") && !library.name.contains("org.lwjgl") && !library.name.contains("platform")) {
                arrayList.add(parseLibNameToPath(library.name));
            }
        }
        return arrayList;
    }

    public String getSHA1(String str) {
        if (this.SHAs == null) {
            this.SHAs = new ArrayMap();
            for (Library library : this.libraries) {
                if (library.name != null && !library.name.equals("") && !library.name.contains("net.java.jinput") && !library.name.contains("org.lwjgl") && !library.name.contains("platform")) {
                    try {
                        this.SHAs.put(parseLibNameToPath(library.name), library.downloads.get("artifact").sha1);
                    } catch (Exception unused) {
                    }
                }
            }
        }
        return this.SHAs.get(str);
    }

    public String parseLibNameToPath(String str) {
        String[] split = str.split(":");
        return split[0].replace(".", "/") + "/" + split[1] + "/" + split[2] + "/" + split[1] + "-" + split[2] + ".jar";
    }
}
