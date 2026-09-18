package com.qcl.launcher.launcher.uis.universal.setting.right.launcher;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.update.UpdateChecker;
import com.qcl.launcher.utils.LocaleUtils;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;
import com.qcl.launcher.utils.animation.HiddenAnimationUtils;
import com.qcl.launcher.utils.file.FileUtils;
import com.qcl.launcher.utils.file.UriUtils;
import com.qcl.launcher.utils.gson.GsonUtils;
import com.tungsten.filepicker.Constants;
import com.tungsten.filepicker.FolderChooser;
import java.io.File;
import java.util.ArrayList;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class UniversalSettingUI extends BaseUI implements View.OnClickListener, AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener, TextWatcher {
    private static final int PICK_CACHE_FOLDER_REQUEST = 1002;
    private TextView cachePathText;
    private LinearLayout cacheSetting;
    private int cacheSettingHeight;
    private RadioButton checkBeta;
    private RadioButton checkCustom;
    private RadioButton checkDefault;
    private RadioButton checkRelease;
    private Button clearCache;
    private EditText editCacheContent;
    private Button exportLog;
    private ImageButton selectCachePath;
    private ImageView showCache;
    private LinearLayout showCacheSetting;
    private ImageView showUpdate;
    private LinearLayout showUpdateSetting;
    private Spinner switchLang;
    public LinearLayout universalSettingUI;
    private UpdateChecker.UpdateCallback updateCallback;
    private LinearLayout updateSetting;
    private int updateSettingHeight;
    private TextView updateStateText;

    @Override // android.text.TextWatcher
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override // android.widget.AdapterView.OnItemSelectedListener
    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    @Override // android.text.TextWatcher
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    public UniversalSettingUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
        this.updateCallback = new UpdateChecker.UpdateCallback() { // from class: com.qcl.launcher.launcher.uis.universal.setting.right.launcher.UniversalSettingUI.1
            @Override // com.qcl.launcher.update.UpdateChecker.UpdateCallback
            public void onCheck() {
                UniversalSettingUI.this.updateStateText.setText(UniversalSettingUI.this.context.getString(R.string.universal_setting_ui_update_state_checking));
            }

            @Override // com.qcl.launcher.update.UpdateChecker.UpdateCallback
            public void onFinish(boolean z) {
                if (z) {
                    UniversalSettingUI.this.updateStateText.setText(UniversalSettingUI.this.context.getString(R.string.universal_setting_ui_update_state_latest));
                } else {
                    UniversalSettingUI.this.updateStateText.setText(UniversalSettingUI.this.context.getString(R.string.universal_setting_ui_update_state_update));
                    UniversalSettingUI.this.updateStateText.setTextColor(-65536);
                }
            }
        };
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onCreate() {
        super.onCreate();
        this.universalSettingUI = (LinearLayout) this.activity.findViewById(R.id.ui_setting_universal);
        this.showUpdateSetting = (LinearLayout) this.activity.findViewById(R.id.show_update_setting);
        this.updateStateText = (TextView) this.activity.findViewById(R.id.update_state_text);
        this.showUpdate = (ImageView) this.activity.findViewById(R.id.show_update);
        this.showCacheSetting = (LinearLayout) this.activity.findViewById(R.id.show_cache_setting);
        this.cachePathText = (TextView) this.activity.findViewById(R.id.cache_content_text);
        this.showCache = (ImageView) this.activity.findViewById(R.id.show_cache);
        this.clearCache = (Button) this.activity.findViewById(R.id.clear_cache);
        this.switchLang = (Spinner) this.activity.findViewById(R.id.language_spinner);
        this.exportLog = (Button) this.activity.findViewById(R.id.export_log);
        this.showUpdateSetting.setOnClickListener(this);
        this.showCacheSetting.setOnClickListener(this);
        this.clearCache.setOnClickListener(this);
        this.exportLog.setOnClickListener(this);
        this.updateSetting = (LinearLayout) this.activity.findViewById(R.id.update_setting);
        this.checkRelease = (RadioButton) this.activity.findViewById(R.id.update_to_rec);
        this.checkBeta = (RadioButton) this.activity.findViewById(R.id.update_to_beta);
        this.checkRelease.setOnCheckedChangeListener(this);
        this.checkBeta.setOnCheckedChangeListener(this);
        this.cacheSetting = (LinearLayout) this.activity.findViewById(R.id.cache_setting);
        this.checkDefault = (RadioButton) this.activity.findViewById(R.id.check_default_cache_path);
        this.checkCustom = (RadioButton) this.activity.findViewById(R.id.check_custom_cache_path);
        this.editCacheContent = (EditText) this.activity.findViewById(R.id.edit_cache_path);
        this.selectCachePath = (ImageButton) this.activity.findViewById(R.id.select_cache_path);
        this.checkDefault.setOnCheckedChangeListener(this);
        this.checkCustom.setOnCheckedChangeListener(this);
        this.editCacheContent.addTextChangedListener(this);
        this.selectCachePath.setOnClickListener(this);
        ArrayList arrayList = new ArrayList();
        arrayList.add(this.context.getString(R.string.universal_setting_ui_lang_sys));
        arrayList.add("English");
        arrayList.add("简体中文");
        arrayList.add("繁體中文");
        this.switchLang.setAdapter((SpinnerAdapter) new ArrayAdapter(this.context, R.layout.item_spinner, arrayList));
        this.switchLang.setSelection(this.context.getSharedPreferences("lang", 0).getInt("lang", 0));
        this.switchLang.setOnItemSelectedListener(this);
        this.updateSetting.post(new Runnable() { // from class: com.qcl.launcher.launcher.uis.universal.setting.right.launcher.UniversalSettingUI$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                UniversalSettingUI.this.m583xce4888e9();
            }
        });
        this.cacheSetting.post(new Runnable() { // from class: com.qcl.launcher.launcher.uis.universal.setting.right.launcher.UniversalSettingUI$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                UniversalSettingUI.this.m584x75c462aa();
            }
        });
        this.activity.updateChecker.check(this.activity.launcherSetting.getBetaVersion, this.updateCallback);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onCreate$0$com-qcl-launcher-launcher-uis-universal-setting-right-launcher-UniversalSettingUI, reason: not valid java name */
    public /* synthetic */ void m583xce4888e9() {
        this.updateSettingHeight = this.updateSetting.getHeight();
        this.updateSetting.setVisibility(8);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onCreate$1$com-qcl-launcher-launcher-uis-universal-setting-right-launcher-UniversalSettingUI, reason: not valid java name */
    public /* synthetic */ void m584x75c462aa() {
        this.cacheSettingHeight = this.cacheSetting.getHeight();
        this.cacheSetting.setVisibility(8);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStart() {
        super.onStart();
        CustomAnimationUtils.showViewFromLeft(this.universalSettingUI, this.activity, this.context, false);
        if (this.activity.isLoaded) {
            this.activity.uiManager.settingUI.startUniversalSettingUI.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_white));
        }
        if (this.activity.launcherSetting.getBetaVersion) {
            this.checkBeta.setChecked(true);
            this.checkRelease.setChecked(false);
        } else {
            this.checkBeta.setChecked(false);
            this.checkRelease.setChecked(true);
        }
        if (this.activity.launcherSetting.cachePath.equals(AppManifest.DEFAULT_CACHE_DIR)) {
            this.checkDefault.setChecked(true);
        } else {
            this.checkCustom.setChecked(true);
        }
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.universalSettingUI, this.activity, this.context, false);
        if (this.activity.isLoaded) {
            this.activity.uiManager.settingUI.startUniversalSettingUI.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_parent));
        }
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (i == 1002 && intent != null && i2 == -1) {
            this.editCacheContent.setText(UriUtils.getRealPathFromUri_AboveApi19(this.context, intent.getData()));
        }
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.showUpdateSetting) {
            HiddenAnimationUtils.newInstance(this.context, this.updateSetting, this.showUpdate, this.updateSettingHeight).toggle();
        }
        if (view == this.showCacheSetting) {
            HiddenAnimationUtils.newInstance(this.context, this.cacheSetting, this.showCache, this.cacheSettingHeight).toggle();
        }
        if (view == this.clearCache) {
            FileUtils.deleteDirectory(AppManifest.DEFAULT_CACHE_DIR);
        }
        if (view == this.selectCachePath) {
            Intent intent = new Intent(this.context, (Class<?>) FolderChooser.class);
            intent.putExtra("SELECTION_MODE", Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal());
            intent.putExtra("INITIAL_DIRECTORY", new File(Environment.getExternalStorageDirectory().getAbsolutePath()).getAbsolutePath());
            this.activity.startActivityForResult(intent, 1002);
        }
    }

    @Override // android.widget.AdapterView.OnItemSelectedListener
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
        if (adapterView != this.switchLang || this.context.getSharedPreferences("lang", 0).getInt("lang", 0) == i) {
            return;
        }
        LocaleUtils.changeLanguage(this.context, i);
        this.activity.recreate();
    }

    @Override // android.widget.CompoundButton.OnCheckedChangeListener
    public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
        if (compoundButton == this.checkRelease && z) {
            this.checkBeta.setChecked(false);
            this.activity.launcherSetting.getBetaVersion = false;
            GsonUtils.saveLauncherSetting(this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
            this.activity.updateChecker.check(false, this.updateCallback);
        }
        if (compoundButton == this.checkBeta && z) {
            this.checkRelease.setChecked(false);
            this.activity.launcherSetting.getBetaVersion = true;
            GsonUtils.saveLauncherSetting(this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
            this.activity.updateChecker.check(true, this.updateCallback);
        }
        if (compoundButton == this.checkDefault && z) {
            this.checkCustom.setChecked(false);
            this.editCacheContent.setEnabled(false);
            this.selectCachePath.setEnabled(false);
            this.activity.launcherSetting.cachePath = AppManifest.DEFAULT_CACHE_DIR;
            GsonUtils.saveLauncherSetting(this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
            this.cachePathText.setText(this.activity.launcherSetting.cachePath);
            this.editCacheContent.setText(this.activity.launcherSetting.cachePath);
        }
        if (compoundButton == this.checkCustom && z) {
            this.checkDefault.setChecked(false);
            this.editCacheContent.setEnabled(true);
            this.selectCachePath.setEnabled(true);
            this.cachePathText.setText(this.activity.launcherSetting.cachePath);
            this.editCacheContent.setText(this.activity.launcherSetting.cachePath);
        }
    }

    @Override // android.text.TextWatcher
    public void afterTextChanged(Editable editable) {
        this.activity.launcherSetting.cachePath = this.editCacheContent.getText().toString();
        GsonUtils.saveLauncherSetting(this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
        this.cachePathText.setText(this.activity.launcherSetting.cachePath);
    }
}
