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
import com.qcl.launcher.auth.yggdrasil.Texture;
import com.qcl.launcher.auth.yggdrasil.TextureType;
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
        // ★★★★★ 2026-10-11 修（用户实测「点确定之后没有立刻上传，再进来又变回苗条」）：
        //
        //   照 FCL 的语义改 —— FCL 的 MicrosoftAccountSkinDialog 是：
        //       binding.skinFilePick -> 选文件后**只更新预览**，不上传
        //       binding.upload       -> uploadFromFile()   ← 点这个才上传
        //       binding.positive     -> dismiss()          ← 「确定」只是关窗
        //   也就是说 FCL 有**独立的「上传」按钮**。
        //
        //   QCL 原来把上传塞在"选完文件就自动传"，而「确定」只是关窗 ⇒
        //   玩家改完模型单选钮再点「确定」，**什么都没提交**，下次开窗又从像素重判 ⇒
        //   "上次选经典，结果又变回苗条"。
        //
        //   现在把「确定」直接改成**上传按钮**（文案见 layout 里的 dialog_upload）：
        //   点它 = 把当前选择（皮肤文件 + 经典/苗条）提交上去，成功后再关窗。
        findViewById(R.id.ms_skin_positive).setOnClickListener(v -> commitSkin());

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

        // ★★★ 加载当前微软账号的皮肤显示在 3D 预览，并确定用哪套模型
        if (account.texture != null && !account.texture.isEmpty()) {
            try {
                Bitmap currentSkin = Avatar.stringToBitmap(account.texture);
                // ★★★★★ 2026-10-11 修（用户实测「上次选史蒂夫经典，再点进来又变回艾利克斯苗条」）：
                //   **优先用玩家上次保存的选择**（account.model），只有老账号没有该字段时
                //   才回退到"按皮肤像素判定"。
                //   原来无条件用 NormalizedSkin(...).isSlim() 覆盖单选钮 ⇒
                //   玩家选 classic，只要图片右臂是 3 列，下次开窗又被判成 slim，选择被吃掉。
                boolean slim;
                if (account.model != null) {
                    slim = (account.model == com.qcl.launcher.auth.yggdrasil.TextureModel.ALEX);
                } else {
                    try {
                        slim = new NormalizedSkin(currentSkin).isSlim();
                    } catch (InvalidSkinException e) {
                        slim = false;
                    }
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
     *
     * <p>★★★ 2026-10-11：**没有就主动去微软拉一次**（用户实测「对话框里背后连个毛的披风都没有，
     * 只有主界面人物背后有」）。
     *
     * <p>为什么必须补拉：{@code account.capeTexture} 只在**登录那一刻**（三个登录入口）
     * 和 **MainUI 的后台补拉**里才会被赋值。老账号登录时披风功能还不存在 ⇒ 字段是空的；
     * 主界面碰巧显示，是因为它进页面时会后台补拉一次；
     * 而**皮肤对话框打开时那次补拉往往还没回来** ⇒ 对话框里永远是空的。
     *
     * <p>现在的行为：先用现有的（有就立刻显示），没有就**本对话框自己拉一次**，
     * 拉到后回填 + 存盘 + 立刻重画（拉不到就静默，玩家没披风是正常的）。
     */
    private void loadCurrentCapeForPreview() {
        try {
            if (account.capeTexture != null && !account.capeTexture.trim().isEmpty()) {
                Bitmap cape = Avatar.stringToBitmap(account.capeTexture);
                if (cape != null) {
                    setCapeBitmap(cape);
                }
                return;
            }
        } catch (Throwable ignored) {
        }
        fetchCapeFromMicrosoft();
    }

    /** 是否已经在为本对话框拉披风（防重复请求）。 */
    private volatile boolean capeFetching;

    /**
     * 直接调微软接口拉一次披风：MinecraftProfile → 披风纹理 → base64 → 回填账号 + 重画预览。
     * 全程后台线程，失败静默（不弹 Toast 打扰玩家）。
     */
    private void fetchCapeFromMicrosoft() {
        if (capeFetching || account == null) {
            return;
        }
        final String token = account.auth_access_token;
        if (token == null || token.trim().isEmpty()) {
            return;
        }
        capeFetching = true;
        new Thread(() -> {
            try {
                // ★ 写法完全照抄 MainUI.fetchMicrosoftCapeOnce：
                //   profile 用 "Bearer"，纹理直接交给 Account.downloadTextureAsBase64，
                //   不要自己拼 URL —— Texture 类没有 getUrl()（我先前写错，编译报"找不到符号"）。
                Msa.MinecraftProfileResponse profile = Msa.getMinecraftProfile("Bearer", token);
                if (profile == null) {
                    return;
                }
                java.util.Map<TextureType, Texture> textures = Msa.getTextures(profile).orElse(null);
                if (textures == null) {
                    return;
                }
                String capeB64 = Account.downloadTextureAsBase64(textures.get(TextureType.CAPE));
                if (capeB64 == null || capeB64.trim().isEmpty()) {
                    return;
                }
                final Bitmap cape = Avatar.stringToBitmap(capeB64);
                handler.post(() -> {
                    if (cape == null) {
                        return;
                    }
                    account.capeTexture = capeB64;
                    saveAccountAndRefresh();
                    setCapeBitmap(cape);
                });
            } catch (Throwable ignored) {
                // 拉不到就算了，不打扰玩家
            } finally {
                capeFetching = false;
            }
        }, "qcl-cape-fetch").start();
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
        // ★★★★★ 2026-10-11 关键修复（用户实测「只有主界面显示披风，皮肤对话框里没有」）：
        //   **时序问题** —— init() 里 `previewSkin()` / `setCapeBitmap()` 都在 `show()` 之前跑，
        //   那时候帧循环还没启动（`onResume()` 上面这行才启动），
        //   于是那一次 `updateTexture(skin, cape, slim)` **没被 GL 线程处理**。
        //   结果就是：人物后来靠其它更新显示了，但**披风没跟上** ⇒ 对话框里背后永远是空的。
        //
        //   修法：`onResume()` 之后**重新把 (皮肤, 披风) 成对应用一次**。
        //   （这里再调一次 loadCurrentCapeForPreview 是无害的幂等操作，
        //     且能顺带把"进对话框后主界面刚补拉回来的披风"也带上。）
        if (!isCapeRestored) {
            isCapeRestored = true;
            loadCurrentCapeForPreview();
            if (lastPreviewBitmap != null) {
                previewSkin(lastPreviewBitmap);
            }
        }
    }

    /** show() 之后是否已经重放过一次披风（避免重复重放）。 */
    private boolean isCapeRestored;

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
            // ★★★★★ 2026-10-11 修复（用户实测「上传完之后主界面人物没啥变化」）：
            //   原来这里用的是 **Java 引用比较 `account == activity.publicGameSetting.account`**
            //   —— 只有两边是**同一个对象实例**时才刷主界面。
            //   而对话框拿到的 account 常常是列表里的另一个等价实例（Gson 反序列化 / 列表重建），
            //   引用不同 ⇒ **主界面永远不刷新** ⇒ "上传完没变化"。
            //
            //   现在改成"**等价即刷新**"：UUID 相同、或（UUID 都空时）名字相同就刷新，
            //   并兜底到"任何情况下都刷一次"——多刷一次只是重画人物，无副作用。
            boolean shouldRefresh = false;
            final com.qcl.launcher.auth.Account current =
                    (activity.publicGameSetting == null) ? null : activity.publicGameSetting.account;
            if (account != null && current != null) {
                if (account == current) {
                    shouldRefresh = true;
                } else {
                    String a = account.auth_uuid;
                    String b = current.auth_uuid;
                    if (a != null && !a.trim().isEmpty() && a.equals(b)) {
                        shouldRefresh = true;
                    } else if ((a == null || a.trim().isEmpty())
                            && account.auth_player_name != null
                            && account.auth_player_name.equals(current.auth_player_name)) {
                        shouldRefresh = true;
                    }
                }
            }
            if (activity.uiManager.mainUI != null) {
                try {
                    activity.uiManager.mainUI.refreshAccountModel();
                } catch (Throwable ignored2) {
                }
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
                        // ★★★★★ 2026-10-11 修（用户实测「披风切换按钮颜色与其他按钮不一致」）：
                        //   这些按钮是**运行时 new 出来的**，而 SkinDialogUtils.tintAll 只在 init()
                        //   里遍历一次静态布局 ⇒ **动态新增的披风按钮从来没被染色**，
                        //   于是用系统默认 Button 样式，跟旁边那些深色主题按钮明显不一样。
                        //   这里照静态按钮同一套做法补染：
                        //     ① 背景用同一个 qcl_dialog_button（日月两套 shape 已配好）
                        //     ② 文字用同一个 qcl_dialog_btn_text
                        //     ③ 再用 SkinDialogUtils.tintButton 按当前主题色上色
                        try {
                            btn.setTextSize(15f);
                            btn.setBackgroundResource(com.qcl.launcher.R.drawable.qcl_dialog_button);
                            btn.setTextColor(getContext().getResources()
                                    .getColor(com.qcl.launcher.R.color.qcl_dialog_btn_text));
                            com.qcl.launcher.utils.string.SkinDialogUtils.tintButton(
                                    getContext(), btn, themeColorNow());
                        } catch (Throwable ignored) {
                            // 染不上就退化成普通按钮，不影响功能
                        }
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
                        // ★★★★★ 2026-10-11：标记"刚本地改过披风" ⇒ 主界面在保护期内
                        //   不会用服务端（还没生效的旧披风）覆盖刚切好的这条，
                        //   否则会出现"对话框显示新披风、主界面显示旧披风"的不一致。
                        try {
                            com.qcl.launcher.launcher.uis.main.MainUI.markLocalCapeChanged();
                        } catch (Throwable ignored) {
                        }
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
                        // ★ 2026-10-11：同上 —— 刚本地隐藏了披风，保护期内别被服务端旧数据回填
                        try {
                            com.qcl.launcher.launcher.uis.main.MainUI.markLocalCapeChanged();
                        } catch (Throwable ignored) {
                        }
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

    /**
     * ★★★★★ 2026-10-11 新增：「上传」按钮（原「确定」）被点击时的提交动作。
     *
     * <p>照 FCL 的 `binding.upload -> uploadFromFile()` 语义：
     * <ul>
     *   <li>玩家选过皮肤文件 ⇒ 用**当前单选钮的模型**重新上传一次（把选择真正提交上去）；</li>
     *   <li>没选过文件、只改了模型单选钮 ⇒ 把模型选择**写进账号并存盘**
     *       （皮肤纹理用现有的，不重传图片）；</li>
     *   <li>提交完成后关窗。</li>
     * </ul>
     *
     * <p>为什么要这一步（用户实测「点确定没立刻上传，再进来又变回苗条」）：
     * 模型选择此前**根本没被保存过**，每次开窗都用像素重新判定 ⇒ 玩家的选择被吃掉。
     */
    private void commitSkin() {
        // 先把模型选择落到账号上（无论有没有换图，这一步都做）
        final String chosenModel = modelSlim.isChecked() ? "slim" : "classic";
        this.model = chosenModel;
        try {
            if (account != null) {
                account.model = "slim".equals(chosenModel)
                        ? com.qcl.launcher.auth.yggdrasil.TextureModel.ALEX
                        : com.qcl.launcher.auth.yggdrasil.TextureModel.STEVE;
                saveAccountAndRefresh();
            }
        } catch (Throwable ignored) {
        }

        // 选过文件 ⇒ 连图和模型一起重新上传（这才是"实时上传"）
        if (selectedSkinPath != null && !selectedSkinPath.trim().isEmpty()) {
            uploadSkin(selectedSkinPath);
            // 上传是异步的；上传成功后会自己 toast + 刷新预览。
            // 这里不立刻 dismiss，等玩家看到"皮肤已上传"再手动关（照 FCL 的两段式体验）。
            return;
        }

        // 没选文件：只是改了模型偏好 ⇒ 已存盘，直接关窗即可
        Toast.makeText(getContext(), R.string.microsoft_skin_model_saved, Toast.LENGTH_SHORT).show();
        dismiss();
    }

    /**
     * ★ 2026-10-11：取当前主题色设置字符串（给动态创建的按钮染色用）。
     *
     * <p>口径与 init() 里那次 {@code tintAll(...)} 完全一致 ——
     * 都读 {@code activity.launcherSetting.launcherTheme}，
     * 这样**动态加的按钮和静态按钮颜色必然一样**（用户要求"跟主题色一模一样"）。
     * 读不到就返回 null，由 {@link com.qcl.launcher.utils.string.SkinDialogUtils}
     * 回退到项目默认 colorAccent（它的 resolveThemeColor 已处理）。
     */
    private String themeColorNow() {
        try {
            if (activity != null && activity.launcherSetting != null) {
                return activity.launcherSetting.launcherTheme;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private void uploadSkin(String path) {        setLoading(true);
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
                        // ★★★★★ 2026-10-11：标记"刚刚本地换过皮肤" ⇒ 主界面在保护期内
                        //   **不会**用服务端（还没处理完的旧皮肤）覆盖这张新皮肤，
                        //   否则会出现"对话框显示新皮肤、主界面显示旧皮肤"的不一致。
                        //   （照 FCL 的做法：上传后刻意不重新拉服务端预览。）
                        try {
                            if (activity != null && activity.uiManager != null
                                    && activity.uiManager.mainUI != null) {
                                com.qcl.launcher.launcher.uis.main.MainUI.markLocalSkinUploaded();
                            }
                        } catch (Throwable ignored) {
                        }
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
