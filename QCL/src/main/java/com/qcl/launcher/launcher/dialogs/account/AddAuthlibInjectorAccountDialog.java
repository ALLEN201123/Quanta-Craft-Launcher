package com.qcl.launcher.launcher.dialogs.account;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.qcl.launcher.auth.Account;
import com.qcl.launcher.auth.AuthInfo;
import com.qcl.launcher.auth.AuthenticationException;
import com.qcl.launcher.auth.authlibinjector.AuthlibInjectorServer;
import com.qcl.launcher.auth.yggdrasil.GameProfile;
import com.qcl.launcher.auth.yggdrasil.Texture;
import com.qcl.launcher.auth.yggdrasil.TextureType;
import com.qcl.launcher.auth.yggdrasil.YggdrasilService;
import com.qcl.launcher.auth.yggdrasil.YggdrasilSession;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.dialogs.account.SelectServerTypeDialog;
import com.qcl.launcher.launcher.list.account.server.AuthlibInjectorServerSpinnerAdapter;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.skin.utils.Avatar;
import com.qcl.launcher.utils.gson.GsonUtils;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.UUID;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class AddAuthlibInjectorAccountDialog extends Dialog implements View.OnClickListener, AdapterView.OnItemSelectedListener {
    public static final String NIDE_8_AUTH_SIGN_UP_PAGE = "https://login.mc-user.com:233/";
    private Account account;
    private MainActivity activity;
    private ImageButton addServer;
    private AuthlibInjectorServer authlibInjectorServer;
    private Button cancel;
    private EditText editEmail;
    private EditText editPassword;
    private Spinner editServer;
    private ArrayList<AuthlibInjectorServer> list;
    private Button login;
    public final Handler loginHandler;
    private OnAuthlibInjectorAccountAddListener onAuthlibInjectorAccountAddListener;
    private ProgressBar progressBar;
    private AuthlibInjectorServerSpinnerAdapter serverListAdapter;
    private TextView signUp;
    private String signUpUrl;

    /* loaded from: classes2.dex */
    public interface OnAuthlibInjectorAccountAddListener {
        void onAccountAdd(Account account);
    }

    @Override // android.widget.AdapterView.OnItemSelectedListener
    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    public AddAuthlibInjectorAccountDialog(Context context, MainActivity mainActivity, OnAuthlibInjectorAccountAddListener onAuthlibInjectorAccountAddListener, ArrayList<AuthlibInjectorServer> arrayList, AuthlibInjectorServer authlibInjectorServer) {
        super(context);
        this.loginHandler = new Handler() { // from class: com.qcl.launcher.launcher.dialogs.account.AddAuthlibInjectorAccountDialog.1
            @Override // android.os.Handler
            public void handleMessage(Message message) {
                super.handleMessage(message);
                if (message.what == 0) {
                    AddAuthlibInjectorAccountDialog.this.onAuthlibInjectorAccountAddListener.onAccountAdd(AddAuthlibInjectorAccountDialog.this.account);
                    AddAuthlibInjectorAccountDialog.this.dismiss();
                }
                if (message.what == 1) {
                    Toast.makeText(AddAuthlibInjectorAccountDialog.this.getContext(), AddAuthlibInjectorAccountDialog.this.getContext().getString(R.string.dialog_add_authlib_injector_account_failed), 0).show();
                }
                if (message.what == 2) {
                    Toast.makeText(AddAuthlibInjectorAccountDialog.this.getContext(), AddAuthlibInjectorAccountDialog.this.getContext().getString(R.string.dialog_add_authlib_injector_account_none), 0).show();
                }
            }
        };
        this.activity = mainActivity;
        this.onAuthlibInjectorAccountAddListener = onAuthlibInjectorAccountAddListener;
        this.list = arrayList;
        this.authlibInjectorServer = authlibInjectorServer;
        setContentView(R.layout.dialog_add_authlib_injector_account);
        setCancelable(false);
        init();
    }

    private void init() {
        this.editServer = (Spinner) findViewById(R.id.edit_server);
        this.signUp = (TextView) findViewById(R.id.sign_up);
        this.addServer = (ImageButton) findViewById(R.id.add_server);
        this.editEmail = (EditText) findViewById(R.id.edit_email);
        this.editPassword = (EditText) findViewById(R.id.edit_password);
        this.login = (Button) findViewById(R.id.login_authlib);
        this.cancel = (Button) findViewById(R.id.cancel_login_authlib);
        this.progressBar = (ProgressBar) findViewById(R.id.login_progress);
        this.signUp.setOnClickListener(this);
        this.addServer.setOnClickListener(this);
        this.login.setOnClickListener(this);
        this.cancel.setOnClickListener(this);
        AuthlibInjectorServerSpinnerAdapter authlibInjectorServerSpinnerAdapter = new AuthlibInjectorServerSpinnerAdapter(getContext(), this.list);
        this.serverListAdapter = authlibInjectorServerSpinnerAdapter;
        this.editServer.setAdapter((SpinnerAdapter) authlibInjectorServerSpinnerAdapter);
        this.editServer.setOnItemSelectedListener(this);
        this.editServer.setSelection(this.serverListAdapter.getItemPosition(this.authlibInjectorServer));
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.signUp) {
            getContext().startActivity(new Intent("android.intent.action.VIEW", Uri.parse(this.signUpUrl)));
        }
        if (view == this.addServer) {
            new SelectServerTypeDialog(getContext(), new SelectServerTypeDialog.OnServerAddListener() { // from class: com.qcl.launcher.launcher.dialogs.account.AddAuthlibInjectorAccountDialog$$ExternalSyntheticLambda0
                @Override // com.qcl.launcher.launcher.dialogs.account.SelectServerTypeDialog.OnServerAddListener
                public final void onServerAdd(AuthlibInjectorServer authlibInjectorServer) {
                    AddAuthlibInjectorAccountDialog.this.m251x6effd98(authlibInjectorServer);
                }
            }).show();
        }
        if (view == this.login) {
            if (this.editEmail.getText().toString().equals("") || this.editPassword.getText().toString().equals("")) {
                Toast.makeText(getContext(), getContext().getString(R.string.dialog_add_authlib_injector_account_empty_warn), 0).show();
            } else {
                final String obj = this.editEmail.getText().toString();
                final String obj2 = this.editPassword.getText().toString();
                final boolean startsWith = this.authlibInjectorServer.getUrl().startsWith("https://auth.mc-user.com:233/");
                new Thread(new Runnable() { // from class: com.qcl.launcher.launcher.dialogs.account.AddAuthlibInjectorAccountDialog$$ExternalSyntheticLambda5
                    @Override // java.lang.Runnable
                    public final void run() {
                        AddAuthlibInjectorAccountDialog.this.m256x956d35d(obj, obj2, startsWith);
                    }
                }).start();
            }
        }
        if (view == this.cancel) {
            dismiss();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onClick$0$com-qcl-launcher-launcher-dialogs-account-AddAuthlibInjectorAccountDialog, reason: not valid java name */
    public /* synthetic */ void m251x6effd98(AuthlibInjectorServer authlibInjectorServer) {
        if (this.activity.uiManager.accountUI.serverList.contains(authlibInjectorServer)) {
            return;
        }
        this.activity.uiManager.accountUI.serverList.add(authlibInjectorServer);
        this.activity.uiManager.accountUI.serverListAdapter.notifyDataSetChanged();
        GsonUtils.saveServer(this.activity.uiManager.accountUI.serverList, AppManifest.ACCOUNT_DIR + "/authlib_injector_server.json");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onClick$5$com-qcl-launcher-launcher-dialogs-account-AddAuthlibInjectorAccountDialog, reason: not valid java name */
    public /* synthetic */ void m256x956d35d(final String str, final String str2, final boolean z) {
        Bitmap decodeStream;
        this.loginHandler.post(new Runnable() { // from class: com.qcl.launcher.launcher.dialogs.account.AddAuthlibInjectorAccountDialog$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                AddAuthlibInjectorAccountDialog.this.m252x3a9e2859();
            }
        });
        final YggdrasilService yggdrasilService = this.authlibInjectorServer.getYggdrasilService();
        try {
            final YggdrasilSession authenticate = yggdrasilService.authenticate(str, str2, UUID.randomUUID().toString());
            if (authenticate.getAvailableProfiles().size() > 1) {
                final ArrayList arrayList = new ArrayList();
                for (GameProfile gameProfile : authenticate.getAvailableProfiles()) {
                    if (yggdrasilService.getCompleteGameProfile(gameProfile.getId()).isPresent() && YggdrasilService.getTextures(yggdrasilService.getCompleteGameProfile(gameProfile.getId()).get()).isPresent()) {
                        Texture texture = YggdrasilService.getTextures(yggdrasilService.getCompleteGameProfile(gameProfile.getId()).get()).get().get(TextureType.SKIN);
                        if (texture == null) {
                            arrayList.add(BitmapFactory.decodeStream(getContext().getAssets().open("img/alex.png")));
                        } else {
                            String url = texture.getUrl();
                            if (!url.startsWith("https")) {
                                url = url.replaceFirst("http", "https");
                            }
                            HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(url).openConnection();
                            httpURLConnection.setDoInput(true);
                            httpURLConnection.connect();
                            arrayList.add(BitmapFactory.decodeStream(httpURLConnection.getInputStream()));
                        }
                    } else {
                        arrayList.add(BitmapFactory.decodeStream(getContext().getAssets().open("img/alex.png")));
                    }
                }
                this.loginHandler.post(new Runnable() { // from class: com.qcl.launcher.launcher.dialogs.account.AddAuthlibInjectorAccountDialog$$ExternalSyntheticLambda4
                    @Override // java.lang.Runnable
                    public final void run() {
                        AddAuthlibInjectorAccountDialog.this.m253x6e4c531a(yggdrasilService, authenticate, str, str2, arrayList, z);
                    }
                });
            } else if (authenticate.getAvailableProfiles().size() == 1) {
                final AuthInfo authInfo = authenticate.toAuthInfo();
                if (yggdrasilService.getCompleteGameProfile(authInfo.getUUID()).isPresent() && YggdrasilService.getTextures(yggdrasilService.getCompleteGameProfile(authInfo.getUUID()).get()).isPresent()) {
                    Texture texture2 = YggdrasilService.getTextures(yggdrasilService.getCompleteGameProfile(authInfo.getUUID()).get()).get().get(TextureType.SKIN);
                    if (texture2 == null) {
                        decodeStream = BitmapFactory.decodeStream(getContext().getAssets().open("img/alex.png"));
                    } else {
                        String url2 = texture2.getUrl();
                        if (!url2.startsWith("https")) {
                            url2 = url2.replaceFirst("http", "https");
                        }
                        HttpURLConnection httpURLConnection2 = (HttpURLConnection) new URL(url2).openConnection();
                        httpURLConnection2.setDoInput(true);
                        httpURLConnection2.connect();
                        decodeStream = BitmapFactory.decodeStream(httpURLConnection2.getInputStream());
                    }
                } else {
                    decodeStream = BitmapFactory.decodeStream(getContext().getAssets().open("img/alex.png"));
                }
                final Bitmap bitmap = decodeStream;
                this.loginHandler.post(new Runnable() { // from class: com.qcl.launcher.launcher.dialogs.account.AddAuthlibInjectorAccountDialog$$ExternalSyntheticLambda3
                    @Override // java.lang.Runnable
                    public final void run() {
                        AddAuthlibInjectorAccountDialog.this.m254xa1fa7ddb(bitmap, z, str, str2, authenticate, authInfo);
                    }
                });
                this.loginHandler.sendEmptyMessage(0);
            } else {
                this.loginHandler.sendEmptyMessage(2);
            }
        } catch (AuthenticationException | IOException e) {
            e.printStackTrace();
            this.loginHandler.sendEmptyMessage(1);
        }
        this.loginHandler.post(new Runnable() { // from class: com.qcl.launcher.launcher.dialogs.account.AddAuthlibInjectorAccountDialog$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                AddAuthlibInjectorAccountDialog.this.m255xd5a8a89c();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onClick$1$com-qcl-launcher-launcher-dialogs-account-AddAuthlibInjectorAccountDialog, reason: not valid java name */
    public /* synthetic */ void m252x3a9e2859() {
        this.progressBar.setVisibility(0);
        this.login.setVisibility(8);
        this.cancel.setEnabled(false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onClick$2$com-qcl-launcher-launcher-dialogs-account-AddAuthlibInjectorAccountDialog, reason: not valid java name */
    public /* synthetic */ void m253x6e4c531a(YggdrasilService yggdrasilService, YggdrasilSession yggdrasilSession, String str, String str2, ArrayList arrayList, boolean z) {
        new SelectProfileDialog(getContext(), yggdrasilService, yggdrasilSession, str, str2, this.authlibInjectorServer.getUrl(), arrayList, this.onAuthlibInjectorAccountAddListener, z).show();
        dismiss();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onClick$3$com-qcl-launcher-launcher-dialogs-account-AddAuthlibInjectorAccountDialog, reason: not valid java name */
    public /* synthetic */ void m254xa1fa7ddb(Bitmap bitmap, boolean z, String str, String str2, YggdrasilSession yggdrasilSession, AuthInfo authInfo) {
        this.account = new Account(z ? 5 : 4, str, str2, "mojang", "0", yggdrasilSession.getSelectedProfile().getName(), authInfo.getUUID().toString(), authInfo.getAccessToken(), yggdrasilSession.getClientToken(), "", this.authlibInjectorServer.getUrl(), Avatar.bitmapToString(bitmap));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onClick$4$com-qcl-launcher-launcher-dialogs-account-AddAuthlibInjectorAccountDialog, reason: not valid java name */
    public /* synthetic */ void m255xd5a8a89c() {
        this.progressBar.setVisibility(8);
        this.login.setVisibility(0);
        this.cancel.setEnabled(true);
    }

    @Override // android.widget.AdapterView.OnItemSelectedListener
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
        if (adapterView == this.editServer) {
            AuthlibInjectorServer authlibInjectorServer = (AuthlibInjectorServer) this.serverListAdapter.getItem(i);
            this.authlibInjectorServer = authlibInjectorServer;
            this.signUpUrl = authlibInjectorServer.getLinks().get("register");
            if (this.authlibInjectorServer.getUrl().startsWith("https://auth.mc-user.com:233/")) {
                this.signUpUrl = "https://login.mc-user.com:233/" + this.authlibInjectorServer.getUrl().substring(this.authlibInjectorServer.getUrl().length() - 33);
            }
            if (this.signUpUrl == null) {
                this.signUp.setVisibility(8);
            } else {
                this.signUp.setVisibility(0);
            }
        }
    }
}
