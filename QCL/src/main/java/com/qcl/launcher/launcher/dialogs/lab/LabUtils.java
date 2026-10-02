package com.qcl.launcher.launcher.dialogs.lab;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;

import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.utils.string.StringUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 实验室相关工具方法集合。
 */
public final class LabUtils {

    private LabUtils() {
    }

    /**
     * 获取当前游戏目录（.minecraft）。
     * 优先从当前版本的路径反推（<游戏目录>/versions/<版本>），否则退回默认目录。
     */
    public static File getGameDir(MainActivity activity) {
        try {
            if (activity != null && activity.publicGameSetting != null
                    && !StringUtils.isBlank(activity.publicGameSetting.currentVersion)) {
                File versionDir = new File(activity.publicGameSetting.currentVersion);
                File versionsDir = versionDir.getParentFile();
                if (versionsDir != null && "versions".equals(versionsDir.getName()) && versionsDir.getParentFile() != null) {
                    return versionsDir.getParentFile();
                }
                // 若传入的已经是游戏目录本身
                if (versionDir.isDirectory() && new File(versionDir, "versions").isDirectory()) {
                    return versionDir;
                }
            }
        } catch (Throwable ignored) {
        }
        return new File(AppManifest.DEFAULT_GAME_DIR);
    }

    /** 投影目录 <游戏目录>/schematics，确保存在。 */
    public static File getSchematicsDir(MainActivity activity) {
        File dir = new File(getGameDir(activity), "schematics");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /** 数据包目录 <游戏目录>/datapacks，确保存在。 */
    public static File getDatapacksDir(MainActivity activity) {
        File dir = new File(getGameDir(activity), "datapacks");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /** 将弹窗设置为较大的面板（高度约屏幕 85%），内容自行滚动。 */
    public static void setupDialogWindow(Dialog dialog) {
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        DisplayMetrics dm = dialog.getContext().getResources().getDisplayMetrics();
        int width = (int) Math.min(dm.widthPixels * 0.92f, dm.density * 420f);
        window.setLayout(width, (int) (dm.heightPixels * 0.85f));
    }

    /** 将弹窗设置为自适应高度，宽度受限。 */
    public static void setupDialogWindowWrap(Dialog dialog) {
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        DisplayMetrics dm = dialog.getContext().getResources().getDisplayMetrics();
        int width = (int) Math.min(dm.widthPixels * 0.92f, dm.density * 420f);
        window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public static void toast(Context context, String message) {
        if (context == null || message == null) {
            return;
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void copyToClipboard(Context context, String text) {
        try {
            ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (manager != null) {
                manager.setPrimaryClip(ClipData.newPlainText("qcl_lab", text));
            }
        } catch (Throwable ignored) {
        }
    }

    /** 人类可读的文件大小。 */
    public static String humanSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.US, "%.1f KB", bytes / 1024.0f);
        }
        if (bytes < 1024L * 1024L * 1024L) {
            return String.format(Locale.US, "%.1f MB", bytes / (1024.0f * 1024.0f));
        }
        return String.format(Locale.US, "%.1f GB", bytes / (1024.0f * 1024.0f * 1024.0f));
    }

    public static String formatTime(long millis) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(millis));
        } catch (Throwable ignored) {
            return String.valueOf(millis);
        }
    }

    /** 数据包命名空间：只保留小写字母、数字、下划线、横线和点。 */
    public static String sanitizeNamespace(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = Character.toLowerCase(raw.charAt(i));
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** 通用文件名过滤：去掉路径分隔符等非法字符。 */
    public static String sanitizeFileName(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("\\", "_").replace("/", "_").replace(":", "_")
                .replace("*", "_").replace("?", "_").replace("\"", "_")
                .replace("<", "_").replace(">", "_").replace("|", "_").trim();
    }
}