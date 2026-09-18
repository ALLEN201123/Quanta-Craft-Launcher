package com.qcl.launcher.auth.yggdrasil;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.qcl.launcher.auth.AuthenticationException;
import com.qcl.launcher.auth.ServerDisconnectException;
import com.qcl.launcher.auth.ServerResponseMalformedException;
import com.qcl.launcher.utils.Lang;
import com.qcl.launcher.utils.Pair;
import com.qcl.launcher.utils.gson.UUIDTypeAdapter;
import com.qcl.launcher.utils.gson.ValidationTypeAdapterFactory;
import com.qcl.launcher.utils.io.FileUtils;
import com.qcl.launcher.utils.io.HttpMultipartRequest;
import com.qcl.launcher.utils.io.NetworkUtils;
import com.qcl.launcher.utils.string.StringUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/* loaded from: classes2.dex */
public class YggdrasilService {
    public static final String MIGRATION_FAQ_URL = "https://help.minecraft.net/hc/en-us/articles/360050865492-JAVA-Account-Migration-FAQ";
    public static final String PROFILE_URL = "https://www.minecraft.net/profile";
    public static final String PURCHASE_URL = "https://www.minecraft.net/zh-hans/store/minecraft-java-edition";
    private final YggdrasilProvider provider;
    private static final ThreadPoolExecutor POOL = Lang.threadPool("YggdrasilProfileProperties", true, 2, 10, TimeUnit.SECONDS);
    public static final YggdrasilService MOJANG = new YggdrasilService(new MojangYggdrasilProvider());
    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(UUID.class, UUIDTypeAdapter.INSTANCE).registerTypeAdapterFactory(ValidationTypeAdapterFactory.INSTANCE).create();

    public YggdrasilService(YggdrasilProvider yggdrasilProvider) {
        this.provider = yggdrasilProvider;
    }

    public YggdrasilSession authenticate(String str, String str2, String str3) throws AuthenticationException {
        Objects.requireNonNull(str);
        Objects.requireNonNull(str2);
        Objects.requireNonNull(str3);
        HashMap hashMap = new HashMap();
        hashMap.put("agent", Lang.mapOf(Pair.pair("name", "Minecraft"), Pair.pair("version", 1)));
        hashMap.put("username", str);
        hashMap.put("password", str2);
        hashMap.put("clientToken", str3);
        hashMap.put("requestUser", true);
        return handleAuthenticationResponse(request(this.provider.getAuthenticationURL(), hashMap), str3);
    }

    private static Map<String, Object> createRequestWithCredentials(String str, String str2) {
        HashMap hashMap = new HashMap();
        hashMap.put("accessToken", str);
        hashMap.put("clientToken", str2);
        return hashMap;
    }

    public YggdrasilSession refresh(String str, String str2, GameProfile gameProfile) throws AuthenticationException {
        Objects.requireNonNull(str);
        Objects.requireNonNull(str2);
        Map<String, Object> createRequestWithCredentials = createRequestWithCredentials(str, str2);
        createRequestWithCredentials.put("requestUser", true);
        if (gameProfile != null) {
            createRequestWithCredentials.put("selectedProfile", Lang.mapOf(Pair.pair("id", gameProfile.getId()), Pair.pair("name", gameProfile.getName())));
        }
        YggdrasilSession handleAuthenticationResponse = handleAuthenticationResponse(request(this.provider.getRefreshmentURL(), createRequestWithCredentials), str2);
        if (gameProfile == null || (handleAuthenticationResponse.getSelectedProfile() != null && handleAuthenticationResponse.getSelectedProfile().getId().equals(gameProfile.getId()))) {
            return handleAuthenticationResponse;
        }
        throw new ServerResponseMalformedException("Failed to select character");
    }

    public boolean validate(String str) throws AuthenticationException {
        return validate(str, null);
    }

    public boolean validate(String str, String str2) throws AuthenticationException {
        Objects.requireNonNull(str);
        try {
            requireEmpty(request(this.provider.getValidationURL(), createRequestWithCredentials(str, str2)));
            return true;
        } catch (RemoteAuthenticationException e) {
            if ("ForbiddenOperationException".equals(e.getRemoteName())) {
                return false;
            }
            throw e;
        }
    }

    public void invalidate(String str) throws AuthenticationException {
        invalidate(str, null);
    }

    public void invalidate(String str, String str2) throws AuthenticationException {
        Objects.requireNonNull(str);
        requireEmpty(request(this.provider.getInvalidationURL(), createRequestWithCredentials(str, str2)));
    }

