package com.qcl.launcher.launcher.launch.pojav;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

/**
 * 游戏运行期间的**保活前台服务**。
 *
 * <p><b>修的问题</b>：玩家把游戏切到后台（回桌面、切到别的 App），几秒后游戏就挂掉，
 * 只能杀掉后台重新启动。原因是游戏 Activity 没有前台服务 —— 一旦离开前台，
 * 系统（尤其国产 ROM 的省电策略）很快就把整个进程回收，而游戏 JVM 是跑在这个
 * 进程里的（Pojav 用 JNI 启的 JVM），进程一没，游戏自然就没了。
 *
 * <p><b>做法</b>：游戏 Activity 进入前台时启动本服务并调用 {@code startForeground}
 * 挂一条常驻通知。前台服务优先级高，切后台不会被回收；玩家回到游戏时停止它。
 *
 * <p>通知渠道用最低重要性（IMPORTANCE_MIN），不会有声音、不会弹横幅，只在通知栏
 * 留一条"游戏正在运行"，玩家随时可以手动划掉（划掉不影响服务）。
 */
public class GameAliveService extends Service {

    private static final String TAG = "QCL-alive";
    private static final String CHANNEL_ID = "qcl_game_running";
    private static final int NOTIFY_ID = 0x0C30;

    public static void start(Context ctx) {
        try {
            Intent it = new Intent(ctx, GameAliveService.class);
            if (Build.VERSION.SDK_INT >= 26) {
                ctx.startForegroundService(it);
            } else {
                ctx.startService(it);
            }
        } catch (Throwable t) {
            Log.w(TAG, "启动保活服务失败: " + t);
        }
    }

    public static void stop(Context ctx) {
        try {
            ctx.stopService(new Intent(ctx, GameAliveService.class));
        } catch (Throwable ignored) {
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
                    NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "游戏运行中",
                            NotificationManager.IMPORTANCE_MIN);
                    ch.setShowBadge(false);
                    nm.createNotificationChannel(ch);
                }
            }
            Notification.Builder b;
            if (Build.VERSION.SDK_INT >= 26) {
                b = new Notification.Builder(this, CHANNEL_ID);
            } else {
                b = new Notification.Builder(this);
            }
            b.setContentTitle("QCL 正在运行")
                    .setContentText("游戏在后台继续运行，不会被系统清理")
                    .setSmallIcon(android.R.drawable.ic_menu_compass)
                    .setOngoing(true);
            startForeground(NOTIFY_ID, b.build());
            Log.i(TAG, "保活前台服务已启动");
        } catch (Throwable t) {
            Log.w(TAG, "startForeground 失败: " + t);
        }
        // 被系统回收后自动重建，保证保活不中断
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        try {
            stopForeground(true);
        } catch (Throwable ignored) {
        }
        super.onDestroy();
    }
}
