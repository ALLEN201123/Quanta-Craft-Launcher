package com.qcl.launcher.launcher.dialogs.lab;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.qcl.launcher.R;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 多彩文字生成器：为文字添加 Minecraft 颜色与格式代码，实时预览并可复制 § / & 版本。
 */
public class LabColorTextDialog extends Dialog {

    /** 16 种颜色代码 0-f。 */
    private static final String[] COLOR_CODES = {
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"
    };
    /** 对应的近似 RGB 颜色值，用于预览渲染。 */
    private static final int[] COLOR_ARGB = {
            Color.rgb(0x00, 0x00, 0x00), Color.rgb(0x00, 0x00, 0xAA), Color.rgb(0x00, 0xAA, 0x00),
            Color.rgb(0x00, 0xAA, 0xAA), Color.rgb(0xAA, 0x00, 0x00), Color.rgb(0xAA, 0x00, 0xAA),
            Color.rgb(0xFF, 0xAA, 0x00), Color.rgb(0xAA, 0xAA, 0xAA), Color.rgb(0x55, 0x55, 0x55),
            Color.rgb(0x55, 0x55, 0xFF), Color.rgb(0x55, 0xFF, 0x55), Color.rgb(0x55, 0xFF, 0xFF),
            Color.rgb(0xFF, 0x55, 0x55), Color.rgb(0xFF, 0x55, 0xFF), Color.rgb(0xFF, 0xFF, 0x55),
            Color.rgb(0xFF, 0xFF, 0xFF)
    };
    private static final String[] FORMAT_CODES = {"l", "o", "n", "m", "k"};
    private static final int[] FORMAT_LABELS = {
            R.string.lab_color_fmt_l, R.string.lab_color_fmt_o, R.string.lab_color_fmt_n,
            R.string.lab_color_fmt_m, R.string.lab_color_fmt_k
    };
    /** § 符号，使用转义避免源码编码问题。 */
    private static final String SECTION = "\u00A7";

    private EditText input;
    private TextView preview;
    private TextView raw;
    private LinearLayout palette;
    private LinearLayout formatRow;
    private LinearLayout modeRow;
    private Button modeWhole;
    private Button modeChar;

    /** 每个字符对应的颜色索引。 */
    private int[] charColors = new int[0];
    /** 当前画笔颜色（逐字模式下点击字符时使用）。 */
    private int brushColor = 15;
    /** 当前启用的格式代码集合。 */
    private final Set<String> formats = new LinkedHashSet<>();
    private boolean charMode = false;
    private boolean refreshing = false;

    public LabColorTextDialog(Context context) {
        super(context);
        setContentView(R.layout.dialog_lab_color_text);
        init();
        LabUtils.setupDialogWindow(this);
    }

