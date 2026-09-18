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

    public String[] getJVMArguments(GameLaunchSetting gameLaunchSetting) {
        String str;
        StringBuilder sb = new StringBuilder();
        Arguments arguments = this.arguments;
        if (arguments == null || arguments.jvm == null) {
            return new String[0];
        }
        for (Object obj : this.arguments.jvm) {
            if (obj instanceof String) {
                String str2 = (String) obj;
                if (!str2.startsWith("-Djava.library.path") && !str2.startsWith("-cp") && !str2.startsWith("${classpath}")) {
                    sb.append(obj.toString()).append(" ");
                }
            }
        }
        String str3 = "";
        boolean z = false;
        int i = 0;
        for (int i2 = 0; i2 < sb.length(); i2++) {
            if (!z) {
                if (sb.charAt(i2) != '$') {
                    str3 = str3 + sb.charAt(i2);
                } else {
                    int i3 = i2 + 1;
                    if (i3 >= sb.length() || sb.charAt(i3) != '{') {
                        str3 = str3 + sb.charAt(i2);
                    } else {
                        z = true;
                        i = i2;
                    }
                }
            } else if (sb.charAt(i2) == '}') {
                String substring = sb.substring(i + 2, i2);
                if (substring.equals("version_name")) {
                    str = this.id;
                } else if (substring.equals("launcher_name")) {
                    str = LAUNCHER_NAME;
                } else if (substring.equals("launcher_version")) {
                    str = LAUNCHER_VERSION;
                } else if (substring.equals("version_type")) {
                    str = LAUNCHER_NAME;
                } else if (substring.equals("assets_index_name")) {
                    AssetsIndex assetsIndex = this.assetIndex;
                    if (assetsIndex != null) {
                        str = assetsIndex.id;
                    } else {
                        str = this.assets;
                    }
                } else if (substring.equals("game_directory")) {
                    str = gameLaunchSetting.game_directory;
                } else if (substring.equals("assets_root") || substring.equals("game_assets")) {
                    str = gameLaunchSetting.gameFileDirectory + "/assets";
                } else if (substring.equals("user_properties")) {
                    str = "{}";
                } else if (substring.equals("auth_player_name")) {
                    str = gameLaunchSetting.account.auth_player_name;
                } else if (substring.equals("auth_session")) {
                    str = gameLaunchSetting.account.auth_session;
                } else if (substring.equals("auth_uuid")) {
                    str = gameLaunchSetting.account.auth_uuid;
                } else if (substring.equals("auth_access_token")) {
                    str = gameLaunchSetting.account.auth_access_token;
                } else if (substring.equals("user_type")) {
                    str = gameLaunchSetting.account.user_type;
                } else if (substring.equals("primary_jar_name")) {
                    str = new File(gameLaunchSetting.currentVersion).getName() + ".jar";
                } else if (substring.equals("library_directory")) {
                    str = gameLaunchSetting.gameFileDirectory + "/libraries";
                } else {
                    str = substring.equals("classpath_separator") ? ":" : "";
                }
                str3 = str3 + str;
                z = false;
            }
        }
        return str3.split(" ");
    }

    public String[] getMinecraftArguments(GameLaunchSetting gameLaunchSetting, boolean z) {
        Arguments arguments;
        String str;
        StringBuilder sb = new StringBuilder();
        if (z) {
            for (Object obj : this.arguments.game) {
                if (obj instanceof String) {
                    sb.append(obj.toString()).append(" ");
                }
            }
        } else {
            sb = new StringBuilder(this.minecraftArguments);
        }
        boolean z2 = false;
        int i = 0;
        String str2 = "";
        for (int i2 = 0; i2 < sb.length(); i2++) {
            if (!z2) {
                if (sb.charAt(i2) != '$') {
                    str2 = str2 + sb.charAt(i2);
                } else {
                    int i3 = i2 + 1;
                    if (i3 >= sb.length() || sb.charAt(i3) != '{') {
                        str2 = str2 + sb.charAt(i2);
                    } else {
                        z2 = true;
                        i = i2;
                    }
                }
            } else if (sb.charAt(i2) == '}') {
                String substring = sb.substring(i + 2, i2);
                if (substring.equals("version_name")) {
                    str = this.id;
                } else if (substring.equals("launcher_name")) {
                    str = LAUNCHER_NAME;
                } else if (substring.equals("launcher_version")) {
                    str = LAUNCHER_VERSION;
                } else if (substring.equals("version_type")) {
                    str = LAUNCHER_NAME;
                } else if (substring.equals("assets_index_name")) {
                    AssetsIndex assetsIndex = this.assetIndex;
                    if (assetsIndex != null) {
                        str = assetsIndex.id;
                    } else {
                        str = this.assets;
                    }
                } else if (substring.equals("game_directory")) {
                    str = gameLaunchSetting.game_directory;
                } else if (substring.equals("assets_root") || substring.equals("game_assets")) {
                    str = gameLaunchSetting.gameFileDirectory + "/assets";
                } else if (substring.equals("user_properties")) {
                    str = "{}";
                } else if (substring.equals("auth_player_name")) {
                    str = gameLaunchSetting.account.auth_player_name;
                } else if (substring.equals("auth_session")) {
                    str = gameLaunchSetting.account.auth_session;
                } else if (substring.equals("auth_uuid")) {
                    str = gameLaunchSetting.account.auth_uuid;
                } else if (substring.equals("auth_access_token")) {
                    str = gameLaunchSetting.account.auth_access_token;
                } else if (substring.equals("user_type")) {
                    str = gameLaunchSetting.account.user_type;
                } else if (substring.equals("primary_jar_name")) {
                    str = new File(gameLaunchSetting.currentVersion).getName() + ".jar";
                } else if (substring.equals("library_directory")) {
                    str = gameLaunchSetting.gameFileDirectory + "/libraries";
                } else {
                    str = substring.equals("classpath_separator") ? ":" : "";
                }
                str2 = str2 + str;
                z2 = false;
            }
        }
        if (!z && (arguments = this.arguments) != null && arguments.game != null) {
            for (Object obj2 : this.arguments.game) {
                if (obj2 instanceof String) {
                    str2 = str2 + " " + obj2.toString();
                }
            }
        }
        return str2.split(" ");
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
