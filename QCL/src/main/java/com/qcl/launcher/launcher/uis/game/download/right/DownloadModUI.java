package com.qcl.launcher.launcher.uis.game.download.right;

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
import com.qcl.launcher.launcher.setting.SettingUtils;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.launcher.view.spinner.CategorySpinnerAdapter;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class DownloadModUI extends BaseUI implements View.OnClickListener, AdapterView.OnItemSelectedListener, TextWatcher, TextView.OnEditorActionListener {
    private ArrayList<RemoteModRepository.Category> categoryList;
    private CategorySpinnerAdapter categoryListAdapter;
    public LinearLayout downloadModUI;
    private Spinner downloadSourceSpinner;
    private EditText editName;
    private EditText editVersion;
    private ArrayList<String> gameList;
    private ArrayAdapter<String> gameListAdapter;
    private Spinner gameSpinner;
    public String gameVersion;
    private boolean isSearching;
    public String lastVersion;
    private ArrayList<RemoteMod> modList;
    private DownloadResourceAdapter modListAdapter;
    private ListView modListView;
    private ProgressBar progressBar;
    private TextView refreshText;
    private RemoteModRepository repository;
    private Button search;
    private Button refresh;
    private final Handler searchHandler;
    private ArrayList<String> sortList;
    private ArrayAdapter<String> sortListAdapter;
    private Spinner sortSpinner;
    private ArrayList<String> sourceList;
    private ArrayAdapter<String> sourceListAdapter;
    private Spinner typeSpinner;
    private ArrayList<String> versionList;
    private ArrayAdapter<String> versionListAdapter;
    private Spinner versionSpinner;

    @Override // android.text.TextWatcher
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override // android.widget.AdapterView.OnItemSelectedListener
    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    @Override // android.text.TextWatcher
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    public DownloadModUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
        this.isSearching = false;
        this.searchHandler = new Handler() { // from class: com.qcl.launcher.launcher.uis.game.download.right.DownloadModUI.1
            @Override // android.os.Handler
            public void handleMessage(Message message) {
                super.handleMessage(message);
                if (message.what == 0) {
                    DownloadModUI.this.isSearching = true;
                    DownloadModUI.this.progressBar.setVisibility(0);
                    DownloadModUI.this.refreshText.setVisibility(8);
                    DownloadModUI.this.modListView.setVisibility(8);
                }
                if (message.what == 1) {
                    DownloadModUI.this.modListAdapter.notifyDataSetChanged();
                    DownloadModUI.this.categoryListAdapter.notifyDataSetChanged();
                    DownloadModUI.this.progressBar.setVisibility(8);
                    DownloadModUI.this.refreshText.setVisibility(8);
                    DownloadModUI.this.modListView.setVisibility(0);
                    DownloadModUI.this.isSearching = false;
                }
                if (message.what == 2) {
                    DownloadModUI.this.progressBar.setVisibility(8);
                    DownloadModUI.this.refreshText.setVisibility(0);
                    DownloadModUI.this.modListView.setVisibility(8);
                    DownloadModUI.this.isSearching = false;
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
            if (DownloadModUI.this.downloadSourceSpinner.getSelectedItemPosition() == 1) {
                return ModrinthRemoteModRepository.MODS;
            }
            return CurseForgeRemoteModRepository.MODS;
        }

        @Override // com.qcl.launcher.launcher.mod.RemoteModRepository
        public RemoteModRepository.Type getType() {
            return RemoteModRepository.Type.MOD;
        }
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onCreate() {
        super.onCreate();
        this.downloadModUI = (LinearLayout) this.activity.findViewById(R.id.ui_download_mod);
        this.gameSpinner = (Spinner) this.activity.findViewById(R.id.download_mod_arg_game);
        this.downloadSourceSpinner = (Spinner) this.activity.findViewById(R.id.download_mod_arg_source);
        this.editName = (EditText) this.activity.findViewById(R.id.download_mod_arg_name);
        this.editVersion = (EditText) this.activity.findViewById(R.id.edit_download_mod_arg_version);
        this.versionSpinner = (Spinner) this.activity.findViewById(R.id.download_mod_arg_version);
        this.typeSpinner = (Spinner) this.activity.findViewById(R.id.download_mod_arg_type);
        this.sortSpinner = (Spinner) this.activity.findViewById(R.id.download_mod_arg_sort);
        this.gameList = SettingUtils.getLocalVersionNames(this.activity.launcherSetting.gameFileDirectory);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this.context, R.layout.item_spinner, this.gameList);
        this.gameListAdapter = arrayAdapter;
        this.gameSpinner.setAdapter((SpinnerAdapter) arrayAdapter);
        ArrayList<String> arrayList = new ArrayList<>();
        this.sourceList = arrayList;
        arrayList.add(this.context.getString(R.string.download_mod_source_curse_forge));
        this.sourceList.add(this.context.getString(R.string.download_mod_source_modrinth));
        ArrayAdapter<String> arrayAdapter2 = new ArrayAdapter<>(this.context, R.layout.item_spinner, this.sourceList);
        this.sourceListAdapter = arrayAdapter2;
        arrayAdapter2.setDropDownViewResource(R.layout.item_spinner_drop_down);
        this.downloadSourceSpinner.setAdapter((SpinnerAdapter) this.sourceListAdapter);
        this.downloadSourceSpinner.setSelection(1);
        ArrayList<String> arrayList2 = new ArrayList<>();
        this.versionList = arrayList2;
        arrayList2.add("");
        this.versionList.addAll(Arrays.asList(RemoteModRepository.DEFAULT_GAME_VERSIONS));
        ArrayAdapter<String> arrayAdapter3 = new ArrayAdapter<>(this.context, R.layout.item_spinner, this.versionList);
        this.versionListAdapter = arrayAdapter3;
        arrayAdapter3.setDropDownViewResource(R.layout.item_spinner_drop_down);
        this.versionSpinner.setAdapter((SpinnerAdapter) this.versionListAdapter);
        // ★ 1.2.3：版本筛选默认选「当前正在玩的游戏版本」，不用玩家每次手动翻
        try {
            String cur = this.activity.publicGameSetting.currentVersion;
            if (cur != null) {
                int vi = this.versionList.indexOf(cur);
                if (vi >= 0) {
                    this.versionSpinner.setSelection(vi);
                }
            }
        } catch (Throwable ignored) {
        }
        ArrayList<RemoteModRepository.Category> arrayList3 = new ArrayList<>();
        this.categoryList = arrayList3;
        arrayList3.add(new RemoteModRepository.Category(CurseForgeRemoteModRepository.CATEGORY_ALL, "0", new ArrayList()));
        CategorySpinnerAdapter categorySpinnerAdapter = new CategorySpinnerAdapter(this.context, this.categoryList, 6);
        this.categoryListAdapter = categorySpinnerAdapter;
        this.typeSpinner.setAdapter((SpinnerAdapter) categorySpinnerAdapter);
        ArrayList<String> arrayList4 = new ArrayList<>();
        this.sortList = arrayList4;
        arrayList4.add(this.context.getString(R.string.download_mod_sort_date));
        this.sortList.add(this.context.getString(R.string.download_mod_sort_heat));
        this.sortList.add(this.context.getString(R.string.download_mod_sort_recent));
        this.sortList.add(this.context.getString(R.string.download_mod_sort_name));
        this.sortList.add(this.context.getString(R.string.download_mod_sort_author));
        this.sortList.add(this.context.getString(R.string.download_mod_sort_downloads));
        this.sortList.add(this.context.getString(R.string.download_mod_sort_category));
        this.sortList.add(this.context.getString(R.string.download_mod_sort_game_version));
        ArrayAdapter<String> arrayAdapter4 = new ArrayAdapter<>(this.context, R.layout.item_spinner, this.sortList);
        this.sortListAdapter = arrayAdapter4;
        arrayAdapter4.setDropDownViewResource(R.layout.item_spinner_drop_down);
        this.sortSpinner.setAdapter((SpinnerAdapter) this.sortListAdapter);
        // ★ 1.2.3：默认按「下载量」排序（列表第 6 项 = download_mod_sort_downloads）

        this.sortSpinner.setSelection(this.sortList.indexOf(this.context.getString(R.string.download_mod_sort_downloads)));

        this.gameSpinner.setOnItemSelectedListener(this);
        this.downloadSourceSpinner.setOnItemSelectedListener(this);
        this.versionSpinner.setOnItemSelectedListener(this);
        this.typeSpinner.setOnItemSelectedListener(this);
        this.sortSpinner.setOnItemSelectedListener(this);
        Button button = (Button) this.activity.findViewById(R.id.search_mod);
        this.search = button;

        // ★ 1.2.3：搜索按钮旁的「刷新」——切完版本/排序后点它重新查，和点搜索走同一条路
        Button refreshBtn = (Button) this.activity.findViewById(R.id.refresh_mod_search);
        this.refresh = refreshBtn;
        if (refreshBtn != null) {
            refreshBtn.setOnClickListener(this);
        }
        button.setOnClickListener(this);
        this.editName.setOnEditorActionListener(this);
        this.editVersion.setOnEditorActionListener(this);
        this.editVersion.addTextChangedListener(this);
        this.repository = new Repository();
        this.modListView = (ListView) this.activity.findViewById(R.id.download_mod_list);
        this.modList = new ArrayList<>();
        DownloadResourceAdapter downloadResourceAdapter = new DownloadResourceAdapter(this.context, this.activity, this.repository, this.modList, 0);
        this.modListAdapter = downloadResourceAdapter;
        this.modListView.setAdapter((ListAdapter) downloadResourceAdapter);
        this.progressBar = (ProgressBar) this.activity.findViewById(R.id.loading_download_mod_list_progress);
        TextView textView = (TextView) this.activity.findViewById(R.id.refresh_mod_list);
        this.refreshText = textView;
        textView.setOnClickListener(this);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStart() {
        super.onStart();
        CustomAnimationUtils.showViewFromLeft(this.downloadModUI, this.activity, this.context, false);
        this.activity.uiManager.downloadUI.startDownloadModUI.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_white));
        init();
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.downloadModUI, this.activity, this.context, false);
        if (this.activity.isLoaded) {
            this.activity.uiManager.downloadUI.startDownloadModUI.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_parent));
        }
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.search || view == this.refresh) {
            search();
        }
        if (view == this.refreshText) {
            search();
        }
    }

    private void init() {
        if (this.modList.size() == 0 && this.editName.getText().toString().equals("")) {
            search();
        }
    }

    @Override // android.widget.AdapterView.OnItemSelectedListener
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
        if (adapterView == this.typeSpinner || adapterView == this.sortSpinner || adapterView == this.versionSpinner) {
            search();
            if (adapterView == this.versionSpinner) {
                this.editVersion.setText((String) adapterView.getItemAtPosition(i));
            }
        }
        if (adapterView == this.gameSpinner) {
            this.gameVersion = this.gameList.size() > 0 ? this.gameSpinner.getSelectedItem().toString() : null;
        }
    }

    public void refreshGameList() {
        boolean z;
        String str;
        if (Objects.equals(this.lastVersion, this.activity.publicGameSetting.currentVersion)) {
            z = false;
        } else {
            this.lastVersion = this.activity.publicGameSetting.currentVersion;
            z = true;
        }
        this.gameList = SettingUtils.getLocalVersionNames(this.activity.launcherSetting.gameFileDirectory);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this.context, R.layout.item_spinner, this.gameList);
        this.gameListAdapter = arrayAdapter;
        this.gameSpinner.setAdapter((SpinnerAdapter) arrayAdapter);
        if (this.gameList.size() > 0) {
            if (z && this.activity.publicGameSetting.currentVersion != null && !this.activity.publicGameSetting.currentVersion.equals("")) {
                String substring = this.activity.publicGameSetting.currentVersion.substring(this.activity.publicGameSetting.currentVersion.lastIndexOf("/") + 1);
                if (substring.length() <= 0 || !this.gameList.contains(substring)) {
                    return;
                }
                this.gameSpinner.setSelection(this.gameListAdapter.getPosition(substring));
                return;
            }
            if (!z && (str = this.gameVersion) != null && this.gameList.contains(str)) {
                this.gameSpinner.setSelection(this.gameListAdapter.getPosition(this.gameVersion));
                return;
            } else {
                this.gameSpinner.setSelection(0);
                return;
            }
        }
        this.gameVersion = null;
    }

    private void search() {
        if (this.isSearching) {
            return;
        }
        new Thread(new Runnable() { // from class: com.qcl.launcher.launcher.uis.game.download.right.DownloadModUI$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                DownloadModUI.this.m477xbfb3ff53();
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$search$0$com-qcl-launcher-launcher-uis-game-download-right-DownloadModUI, reason: not valid java name */
    public /* synthetic */ void m477xbfb3ff53() {
        try {
            this.searchHandler.sendEmptyMessage(0);
            List list = (List) this.repository.search(this.editVersion.getText().toString(), (RemoteModRepository.Category) this.categoryListAdapter.getItem(this.typeSpinner.getSelectedItemPosition()), 0, 50, this.editName.getText().toString(), RemoteMod.getSortTypeByPosition(this.sortSpinner.getSelectedItemPosition()), RemoteModRepository.SortOrder.DESC).collect(Collectors.toList());
            this.modList.clear();
            this.modList.addAll(list);
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

    @Override // android.text.TextWatcher
    public void afterTextChanged(Editable editable) {
        search();
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
