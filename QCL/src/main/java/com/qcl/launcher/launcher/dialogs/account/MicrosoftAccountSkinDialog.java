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
import com.qcl.launcher.skin.GameCharacter;
import com.qcl.launcher.skin.MinecraftSkinRenderer;
import com.qcl.launcher.skin.SkinGLSurfaceView;
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
 * 复用 QCL 的 MinecraftSkinRenderer 做 3D 预览；通过 MinecraftSkinService 调微软皮肤/披风 API。
 * 微软 Minecraft access token 取自 account.auth_access_token。
 */
public class MicrosoftAccountSkinDialog extends Dialog implements View.OnClickListener {

    private static final int SELECT_SKIN_REQUEST = 9500;

    private static MicrosoftAccountSkinDialog instance;

    private final MainActivity activity;
    private final Account account;
    private final MinecraftSkinRenderer renderer;
    private final Handler handler = new Handler();

    private LinearLayout skinParentView;
    private LinearLayout capeListLayout;
    private ProgressBar progressBar;
    private Button selectSkin;
    private Button resetSkin;
    private Button hideCape;

    private String selectedSkinPath;
    private String model = "classic"; // classic / slim，随 RadioButton 切换
    private android.widget.RadioButton modelClassic;
    private android.widget.RadioButton modelSlim;

    public MicrosoftAccountSkinDialog(Context context, MainActivity activity, Account account) {
        super(context);
        this.activity = activity;
        this.account = account;
        this.renderer = new MinecraftSkinRenderer(context, R.drawable.skin_steve, false);
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

        // ★★★ 白色背景（人物预览背景改白）
        renderer.setBackgroundColor(1.0f, 1.0f, 1.0f, 1.0f);

        // 皮肤模型选择（classic / slim），切换时重建 3D 人物模型
        modelClassic = findViewById(R.id.ms_model_classic);
        modelSlim = findViewById(R.id.ms_model_slim);
        modelClassic.setOnClickListener(v -> {
            model = "classic";
            renderer.mCharacter = new GameCharacter(false);
        });
        modelSlim.setOnClickListener(v -> {
            model = "slim";
            renderer.mCharacter = new GameCharacter(true);
        });

        // 3D 预览视图
        SkinGLSurfaceView view = new SkinGLSurfaceView(getContext());
        view.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        view.setRenderer(renderer, 5.0f);
        view.setRenderMode(1);
        view.setPreserveEGLContextOnPause(true);
        skinParentView.addView(view);

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
                renderer.mCharacter = new GameCharacter(slim);
                previewSkin(currentSkin);
            } catch (Throwable ignored) {
            }
        }

        loadCapes();
    }

    /** 用皮肤 Bitmap 更新 3D 预览（自动处理 old format 规范化） */
    private void previewSkin(Bitmap bitmap) {
        try {
            NormalizedSkin normalized = new NormalizedSkin(bitmap);
            renderer.updateTexture(normalized.isOldFormat()
                    ? normalized.getNormalizedTexture() : normalized.getOriginalTexture(), null);
        } catch (InvalidSkinException e) {
            renderer.updateTexture(bitmap, null);
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
                        btn.setOnClickListener(v -> activateCape(cape.id));
                        capeListLayout.addView(btn);
                    }
                });
            } catch (Throwable ignored) {
                // 披风列表加载失败不影响换肤
            }
        }).start();
    }

    private void activateCape(String capeId) {
        setLoading(true);
        new Thread(() -> {
            try {
                MinecraftSkinService.showCape(account.auth_access_token, capeId);
                handler.post(() -> {
                    setLoading(false);
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
        new Thread(() -> {
            try {
                File file = new File(path);
                MinecraftSkinService.uploadSkin(account.auth_access_token, model, file);
                handler.post(() -> {
                    setLoading(false);
                    // ★★★ 上传成功后，左边 3D 人物跟着变成新皮肤
                    try {
                        previewSkin(BitmapFactory.decodeFile(path));
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

    /** 简单模型检测：宽度 64 高度 64 视为 slim，否则 classic */
    private String detectModel(File file) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
            if (opts.outWidth == 64 && opts.outHeight == 64) {
                return "slim";
            }
        } catch (Throwable ignored) {
        }
        return "classic";
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
            if (skinParentView != null && skinParentView.getChildCount() > 0
                    && skinParentView.getChildAt(0) instanceof SkinGLSurfaceView) {
                ((SkinGLSurfaceView) skinParentView.getChildAt(0)).onPause();
            }
        } catch (Throwable ignored) {
        }
        super.dismiss();
    }
}
