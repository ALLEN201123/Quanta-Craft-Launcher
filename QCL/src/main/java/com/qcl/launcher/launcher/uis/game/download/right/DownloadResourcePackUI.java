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
public class DownloadResourcePackUI extends BaseUI implements View.OnClickListener, AdapterView.OnItemSelectedListener, TextView.OnEditorActionListener, TextWatcher {
    private ArrayList<RemoteModRepository.Category> categoryList;
    private CategorySpinnerAdapter categoryListAdapter;
    private DownloadResourceAdapter downloadResourcePackListAdapter;
    public LinearLayout downloadResourcePackUI;
    private Spinner downloadSourceSpinner;
    private Spinner editCategory;
    private EditText editName;
    private Spinner editSort;
    private EditText editVersion;
    private Spinner editVersionSpinner;
    private ArrayList<String> gameList;
    private ArrayAdapter<String> gameListAdapter;
    private Spinner gameSpinner;
    public String gameVersion;
    private boolean isSearching;
    public String lastVersion;
    private ProgressBar progressBar;
    private TextView refreshText;
    private RemoteModRepository repository;
    private ArrayList<RemoteMod> resourcePackList;
    private ListView resourcePackListView;
    private Button search;
    private Button refresh;
    private final Handler searchHandler;
    private ArrayList<String> sortList;
    private ArrayAdapter<String> sortListAdapter;
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

    public DownloadResourcePackUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
        this.isSearching = false;
        this.searchHandler = new Handler() { // from class: com.qcl.launcher.launcher.uis.game.download.right.DownloadResourcePackUI.1
            @Override // android.os.Handler
            public void handleMessage(Message message) {
                super.handleMessage(message);
                if (message.what == 0) {
                    DownloadResourcePackUI.this.isSearching = true;
                    DownloadResourcePackUI.this.progressBar.setVisibility(0);
                    DownloadResourcePackUI.this.refreshText.setVisibility(8);
                    DownloadResourcePackUI.this.resourcePackListView.setVisibility(8);
                }
                if (message.what == 1) {
                    DownloadResourcePackUI.this.downloadResourcePackListAdapter.notifyDataSetChanged();
                    DownloadResourcePackUI.this.categoryListAdapter.notifyDataSetChanged();
                    DownloadResourcePackUI.this.progressBar.setVisibility(8);
                    DownloadResourcePackUI.this.refreshText.setVisibility(8);
                    DownloadResourcePackUI.this.resourcePackListView.setVisibility(0);
                    DownloadResourcePackUI.this.isSearching = false;
                }
                if (message.what == 2) {
                    DownloadResourcePackUI.this.progressBar.setVisibility(8);
                    DownloadResourcePackUI.this.refreshText.setVisibility(0);
                    DownloadResourcePackUI.this.resourcePackListView.setVisibility(8);
                    DownloadResourcePackUI.this.isSearching = false;
                }
            }
        };
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onCreate() {
        super.onCreate();
        this.downloadResourcePackUI = (LinearLayout) this.activity.findViewById(R.id.ui_download_resource_pack);
        this.gameSpinner = (Spinner) this.activity.findViewById(R.id.download_resource_pack_arg_game);
        this.downloadSourceSpinner = (Spinner) this.activity.findViewById(R.id.download_resource_pack_arg_source);
        this.editName = (EditText) this.activity.findViewById(R.id.download_resource_pack_arg_name);
        this.editVersion = (EditText) this.activity.findViewById(R.id.edit_download_resource_pack_arg_version);
        this.editVersionSpinner = (Spinner) this.activity.findViewById(R.id.download_resource_pack_arg_version);
        this.editCategory = (Spinner) this.activity.findViewById(R.id.download_resource_pack_arg_type);
        this.editSort = (Spinner) this.activity.findViewById(R.id.download_resource_pack_arg_sort);
        Button button = (Button) this.activity.findViewById(R.id.search_resource_pack);
        this.search = button;
        // ★ 1.2.3：搜索旁的「刷新」——和搜索走同一条查询路

        this.refresh = (Button) this.activity.findViewById(R.id.refresh_resource_pack_search);

        if (this.refresh != null) {

            this.refresh.setOnClickListener((View.OnClickListener)this);

        }

        button.setOnClickListener(this);
        this.gameList = SettingUtils.getLocalVersionNames(this.activity.launcherSetting.gameFileDirectory);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this.context, R.layout.item_spinner, this.gameList);
        this.gameListAdapter = arrayAdapter;
        this.gameSpinner.setAdapter((SpinnerAdapter) arrayAdapter);
        ArrayList<String> arrayList = new ArrayList<>();
        this.sortList = arrayList;
        arrayList.add(this.context.getString(R.string.download_mod_sort_date));
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

