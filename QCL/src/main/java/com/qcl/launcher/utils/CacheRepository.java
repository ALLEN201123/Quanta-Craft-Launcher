/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonParseException
 *  com.google.gson.annotations.SerializedName
 *  com.google.gson.reflect.TypeToken
 */
package com.qcl.launcher.utils;

import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.qcl.launcher.utils.DigestUtils;
import com.qcl.launcher.utils.Hex;
import com.qcl.launcher.utils.Lang;
import com.qcl.launcher.utils.Logging;
import com.qcl.launcher.utils.function.ExceptionalSupplier;
import com.qcl.launcher.utils.gson.JsonUtils;
import com.qcl.launcher.utils.io.FileUtils;
import com.qcl.launcher.utils.io.IOUtils;
import com.qcl.launcher.utils.string.StringUtils;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.stream.Stream;

public class CacheRepository {
    private Path commonDirectory;
    private Path cacheDirectory;
    private Path indexFile;
    private Map<String, ETagItem> index;
    private Map<String, Storage> storages = new HashMap<String, Storage>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private static CacheRepository instance = new CacheRepository();
    public static final String SHA1 = "SHA-1";

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void changeDirectory(Path commonDir) {
        this.commonDirectory = commonDir;
        this.cacheDirectory = commonDir.resolve("cache");
        this.indexFile = this.cacheDirectory.resolve("etag.json");
        this.lock.writeLock().lock();
        try {
            ETagIndex raw;
            for (Storage storage : this.storages.values()) {
                storage.changeDirectory(this.cacheDirectory);
            }
            this.index = Files.isRegularFile(this.indexFile, new LinkOption[0]) ? ((raw = (ETagIndex)JsonUtils.GSON.fromJson(FileUtils.readText(this.indexFile.toFile()), ETagIndex.class)) == null ? new HashMap<String, ETagItem>() : this.joinETagIndexes(raw.eTag)) : new HashMap<String, ETagItem>();
        }
        catch (JsonParseException | IOException e) {
            Logging.LOG.log(Level.WARNING, "Unable to read index file", e);
            this.index = new HashMap<String, ETagItem>();
        }
        finally {
            this.lock.writeLock().unlock();
        }
    }

    public Path getCommonDirectory() {
        return this.commonDirectory;
    }

    public Path getCacheDirectory() {
        return this.cacheDirectory;
    }

    public Storage getStorage(String key) {
        this.lock.readLock().lock();
        try {
            Storage storage = this.storages.computeIfAbsent(key, Storage::new);
            return storage;
        }
        finally {
            this.lock.readLock().unlock();
        }
    }

    protected Path getFile(String algorithm, String hash) {
        return this.getCacheDirectory().resolve(algorithm).resolve(hash.substring(0, 2)).resolve(hash);
    }

    protected boolean fileExists(String algorithm, String hash) {
        if (hash == null) {
            return false;
        }
        Path file = this.getFile(algorithm, hash);
        if (Files.exists(file, new LinkOption[0])) {
            try {
                return Hex.encodeHex(DigestUtils.digest(algorithm, file)).equalsIgnoreCase(hash);
            }
            catch (IOException e) {
                return false;
            }
        }
        return false;
    }

    public void tryCacheFile(Path path, String algorithm, String hash) throws IOException {
        Path cache = this.getFile(algorithm, hash);
        if (Files.isRegularFile(cache, new LinkOption[0])) {
            return;
        }
        FileUtils.copyFile(path.toFile(), cache.toFile());
    }

    public Path cacheFile(Path path, String algorithm, String hash) throws IOException {
        Path cache = this.getFile(algorithm, hash);
        FileUtils.copyFile(path.toFile(), cache.toFile());
        return cache;
    }

    public Optional<Path> checkExistentFile(Path original, String algorithm, String hash) {
        if (this.fileExists(algorithm, hash)) {
            return Optional.of(this.getFile(algorithm, hash));
        }
        if (original != null && Files.exists(original, new LinkOption[0])) {
            if (hash != null) {
                try {
                    String checksum = Hex.encodeHex(DigestUtils.digest(algorithm, original));
                    if (checksum.equalsIgnoreCase(hash)) {
                        return Optional.of(this.restore(original, () -> this.cacheFile(original, algorithm, hash)));
                    }
                }
                catch (IOException iOException) {}
            } else {
                return Optional.of(original);
            }
        }
        return Optional.empty();
    }

