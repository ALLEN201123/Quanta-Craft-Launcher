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
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.launcher.view.spinner.CategorySpinnerAdapter;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class DownloadWorldUI extends BaseUI implements View.OnClickListener, AdapterView.OnItemSelectedListener, TextWatcher, TextView.OnEditorActionListener {
    private ArrayList<RemoteModRepository.Category> categoryList;
    private CategorySpinnerAdapter categoryListAdapter;
    private Spinner downloadSourceSpinner;
    private DownloadResourceAdapter downloadWorldListAdapter;
    public LinearLayout downloadWorldUI;
    private Spinner editCategory;
    private EditText editName;
    private Spinner editSort;
    private EditText editVersion;
    private Spinner editVersionSpinner;
    private boolean isSearching;
    private ProgressBar progressBar;
    private TextView refreshText;
    private RemoteModRepository repository;
    private Button search;
    private final Handler searchHandler;
    private ArrayList<String> sortList;
    private ArrayAdapter<String> sortListAdapter;
    private ArrayList<String> versionList;
    private ArrayAdapter<String> versionListAdapter;
    private ArrayList<RemoteMod> worldList;
    private ListView worldListView;

    @Override // android.text.TextWatcher
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override // android.widget.AdapterView.OnItemSelectedListener
    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    @Override // android.text.TextWatcher
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    public DownloadWorldUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
        this.isSearching = false;
        this.searchHandler = new Handler() { // from class: com.qcl.launcher.launcher.uis.game.download.right.DownloadWorldUI.1
            @Override // android.os.Handler
            public void handleMessage(Message message) {
                super.handleMessage(message);
                if (message.what == 0) {
                    DownloadWorldUI.this.isSearching = true;
                    DownloadWorldUI.this.progressBar.setVisibility(0);
                    DownloadWorldUI.this.refreshText.setVisibility(8);
                    DownloadWorldUI.this.worldListView.setVisibility(8);
                }
                if (message.what == 1) {
                    DownloadWorldUI.this.downloadWorldListAdapter.notifyDataSetChanged();
                    DownloadWorldUI.this.categoryListAdapter.notifyDataSetChanged();
                    DownloadWorldUI.this.progressBar.setVisibility(8);
                    DownloadWorldUI.this.refreshText.setVisibility(8);
                    DownloadWorldUI.this.worldListView.setVisibility(0);
                    DownloadWorldUI.this.isSearching = false;
                }
                if (message.what == 2) {
                    DownloadWorldUI.this.progressBar.setVisibility(8);
                    DownloadWorldUI.this.refreshText.setVisibility(0);
                    DownloadWorldUI.this.worldListView.setVisibility(8);
                    DownloadWorldUI.this.isSearching = false;
                }
            }
        };
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onCreate() {
        super.onCreate();
        this.downloadWorldUI = (LinearLayout) this.activity.findViewById(R.id.ui_download_world);
        this.editName = (EditText) this.activity.findViewById(R.id.download_world_arg_name);
        this.editVersion = (EditText) this.activity.findViewById(R.id.edit_download_world_arg_version);
        this.editVersionSpinner = (Spinner) this.activity.findViewById(R.id.download_world_arg_version);
        this.editCategory = (Spinner) this.activity.findViewById(R.id.download_world_arg_type);
        this.editSort = (Spinner) this.activity.findViewById(R.id.download_world_arg_sort);
        Button button = (Button) this.activity.findViewById(R.id.search_world_list);
        this.search = button;
        button.setOnClickListener(this);
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
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this.context, R.layout.item_spinner, this.sortList);
        this.sortListAdapter = arrayAdapter;
        arrayAdapter.setDropDownViewResource(R.layout.item_spinner_drop_down);
        this.editSort.setAdapter((SpinnerAdapter) this.sortListAdapter);
        ArrayList<String> arrayList2 = new ArrayList<>();
        this.versionList = arrayList2;
        arrayList2.add("");
        this.versionList.addAll(Arrays.asList(RemoteModRepository.DEFAULT_GAME_VERSIONS));
        ArrayAdapter<String> arrayAdapter2 = new ArrayAdapter<>(this.context, R.layout.item_spinner, this.versionList);
        this.versionListAdapter = arrayAdapter2;
        arrayAdapter2.setDropDownViewResource(R.layout.item_spinner_drop_down);
        this.editVersionSpinner.setAdapter((SpinnerAdapter) this.versionListAdapter);
        ArrayList<RemoteModRepository.Category> arrayList3 = new ArrayList<>();
        this.categoryList = arrayList3;
        arrayList3.add(new RemoteModRepository.Category(CurseForgeRemoteModRepository.CATEGORY_ALL, "0", new ArrayList()));
        CategorySpinnerAdapter categorySpinnerAdapter = new CategorySpinnerAdapter(this.context, this.categoryList, 17);
        this.categoryListAdapter = categorySpinnerAdapter;
        this.editCategory.setAdapter((SpinnerAdapter) categorySpinnerAdapter);
        this.editVersionSpinner.setOnItemSelectedListener(this);
        this.editCategory.setOnItemSelectedListener(this);
        this.editSort.setOnItemSelectedListener(this);
        this.editName.setOnEditorActionListener(this);
        this.editVersion.setOnEditorActionListener(this);
        this.editVersion.addTextChangedListener(this);
        this.progressBar = (ProgressBar) this.activity.findViewById(R.id.loading_download_world_list_progress);
        TextView textView = (TextView) this.activity.findViewById(R.id.refresh_world_list);
        this.refreshText = textView;
        textView.setOnClickListener(this);
        this.repository = new Repository();
        this.downloadSourceSpinner = (Spinner) this.activity.findViewById(R.id.download_world_arg_source);
        ArrayList arrayList4 = new ArrayList();
        arrayList4.add(this.context.getString(R.string.download_mod_source_curse_forge));
        arrayList4.add(this.context.getString(R.string.download_mod_source_modrinth));
        ArrayAdapter arrayAdapter3 = new ArrayAdapter(this.context, R.layout.item_spinner, arrayList4);
        arrayAdapter3.setDropDownViewResource(R.layout.item_spinner_drop_down);
        this.downloadSourceSpinner.setAdapter((SpinnerAdapter) arrayAdapter3);
        this.downloadSourceSpinner.setSelection(1);
        this.downloadSourceSpinner.setOnItemSelectedListener(this);
        this.worldListView = (ListView) this.activity.findViewById(R.id.download_world_list);
        this.worldList = new ArrayList<>();
        DownloadResourceAdapter downloadResourceAdapter = new DownloadResourceAdapter(this.context, this.activity, this.repository, this.worldList, 3);
        this.downloadWorldListAdapter = downloadResourceAdapter;
        this.worldListView.setAdapter((ListAdapter) downloadResourceAdapter);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStart() {
        super.onStart();
        CustomAnimationUtils.showViewFromLeft(this.downloadWorldUI, this.activity, this.context, false);
        this.activity.uiManager.downloadUI.startDownloadWorldUI.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_white));
        init();
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.downloadWorldUI, this.activity, this.context, false);
        if (this.activity.isLoaded) {
            this.activity.uiManager.downloadUI.startDownloadWorldUI.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_parent));
        }
    }

    private void init() {
        if (this.worldList.size() == 0 && this.editName.getText().toString().equals("")) {
            search();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class Repository extends LocalizedRemoteModRepository {
        private Repository() {
        }

        @Override // com.qcl.launcher.launcher.mod.LocalizedRemoteModRepository
        protected RemoteModRepository getBackedRemoteModRepository() {
            if (DownloadWorldUI.this.downloadSourceSpinner.getSelectedItemPosition() == 1) {
                return ModrinthRemoteModRepository.MODPACKS;
            }
            return new CurseForgeRemoteModRepository(RemoteModRepository.Type.WORLD, 17);
        }

        @Override // com.qcl.launcher.launcher.mod.RemoteModRepository
        public RemoteModRepository.Type getType() {
            return RemoteModRepository.Type.WORLD;
        }
    }

    private void search() {
        if (this.isSearching) {
            return;
        }
        new Thread(new Runnable() { // from class: com.qcl.launcher.launcher.uis.game.download.right.DownloadWorldUI$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                DownloadWorldUI.this.m481xb47ff4a3();
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$search$0$com-qcl-launcher-launcher-uis-game-download-right-DownloadWorldUI, reason: not valid java name */
    public /* synthetic */ void m481xb47ff4a3() {
        try {
            this.searchHandler.sendEmptyMessage(0);
            List list = (List) this.repository.search(this.editVersion.getText().toString(), (RemoteModRepository.Category) this.categoryListAdapter.getItem(this.editCategory.getSelectedItemPosition()), 0, 50, this.editName.getText().toString(), RemoteMod.getSortTypeByPosition(this.editSort.getSelectedItemPosition()), RemoteModRepository.SortOrder.DESC).collect(Collectors.toList());
            this.worldList.clear();
            this.worldList.addAll(list);
            List list2 = (List) this.repository.getCategories().collect(Collectors.toList());
            this.categoryList.clear();
            this.categoryList.add(new RemoteModRepository.Category(CurseForgeRemoteModRepository.CATEGORY_ALL, "0", new ArrayList()));
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
        if (view == this.search) {
            search();
        }
        if (view == this.refreshText) {
            search();
        }
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
