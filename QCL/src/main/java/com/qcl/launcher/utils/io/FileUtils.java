/*
 * Decompiled with CFR 0.152.
 */
package com.qcl.launcher.utils.io;

import com.qcl.launcher.utils.Lang;
import com.qcl.launcher.utils.string.StringUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public final class FileUtils {
    private FileUtils() {
    }

    public static boolean canCreateDirectory(String path) {
        try {
            return FileUtils.canCreateDirectory(Paths.get(path, new String[0]));
        }
        catch (InvalidPathException e) {
            return false;
        }
    }

    public static boolean canCreateDirectory(Path path) {
        if (Files.isDirectory(path, new LinkOption[0])) {
            return true;
        }
        if (Files.exists(path, new LinkOption[0])) {
            return false;
        }
        Path lastPath = path;
        for (path = path.getParent(); path != null && !Files.exists(path, new LinkOption[0]); path = path.getParent()) {
            lastPath = path;
        }
        if (path == null) {
            return false;
        }
        if (!Files.isDirectory(path, new LinkOption[0])) {
            return false;
        }
        try {
            Files.createDirectory(lastPath, new FileAttribute[0]);
            Files.delete(lastPath);
            return true;
        }
        catch (IOException e) {
            return false;
        }
    }

    public static String getNameWithoutExtension(String fileName) {
        return StringUtils.substringBeforeLast(fileName, '.');
    }

    public static String getNameWithoutExtension(File file) {
        return StringUtils.substringBeforeLast(file.getName(), '.');
    }

    public static String getNameWithoutExtension(Path file) {
        return StringUtils.substringBeforeLast(FileUtils.getName(file), '.');
    }

    public static String getExtension(File file) {
        return StringUtils.substringAfterLast(file.getName(), '.');
    }

    public static String getExtension(Path file) {
        return StringUtils.substringAfterLast(FileUtils.getName(file), '.');
    }

    public static String normalizePath(String path) {
        return StringUtils.addPrefix(StringUtils.removeSuffix(path, "/", "\\"), "/");
    }

    public static String getName(Path path) {
        if (path.getFileName() == null) {
            return "";
        }
        return StringUtils.removeSuffix(path.getFileName().toString(), "/", "\\");
    }

    public static String getName(Path path, String candidate) {
        if (path.getFileName() == null) {
            return candidate;
        }
        return FileUtils.getName(path);
    }

    public static String readText(File file) throws IOException {
        return FileUtils.readText(file, StandardCharsets.UTF_8);
    }

    public static String readText(File file, Charset charset) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), charset);
    }

    public static String readText(Path file) throws IOException {
        return FileUtils.readText(file, StandardCharsets.UTF_8);
    }

    public static String readText(Path file, Charset charset) throws IOException {
        return new String(Files.readAllBytes(file), charset);
    }

    public static void writeText(File file, String text) throws IOException {
        FileUtils.writeText(file, text, StandardCharsets.UTF_8);
    }

    public static void writeText(File file, String text, Charset charset) throws IOException {
        FileUtils.writeBytes(file, text.getBytes(charset));
    }

    public static void writeBytes(File file, byte[] array) throws IOException {
        Files.createDirectories(file.toPath().getParent(), new FileAttribute[0]);
        Files.write(file.toPath(), array, new OpenOption[0]);
    }

    public static void deleteDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }
        if (!FileUtils.isSymlink(directory)) {
            FileUtils.cleanDirectory(directory);
        }
        if (!directory.delete()) {
            String message = "Unable to delete directory " + directory + ".";
            throw new IOException(message);
        }
    }

    public static boolean deleteDirectoryQuietly(File directory) {
        try {
            FileUtils.deleteDirectory(directory);
            return true;
        }
        catch (IOException e) {
            return false;
        }
    }

    public static void copyDirectory(Path src, Path dest) throws IOException {
        FileUtils.copyDirectory(src, dest, path -> true);
    }

    public static void copyDirectory(final Path src, final Path dest, final Predicate<String> filePredicate) throws IOException {
        Files.walkFileTree(src, (FileVisitor<? super Path>)new SimpleFileVisitor<Path>(){

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!filePredicate.test(src.relativize(file).toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                Path destFile = dest.resolve(src.relativize(file).toString());
                Files.copy(file, destFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!filePredicate.test(src.relativize(dir).toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                Path destDir = dest.resolve(src.relativize(dir).toString());
                Files.createDirectories(destDir, new FileAttribute[0]);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void cleanDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            if (!FileUtils.makeDirectory(directory)) {
                throw new IOException("Failed to create directory: " + directory);
            }
            return;
        }
        if (!directory.isDirectory()) {
            String message = directory + " is not a directory";
            throw new IllegalArgumentException(message);
        }
        File[] files = directory.listFiles();
        if (files == null) {
            throw new IOException("Failed to list contents of " + directory);
        }
        IOException exception = null;
        for (File file : files) {
            try {
                FileUtils.forceDelete(file);
            }
            catch (IOException ioe) {
                exception = ioe;
            }
        }
        if (null != exception) {
            throw exception;
        }
    }

    public static boolean cleanDirectoryQuietly(File directory) {
        try {
            FileUtils.cleanDirectory(directory);
            return true;
        }
        catch (IOException e) {
            return false;
        }
    }

    public static void forceDelete(File file) throws IOException {
        if (file.isDirectory()) {
            FileUtils.deleteDirectory(file);
        } else {
            boolean filePresent = file.exists();
            if (!file.delete()) {
                if (!filePresent) {
                    throw new FileNotFoundException("File does not exist: " + file);
                }
                throw new IOException("Unable to delete file: " + file);
            }
        }
    }

    public static boolean isSymlink(File file) throws IOException {
        File fileInCanonicalDir;
        Objects.requireNonNull(file, "File must not be null");
        if (File.separatorChar == '\\') {
            return false;
        }
        if (file.getParent() == null) {
            fileInCanonicalDir = file;
        } else {
            File canonicalDir = file.getParentFile().getCanonicalFile();
            fileInCanonicalDir = new File(canonicalDir, file.getName());
        }
        return !fileInCanonicalDir.getCanonicalFile().equals(fileInCanonicalDir.getAbsoluteFile());
    }

    public static void copyFile(File srcFile, File destFile) throws IOException {
        Objects.requireNonNull(srcFile, "Source must not be null");
        Objects.requireNonNull(destFile, "Destination must not be null");
        if (!srcFile.exists()) {
            throw new FileNotFoundException("Source '" + srcFile + "' does not exist");
        }
        if (srcFile.isDirectory()) {
            throw new IOException("Source '" + srcFile + "' exists but is a directory");
        }
        if (srcFile.getCanonicalPath().equals(destFile.getCanonicalPath())) {
            throw new IOException("Source '" + srcFile + "' and destination '" + destFile + "' are the same");
        }
        File parentFile = destFile.getParentFile();
        if (parentFile != null && !FileUtils.makeDirectory(parentFile)) {
            throw new IOException("Destination '" + parentFile + "' directory cannot be created");
        }
        if (destFile.exists() && !destFile.canWrite()) {
            throw new IOException("Destination '" + destFile + "' exists but is read-only");
        }
        Files.copy(srcFile.toPath(), destFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void copyFile(Path srcFile, Path destFile) throws IOException {
        Objects.requireNonNull(srcFile, "Source must not be null");
        Objects.requireNonNull(destFile, "Destination must not be null");
        if (!Files.exists(srcFile, new LinkOption[0])) {
            throw new FileNotFoundException("Source '" + srcFile + "' does not exist");
        }
        if (Files.isDirectory(srcFile, new LinkOption[0])) {
            throw new IOException("Source '" + srcFile + "' exists but is a directory");
        }
        Path parentFile = destFile.getParent();
        Files.createDirectories(parentFile, new FileAttribute[0]);
        if (Files.exists(destFile, new LinkOption[0]) && !Files.isWritable(destFile)) {
            throw new IOException("Destination '" + destFile + "' exists but is read-only");
        }
        Files.copy(srcFile, destFile, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void moveFile(File srcFile, File destFile) throws IOException {
        FileUtils.copyFile(srcFile, destFile);
        srcFile.delete();
    }

    public static boolean makeDirectory(File directory) {
        directory.mkdirs();
        return directory.isDirectory();
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public static boolean makeFile(File file) {
        if (!FileUtils.makeDirectory(file.getAbsoluteFile().getParentFile())) return false;
        if (file.exists()) return true;
        if (!Lang.test(file::createNewFile)) return false;
        return true;
    }

    public static List<File> listFilesByExtension(File file, String extension) {
        LinkedList<File> result = new LinkedList<File>();
        File[] files = file.listFiles();
        if (files != null) {
            for (File it : files) {
                if (!extension.equals(FileUtils.getExtension(it))) continue;
                result.add(it);
            }
        }
        return result;
    }

    public static boolean isValidPath(File file) {
        try {
            file.toPath();
            return true;
        }
        catch (InvalidPathException ignored) {
            return false;
        }
    }

    public static Optional<Path> tryGetPath(String first, String ... more) {
        if (first == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Paths.get(first, more));
        }
        catch (InvalidPathException e) {
            return Optional.empty();
        }
    }
}

