package com.qcl.launcher.launcher.uis.universal.setting.right.launcher;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.update.UpdateChecker;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;
import com.qcl.launcher.utils.gson.GsonUtils;
import java.util.ArrayList;
import java.util.Iterator;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class DownloadSettingUI extends BaseUI implements CompoundButton.OnCheckedChangeListener, SeekBar.OnSeekBarChangeListener, AdapterView.OnItemSelectedListener, TextWatcher {
    /** ★ 1.2.9：启动器设置里的「检查更新」一行 */
    private LinearLayout checkUpdateRow;
    private TextView checkUpdateState;

    private LinearLayout autoSourceLayout;
    private Spinner autoSourceSpinner;
    private CheckBox checkAutoDownload;
    private CheckBox checkAutoSelect;
    public LinearLayout downloadSettingUI;
    private EditText editTaskSize;
    private LinearLayout fixSourceLayout;
    private Spinner fixSourceSpinner;
    private LinearLayout taskSizeLayout;
    private SeekBar taskSizeSeekbar;

    @Override // android.text.TextWatcher
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override // android.widget.AdapterView.OnItemSelectedListener
    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    @Override // android.widget.SeekBar.OnSeekBarChangeListener
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override // android.widget.SeekBar.OnSeekBarChangeListener
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    @Override // android.text.TextWatcher
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    public DownloadSettingUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onCreate() {
        super.onCreate();
        this.downloadSettingUI = (LinearLayout) this.activity.findViewById(R.id.ui_setting_download);
        // ★ 1.2.9：启动器设置里的「检查更新」
        this.checkUpdateRow = (LinearLayout) this.activity.findViewById(R.id.check_update_row);
        this.checkUpdateState = (TextView) this.activity.findViewById(R.id.check_update_state);
        if (this.checkUpdateRow != null) {
            this.checkUpdateRow.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    DownloadSettingUI.this.checkUpdate();
                }
            });
        }
        this.checkAutoSelect = (CheckBox) this.activity.findViewById(R.id.auto_select_source);
        this.checkAutoDownload = (CheckBox) this.activity.findViewById(R.id.auto_select_download_num);
        this.autoSourceLayout = (LinearLayout) this.activity.findViewById(R.id.auto_source_layout);
        this.fixSourceLayout = (LinearLayout) this.activity.findViewById(R.id.fix_source_layout);
        this.taskSizeLayout = (LinearLayout) this.activity.findViewById(R.id.task_size_layout);
        this.autoSourceSpinner = (Spinner) this.activity.findViewById(R.id.auto_source_spinner);
        this.fixSourceSpinner = (Spinner) this.activity.findViewById(R.id.fix_source_spinner);
        this.taskSizeSeekbar = (SeekBar) this.activity.findViewById(R.id.task_size_seekbar);
        this.editTaskSize = (EditText) this.activity.findViewById(R.id.edit_download_task_size);
        ArrayList arrayList = new ArrayList();
        arrayList.add(this.context.getString(R.string.download_setting_ui_auto_official));
        arrayList.add(this.context.getString(R.string.download_setting_ui_auto_balance));
        arrayList.add(this.context.getString(R.string.download_setting_ui_auto_mirror));
        this.autoSourceSpinner.setAdapter((SpinnerAdapter) new ArrayAdapter(this.context, R.layout.item_spinner, arrayList));
        ArrayList arrayList2 = new ArrayList();
        arrayList2.add(this.context.getString(R.string.download_setting_ui_source_official));
        arrayList2.add(this.context.getString(R.string.download_setting_ui_source_bmclapi));
        arrayList2.add(this.context.getString(R.string.download_setting_ui_source_bmclapi));
        this.fixSourceSpinner.setAdapter((SpinnerAdapter) new ArrayAdapter(this.context, R.layout.item_spinner, arrayList2));
        this.checkAutoSelect.setChecked(this.activity.launcherSetting.downloadUrlSource.autoSelect);
        this.autoSourceSpinner.setSelection(this.activity.launcherSetting.downloadUrlSource.autoSourceType);
        this.fixSourceSpinner.setSelection(this.activity.launcherSetting.downloadUrlSource.fixSourceType);
        this.checkAutoDownload.setChecked(this.activity.launcherSetting.autoDownloadTaskQuantity);
        this.taskSizeSeekbar.setProgress(this.activity.launcherSetting.maxDownloadTask);
        this.editTaskSize.setText(Integer.toString(this.activity.launcherSetting.maxDownloadTask));
        refreshSourceLayout(this.activity.launcherSetting.downloadUrlSource.autoSelect);
        refreshSizeLayout(this.activity.launcherSetting.autoDownloadTaskQuantity);
        this.checkAutoSelect.setOnCheckedChangeListener(this);
        this.checkAutoDownload.setOnCheckedChangeListener(this);
        this.autoSourceSpinner.setOnItemSelectedListener(this);
        this.fixSourceSpinner.setOnItemSelectedListener(this);
        this.taskSizeSeekbar.setOnSeekBarChangeListener(this);
        this.editTaskSize.addTextChangedListener(this);
    }

    /**
     * ★ 1.2.9：手动检查更新（启动器设置 → 检查更新）。
     * 收不到推送的旧版本也能在这里主动点一下；有新版会直接弹更新框，没有就显示「已是最新版」。
     */
    private void checkUpdate() {
        try {
            if (this.checkUpdateState != null) {
                this.checkUpdateState.setText(R.string.setting_check_update_checking);
            }
            if (this.checkUpdateRow != null) {
                this.checkUpdateRow.setClickable(false);
            }
            if (this.activity.updateChecker == null) {
                this.activity.updateChecker = new UpdateChecker((Context) this.activity, this.activity);
            }
            this.activity.updateChecker.checkManually(new UpdateChecker.UpdateCallback() {

                @Override
                public void onCheck() {
                }

                @Override
                public void onFinish(final boolean noUpdate) {
                    DownloadSettingUI.this.activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (DownloadSettingUI.this.checkUpdateRow != null) {
                                    DownloadSettingUI.this.checkUpdateRow.setClickable(true);
                                }
                                if (DownloadSettingUI.this.checkUpdateState != null) {
                                    DownloadSettingUI.this.checkUpdateState.setText(noUpdate
                                            ? DownloadSettingUI.this.activity.getString(R.string.setting_check_update_none)
                                            : "");
                                }
                            }
                            catch (Throwable ignored) {
                            }
                        }
                    });
                }
            });
        }
        catch (Throwable ignored) {
        }
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStart() {
        super.onStart();
        CustomAnimationUtils.showViewFromLeft(this.downloadSettingUI, this.activity, this.context, false);
        if (this.activity.isLoaded) {
            this.activity.uiManager.settingUI.startDownloadSettingUI.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_white));
        }
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.downloadSettingUI, this.activity, this.context, false);
        if (this.activity.isLoaded) {
            this.activity.uiManager.settingUI.startDownloadSettingUI.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_parent));
        }
    }

    private void refreshSourceLayout(boolean z) {
        if (z) {
            Iterator<View> it = getAllChild(this.autoSourceLayout).iterator();
            while (it.hasNext()) {
                View next = it.next();
                next.setAlpha(1.0f);
                next.setEnabled(true);
            }
            Iterator<View> it2 = getAllChild(this.fixSourceLayout).iterator();
            while (it2.hasNext()) {
                View next2 = it2.next();
                next2.setAlpha(0.4f);
                next2.setEnabled(false);
            }
            return;
        }
        Iterator<View> it3 = getAllChild(this.autoSourceLayout).iterator();
        while (it3.hasNext()) {
            View next3 = it3.next();
            next3.setAlpha(0.4f);
            next3.setEnabled(false);
        }
        Iterator<View> it4 = getAllChild(this.fixSourceLayout).iterator();
        while (it4.hasNext()) {
            View next4 = it4.next();
            next4.setAlpha(1.0f);
            next4.setEnabled(true);
        }
    }

    private void refreshSizeLayout(boolean z) {
        if (z) {
            Iterator<View> it = getAllChild(this.taskSizeLayout).iterator();
            while (it.hasNext()) {
                View next = it.next();
                next.setAlpha(0.4f);
                next.setEnabled(false);
            }
            return;
        }
        Iterator<View> it2 = getAllChild(this.taskSizeLayout).iterator();
        while (it2.hasNext()) {
            View next2 = it2.next();
            next2.setAlpha(1.0f);
            next2.setEnabled(true);
        }
    }

    private ArrayList<View> getAllChild(ViewGroup viewGroup) {
        ArrayList<View> arrayList = new ArrayList<>();
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            if (viewGroup.getChildAt(i) instanceof ViewGroup) {
                arrayList.addAll(getAllChild((ViewGroup) viewGroup.getChildAt(i)));
            }
            arrayList.add(viewGroup.getChildAt(i));
        }
        return arrayList;
    }

    @Override // android.widget.CompoundButton.OnCheckedChangeListener
    public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
        if (compoundButton == this.checkAutoSelect) {
            this.activity.launcherSetting.downloadUrlSource.autoSelect = z;
            refreshSourceLayout(z);
        }
        if (compoundButton == this.checkAutoDownload) {
            this.activity.launcherSetting.autoDownloadTaskQuantity = z;
            refreshSizeLayout(z);
        }
        GsonUtils.saveLauncherSetting(this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
    }

    @Override // android.widget.SeekBar.OnSeekBarChangeListener
    public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
        if (z && seekBar == this.taskSizeSeekbar) {
            this.activity.launcherSetting.maxDownloadTask = i;
            this.editTaskSize.setText(Integer.toString(i));
        }
        GsonUtils.saveLauncherSetting(this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
    }

    @Override // android.widget.AdapterView.OnItemSelectedListener
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
        if (adapterView == this.autoSourceSpinner) {
            this.activity.launcherSetting.downloadUrlSource.autoSourceType = i;
        }
        if (adapterView == this.fixSourceSpinner) {
            this.activity.launcherSetting.downloadUrlSource.fixSourceType = i;
        }
        GsonUtils.saveLauncherSetting(this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
    }

    @Override // android.text.TextWatcher
    public void afterTextChanged(Editable editable) {
        if (this.editTaskSize.getText().toString().equals("")) {
            return;
        }
        if (Integer.parseInt(this.editTaskSize.getText().toString()) > 128) {
            this.activity.launcherSetting.maxDownloadTask = 128;
        } else {
            this.activity.launcherSetting.maxDownloadTask = Math.max(Integer.parseInt(this.editTaskSize.getText().toString()), 1);
        }
        this.taskSizeSeekbar.setProgress(this.activity.launcherSetting.maxDownloadTask);
        GsonUtils.saveLauncherSetting(this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
    }
}
