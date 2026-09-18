package net.kdt.pojavlaunch;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Process;
import java.util.ArrayList;
import net.kdt.pojavlaunch.function.ApiInstallerCallback;
import net.kdt.pojavlaunch.utils.JREUtils;

/* loaded from: classes2.dex */
public class PojavApiService extends Service {
    public ApiInstallerCallback callback;

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void startApiInstaller(final String str, final ArrayList<String> arrayList, final String str2, ApiInstallerCallback apiInstallerCallback) {
        this.callback = apiInstallerCallback;
        new Thread(new Runnable() { // from class: net.kdt.pojavlaunch.PojavApiService$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                PojavApiService.this.m2270lambda$startApiInstaller$0$netkdtpojavlaunchPojavApiService(str, arrayList, str2);
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$startApiInstaller$0$net-kdt-pojavlaunch-PojavApiService, reason: not valid java name */
    public /* synthetic */ void m2270lambda$startApiInstaller$0$netkdtpojavlaunchPojavApiService(String str, ArrayList arrayList, String str2) {
        onExit(this, JREUtils.launchAPIInstaller(getApplicationContext(), str, arrayList, str2));
    }

    public static void onExit(Context context, int i) {
        PojavApiService pojavApiService = (PojavApiService) context;
        pojavApiService.callback.onExit(i);
        pojavApiService.stopSelf();
    }

    @Override // android.app.Service
    public void onDestroy() {
        Process.killProcess(Process.myPid());
        super.onDestroy();
    }
}