        ArrayList<String> arrayList2 = new ArrayList<>();
        this.versionList = arrayList2;
        arrayList2.add("");
        this.versionList.addAll(Arrays.asList(RemoteModRepository.DEFAULT_GAME_VERSIONS));
        ArrayAdapter<String> arrayAdapter3 = new ArrayAdapter<>(this.context, R.layout.item_spinner, this.versionList);
        this.versionListAdapter = arrayAdapter3;
        arrayAdapter3.setDropDownViewResource(R.layout.item_spinner_drop_down);
        this.editVersionSpinner.setAdapter((SpinnerAdapter) this.versionListAdapter);
        // ★ 1.2.5：照 FCL DownloadPage.java —— 版本筛选下拉第一项是「不筛选」，
        //   默认停在第一项，**不做任何自动匹配**；玩家自己选版本，查询按所选版本走。

        this.editVersionSpinner.setSelection(0);

        ArrayList<RemoteModRepository.Category> arrayList3 = new ArrayList<>();
        this.categoryList = arrayList3;
        arrayList3.add(new RemoteModRepository.Category(CurseForgeRemoteModRepository.CATEGORY_ALL, "0", new ArrayList()));
        CategorySpinnerAdapter categorySpinnerAdapter = new CategorySpinnerAdapter(this.context, this.categoryList, 12);
        this.categoryListAdapter = categorySpinnerAdapter;
        this.editCategory.setAdapter((SpinnerAdapter) categorySpinnerAdapter);
        this.gameSpinner.setOnItemSelectedListener(this);
        this.editVersionSpinner.setOnItemSelectedListener(this);
        this.editCategory.setOnItemSelectedListener(this);
        this.editSort.setOnItemSelectedListener(this);
        this.editName.setOnEditorActionListener(this);
        this.editVersion.setOnEditorActionListener(this);
        this.editVersion.addTextChangedListener(this);
        this.progressBar = (ProgressBar) this.activity.findViewById(R.id.loading_download_resource_pack_list_progress);
        TextView textView = (TextView) this.activity.findViewById(R.id.refresh_resource_pack_list);
        this.refreshText = textView;
        textView.setOnClickListener(this);
        this.repository = new Repository();
        ArrayList arrayList4 = new ArrayList();
        arrayList4.add(this.context.getString(R.string.download_mod_source_curse_forge));
        arrayList4.add(this.context.getString(R.string.download_mod_source_modrinth));
        ArrayAdapter arrayAdapter4 = new ArrayAdapter(this.context, R.layout.item_spinner, arrayList4);
        arrayAdapter4.setDropDownViewResource(R.layout.item_spinner_drop_down);
        this.downloadSourceSpinner.setAdapter((SpinnerAdapter) arrayAdapter4);
        this.downloadSourceSpinner.setSelection(1);
        this.downloadSourceSpinner.setOnItemSelectedListener(this);
        this.resourcePackListView = (ListView) this.activity.findViewById(R.id.download_resource_pack_list);
        this.resourcePackList = new ArrayList<>();
        DownloadResourceAdapter downloadResourceAdapter = new DownloadResourceAdapter(this.context, this.activity, this.repository, this.resourcePackList, 2);
        this.downloadResourcePackListAdapter = downloadResourceAdapter;
        this.resourcePackListView.setAdapter((ListAdapter) downloadResourceAdapter);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStart() {
        super.onStart();
        CustomAnimationUtils.showViewFromLeft(this.downloadResourcePackUI, this.activity, this.context, false);
        this.activity.uiManager.downloadUI.startDownloadResourcePackUI.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_white));
        init();
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.downloadResourcePackUI, this.activity, this.context, false);
        if (this.activity.isLoaded) {
            this.activity.uiManager.downloadUI.startDownloadResourcePackUI.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_parent));
        }
    }

    private void init() {
        if (this.resourcePackList.size() == 0 && this.editName.getText().toString().equals("")) {
            search();
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

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class Repository extends LocalizedRemoteModRepository {
        private Repository() {
        }

        @Override // com.qcl.launcher.launcher.mod.LocalizedRemoteModRepository
        protected RemoteModRepository getBackedRemoteModRepository() {
            if (DownloadResourcePackUI.this.downloadSourceSpinner.getSelectedItemPosition() == 1) {
                return ModrinthRemoteModRepository.RESOURCE_PACKS;
            }
            return new CurseForgeRemoteModRepository(RemoteModRepository.Type.RESOURCE_PACK, 12);
        }

        @Override // com.qcl.launcher.launcher.mod.RemoteModRepository
        public RemoteModRepository.Type getType() {
            return RemoteModRepository.Type.RESOURCE_PACK;
        }
    }

    private void search() {
        if (this.isSearching) {
            return;
        }
        new Thread(new Runnable() { // from class: com.qcl.launcher.launcher.uis.game.download.right.DownloadResourcePackUI$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                DownloadResourcePackUI.this.m479xdc0fe67e();
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$search$0$com-qcl-launcher-launcher-uis-game-download-right-DownloadResourcePackUI, reason: not valid java name */
    public /* synthetic */ void m479xdc0fe67e() {
        try {
            this.searchHandler.sendEmptyMessage(0);
            List list = (List) this.repository.search(this.editVersion.getText().toString(), (RemoteModRepository.Category) this.categoryListAdapter.getItem(this.editCategory.getSelectedItemPosition()), 0, 50, this.editName.getText().toString(), RemoteMod.getSortTypeByPosition(this.editSort.getSelectedItemPosition()), RemoteModRepository.SortOrder.DESC).collect(Collectors.toList());
            this.resourcePackList.clear();
            this.resourcePackList.addAll(list);
            List list2 = (List) this.repository.getCategories().collect(Collectors.toList());
            this.categoryList.clear();
            boolean z = this.downloadSourceSpinner.getSelectedItemPosition() == 1;
            this.categoryList.add(new RemoteModRepository.Category(z ? ModrinthRemoteModRepository.CATEGORY_ALL : CurseForgeRemoteModRepository.CATEGORY_ALL, z ? "all" : "0", new ArrayList()));
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
        if (view == this.search || view == this.refresh) {
            search();
        }
        if (view == this.refreshText) {
            search();
        }
    }

    @Override // android.widget.AdapterView.OnItemSelectedListener
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
        if (adapterView == this.downloadSourceSpinner) {
            search();
            return;
        }
        if (adapterView == this.editCategory || adapterView == this.editSort || adapterView == this.editVersionSpinner) {
            search();
            if (adapterView == this.editVersionSpinner) {
                this.editVersion.setText((String) adapterView.getItemAtPosition(i));
            }
        }
        if (adapterView == this.gameSpinner) {
            this.gameVersion = this.gameList.size() > 0 ? this.gameSpinner.getSelectedItem().toString() : null;
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

    @Override // android.text.TextWatcher
    public void afterTextChanged(Editable editable) {
        search();
    }
}
