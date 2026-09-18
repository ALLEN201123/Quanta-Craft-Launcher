package com.qcl.launcher.launcher.download;

import android.content.Intent;
import android.os.IBinder;
import com.qcl.launcher.manifest.AppManifest;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import net.kdt.pojavlaunch.PojavApiService;
import net.kdt.pojavlaunch.function.ApiInstallerCallback;

/* loaded from: classes2.dex */
public class ApiService extends PojavApiService {
    public static final int API_SERVICE_PORT = 6868;

    @Override // net.kdt.pojavlaunch.PojavApiService, android.app.Service
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int i, int i2) {
        AppManifest.initializeManifest(getApplicationContext());
        startApiInstaller(AppManifest.JAVA_DIR + "/default", intent.getExtras().getStringArrayList("commands"), AppManifest.DEBUG_DIR, new ApiInstallerCallback() { // from class: com.qcl.launcher.launcher.download.ApiService$$ExternalSyntheticLambda0
            @Override // net.kdt.pojavlaunch.function.ApiInstallerCallback
            public final void onExit(int i3) {
                ApiService.lambda$onStartCommand$0(i3);
            }
        });
        return super.onStartCommand(intent, i, i2);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$onStartCommand$0(int i) {
        try {
            DatagramSocket datagramSocket = new DatagramSocket();
            datagramSocket.connect(new InetSocketAddress("127.0.0.1", 6868));
            byte[] bytes = (i + "").getBytes();
            datagramSocket.send(new DatagramPacket(bytes, bytes.length));
            datagramSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
