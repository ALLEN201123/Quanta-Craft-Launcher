package com.qcl.launcher.utils.io;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import java.util.Timer;
import java.util.TimerTask;

/* loaded from: classes2.dex */
public class NetSpeedTimer {
    private static final int ERROR_CODE = -101011010;
    public static final int NET_SPEED_TIMER_DEFAULT = 101010;
    private Context mContext;
    private Handler mHandler;
    private NetSpeed mNetSpeed;
    private SpeedTimerTask mSpeedTimerTask;
    private long defaultDelay = 1000;
    private long defaultPeriod = 1000;
    private int mMsgWhat = -101011010;

    public NetSpeedTimer(Context context, NetSpeed netSpeed, Handler handler) {
        this.mContext = context;
        this.mNetSpeed = netSpeed;
        this.mHandler = handler;
    }

    public NetSpeedTimer setDelayTime(long j) {
        this.defaultDelay = j;
        return this;
    }

    public NetSpeedTimer setPeriodTime(long j) {
        this.defaultPeriod = j;
        return this;
    }

    public NetSpeedTimer setHanderWhat(int i) {
        this.mMsgWhat = i;
        return this;
    }

    public void startSpeedTimer() {
        Timer timer = new Timer();
        SpeedTimerTask speedTimerTask = new SpeedTimerTask(this.mContext, this.mNetSpeed, this.mHandler, this.mMsgWhat);
        this.mSpeedTimerTask = speedTimerTask;
        timer.schedule(speedTimerTask, this.defaultDelay, this.defaultPeriod);
    }

    public void stopSpeedTimer() {
        SpeedTimerTask speedTimerTask = this.mSpeedTimerTask;
        if (speedTimerTask != null) {
            speedTimerTask.cancel();
        }
    }

    /* loaded from: classes2.dex */
    private static class SpeedTimerTask extends TimerTask {
        private Context mContext;
        private Handler mHandler;
        private int mMsgWhat;
        private NetSpeed mNetSpeed;

        public SpeedTimerTask(Context context, NetSpeed netSpeed, Handler handler, int i) {
            this.mContext = context;
            this.mHandler = handler;
            this.mNetSpeed = netSpeed;
            this.mMsgWhat = i;
        }

        @Override // java.util.TimerTask, java.lang.Runnable
        public void run() {
            Handler handler;
            if (this.mNetSpeed == null || (handler = this.mHandler) == null) {
                return;
            }
            Message obtainMessage = handler.obtainMessage();
            int i = this.mMsgWhat;
            if (i != -101011010) {
                obtainMessage.what = i;
            } else {
                obtainMessage.what = 101010;
            }
            obtainMessage.obj = this.mNetSpeed.getNetSpeed(this.mContext.getApplicationInfo().uid);
            this.mHandler.sendMessage(obtainMessage);
        }
    }
}
