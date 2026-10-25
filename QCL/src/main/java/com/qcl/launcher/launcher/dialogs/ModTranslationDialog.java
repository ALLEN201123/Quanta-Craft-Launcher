package com.qcl.launcher.launcher.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.qcl.launcher.R;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.utils.string.ModTranslations;

import java.util.ArrayList;
import java.util.List;

/**
 * ★★★ 2026-10-25（1.5.5）**「模组/整合包对应英文查询」弹窗**。
 *
 * <p>用户需求原话（视频演示的那一屏）：搜中文「暮色森林」时，弹一个框列出
 * 「暮色森林 The Twilight Forest TF」「巫术学：暮色森林法术包 Electroblob's Wizardry…」
 * 「动态的树：暮色森林附属 Dynamic Trees-The Twilight Forest」……，
 * 玩家点其中一条，就把搜索词换成对应的英文名再搜。
 *
 * <p>★ 为什么 QCL 需要这个（1.5.2 只做了「静默自动翻译」的坑）：
 * <ul>
 *   <li>静默翻译只取**最匹配的一条**。玩家搜「暮色森林」时，翻译表里有多条都沾边
 *       （本体、法术包、附属模组、传送门催化剂……），取哪条全看 LCS 得分，
 *       玩家**看不到候选、也换不了**——搜不到想要的那个时唯一的办法是自己猜英文名。</li>
 *   <li>这个弹窗把候选**摆出来给玩家选**，等于把「猜英文名」变成「点一下」。</li>
 * </ul>
 *
 * <p>数据源与搜索页共用同一份翻译表（{@code assets/mod_data.txt} /
 * {@code assets/modpack_data.txt}），按仓库类型（MOD / MODPACK）取对应那张，
 * 与 {@link com.qcl.launcher.launcher.mod.LocalizedRemoteModRepository} 的自动翻译**同源**，
 * 所以这里选中的英文名，搜索页一定认得。
 */
public class ModTranslationDialog extends Dialog {

    /** 选中某一条后的回调：参数是**英文名**（subname 为空时回退到缩写）。 */
    public interface OnPickListener {
        void onPick(String englishName);
    }

    /** ★ 1.4.3 教训：直接持有 MainActivity（不要用 getContext() 转，Dialog 的主题包装会让 instanceof 落空）。 */
    private final MainActivity activity;
    private final ModTranslations translations;
    private final OnPickListener callback;

    private EditText searchInput;
    private LinearLayout resultsContainer;

    public ModTranslationDialog(Context context, MainActivity activity,
                                ModTranslations translations, OnPickListener callback) {
        super(context);
        this.activity = activity;
        this.translations = translations == null ? ModTranslations.EMPTY : translations;
        this.callback = callback;
    }

    @Override
    public void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_mod_translation);

        searchInput = findViewById(R.id.translation_search_input);
        resultsContainer = findViewById(R.id.translation_results);

        View cancel = findViewById(R.id.translation_cancel);
        if (cancel != null) {
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        }

        // 输入变化 → 实时列候选（照 FCL 的 TranslationDialog：每敲一个字就重查）
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                refresh(s == null ? "" : s.toString());
            }
        });
    }

    /**
     * ★ 照 FCL：弹窗出来就**自动聚焦输入框**并带上当前搜索词 ——
     * 玩家多半是「搜不到，想换个名字再试」，直接改字比从头打字快。
     */
    @Override
    public void onStart() {
        super.onStart();
        setTitle(R.string.translation_dialog_title);
        try {
            searchInput.requestFocus();
        } catch (Throwable ignored) {
        }
    }

    /** 用当前搜索页里的关键词预填。 */
    public void setInitialKeyword(String keyword) {
        if (keyword != null && searchInput != null) {
            searchInput.setText(keyword);
            searchInput.setSelection(keyword.length());
        }
    }

    /**
     * 按输入框里的内容重新列候选。
     *
     * <p>★ 中文用 {@code searchMod}（按中文名做 LCS 匹配），
     * 英文用 {@code searchModByEnglish}（纠正大小写/拼写）——
     * 与搜索页 {@code LocalizedRemoteModRepository.search} 的处理**完全一致**，
     * 所以候选列表和实际会搜的内容对得上。
     */
    private void refresh(String keyword) {
        if (resultsContainer == null) {
            return;
        }
        resultsContainer.removeAllViews();
        String q = keyword == null ? "" : keyword.trim();
        if (q.isEmpty()) {
            return;
        }
        List<ModTranslations.Mod> hits;
        try {
            boolean chinese = com.qcl.launcher.utils.string.StringUtils.CHINESE_PATTERN.matcher(q).find();
            hits = chinese ? translations.searchMod(q) : translations.searchModByEnglish(q);
        } catch (Throwable t) {
            hits = new ArrayList<>();
        }
        if (hits == null || hits.isEmpty()) {
            return;
        }
        for (final ModTranslations.Mod mod : hits) {
            resultsContainer.addView(buildRow(mod));
        }
    }

    /**
     * 一行候选：**中文名 + 英文名 + 缩写**（照 FCL 的 {@code "${name} ${subname} ${abbr}"}）。
     * 点它就回调英文名。
     */
    private View buildRow(final ModTranslations.Mod mod) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, 12, 0, 12);
        row.setBackgroundResource(R.drawable.launcher_button_parent);
        row.setClickable(true);
        row.setFocusable(true);

        String name = safe(mod.getName());
        String sub = safe(mod.getSubname());
        String abbr = safe(mod.getAbbr());

        TextView title = new TextView(activity);
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        if (!sub.isEmpty()) {
            sb.append(' ').append(sub);
        }
        if (!abbr.isEmpty()) {
            sb.append(' ').append(abbr);
        }
        title.setText(sb.toString());
        title.setTextSize(14);
        title.setTextColor(activity.getResources().getColor(R.color.colorPureBlack));
        title.setSingleLine(false);
        row.addView(title);

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String en = safe(mod.getSubname());
                if (en.isEmpty()) {
                    en = safe(mod.getAbbr());
                }
                if (en.isEmpty()) {
                    en = safe(mod.getName());
                }
                if (callback != null) {
                    callback.onPick(en);
                }
                dismiss();
            }
        });
        return row;
    }

    /** null → 空串（翻译表里有些条目字段是空的）。 */
    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
