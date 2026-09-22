package com.qcl.launcher.launcher.uis.game.download.right;

import com.qcl.launcher.launcher.setting.SettingUtils;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.list.download.DownloadResourceAdapter;
import com.qcl.launcher.launcher.mod.LocalizedRemoteModRepository;
import com.qcl.launcher.launcher.mod.RemoteMod;
import com.qcl.launcher.launcher.mod.RemoteModRepository;
import com.qcl.launcher.launcher.mod.curse.CurseForgeRemoteModRepository;
import com.qcl.launcher.launcher.mod.modrinth.ModrinthRemoteModRepository;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.launcher.view.spinner.CategorySpinnerAdapter;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class DownloadPackageUI extends BaseUI implements View.OnClickListener, AdapterView.OnItemSelectedListener, TextView.OnEditorActionListener, TextWatcher {
    private ArrayList<RemoteModRepository.Category> categoryList;
    private CategorySpinnerAdapter categoryListAdapter;
    private DownloadResourceAdapter downloadPackageListAdapter;
    public LinearLayout downloadPackageUI;
    private Spinner downloadSourceSpinner;
    private Spinner editCategory;
    private EditText editName;
    private Spinner editSort;
    private EditText editVersion;
    private Spinner editVersionSpinner;
    private Button installPackage;
    private boolean isSearching;
    private ArrayList<RemoteMod> packageList;
    private ListView packageListView;
    private ProgressBar progressBar;
    private TextView refreshText;
    private RemoteModRepository repository;
    private Button search;
    private Button refresh;
    private final Handler searchHandler;
    private ArrayList<String> sortList;
    private ArrayAdapter<String> sortListAdapter;
    private ArrayList<String> sourceList;
    private ArrayAdapter<String> sourceListAdapter;
    private ArrayList<String> versionList;
    private ArrayAdapter<String> versionListAdapter;

    @Override // android.text.TextWatcher
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override // android.widget.AdapterView.OnItemSelectedListener
    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    @Override // android.text.TextWatcher
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    public DownloadPackageUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
        this.isSearching = false;
        this.searchHandler = new Handler() { // from class: com.qcl.launcher.launcher.uis.game.download.right.DownloadPackageUI.1
            @Override // android.os.Handler
            public void handleMessage(Message message) {
                super.handleMessage(message);
                if (message.what == 0) {
                    DownloadPackageUI.this.isSearching = true;
                    DownloadPackageUI.this.progressBar.setVisibility(0);
                    DownloadPackageUI.this.refreshText.setVisibility(8);
                    DownloadPackageUI.this.packageListView.setVisibility(8);
                }
                if (message.what == 1) {
                    DownloadPackageUI.this.downloadPackageListAdapter.notifyDataSetChanged();
                    DownloadPackageUI.this.categoryListAdapter.notifyDataSetChanged();
                    DownloadPackageUI.this.progressBar.setVisibility(8);
                    DownloadPackageUI.this.refreshText.setVisibility(8);
                    DownloadPackageUI.this.packageListView.setVisibility(0);
                    DownloadPackageUI.this.isSearching = false;
                }
                if (message.what == 2) {
                    DownloadPackageUI.this.progressBar.setVisibility(8);
                    DownloadPackageUI.this.refreshText.setVisibility(0);
                    DownloadPackageUI.this.packageListView.setVisibility(8);
                    DownloadPackageUI.this.isSearching = false;
                }
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class Repository extends LocalizedRemoteModRepository {
        private Repository() {
        }

        @Override // com.qcl.launcher.launcher.mod.LocalizedRemoteModRepository
        protected RemoteModRepository getBackedRemoteModRepository() {
            if (DownloadPackageUI.this.downloadSourceSpinner.getSelectedItemPosition() == 1) {
                return ModrinthRemoteModRepository.MODPACKS;
            }
            return CurseForgeRemoteModRepository.MODPACKS;
        }

        @Override // com.qcl.launcher.launcher.mod.RemoteModRepository
        public RemoteModRepository.Type getType() {
            return RemoteModRepository.Type.MODPACK;
        }
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onCreate() {
        super.onCreate();
        this.downloadPackageUI = (LinearLayout) this.activity.findViewById(R.id.ui_download_package);
        this.editName = (EditText) this.activity.findViewById(R.id.download_package_arg_name);
        this.downloadSourceSpinner = (Spinner) this.activity.findViewById(R.id.download_package_arg_source);
        this.editVersion = (EditText) this.activity.findViewById(R.id.edit_download_package_arg_version);
        this.editVersionSpinner = (Spinner) this.activity.findViewById(R.id.download_package_arg_version);
        this.editCategory = (Spinner) this.activity.findViewById(R.id.download_package_arg_type);
        this.editSort = (Spinner) this.activity.findViewById(R.id.download_package_arg_sort);
        Button button = (Button) this.activity.findViewById(R.id.install_package_from_download_page);
        this.installPackage = button;
        button.setOnClickListener(this);
        Button button2 = (Button) this.activity.findViewById(R.id.search_package);
        this.search = button2;
        // ★ 1.2.3：搜索旁的「刷新」——和搜索走同一条查询路

        this.refresh = (Button) this.activity.findViewById(R.id.refresh_package_search);

        if (this.refresh != null) {

            this.refresh.setOnClickListener((View.OnClickListener)this);

        }

        button2.setOnClickListener(this);
        ArrayList<String> arrayList = new ArrayList<>();
        this.sourceList = arrayList;
        arrayList.add(this.context.getString(R.string.download_mod_source_curse_forge));
        this.sourceList.add(this.context.getString(R.string.download_mod_source_modrinth));
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this.context, R.layout.item_spinner, this.sourceList);
        this.sourceListAdapter = arrayAdapter;
        arrayAdapter.setDropDownViewResource(R.layout.item_spinner_drop_down);
        this.downloadSourceSpinner.setAdapter((SpinnerAdapter) this.sourceListAdapter);
        this.downloadSourceSpinner.setSelection(1);
        ArrayList<String> arrayList2 = new ArrayList<>();
        this.sortList = arrayList2;
        arrayList2.add(this.context.getString(R.string.download_mod_sort_date));
        this.sortList.add(this.context.getString(R.string.download_mod_sort_heat));
        this.sortList.add(this.context.getString(R.string.download_mod_sort_recent));
        this.sortList.add(this.context.getString(R.string.download_mod_sort_name));
        this.sortList.add(this.context.getString(R.string.download_mod_sort_author));
        this.sortList.add(this.context.getString(R.string.download_mod_sort_downloads));
        this.sortList.add(this.context.getString(R.string.download_mod_sort_category));
        this.sortList.add(this.context.getString(R.string.download_mod_sort_game_version));
        ArrayAdapter<String> arrayAdapter2 = new ArrayAdapter<>(this.context, R.layout.item_spinner, this.sortList);
        this.sortListAdapter = arrayAdapter2;
        arrayAdapter2.setDropDownViewResource(R.layout.item_spinner_drop_down);
        this.editSort.setAdapter((SpinnerAdapter) this.sortListAdapter);
        // ★ 1.2.3：默认按「下载量」排序
        this.editSort.setSelection(this.sortList.indexOf(this.context.getString(R.string.download_mod_sort_downloads)));
        ArrayList<String> arrayList3 = new ArrayList<>();
        this.versionList = arrayList3;
        arrayList3.add("");
        this.versionList.addAll(Arrays.asList(RemoteModRepository.DEFAULT_GAME_VERSIONS));
        ArrayAdapter<String> arrayAdapter3 = new ArrayAdapter<>(this.context, R.layout.item_spinner, this.versionList);
        this.versionListAdapter = arrayAdapter3;
        arrayAdapter3.setDropDownViewResource(R.layout.item_spinner_drop_down);
        this.editVersionSpinner.setAdapter((SpinnerAdapter) this.versionListAdapter);
        // ★ 1.2.3：版本筛选默认选「当前正在玩的游戏版本」

        try {

            // ★ 1.2.5 修：currentVersion 存的是完整路径，直接 indexOf 必然 -1，
            //   五个下载页的「游戏版本」默认都停在第 0 项。改成先解析出真正的游戏版本号。
            String cur = SettingUtils.getCurrentGameVersion(this.activity);

            if (cur != null) {

                int vi = this.versionList.indexOf(cur);

                if (vi >= 0) {

                    this.editVersionSpinner.setSelection(vi);
                    // ★★★ 1.2.5 修：光设下拉不够 —— 真正参与查询的是旁边那个输入框（查询读 editVersion.getText()）。
                    //   只 setSelection 不写输入框，就会出现「下拉显示 b1.7.3、
                    //   列表里却还是 Fabric API / Sodium 这些不支持该版本的模组」。
                    this.editVersion.setText(cur);

                }

            }

        } catch (Throwable ignored) {

        }

        ArrayList<RemoteModRepository.Category> arrayList4 = new ArrayList<>();
        this.categoryList = arrayList4;
        arrayList4.add(new RemoteModRepository.Category(CurseForgeRemoteModRepository.CATEGORY_ALL, "0", new ArrayList()));
        CategorySpinnerAdapter categorySpinnerAdapter = new CategorySpinnerAdapter(this.context, this.categoryList, 4471);
        this.categoryListAdapter = categorySpinnerAdapter;
        this.editCategory.setAdapter((SpinnerAdapter) categorySpinnerAdapter);
        this.downloadSourceSpinner.setOnItemSelectedListener(this);
        this.editVersionSpinner.setOnItemSelectedListener(this);
        this.editCategory.setOnItemSelectedListener(this);
        this.editSort.setOnItemSelectedListener(this);
        this.editName.setOnEditorActionListener(this);
        this.editVersion.setOnEditorActionListener(this);
        this.editVersion.addTextChangedListener(this);
        this.progressBar = (ProgressBar) this.activity.findViewById(R.id.loading_download_package_list_progress);
        TextView textView = (TextView) this.activity.findViewById(R.id.refresh_package_list);
        this.refreshText = textView;
        textView.setOnClickListener(this);
        this.repository = new Repository();
        this.packageListView = (ListView) this.activity.findViewById(R.id.download_package_list);
        this.packageList = new ArrayList<>();
        DownloadResourceAdapter downloadResourceAdapter = new DownloadResourceAdapter(this.context, this.activity, this.repository, this.packageList, 1);
        this.downloadPackageListAdapter = downloadResourceAdapter;
        this.packageListView.setAdapter((ListAdapter) downloadResourceAdapter);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStart() {
        super.onStart();
        CustomAnimationUtils.showViewFromLeft(this.downloadPackageUI, this.activity, this.context, false);
        this.activity.uiManager.downloadUI.startDownloadPackageUI.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_white));
        init();
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.downloadPackageUI, this.activity, this.context, false);
        if (this.activity.isLoaded) {
            this.activity.uiManager.downloadUI.startDownloadPackageUI.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_parent));
        }
    }

    private void init() {
        if (this.packageList.size() == 0 && this.editName.getText().toString().equals("")) {
            search();
        }
    }

    private void search() {
        if (this.isSearching) {
            return;
        }
        new Thread(new Runnable() { // from class: com.qcl.launcher.launcher.uis.game.download.right.DownloadPackageUI$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                DownloadPackageUI.this.m478xbcd1e517();
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$search$0$com-qcl-launcher-launcher-uis-game-download-right-DownloadPackageUI, reason: not valid java name */
    public /* synthetic */ void m478xbcd1e517() {
        try {
            this.searchHandler.sendEmptyMessage(0);
            List list = (List) this.repository.search(this.editVersion.getText().toString(), (RemoteModRepository.Category) this.categoryListAdapter.getItem(this.editCategory.getSelectedItemPosition()), 0, 50, this.editName.getText().toString(), RemoteMod.getSortTypeByPosition(this.editSort.getSelectedItemPosition()), RemoteModRepository.SortOrder.DESC).collect(Collectors.toList());
            this.packageList.clear();
            this.packageList.addAll(list);
            List list2 = (List) this.repository.getCategories().collect(Collectors.toList());
            this.categoryList.clear();
            this.categoryList.add(new RemoteModRepository.Category(this.downloadSourceSpinner.getSelectedItemPosition() == 0 ? CurseForgeRemoteModRepository.CATEGORY_ALL : ModrinthRemoteModRepository.CATEGORY_ALL, this.downloadSourceSpinner.getSelectedItemPosition() == 0 ? "0" : "all", new ArrayList()));
            for (int i = 0; i < list2.size(); i++) {
                this.categoryList.add((RemoteModRepository.Category) list2.get(i));
                this.categoryList.addAll(((RemoteModRepository.Category) list2.get(i)).getSubcategories());
            }
            this.searchHandler.sendEmptyMessage(1);
        } catch (Exception e) {
            this.searchHandler.sendEmptyMessage(2);
            e.printStackTrace();
        }
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.installPackage) {
            this.activity.uiManager.switchMainUI(this.activity.uiManager.installPackageUI);
        }
        if (view == this.search || view == this.refresh) {
            search();
        }
        if (view == this.refreshText) {
            search();
        }
    }

    @Override // android.text.TextWatcher
    public void afterTextChanged(Editable editable) {
        search();
    }

    @Override // android.widget.AdapterView.OnItemSelectedListener
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
        if (adapterView == this.editCategory || adapterView == this.editSort || adapterView == this.editVersionSpinner) {
            search();
            if (adapterView == this.editVersionSpinner) {
                this.editVersion.setText((String) adapterView.getItemAtPosition(i));
            }
        }
    }

    @Override // android.widget.TextView.OnEditorActionListener
    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        if (textView != this.editName && textView != this.editVersion) {
            return false;
        }
        search();
        return false;
    }
}
