package com.qcl.launcher.launcher.setting;

import android.content.Context;
import android.system.Os;
import android.util.Log;
import com.qcl.launcher.utils.file.FileStringUtils;
import com.qcl.launcher.utils.io.FileUtils;
import com.qcl.launcher.utils.io.IOUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Objects;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;

/* loaded from: classes2.dex */
public final class RuntimeUtils {
    private static final String TAG = "RuntimeUtils";

    /* loaded from: classes2.dex */
    public interface InstallListener {
        default void onProgress(int i) {
        }

        void onStage(String str);

        void onUpdate(String str);
    }

    private RuntimeUtils() {
    }

    public static boolean isLatest(Context context, String str, String str2) throws IOException {
        File file = new File(str + "/version");
        InputStream open = context.getAssets().open(str2 + "/version");
        if (open == null) {
            return true;
        }
        open.close();
        if (!file.exists()) {
            return false;
        }
        String trim = readAssetText(context, str2 + "/version").trim();
        String trim2 = FileStringUtils.getStringFromFile(file.getAbsolutePath()).trim();
        if (trim2.isEmpty()) {
            return false;
        }
        try {
            return Long.parseLong(trim2) == Long.parseLong(trim);
        } catch (NumberFormatException unused) {
            return false;
        }
    }

    public static void install(Context context, String str, String str2) throws IOException {
        install(context, str, str2, null);
    }

    public static void install(Context context, String str, String str2, InstallListener installListener) throws IOException {
        FileUtils.deleteDirectory(new File(str));
        File file = new File(str);
        if (!file.mkdirs() && !file.isDirectory()) {
            throw new IOException("Cannot create runtime dir: " + str);
        }
        copyAssets(context, str2, str, installListener);
    }

    public static void installJava(Context context, String str, String str2, String str3, InstallListener installListener) throws IOException {
        FileUtils.deleteDirectory(new File(str));
        File file = new File(str);
        if (!file.mkdirs() && !file.isDirectory()) {
            throw new IOException("Cannot create java dir: " + str);
        }
        String str4 = str2 + "/universal.tar.xz";
        String str5 = "bin-" + str3 + ".tar.xz";
        String readAssetText = readAssetText(context, str2 + "/version");
        int countTarXzEntries = countTarXzEntries(context.getAssets().open(str4)) + countTarXzEntries(context.getAssets().open(str2 + "/" + str5));
        if (countTarXzEntries <= 0) {
            countTarXzEntries = 1;
        }
        int[] iArr = {0};
        if (installListener != null) {
            installListener.onStage("universal");
        }
        uncompressTarXz(context.getAssets().open(str4), file, installListener, iArr, countTarXzEntries);
        if (installListener != null) {
            installListener.onStage("bin");
        }
        uncompressTarXz(context.getAssets().open(str2 + "/" + str5), file, installListener, iArr, countTarXzEntries);
        FileStringUtils.writeFile(str + "/version", readAssetText);
        if (installListener != null) {
            installListener.onStage("patching");
        }
        patchJava(context, str);
    }

    public static void uncompressTarXz(InputStream inputStream, File file, InstallListener installListener) throws IOException {
        uncompressTarXz(inputStream, file, installListener, null, 0);
    }

    public static void uncompressTarXz(InputStream inputStream, File file, InstallListener installListener, int[] iArr, int i) throws IOException {
        file.mkdirs();
        TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(new XZCompressorInputStream(inputStream));
        while (true) {
            try {
                TarArchiveEntry nextTarEntry = tarArchiveInputStream.getNextTarEntry();
                if (nextTarEntry != null) {
                    if (installListener != null && !nextTarEntry.isDirectory()) {
                        installListener.onUpdate(nextTarEntry.getName());
                    }
                    if (iArr != null && installListener != null) {
                        iArr[0] = iArr[0] + 1;
                        installListener.onProgress(Math.min(100, (int) ((iArr[0] * 100) / i)));
                    }
                    File file2 = new File(file, nextTarEntry.getName());
                    if (nextTarEntry.isSymbolicLink()) {
                        ((File) Objects.requireNonNull(file2.getParentFile())).mkdirs();
                        try {
                            Os.symlink(nextTarEntry.getLinkName().replace("..", file.getAbsolutePath()), file2.getAbsolutePath());
                        } catch (Throwable th) {
                            Log.w("RuntimeUtils", "symlink failed: " + nextTarEntry.getName() + " -> " + th.getMessage());
                        }
                    } else if (nextTarEntry.isDirectory()) {
                        file2.mkdirs();
                        file2.setExecutable(true);
                    } else if (!file2.exists() || file2.length() != nextTarEntry.getSize()) {
                        ((File) Objects.requireNonNull(file2.getParentFile())).mkdirs();
                        file2.createNewFile();
                        FileOutputStream fileOutputStream = new FileOutputStream(file2);
                        try {
                            byte[] bArr = new byte[65536];
                            while (true) {
                                int read = tarArchiveInputStream.read(bArr);
                                if (read == -1) {
                                    break;
                                } else {
                                    fileOutputStream.write(bArr, 0, read);
                                }
                            }
                            fileOutputStream.getFD().sync();
                            fileOutputStream.close();
                        } finally {
                        }
                    }
                } else {
                    tarArchiveInputStream.close();
                    return;
                }
            } catch (Throwable th2) {
                try {
                    tarArchiveInputStream.close();
                } catch (Throwable th3) {
                    th2.addSuppressed(th3);
                }
                throw th2;
            }
        }
    }

