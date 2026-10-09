package com.qcl.launcher.launcher.dialogs.account;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.qcl.launcher.R;
import com.qcl.launcher.auth.Account;
import com.qcl.launcher.auth.microsoft.MinecraftSkinService;
import com.qcl.launcher.auth.microsoft.Msa;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.skin.gltf.SkinRenderer;
import com.qcl.launcher.skin.gltf.SkinViewer;
import com.qcl.launcher.skin.utils.Avatar;
import com.qcl.launcher.skin.utils.InvalidSkinException;
import com.qcl.launcher.skin.utils.NormalizedSkin;
import com.qcl.launcher.utils.file.UriUtils;
import com.tungsten.filepicker.Constants;
import com.tungsten.filepicker.FileChooser;

import java.io.File;
import java.util.List;

/**
 * ★★★ 1.1.6：微软账号本地换皮（照搬 FCL 的 MicrosoftAccountSkinDialog）。
 * 预览用主界面同款 glTF 管线（SkinViewer + SkinRenderer）；通过 MinecraftSkinService 调微软皮肤/披风 API。
 * 微软 Minecraft access token 取自 account.auth_access_token。
 */
public class MicrosoftAccountSkinDialog extends Dialog implements View.OnClickListener {

    private static final int SELECT_SKIN_REQUEST = 9500;

    private static MicrosoftAccountSkinDialog instance;

    private final MainActivity activity;
    private final Account account;
    // ★★★ 1.5.0：老 `MinecraftSkinRenderer + GameCharacter` 全站弃用，改用主界面同款 glTF 管线。
    private final SkinRenderer renderer;
    private SkinViewer skinViewer;
    private final Handler handler = new Handler();

    private LinearLayout skinParentView;
    private LinearLayout capeListLayout;
    private ProgressBar progressBar;
    private Button selectSkin;
    private Button resetSkin;
    private Button hideCape;

    private String selectedSkinPath;
    private String model = "classic"; // classic / slim，随 RadioButton 切换
    /** ★ 1.5.0：记住最后一次预览的皮肤，供 classic/slim 切换时重新贴图 */
    private Bitmap lastPreviewBitmap;
    private android.widget.RadioButton modelClassic;
    private android.widget.RadioButton modelSlim;

    public MicrosoftAccountSkinDialog(Context context, MainActivity activity, Account account) {
        super(context);
        this.activity = activity;
        this.account = account;
        this.renderer = new SkinRenderer(context);
        setContentView(R.layout.dialog_microsoft_skin);
        instance = this;
        init();
    }

    public static MicrosoftAccountSkinDialog getInstance() {
        return instance;
    }

    private void init() {
        skinParentView = findViewById(R.id.ms_skin_parent_view);
        capeListLayout = findViewById(R.id.ms_cape_list_layout);
        progressBar = findViewById(R.id.ms_skin_progress);
        selectSkin = findViewById(R.id.ms_skin_select);
        resetSkin = findViewById(R.id.ms_skin_reset);
        hideCape = findViewById(R.id.ms_cape_hide);

        selectSkin.setOnClickListener(this);
        resetSkin.setOnClickListener(this);
        hideCape.setOnClickListener(this);
        findViewById(R.id.ms_skin_negative).setOnClickListener(v -> dismiss());
        // ★ 1.5.0 用户要求：「微软账号这个只有取消，没有确定」⇒ 补上「确定」。
        //   语义：真正生效的是上面「选择皮肤」的上传动作，这里只是按常规关窗，
        //   所以与取消等价（同 dismiss），不能写成 reload —— 那样会打断正在上传的换肤。
        findViewById(R.id.ms_skin_positive).setOnClickListener(v -> dismiss());

        // ★★★ 1.5.0：所有按钮染成「深色底 + 浅色字」，底色跟随玩家主题色（见 SkinDialogUtils）
        try {
            com.qcl.launcher.utils.string.SkinDialogUtils.tintAll(getContext(),
                    (android.view.View) findViewById(android.R.id.content),
                    (activity != null && activity.launcherSetting != null)
                            ? activity.launcherSetting.launcherTheme : null);
        } catch (Throwable ignored) {
        }

        // ★★★ 人物预览背景：老管线的 `renderer.setBackgroundColor(1,1,1,1)`（GLSurfaceView 清屏用）
        //   在 glTF 管线里**没有这个方法**，背景由 SkinViewer/EGL 与布局决定。

        // 皮肤模型选择（classic / slim）。★★★ 1.5.0：glTF 管线的模型由
        //   `updateTexture(skin, cape, slim)` 自己切（SkinRenderer 内部 classic/slim 两套模型），
        //   所以这里**不再 new GameCharacter**（那是老管线的用法），只记住选择、贴图时生效。
        modelClassic = findViewById(R.id.ms_model_classic);
        modelSlim = findViewById(R.id.ms_model_slim);
        modelClassic.setOnClickListener(v -> {
            model = "classic";
            reapplyPreviewModel();
        });
        modelSlim.setOnClickListener(v -> {
            model = "slim";
            reapplyPreviewModel();
        });

        // 3D 预览视图：★★★ 1.5.0 SkinGLSurfaceView → glTF 的 SkinViewer（TextureView + EGL）
        try {
            skinViewer = new SkinViewer(getContext());
            skinViewer.setRenderer(renderer,
                    getContext().getResources().getDisplayMetrics().density);
            skinParentView.addView(skinViewer, new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT));
        } catch (Throwable t) {
            t.printStackTrace();
            skinViewer = null;
        }

