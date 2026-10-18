package com.qcl.launcher.launcher.uis.universal.setting.right.launcher;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.qcl.launcher.utils.QclColors;

/**
 * ★ 1.5.0：**主题色下拉框**的适配器（用户要求「最上面是颜色的下拉框」）。
 *
 * <p>每项 = 「一个色块 + 颜色名/十六进制」，默认项叫「默认（斑驳森林）」。
 * 下拉展开时是同一套 item 视图，所以列表里也能直观看到颜色。
 *
 * <p>不用自定义布局文件：item 视图在代码里拼（一个圆角色块 + 一个 TextView），
 * 好处是**不受下拉框高度限制**，也不新增布局资源需要维护。
 */
public class ThemeColorSpinnerAdapter extends ArrayAdapter<String> {

    /** 预设配色：(名字, 颜色)。全部是在深浅底上都能看清的稳色。 */
    public static final String[] NAMES = {
            "默认（斑驳森林）",
            "靛蓝",
            "青绿",
            "绛红",
            "橙",
            "紫",
            "石板灰",
    };

    public static final int[] COLORS = {
            // ★★★★★ 1.5.2 修正（用户要求）：
            //   「末日斑驳森林的颜色，主题色白天能不能取斑驳森林真实的颜色，
            //     别取那个水的颜色了」。
            //   原来这里是 0xFF556980 —— 那是**斑驳森林壁纸里水面的蓝灰反光色**，
            //   跟整张图的暖棕主调完全不搭。
            //   ⇒ 现在改成从壁纸 ic_background.jpg（斑驳森林，2560x1600）**实测取样**：
            //        整体平均色 #7D5753 / 中间调平均 #865D58
            //      → 取 #865D58（略亮一档，保证按钮/勾选框在深浅底上都看得见）。
            //   对照：经典背景 ic_background_classic.jpg 的中间调是 #1E3441（深蓝），
            //        所以"水的蓝灰"其实属于经典那张图，不属于斑驳森林。
            0xFF865D58,  // 默认：斑驳森林真实色（壁纸中间调取样）
            0xFF283593,  // 靛蓝
            0xFF0E9384,  // 青绿
            0xFFB71C1C,  // 绛红
            0xFFE67E22,  // 橙
            0xFF9C27B0,  // 紫
            0xFF37474F,  // 石板灰
    };

    private final Context context;

    public ThemeColorSpinnerAdapter(Context context) {
        super(context, android.R.layout.simple_spinner_item, NAMES);
        this.context = context;
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    /** 第 position 项的颜色值。越界时回退到默认色。 */
    public int colorAt(int position) {
        if (position < 0 || position >= COLORS.length) {
            return COLORS[0];
        }
        return COLORS[position];
    }

    /** 当前已选颜色对应的预设下标；不是预设色（玩家自定义）→ 返回 -1。 */
    public int indexOfColor(String colorString) {
        if (colorString == null) {
            return -1;
        }
        try {
            int c = Color.parseColor(colorString);
            for (int i = 0; i < COLORS.length; i++) {
                if (COLORS[i] == c) {
                    return i;
                }
            }
        } catch (Throwable ignored) {
        }
        return -1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return buildItem(position, convertView, false);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return buildItem(position, convertView, true);
    }

    /** 拼一项：圆角色块 + 文字。 */
    private View buildItem(int position, View convertView, boolean dropdown) {
        LinearLayout row;
        if (convertView instanceof LinearLayout) {
            row = (LinearLayout) convertView;
        } else {
            row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            int pad = dp(dropdown ? 12 : 6);
            row.setPadding(pad, pad, pad, pad);

            View swatch = new View(context);
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(18), dp(18));
            sp.setMarginEnd(dp(8));
            swatch.setLayoutParams(sp);
            swatch.setTag("swatch");
            row.addView(swatch);

            TextView tv = new TextView(context);
            tv.setTextSize(14f);
            tv.setSingleLine(true);
            // ★★★★★ 2026-10-16 修「外观页主题下拉框文字看不清 / 像叠字」（评测视频 27.0s）。
            //
            //   原来是**硬编码** `0xFFE8E8E8`（浅灰），注释写「下拉在深色底上，用浅色文字保可读」。
            //   但 Android Spinner 用的是**系统默认弹窗背景**（跟随主题）：
            //     · 夜间模式 → 深色弹窗 ⇒ 浅灰字勉强可读；
            //     · 浅色模式 → **白色弹窗** ⇒ 浅灰字几乎看不见，与相邻内容糊在一起像"叠字"。
            //   ⇒ 改用项目统一的正文色资源 `colorPureBlack`：
            //     `values/` = #000000（浅底黑字）、`values-night/` = #FFFFFF（深底白字），
            //     **自动昼夜翻转**，与「底/字成对翻转」的项目铁律一致。
            tv.setTextColor(context.getResources().getColor(com.qcl.launcher.R.color.colorPureBlack));
            tv.setTag("label");
            row.addView(tv);
        }

        View swatch = row.findViewWithTag("swatch");
        TextView label = (TextView) row.findViewWithTag("label");

        int color = colorAt(position);
        Drawable d = swatch.getBackground();
        if (!(d instanceof GradientDrawable)) {
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.RECTANGLE);
            gd.setCornerRadius(dp(5));
            gd.setStroke(dp(1), 0x66FFFFFF);
            swatch.setBackground(gd);
            d = gd;
        }
        if (d instanceof GradientDrawable) {
            ((GradientDrawable) d).setColor(color);
        } else {
            swatch.setBackground(new ColorDrawable(color));
        }

        String name = position >= 0 && position < NAMES.length ? NAMES[position] : "";
        label.setText(name + "  " + QclColors.format(color));
        return row;
    }

    private int dp(int v) {
        float den = context.getResources().getDisplayMetrics().density;
        return Math.round(v * den);
    }
}
