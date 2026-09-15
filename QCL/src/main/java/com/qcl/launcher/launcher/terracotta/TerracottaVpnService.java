package com.qcl.launcher.launcher.terracotta;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import net.burningtnt.terracotta.TerracottaAndroidAPI;

/**
 * Terracotta（陶瓷联机）的 VPN 服务：把 EasyTier 的虚拟网卡交给 Terracotta 原生层。
 * 逻辑移植自 FCL 的 TerracottaVPNService（GPL-3.0），按 QCL 的工具集改写。
 */
@SuppressLint("VpnServicePolicy")
public class TerracottaVpnService extends VpnService {

    private static final String TAG = "QCLTerracottaVPN";
    private static final String CHANNEL_ID = "qcl_terracotta_vpn";
    private static final int NOTIFICATION_ID = 0x5151;

    public static final String ACTION_START = "net.burningtnt.terracotta.action.START";
    public static final String ACTION_STOP = "net.burningtnt.terracotta.action.STOP";
    public static final String EXTRA_STATE_TEXT = "terracotta_state_text";

    private static volatile boolean running = false;

    private NotificationManager notificationManager;
    private ParcelFileDescriptor vpnInterface;
    private String stateText = "多人联机已连接";

    public static boolean isRunning() {
        return running;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (notificationManager == null) {
            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }
        if (ACTION_STOP.equals(action)) {
            cleanup();
            stopForeground(true);
            stopSelf();
            running = false;
            return Service.START_NOT_STICKY;
        }
        if (intent != null && intent.hasExtra(EXTRA_STATE_TEXT)) {
            stateText = intent.getStringExtra(EXTRA_STATE_TEXT);
            if (notificationManager != null) {
                notificationManager.notify(NOTIFICATION_ID, buildNotification());
            }
        }
        running = true;
        createChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
        try {
            Builder builder = new Builder().setSession("QCL Multiplayer (Terracotta)");
            try {
                builder.addDisallowedApplication(getPackageName());
            } catch (PackageManager.NameNotFoundException ignored) {
            }
            TerracottaAndroidAPI.VpnServiceRequest request = TerracottaAndroidAPI.getPendingVpnServiceRequest();
            if (request != null) {
                vpnInterface = request.startVpnService(builder);
            }
        } catch (Throwable t) {
            Log.e(TAG, "startVpnService failed", t);
        }
        return Service.START_STICKY;
    }

    @Override
    public void onRevoke() {
        Log.w(TAG, "onRevoke(): VPN preempted or revoked");
        cleanup();
        stopForeground(true);
        stopSelf();
        running = false;
        super.onRevoke();
    }

    @Override
    public void onDestroy() {
        cleanup();
        running = false;
        super.onDestroy();
    }

    private void cleanup() {
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (Throwable ignored) {
            }
            vpnInterface = null;
        }
    }

    private void createChannel() {
        if (notificationManager == null) return;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
                && notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "QCL 多人联机",
                    NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Notification.Builder builder = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, com.qcl.launcher.launcher.MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return builder
                .setContentTitle("QCL 多人联机")
                .setContentText(stateText)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setOngoing(true)
                .setContentIntent(contentIntent)
                .build();
    }
}
