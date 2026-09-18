/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.annotation.SuppressLint
 *  android.app.Activity
 *  android.content.ClipData
 *  android.content.ClipboardManager
 *  android.content.Context
 *  android.graphics.Typeface
 *  android.graphics.drawable.Drawable
 *  android.graphics.drawable.GradientDrawable
 *  android.os.Handler
 *  android.os.Looper
 *  android.os.SystemClock
 *  android.view.GestureDetector
 *  android.view.GestureDetector$OnGestureListener
 *  android.view.GestureDetector$SimpleOnGestureListener
 *  android.view.MotionEvent
 *  android.view.View
 *  android.view.View$OnTouchListener
 *  android.view.ViewGroup
 *  android.view.ViewGroup$LayoutParams
 *  android.widget.FrameLayout
 *  android.widget.FrameLayout$LayoutParams
 *  android.widget.LinearLayout
 *  android.widget.LinearLayout$LayoutParams
 *  android.widget.ScrollView
 *  android.widget.TextView
 *  android.widget.Toast
 *  net.kdt.pojavlaunch.Logger
 */
package com.qcl.launcher.launcher.launch;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.qcl.launcher.utils.file.FileStringUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import net.kdt.pojavlaunch.Logger;

@SuppressLint(value={"SetTextI18n"})
public class LaunchLogWindow {
    private static final int MAX_CHARS = 24000;
    private final Activity activity;
    private final ViewGroup parent;
    private final StringBuilder buffer = new StringBuilder();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private View panel;
    private TextView logView;
    private ScrollView scroller;
    private TextView closeBtn;
    private TextView selectBtn;
    private FrameLayout.LayoutParams panelLp;
    private LinearLayout headerBar;
    private boolean attached;
    private static LaunchLogWindow current;
    private volatile boolean closed;
    private boolean selecting;
    private static boolean captureLogcat;
    private static final String[] LOGCAT_TAGS;
    private static final int MAX_LINE_CHARS = 400;
    private static final String[] FCL_SKIP_CONTAINS;
    private static final String[] NOISE_PREFIXES;
    private long shownAt;
    private long lastLogTime;
    private boolean gotAnyLine;
    private final Runnable silenceChecker = new Runnable(){

        @Override
        public void run() {
        }
    };
    private static GameLaunchSettingInfo sBasics;
    private String lastDedupLine;
    private long lastDedupAt;

    public LaunchLogWindow(Activity activity, ViewGroup parent) {
        this.activity = activity;
        this.parent = parent;
    }

    public static void showForBoat(Activity activity, ViewGroup parent) {
        LaunchLogWindow.showForBoat(activity, parent, null);
    }

    public static void showForBoat(Activity activity, ViewGroup parent, GameLaunchSettingInfo info) {
        captureLogcat = true;
        new LaunchLogWindow(activity, parent).show(info);
    }

    private void startLogcatCapture() {
    }

