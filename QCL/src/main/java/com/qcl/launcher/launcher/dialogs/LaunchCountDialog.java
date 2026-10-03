package com.qcl.launcher.launcher.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.qcl.launcher.R;

/**
 * ★ 1.4.5：启动次数提示弹窗。
 *
 * <p>玩家累计启动游戏达到一定次数时弹一次，显示"你已经启动了几次来着"，
 * 给两个选择：<b>赞助一下</b>（打开作者的赞助页）/ <b>以后再说</b>（直接关掉，继续启动游戏）。
 *
 * <p>只在**里程碑次数**弹（20 / 60 / 80 / 100 …），点过任意一个按钮后把该里程碑记下来，
 * 不会反复弹；记录存在 {@code launcher_setting.json} 的
 * {@code gameLaunchCount} / {@code lastLaunchPromptAt} 两个字段里。
 */
public class LaunchCountDialog extends Dialog implements View.OnClickListener {

    /**
     * ★ 1.4.5：QCL 官方赞助页（作者朋友重做的新页面，2026-10-03 起启用）。
     * <p>改这里就够了：设置→赞助、验证弹窗 与 启动次数弹窗 都用同一个地址。
     * <p>⚠️ 用 https：该站 http 会 302 跳到 https，直连 https 少一跳。
     */
    public static final String SPONSOR_URL = "https://qcl5g.de5.net/";

    private final OnDismissedListener listener;
    private Button later;
    private Button sponsor;

    /** 关掉弹窗后的回调（用于"记下里程碑"并继续启动游戏）。 */
    public interface OnDismissedListener {
        void onDismissed();
    }

    public LaunchCountDialog(Context context, int launchCount, OnDismissedListener listener) {
        super(context);
        this.listener = listener;
        setContentView(R.layout.dialog_launch_count);
        setCancelable(false);
        init(launchCount);
    }

    private void init(int launchCount) {
        TextView text = (TextView) findViewById(R.id.launch_count_text);
        if (text != null) {
            text.setText(getContext().getString(R.string.dialog_launch_count_msg, launchCount));
        }
        this.later = (Button) findViewById(R.id.launch_count_later);
        this.sponsor = (Button) findViewById(R.id.launch_count_sponsor);
        if (this.later != null) {
            this.later.setOnClickListener(this);
        }
        if (this.sponsor != null) {
            this.sponsor.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View view) {
        if (view == this.sponsor) {
            // ★ 打开赞助页：未成年人请在页面上看到"未成年不允许捐款"的说明后再决定
            try {
                getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(SPONSOR_URL)));
            } catch (Throwable ignored) {
                // 没浏览器也不该影响启动游戏
            }
        }
        // 「以后再说」与「赞助一下」都算已提示过：关掉弹窗并继续启动游戏
        dismiss();
        if (this.listener != null) {
            this.listener.onDismissed();
        }
    }
}
