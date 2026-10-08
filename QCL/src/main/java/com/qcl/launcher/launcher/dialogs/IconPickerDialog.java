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
            // ★★★ 1.5.0：**QCL 自己的等距立方体方块图标**（用户要求"把剩余的箱子、立方体之类的图标全加进去"）。
            //   这些是 150x150 的 3D 立方体渲染图，风格与启动器其余地方一致，原来漏了没进选择器。
            //   ★ ic_cobble = 原石立方体 —— **远古版本 / 归档版本统一用它**（用户指定）。
            R.drawable.ic_grass, R.drawable.ic_cobble, R.drawable.ic_chest,
            R.drawable.ic_furnace, R.drawable.ic_command_block, R.drawable.ic_bookshelf,
            // MultiMC 全套 24 个实例图标
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
        // ★★★ 1.5.0：底板跟随**昼夜主题**（用户实测：「自定义选择图标里面的背景为什么是
        //   白色的？没有根据时间变化吗？」）。
        //   根因：这个对话框是**纯代码构建**的，root 从来没设过背景 ⇒ 露出 Dialog 的默认白底，
        //   夜间模式下就是一大片白、刺眼。
        //   ⇒ 用与版本列表同款的**半透明昼夜底** @drawable/qcl_dialog_gray
        //     （日间 #9EEDEDED 近白 / 夜间 #9E171C22 深色），壁纸能透上来。
        root.setBackgroundResource(R.drawable.qcl_dialog_gray);

        TextView title = new TextView(getContext());
        title.setText("选择版本图标");
        title.setTextSize(16);
        title.setPadding(0, 0, 0, (int) (8 * d));
        // ★ 文字色同样昼夜成对翻转，否则夜间是"深底黑字"看不见
        title.setTextColor(getContext().getResources().getColor(R.color.qcl_nav_text_primary));
        root.addView(title);

        TextView hint = new TextView(getContext());
        hint.setText("立方体方块 + MultiMC 实例图标 + 加载器 logo（点一下直接用）");
        hint.setTextSize(13);
        hint.setPadding(0, 0, 0, (int) (8 * d));
        hint.setTextColor(getContext().getResources().getColor(R.color.qcl_nav_text_secondary));
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
        // ★ 占满剩余空间（weight），列表自己滚动，底部按钮永远可见
        root.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

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
            // ★★★ 1.5.0：把 Dialog **窗口本身**的背景设为透明 ——
            //   只给 root 设底板还不够，窗口默认的白色方角会从圆角底板四周露出来
            //   （用户看到的"背景是白色的"就是这层）。
            w.setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            android.util.DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    (int) (dm.heightPixels * 0.85));
        }
    }
}
