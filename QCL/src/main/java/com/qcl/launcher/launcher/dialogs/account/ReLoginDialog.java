package com.qcl.launcher.launcher.dialogs.account;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.qcl.launcher.auth.Account;
import com.qcl.launcher.auth.AuthInfo;
import com.qcl.launcher.auth.AuthenticationException;
import com.qcl.launcher.auth.yggdrasil.GameProfile;
import com.qcl.launcher.auth.yggdrasil.Texture;
import com.qcl.launcher.auth.yggdrasil.TextureType;
import com.qcl.launcher.auth.yggdrasil.YggdrasilService;
import com.qcl.launcher.auth.yggdrasil.YggdrasilSession;
import com.qcl.launcher.skin.utils.Avatar;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.UUID;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class ReLoginDialog extends Dialog implements View.OnClickListener {
    private Account account;
    private ReloginCallback callback;
    private EditText editPassword;
    private String email;
    private TextView emailText;
    public final Handler loginHandler;
    private Button negative;
    private Button positive;
    private ProgressBar progressBar;
    private YggdrasilService yggdrasilService;

    /* loaded from: classes2.dex */
    public interface ReloginCallback {
        void onCancel();

        void onRelogin(Account account);
    }

    public ReLoginDialog(Context context, String str, YggdrasilService yggdrasilService, Account account, ReloginCallback reloginCallback) {
        super(context);
        this.loginHandler = new Handler() { // from class: com.qcl.launcher.launcher.dialogs.account.ReLoginDialog.1
            @Override // android.os.Handler
            public void handleMessage(Message message) {
                super.handleMessage(message);
                if (message.what == 1) {
                    Toast.makeText(ReLoginDialog.this.getContext(), ReLoginDialog.this.getContext().getString(R.string.dialog_add_authlib_injector_account_failed), 0).show();
                }
            }
        };
        this.email = str;
        this.yggdrasilService = yggdrasilService;
        this.account = account;
        this.callback = reloginCallback;
        setContentView(R.layout.dialog_relogin);
        setCancelable(false);
        init();
    }

    private void init() {
        this.emailText = (TextView) findViewById(R.id.relogin_email);
        this.editPassword = (EditText) findViewById(R.id.edit_password);
        this.positive = (Button) findViewById(R.id.relogin);
        this.negative = (Button) findViewById(R.id.cancel_relogin);
        this.progressBar = (ProgressBar) findViewById(R.id.login_progress);
        this.positive.setOnClickListener(this);
        this.negative.setOnClickListener(this);
        this.emailText.setText(this.email);
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.positive) {
            final String obj = this.editPassword.getText().toString();
            new Thread(new Runnable() { // from class: com.qcl.launcher.launcher.dialogs.account.ReLoginDialog$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    ReLoginDialog.this.m269x1a97f5d5(obj);
                }
            }).start();
        }
        if (view == this.negative) {
            this.callback.onCancel();
            dismiss();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onClick$4$com-qcl-launcher-launcher-dialogs-account-ReLoginDialog, reason: not valid java name */
    public /* synthetic */ void m269x1a97f5d5(final String str) {
        Bitmap decodeStream;
        Bitmap decodeStream2;
        this.loginHandler.post(new Runnable() { // from class: com.qcl.launcher.launcher.dialogs.account.ReLoginDialog$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ReLoginDialog.this.m265x8447d1d1();
            }
        });
        try {
            final YggdrasilSession authenticate = this.yggdrasilService.authenticate(this.email, str, UUID.randomUUID().toString());
            if (authenticate.getAvailableProfiles().size() > 1) {
                Iterator<GameProfile> it = authenticate.getAvailableProfiles().iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    final GameProfile next = it.next();
                    if (next.getName().equals(this.account.auth_player_name)) {
                        Texture texture = YggdrasilService.getTextures(this.yggdrasilService.getCompleteGameProfile(next.getId()).get()).get().get(TextureType.SKIN);
                        if (texture == null) {
                            decodeStream2 = BitmapFactory.decodeStream(getContext().getAssets().open("img/alex.png"));
                        } else {
                            String url = texture.getUrl();
                            if (!url.startsWith("https")) {
                                url = url.replaceFirst("http", "https");
                            }
                            HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(url).openConnection();
                            httpURLConnection.setDoInput(true);
                            httpURLConnection.connect();
                            decodeStream2 = BitmapFactory.decodeStream(httpURLConnection.getInputStream());
                        }
                        final Bitmap bitmap = decodeStream2;
                        this.loginHandler.post(new Runnable() { // from class: com.qcl.launcher.launcher.dialogs.account.ReLoginDialog$$ExternalSyntheticLambda2
                            @Override // java.lang.Runnable
                            public final void run() {
                                ReLoginDialog.this.m266xa9dbdad2(bitmap, str, next, authenticate);
                            }
                        });
                    }
                }
            } else {
                final AuthInfo authInfo = authenticate.toAuthInfo();
                Texture texture2 = YggdrasilService.getTextures(this.yggdrasilService.getCompleteGameProfile(authInfo.getUUID()).get()).get().get(TextureType.SKIN);
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
                final Bitmap bitmap2 = decodeStream;
                this.loginHandler.post(new Runnable() { // from class: com.qcl.launcher.launcher.dialogs.account.ReLoginDialog$$ExternalSyntheticLambda3
                    @Override // java.lang.Runnable
                    public final void run() {
                        ReLoginDialog.this.m267xcf6fe3d3(bitmap2, str, authenticate, authInfo);
                    }
                });
            }
        } catch (AuthenticationException | IOException e) {
            e.printStackTrace();
            this.loginHandler.sendEmptyMessage(1);
        }
        this.loginHandler.post(new Runnable() { // from class: com.qcl.launcher.launcher.dialogs.account.ReLoginDialog$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                ReLoginDialog.this.m268xf503ecd4();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onClick$0$com-qcl-launcher-launcher-dialogs-account-ReLoginDialog, reason: not valid java name */
    public /* synthetic */ void m265x8447d1d1() {
        this.progressBar.setVisibility(0);
        this.positive.setVisibility(8);
        this.negative.setEnabled(false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onClick$1$com-qcl-launcher-launcher-dialogs-account-ReLoginDialog, reason: not valid java name */
    public /* synthetic */ void m266xa9dbdad2(Bitmap bitmap, String str, GameProfile gameProfile, YggdrasilSession yggdrasilSession) {
        this.callback.onRelogin(new Account(this.account.loginType, this.email, str, this.account.user_type, this.account.auth_session, gameProfile.getName(), gameProfile.getId().toString(), yggdrasilSession.getAccessToken(), yggdrasilSession.getClientToken(), this.account.refresh_token, this.account.loginServer, Avatar.bitmapToString(bitmap)));
        dismiss();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onClick$2$com-qcl-launcher-launcher-dialogs-account-ReLoginDialog, reason: not valid java name */
    public /* synthetic */ void m267xcf6fe3d3(Bitmap bitmap, String str, YggdrasilSession yggdrasilSession, AuthInfo authInfo) {
        this.callback.onRelogin(new Account(4, this.email, str, this.account.user_type, this.account.auth_session, yggdrasilSession.getSelectedProfile().getName(), authInfo.getUUID().toString(), authInfo.getAccessToken(), yggdrasilSession.getClientToken(), this.account.refresh_token, this.account.loginServer, Avatar.bitmapToString(bitmap)));
        dismiss();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onClick$3$com-qcl-launcher-launcher-dialogs-account-ReLoginDialog, reason: not valid java name */
    public /* synthetic */ void m268xf503ecd4() {
        this.progressBar.setVisibility(8);
        this.positive.setVisibility(0);
        this.negative.setEnabled(true);
    }
}