    private void startFileTail(Activity activity) {
        try {
            File dir = activity.getExternalFilesDir("debug");
            if (dir == null) {
                return;
            }
            File logFile = new File(dir, "pojav_latest_log.txt");
            Thread thread = new Thread(() -> {
                long offset = 0L;
                while (!this.closed) {
                    try {
                        if (logFile.isFile()) {
                            long len = logFile.length();
                            if (len < offset) {
                                offset = 0L;
                            }
                            if (len > offset) {
                                try (RandomAccessFile raf = new RandomAccessFile(logFile, "r");){
                                    String line;
                                    raf.seek(offset);
                                    while ((line = raf.readLine()) != null) {
                                        try {
                                            byte[] raw = line.getBytes(StandardCharsets.ISO_8859_1);
                                            line = new String(raw, StandardCharsets.UTF_8);
                                        }
                                        catch (Throwable throwable) {
                                            // empty catch block
                                        }
                                        offset += (long)(line.getBytes(StandardCharsets.UTF_8).length + 1);
                                        this.onLogLine(line);
                                    }
                                }
                            }
                        }
                        Thread.sleep(400L);
                    }
                    catch (Throwable t) {
                        try {
                            Thread.sleep(1000L);
                        }
                        catch (InterruptedException interruptedException) {}
                    }
                }
            }, "qcl-logtail");
            thread.setDaemon(true);
            thread.start();
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    public void show() {
        this.show(null);
    }

    public void show(GameLaunchSettingInfo info) {
        if (this.attached) {
            return;
        }
        this.lastLogTime = this.shownAt = SystemClock.uptimeMillis();
        current = this;
        Logger.getInstance((Context)this.activity).setLogListener(this::onLogLine);
        this.startFileTail(this.activity);
        this.startLogcatCapture();
        this.mainHandler.post(this::attach);
        this.mainHandler.post(() -> this.appendBasics(info));
        this.mainHandler.postDelayed(this.silenceChecker, 1000L);
    }

    public static void closeCurrentIfAny() {
        LaunchLogWindow w = current;
        if (w != null) {
            w.close();
        }
    }

    private void appendBasics(GameLaunchSettingInfo info) {
        boolean printDeviceSummary = false;
    }

    public static void setBasics(GameLaunchSettingInfo info) {
        sBasics = info;
    }

    public static void showFor(Activity activity, ViewGroup parent) {
        LaunchLogWindow.showFor(activity, parent, null);
    }

    public static void showFor(Activity activity, ViewGroup parent, GameLaunchSettingInfo info) {
        if (current != null && LaunchLogWindow.current.attached) {
            return;
        }
        captureLogcat = true;
        new LaunchLogWindow(activity, parent).show(info != null ? info : sBasics);
    }

    private void autoScrollIfAtBottom() {
        if (this.scroller == null) {
            return;
        }
        this.scroller.post(() -> {
            try {
                if (!this.scroller.canScrollVertically(1)) {
                    this.scroller.fullScroll(130);
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        });
    }

    private void toggleTextSelection() {
        try {
            if (this.logView == null) {
                return;
            }
            boolean bl = this.selecting = !this.selecting;
            if (this.selecting) {
                this.scroller.requestDisallowInterceptTouchEvent(true);
                this.logView.setFocusableInTouchMode(true);
                this.logView.requestFocus();
                this.logView.setTextIsSelectable(true);
                this.logView.performLongClick();
                if (this.selectBtn != null) {
                    this.selectBtn.setText((CharSequence)"\u5b8c\u6210");
                }
            } else {
                this.scroller.requestDisallowInterceptTouchEvent(false);
                ClipboardManager cm = (ClipboardManager)this.activity.getSystemService("clipboard");
                if (cm != null && this.logView.hasSelection()) {
                    cm.setPrimaryClip(ClipData.newPlainText((CharSequence)"QCL \u65e5\u5fd7", (CharSequence)this.logView.getText().subSequence(this.logView.getSelectionStart(), this.logView.getSelectionEnd())));
                    this.toast("\u5df2\u590d\u5236\u9009\u4e2d\u7684\u65e5\u5fd7");
                }
                this.logView.clearFocus();
                this.logView.setTextIsSelectable(false);
                this.logView.setFocusable(false);
                this.logView.setFocusableInTouchMode(false);
                if (this.selectBtn != null) {
                    this.selectBtn.setText((CharSequence)"\u9009\u62e9");
                }
            }
        }
        catch (Throwable t) {
            try {
                if (this.logView != null) {
                    this.logView.setTextIsSelectable(false);
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            this.toast("\u6b64\u8bbe\u5907\u4e0d\u652f\u6301\u6587\u672c\u9009\u62e9\uff0c\u53ef\u76f4\u63a5\u622a\u56fe\u53cd\u9988");
        }
    }

    private void toast(String msg) {
        try {
            Toast.makeText((Context)this.activity, (CharSequence)msg, (int)0).show();
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private void copyAllToClipboard() {
        try {
            if (this.logView == null) {
                return;
            }
            CharSequence text = this.logView.getText();
            if (text == null || text.length() == 0) {
                this.toast("\u65e5\u5fd7\u8fd8\u662f\u7a7a\u7684\uff0c\u7b49\u542f\u52a8\u8dd1\u4e00\u4f1a\u513f\u518d\u8bd5");
                return;
            }
            ClipboardManager cm = (ClipboardManager)this.activity.getSystemService("clipboard");
            if (cm == null) {
                this.toast("\u6b64\u8bbe\u5907\u4e0d\u652f\u6301\u526a\u8d34\u677f");
                return;
            }
            cm.setPrimaryClip(ClipData.newPlainText((CharSequence)"QCL \u65e5\u5fd7", (CharSequence)text));
            this.toast("\u5df2\u590d\u5236\u5168\u90e8\u65e5\u5fd7\uff08" + text.length() + " \u5b57\uff09");
        }
        catch (Throwable ignored) {
            this.toast("\u590d\u5236\u5931\u8d25\uff0c\u53ef\u622a\u56fe\u53cd\u9988");
        }
    }

    private boolean isTouchInside(View v, MotionEvent event) {
        try {
            int[] loc = new int[2];
            v.getLocationOnScreen(loc);
            float x = event.getRawX();
            float y = event.getRawY();
            return x >= (float)loc[0] && x <= (float)(loc[0] + v.getWidth()) && y >= (float)loc[1] && y <= (float)(loc[1] + v.getHeight());
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    public void close() {
        this.closed = true;
        current = null;
        this.mainHandler.removeCallbacks(this.silenceChecker);
        Logger.getInstance((Context)this.activity).setLogListener(null);
        this.mainHandler.post(() -> {
            if (this.panel != null && this.panel.getParent() instanceof ViewGroup) {
                ((ViewGroup)this.panel.getParent()).removeView(this.panel);
            }
            this.panel = null;
            this.attached = false;
        });
    }

    private void attach() {
        LinearLayout header;
        if (this.attached) {
            return;
        }
        int pad = this.dp(8);
        FrameLayout box = new FrameLayout((Context)this.activity);
        GradientDrawable background = new GradientDrawable();
        background.setColor(-16777216);
        box.setBackground((Drawable)background);
        box.setPadding(pad, pad, pad, pad);
        LinearLayout column = new LinearLayout((Context)this.activity);
        column.setOrientation(1);
        box.addView((View)column, (ViewGroup.LayoutParams)new FrameLayout.LayoutParams(-1, -1));
        this.headerBar = header = new LinearLayout((Context)this.activity);
        header.setOrientation(0);
        header.setGravity(16);
        TextView title = new TextView((Context)this.activity);
        title.setText((CharSequence)"\u2261 \u542f\u52a8\u65e5\u5fd7");
        title.setTextColor(-1644826);
        title.setTextSize(12.0f);
        title.setPadding(this.dp(2), this.dp(4), this.dp(2), this.dp(4));
        header.addView((View)title, (ViewGroup.LayoutParams)new LinearLayout.LayoutParams(0, -2, 1.0f));
        this.selectBtn = new TextView((Context)this.activity);
        this.selectBtn.setText((CharSequence)"\u9009\u62e9");
        this.selectBtn.setTextColor(-1);
        this.selectBtn.setTextSize(10.0f);
        this.selectBtn.setGravity(17);
        GradientDrawable selBg = new GradientDrawable();
        selBg.setCornerRadius((float)this.dp(11));
        selBg.setColor(0x33FFFFFF);
        selBg.setStroke(this.dp(1), 0x55FFFFFF);
        this.selectBtn.setBackground((Drawable)selBg);
        this.selectBtn.setClickable(true);
        this.selectBtn.setFocusable(true);
        this.selectBtn.setPadding(this.dp(8), 0, this.dp(8), 0);
        this.selectBtn.setOnClickListener(v -> this.toggleTextSelection());
        this.selectBtn.setOnLongClickListener(v -> {
            this.copyAllToClipboard();
            return true;
        });
        LinearLayout.LayoutParams selLp = new LinearLayout.LayoutParams(-2, this.dp(22));
        selLp.leftMargin = this.dp(4);
        header.addView((View)this.selectBtn, (ViewGroup.LayoutParams)selLp);
        this.closeBtn = new TextView((Context)this.activity);
        this.closeBtn.setText((CharSequence)"\u00d7");
        this.closeBtn.setTextColor(-1);
        this.closeBtn.setTextSize(14.0f);
        this.closeBtn.setGravity(17);
        GradientDrawable closeBg = new GradientDrawable();
        closeBg.setShape(1);
        closeBg.setColor(0x33FFFFFF);
        closeBg.setStroke(this.dp(1), 0x55FFFFFF);
        this.closeBtn.setBackground((Drawable)closeBg);
        this.closeBtn.setClickable(true);
        this.closeBtn.setFocusable(true);
        this.closeBtn.setOnClickListener(v -> this.close());
        int closeSize = this.dp(22);
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(closeSize, closeSize);
        closeLp.leftMargin = this.dp(4);
        header.addView((View)this.closeBtn, (ViewGroup.LayoutParams)closeLp);
        column.addView((View)header, (ViewGroup.LayoutParams)new LinearLayout.LayoutParams(-1, -2));
        this.scroller = new ScrollView((Context)this.activity);
        this.scroller.setFillViewport(true);
        this.scroller.setVerticalScrollBarEnabled(true);
        this.logView = new TextView((Context)this.activity);
        this.logView.setTextColor(-2236963);
        this.logView.setTextSize(10.0f);
        this.logView.setTypeface(Typeface.MONOSPACE);
        this.logView.setTextIsSelectable(false);
        this.logView.setFocusable(false);
        this.logView.setFocusableInTouchMode(false);
        this.logView.setClickable(false);
        this.logView.setLongClickable(false);
        this.logView.setHorizontallyScrolling(false);
        this.logView.setMaxLines(Integer.MAX_VALUE);
        this.scroller.addView((View)this.logView, (ViewGroup.LayoutParams)new FrameLayout.LayoutParams(-1, -2));
        column.addView((View)this.scroller, (ViewGroup.LayoutParams)new LinearLayout.LayoutParams(-1, 0, 1.0f));
        GestureDetector dragDetector = new GestureDetector((Context)this.activity, (GestureDetector.OnGestureListener)new GestureDetector.SimpleOnGestureListener(){

            public boolean onDown(MotionEvent e) {
                return true;
            }

            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (LaunchLogWindow.this.selecting) {
                    return false;
                }
                if (LaunchLogWindow.this.scroller != null) {
                    LaunchLogWindow.this.scroller.scrollBy(0, (int)distanceY);
                }
                return true;
            }
        });
        this.scroller.setOnTouchListener((v, event) -> {
            if (this.selecting) {
                return false;
            }
            dragDetector.onTouchEvent(event);
            if (event.getActionMasked() == 1 || event.getActionMasked() == 3) {
                v.performClick();
            }
            return true;
        });
        this.panelLp = new FrameLayout.LayoutParams(this.dp(250), this.dp(160), 8388691);
        this.panelLp.leftMargin = this.dp(8);
        this.panelLp.bottomMargin = this.dp(8);
        box.setLayoutParams((ViewGroup.LayoutParams)this.panelLp);
        header.setOnTouchListener(new View.OnTouchListener(){
            private float downRawX;
            private float downRawY;
            private int startLeft;
            private int startTop;

            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case 0: {
                        if (LaunchLogWindow.this.closeBtn != null && LaunchLogWindow.this.isTouchInside((View)LaunchLogWindow.this.closeBtn, event)) {
                            return false;
                        }
                        if (LaunchLogWindow.this.selectBtn != null && LaunchLogWindow.this.isTouchInside((View)LaunchLogWindow.this.selectBtn, event)) {
                            return false;
                        }
                        this.downRawX = event.getRawX();
                        this.downRawY = event.getRawY();
                        this.startLeft = ((LaunchLogWindow)LaunchLogWindow.this).panelLp.leftMargin;
                        this.startTop = ((LaunchLogWindow)LaunchLogWindow.this).panelLp.topMargin;
                        return true;
                    }
                    case 2: {
                        int dx = (int)(event.getRawX() - this.downRawX);
                        int dy = (int)(event.getRawY() - this.downRawY);
                        int newLeft = this.startLeft + dx;
                        int newTop = this.startTop + dy;
                        int maxLeft = Math.max(0, LaunchLogWindow.this.parent.getWidth() - LaunchLogWindow.this.panel.getWidth());
                        int maxTop = Math.max(0, LaunchLogWindow.this.parent.getHeight() - LaunchLogWindow.this.panel.getHeight());
                        newLeft = Math.max(0, Math.min(maxLeft, newLeft));
                        newTop = Math.max(0, Math.min(maxTop, newTop));
                        ((LaunchLogWindow)LaunchLogWindow.this).panelLp.gravity = 0x800033;
                        ((LaunchLogWindow)LaunchLogWindow.this).panelLp.leftMargin = newLeft;
                        ((LaunchLogWindow)LaunchLogWindow.this).panelLp.topMargin = newTop;
                        LaunchLogWindow.this.panel.setLayoutParams((ViewGroup.LayoutParams)LaunchLogWindow.this.panelLp);
                        return true;
                    }
                    case 1: 
                    case 3: {
                        return true;
                    }
                }
                return false;
            }
        });
        this.parent.addView((View)box);
        this.panel = box;
        this.attached = true;
        try {
            TextView resizer = new TextView((Context)this.activity);
            resizer.setText((CharSequence)"\u25e2");
            resizer.setTextColor(-1996488705);
            resizer.setTextSize(11.0f);
            resizer.setGravity(0x800055);
            int rSize = this.dp(20);
            FrameLayout.LayoutParams rLp = new FrameLayout.LayoutParams(rSize, rSize);
            rLp.gravity = 0x800055;
            box.addView((View)resizer, (ViewGroup.LayoutParams)rLp);
            resizer.setOnTouchListener(new View.OnTouchListener(){
                private float downRawX;
                private float downRawY;
                private int startW;
                private int startH;

                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getActionMasked()) {
                        case 0: {
                            this.downRawX = event.getRawX();
                            this.downRawY = event.getRawY();
                            this.startW = ((LaunchLogWindow)LaunchLogWindow.this).panelLp.width;
                            this.startH = ((LaunchLogWindow)LaunchLogWindow.this).panelLp.height;
                            return true;
                        }
                        case 2: {
                            int w = this.startW + (int)(event.getRawX() - this.downRawX);
                            int h = this.startH + (int)(event.getRawY() - this.downRawY);
                            ((LaunchLogWindow)LaunchLogWindow.this).panelLp.width = Math.max(LaunchLogWindow.this.dp(180), Math.min(LaunchLogWindow.this.dp(640), w));
                            ((LaunchLogWindow)LaunchLogWindow.this).panelLp.height = Math.max(LaunchLogWindow.this.dp(110), Math.min(LaunchLogWindow.this.dp(520), h));
                            LaunchLogWindow.this.panel.setLayoutParams((ViewGroup.LayoutParams)LaunchLogWindow.this.panelLp);
                            return true;
                        }
                        case 1: 
                        case 3: {
                            return true;
                        }
                    }
                    return false;
                }
            });
        }
        catch (Throwable resizer) {
            // empty catch block
        }
        if (this.logView != null && this.buffer.length() > 0) {
            this.logView.setText((CharSequence)LaunchLogWindow.trim(this.buffer));
        }
        try {
            String existing;
            File file = new File(this.activity.getExternalFilesDir("debug"), "pojav_latest_log.txt");
            if (file.isFile() && (existing = FileStringUtils.getStringFromFile(file.getAbsolutePath())) != null && !existing.isEmpty()) {
                this.buffer.append(existing);
                if (this.logView.hasSelection()) {
                    return;
                }
                this.logView.setText((CharSequence)LaunchLogWindow.trim(this.buffer));
                this.autoScrollIfAtBottom();
                this.scrollToEnd();
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private boolean isDuplicate(String line) {
        long now = SystemClock.uptimeMillis();
        if (line.equals(this.lastDedupLine) && now - this.lastDedupAt < 2500L) {
            this.lastDedupAt = now;
            return true;
        }
        this.lastDedupLine = line;
        this.lastDedupAt = now;
        return false;
    }

    private void onLogLine(String text) {
        this.lastLogTime = SystemClock.uptimeMillis();
        this.gotAnyLine = true;
        String filtered = LaunchLogWindow.filterLine(text);
        if (filtered == null) {
            return;
        }
        String out = filtered;
        if (this.isDuplicate(out)) {
            return;
        }
        String outFinal = out;
        this.mainHandler.post(() -> {
            if (!this.attached) {
                return;
            }
            this.buffer.append(outFinal);
            if (!outFinal.endsWith("\n")) {
                this.buffer.append('\n');
            }
            if (this.attached && this.logView != null) {
                if (this.logView.hasSelection()) {
                    return;
                }
                this.logView.setText((CharSequence)LaunchLogWindow.trim(this.buffer));
                this.autoScrollIfAtBottom();
                this.scrollToEnd();
            }
        });
    }

    private static String filterLine(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim();
        if (t.isEmpty()) {
            return null;
        }
        for (String skip : FCL_SKIP_CONTAINS) {
            if (!t.contains(skip)) continue;
            return null;
        }
        for (String noise : NOISE_PREFIXES) {
            if (!t.startsWith(noise)) continue;
            return null;
        }
        if (t.length() > 400) {
            t = t.substring(0, 400) + " \u2026[\u5df2\u622a\u65ad " + (text.length() - 400) + " \u5b57\u7b26]";
        }
        return t;
    }

    private void scrollToEnd() {
        if (this.scroller != null) {
            this.scroller.post(() -> this.scroller.fullScroll(130));
        }
    }

    private static String trim(StringBuilder sb) {
        return sb.length() > 24000 ? sb.substring(sb.length() - 24000) : sb.toString();
    }

    private int dp(int value) {
        return Math.round((float)value * this.activity.getResources().getDisplayMetrics().density);
    }

    private /* synthetic */ void lambda$startLogcatCapture$0(BufferedReader reader) {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                this.onLogLine(line);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    static {
        captureLogcat = false;
        LOGCAT_TAGS = new String[]{"jrelog", "LIBGL", "Boat", "xhook", "System.out", "System.err", "OpenJDK"};
        FCL_SKIP_CONTAINS = new String[]{"version string:", "OR:", "ERROR:", "INTERNAL ERROR:"};
        NOISE_PREFIXES = new String[]{"--------- beginning of", "GLib-GIO", "libc    ", "Zygote  "};
    }

    public static class GameLaunchSettingInfo {
        public String backend = "?";
        public String version = "?";
        public String javaRuntime = "?";
        public String renderer = "?";
        public int ramMb = 0;
    }
}

