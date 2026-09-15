package com.qcl.launcher.launcher.mod.server;

import com.google.gson.JsonParseException;
import com.qcl.launcher.launcher.mod.Modpack;
import com.qcl.launcher.launcher.mod.ModpackProvider;
import com.qcl.launcher.utils.gson.JsonUtils;
import com.qcl.launcher.utils.io.ZipTools;

import org.apache.commons.compress.archivers.zip.ZipFile;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

public final class ServerModpackProvider implements ModpackProvider {
    public static final ServerModpackProvider INSTANCE = new ServerModpackProvider();

    @Override
    public String getName() {
        return "Server";
    }

    @Override
    public Modpack readManifest(ZipFile zip, Path file, Charset encoding) throws IOException, JsonParseException {
        String json = ZipTools.readTextZipEntry(zip, "server-manifest.json");
        ServerModpackManifest manifest = JsonUtils.fromNonNullJson(json, ServerModpackManifest.class);
        return manifest.toModpack(encoding);
    }
}