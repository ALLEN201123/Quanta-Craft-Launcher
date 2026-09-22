package com.qcl.launcher.launcher.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.qcl.launcher.R;

/**
 * ★ 1.2.3：版本图标选择器 —— 内置一批图标 + 自定义图片，两种方式并存
 * （交互参考 mitimc：点铅笔 → 网格选内置，底部按钮走自定义选图）。
 */
public class IconPickerDialog extends Dialog {

    public interface Listener {
        void onBuiltinPicked(int drawableRes);
        void onCustomRequested();
    }

    private static final int[] ICONS = {
            // MultiMC 实例图标（用户指定的风格）
            R.drawable.ic_mm_grass, R.drawable.ic_mm_dirt, R.drawable.ic_mm_stone,
            R.drawable.ic_mm_planks, R.drawable.ic_mm_tnt, R.drawable.ic_mm_gold,
            R.drawable.ic_mm_iron, R.drawable.ic_mm_brick, R.drawable.ic_mm_enderpearl,
            // 加载器 logo
            R.drawable.ic_forge, R.drawable.ic_neoforge, R.drawable.ic_fabric,
            R.drawable.ic_quilt, R.drawable.ic_modloader, R.drawable.ic_babric,
            // 其它
            R.drawable.ic_optifine, R.drawable.ic_chicken, R.drawable.ic_cleanroom
    };

    private final Listener listener;

    public IconPickerDialog(Context context, Listener listener) {
        super(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        float d = getContext().getResources().getDisplayMetrics().density;
        int p = (int) (16 * d);

        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(p, p, p, p);

        TextView title = new TextView(getContext());
        title.setText("选择版本图标");
        title.setTextSize(16);
        title.setPadding(0, 0, 0, (int) (8 * d));
        root.addView(title);

        TextView hint = new TextView(getContext());
        hint.setText("内置图标（点一下直接用）");
        hint.setTextSize(13);
        hint.setPadding(0, 0, 0, (int) (8 * d));
        root.addView(hint);

        GridView grid = new GridView(getContext());
        grid.setNumColumns(4);
        grid.setVerticalSpacing((int) (10 * d));
        grid.setHorizontalSpacing((int) (10 * d));
        grid.setAdapter(new BaseAdapter() {
            @Override public int getCount() { return ICONS.length; }
            @Override public Object getItem(int position) { return ICONS[position]; }
            @Override public long getItemId(int position) { return position; }
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                ImageView iv = (convertView instanceof ImageView)
                        ? (ImageView) convertView : new ImageView(getContext());
                int s = (int) (48 * d);
                iv.setLayoutParams(new ViewGroup.LayoutParams(s, s));
                iv.setPadding((int) (4 * d), (int) (4 * d), (int) (4 * d), (int) (4 * d));
                iv.setImageResource(ICONS[position]);
                return iv;
            }
        });
        grid.setOnItemClickListener((parent, view, position, id) -> {
            listener.onBuiltinPicked(ICONS[position]);
            dismiss();
        });
        root.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (250 * d)));

        Button custom = new Button(getContext());
        custom.setText("从文件选择自定义图片…");
        custom.setAllCaps(false);
        custom.setOnClickListener(v -> {
            listener.onCustomRequested();
            dismiss();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) (10 * d);
        root.addView(custom, lp);

        setContentView(root);
        Window w = getWindow();
        if (w != null) {
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