        // ★★★ 尺寸照 FCL 的皮肤对话框：宽=屏幕 2/3；高=横屏时全高、竖屏时 2/3
        if (getWindow() != null) {
            android.util.DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
            int w = dm.widthPixels;
            int h = dm.heightPixels;
            int dialogW = w * 2 / 3;
            int dialogH = (h * 2 < w) ? android.view.WindowManager.LayoutParams.MATCH_PARENT : h * 2 / 3;
            getWindow().setLayout(dialogW, dialogH);
        }

        // ★★★ 加载当前微软账号的皮肤显示在 3D 预览，并自动检测是苗条还是经典
        if (account.texture != null && !account.texture.isEmpty()) {
            try {
                Bitmap currentSkin = Avatar.stringToBitmap(account.texture);
                boolean slim;
                try {
                    slim = new NormalizedSkin(currentSkin).isSlim();
                } catch (InvalidSkinException e) {
                    slim = false;
                }
                model = slim ? "slim" : "classic";
                modelSlim.setChecked(slim);
                modelClassic.setChecked(!slim);
                previewSkin(currentSkin);
            } catch (Throwable ignored) {
            }
        }

        loadCapes();
        // ★★★ 1.5.0：进对话框就把**已有披风**装上预览（照 FCL `refreshPreview()` 成对传）。
        //   原来只在用户点选披风时才更新 currentCapeBitmap ⇒ 一进来人物背后是空的。
        loadCurrentCapeForPreview();
    }

    /**
     * 载入这个账号当前生效的披风（{@code account.capeTexture}）到预览。
     * 没有就静默跳过 —— 玩家没披风是正常的。
     */
    private void loadCurrentCapeForPreview() {
        try {
            if (account.capeTexture == null || account.capeTexture.trim().isEmpty()) {
                return;
            }
            Bitmap cape = Avatar.stringToBitmap(account.capeTexture);
            if (cape != null) {
                setCapeBitmap(cape);
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void show() {
        super.show();
        // ★★★ 1.5.0：SkinViewer 只有在 `resumeRequested == true` 时才启动帧循环
        //   （见 SkinViewer.startRenderThread：surface 就绪后 `if (resumeRequested) startFrameLoop()`）
        //   ⇒ Dialog 没覆写 onResume 的话，不调这里就**只建 EGL 不渲染 → 人物一片空白**。
        //   Dialog 的 onResume 在首次 show 时**不会**被调用，所以必须在 show() 里主动起。
        if (skinViewer != null) {
            skinViewer.onResume();
        }
    }

    /**
     * ★★★ 1.5.0：把「经典 / 苗条」这个选择重新应用到当前预览皮肤。
     *   老代码这里是 `renderer.mCharacter = new GameCharacter(slim)`（重建老模型）；
     *   glTF 管线没有 mCharacter，模型由贴图更新时的 slim 参数决定
     *   ⇒ 这里只需拿当前贴图再 updateTexture 一次。
     */
    private void reapplyPreviewModel() {
        try {
            Bitmap cur = lastPreviewBitmap;
            if (cur == null && account != null) {
                cur = Avatar.stringToBitmap(account.texture);
            }
            if (cur != null) {
                previewSkin(cur);
            }
        } catch (Throwable ignored) {
        }
    }

    // ★ 1.5.0：当前预览用的披风（贴图）。
    private Bitmap currentCapeBitmap;

    /** 用皮肤 Bitmap 更新 3D 预览（自动处理 old format 规范化；披风沿用当前那张） */
    private void previewSkin(Bitmap bitmap) {
        if (bitmap == null) return;
        lastPreviewBitmap = bitmap;
        // ★★★ 1.5.0 照 FCL `updatePreviewFromFile()`：预览是 **成对**传的 ——
        //   换皮肤时**保留当前披风**：`renderer.setTexture(normalized, currentCape)`。
        //   原来这里一直写 `null` ⇒ 人物背后永远是空的（用户实测「一点披风都看不出来」）。
        boolean useSlim = ("slim".equals(model)) || (model == null && isSkinSlim(bitmap));
        try {
            NormalizedSkin normalized = new NormalizedSkin(bitmap);
            renderer.updateTexture(normalized.isOldFormat()
                    ? normalized.getNormalizedTexture() : normalized.getOriginalTexture(),
                    currentCapeBitmap, useSlim);
        } catch (InvalidSkinException e) {
            renderer.updateTexture(bitmap, currentCapeBitmap, useSlim);
        }
    }

    /** 记住当前披风并重画（选中披风时调用）。 */
    private void setCapeBitmap(Bitmap cape) {
        this.currentCapeBitmap = cape;
        if (lastPreviewBitmap != null) {
            previewSkin(lastPreviewBitmap);
        }
    }

    /**
     * ★ 1.5.0：把改过的 account 存盘，并让正在显示它的主界面刷新。
     *
     * <p>写法照 {@code AccountListAdapter}：`saveAccounts(accountUI.accounts, ACCOUNT_DIR + "/accounts.json")`。
     * <p>只在「该账号当前正被选中使用时」才顺带刷主界面人物，避免无谓的重绘。
     */
    private void saveAccountAndRefresh() {
        try {
            if (activity == null || activity.uiManager == null || activity.uiManager.accountUI == null) {
                return;
            }
            com.qcl.launcher.utils.gson.GsonUtils.saveAccounts(
                    activity.uiManager.accountUI.accounts,
                    com.qcl.launcher.manifest.AppManifest.ACCOUNT_DIR + "/accounts.json");
            // 正好是当前在用的账号 ⇒ 主界面 3D 人物要立刻跟着换
            if (account != null && account == activity.publicGameSetting.account
                    && activity.uiManager.mainUI != null) {
                activity.uiManager.mainUI.refreshAccountModel();
            }
        } catch (Throwable ignored) {
        }
    }

    /** 贴图本身是不是 slim 布局（只在玩家还没手动选过模型时用作兜底）。 */
    private boolean isSkinSlim(Bitmap bitmap) {
        try {
            return new NormalizedSkin(bitmap).isSlim();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** 加载当前账号的披风列表 */
    private void loadCapes() {
        capeListLayout.removeAllViews();
        new Thread(() -> {
            try {
                Msa.MinecraftProfileResponse profile = Msa.getMinecraftProfile("Bearer", account.auth_access_token);
                List<Msa.MinecraftProfileResponseCape> capes = profile.capes;
                handler.post(() -> {
                    if (capes == null || capes.isEmpty()) {
                        return;
                    }
                    for (Msa.MinecraftProfileResponseCape cape : capes) {
                        if (cape == null || cape.id == null) continue;
                        String label = (cape.alias != null && !cape.alias.isEmpty()) ? cape.alias : cape.id;
                        if ("ACTIVE".equals(cape.state)) {
                            label = label + " [已启用]";
                        }
                        Button btn = new Button(getContext());
                        btn.setText(label);
                        btn.setOnClickListener(v -> activateCape(cape.id, cape.url));
                        capeListLayout.addView(btn);
                    }
                });
            } catch (Throwable ignored) {
                // 披风列表加载失败不影响换肤
            }
        }).start();
    }

    private void activateCape(String capeId, final String capeUrl) {
        setLoading(true);
        new Thread(() -> {
            try {
                MinecraftSkinService.showCape(account.auth_access_token, capeId);
                // ★★★ 1.5.0：激活成功后**立刻把这条披风下载下来装到预览上**。
                //   原来只刷列表、预览仍旧是空的 ⇒ 玩家点了披风但人物背后什么都没变
                //   （用户实测「一点披风都看不出来」）。
                //   下载走 FCL 同款思路：URL → Bitmap → setCapeBitmap（内部会重画人物）。
                Bitmap capeImg = downloadBitmapFromUrl(capeUrl);
                final Bitmap cape = capeImg;
                final String capeBase64 = (cape == null) ? null : Avatar.bitmapToString(cape);
                handler.post(() -> {
                    setLoading(false);
                    if (cape != null) {
                        account.capeTexture = capeBase64;
                        saveAccountAndRefresh();
                        setCapeBitmap(cape);
                    }
                    Toast.makeText(getContext(), R.string.microsoft_cape_activated, Toast.LENGTH_SHORT).show();
                    loadCapes();
                });
            } catch (Throwable e) {
                handler.post(() -> {
                    setLoading(false);
                    Toast.makeText(getContext(), getContext().getString(R.string.message_failed) + "\n" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /** 下载一张纹理 URL 并解码为 Bitmap（http 自动升 https）；失败返回 null。 */
    private static Bitmap downloadBitmapFromUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        java.io.InputStream is = null;
        java.net.HttpURLConnection con = null;
        try {
            String u = url.trim();
            if (!u.startsWith("https")) {
                u = u.replaceFirst("http", "https");
            }
            con = (java.net.HttpURLConnection) new java.net.URL(u).openConnection();
            con.setDoInput(true);
            con.setConnectTimeout(15000);
            con.setReadTimeout(30000);
            con.connect();
            is = con.getInputStream();
            return BitmapFactory.decodeStream(is);
        } catch (Throwable ignored) {
            return null;
        } finally {
            try { if (is != null) is.close(); } catch (Throwable ignored) {}
            try { if (con != null) con.disconnect(); } catch (Throwable ignored) {}
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.ms_skin_select) {
            Intent intent = new Intent(getContext(), FileChooser.class);
            intent.putExtra("SELECTION_MODE", Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal());
            intent.putExtra(Constants.ALLOWED_FILE_EXTENSIONS, "png");
            activity.startActivityForResult(intent, SELECT_SKIN_REQUEST);
        } else if (id == R.id.ms_skin_reset) {
            setLoading(true);
            new Thread(() -> {
                try {
                    MinecraftSkinService.resetSkin(account.auth_access_token);
                    handler.post(() -> {
                        setLoading(false);
                        Toast.makeText(getContext(), R.string.microsoft_skin_reset_done, Toast.LENGTH_SHORT).show();
                    });
                } catch (Throwable e) {
                    handler.post(() -> {
                        setLoading(false);
                        Toast.makeText(getContext(), getContext().getString(R.string.message_failed) + "\n" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        } else if (id == R.id.ms_cape_hide) {
            setLoading(true);
            new Thread(() -> {
                try {
                    MinecraftSkinService.hideCape(account.auth_access_token);
                    handler.post(() -> {
                        setLoading(false);
                        // ★ 1.5.0：隐藏披风后预览也要跟着去掉（否则人物背后还挂着）
                        account.capeTexture = null;
                        saveAccountAndRefresh();
                        setCapeBitmap(null);
                        Toast.makeText(getContext(), R.string.microsoft_cape_hidden, Toast.LENGTH_SHORT).show();
                        loadCapes();
                    });
                } catch (Throwable e) {
                    handler.post(() -> {
                        setLoading(false);
                        Toast.makeText(getContext(), getContext().getString(R.string.message_failed) + "\n" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        }
    }

    /** FileChooser 返回后上传皮肤 */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_SKIN_REQUEST && resultCode == -1 && data != null) {
            Uri uri = data.getData();
            String path = UriUtils.getRealPathFromUri_AboveApi19(getContext(), uri);
            if (path == null) return;
            selectedSkinPath = path;
            uploadSkin(path);
        }
    }

    private void uploadSkin(String path) {
        setLoading(true);
        // ★★★★★ 2026-10-11 修复（用户实测「换成经典的哥哥 → 点确认 → 重启 → 又变回苗条」）：
        //   原来**上传时用皮肤像素自己判定 variant**（detectedModel），然后把单选钮
        //   **强行改成判定结果** —— 玩家明明选了 classic，只要图片的右臂是 3 列像素，
        //   就被判成 slim 并覆盖掉玩家的选择。重启后自然还是 slim，看着就是"我的选择被吃了"。
        //
        //   现在：**以玩家在单选钮上的选择为准**上传；像素判定只用于"还没选过时"的兜底。
        //   （模型由账号侧 variant 决定，玩家选了就该按玩家选的走。）
        final String chosen = modelSlim.isChecked() ? "slim" : "classic";
        new Thread(() -> {
            try {
                File file = new File(path);
                // ★ 仍然解一张 Bitmap：上传成功后要**回填本地账号并刷新预览**（见下方）
                final Bitmap skinImg = BitmapFactory.decodeFile(path);
                if (skinImg == null) {
                    throw new IllegalArgumentException("Failed to read skin image: " + path);
                }
                final String detected = chosen;
                MinecraftSkinService.uploadSkin(account.auth_access_token, detected, file);
                handler.post(() -> {
                    setLoading(false);
                    // ★ 不再回头改单选钮 —— 保留玩家自己的选择
                    if ("slim".equals(detected)) {
                        modelSlim.setChecked(true);
                        modelClassic.setChecked(false);
                    } else {
                        modelClassic.setChecked(true);
                        modelSlim.setChecked(false);
                    }
                    model = detected;
                    // ★★★ 上传成功后必须写回 account.texture 并存盘 ——
                    //   原来只刷新预览、关掉对话框就没了（用户实测「点了确定根本上传不了」）。
                    try {
                        account.texture = Avatar.bitmapToString(skinImg);
                        saveAccountAndRefresh();
                        previewSkin(skinImg);
                    } catch (Throwable ignored) {
                    }
                    Toast.makeText(getContext(), R.string.microsoft_skin_uploaded, Toast.LENGTH_SHORT).show();
                });
            } catch (Throwable e) {
                handler.post(() -> {
                    setLoading(false);
                    Toast.makeText(getContext(), getContext().getString(R.string.message_failed) + "\n" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /**
     * ★ 1.5.0 照 FCL 改：模型（classic / slim）必须**从皮肤像素真实判定**。
     *
     * <p>旧实现是错的：`opts.outWidth == 64 && outHeight == 64 → "slim"` ——
     * 但 64×64 是**所有**标准皮肤（含 classic 史蒂夫）的尺寸，
     * 于是**几乎每张皮肤都会被判成 slim** ⇒ 上传时向微软声明的 variant 常常是错的
     * ⇒ 玩家看到"皮肤没传上去 / 手臂型号不对"（用户实测）。
     *
     * <p>照 FCL {@code MicrosoftAccountSkinDialog.uploadFromFile()}：
     * {@code val skin = NormalizedSkin(skinImg); if (skin.isSlim) "slim" else "classic"}。
     */
    private String detectModel(Bitmap bitmap) {
        try {
            return new NormalizedSkin(bitmap).isSlim() ? "slim" : "classic";
        } catch (Throwable ignored) {
            return "classic";
        }
    }

    /** 旧签名（按文件）保留，内部先解码再按像素判定。 */
    private String detectModel(File file) {
        try {
            Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
            return (bmp == null) ? "classic" : detectModel(bmp);
        } catch (Throwable ignored) {
            return "classic";
        }
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        selectSkin.setEnabled(!loading);
        resetSkin.setEnabled(!loading);
        hideCape.setEnabled(!loading);
    }

    @Override
    public void dismiss() {
        instance = null;
        try {
            // ★ 1.5.0：暂停 glTF 的渲染线程 + 摘掉 TextureView，否则关窗后 EGL 线程还在跑
            if (skinViewer != null) {
                skinViewer.onPause();
            }
            if (skinParentView != null) {
                skinParentView.removeAllViews();
            }
        } catch (Throwable ignored) {
        }
        super.dismiss();
    }
}
