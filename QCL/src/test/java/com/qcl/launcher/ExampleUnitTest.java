package com.qcl.launcher;

import com.google.gson.Gson;
import com.qcl.launcher.launcher.download.game.VersionManifest;
import com.qcl.launcher.launcher.game.Artifact;
import com.qcl.launcher.launcher.game.Version;
import com.qcl.launcher.launcher.setting.game.GameLaunchSetting;
import com.qcl.launcher.launcher.uis.game.download.DownloadUrlSource;
import com.qcl.launcher.utils.gson.JsonUtils;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import static org.junit.Assert.*;

/** Regression checks for the legacy-based compatibility build; no device required. */
public class ExampleUnitTest {
    @Test public void autoJavaSelectionUsesTheSameRuntimeAtBoundaries() {
        int[] required = {8, 9, 16, 17, 18, 21, 22, 25};
        String[] expected = {"default", "JRE17", "JRE17", "JRE17", "JRE21", "JRE21", "JRE25", "JRE25"};
        for (int i = 0; i < required.length; i++) {
            assertEquals(expected[i], GameLaunchSetting.selectJavaRuntime(required[i]));
        }
    }
    @Test(expected = IllegalArgumentException.class) public void futureJavaIsNotSilentlyDowngraded() {
        GameLaunchSetting.selectJavaRuntime(26);
    }
    @Test public void unknownRuntimeIsNotJava25() {
        assertEquals(-1, GameLaunchSetting.runtimeMajor("custom"));
        assertEquals(-1, GameLaunchSetting.runtimeMajor(null));
        assertEquals(25, GameLaunchSetting.runtimeMajor("JRE25"));
    }
    @Test public void javaMetadataOverridesLauncherProtocolVersion() {
        Version version = new Gson().fromJson("{\"id\":\"test\",\"minimumLauncherVersion\":0,\"javaVersion\":{\"majorVersion\":25}}", Version.class);
        assertEquals(25, GameLaunchSetting.requiredJava(version));
        assertEquals(8, GameLaunchSetting.requiredJava(new Version("legacy")));
    }
    @Test public void sdlDetectionUsesDependencyNotVersionName() {
        Gson gson = JsonUtils.defaultGsonBuilder().registerTypeAdapter(Artifact.class, new Artifact.Serializer()).create();
        Version sdl = gson.fromJson("{\"id\":\"renamed-game\",\"libraries\":[{\"name\":\"org.lwjgl:lwjgl-sdl:3.4.1\"}]}", Version.class);
        Version glfw = gson.fromJson("{\"id\":\"26.3\",\"libraries\":[{\"name\":\"org.lwjgl:lwjgl-glfw:3.3.3\"}]}", Version.class);
        assertTrue(GameLaunchSetting.requiresSdl(sdl));
        assertFalse(GameLaunchSetting.requiresSdl(glfw));
    }
    @Test public void minecraft26FallbackCoversReleaseAndTestIds() {
        String[] ids = {"26.1", "26.1.1", "26.1-snapshot-1", "26.3-pre-2", "26.3-rc-1"};
        for (String id : ids) assertEquals(25, GameLaunchSetting.requiredJava(new Version(id)));
        assertEquals(8, GameLaunchSetting.requiredJava(new Version("my-26.1-world")));
        Version explicit = new Gson().fromJson("{\"id\":\"26.1\",\"javaVersion\":{\"majorVersion\":26}}", Version.class);
        assertEquals(26, GameLaunchSetting.requiredJava(explicit));
    }
    @Test public void renamedMinecraftAndGamePatchRetainJava25() {
        Gson gson = new Gson();
        Version child = gson.fromJson("{\"id\":\"custom\",\"inheritsFrom\":\"26.1\"}", Version.class);
        Version patched = gson.fromJson("{\"id\":\"custom\",\"patches\":[{\"id\":\"game\",\"version\":\"26.3-snapshot-4\"}]}", Version.class);
        Version unrelated = gson.fromJson("{\"id\":\"forge\",\"version\":\"26.1\"}", Version.class);
        assertEquals(25, GameLaunchSetting.requiredJava(child));
        assertEquals(25, GameLaunchSetting.requiredJava(patched));
        assertEquals(8, GameLaunchSetting.requiredJava(unrelated));
    }
    @Test public void controllerSdlAlongsideGlfwIsNotWindowSdl() {
        Gson gson = JsonUtils.defaultGsonBuilder().registerTypeAdapter(Artifact.class, new Artifact.Serializer()).create();
        Version controllers = gson.fromJson("{\"id\":\"modded\",\"libraries\":[{\"name\":\"org.lwjgl:lwjgl-glfw:3.4.1\"}],\"patches\":[{\"id\":\"controller\",\"libraries\":[{\"name\":\"org.lwjgl:lwjgl-sdl:3.4.1\"}]}]}", Version.class);
        Version sdlPatch = gson.fromJson("{\"id\":\"modded\",\"patches\":[{\"id\":\"game\",\"libraries\":[{\"name\":\"org.lwjgl:lwjgl-sdl:3.4.1\"}]}]}", Version.class);
        assertFalse(GameLaunchSetting.requiresSdl(controllers));
        assertTrue(GameLaunchSetting.requiresSdl(sdlPatch));
    }
    @Test public void fallbackKeepsMirrorFirstThenUsesOriginal() {
        com.qcl.launcher.launcher.list.install.DownloadTaskListBean bean =
                new com.qcl.launcher.launcher.list.install.DownloadTaskListBean("client", "https://mirror.test/client", "client.jar", "hash")
                        .withFallback("https://original.test/client");
        assertEquals("https://mirror.test/client", bean.urlForAttempt(0));
        assertEquals("https://mirror.test/client", bean.urlForAttempt(1));
        assertEquals("https://original.test/client", bean.urlForAttempt(2));
        assertEquals("https://original.test/client", bean.urlForAttempt(4));
        bean.withFallback(bean.url);
        assertNull(bean.fallbackUrl);
        assertEquals(bean.url, bean.urlForAttempt(4));
    }
    @Test public void thirdPartyLibraryUrlAndMissingTrailingSlashArePreserved() {
        Gson gson = JsonUtils.defaultGsonBuilder().registerTypeAdapter(Artifact.class, new Artifact.Serializer()).create();
        com.qcl.launcher.launcher.game.Library library = gson.fromJson("{\"name\":\"example:mod:1\",\"url\":\"https://repo.example.test/maven\"}", com.qcl.launcher.launcher.game.Library.class);
        assertEquals("https://repo.example.test/maven/example/mod/1/mod-1.jar", library.getDownload().getUrl());
        assertEquals(library.getDownload().getUrl(), DownloadUrlSource.replaceSubUrl(library.getDownload().getUrl(), 1, 5));
    }
    @Test public void officialSelectionDoesNotRewrite() {
        String url = "https://piston-meta.mojang.com/v1/packages/hash/test.json";
        assertEquals(url, DownloadUrlSource.replaceSubUrl(url, 0, 1));
    }
    @Test public void mirrorUsesHostnameAndPreservesPathAndQuery() {
        assertEquals("https://bmclapi2.bangbang93.com/v1/packages/hash/test.json?x=1", DownloadUrlSource.replaceSubUrl("https://piston-meta.mojang.com/v1/packages/hash/test.json?x=1", 1, 1));
        assertEquals("https://bmclapi2.bangbang93.com/v1/objects/hash/client.jar", DownloadUrlSource.replaceSubUrl("https://piston-data.mojang.com/v1/objects/hash/client.jar", 2, 2));
    }
    @Test public void retiredSourceIndexUsesBmclapi() {
        assertEquals(DownloadUrlSource.getSubUrl(1, 0), DownloadUrlSource.getSubUrl(2, 0));
        assertTrue(DownloadUrlSource.getSubUrl(2, 0).endsWith("version_manifest_v2.json"));
    }
    @Test public void assetsAndMavenHaveExactlyOnePrefix() {
        assertEquals("https://bmclapi2.bangbang93.com/assets/ab/hash", DownloadUrlSource.replaceSubUrl("https://resources.download.minecraft.net/ab/hash", 1, 4));
        assertEquals("https://bmclapi2.bangbang93.com/maven/a/b.jar", DownloadUrlSource.replaceSubUrl("https://files.minecraftforge.net/maven/a/b.jar", 1, 6));
    }
    @Test public void unrelatedAndMalformedAddressesRemainUnmodified() {
        String[] urls = {"https://files.betacraft.uk/launcher/assets/versions/test.jar", "https://piston-meta.mojang.com.example.net/test", "https://bmclapi2.bangbang93.com/v1/test", "invalid url"};
        for (String url : urls) assertEquals(url, DownloadUrlSource.replaceSubUrl(url, 1, 1));
        assertNull(DownloadUrlSource.replaceSubUrl(null, 1, 1));
    }
    @Test public void orderingIsNewestFirstNullLastAndStableForTies() {
        VersionManifest manifest = new VersionManifest(null, null);
        VersionManifest.Version old = manifest.new Version("old", "old_alpha", "https://example.test/old", null, new Date(100));
        VersionManifest.Version first = manifest.new Version("first", "snapshot", "https://example.test/1", null, new Date(200));
        VersionManifest.Version second = manifest.new Version("second", "snapshot", "https://example.test/2", null, new Date(200));
        VersionManifest.Version unknown = manifest.new Version("unknown", "release", "https://example.test/3", null, null);
        ArrayList<VersionManifest.Version> list = new ArrayList<>(Arrays.asList(old, unknown, first, second));
        VersionManifest.sortNewestFirst(list);
        assertEquals(Arrays.asList(first, second, old, unknown), list);
    }
}
