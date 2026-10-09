package com.qcl.launcher.launcher.uis.main;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * ★ 2026-10-09 用户要求：做 QCL 自己的「新手教程」——**按现在左侧栏的布局**来，参考 FCL 的
 * {@code com/mio/util/GuideUtil.kt}。
 *
 * <p>FCL 用的是第三方库 {@code com.getkeepsafe.taptargetview}（QCL 没引、且它要联网拉包）。
 * 这里**不引新依赖，用纯 View 复刻同款观感**：
 * 暗底遮罩 + 目标处**圆形镂空** + 外圈主题色高亮环 + 标题/说明气泡 + 点一下进入下一步。
 *
 * <p>「已看过」的步骤记在**外部私有目录**的 {@code guide_tag.txt}（方便测试：直接删掉文件即可重看）。
 * 与 FCL 一样，用 tag 去重：只看过的步骤不再出现。
 */
public final class GuideUtil {

    private static final String TAG_FILE = "guide_tag.txt";
    private static final Set<String> SEEN = new LinkedHashSet<>();
    private static boolean loaded;
    private static boolean showing;

    private GuideUtil() {
    }

    /** 一个引导步骤。 */
    public static final class Step {
        public final String tag;
        public final View view;
        public final String title;
        public final String desc;

        public Step(String tag, View view, String title, String desc) {
            this.tag = tag;
            this.view = view;
            this.title = title;
            this.desc = desc;
        }
    }

    /** 供 MainUI 调用：自动取主题色 + 依次展示未看过的步骤。 */
    public static void showOnMain(Activity activity, Step... steps) {
        if (activity == null || steps == null || steps.length == 0) {
            return;
        }
        int accent = 0xFF556980;
        try {
            com.qcl.launcher.launcher.MainActivity a =
                    (com.qcl.launcher.launcher.MainActivity) activity;
            accent = Color.parseColor(
                    com.qcl.launcher.launcher.uis.universal.setting.right.launcher.ExteriorSettingUI
                            .getThemeColor(activity, a.launcherSetting.launcherTheme));
        } catch (Throwable ignored) {
        }
        show(activity, accent, Arrays.asList(steps));
    }

