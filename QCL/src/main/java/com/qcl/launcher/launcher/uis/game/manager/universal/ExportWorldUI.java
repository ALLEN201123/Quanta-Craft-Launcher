package com.qcl.launcher.launcher.uis.game.manager.universal;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.dialogs.LoadingDialog;
import com.qcl.launcher.launcher.game.World;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;
import com.qcl.launcher.utils.file.UriUtils;
import com.tungsten.filepicker.Constants;
import com.tungsten.filepicker.FolderChooser;
import java.io.File;
import java.io.IOException;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class ExportWorldUI extends BaseUI implements View.OnClickListener {
    public static final int EXPORT_WORLD_REQUEST = 3099;
    private ImageButton editPath;
    private Button export;
    private TextView exportPath;
    public LinearLayout exportWorldUI;
    private TextView gameVersion;
    public World world;
    private TextView worldName;

    public ExportWorldUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onCreate() {
        super.onCreate();
        this.exportWorldUI = (LinearLayout) this.activity.findViewById(R.id.ui_export_world);
        this.exportPath = (TextView) this.activity.findViewById(R.id.export_world_path);
        this.editPath = (ImageButton) this.activity.findViewById(R.id.edit_world_export_path);
        this.worldName = (TextView) this.activity.findViewById(R.id.export_world_name);
        this.gameVersion = (TextView) this.activity.findViewById(R.id.export_world_game_version);
        this.export = (Button) this.activity.findViewById(R.id.export_world);
        this.editPath.setOnClickListener(this);
        this.export.setOnClickListener(this);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStart() {
        super.onStart();
        this.activity.showBarTitle(this.context.getResources().getString(R.string.export_world_ui_title) + " " + this.world.getWorldName(), false, true);
        CustomAnimationUtils.showViewFromLeft(this.exportWorldUI, this.activity, this.context, true);
        init();
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.exportWorldUI, this.activity, this.context, true);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (i == 3099 && i2 == -1 && intent != null) {
            this.exportPath.setText(UriUtils.getRealPathFromUri_AboveApi19(this.context, intent.getData()) + "/" + this.world.getFileName() + ".zip");
        }
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.editPath) {
            Intent intent = new Intent(this.context, (Class<?>) FolderChooser.class);
            intent.putExtra("SELECTION_MODE", Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal());
            intent.putExtra("INITIAL_DIRECTORY", new File(AppManifest.LAUNCHER_DIR).getAbsolutePath());
            this.activity.startActivityForResult(intent, 3099);
        }
        if (view == this.export) {
            if (!new File(this.exportPath.getText().toString()).exists()) {
                final LoadingDialog loadingDialog = new LoadingDialog(this.context);
                loadingDialog.setLoadingText(this.context.getString(R.string.dialog_export_world_text));
                loadingDialog.show();
                new Thread(new Runnable() { // from class: com.qcl.launcher.launcher.uis.game.manager.universal.ExportWorldUI$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        ExportWorldUI.this.m543x7f65960b(loadingDialog);
                    }
                }).start();
                return;
            }
            Toast.makeText(this.context, "file already exist!", 0).show();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onClick$3$com-qcl-launcher-launcher-uis-game-manager-universal-ExportWorldUI, reason: not valid java name */
    public /* synthetic */ void m543x7f65960b(final LoadingDialog loadingDialog) {
        try {
            this.world.export(this.exportPath.getText().toString().substring(0, this.exportPath.getText().toString().lastIndexOf("/")), this.exportPath.getText().toString().substring(this.exportPath.getText().toString().lastIndexOf("/") + 1));
            this.activity.runOnUiThread(new Runnable() { // from class: com.qcl.launcher.launcher.uis.game.manager.universal.ExportWorldUI$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    ExportWorldUI.this.m541x46899d4d(loadingDialog);
                }
            });
        } catch (IOException e) {
            this.activity.runOnUiThread(new Runnable() { // from class: com.qcl.launcher.launcher.uis.game.manager.universal.ExportWorldUI$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    ExportWorldUI.this.m542xe2f799ac(loadingDialog, e);
                }
            });
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onClick$1$com-qcl-launcher-launcher-uis-game-manager-universal-ExportWorldUI, reason: not valid java name */
    public /* synthetic */ void m541x46899d4d(LoadingDialog loadingDialog) {
        loadingDialog.dismiss();
        AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
        builder.setTitle(this.context.getString(R.string.dialog_export_world_title));
        builder.setMessage(this.context.getString(R.string.dialog_export_world_msg));
        builder.setPositiveButton(this.context.getString(R.string.dialog_export_world_positive), new DialogInterface.OnClickListener() { // from class: com.qcl.launcher.launcher.uis.game.manager.universal.ExportWorldUI$$ExternalSyntheticLambda0
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                ExportWorldUI.this.m540xaa1ba0ee(dialogInterface, i);
            }
        });
        builder.create().show();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onClick$0$com-qcl-launcher-launcher-uis-game-manager-universal-ExportWorldUI, reason: not valid java name */
    public /* synthetic */ void m540xaa1ba0ee(DialogInterface dialogInterface, int i) {
        this.activity.backToLastUI();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onClick$2$com-qcl-launcher-launcher-uis-game-manager-universal-ExportWorldUI, reason: not valid java name */
    public /* synthetic */ void m542xe2f799ac(LoadingDialog loadingDialog, IOException iOException) {
        loadingDialog.dismiss();
        Toast.makeText(this.context, iOException.toString(), 0).show();
    }

    private void init() {
        this.exportPath.setText(AppManifest.LAUNCHER_DIR + "/" + this.world.getWorldName() + ".zip");
        this.worldName.setText(this.world.getWorldName());
        this.gameVersion.setText(this.world.getGameVersion() == null ? "" : this.world.getGameVersion());
    }
}
