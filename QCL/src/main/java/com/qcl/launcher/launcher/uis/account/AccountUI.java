package com.qcl.launcher.launcher.uis.account;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.qcl.launcher.auth.Account;
import com.qcl.launcher.auth.authlibinjector.AuthlibInjectorServer;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.dialogs.account.AddMicrosoftAccountDialog;
import com.qcl.launcher.launcher.dialogs.account.AddMojangAccountDialog;
import com.qcl.launcher.launcher.dialogs.account.AddOfflineAccountDialog;
import com.qcl.launcher.launcher.dialogs.account.SelectServerTypeDialog;
import com.qcl.launcher.launcher.launch.check.LaunchTools;
import com.qcl.launcher.launcher.list.account.AccountListAdapter;
import com.qcl.launcher.launcher.list.account.server.AuthlibInjectorServerListAdapter;
import com.qcl.launcher.launcher.setting.InitializeSetting;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;
import com.qcl.launcher.utils.file.UriUtils;
import com.qcl.launcher.utils.gson.GsonUtils;
import java.util.ArrayList;
import java.util.Iterator;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class AccountUI extends BaseUI implements View.OnClickListener {
    public static final int SELECT_SKIN_REQUEST = 6900;
    private ListView accountList;
    public AccountListAdapter accountListAdapter;
    public LinearLayout accountUI;
    public ArrayList<Account> accounts;
    private LinearLayout addLoginServer;
    private LinearLayout addMicrosoftAccount;
    private AddMicrosoftAccountDialog addMicrosoftAccountDialog;
    private LinearLayout addMojangAccount;
    private LinearLayout addOfflineAccount;
    private ListView externalServerList;
    public ArrayList<AuthlibInjectorServer> serverList;
    public AuthlibInjectorServerListAdapter serverListAdapter;

    public AccountUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onCreate() {
        super.onCreate();
        this.accountUI = (LinearLayout) this.activity.findViewById(R.id.ui_account);
        this.addOfflineAccount = (LinearLayout) this.activity.findViewById(R.id.add_offline_account);
        this.addMojangAccount = (LinearLayout) this.activity.findViewById(R.id.add_mojang_account);
        this.addMicrosoftAccount = (LinearLayout) this.activity.findViewById(R.id.add_microsoft_account);
        this.addLoginServer = (LinearLayout) this.activity.findViewById(R.id.add_login_server);
        this.addOfflineAccount.setOnClickListener(this);
        this.addMojangAccount.setOnClickListener(this);
        this.addMicrosoftAccount.setOnClickListener(this);
        this.addLoginServer.setOnClickListener(this);
        this.externalServerList = (ListView) this.activity.findViewById(R.id.external_server_list);
        this.accountList = (ListView) this.activity.findViewById(R.id.account_list);
        this.addMojangAccount.setVisibility(8);
        init();
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStart() {
        super.onStart();
        this.activity.showBarTitle(this.context.getResources().getString(R.string.account_ui_title), canGoBackToLast(), true);
        CustomAnimationUtils.showViewFromLeft(this.accountUI, this.activity, this.context, true);
        init();
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.accountUI, this.activity, this.context, true);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onActivityResult(int i, int i2, Intent intent) {
        AccountListAdapter accountListAdapter;
        super.onActivityResult(i, i2, intent);
        if (i == 2000 && i2 == -1) {
            AddMicrosoftAccountDialog addMicrosoftAccountDialog = this.addMicrosoftAccountDialog;
            if (addMicrosoftAccountDialog != null && addMicrosoftAccountDialog.isShowing()) {
                this.addMicrosoftAccountDialog.login(intent);
            }
            if (LaunchTools.addAccountDialog != null && LaunchTools.addAccountDialog.isShowing()) {
                LaunchTools.addAccountDialog.login(intent);
            }
        }
        if (i == 6900 && i2 == -1 && intent != null) {
            String realPathFromUri_AboveApi19 = UriUtils.getRealPathFromUri_AboveApi19(this.context, intent.getData());
            if (realPathFromUri_AboveApi19 == null || (accountListAdapter = this.accountListAdapter) == null) {
                return;
            }
            accountListAdapter.uploadSkin(realPathFromUri_AboveApi19);
        }
    }

    private void init() {
        this.accounts = InitializeSetting.initializeAccounts(this.context);
        AccountListAdapter accountListAdapter = new AccountListAdapter(this.context, this.activity, this.accounts);
        this.accountListAdapter = accountListAdapter;
        this.accountList.setAdapter((ListAdapter) accountListAdapter);
        this.serverList = InitializeSetting.initializeAuthlibInjectorServer(this.context);
        AuthlibInjectorServerListAdapter authlibInjectorServerListAdapter = new AuthlibInjectorServerListAdapter(this.context, this.activity, this.serverList);
        this.serverListAdapter = authlibInjectorServerListAdapter;
        this.externalServerList.setAdapter((ListAdapter) authlibInjectorServerListAdapter);
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.addOfflineAccount) {
            new AddOfflineAccountDialog(this.context, this.accounts, new AddOfflineAccountDialog.OnOfflineAccountAddListener() { // from class: com.qcl.launcher.launcher.uis.account.AccountUI$$ExternalSyntheticLambda2
                @Override // com.qcl.launcher.launcher.dialogs.account.AddOfflineAccountDialog.OnOfflineAccountAddListener
                public final void onPositive(Account account) {
                    AccountUI.this.m471lambda$onClick$0$comqcllauncherlauncheruisaccountAccountUI(account);
                }
            }).show();
        }
        if (view == this.addMojangAccount) {
            new AddMojangAccountDialog(this.context, this.accounts, new AddMojangAccountDialog.OnMojangAccountAddListener() { // from class: com.qcl.launcher.launcher.uis.account.AccountUI$$ExternalSyntheticLambda1
                @Override // com.qcl.launcher.launcher.dialogs.account.AddMojangAccountDialog.OnMojangAccountAddListener
                public final void onPositive(Account account) {
                    AccountUI.this.m472lambda$onClick$1$comqcllauncherlauncheruisaccountAccountUI(account);
                }
            }).show();
        }
        if (view == this.addMicrosoftAccount) {
            AddMicrosoftAccountDialog addMicrosoftAccountDialog = new AddMicrosoftAccountDialog(this.context, this.activity, new AddMicrosoftAccountDialog.OnMicrosoftAccountAddListener() { // from class: com.qcl.launcher.launcher.uis.account.AccountUI$$ExternalSyntheticLambda0
                @Override // com.qcl.launcher.launcher.dialogs.account.AddMicrosoftAccountDialog.OnMicrosoftAccountAddListener
                public final void onPositive(Account account) {
                    AccountUI.this.m473lambda$onClick$2$comqcllauncherlauncheruisaccountAccountUI(account);
                }
            });
            this.addMicrosoftAccountDialog = addMicrosoftAccountDialog;
            addMicrosoftAccountDialog.show();
        }
        if (view == this.addLoginServer) {
            new SelectServerTypeDialog(this.context, new SelectServerTypeDialog.OnServerAddListener() { // from class: com.qcl.launcher.launcher.uis.account.AccountUI$$ExternalSyntheticLambda3
                @Override // com.qcl.launcher.launcher.dialogs.account.SelectServerTypeDialog.OnServerAddListener
                public final void onServerAdd(AuthlibInjectorServer authlibInjectorServer) {
                    AccountUI.this.m474lambda$onClick$3$comqcllauncherlauncheruisaccountAccountUI(authlibInjectorServer);
                }
            }).show();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onClick$0$com-qcl-launcher-launcher-uis-account-AccountUI, reason: not valid java name */
    public /* synthetic */ void m471lambda$onClick$0$comqcllauncherlauncheruisaccountAccountUI(Account account) {
        this.activity.publicGameSetting.account = account;
        GsonUtils.savePublicGameSetting(this.activity.publicGameSetting, AppManifest.SETTING_DIR + "/public_game_setting.json");
        this.accounts.add(account);
        this.accountListAdapter.notifyDataSetChanged();
        GsonUtils.saveAccounts(this.accounts, AppManifest.ACCOUNT_DIR + "/accounts.json");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onClick$1$com-qcl-launcher-launcher-uis-account-AccountUI, reason: not valid java name */
    public /* synthetic */ void m472lambda$onClick$1$comqcllauncherlauncheruisaccountAccountUI(Account account) {
        this.activity.publicGameSetting.account = account;
        GsonUtils.savePublicGameSetting(this.activity.publicGameSetting, AppManifest.SETTING_DIR + "/public_game_setting.json");
        this.accounts.add(account);
        this.accountListAdapter.notifyDataSetChanged();
        GsonUtils.saveAccounts(this.accounts, AppManifest.ACCOUNT_DIR + "/accounts.json");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onClick$2$com-qcl-launcher-launcher-uis-account-AccountUI, reason: not valid java name */
    public /* synthetic */ void m473lambda$onClick$2$comqcllauncherlauncheruisaccountAccountUI(Account account) {
        ArrayList arrayList = new ArrayList();
        Iterator<Account> it = this.accounts.iterator();
        while (it.hasNext()) {
            Account existingAccount = it.next();
            if (existingAccount.loginType == 3) {
                // ★★★ 修复：收集【已有】微软账户的玩家名，而不是新账号名。
                //   原代码误写为 arrayList.add(account.auth_player_name)（新账号名），
                //   导致只要已存在一个微软账户，contains 必为 true → 永远拒绝添加第二个微软账户。
                arrayList.add(existingAccount.auth_player_name);
            }
        }
        if (arrayList.contains(account.auth_player_name)) {
            return;
        }
        this.activity.publicGameSetting.account = account;
        GsonUtils.savePublicGameSetting(this.activity.publicGameSetting, AppManifest.SETTING_DIR + "/public_game_setting.json");
        this.accounts.add(account);
        this.accountListAdapter.notifyDataSetChanged();
        GsonUtils.saveAccounts(this.accounts, AppManifest.ACCOUNT_DIR + "/accounts.json");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onClick$3$com-qcl-launcher-launcher-uis-account-AccountUI, reason: not valid java name */
    public /* synthetic */ void m474lambda$onClick$3$comqcllauncherlauncheruisaccountAccountUI(AuthlibInjectorServer authlibInjectorServer) {
        if (this.serverList.contains(authlibInjectorServer)) {
            return;
        }
        this.serverList.add(authlibInjectorServer);
        this.serverListAdapter.notifyDataSetChanged();
        GsonUtils.saveServer(this.serverList, AppManifest.ACCOUNT_DIR + "/authlib_injector_server.json");
    }
}