    public static void show(Activity activity, int accentColor, List<Step> steps) {
        if (activity == null || steps == null || steps.isEmpty() || showing) {
            return;
        }
        Set<String> seen = loadSeen(activity);
        List<Step> todo = new ArrayList<>();
        for (Step s : steps) {
            if (s == null || s.view == null || s.tag == null) {
                continue;
            }
            if (seen.contains(s.tag)) {
                continue;
            }
            if (s.view.getVisibility() != View.VISIBLE) {
                continue;
            }
            if (s.view.getWidth() <= 0 || s.view.getHeight() <= 0) {
                continue;   // 还没布局完 → 跳过（下次进来再引导）
            }
            todo.add(s);
        }
        if (todo.isEmpty()) {
            return;
        }
        ViewGroup root = activity.findViewById(android.R.id.content);
        if (root == null) {
            return;
        }
        showing = true;
        Overlay overlay = new Overlay(activity, accentColor, todo);
        root.addView(overlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    // ------------------------------------------------------------------ 遮罩 + 气泡

    private static final class Overlay extends FrameLayout {
        private final Activity activity;
        private final int accent;
        private final List<Step> steps;
        private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path holePath = new Path();
        private final RectF targetRect = new RectF();
        private final TextView bubble;
        private final TextView titleView;
        private int index;

        Overlay(Activity activity, int accent, List<Step> steps) {
            super(activity);
            this.activity = activity;
            this.accent = accent;
            this.steps = steps;
            setWillNotDraw(false);
            setClickable(true);

            dimPaint.setColor(0xD9000000);
            dimPaint.setStyle(Paint.Style.FILL);
            ringPaint.setColor(accent);
            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setStrokeWidth(dp(2.5f));
            glowPaint.setColor((accent & 0x00FFFFFF) | 0x33000000);
            glowPaint.setStyle(Paint.Style.STROKE);
            glowPaint.setStrokeWidth(dp(10f));

            // 气泡：标题 + 说明
            android.widget.LinearLayout box = new android.widget.LinearLayout(activity);
            box.setOrientation(android.widget.LinearLayout.VERTICAL);
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setCornerRadius(dp(10f));
            bg.setColor(accent);
            box.setBackground(bg);
            int p = (int) dp(12f);
            box.setPadding(p, p, p, p);

            titleView = new TextView(activity);
            titleView.setTextColor(isDark(accent) ? 0xFFFFFFFF : 0xFF000000);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
            titleView.setTypeface(Typeface.DEFAULT_BOLD);
            box.addView(titleView);

            bubble = new TextView(activity);
            bubble.setTextColor(isDark(accent) ? 0xE6FFFFFF : 0xCC000000);
            bubble.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
            android.widget.LinearLayout.LayoutParams blp =
                    new android.widget.LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            blp.topMargin = (int) dp(4f);
            box.addView(bubble, blp);

            TextView hint = new TextView(activity);
            hint.setText("点一下继续");
            hint.setTextColor(isDark(accent) ? 0x99FFFFFF : 0x99000000);
            hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
            hint.setGravity(Gravity.END);
            android.widget.LinearLayout.LayoutParams hlp =
                    new android.widget.LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            hlp.topMargin = (int) dp(6f);
            box.addView(hint, hlp);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            box.setLayoutParams(lp);
            addView(box);
            bubble.setTag(box);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (targetRect.isEmpty()) {
                return;
            }
            float cx = targetRect.centerX();
            float cy = targetRect.centerY();
            float r = Math.max(targetRect.width(), targetRect.height()) / 2f + dp(8f);

            // 暗底（带圆形镂空）
            holePath.reset();
            holePath.setFillType(Path.FillType.EVEN_ODD);
            holePath.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);
            holePath.addCircle(cx, cy, r, Path.Direction.CCW);
            canvas.drawPath(holePath, dimPaint);

            // 外圈高亮环 + 光晕
            canvas.drawCircle(cx, cy, r + dp(4f), glowPaint);
            canvas.drawCircle(cx, cy, r, ringPaint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                index++;
                if (index >= steps.size()) {
                    finish();
                } else {
                    showStep();
                }
                return true;
            }
            return true;
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            post(this::showStep);
        }

        private void showStep() {
            try {
                Step s = steps.get(index);
                int[] loc = new int[2];
                s.view.getLocationOnScreen(loc);
                targetRect.set(loc[0], loc[1], loc[0] + s.view.getWidth(), loc[1] + s.view.getHeight());
                titleView.setText(s.title == null ? "" : s.title);
                bubble.setText(s.desc == null ? "" : s.desc);
                layoutBubble((View) bubble.getTag());
                invalidate();
            } catch (Throwable ignored) {
                finish();
            }
        }

        private void layoutBubble(View box) {
            if (box == null) {
                return;
            }
            int w = getWidth();
            int h = getHeight();
            box.measure(View.MeasureSpec.makeMeasureSpec((int) (w * 0.72f), View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.AT_MOST));
            int bw = box.getMeasuredWidth();
            int bh = box.getMeasuredHeight();

            float cx = targetRect.centerX();
            float top = targetRect.top;
            float bottom = targetRect.bottom;

            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) box.getLayoutParams();
            lp.leftMargin = (int) Math.max(dp(12f), Math.min(w - bw - dp(12f), cx - bw / 2f));
            // 优先放目标下方（左栏按钮在左边，气泡放右边更合适）
            float ty;
            if (cx < w / 2f) {
                // 目标是左侧栏 → 气泡放它右边
                lp.leftMargin = (int) Math.min(w - bw - dp(12f), targetRect.right + dp(16f));
                ty = Math.max(dp(12f), Math.min(h - bh - dp(12f), targetRect.centerY() - bh / 2f));
            } else {
                ty = (bottom + bh + dp(20f) < h) ? (bottom + dp(14f)) : Math.max(dp(12f), top - bh - dp(14f));
            }
            lp.topMargin = (int) ty;
            lp.gravity = Gravity.TOP | Gravity.START;
            box.setLayoutParams(lp);
        }

        private void finish() {
            try {
                Set<String> seen = loadSeen(activity);
                for (Step s : steps) {
                    seen.add(s.tag);
                }
                saveSeen(activity, seen);
            } catch (Throwable ignored) {
            }
            showing = false;
            ViewGroup parent = (ViewGroup) getParent();
            if (parent != null) {
                parent.removeView(this);
            }
        }

        private float dp(float v) {
            return v * getResources().getDisplayMetrics().density;
        }
    }

    // ------------------------------------------------------------------ tag 持久化

    private static File tagFile(Context context) {
        File dir = null;
        try {
            dir = context.getExternalFilesDir("debug");
        } catch (Throwable ignored) {
        }
        if (dir == null) {
            dir = context.getFilesDir();
        }
        return new File(dir, TAG_FILE);
    }

    private static synchronized Set<String> loadSeen(Context context) {
        if (!loaded) {
            loaded = true;
            try {
                File f = tagFile(context);
                if (f.isFile()) {
                    for (String line : new String(readAll(f), "UTF-8").split("\n")) {
                        String t = line.trim();
                        if (!t.isEmpty()) {
                            SEEN.add(t);
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return new LinkedHashSet<>(SEEN);
    }

    private static synchronized void saveSeen(Context context, Set<String> seen) {
        SEEN.clear();
        SEEN.addAll(seen);
        loaded = true;
        try {
            StringBuilder sb = new StringBuilder();
            for (String t : SEEN) {
                sb.append(t).append('\n');
            }
            File f = tagFile(context);
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(f)) {
                out.write(sb.toString().getBytes("UTF-8"));
                out.getFD().sync();
            }
        } catch (Throwable ignored) {
        }
    }

    private static byte[] readAll(File f) throws java.io.IOException {
        try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            return bos.toByteArray();
        }
    }

    private static boolean isDark(int color) {
        double r = Color.red(color) / 255.0;
        double g = Color.green(color) / 255.0;
        double b = Color.blue(color) / 255.0;
        return (0.2126 * r + 0.7152 * g + 0.0722 * b) < 0.55;
    }
}
