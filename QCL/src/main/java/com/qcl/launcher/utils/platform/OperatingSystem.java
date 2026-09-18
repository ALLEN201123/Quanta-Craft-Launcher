/*
 * Decompiled with CFR 0.152.
 */
package com.qcl.launcher.utils.platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum OperatingSystem {
    WINDOWS("windows"),
    LINUX("linux"),
    OSX("osx"),
    UNKNOWN("universal");

    private final String checkedName;
    public static final OperatingSystem CURRENT_OS;
    public static final int TOTAL_MEMORY;
    public static final int SUGGESTED_MEMORY;
    public static final String PATH_SEPARATOR;
    public static final String FILE_SEPARATOR;
    public static final String LINE_SEPARATOR;
    public static final Charset NATIVE_CHARSET;
    public static final int SYSTEM_BUILD_NUMBER;
    public static final String SYSTEM_NAME;
    public static final String SYSTEM_VERSION;
    public static final Pattern INVALID_RESOURCE_CHARACTERS;
    private static final String[] INVALID_RESOURCE_BASENAMES;
    private static final String[] INVALID_RESOURCE_FULLNAMES;
    private static final Pattern MEMINFO_PATTERN;

    private OperatingSystem(String checkedName) {
        this.checkedName = checkedName;
    }

    public String getCheckedName() {
        return this.checkedName;
    }

    public static OperatingSystem parseOSName(String name) {
        if (name == null) {
            return UNKNOWN;
        }
        if ((name = name.trim().toLowerCase(Locale.ROOT)).contains("win")) {
            return WINDOWS;
        }
        if (name.contains("mac")) {
            return OSX;
        }
        if (name.contains("solaris") || name.contains("linux") || name.contains("unix") || name.contains("sunos")) {
            return LINUX;
        }
        return UNKNOWN;
    }

    public static Optional<PhysicalMemoryStatus> getPhysicalMemoryStatus() {
        if (CURRENT_OS == LINUX) {
            try {
                long free = 0L;
                long available = 0L;
                long total = 0L;
                for (String line : Files.readAllLines(Paths.get("/proc/meminfo", new String[0]))) {
                    Matcher matcher = MEMINFO_PATTERN.matcher(line);
                    if (!matcher.find()) continue;
                    String key = matcher.group("key");
                    String value = matcher.group("value");
                    if ("MemAvailable".equals(key)) {
                        available = Long.parseLong(value) * 1024L;
                    }
                    if ("MemFree".equals(key)) {
                        free = Long.parseLong(value) * 1024L;
                    }
                    if (!"MemTotal".equals(key)) continue;
                    total = Long.parseLong(value) * 1024L;
                }
                if (total > 0L) {
                    return Optional.of(new PhysicalMemoryStatus(total, available > 0L ? available : free));
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Optional.empty();
    }

    public static void forceGC() {
        System.gc();
        System.runFinalization();
        System.gc();
    }

    public static Path getWorkingDirectory(String folder) {
        String home = System.getProperty("user.home", ".");
        switch (CURRENT_OS) {
            case LINUX: {
                return Paths.get(home, "." + folder);
            }
            case WINDOWS: {
                String appdata = System.getenv("APPDATA");
                return Paths.get(appdata == null ? home : appdata, "." + folder);
            }
            case OSX: {
                return Paths.get(home, "Library", "Application Support", folder);
            }
        }
        return Paths.get(home, folder);
    }

    public static boolean isNameValid(String name) {
        if (name.isEmpty()) {
            return false;
        }
        if (name.equals(".")) {
            return false;
        }
        if (name.indexOf(47) != -1 || name.indexOf(0) != -1) {
            return false;
        }
        if (CURRENT_OS == WINDOWS) {
            String basename;
            char lastChar = name.charAt(name.length() - 1);
            if (lastChar == '.') {
                return false;
            }
            if (Character.isWhitespace(lastChar)) {
                return false;
            }
            int dot = name.indexOf(46);
            String string2 = basename = dot == -1 ? name : name.substring(0, dot);
            if (Arrays.binarySearch(INVALID_RESOURCE_BASENAMES, basename.toLowerCase()) >= 0) {
                return false;
            }
            if (Arrays.binarySearch(INVALID_RESOURCE_FULLNAMES, name.toLowerCase()) >= 0) {
                return false;
            }
            if (INVALID_RESOURCE_CHARACTERS.matcher(name).find()) {
                return false;
            }
        }
        return true;
    }

    static {
        CURRENT_OS = OperatingSystem.parseOSName(System.getProperty("os.name"));
        PATH_SEPARATOR = File.pathSeparator;
        FILE_SEPARATOR = File.separator;
        LINE_SEPARATOR = System.lineSeparator();
        MEMINFO_PATTERN = Pattern.compile("^(?<key>.*?):\\s+(?<value>\\d+) kB?$");
        String nativeEncoding = System.getProperty("native.encoding", System.getProperty("sun.jnu.encoding"));
        Charset nativeCharset = Charset.defaultCharset();
        if (nativeEncoding != null) {
            try {
                nativeCharset = Charset.forName(nativeEncoding);
            }
            catch (UnsupportedCharsetException e) {
                e.printStackTrace();
            }
        }
        NATIVE_CHARSET = nativeCharset;
        if (CURRENT_OS == WINDOWS) {
            String osName;
            String versionNumber = null;
            int buildNumber = -1;
            try {
                Process process = Runtime.getRuntime().exec(new String[]{"cmd", "ver"});
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), NATIVE_CHARSET));){
                    Matcher matcher = Pattern.compile("(?<version>[0-9]+\\.[0-9]+\\.(?<build>[0-9]+)(\\.[0-9]+)?)]$").matcher(reader.readLine().trim());
                    if (matcher.find()) {
                        versionNumber = matcher.group("version");
                        buildNumber = Integer.parseInt(matcher.group("build"));
                    }
                }
                process.destroy();
            }
            catch (Throwable process) {
                // empty catch block
            }
            if (versionNumber == null) {
                versionNumber = System.getProperty("os.version");
            }
            if ((osName = System.getProperty("os.name")).equals("Windows 10") && buildNumber >= 22000) {
                osName = "Windows 11";
            }
            SYSTEM_NAME = osName;
            SYSTEM_VERSION = versionNumber;
            SYSTEM_BUILD_NUMBER = buildNumber;
        } else {
            SYSTEM_NAME = System.getProperty("os.name");
            SYSTEM_VERSION = System.getProperty("os.version");
            SYSTEM_BUILD_NUMBER = -1;
        }
        TOTAL_MEMORY = OperatingSystem.getPhysicalMemoryStatus().map(PhysicalMemoryStatus::getTotal).map(bytes -> (int)(bytes / 1024L / 1024L)).orElse(1024);
        SUGGESTED_MEMORY = (int)(Math.round(1.0 * (double)TOTAL_MEMORY / 4.0 / 128.0) * 128L);
        if (CURRENT_OS == WINDOWS) {
            INVALID_RESOURCE_CHARACTERS = Pattern.compile("[/\"<>|?*:\\\\]");
            INVALID_RESOURCE_BASENAMES = new String[]{"aux", "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9", "con", "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9", "nul", "prn"};
            Arrays.sort(INVALID_RESOURCE_BASENAMES);
            INVALID_RESOURCE_FULLNAMES = new String[]{"clock$"};
        } else {
            INVALID_RESOURCE_CHARACTERS = null;
            INVALID_RESOURCE_BASENAMES = null;
            INVALID_RESOURCE_FULLNAMES = null;
        }
    }

    public static class PhysicalMemoryStatus {
        private final long total;
        private final long available;
        public static final PhysicalMemoryStatus INVALID = new PhysicalMemoryStatus(0L, -1L);

        public PhysicalMemoryStatus(long total, long available) {
            this.total = total;
            this.available = available;
        }

        public long getTotal() {
            return this.total;
        }

        public double getTotalGB() {
            return PhysicalMemoryStatus.toGigaBytes(this.total);
        }

        public long getUsed() {
            return this.hasAvailable() ? this.total - this.available : 0L;
        }

        public double getUsedGB() {
            return PhysicalMemoryStatus.toGigaBytes(this.getUsed());
        }

        public long getAvailable() {
            return this.available;
        }

        public double getAvailableGB() {
            return PhysicalMemoryStatus.toGigaBytes(this.available);
        }

        public boolean hasAvailable() {
            return this.available >= 0L;
        }

        public static double toGigaBytes(long bytes) {
            return (double)bytes / 1024.0 / 1024.0 / 1024.0;
        }
    }
}