    public static int countTarXzEntries(InputStream inputStream) {
        int i = 0;
        try {
            TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(new XZCompressorInputStream(inputStream));
            while (tarArchiveInputStream.getNextTarEntry() != null) {
                try {
                    i++;
                } finally {
                }
            }
            tarArchiveInputStream.close();
        } catch (Throwable th) {
            Log.w("RuntimeUtils", "countTarXzEntries failed: " + th.getMessage());
        }
        return i;
    }

    public static void patchJava(Context context, String str) throws IOException {
        File file = new File(str);
        if (file.exists()) {
            String str2 = context.getApplicationInfo().nativeLibraryDir;
            unpack200(str2, str);
            String resolveLibFolder = resolveLibFolder(file);
            File file2 = new File(file, resolveLibFolder + "/libfreetype.so.6");
            File file3 = new File(file, resolveLibFolder + "/libfreetype.so");
            if (file2.exists() && (!file3.exists() || file2.length() != file3.length())) {
                file2.renameTo(file3);
            }
            File file4 = new File(file, resolveLibFolder + "/libawt_xawt.so");
            file4.delete();
            FileUtils.copyFile(new File(str2, "libawt_xawt.so"), file4);
            File file5 = new File(str2, "libjsound.so");
            if (file5.exists()) {
                File file6 = new File(file, resolveLibFolder + "/libjsound.so");
                file6.delete();
                FileUtils.copyFile(file5, file6);
            }
        }
    }

    public static void unpack200(String str, String str2) {
        Collection<File> listFiles = org.apache.commons.io.FileUtils.listFiles(new File(str2), new String[]{"pack"}, true);
        ProcessBuilder directory = new ProcessBuilder(new String[0]).directory(new File(str));
        for (File file : listFiles) {
            try {
                int waitFor = directory.command("./libunpack200.so", "-r", file.getAbsolutePath(), file.getAbsolutePath().replace(".pack", "")).start().waitFor();
                if (waitFor != 0) {
                    Log.w("RuntimeUtils", "unpack200 failed (" + waitFor + "): " + file.getAbsolutePath());
                }
            } catch (IOException e) {
                Log.w("RuntimeUtils", "unpack200 failed: " + file.getAbsolutePath() + " - " + e.getMessage());
            } catch (InterruptedException unused) {
                Thread.currentThread().interrupt();
                Log.w("RuntimeUtils", "unpack200 interrupted: " + file.getAbsolutePath());
            }
        }
    }

    private static String resolveLibFolder(File file) {
        String[] strArr = {"lib/aarch64", "lib/amd64", "lib/i386", "lib/arm", "lib"};
        for (int i = 0; i < 5; i++) {
            String str = strArr[i];
            File file2 = new File(file, str);
            if (file2.isDirectory() && new File(file2, "libjava.so").isFile()) {
                return str;
            }
        }
        return "lib";
    }

    public static void copyAssets(Context context, String str, String str2) throws IOException {
        copyAssets(context, str, str2, null);
    }

    public static void copyAssets(Context context, String str, String str2, InstallListener installListener) throws IOException {
        copyAssetsInternal(context, str, str2, str, installListener);
    }

    private static void copyAssetsInternal(Context context, String str, String str2, String str3, InstallListener installListener) throws IOException {
        String[] list = context.getAssets().list(str);
        if (list != null && list.length > 0) {
            File file = new File(str2);
            if (!file.exists() && !file.mkdirs()) {
                throw new IOException("Cannot create dir: " + str2);
            }
            for (String str4 : list) {
                copyAssetsInternal(context, str + "/" + str4, str2 + File.separator + str4, str3, installListener);
            }
            return;
        }
        if (installListener != null) {
            installListener.onUpdate(str.equals(str3) ? new File(str).getName() : str.substring(str3.length() + 1));
        }
        File parentFile = new File(str2).getParentFile();
        if (parentFile != null && !parentFile.exists() && !parentFile.mkdirs()) {
            throw new IOException("Cannot create dir: " + parentFile);
        }
        InputStream open = context.getAssets().open(str);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(str2);
            try {
                byte[] bArr = new byte[65536];
                while (true) {
                    int read = open.read(bArr);
                    if (read == -1) {
                        break;
                    } else {
                        fileOutputStream.write(bArr, 0, read);
                    }
                }
                fileOutputStream.getFD().sync();
                fileOutputStream.close();
                if (open != null) {
                    open.close();
                }
            } finally {
            }
        } catch (Throwable th) {
            if (open != null) {
                try {
                    open.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    public static String readAssetText(Context context, String str) throws IOException {
        InputStream open = context.getAssets().open(str);
        try {
            String readFullyAsString = IOUtils.readFullyAsString(open);
            if (open != null) {
                open.close();
            }
            return readFullyAsString;
        } catch (Throwable th) {
            if (open != null) {
                try {
                    open.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    public static String readLocalText(String str) {
        try {
            FileInputStream fileInputStream = new FileInputStream(str);
            try {
                String readFullyAsString = IOUtils.readFullyAsString(fileInputStream);
                fileInputStream.close();
                return readFullyAsString;
            } finally {
            }
        } catch (IOException unused) {
            return null;
        }
    }

    static {
        Objects.requireNonNull(StandardCharsets.UTF_8);
    }
}
