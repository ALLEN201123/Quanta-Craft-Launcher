package com.qcl.launcher.launcher.list.account.server;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.qcl.launcher.auth.Account;
import com.qcl.launcher.auth.authlibinjector.AuthlibInjectorServer;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.dialogs.account.AddAuthlibInjectorAccountDialog;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.utils.gson.GsonUtils;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.commons.lang3.StringUtils;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class AuthlibInjectorServerListAdapter extends BaseAdapter {
    private MainActivity activity;
    private Context context;
    private ArrayList<AuthlibInjectorServer> list;

    @Override // android.widget.Adapter
    public long getItemId(int i) {
        return 0L;
    }

    /* loaded from: classes2.dex */
    private class ViewHolder {
        LinearLayout add;
        ImageButton delete;
        TextView name;
        TextView url;

        private ViewHolder() {
        }
    }

    private String getSimplifiedUrl(String str) {
        return str.startsWith("https://auth.mc-user.com:233/") ? "auth.mc-user.com" : str.substring(StringUtils.ordinalIndexOf(str, "/", 2) + 1, StringUtils.ordinalIndexOf(str, "/", 3));
    }

    public AuthlibInjectorServerListAdapter(Context context, MainActivity mainActivity, ArrayList<AuthlibInjectorServer> arrayList) {
        this.context = context;
        this.activity = mainActivity;
        this.list = arrayList;
    }

    @Override // android.widget.Adapter
    public int getCount() {
        return this.list.size();
    }

    @Override // android.widget.Adapter
    public Object getItem(int i) {
        return this.list.get(i);
    }

    @Override // android.widget.Adapter
    public View getView(int i, View view, ViewGroup viewGroup) {
        View view2;
        ViewHolder viewHolder;
        if (view == null) {
            viewHolder = new ViewHolder();
            view2 = LayoutInflater.from(this.context).inflate(R.layout.item_authlib_injector_server, (ViewGroup) null);
            viewHolder.add = (LinearLayout) view2.findViewById(R.id.add_authlib_injector_account);
            viewHolder.name = (TextView) view2.findViewById(R.id.server_name);
            viewHolder.url = (TextView) view2.findViewById(R.id.server_url);
            viewHolder.delete = (ImageButton) view2.findViewById(R.id.delete_server);
            view2.setTag(viewHolder);
        } else {
            view2 = view;
            viewHolder = (ViewHolder) view.getTag();
        }
        final AuthlibInjectorServer authlibInjectorServer = this.list.get(i);
        viewHolder.name.setText(authlibInjectorServer.getName());
        viewHolder.url.setText(getSimplifiedUrl(authlibInjectorServer.getUrl()));
        viewHolder.add.setOnClickListener(new View.OnClickListener() { // from class: com.qcl.launcher.launcher.list.account.server.AuthlibInjectorServerListAdapter$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view3) {
                AuthlibInjectorServerListAdapter.this.m397xe515d270(authlibInjectorServer, view3);
            }
        });
        viewHolder.delete.setOnClickListener(new View.OnClickListener() { // from class: com.qcl.launcher.launcher.list.account.server.AuthlibInjectorServerListAdapter$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view3) {
                AuthlibInjectorServerListAdapter.this.m398xc309384f(authlibInjectorServer, view3);
            }
        });
        return view2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getView$1$com-qcl-launcher-launcher-list-account-server-AuthlibInjectorServerListAdapter, reason: not valid java name */
    public /* synthetic */ void m397xe515d270(final AuthlibInjectorServer authlibInjectorServer, View view) {
        new AddAuthlibInjectorAccountDialog(this.context, this.activity, new AddAuthlibInjectorAccountDialog.OnAuthlibInjectorAccountAddListener() { // from class: com.qcl.launcher.launcher.list.account.server.AuthlibInjectorServerListAdapter$$ExternalSyntheticLambda2
            @Override // com.qcl.launcher.launcher.dialogs.account.AddAuthlibInjectorAccountDialog.OnAuthlibInjectorAccountAddListener
            public final void onAccountAdd(Account account) {
                AuthlibInjectorServerListAdapter.this.m396x7226c91(authlibInjectorServer, account);
            }
        }, this.list, authlibInjectorServer).show();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getView$0$com-qcl-launcher-launcher-list-account-server-AuthlibInjectorServerListAdapter, reason: not valid java name */
    public /* synthetic */ void m396x7226c91(AuthlibInjectorServer authlibInjectorServer, Account account) {
        Iterator<Account> it = this.activity.uiManager.accountUI.accounts.iterator();
        boolean z = false;
        while (it.hasNext()) {
            Account next = it.next();
            if (next.loginType == 4 && next.loginServer.equals(authlibInjectorServer.getUrl())) {
                z = next.email.equals(account.email) && next.auth_player_name.equals(account.auth_player_name);
                if (z) {
                    break;
                }
            }
        }
        if (z) {
            return;
        }
        this.activity.publicGameSetting.account = account;
        GsonUtils.savePublicGameSetting(this.activity.publicGameSetting, AppManifest.SETTING_DIR + "/public_game_setting.json");
        this.activity.uiManager.accountUI.accounts.add(account);
        this.activity.uiManager.accountUI.accountListAdapter.notifyDataSetChanged();
        GsonUtils.saveAccounts(this.activity.uiManager.accountUI.accounts, AppManifest.ACCOUNT_DIR + "/accounts.json");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getView$2$com-qcl-launcher-launcher-list-account-server-AuthlibInjectorServerListAdapter, reason: not valid java name */
    public /* synthetic */ void m398xc309384f(AuthlibInjectorServer authlibInjectorServer, View view) {
        this.activity.uiManager.accountUI.serverList.remove(authlibInjectorServer);
        this.activity.uiManager.accountUI.serverListAdapter.notifyDataSetChanged();
        GsonUtils.saveServer(this.activity.uiManager.accountUI.serverList, AppManifest.ACCOUNT_DIR + "/authlib_injector_server.json");
        Iterator<Account> it = this.activity.uiManager.accountUI.accounts.iterator();
        while (it.hasNext()) {
            Account next = it.next();
            if (next.loginServer.equals(authlibInjectorServer.getUrl())) {
                boolean z = next.email.equals(this.activity.publicGameSetting.account.email) && next.auth_player_name.equals(this.activity.publicGameSetting.account.auth_player_name) && next.auth_uuid.equals(this.activity.publicGameSetting.account.auth_uuid) && next.loginServer.equals(this.activity.publicGameSetting.account.loginServer);
                it.remove();
                GsonUtils.saveAccounts(this.activity.uiManager.accountUI.accounts, AppManifest.ACCOUNT_DIR + "/accounts.json");
                if (this.activity.uiManager.accountUI.accounts.size() == 0) {
                    this.activity.publicGameSetting.account = new Account(0, "", "", "", "", "", "", "", "", "", "", "");
                } else if (z) {
                    this.activity.publicGameSetting.account = this.activity.uiManager.accountUI.accounts.get(0);
                }
                GsonUtils.savePublicGameSetting(this.activity.publicGameSetting, AppManifest.SETTING_DIR + "/public_game_setting.json");
                this.activity.uiManager.accountUI.accountListAdapter.notifyDataSetChanged();
            }
        }
    }
}
