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
import com.qcl.launcher.launcher.mod.HybridRemoteModRepository;
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
                    DownloadModUI.this.modListView.setVisibility(8);
                }
                if (message.what == 1) {
                    DownloadModUI.this.modListAdapter.notifyDataSetChanged();
                    DownloadModUI.this.categoryListAdapter.notifyDataSetChanged();
                    DownloadModUI.this.progressBar.setVisibility(8);
                    DownloadModUI.this.modListView.setVisibility(0);
                    DownloadModUI.this.isSearching = false;
                }
                if (message.what == 2) {
                    // ★★★★★ 2026-10-11 修复（用户实测：「转圈转半天，突然圈消失了，
                    //   给我来了一张空白的列表，我还要再点一次刷新，列表才有可能出现」）：
                    //
                    //   原来这里除了收进度圈，还**把整个列表 setVisibility(8) 隐藏掉** ——
                    //   一旦这次搜索失败（国内连 CurseForge/Modrinth 超时很常见），
                    //   玩家看到的就是"圈转完 → 一片空白"，而不是"保留上一次的结果"。
                    //   更糟的是这个空白会让人以为整个页面坏了。
                    //
                    //   照 FCL 的做法改：失败时**保留列表原样可见**（有旧结果就继续显示旧结果，
                    //   没有就是空列表但页面结构还在），只收掉进度圈、并把搜索标志复位
                    //   —— 这样玩家可以立刻再点一次刷新，不必先面对一张白纸。
                    DownloadModUI.this.progressBar.setVisibility(8);
                    if (DownloadModUI.this.modListView != null) {
                        DownloadModUI.this.modListView.setVisibility(0);
                    }
                    DownloadModUI.this.isSearching = false;
                }
            }
        };
    }

    /**
     * 「下载源」下拉的选中项 → 实际仓库。
     *
     * <p>★ 1.5.0 用户要求混合搜索：下拉多了一项「CurseForge + Modrinth」，
     *   选中时用 {@link HybridRemoteModRepository} 并发查两站再合并去重。
     *   （下标 0=CurseForge、1=Modrinth、2=混合；别用字符串比，别写死 magic number。）
     */
    private RemoteModRepository repositoryForPosition(int position) {
        if (position == 2) {
            return HybridModRepository.MODS;
        }
        if (position == 1) {
            return ModrinthRemoteModRepository.MODS;
        }
        return CurseForgeRemoteModRepository.MODS;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class Repository extends LocalizedRemoteModRepository {
        private Repository() {
        }

        @Override // com.qcl.launcher.launcher.mod.LocalizedRemoteModRepository
        protected RemoteModRepository getBackedRemoteModRepository() {
            return repositoryForPosition(DownloadModUI.this.downloadSourceSpinner.getSelectedItemPosition());
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
        // ★ 1.5.0 混合搜索：第三项，两站并发查完合并去重（见 HybridRemoteModRepository）
        this.sourceList.add(this.context.getString(R.string.download_mod_source_hybrid));
        ArrayAdapter<String> arrayAdapter2 = new ArrayAdapter<>(this.context, R.layout.item_spinner, this.sourceList);
        this.sourceListAdapter = arrayAdapter2;
        arrayAdapter2.setDropDownViewResource(R.layout.item_spinner_drop_down);
        this.downloadSourceSpinner.setAdapter((SpinnerAdapter) this.sourceListAdapter);
        // ★ 2026-10-09 用户要求：「把混合设置为默认」⇒ 停在第 3 项（下标 2）。
        //   注意必须放在 setAdapter 之后、注册监听之前，否则会被下面的监听立刻当成切换事件。
        this.downloadSourceSpinner.setSelection(2);
        ArrayList<String> arrayList2 = new ArrayList<>();
        this.versionList = arrayList2;
        arrayList2.add("");
        this.versionList.addAll(Arrays.asList(RemoteModRepository.DEFAULT_GAME_VERSIONS));
        ArrayAdapter<String> arrayAdapter3 = new ArrayAdapter<>(this.context, R.layout.item_spinner, this.versionList);
        this.versionListAdapter = arrayAdapter3;
        arrayAdapter3.setDropDownViewResource(R.layout.item_spinner_drop_down);
        this.versionSpinner.setAdapter((SpinnerAdapter) this.versionListAdapter);
        // ★ 1.2.5：照 FCL DownloadPage.java —— 版本筛选下拉第一项是「不筛选」，
        //   默认就停在第一项，**不做任何自动匹配**；玩家自己选版本，查询按所选版本走。
        this.versionSpinner.setSelection(0);
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
    }

    private void init() {
        if (this.modList.size() == 0 && this.editName.getText().toString().equals("")) {
            search();
        }
    }

    @Override // android.widget.AdapterView.OnItemSelectedListener
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
        // ★ 1.5.0：切「下载源」必须重新查一次 —— 以前这里没监听 downloadSourceSpinner，
        //   玩家从 Modrinth 切到 CurseForge，看到的还是上一站的旧列表（经典「源没生效」）。
        if (adapterView == this.downloadSourceSpinner) {
            // 分类体系随源变 ⇒ 复位到第 0 项「全部」，否则旧分类 id 会让新源查不到东西
            if (this.typeSpinner != null) {
                this.typeSpinner.setSelection(0);
            }
            search();
            return;
        }
        if (adapterView == this.typeSpinner || adapterView == this.sortSpinner || adapterView == this.versionSpinner) {
            search();
            if (adapterView == this.versionSpinner) {
                this.editVersion.setText((String) adapterView.getItemAtPosition(i));
            }
        }
        if (adapterView == this.gameSpinner) {
            this.gameVersion = this.gameList.size() > 0 ? this.gameSpinner.getSelectedItem().toString() : null;
            // ★ 1.3.0：切换版本后立刻重算「不支持你当前的版本」徽章（不重新联网，只刷新显示）
            if (this.modListAdapter != null) {
                this.modListAdapter.notifyDataSetChanged();
            }
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
            int srcPos = this.downloadSourceSpinner.getSelectedItemPosition();
            boolean hybrid = srcPos == 2;
            // ★ 1.5.0 混合模式：分类下拉只有「全部」（两站分类体系不通用，
            //   列出来只会让玩家选一个然后某一边搜不到 —— 判定在 HybridRemoteModRepository 里）。
            RemoteModRepository.Category category = hybrid
                    ? null
                    : (RemoteModRepository.Category) this.categoryListAdapter.getItem(this.typeSpinner.getSelectedItemPosition());
            List list = (List) this.repository.search(this.editVersion.getText().toString(), category, 0, 50, this.editName.getText().toString(), RemoteMod.getSortTypeByPosition(this.sortSpinner.getSelectedItemPosition()), RemoteModRepository.SortOrder.DESC).collect(Collectors.toList());
            this.modList.clear();
            this.modList.addAll(list);
            if (this.modListAdapter != null) {
                this.modListAdapter.notifyDataSetChanged();
                // ★ 1.5.0：数据填好后给列表一次入场动画（用户要求「列表没有动态切换」）。
                //   ★ 必须 post 一下：此时 ListView 还没完成布局，getChildAt(0) 为 null，动画会失效。
                this.modListView.post(() -> {
                    if (this.modListAdapter != null) {
                        this.modListAdapter.animateList(this.modListView);
                    }
                });
            }
            List list2 = (List) this.repository.getCategories().collect(Collectors.toList());
            this.categoryList.clear();
            if (hybrid) {
                // ★ 混合：只给「全部」一项，并同步把下拉选中项复位到第 0 项
                this.categoryList.add(new RemoteModRepository.Category(CurseForgeRemoteModRepository.CATEGORY_ALL, "0", new ArrayList()));
            } else {
                this.categoryList.add(new RemoteModRepository.Category(srcPos == 0 ? CurseForgeRemoteModRepository.CATEGORY_ALL : ModrinthRemoteModRepository.CATEGORY_ALL, srcPos == 0 ? "0" : "all", new ArrayList()));
                for (int i = 0; i < list2.size(); i++) {
                    this.categoryList.add((RemoteModRepository.Category) list2.get(i));
                    this.categoryList.addAll(((RemoteModRepository.Category) list2.get(i)).getSubcategories());
                }
            }
            this.searchHandler.sendEmptyMessage(1);
            // ★ 照 FCL `searchAggregated`：聚合模式下若**只有一源成功**，
            //   结果照常显示，但要告诉玩家"可能不全"（否则玩家以为那就是全部）。
            //   ⚠ 本方法跑在**后台线程** ⇒ 只能取值，弹 Toast 必须回主线程（在 handler what==1 里做）。
            final String partialFailed = hybrid ? HybridRemoteModRepository.lastPartialWarning() : null;
            final boolean hasResults = this.modList != null && !this.modList.isEmpty();
            if (partialFailed != null && hasResults) {
                final int failedRes = "CURSEFORGE".equals(partialFailed)
                        ? R.string.download_mod_source_curse_forge : R.string.download_mod_source_modrinth;
                final int onlyRes = "CURSEFORGE".equals(partialFailed)
                        ? R.string.download_mod_source_modrinth : R.string.download_mod_source_curse_forge;
                this.activity.runOnUiThread(() -> {
                    try {
                        android.widget.Toast.makeText(DownloadModUI.this.context,
                                DownloadModUI.this.context.getString(R.string.download_search_partial,
                                        DownloadModUI.this.context.getString(failedRes),
                                        DownloadModUI.this.context.getString(onlyRes)),
                                android.widget.Toast.LENGTH_LONG).show();
                    } catch (Throwable ignoreToast) {
                    }
                });
            }
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
