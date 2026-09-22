package com.qcl.launcher.launcher.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.qcl.launcher.R;

/**
 * ★ 1.2.4：版本图标选择器。
 * 内置 = MultiMC 全套 24 个实例图标 + 6 个加载器 logo + OptiFine/Cleanroom；
 * 底部「返回」和「自定义图片」两个按钮，自定义走老的文件选择流程。
 */
public class IconPickerDialog extends Dialog {

    public interface Listener {
        void onBuiltinPicked(int drawableRes);
        void onCustomRequested();
    }

    private static final int[] ICONS = {
            R.drawable.ic_mm_brick, R.drawable.ic_mm_chicken, R.drawable.ic_mm_creeper, R.drawable.ic_mm_diamond, R.drawable.ic_mm_dirt, R.drawable.ic_mm_enderpearl, R.drawable.ic_mm_flame, R.drawable.ic_mm_ftb_glow, R.drawable.ic_mm_ftb_logo, R.drawable.ic_mm_gear, R.drawable.ic_mm_gold, R.drawable.ic_mm_grass, R.drawable.ic_mm_herobrine, R.drawable.ic_mm_infinity, R.drawable.ic_mm_iron, R.drawable.ic_mm_magitech, R.drawable.ic_mm_meat, R.drawable.ic_mm_netherstar, R.drawable.ic_mm_planks, R.drawable.ic_mm_skeleton, R.drawable.ic_mm_squarecreeper, R.drawable.ic_mm_steve, R.drawable.ic_mm_stone, R.drawable.ic_mm_tnt,
            // 加载器 logo
            R.drawable.ic_forge, R.drawable.ic_neoforge, R.drawable.ic_fabric,
            R.drawable.ic_quilt, R.drawable.ic_modloader, R.drawable.ic_babric,
            // 其它
            R.drawable.ic_optifine, R.drawable.ic_cleanroom
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
        hint.setText("MultiMC 实例图标 + 加载器 logo（点一下直接用）");
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
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (300 * d)));

        // ★ 底部：返回 + 自定义图片（用户点名要的两个按钮）
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button back = new Button(getContext());
        back.setText("返回");
        back.setAllCaps(false);
        back.setOnClickListener(v -> dismiss());
        Button custom = new Button(getContext());
        custom.setText("从文件选择自定义图片…");
        custom.setAllCaps(false);
        custom.setOnClickListener(v -> {
            listener.onCustomRequested();
            dismiss();
        });
        LinearLayout.LayoutParams b1 = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        LinearLayout.LayoutParams b2 = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.6f);
        b1.setMargins(0, (int) (10 * d), (int) (6 * d), 0);
        b2.setMargins((int) (6 * d), (int) (10 * d), 0, 0);
        row.addView(back, b1);
        row.addView(custom, b2);
        root.addView(row);

        setContentView(root);
        Window w = getWindow();
        if (w != null) {
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    (int) (d * 460));
        }
    }
}
