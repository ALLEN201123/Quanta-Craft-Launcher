package com.qcl.launcher.auth.microsoft;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import com.qcl.launcher.utils.LocaleUtils;
import com.qcl.launcher.utils.activity.ActivityUtils;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class MicrosoftLoginActivity extends AppCompatActivity {
    public static final int AUTHENTICATE_MICROSOFT_REQUEST = 2000;
    private ProgressBar progressBar;
    private WebView webView;

    private boolean isFullscreen() {
        try {
            return getIntent().getExtras() != null
                    && getIntent().getExtras().getBoolean("fullscreen");
        } catch (Throwable t) {
            return false;
        }
    }

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        // ★ 2026-10-07：getIntent().getExtras() 原来没判空，缺 extras 时直接 NPE。
        if (Build.VERSION.SDK_INT >= 28) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    isFullscreen() ? 1 : 2;
        }
        getWindow().setFlags(256, 256);
        // ★ 软键盘：全屏沉浸下默认不 resize，微软登录页的输入框会被键盘盖住。
        setContentView(R.layout.activity_microsoft_login);
        this.progressBar = (ProgressBar) findViewById(R.id.web_loading_progress);
        WebView webView = (WebView) findViewById(R.id.web_view);
        this.webView = webView;
        webView.setWebViewClient(new WebViewTrackClient());
        WebSettings settings = this.webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setCacheMode(2);
        // ★ 微软登录是 SPA，依赖 localStorage/sessionStorage，缺了会白屏或卡在第一步。
        settings.setDomStorageEnabled(true);
        // ★ 焦点：Android 上 WebView 内的 <input> 要弹软键盘，WebView 自身必须先拿到焦点。
        this.webView.setFocusable(true);
        this.webView.setFocusableInTouchMode(true);
        this.webView.requestFocus();
        this.webView.loadUrl("https://login.live.com/oauth20_authorize.srf?client_id=00000000402b5328&response_type=code&scope=service%3A%3Auser.auth.xboxlive.com%3A%3AMBI_SSL&redirect_url=https%3A%2F%2Flogin.live.com%2Foauth20_desktop.srf");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ★ 从键盘/别的界面回来时焦点常被系统清掉，点输入框不弹键盘，这里补回来。
        if (this.webView != null) this.webView.requestFocus();
    }

    /* loaded from: classes2.dex */
    class WebViewTrackClient extends WebViewClient {
        WebViewTrackClient() {
        }

        @Override // android.webkit.WebViewClient
        public boolean shouldOverrideUrlLoading(WebView webView, String str) {
            if (str.startsWith("ms-xal-00000000402b5328")) {
                Intent intent = new Intent();
                intent.setData(Uri.parse(str));
                if (MicrosoftLoginActivity.this.progressBar != null) {
                    MicrosoftLoginActivity.this.progressBar.setVisibility(8);
                }
                MicrosoftLoginActivity.this.setResult(-1, intent);
                ActivityUtils.clearWebViewCache(MicrosoftLoginActivity.this);
                MicrosoftLoginActivity.this.finish();
                return true;
            }
            return super.shouldOverrideUrlLoading(webView, str);
        }

        @Override // android.webkit.WebViewClient
        public void onPageStarted(WebView webView, String str, Bitmap bitmap) {
            if (MicrosoftLoginActivity.this.progressBar != null) {
                MicrosoftLoginActivity.this.progressBar.setVisibility(0);
            }
        }

        @Override // android.webkit.WebViewClient
        public void onPageFinished(WebView webView, String str) {
            if (MicrosoftLoginActivity.this.progressBar != null) {
                MicrosoftLoginActivity.this.progressBar.setVisibility(8);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.appcompat.app.AppCompatActivity, android.app.Activity, android.view.ContextThemeWrapper, android.content.ContextWrapper
    public void attachBaseContext(Context context) {
        super.attachBaseContext(LocaleUtils.setLanguage(context));
    }

    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, android.app.Activity, android.content.ComponentCallbacks
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        LocaleUtils.setLanguage(this);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onPostResume() {
        super.onPostResume();
        if (Build.VERSION.SDK_INT >= 28) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    isFullscreen() ? 1 : 2;
        }
        // ★ 从键盘回来时把焦点还给 WebView，否则点输入框不弹软键盘。
        if (this.webView != null) this.webView.requestFocus();
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public void onWindowFocusChanged(boolean z) {
        super.onWindowFocusChanged(z);
        if (z) {
            getWindow().getDecorView().setSystemUiVisibility(5894);
        }
    }
}