    private void init() {
        this.input = findViewById(R.id.lab_color_input);
        this.preview = findViewById(R.id.lab_color_preview);
        this.raw = findViewById(R.id.lab_color_raw);
        this.palette = findViewById(R.id.lab_color_palette);
        this.formatRow = findViewById(R.id.lab_color_format_row);
        this.modeRow = findViewById(R.id.lab_color_mode_row);

        buildModeRow();
        buildPalette();
        buildFormatRow();

        findViewById(R.id.lab_color_copy_section).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copy(false);
            }
        });
        findViewById(R.id.lab_color_copy_amp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copy(true);
            }
        });
        findViewById(R.id.lab_color_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                input.setText("");
            }
        });
        findViewById(R.id.lab_color_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (refreshing) {
                    return;
                }
                int len = s.length();
                int[] next = new int[len];
                for (int i = 0; i < len; i++) {
                    next[i] = i < charColors.length ? charColors[i] : 15;
                }
                charColors = next;
                refresh();
            }
        });

        refresh();
    }

    private void buildModeRow() {
        Context context = getContext();
        modeWhole = createButton(context.getString(R.string.lab_color_mode_whole));
        modeChar = createButton(context.getString(R.string.lab_color_mode_char));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        modeWhole.setLayoutParams(lp);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp2.setMargins(dp(8), 0, 0, 0);
        modeChar.setLayoutParams(lp2);
        modeWhole.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                charMode = false;
                updateModeButtons();
                refresh();
            }
        });
        modeChar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                charMode = true;
                updateModeButtons();
                refresh();
            }
        });
        modeRow.addView(modeWhole);
        modeRow.addView(modeChar);
        updateModeButtons();
    }

    private void updateModeButtons() {
        modeWhole.setTextColor(charMode ? Color.BLACK : Color.parseColor("#0E9384"));
        modeChar.setTextColor(charMode ? Color.parseColor("#0E9384") : Color.BLACK);
    }

    private void buildPalette() {
        Context context = getContext();
        for (int row = 0; row < 2; row++) {
            LinearLayout rowLayout = new LinearLayout(context);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            for (int i = 0; i < 8; i++) {
                final int index = row * 8 + i;
                TextView cell = new TextView(context);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(32), 1f);
                lp.setMargins(dp(2), dp(2), dp(2), dp(2));
                cell.setLayoutParams(lp);
                cell.setGravity(Gravity.CENTER);
                cell.setText(COLOR_CODES[index].toUpperCase());
                cell.setTextSize(12);
                cell.setBackgroundColor(COLOR_ARGB[index]);
                cell.setTextColor(isDark(index) ? Color.WHITE : Color.BLACK);
                cell.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        brushColor = index;
                        if (!charMode) {
                            for (int j = 0; j < charColors.length; j++) {
                                charColors[j] = index;
                            }
                        }
                        refresh();
                    }
                });
                rowLayout.addView(cell);
            }
            palette.addView(rowLayout);
        }
    }

    private void buildFormatRow() {
        Context context = getContext();
        LinearLayout rowLayout = new LinearLayout(context);
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
        rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        final Button[] buttons = new Button[FORMAT_CODES.length];
        for (int i = 0; i < FORMAT_CODES.length; i++) {
            final String code = FORMAT_CODES[i];
            Button button = createButton(context.getString(FORMAT_LABELS[i]));
            button.setTextSize(11);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) {
                lp.setMargins(dp(4), 0, 0, 0);
            }
            button.setLayoutParams(lp);
            buttons[i] = button;
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (formats.contains(code)) {
                        formats.remove(code);
                    } else {
                        formats.add(code);
                    }
                    updateFormatButtons(buttons);
                    refresh();
                }
            });
            rowLayout.addView(button);
        }
        formatRow.addView(rowLayout);
    }

    private void updateFormatButtons(Button[] buttons) {
        for (int i = 0; i < buttons.length; i++) {
            boolean active = formats.contains(FORMAT_CODES[i]);
            buttons[i].setTextColor(active ? Color.parseColor("#0E9384") : Color.BLACK);
        }
    }

    private Button createButton(String text) {
        Button button = new Button(getContext());
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(13);
        button.setTextColor(Color.BLACK);
        button.setBackgroundResource(R.drawable.launcher_button_parent);
        return button;
    }

    private boolean isDark(int index) {
        int color = COLOR_ARGB[index];
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return (r * 0.299 + g * 0.587 + b * 0.114) < 140;
    }

    /** 重新渲染预览与代码。 */
    private void refresh() {
        String text = input.getText().toString();
        SpannableStringBuilder sb = new SpannableStringBuilder(text);
        for (int i = 0; i < text.length(); i++) {
            int colorIndex = i < charColors.length ? charColors[i] : 15;
            sb.setSpan(new ForegroundColorSpan(COLOR_ARGB[colorIndex]), i, i + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            applyFormats(sb, i);
        }
        if (charMode) {
            for (int i = 0; i < text.length(); i++) {
                final int index = i;
                sb.setSpan(new ClickableSpan() {
                    @Override
                    public void updateDrawState(TextPaint ds) {
                        // 保留其他 Span 设置的字体颜色与样式，不做覆盖
                    }

                    @Override
                    public void onClick(View widget) {
                        if (index < charColors.length) {
                            charColors[index] = brushColor;
                            refresh();
                        }
                    }
                }, i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        preview.setText(sb);
        preview.setMovementMethod(charMode ? LinkMovementMethod.getInstance() : null);

        String sectionText = buildOutput(false);
        String ampText = buildOutput(true);
        raw.setText(sectionText.isEmpty() ? "" : sectionText + "\n" + ampText);
    }

    private void applyFormats(SpannableStringBuilder sb, int i) {
        for (String code : formats) {
            Object span = null;
            if ("l".equals(code)) {
                span = new StyleSpan(Typeface.BOLD);
            } else if ("o".equals(code)) {
                span = new StyleSpan(Typeface.ITALIC);
            } else if ("n".equals(code)) {
                span = new UnderlineSpan();
            } else if ("m".equals(code)) {
                span = new StrikethroughSpan();
            }
            // §k（随机）在预览中无法真正随机，仅保留原文，不做额外渲染
            if (span != null) {
                sb.setSpan(span, i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    /** 生成带代码的文本，amp 为 true 时使用 & 代替 §。 */
    private String buildOutput(boolean amp) {
        String text = input.getText().toString();
        String mark = amp ? "&" : SECTION;
        StringBuilder sb = new StringBuilder();
        for (String code : formats) {
            sb.append(mark).append(code);
        }
        int prev = -1;
        for (int i = 0; i < text.length(); i++) {
            int colorIndex = i < charColors.length ? charColors[i] : 15;
            if (colorIndex != prev) {
                sb.append(mark).append(COLOR_CODES[colorIndex]);
                prev = colorIndex;
            }
            sb.append(text.charAt(i));
        }
        return sb.toString();
    }

    private void copy(boolean amp) {
        String text = input.getText().toString();
        if (text.isEmpty()) {
            LabUtils.toast(getContext(), getContext().getString(R.string.lab_color_toast_empty));
            return;
        }
        LabUtils.copyToClipboard(getContext(), buildOutput(amp));
        LabUtils.toast(getContext(), getContext().getString(R.string.lab_color_toast_copied));
    }

    private int dp(int value) {
        return (int) (value * getContext().getResources().getDisplayMetrics().density + 0.5f);
    }
}