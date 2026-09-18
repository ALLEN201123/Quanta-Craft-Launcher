package com.qcl.launcher.launcher.launch;

import android.os.Build;
import android.os.FileObserver;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kdt.pojavlaunch.utils.Tools;
import org.lwjgl.glfw.CallbackBridge;

/* loaded from: classes2.dex */
public class MCOptionUtils {
    private static FileObserver fileObserver;
    private static final HashMap<String, String> parameterMap = new HashMap<>();
    private static final ArrayList<WeakReference<MCOptionListener>> optionListeners = new ArrayList<>();

    /* loaded from: classes2.dex */
    public interface MCOptionListener {
        void onOptionChanged();
    }

    public static void load(String str) {
        if (fileObserver == null) {
            setupFileObserver(str);
        }
        parameterMap.clear();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(str + "/options.txt"));
            while (true) {
                String readLine = bufferedReader.readLine();
                if (readLine != null) {
                    int indexOf = readLine.indexOf(58);
                    if (indexOf >= 0) {
                        parameterMap.put(readLine.substring(0, indexOf), readLine.substring(indexOf + 1));
                    }
                } else {
                    bufferedReader.close();
                    return;
                }
            }
        } catch (IOException unused) {
        }
    }

    public static void set(String str, String str2) {
        parameterMap.put(str, str2);
    }

    public static void set(String str, List<String> list) {
        parameterMap.put(str, list.toString());
    }

    public static String get(String str) {
        return parameterMap.get(str);
    }

    public static List<String> getAsList(String str) {
        String str2 = get(str);
        if (str2 == null) {
            return new ArrayList();
        }
        String replace = str2.replace("[", "").replace("]", "");
        return replace.isEmpty() ? new ArrayList() : Arrays.asList(replace.split(","));
    }

    public static void save(String str) {
        StringBuilder sb = new StringBuilder();
        for (String str2 : parameterMap.keySet()) {
            sb.append(str2).append(':').append(parameterMap.get(str2)).append('\n');
        }
        try {
            Tools.write(str + "/options.txt", sb.toString());
        } catch (IOException unused) {
        }
    }

    public static int getMcScale(String str) {
        load(str);
        String str2 = get("guiScale");
        int parseInt = str2 == null ? 0 : Integer.parseInt(str2);
        int max = Math.max(Math.min(CallbackBridge.windowWidth / 320, CallbackBridge.windowHeight / 240), 1);
        return (max < parseInt || parseInt == 0) ? max : parseInt;
    }

    public static void applyGameLanguage(String str, String str2) {
        if (!new File(str, "options.txt").exists()) {
            set("lang", str2);
            save(str);
        } else {
            load(str);
            set("lang", str2);
            save(str);
        }
    }

    public static void fixLang(String str, String str2) {
        load(str);
        String str3 = get("lang");
        if (str3 == null || !str3.contains("_") || isOlderThan1_1(str2)) {
            return;
        }
        boolean isOlderThan1_11 = isOlderThan1_11(str2);
        String[] split = str3.split("_", 2);
        if (split.length != 2) {
            return;
        }
        String str4 = split[0] + "_" + (isOlderThan1_11 ? split[1].toUpperCase() : split[1].toLowerCase());
        if (str4.equals(str3)) {
            return;
        }
        set("lang", str4);
        save(str);
    }

    private static boolean isOlderThan1_1(String str) {
        int[] parseVersion = parseVersion(str);
        if (parseVersion == null) {
            return false;
        }
        return parseVersion[0] < 1 || (parseVersion[0] == 1 && parseVersion[1] == 0);
    }

    private static boolean isOlderThan1_11(String str) {
        int[] parseVersion = parseVersion(str);
        if (parseVersion == null) {
            return false;
        }
        return parseVersion[0] != 1 ? parseVersion[0] < 1 : parseVersion[1] < 11;
    }

    private static int[] parseVersion(String str) {
        if (str == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("(\\d+)\\.(\\d+)").matcher(str);
        if (!matcher.find()) {
            return null;
        }
        try {
            return new int[]{Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))};
        } catch (NumberFormatException unused) {
            return null;
        }
    }

    private static void setupFileObserver(final String str) {
        int i = 2;
        if (Build.VERSION.SDK_INT >= 29) {
            fileObserver = new FileObserver(new File(str + "/options.txt"), i) { // from class: com.qcl.launcher.launcher.launch.MCOptionUtils.1
                @Override // android.os.FileObserver
                public void onEvent(int i2, String str2) {
                    MCOptionUtils.load(str);
                    MCOptionUtils.notifyListeners();
                }
            };
        } else {
            fileObserver = new FileObserver(str + "/options.txt", i) { // from class: com.qcl.launcher.launcher.launch.MCOptionUtils.2
                @Override // android.os.FileObserver
                public void onEvent(int i2, String str2) {
                    MCOptionUtils.load(str);
                    MCOptionUtils.notifyListeners();
                }
            };
        }
        fileObserver.startWatching();
    }

    public static void notifyListeners() {
        Iterator<WeakReference<MCOptionListener>> it = optionListeners.iterator();
        while (it.hasNext()) {
            MCOptionListener mCOptionListener = it.next().get();
            if (mCOptionListener != null) {
                mCOptionListener.onOptionChanged();
            }
        }
    }

    public static void addMCOptionListener(MCOptionListener mCOptionListener) {
        optionListeners.add(new WeakReference<>(mCOptionListener));
    }

    public static void removeMCOptionListener(MCOptionListener mCOptionListener) {
        Iterator<WeakReference<MCOptionListener>> it = optionListeners.iterator();
        while (it.hasNext()) {
            WeakReference<MCOptionListener> next = it.next();
            MCOptionListener mCOptionListener2 = next.get();
            if (mCOptionListener2 != null && mCOptionListener2 == mCOptionListener) {
                optionListeners.remove(next);
                return;
            }
        }
    }
}
