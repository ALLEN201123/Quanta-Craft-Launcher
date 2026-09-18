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

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (Build.VERSION.SDK_INT >= 28) {
            if (getIntent().getExtras().getBoolean("fullscreen")) {
                getWindow().getAttributes().layoutInDisplayCutoutMode = 1;
            } else {
                getWindow().getAttributes().layoutInDisplayCutoutMode = 2;
            }
        }
        getWindow().setFlags(256, 256);
        setContentView(R.layout.activity_microsoft_login);
        this.progressBar = (ProgressBar) findViewById(R.id.web_loading_progress);
        WebView webView = (WebView) findViewById(R.id.web_view);
        this.webView = webView;
        webView.setWebViewClient(new WebViewTrackClient());
        WebSettings settings = this.webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setCacheMode(2);
        this.webView.loadUrl("https://login.live.com/oauth20_authorize.srf?client_id=00000000402b5328&response_type=code&scope=service%3A%3Auser.auth.xboxlive.com%3A%3AMBI_SSL&redirect_url=https%3A%2F%2Flogin.live.com%2Foauth20_desktop.srf");
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
                MicrosoftLoginActivity.this.progressBar.setVisibility(8);
                MicrosoftLoginActivity.this.setResult(-1, intent);
                ActivityUtils.clearWebViewCache(MicrosoftLoginActivity.this);
                MicrosoftLoginActivity.this.finish();
                return true;
            }
            return super.shouldOverrideUrlLoading(webView, str);
        }

        @Override // android.webkit.WebViewClient
        public void onPageStarted(WebView webView, String str, Bitmap bitmap) {
            MicrosoftLoginActivity.this.progressBar.setVisibility(0);
        }

        @Override // android.webkit.WebViewClient
        public void onPageFinished(WebView webView, String str) {
            MicrosoftLoginActivity.this.progressBar.setVisibility(8);
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
            if (getIntent().getExtras().getBoolean("fullscreen")) {
                getWindow().getAttributes().layoutInDisplayCutoutMode = 1;
            } else {
                getWindow().getAttributes().layoutInDisplayCutoutMode = 2;
            }
        }
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public void onWindowFocusChanged(boolean z) {
        super.onWindowFocusChanged(z);
        if (z) {
            getWindow().getDecorView().setSystemUiVisibility(5894);
        }
    }
}
