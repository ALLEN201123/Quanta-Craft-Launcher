package com.qcl.launcher.launcher.mod.modrinth;

import android.os.AsyncTask;

import com.google.gson.JsonParseException;
import com.qcl.launcher.launcher.mod.Modpack;
import com.qcl.launcher.launcher.mod.ModpackProvider;
import com.qcl.launcher.utils.gson.JsonUtils;
import com.qcl.launcher.utils.io.ZipTools;

import org.apache.commons.compress.archivers.zip.ZipFile;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

public final class ModrinthModpackProvider implements ModpackProvider {
    public static final ModrinthModpackProvider INSTANCE = new ModrinthModpackProvider();

    @Override
    public String getName() {
        return "Modrinth";
    }

    @Override
    public Modpack readManifest(ZipFile zip, Path file, Charset encoding) throws IOException, JsonParseException {
        ModrinthManifest manifest = JsonUtils.fromNonNullJson(ZipTools.readTextZipEntry(zip, "modrinth.index.json"), ModrinthManifest.class);
        return new Modpack(manifest.getName(), "", manifest.getVersionId(), manifest.getGameVersion(), manifest.getSummary(), encoding, manifest) {
            @Override
            public AsyncTask getInstallTask(java.io.File zipFile, String name) {
                return new ModrinthInstallTask(zipFile, this, manifest, name);
            }
        };
    }

}