    public void uploadSkin(UUID uuid, String str, String str2, Path path) throws AuthenticationException, UnsupportedOperationException {
        try {
            HttpURLConnection createHttpConnection = NetworkUtils.createHttpConnection(this.provider.getSkinUploadURL(uuid));
            createHttpConnection.setRequestMethod("PUT");
            createHttpConnection.setRequestProperty("Authorization", "Bearer " + str);
            createHttpConnection.setDoOutput(true);
            HttpMultipartRequest httpMultipartRequest = new HttpMultipartRequest(createHttpConnection);
            try {
                httpMultipartRequest.param("model", str2);
                InputStream newInputStream = Files.newInputStream(path, new OpenOption[0]);
                try {
                    httpMultipartRequest.file("file", FileUtils.getName(path), "image/" + FileUtils.getExtension(path), newInputStream);
                    if (newInputStream != null) {
                        newInputStream.close();
                    }
                    httpMultipartRequest.close();
                    requireEmpty(NetworkUtils.readData(createHttpConnection));
                } finally {
                }
            } finally {
            }
        } catch (IOException e) {
            throw new AuthenticationException(e);
        }
    }

    public Optional<CompleteGameProfile> getCompleteGameProfile(UUID uuid) throws AuthenticationException {
        Objects.requireNonNull(uuid);
        return Optional.ofNullable((CompleteGameProfile) fromJson(request(this.provider.getProfilePropertiesURL(uuid), null), CompleteGameProfile.class));
    }

    public static Optional<Map<TextureType, Texture>> getTextures(CompleteGameProfile completeGameProfile) throws ServerResponseMalformedException {
        Objects.requireNonNull(completeGameProfile);
        String str = completeGameProfile.getProperties().get("textures");
        if (str != null) {
            try {
                return Optional.ofNullable(((TextureResponse) fromJson(new String(Base64.getDecoder().decode(str), StandardCharsets.UTF_8), TextureResponse.class)).textures);
            } catch (IllegalArgumentException e) {
                throw new ServerResponseMalformedException(e);
            }
        }
        return Optional.empty();
    }

    private static YggdrasilSession handleAuthenticationResponse(String str, String str2) throws AuthenticationException {
        AuthenticationResponse authenticationResponse = (AuthenticationResponse) fromJson(str, AuthenticationResponse.class);
        handleErrorMessage(authenticationResponse);
        return new YggdrasilSession(authenticationResponse.clientToken, authenticationResponse.accessToken, authenticationResponse.selectedProfile, authenticationResponse.availableProfiles == null ? null : Collections.unmodifiableList(authenticationResponse.availableProfiles), authenticationResponse.user == null ? null : authenticationResponse.user.getProperties());
    }

    private static void requireEmpty(String str) throws AuthenticationException {
        if (StringUtils.isBlank(str)) {
            return;
        }
        handleErrorMessage((ErrorResponse) fromJson(str, ErrorResponse.class));
    }

    private static void handleErrorMessage(ErrorResponse errorResponse) throws AuthenticationException {
        if (!StringUtils.isBlank(errorResponse.error)) {
            throw new RemoteAuthenticationException(errorResponse.error, errorResponse.errorMessage, errorResponse.cause);
        }
    }

    private static String request(URL url, Object obj) throws AuthenticationException {
        try {
            if (obj == null) {
                return NetworkUtils.doGet(url);
            }
            return NetworkUtils.doPost(url, obj instanceof String ? (String) obj : GSON.toJson(obj), "application/json");
        } catch (IOException e) {
            throw new ServerDisconnectException(e);
        }
    }

    private static <T> T fromJson(String str, Class<T> cls) throws ServerResponseMalformedException {
        try {
            return (T) GSON.fromJson(str, (Class) cls);
        } catch (JsonParseException e) {
            throw new ServerResponseMalformedException(str, e);
        }
    }

    /* loaded from: classes2.dex */
    private static class TextureResponse {
        public Map<TextureType, Texture> textures;

        private TextureResponse() {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class AuthenticationResponse extends ErrorResponse {
        public String accessToken;
        public List<GameProfile> availableProfiles;
        public String clientToken;
        public GameProfile selectedProfile;
        public User user;

        private AuthenticationResponse() {
            super();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class ErrorResponse {
        public String cause;
        public String error;
        public String errorMessage;

        private ErrorResponse() {
        }
    }
}
