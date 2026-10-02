package com.qcl.launcher.launcher.dialogs.account;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.qcl.launcher.R;
import com.qcl.launcher.auth.Account;
import com.qcl.launcher.auth.microsoft.MinecraftSkinService;
import com.qcl.launcher.auth.offline.OfflineSkinSetting;
import com.qcl.launcher.auth.yggdrasil.TextureModel;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.utils.file.UriUtils;
import com.qcl.launcher.utils.gson.JsonUtils;
import com.tungsten.filepicker.Constants;
import com.tungsten.filepicker.FileChooser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * ★★★ 1.4.1：皮肤库（QCL 自研，未照搬第三方实现）
 *
 * 设计目标：让「任何类型账户」都能从同一个库里挑皮肤——
 *   1. 微软账户（loginType == 3）：选中后直接调 MinecraftSkinService.uploadSkin 真换肤；
 *   2. 离线账户（loginType == 1）/ 第三方账户（loginType == 4）：
 *      选中后写入 account.offlineSkinSetting（本地皮肤文件），回调交给账户列表保存。
 *
 * 皮肤来源（全部是公开且长期可用的接口，不依赖任何需要密钥的私有 API）：
 *   a. 正版玩家名  → api.mojang.com 取 UUID → sessionserver.mojang.com 取 textures 里的 SKIN.url
 *   b. 皮肤图片直链（http/https）
 *   c. 本地 png 文件（走 FileChooser）
 *   d. 我的收藏（本地 JSON 持久化，下次打开仍在）
 */
public class SkinLibraryDialog extends Dialog implements View.OnClickListener {

    private static final String LIB_DIR = AppManifest.ACCOUNT_DIR + "/skin_library";
    private static final String FAV_JSON = LIB_DIR + "/favorites.json";
    private static final int REQ_LOCAL_SKIN = 9600;
    private static final int COLS = 4;
    private static final int AVATAR_PX = 96;

    private static SkinLibraryDialog instance;

    /** 一条皮肤记录（收藏用，字段名即 JSON 键，勿随意改） */
    public static class SkinEntry {
        public String name;
        public String url;
        public boolean slim;
        public String file;

        public SkinEntry() {
        }

        public SkinEntry(String name, String url, boolean slim) {
            this.name = name;
            this.url = url;
            this.slim = slim;
        }
    }

    private final Context context;
    private final MainActivity activity;
    private final Account account;
    private final SkinPreviewDialog.OfflineSkinCallback callback;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final List<SkinEntry> shown = new ArrayList<>();
    private final List<SkinEntry> favorites = new ArrayList<>();
    private final Map<String, Bitmap> bitmapCache = new HashMap<>();

    private EditText searchInput;
    private CheckBox slimBox;
    private LinearLayout grid;
    private ImageView preview;
    private TextView status;
    private Button applyBtn;
    private Button favBtn;

    private SkinEntry selected;
    private boolean busy;

    public SkinLibraryDialog(Context context, MainActivity activity, Account account,
                             SkinPreviewDialog.OfflineSkinCallback callback) {
        super(context);
        this.context = context;
        this.activity = activity;
        this.account = account;
        this.callback = callback;
        setContentView(R.layout.dialog_skin_library);
        instance = this;
        init();
    }

    public static SkinLibraryDialog getInstance() {
        return instance;
    }

    private void init() {
        searchInput = findViewById(R.id.sk_search_input);
        slimBox = findViewById(R.id.sk_slim_box);
        grid = findViewById(R.id.sk_grid);
        preview = findViewById(R.id.sk_preview);
        status = findViewById(R.id.sk_status);
        applyBtn = findViewById(R.id.sk_apply);
        favBtn = findViewById(R.id.sk_favorite);

        Button searchBtn = findViewById(R.id.sk_search_btn);
        Button localBtn = findViewById(R.id.sk_local_btn);
        Button cancelBtn = findViewById(R.id.sk_cancel);
        searchBtn.setOnClickListener(this);
        localBtn.setOnClickListener(this);
        cancelBtn.setOnClickListener(this);
        applyBtn.setOnClickListener(this);
        favBtn.setOnClickListener(this);

        loadFavorites();
        shown.addAll(favorites);
        status.setText(favorites.isEmpty()
                ? context.getString(R.string.skin_library_empty)
                : context.getString(R.string.skin_library_favorites));
        renderGrid();

        // 尺寸照项目里其它皮肤对话框：宽 2/3，横屏时全高
        if (getWindow() != null) {
            android.util.DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
            int w = dm.widthPixels;
            int h = dm.heightPixels;
            getWindow().setLayout(w * 2 / 3, (h * 2 < w) ? -1 : h * 2 / 3);
        }
    }

    // =============================== 列表渲染 ===============================

