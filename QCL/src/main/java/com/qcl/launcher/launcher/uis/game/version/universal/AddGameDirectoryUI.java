package com.qcl.launcher.launcher.uis.game.version.universal;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.list.info.contents.ContentListBean;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;
import com.qcl.launcher.utils.file.UriUtils;
import com.qcl.launcher.utils.gson.GsonUtils;
import com.tungsten.filepicker.Constants;
import com.tungsten.filepicker.FolderChooser;
import java.io.File;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class AddGameDirectoryUI extends BaseUI implements View.OnClickListener {
    public static final int PICK_GAME_FILE_FOLDER_REQUEST = 1001;
    public LinearLayout addGameDirUI;
    private EditText editName;
    private ImageButton editPath;
    private Button save;
    private TextView selectedDir;

    public AddGameDirectoryUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onCreate() {
        super.onCreate();
        this.addGameDirUI = (LinearLayout) this.activity.findViewById(R.id.ui_add_game_directory);
        this.editName = (EditText) this.activity.findViewById(R.id.edit_content_name);
        ImageButton imageButton = (ImageButton) this.activity.findViewById(R.id.edit_content_path);
        this.editPath = imageButton;
        imageButton.setOnClickListener(this);
        this.selectedDir = (TextView) this.activity.findViewById(R.id.content_path);
        Button button = (Button) this.activity.findViewById(R.id.save_contents);
        this.save = button;
        button.setOnClickListener(this);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStart() {
        super.onStart();
        this.activity.showBarTitle(this.context.getResources().getString(R.string.add_game_dir_ui_title), canGoBackToLast(), false);
        CustomAnimationUtils.showViewFromLeft(this.addGameDirUI, this.activity, this.context, true);
        this.selectedDir.setText(AppManifest.DEFAULT_GAME_DIR);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.addGameDirUI, this.activity, this.context, true);
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.editPath) {
            Intent intent = new Intent(this.context, (Class<?>) FolderChooser.class);
            intent.putExtra("SELECTION_MODE", Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal());
            intent.putExtra("INITIAL_DIRECTORY", new File(AppManifest.DEFAULT_GAME_DIR).getAbsolutePath());
            this.activity.startActivityForResult(intent, 1001);
        }
        if (view != this.save || this.editName.getText().toString().equals("")) {
            return;
        }
        boolean z = false;
        for (int i = 0; i < this.activity.uiManager.versionListUI.contentList.size(); i++) {
            if (this.activity.uiManager.versionListUI.contentList.get(i).name.equals(this.editName.getText().toString())) {
                z = true;
            }
        }
        if (!z) {
            this.activity.uiManager.versionListUI.contentList.add(new ContentListBean(this.editName.getText().toString(), this.selectedDir.getText().toString(), false));
            GsonUtils.saveContents(this.activity.uiManager.versionListUI.contentList, AppManifest.GAME_FILE_DIRECTORY_DIR + "/game_file_directories.json");
            this.activity.backToLastUI();
            return;
        }
        Toast.makeText(this.context, this.context.getString(R.string.add_game_dir_ui_alert), 0).show();
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (i == 1001 && intent != null && i2 == -1) {
            this.selectedDir.setText(UriUtils.getRealPathFromUri_AboveApi19(this.context, intent.getData()));
        }
    }
}