    protected Path restore(Path original, ExceptionalSupplier<Path, ? extends IOException> cacheSupplier) throws IOException {
        Path cache = cacheSupplier.get();
        Files.delete(original);
        Files.createLink(original, cache);
        return cache;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public Path getCachedRemoteFile(URLConnection conn) throws IOException {
        String hash;
        ETagItem eTagItem;
        String url = conn.getURL().toString();
        this.lock.readLock().lock();
        try {
            eTagItem = this.index.get(url);
        }
        finally {
            this.lock.readLock().unlock();
        }
        if (eTagItem == null) {
            throw new IOException("Cannot find the URL");
        }
        if (StringUtils.isBlank(eTagItem.hash) || !this.fileExists(SHA1, eTagItem.hash)) {
            throw new FileNotFoundException();
        }
        Path file = this.getFile(SHA1, eTagItem.hash);
        if (Files.getLastModifiedTime(file, new LinkOption[0]).toMillis() != eTagItem.localLastModified && !Objects.equals(hash = Hex.encodeHex(DigestUtils.digest(SHA1, file)), eTagItem.hash)) {
            throw new IOException("This file is modified");
        }
        return file;
    }

    public void removeRemoteEntry(URLConnection conn) {
        String url = conn.getURL().toString();
        this.lock.readLock().lock();
        try {
            this.index.remove(url);
        }
        finally {
            this.lock.readLock().unlock();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void injectConnection(URLConnection conn) {
        ETagItem eTagItem;
        String url = conn.getURL().toString();
        this.lock.readLock().lock();
        try {
            eTagItem = this.index.get(url);
        }
        finally {
            this.lock.readLock().unlock();
        }
        if (eTagItem == null) {
            return;
        }
        if (eTagItem.eTag != null) {
            conn.setRequestProperty("If-None-Match", eTagItem.eTag);
        }
    }

    public void cacheRemoteFile(Path downloaded, URLConnection conn) throws IOException {
        this.cacheData(() -> {
            String hash = Hex.encodeHex(DigestUtils.digest(SHA1, downloaded));
            Path cached = this.cacheFile(downloaded, SHA1, hash);
            return new CacheResult(hash, cached);
        }, conn);
    }

    public void cacheText(String text, URLConnection conn) throws IOException {
        this.cacheBytes(text.getBytes(StandardCharsets.UTF_8), conn);
    }

    public void cacheBytes(byte[] bytes, URLConnection conn) throws IOException {
        this.cacheData(() -> {
            String hash = Hex.encodeHex(DigestUtils.digest(SHA1, bytes));
            Path cached = this.getFile(SHA1, hash);
            FileUtils.writeBytes(cached.toFile(), bytes);
            return new CacheResult(hash, cached);
        }, conn);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public synchronized void cacheData(ExceptionalSupplier<CacheResult, IOException> cacheSupplier, URLConnection conn) throws IOException {
        String eTag = conn.getHeaderField("ETag");
        if (eTag == null) {
            return;
        }
        String url = conn.getURL().toString();
        String lastModified = conn.getHeaderField("Last-Modified");
        CacheResult cacheResult = cacheSupplier.get();
        ETagItem eTagItem = new ETagItem(url, eTag, cacheResult.hash, Files.getLastModifiedTime(cacheResult.cachedFile, new LinkOption[0]).toMillis(), lastModified);
        Lock writeLock = this.lock.writeLock();
        writeLock.lock();
        try {
            this.index.compute(eTagItem.url, this.updateEntity(eTagItem));
            this.saveETagIndex();
        }
        finally {
            writeLock.unlock();
        }
    }

    private BiFunction<String, ETagItem, ETagItem> updateEntity(ETagItem newItem) {
        return (key, oldItem) -> {
            if (oldItem == null) {
                return newItem;
            }
            if (oldItem.compareTo(newItem) < 0) {
                Path cached = this.getFile(SHA1, ((ETagItem)oldItem).hash);
                try {
                    Files.deleteIfExists(cached);
                }
                catch (IOException iOException) {
                    // empty catch block
                }
                return newItem;
            }
            return oldItem;
        };
    }

    @SafeVarargs
    private final Map<String, ETagItem> joinETagIndexes(Collection<ETagItem> ... indexes) {
        ConcurrentHashMap<String, ETagItem> eTags = new ConcurrentHashMap<String, ETagItem>();
        Stream stream = Arrays.stream(indexes).filter(Objects::nonNull).map(Collection::stream).reduce(Stream.empty(), Stream::concat);
        stream.forEach(eTag -> eTags.compute(((ETagItem)eTag).url, this.updateEntity((ETagItem)eTag)));
        return eTags;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void saveETagIndex() throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(this.indexFile.toFile(), "rw");
             FileChannel channel = file.getChannel();){
            FileLock lock = channel.lock();
            try {
                ETagIndex indexOnDisk = JsonUtils.fromMaybeMalformedJson(new String(IOUtils.readFullyWithoutClosing(Channels.newInputStream(channel)), StandardCharsets.UTF_8), ETagIndex.class);
                Map<String, ETagItem> newIndex = this.joinETagIndexes(indexOnDisk == null ? null : indexOnDisk.eTag, this.index.values());
                channel.truncate(0L);
                OutputStream os = Channels.newOutputStream(channel);
                ETagIndex writeTo = new ETagIndex(newIndex.values());
                IOUtils.write(JsonUtils.GSON.toJson((Object)writeTo).getBytes(StandardCharsets.UTF_8), os);
                this.index = newIndex;
            }
            finally {
                lock.release();
            }
        }
    }

    public static CacheRepository getInstance() {
        return instance;
    }

    public static void setInstance(CacheRepository instance) {
        CacheRepository.instance = instance;
    }

    public static class Storage {
        private final String name;
        private Map<String, Object> storage;
        private final ReadWriteLock lock = new ReentrantReadWriteLock();
        private Path indexFile;

        public Storage(String name) {
            this.name = name;
        }

        public Object getEntry(String key) {
            this.lock.readLock().lock();
            try {
                Object object = this.storage.get(key);
                return object;
            }
            finally {
                this.lock.readLock().unlock();
            }
        }

        public void putEntry(String key, Object value) {
            this.lock.writeLock().lock();
            try {
                this.storage.put(key, value);
                this.saveToFile();
            }
            finally {
                this.lock.writeLock().unlock();
            }
        }

        private void joinEntries(Map<String, Object> storage) {
            this.storage.putAll(storage);
        }

        private void changeDirectory(Path cacheDirectory) {
            this.lock.writeLock().lock();
            try {
                this.indexFile = cacheDirectory.resolve(this.name + ".json");
                if (Files.isRegularFile(this.indexFile, new LinkOption[0])) {
                    this.joinEntries((Map)JsonUtils.fromNonNullJson(FileUtils.readText(this.indexFile.toFile()), new TypeToken<Map<String, Object>>(){}.getType()));
                }
            }
            catch (JsonParseException | IOException e) {
                Logging.LOG.log(Level.WARNING, "Unable to read storage {" + this.name + "} file");
            }
            finally {
                this.lock.writeLock().unlock();
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        public void saveToFile() {
            try (RandomAccessFile file = new RandomAccessFile(this.indexFile.toFile(), "rw");
                 FileChannel channel = file.getChannel();){
                FileLock lock = channel.lock();
                try {
                    HashMap<String, Object> indexOnDisk = (HashMap<String, Object>)JsonUtils.fromMaybeMalformedJson(new String(IOUtils.readFullyWithoutClosing(Channels.newInputStream(channel)), StandardCharsets.UTF_8), new TypeToken<Map<String, Object>>(){}.getType());
                    if (indexOnDisk == null) {
                        indexOnDisk = new HashMap<String, Object>();
                    }
                    indexOnDisk.putAll(this.storage);
                    channel.truncate(0L);
                    OutputStream os = Channels.newOutputStream(channel);
                    IOUtils.write(JsonUtils.GSON.toJson(this.storage).getBytes(StandardCharsets.UTF_8), os);
                    this.storage = indexOnDisk;
                }
                finally {
                    lock.release();
                }
            }
            catch (IOException e) {
                Logging.LOG.log(Level.WARNING, "Unable to write storage {" + this.name + "} file");
            }
        }
    }

    private class ETagIndex {
        private final Collection<ETagItem> eTag;

        public ETagIndex() {
            this.eTag = new HashSet<ETagItem>();
        }

        public ETagIndex(Collection<ETagItem> eTags) {
            this.eTag = new HashSet<ETagItem>(eTags);
        }
    }

    private class ETagItem {
        private final String url;
        private final String eTag;
        private final String hash;
        @SerializedName(value="local")
        private final long localLastModified;
        @SerializedName(value="remote")
        private final String remoteLastModified;

        public ETagItem() {
            this(null, null, null, 0L, null);
        }

        public ETagItem(String url, String eTag, String hash, long localLastModified, String remoteLastModified) {
            this.url = url;
            this.eTag = eTag;
            this.hash = hash;
            this.localLastModified = localLastModified;
            this.remoteLastModified = remoteLastModified;
        }

        public int compareTo(ETagItem other) {
            if (!this.url.equals(other.url)) {
                throw new IllegalArgumentException();
            }
            ZonedDateTime thisTime = Lang.ignoringException(() -> ZonedDateTime.parse(this.remoteLastModified, DateTimeFormatter.RFC_1123_DATE_TIME), null);
            ZonedDateTime otherTime = Lang.ignoringException(() -> ZonedDateTime.parse(other.remoteLastModified, DateTimeFormatter.RFC_1123_DATE_TIME), null);
            if (thisTime == null && otherTime == null) {
                return 0;
            }
            if (thisTime == null) {
                return -1;
            }
            if (otherTime == null) {
                return 1;
            }
            return thisTime.compareTo(otherTime);
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            ETagItem eTagItem = (ETagItem)o;
            return this.localLastModified == eTagItem.localLastModified && Objects.equals(this.url, eTagItem.url) && Objects.equals(this.eTag, eTagItem.eTag) && Objects.equals(this.hash, eTagItem.hash) && Objects.equals(this.remoteLastModified, eTagItem.remoteLastModified);
        }

        public int hashCode() {
            return Objects.hash(this.url, this.eTag, this.hash, this.localLastModified, this.remoteLastModified);
        }
    }

    private static class CacheResult {
        public String hash;
        public Path cachedFile;

        public CacheResult(String hash, Path cachedFile) {
            this.hash = hash;
            this.cachedFile = cachedFile;
        }
    }
}

