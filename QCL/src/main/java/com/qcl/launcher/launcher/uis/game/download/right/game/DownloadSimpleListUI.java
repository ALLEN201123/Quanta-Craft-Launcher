package com.qcl.launcher.launcher.uis.game.download.right.game;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.qcl.launcher.R;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;

/**
 * ★★★★★ 2026-10-17（1.5.2 新增）**通用版本选择列表页**。
 *
 * <p>用户反馈（两次）：「Legacy Fabric 怎么弄的跟其他详情页不一样？
 * 那个安装详情页选择版本根本不一样跟其他的」「你加的，其他的也有可能」。
 *
 * <p>★ 问题：我最初给 Cleanroom / Legacy Fabric / Legacy Fabric API 用的是
 * {@code AlertDialog + setItems} 的**弹窗**，而 QCL 里其他所有加载器
 * （Forge / Fabric / Quilt / OptiFine …）用的都是**独立列表页**：
 * 顶部标题栏 + 白色 ListView + 居中进度圈 + 「没有可用版本」占位 + 返回。
 * 两者观感差别很大，玩家会觉得"这不是同一个启动器里的东西"。
 *
 * <p>★ 本类就是那个"和其他页面一致"的列表页 —— **三个新加载器共用它**，
 * 复用 {@code ui_install_simple_list.xml}（照 {@code ui_install_fabric_list.xml} 复制，
 * 只改了 id）。
 *
 * <p>用法（调用方先拉好数据，再切页面）：
 * <pre>
 *   DownloadSimpleListUI ui = activity.uiManager.simpleListUI;
 *   ui.listTitle = "选择 Legacy Fabric 版本";
 *   ui.listItems = new String[]{ "0.14.22", ... };
 *   ui.onPick = (index) -&gt; { ... 选中第 index 项 ...; activity.backToLastUI(); };
 *   activity.uiManager.switchMainUI(ui);
 * </pre>
 */
public class DownloadSimpleListUI extends BaseUI implements AdapterView.OnItemClickListener {

    public LinearLayout simpleListUI;
    public ListView listView;
    private ProgressBar progressBar;
    private TextView emptyView;

    /** 页面标题（调用方设置）。 */
    public String listTitle = "";
    /** 列表项文字（调用方设置）。 */
    public String[] listItems = null;
    /** 选中回调（调用方设置）。 */
    public PickListener onPick = null;

    /** 选中某一项（下标从 0 开始）。 */
    public interface PickListener {
        void onPick(int index);
    }

    public DownloadSimpleListUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.simpleListUI = (LinearLayout) this.activity.findViewById(R.id.ui_install_simple_list);
        this.listView = (ListView) this.activity.findViewById(R.id.simple_version_list);
        this.progressBar = (ProgressBar) this.activity.findViewById(R.id.loading_simple_list_progress);
        this.emptyView = (TextView) this.activity.findViewById(R.id.back_to_install_ui_simple);
        if (this.listView != null) {
            this.listView.setOnItemClickListener(this);
        }
        if (this.emptyView != null) {
            // 「没有可用版本 / 点击返回」占位：点一下退回上一页（与 fabric 页一致）
            this.emptyView.setOnClickListener(v -> {
                try {
                    this.activity.backToLastUI();
                } catch (Throwable ignored) {
                }
            });
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            this.activity.showBarTitle(this.listTitle, true, true);
        } catch (Throwable ignored) {
        }
        if (this.simpleListUI != null) {
            CustomAnimationUtils.showViewFromLeft(this.simpleListUI, this.activity, this.context, true);
        }
        render();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (this.simpleListUI != null) {
            CustomAnimationUtils.hideViewToLeft(this.simpleListUI, this.activity, this.context, true);
        }
    }

    /** 把 listItems 渲染到列表里（onStart 时调用）。 */
    private void render() {
        boolean has = this.listItems != null && this.listItems.length > 0;
        if (this.listView != null) {
            if (has) {
                this.listView.setVisibility(View.VISIBLE);
                this.listView.setAdapter(new SimpleAdapter());
            } else {
                this.listView.setVisibility(View.GONE);
            }
        }
        if (this.progressBar != null) {
            this.progressBar.setVisibility(View.GONE);
        }
        if (this.emptyView != null) {
            this.emptyView.setVisibility(has ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        try {
            // ★ ListView 没有 header，position 就是数据下标（不能减 1，否则整列错位）
            if (this.onPick != null) {
                this.onPick.onPick(position);
            }
            this.activity.backToLastUI();
        } catch (Throwable ignored) {
        }
    }

    /** 简单列表适配器（纯文字行，与工程里其它列表观感一致）。 */
    private final class SimpleAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return DownloadSimpleListUI.this.listItems == null
                    ? 0 : DownloadSimpleListUI.this.listItems.length;
        }

        @Override
        public Object getItem(int position) {
            return DownloadSimpleListUI.this.listItems[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv;
            if (convertView instanceof TextView) {
                tv = (TextView) convertView;
            } else {
                tv = new TextView(DownloadSimpleListUI.this.context);
                tv.setPadding(36, 40, 36, 40);
                tv.setTextSize(15f);
                try {
                    tv.setTextColor(DownloadSimpleListUI.this.context.getResources()
                            .getColor(R.color.colorPureBlack));
                } catch (Throwable ignored) {
                }
            }
            tv.setText(DownloadSimpleListUI.this.listItems[position]);
            return tv;
        }
    }
}