    private void renderGrid() {
        grid.removeAllViews();
        LinearLayout row = null;
        for (int i = 0; i < shown.size(); i++) {
            if (i % COLS == 0) {
                row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                rp.topMargin = dp(6);
                row.setLayoutParams(rp);
                grid.addView(row);
            }
            row.addView(buildCard(shown.get(i)));
        }
    }

    private View buildCard(SkinEntry entry) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(4), 0, dp(4), 0);
        card.setLayoutParams(lp);
        card.setPadding(dp(6), dp(6), dp(6), dp(6));
        card.setBackground(roundRect(entry == selected ? 0xFF3D6EF5 : 0x22FFFFFF));

        ImageView icon = new ImageView(context);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(AVATAR_PX, AVATAR_PX);
        icon.setLayoutParams(ip);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        card.addView(icon);

        TextView label = new TextView(context);
        label.setText(entry.name);
        label.setTextSize(11f);
        label.setTextColor(0xFFE8ECF3);
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        card.addView(label);

        // 缩略图异步加载（缓存命中则立即显示）
        Bitmap cached = bitmapCache.get(cacheKey(entry));
        if (cached != null) {
            icon.setImageBitmap(cached);
        } else {
            icon.setImageResource(R.drawable.skin_steve);
            loadThumb(entry, icon);
        }

        card.setOnClickListener(v -> select(entry));
        return card;
    }

    private void select(SkinEntry entry) {
        selected = entry;
        if (slimBox != null) {
            slimBox.setChecked(entry.slim);
        }
        Bitmap bmp = bitmapCache.get(cacheKey(entry));
        if (bmp != null) {
            preview.setImageBitmap(bmp);
            preview.setBackgroundColor(Color.TRANSPARENT);
        } else {
            preview.setBackgroundColor(0xFF1B2029);
        }
        status.setText(entry.name + (entry.slim ? "  ·  Alex" : "  ·  Steve"));
        renderGrid();
    }

    private String cacheKey(SkinEntry entry) {
        return entry.file != null ? entry.file : String.valueOf(entry.url);
    }

    private void loadThumb(SkinEntry entry, ImageView target) {
        new Thread(() -> {
            try {
                Bitmap skin = skinBitmap(entry);
                if (skin == null) {
                    return;
                }
                bitmapCache.put(cacheKey(entry), skin);
                Bitmap head = headOf(skin);
                handler.post(() -> {
                    target.setImageBitmap(head);
                    if (entry == selected) {
                        preview.setImageBitmap(skin);
                        preview.setBackgroundColor(Color.TRANSPARENT);
                    }
                });
            } catch (Throwable ignored) {
            }
        }).start();
    }

    /** 取整个皮肤图（缓存优先，其次本地文件，最后网络）；结果会缓存 */
    private Bitmap skinBitmap(SkinEntry entry) throws IOException {
        Bitmap cached = bitmapCache.get(cacheKey(entry));
        if (cached != null) {
            return cached;
        }
        if (entry.file != null) {
            File f = new File(entry.file);
            if (f.exists()) {
                return BitmapFactory.decodeFile(entry.file);
            }
        }
        if (entry.url == null || entry.url.isEmpty()) {
            return null;
        }
        byte[] bytes = httpGetBytes(entry.url);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    /** 从 64x64 / 64x32 皮肤里裁出 8x8 头部并放大，作为缩略图 */
    private Bitmap headOf(Bitmap skin) {
        try {
            if (skin.getWidth() < 16 || skin.getHeight() < 16) {
                return skin;
            }
            Bitmap head = Bitmap.createBitmap(skin, 8, 8, 8, 8);
            return Bitmap.createScaledBitmap(head, AVATAR_PX, AVATAR_PX, false);
        } catch (Throwable t) {
            return skin;
        }
    }

    // =============================== 搜索 ===============================

    @Override
    public void onClick(View v) {
        if (v == findViewById(R.id.sk_search_btn)) {
            doSearch();
        } else if (v == findViewById(R.id.sk_local_btn)) {
            pickLocal();
        } else if (v == findViewById(R.id.sk_cancel)) {
            dismiss();
        } else if (v == findViewById(R.id.sk_apply)) {
            applySelected();
        } else if (v == findViewById(R.id.sk_favorite)) {
            addFavorite();
        }
    }

    private void doSearch() {
        if (busy) {
            return;
        }
        String query = searchInput.getText().toString().trim();
        if (query.isEmpty()) {
            toast(context.getString(R.string.skin_library_need_input));
            return;
        }
        setBusy(true, context.getString(R.string.skin_library_loading));
        new Thread(() -> {
            try {
                final SkinEntry entry;
                if (query.startsWith("http://") || query.startsWith("https://")) {
                    entry = new SkinEntry(shortName(query), query, false);
                } else {
                    String skinUrl = fetchMojangSkinUrl(query);
                    if (skinUrl == null) {
                        handler.post(() -> {
                            setBusy(false, context.getString(R.string.skin_library_not_found));
                            toast(context.getString(R.string.skin_library_not_found));
                        });
                        return;
                    }
                    boolean slim = fetchMojangSlim(query);
                    entry = new SkinEntry(query, skinUrl, slim);
                }
                // 先下载一次，确保这条记录可用（也顺便验证链接有效性）
                Bitmap bmp = skinBitmap(entry);
                if (bmp == null) {
                    throw new IOException("empty image");
                }
                handler.post(() -> {
                    setBusy(false, context.getString(R.string.skin_library_search_done));
                    shown.add(0, entry);
                    renderGrid();
                    select(entry);
                });
            } catch (Throwable e) {
                handler.post(() -> {
                    setBusy(false, context.getString(R.string.skin_library_failed) + " " + e.getMessage());
                    toast(context.getString(R.string.skin_library_failed) + " " + e.getMessage());
                });
            }
        }).start();
    }

    private String shortName(String url) {
        try {
            String n = url.substring(url.lastIndexOf('/') + 1);
            return n.length() > 14 ? n.substring(0, 14) : n;
        } catch (Throwable t) {
            return "URL";
        }
    }

    /**
     * api.mojang.com 取 UUID → sessionserver.mojang.com 取 base64 textures → SKIN.url
     * 该接口为 Mojang 官方公开接口，无需任何密钥。
     */
    private String fetchMojangSkinUrl(String name) throws IOException {
        String profile = httpGetString("https://api.mojang.com/users/profiles/minecraft/"
                + URLEncoder.encode(name, "UTF-8"));
        JsonObject profileJson = JsonParser.parseString(profile).getAsJsonObject();
        if (profileJson == null || !profileJson.has("id")) {
            return null;
        }
        String uuid = profileJson.get("id").getAsString();
        String session = httpGetString("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
        JsonObject sessionJson = JsonParser.parseString(session).getAsJsonObject();
        if (sessionJson == null || !sessionJson.has("properties")) {
            return null;
        }
        JsonArray properties = sessionJson.getAsJsonArray("properties");
        if (properties == null || properties.size() == 0) {
            return null;
        }
        String value = properties.get(0).getAsJsonObject().get("value").getAsString();
        String decoded = new String(Base64.decode(value, Base64.DEFAULT), "UTF-8");
        JsonObject root = JsonParser.parseString(decoded).getAsJsonObject();
        JsonObject skin = root.getAsJsonObject("textures").getAsJsonObject("SKIN");
        if (skin == null) {
            return null;
        }
        String url = skin.get("url").getAsString();
        if (url.startsWith("http://")) {
            url = url.replaceFirst("http://", "https://");
        }
        return url;
    }

    /** 顺带读一下这条皮肤的 model 是否为 slim（细手臂） */
    private boolean fetchMojangSlim(String name) {
        try {
            String profile = httpGetString("https://api.mojang.com/users/profiles/minecraft/"
                    + URLEncoder.encode(name, "UTF-8"));
            String uuid = JsonParser.parseString(profile).getAsJsonObject().get("id").getAsString();
            String session = httpGetString("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
            String value = JsonParser.parseString(session).getAsJsonObject()
                    .getAsJsonArray("properties").get(0).getAsJsonObject().get("value").getAsString();
            String decoded = new String(Base64.decode(value, Base64.DEFAULT), "UTF-8");
            JsonObject skin = JsonParser.parseString(decoded).getAsJsonObject()
                    .getAsJsonObject("textures").getAsJsonObject("SKIN");
            if (skin != null && skin.has("metadata")) {
                return "slim".equals(skin.getAsJsonObject("metadata").get("model").getAsString());
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private void pickLocal() {
        Intent intent = new Intent(context, FileChooser.class);
        intent.putExtra(Constants.SELECTION_MODE, Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal());
        intent.putExtra(Constants.ALLOWED_FILE_EXTENSIONS, "png");
        intent.putExtra(Constants.INITIAL_DIRECTORY, Environment.getExternalStorageDirectory().getAbsolutePath());
        activity.startActivityForResult(intent, REQ_LOCAL_SKIN);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_LOCAL_SKIN && resultCode == -1 && data != null) {
            Uri uri = data.getData();
            String path = UriUtils.getRealPathFromUri_AboveApi19(context, uri);
            if (path == null) {
                return;
            }
            File f = new File(path);
            SkinEntry entry = new SkinEntry(f.getName(), null, false);
            entry.file = path;
            shown.add(0, entry);
            renderGrid();
            select(entry);
        }
    }

    // =============================== 应用 / 收藏 ===============================

    private void addFavorite() {
        if (selected == null) {
            toast(context.getString(R.string.skin_library_pick_first));
            return;
        }
        for (SkinEntry e : favorites) {
            if (e.name != null && e.name.equals(selected.name)) {
                toast(context.getString(R.string.skin_library_already_fav));
                return;
            }
        }
        SkinEntry copy = new SkinEntry(selected.name, selected.url, selected.slim);
        copy.file = selected.file;
        favorites.add(0, copy);
        saveFavorites();
        toast(context.getString(R.string.skin_library_added_fav));
    }

    private void applySelected() {
        if (busy) {
            return;
        }
        if (selected == null) {
            toast(context.getString(R.string.skin_library_pick_first));
            return;
        }
        final SkinEntry entry = selected;
        final boolean slim = slimBox != null && slimBox.isChecked();
        entry.slim = slim;
        setBusy(true, context.getString(R.string.skin_library_applying));
        new Thread(() -> {
            try {
                File local = ensureLocalFile(entry);
                String model = slim ? "slim" : "classic";
                if (account != null && account.loginType == 3) {
                    // 微软账户：真正上传换肤
                    MinecraftSkinService.uploadSkin(account.auth_access_token, model, local);
                    handler.post(() -> {
                        setBusy(false, context.getString(R.string.skin_library_ms_uploaded));
                        toast(context.getString(R.string.skin_library_ms_uploaded));
                        dismiss();
                    });
                } else {
                    // 离线 / 第三方账户：写本地皮肤设置，交给账户列表保存
                    final OfflineSkinSetting setting = new OfflineSkinSetting(3,
                            slim ? TextureModel.ALEX : TextureModel.STEVE,
                            local.getAbsolutePath(), "", "");
                    handler.post(() -> {
                        setBusy(false, context.getString(R.string.skin_library_applied));
                        if (callback != null) {
                            callback.onPositive(setting);
                        }
                        toast(context.getString(R.string.skin_library_applied));
                        dismiss();
                    });
                }
            } catch (Throwable e) {
                handler.post(() -> {
                    setBusy(false, context.getString(R.string.skin_library_failed) + " " + e.getMessage());
                    toast(context.getString(R.string.skin_library_failed) + " " + e.getMessage());
                });
            }
        }).start();
    }

    /** 保证皮肤已落到本地缓存文件，返回该文件 */
    private File ensureLocalFile(SkinEntry entry) throws IOException {
        File dir = new File(LIB_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("cannot create " + LIB_DIR);
        }
        if (entry.file != null) {
            File f = new File(entry.file);
            if (f.exists()) {
                return f;
            }
        }
        byte[] bytes;
        if (entry.url != null && !entry.url.isEmpty()) {
            bytes = httpGetBytes(entry.url);
        } else {
            throw new IOException("no skin source");
        }
        String fileName = (entry.name == null ? "skin" : entry.name.replaceAll("[^A-Za-z0-9_\\-]", "_"))
                + "_" + Integer.toHexString(entry.url.hashCode()) + ".png";
        File out = new File(dir, fileName);
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(out)) {
            fos.write(bytes);
        }
        entry.file = out.getAbsolutePath();
        return out;
    }

    private void loadFavorites() {
        try {
            File f = new File(FAV_JSON);
            if (!f.exists()) {
                return;
            }
            Type type = new TypeToken<ArrayList<SkinEntry>>() {
            }.getType();
            List<SkinEntry> list = JsonUtils.GSON.fromJson(new FileReader(f), type);
            if (list != null) {
                favorites.addAll(list);
            }
        } catch (Throwable ignored) {
        }
    }

    private void saveFavorites() {
        try {
            File dir = new File(LIB_DIR);
            if (!dir.exists() && !dir.mkdirs()) {
                return;
            }
            try (FileWriter writer = new FileWriter(FAV_JSON)) {
                JsonUtils.GSON.toJson(favorites, writer);
            }
        } catch (Throwable ignored) {
        }
    }

    // =============================== 工具 ===============================

    private void setBusy(boolean value, String text) {
        busy = value;
        if (applyBtn != null) {
            applyBtn.setEnabled(!value);
        }
        if (favBtn != null) {
            favBtn.setEnabled(!value);
        }
        if (status != null && text != null) {
            status.setText(text);
        }
    }

    private void toast(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable roundRect(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(8));
        return d;
    }

    private String httpGetString(String url) throws IOException {
        return new String(httpGetBytes(url), "UTF-8");
    }

    private byte[] httpGetBytes(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(20000);
        conn.setDoInput(true);
        conn.setRequestProperty("User-Agent", "QCL-SkinLibrary/1.0");
        try {
            conn.connect();
            try (InputStream in = conn.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                return out.toByteArray();
            }
        } finally {
            conn.disconnect();
        }
    }

    @Override
    public void dismiss() {
        if (instance == this) {
            instance = null;
        }
        super.dismiss();
    }
}
