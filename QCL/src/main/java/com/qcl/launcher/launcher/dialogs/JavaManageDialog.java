package com.qcl.launcher.launcher.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.qcl.launcher.R;
import com.qcl.launcher.utils.game.QclJavaManager;

import java.util.ArrayList;
import java.util.List;

/**
 * ★★★★★ 2026-10-17（1.5.2 新增）**Java 管理对话框**（照 FCL 的 {@code JavaManageDialog}）。
 *
 * <p>用户需求：「别人跟我说 FCL 有这个功能，我也需要加这个」。
 *
 * <p>FCL 的这个对话框同时提供两件事：
 * <ol>
 *   <li>列出本机所有 Java 运行时，可删除；</li>
 *   <li>底部一个「执行 Jar 文件」按钮（FCL 的 jar 执行入口就在这里）。</li>
 * </ol>
 * 本对话框同样把两者放在一起 —— 这也是用户最容易找到 jar 启动入口的地方。
 *
 * <p>★ 纯代码构建 UI（不新增布局文件），减少一个布局 + 一次资源编译，
 *   样式用工程已有的 {@code @drawable/launcher_button_parent} / {@code launcher_view_white}。
 */
public class JavaManageDialog extends AlertDialog {

    private final Context context;
    private final Runnable onRunJar;

    private ListView listView;
    private LinearLayout emptyView;
    private final List<QclJavaManager.JavaEntry> data = new ArrayList<>();
    private Adapter adapter;

    /**
     * @param onRunJar 点「执行 Jar 文件」时的回调（通常是启动 JarLaunchActivity 的文件选择）；
     *                 传 null 则不显示该按钮
     */
    public JavaManageDialog(Context context, Runnable onRunJar) {
        super(context);
        this.context = context;
        this.onRunJar = onRunJar;
    }

    @Override
    public void show() {
        this.listView = new ListView(this.context);
        this.listView.setBackgroundResource(R.drawable.launcher_view_white);
        this.adapter = new Adapter();
        this.listView.setAdapter(this.adapter);

        this.emptyView = new LinearLayout(this.context);
        this.emptyView.setOrientation(LinearLayout.VERTICAL);
        TextView tv = new TextView(this.context);
        tv.setText("还没有检测到任何 Java 运行时。\n\n"
                + "在「设置 → 运行环境」里下载 Java 8 / 17 / 21 / 25 之后，"
                + "它们会出现在这里。");
        tv.setPadding(32, 48, 32, 48);
        this.emptyView.addView(tv);

        LinearLayout root = new LinearLayout(this.context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 16, 24, 8);

        TextView hint = new TextView(this.context);
        hint.setText("下面是本机可用的 Java 运行时。长按某一项可以删除。");
        hint.setTextSize(12f);
        hint.setPadding(0, 0, 0, 12);
        root.addView(hint);

        root.addView(this.listView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        if (this.onRunJar != null) {
            Button runJar = new Button(this.context);
            runJar.setText(this.context.getString(R.string.menu_jar_launch));
            runJar.setOnClickListener(v -> {
                dismiss();
                this.onRunJar.run();
            });
            root.addView(runJar, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        Button close = new Button(this.context);
        close.setText(this.context.getString(R.string.jar_launch_exit));
        close.setOnClickListener(v -> dismiss());
        root.addView(close, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setView(root);
        setTitle("Java 管理");
        reload();
        super.show();
    }

    /** 重新扫描一遍并刷新列表。 */
    private void reload() {
        this.data.clear();
        this.data.addAll(QclJavaManager.scan());
        if (this.listView != null) {
            this.listView.setVisibility(this.data.isEmpty() ? View.GONE : View.VISIBLE);
        }
        if (this.emptyView != null) {
            this.emptyView.setVisibility(this.data.isEmpty() ? View.VISIBLE : View.GONE);
        }
        if (this.adapter != null) {
            this.adapter.notifyDataSetChanged();
        }
    }

    private final class Adapter extends BaseAdapter {
        private final LayoutInflater inflater = LayoutInflater.from(JavaManageDialog.this.context);

        @Override
        public int getCount() {
            return JavaManageDialog.this.data.size();
        }

        @Override
        public Object getItem(int position) {
            return JavaManageDialog.this.data.get(position);
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
                tv = new TextView(JavaManageDialog.this.context);
                tv.setPadding(32, 36, 32, 36);
                tv.setTextSize(15f);
                tv.setTextColor(JavaManageDialog.this.context.getResources()
                        .getColor(R.color.colorPureBlack));
            }
            final QclJavaManager.JavaEntry e = JavaManageDialog.this.data.get(position);
            tv.setText(e.display() + "\n" + e.path);
            tv.setOnLongClickListener(v -> {
                new AlertDialog.Builder(JavaManageDialog.this.context)
                        .setTitle("删除这个 Java？")
                        .setMessage(e.display() + "\n\n删除后需要重新下载才能使用。")
                        .setPositiveButton("删除", (d, w) -> {
                            if (QclJavaManager.remove(e.name)) {
                                Toast.makeText(JavaManageDialog.this.context,
                                        "已删除 " + e.name, Toast.LENGTH_SHORT).show();
                                JavaManageDialog.this.reload();
                            } else {
                                Toast.makeText(JavaManageDialog.this.context,
                                        "删除失败（可能正在使用）", Toast.LENGTH_LONG).show();
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
                return true;
            });
            return tv;
        }
    }
}
