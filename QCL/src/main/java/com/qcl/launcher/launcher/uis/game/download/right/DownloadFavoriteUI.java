package com.qcl.launcher.launcher.uis.game.download.right;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.qcl.launcher.R;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.download.favorite.DownloadFavorite;
import com.qcl.launcher.launcher.download.favorite.FavoriteManager;
import com.qcl.launcher.launcher.list.download.ModIconLoader;
import com.qcl.launcher.launcher.mod.RemoteMod;
import com.qcl.launcher.launcher.mod.RemoteModRepository;
import com.qcl.launcher.launcher.mod.curse.CurseForgeRemoteModRepository;
import com.qcl.launcher.launcher.mod.modrinth.ModrinthRemoteModRepository;
import com.qcl.launcher.launcher.uis.game.download.right.resource.DownloadResourceUI;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;
import com.qcl.launcher.utils.string.ModTranslations;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ★ 2026-10-09 用户要求：下载页「收藏夹」（对齐 FCL 的
 * {@code com/tungsten/fcl/ui/download/favorite/FavoritePage.kt}）。
 *
 * <p>本轮补齐（用户逐条提的）：
 * <ol>
 *   <li><b>分类</b>：顶部一排筛选 —— 全部 / 模组 / 整合包 / 资源包 / 光影 / 世界（按条目自身 type 过滤）；</li>
 *   <li><b>中文名</b>：复用下载列表那套 {@link ModTranslations}，把收藏的模组/整合包标题翻成中文；</li>
 *   <li><b>图标</b>：每行左侧显示该模组的图标（复用 {@link ModIconLoader}，带缓存/超时/列表复用校验）。</li>
 * </ol>
 * 「打开」= 按 id 重新 {@code getModById} 拉最新详情 → 进资源详情页（可实时下载最新版）；
 * 拉不到就退化为用浏览器打开项目主页。
 */
public class DownloadFavoriteUI extends BaseUI {

    /** {type 关键字(null=全部), 字符串资源名}；顺序即展示顺序。 */
    private static final Object[][] FILTERS = {
            {null, "download_ui_favorite_all"},
            {"MOD", "download_ui_mod"},
            {"MODPACK", "download_ui_package"},
            {"RESOURCE_PACK", "download_ui_resource_pack"},
            {"SHADER", "download_ui_shader"},
            {"WORLD", "download_ui_world"},
    };

    private LinearLayout favoriteUI;
    private LinearLayout filterRow;
    private LinearLayout listBox;
    private TextView emptyHint;
    /** 当前选中的分类（null = 全部）。 */
    private String filterType;

    public DownloadFavoriteUI(Context context, MainActivity activity) {
        super(context, activity);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ensureViews();
    }

    /**
     * ★ 视图**惰性获取 + 全部判空**（崩溃教训见 memory：{@code DownloadUIManager} 构造器会立刻
     * {@code switchDownloadUI}，把其余页挨个 {@code onStop()}；那一刻视图还没拿到 ⇒ NPE 炸启动器）。
     */
    private void ensureViews() {
        if (this.favoriteUI != null) {
            return;
        }
        this.favoriteUI = (LinearLayout) this.activity.findViewById(R.id.ui_download_favorite);
        this.filterRow = (LinearLayout) this.activity.findViewById(R.id.favorite_filter);
        this.listBox = (LinearLayout) this.activity.findViewById(R.id.favorite_list);
        this.emptyHint = (TextView) this.activity.findViewById(R.id.favorite_empty);
        View refresh = this.activity.findViewById(R.id.favorite_refresh);
        if (refresh != null) {
            refresh.setOnClickListener(v -> rebuild());
        }
        buildFilterChips();
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            ensureViews();
            FavoriteManager.init(this.context);
            if (this.favoriteUI != null) {
                // ★★★★★ 2026-10-11（用户实测「收藏夹点进去是从左到右滑出来的，
                //   而其他页面基本都是点击一下直接从中间浮现，动画不一致」）：
                //
                //   根因：`CustomAnimationUtils.showViewFromLeft(..., z)` 的第 4 个参数
                //   就是"要不要播页面转场动画"：
                //     · 收藏夹这里传的是 **true** ⇒ 播 makeInAnimation ⇒ **从左滑入**；
                //     · 其它页面（模组/整合包/资源包…）都传 false ⇒ **瞬间显示**；
                //     · 而 UIManager.switchMainUI 本身**不做任何页面动画**。
                //   于是全项目只有收藏夹一个页面带滑入动画 —— 这就是"不一致"。
                //
                //   统一做法：**去掉收藏夹这一个特例**，改成和其它页面完全一样
                //   （直接 VISIBLE，不播滑入）。这样所有页面切换动画就一致了。
                this.favoriteUI.setVisibility(View.VISIBLE);
            }
            rebuild();
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        try {
            // ★★★ 用户实测「收藏夹像幻影，点别的页面它会从右往左消失」——
            //   根因：这里只调 `hideViewToLeft`（**带动画**），而切页是**同步**发生的：
            //   动画还没播完，界面已经切到下一页了 ⇒ 收藏页仍叠在上面，
            //   之后动画结束才"凭空消失"。表现就是"幻影 + 自己滑走"。
            //   ⇒ 切走时**立即** `setVisibility(GONE)`（不留残影），下个页面自然盖住。
            if (this.favoriteUI != null) {
                this.favoriteUI.setVisibility(View.GONE);
            }
        } catch (Throwable ignored) {
        }
    }

    // ------------------------------------------------------------------ 分类筛选

    /** 顶部一排分类（全部 / 模组 / 整合包 / 资源包 / 光影 / 世界）。 */
    private void buildFilterChips() {
        if (this.filterRow == null) {
            return;
        }
        this.filterRow.removeAllViews();
        for (Object[] entry : FILTERS) {
            final String type = (String) entry[0];
            final String resName = (String) entry[1];
            TextView chip = new TextView(this.context);
            chip.setText(labelOf(resName));
            chip.setTextSize(12f);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(12), dp(6), dp(12), dp(6));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(8));
            chip.setLayoutParams(lp);
            boolean selected = (this.filterType == null && type == null)
                    || (this.filterType != null && this.filterType.equals(type));
            styleChip(chip, selected);
            chip.setOnClickListener(v -> {
                this.filterType = type;
                buildFilterChips();
                rebuild();
            });
            this.filterRow.addView(chip);
        }
    }

    private void styleChip(TextView chip, boolean selected) {
        int accent = this.context.getResources().getColor(R.color.colorAccent);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(dp(14));
        if (selected) {
            bg.setColor(accent);
            chip.setTextColor(0xFFFFFFFF);
        } else {
            bg.setColor(0x22000000);
            chip.setTextColor(accent);
        }
        chip.setBackground(bg);
    }

    /** 资源名 → 文案（拿不到就退回资源名本身，不崩）。 */
    private String labelOf(String resName) {
        try {
            int id = this.context.getResources().getIdentifier(
                    resName, "string", this.context.getPackageName());
            if (id != 0) {
                return this.context.getString(id);
            }
        } catch (Throwable ignored) {
        }
        return resName;
    }

    /** 条目是否命中当前分类。 */
    private boolean matches(DownloadFavorite f) {
        if (this.filterType == null) {
            return true;
        }
        String t = f.type == null ? "" : f.type.toUpperCase(Locale.ROOT);
        if ("SHADER".equals(this.filterType)) {
            return t.contains("SHADER");
        }
        if ("MOD".equals(this.filterType)) {
            return "MOD".equals(t);   // 不要把 MODPACK 也算成模组
        }
        return this.filterType.equals(t);
    }

    // ------------------------------------------------------------------ 列表

    private void rebuild() {
        if (this.listBox == null) {
            return;
        }
        try {
            FavoriteManager.init(this.context);
            this.listBox.removeAllViews();
            List<DownloadFavorite> all = FavoriteManager.list();
            List<DownloadFavorite> list = new ArrayList<>();
            for (DownloadFavorite f : all) {
                if (matches(f)) {
                    list.add(f);
                }
            }
            if (this.emptyHint != null) {
                this.emptyHint.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            }
            for (int i = 0; i < list.size(); i++) {
                android.view.View row = buildRow(list.get(i), i);
                this.listBox.addView(row);
                animateRowIn(row, i);
            }
            // ★★ 2026-10-09 用户实测「左边的模组图标不准确、还有空白」——
            //   根因：收藏时存下的是**当时**的 iconUrl，项目换图标后 URL 就过期了，
            //   而**自愈（FavoriteManager.refresh）此前只在点「打开」进详情页时才跑**，
            //   列表首次显示永远用过期 URL ⇒ 图标拉不到 ⇒ 只剩占位（看着像空白）。
            //   ⇒ 现在列表显示完就后台逐条补全，拿到新 URL 后回主线程刷新对应行。
            healIcons(list);
        } catch (Throwable ignored) {
        }
    }

    /**
     * ★ 1.5.0：收藏夹列表逐行入场动画（淡入 + 轻微右移）。
     *
     * <p>用户要求「只有收藏按钮有动态切换，其他条目没有」—— 根因是
     * {@link #rebuild()} 里 {@code addView} 之后没有任何动画，分类一切就瞬间刷出来。
     * <p>用 Android 原生 ViewPropertyAnimator（项目其他页面的 CustomAnimationUtils 是**页面级**转场，
     * 不适用列表项），逐行延迟 18ms 形成依次出现的效果；行数很多时封顶总时长，避免等太久。
     */
    private void animateRowIn(android.view.View row, int index) {
        try {
            if (row == null) {
                return;
            }
            row.setAlpha(0f);
            row.setTranslationX(dp(18));
            row.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(180L)
                    // 封顶总时长：第 20 行之后不再累加延迟，最多等约 360ms
                    .setStartDelay(Math.min(index, 20) * 18L)
                    .start();
        } catch (Throwable ignored) {
            // 动画失败就静态显示，绝不影响功能
        }
    }

    /**
     * ★ 后台逐条补全过期图标 URL（自愈），完成后重绘列表。
     * 失败/取不到就保持原样 —— 绝不因为图标问题让整页失败。
     */
    private void healIcons(final List<DownloadFavorite> list) {
        try {
            if (list == null || list.isEmpty()) {
                return;
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    boolean changed = false;
                    for (DownloadFavorite f : list) {
                        try {
                            if (f == null || f.source == null) {
                                continue;
                            }
                            RemoteModRepository repo = repositoryFor(f.source, f.type);
                            if (repo == null) {
                                continue;
                            }
                            // ★★★ 2026-10-09 用户实测「整合包里是 FO 圆标，收藏夹里变成 Modrinth
                            //   默认的彩色圆环」—— 因为收藏时**只有 slug**（Modrinth 项目 slug），
                            //   拿它去 `getModById` 得到的 iconUrl 是 Modrinth 的**通用占位图**。
                            //   ⇒ 必须**无条件回查一次**，拿项目**真正的** iconUrl 覆盖掉占位图。
                            //     （原来写着「iconUrl 非空就跳过」= 永远不生效，等于没这段代码。）
                            String id = f.modId;
                            if (id == null || id.isEmpty()) {
                                id = f.slug;
                            }
                            if (id == null || id.isEmpty()) {
                                continue;
                            }
                            RemoteMod m = repo.getModById(id);
                            if (m == null) {
                                continue;
                            }
                            String fresh = m.getIconUrl();
                            if (fresh != null && !fresh.isEmpty()
                                    && (f.iconUrl == null || f.iconUrl.isEmpty()
                                        || !fresh.equals(f.iconUrl))) {
                                // ★ 同时把原名也校正一遍（收藏时可能只存了 slug）
                                FavoriteManager.refresh(DownloadFavoriteUI.this.context, f,
                                        fresh, m.getTitle());
                                changed = true;
                            }
                        } catch (Throwable ignoredOne) {
                        }
                    }
                    if (!changed) {
                        return;
                    }
                    DownloadFavoriteUI.this.activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                rebuild();
                            } catch (Throwable ignored) {
                            }
                        }
                    });
                }
            }, "qcl-fav-icon-heal").start();
        } catch (Throwable ignored) {
        }
    }

    /** 一行收藏：图标 + （中文）标题 + 「来源 · 类型」+ [打开] [★]。 */
    private View buildRow(final DownloadFavorite f, int index) {
        LinearLayout row = new LinearLayout(this.context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        try {
            row.setBackground(this.context.getDrawable(R.drawable.launcher_button_gray_blue));
        } catch (Throwable ignored) {
        }
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.bottomMargin = dp(6);
        row.setLayoutParams(rowLp);

        // ★ 图标（用户要求：收藏的模组旁边要有它自己的图标）
        //   ★ 2026-10-09 修「图标不显示」：**图标下面垫一个占位**（圆角底 + 标题首字）。
        //   ★ 2026-10-09 用户实测「图标不准确、还有空白」，照**整合包列表**
        //     （`R.layout.item_download_mod`）的规格逐项对齐：
        //       ① 尺寸 30dp          —— 原样
        //       ② **scaleType=centerInside** —— 原来没设（默认 fitCenter），
        //          模组图标多为 64×64 小图，缺这一句会被拉伸变形 ⇒ "图标不准确"
        //       ③ 占位底色**保持默认透明**（用 ui_download_favorite 自带的底色，
        //          不额外加昼夜色 —— 用户明确要求"移除随昼夜变化，改为默认颜色"）
        android.widget.FrameLayout iconBox = new android.widget.FrameLayout(this.context);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(30), dp(30));
        iconLp.setMarginEnd(dp(10));
        iconBox.setLayoutParams(iconLp);

        TextView placeholder = new TextView(this.context);
        placeholder.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
        placeholder.setGravity(Gravity.CENTER);
        placeholder.setTextSize(13f);
        try {
            // ★ 只用**默认色**（colorAccent / colorPureBlack 随系统主题），
            //   不引入半透明昼夜色 —— 保持和整合包列表一致的默认观感。
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setCornerRadius(dp(6));
            bg.setColor(0x00000000);
            placeholder.setBackground(bg);
            placeholder.setTextColor(this.context.getResources().getColor(R.color.colorPureBlack));
        } catch (Throwable ignored) {
        }
        String t0 = displayTitle(f);
        placeholder.setText(t0.isEmpty() ? "?" : t0.substring(0, 1));
        iconBox.addView(placeholder);

        ImageView icon = new ImageView(this.context);
        icon.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
        // ★ 与整合包列表一致：centerInside，绝不把 64×64 的模组图拉伸变形
        icon.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        try {
            ModIconLoader.load(this.context, icon, f.iconUrl, index);
        } catch (Throwable ignored) {
        }
        iconBox.addView(icon);
        row.addView(iconBox);

        LinearLayout col = new LinearLayout(this.context);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = new TextView(this.context);
        title.setText(displayTitle(f));          // ★ 中文标题
        title.setTextSize(14f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        col.addView(title);

        TextView meta = new TextView(this.context);
        meta.setText((f.source == null ? "" : f.source) + " · " + (f.type == null ? "" : f.type)
                + (isTitleTranslated(f) ? "   " + (f.title == null ? "" : f.title) : ""));
        meta.setTextSize(11f);
        meta.setSingleLine(true);
        meta.setEllipsize(android.text.TextUtils.TruncateAt.END);
        try {
            meta.setTextColor(this.context.getResources().getColor(R.color.colorAccent));
        } catch (Throwable ignored) {
        }
        col.addView(meta);
        row.addView(col);

        TextView open = new TextView(this.context);
        open.setText(R.string.download_ui_favorite_open);
        open.setTextSize(13f);
        open.setPadding(dp(10), dp(6), dp(10), dp(6));
        open.setTextColor(this.context.getResources().getColor(R.color.colorAccent));
        open.setOnClickListener(v -> openFavorite(f));
        row.addView(open);

        TextView del = new TextView(this.context);
        del.setText("★");
        del.setTextSize(16f);
        del.setPadding(dp(8), dp(6), dp(4), dp(6));
        del.setOnClickListener(v -> {
            try {
                FavoriteManager.remove(this.context, f.id);
                rebuild();
            } catch (Throwable ignored) {
            }
        });
        row.addView(del);
        return row;
    }

    /**
     * ★ 用户要求「收藏的模组也翻译成中文」：复用下载列表那套 {@link ModTranslations}。
     * 拿不到中文就返回原名（绝不显示空白）。
     */
    private String displayTitle(DownloadFavorite f) {
        String raw = f.title == null ? "" : f.title;
        try {
            ModTranslations tr = translationsFor(f.type);
            // ★★★ 2026-10-09 用户实测：收藏夹标题变成
            //     「[FO] 难以置信的优化 (Fabulously Optimized) Fabulously Optimized」
            //     —— 中文译名后多跟一遍原名，前面还多个 `[FO]`。
            //   根因：拿 **slug** 去查 `getModByCurseForgeId`，但 Modrinth 项目 slug 与
            //   CurseForge modId **是两套编号**，查出的是**别的项目**的译名，于是拼重了。
            if (tr != null) {
                // ★★★ 1.5.0 修正（用户实测「收藏夹里的根本没显示出来翻译」）：
                //   原来**只有 source == CURSEFORGE 才查**，于是收藏夹里绝大多数
                //   Modrinth 收藏项直接显示英文原名、完全没有中文。
                //   ⇒ 去掉来源限制，改为与下载主列表一致的三路回退：
                //     slug → modId 索引 → 英文名去空格。
                ModTranslations.Mod m = null;
                if (f.slug != null && !f.slug.isEmpty()) {
                    m = tr.getModByCurseForgeId(f.slug);
                    if (m == null) {
                        m = tr.getModById(f.slug);
                    }
                }
                if (m == null && raw.contains(" ")) {
                    // 有些站点的 slug 就是「名称 去空格」，用整名再试一次
                    m = tr.getModByCurseForgeId(raw.replace(" ", ""));
                    if (m == null) {
                        m = tr.getModById(raw.replace(" ", ""));
                    }
                }
                if (m != null && m.getDisplayName() != null && !m.getDisplayName().isEmpty()) {
                    return joinCnEn(m.getDisplayName(), raw);
                }
            }
        } catch (Throwable ignored) {
        }
        return raw;
    }

    /**
     * ★ 标题拼接规则（用户要求：「英文和中文各出现一次」）：`中文（English）`。
     * · 只有中文 → 只显示中文
     * · 只有英文 → 只显示英文
     * · 两者都有且**不相同** → `中文（English）`
     * · 译名里本来就带原名（如 `难以置信的优化 (Fabulously Optimized)`）⇒ 原样返回，不再追加
     * · 去掉 FCL 那种 `[FO]` 之类的前缀标记
     */
    private static String joinCnEn(String cn, String en) {
        String c = cn == null ? "" : cn.trim();
        String e = en == null ? "" : en.trim();
        // 去掉译名开头的 [XX] 标记
        while (c.startsWith("[") && c.contains("]")) {
            int k = c.indexOf(']');
            if (k < 0 || k > 12) {
                break;
            }
            c = c.substring(k + 1).trim();
        }
        if (c.isEmpty()) {
            return e;
        }
        if (e.isEmpty()) {
            return c;
        }
        if (c.equals(e)) {
            return c;
        }
        // 译名里已经含有原名（原样或加括号）⇒ 不再追加
        if (c.contains(e)) {
            return c;
        }
        return c + "（" + e + "）";
    }

    /** 原标题是否被翻译过（翻译过就把原标题附在副标题里，方便玩家对照）。 */
    private boolean isTitleTranslated(DownloadFavorite f) {
        String raw = f.title == null ? "" : f.title;
        return !raw.isEmpty() && !raw.equals(displayTitle(f));
    }

    private ModTranslations translationsFor(String type) {
        try {
            String t = type == null ? "" : type.toUpperCase(Locale.ROOT);
            if (t.contains("MODPACK")) {
                return ModTranslations.MODPACK;
            }
            if ("MOD".equals(t)) {
                return ModTranslations.MOD;
            }
        } catch (Throwable ignored) {
        }
        return ModTranslations.EMPTY;
    }

    // ------------------------------------------------------------------ 打开

    /**
     * 「打开」：**按 id 重新向平台拉一次详情**（→ 看到最新更新），再进资源详情页（→ 可实时下载）。
     * 拉不到就退化为用浏览器打开项目主页。
     */
    private void openFavorite(final DownloadFavorite f) {
        if (f == null) {
            return;
        }
        Toast.makeText(this.context, R.string.download_ui_favorite_loading, Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            RemoteMod mod = null;
            RemoteModRepository repo = null;
            try {
                repo = repositoryFor(f.source, f.type);
                if (repo != null && f.modId != null && !f.modId.isEmpty()) {
                    mod = repo.getModById(f.modId);
                }
            } catch (Throwable ignored) {
            }
            final RemoteMod fMod = mod;
            final RemoteModRepository fRepo = repo;
            this.activity.runOnUiThread(() -> {
                try {
                    if (fMod != null && fRepo != null) {
                        // ★ 2026-10-09：拉到详情后把最新图标/标题写回收藏条目（图标不显示的根因修复）
                        try {
                            FavoriteManager.refresh(this.context, f, fMod.getIconUrl(), fMod.getTitle());
                        } catch (Throwable ignored) {
                        }
                        this.activity.uiManager.switchMainUI(new DownloadResourceUI(
                                this.context, this.activity, fRepo, fMod, typeOf(f.type)));
                    } else {
                        Toast.makeText(this.context, R.string.download_ui_favorite_fail,
                                Toast.LENGTH_LONG).show();
                        if (f.pageUrl != null && !f.pageUrl.isEmpty()) {
                            Intent it = new Intent(Intent.ACTION_VIEW, Uri.parse(f.pageUrl));
                            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            this.context.startActivity(it);
                        }
                    }
                } catch (Throwable ignored) {
                }
            });
        }, "qcl-favorite-open").start();
    }

    /**
     * 平台 + 资源类型 → 仓库实例。
     *
     * <p>★ 两个仓库都提供了**按类型准备好的静态实例**（构造器 private，不能 new）：
     * {@code ModrinthRemoteModRepository.MODS/MODPACKS/RESOURCE_PACKS/SHADERS}、
     * {@code CurseForgeRemoteModRepository.MODS/MODPACKS/RESOURCE_PACKS/WORLDS/CUSTOMIZATIONS}。
     */
    private RemoteModRepository repositoryFor(String source, String typeName) {
        try {
            String t = typeName == null ? "MOD" : typeName.toUpperCase(Locale.ROOT);
            if ("CURSEFORGE".equals(source)) {
                if (t.contains("MODPACK")) {
                    return CurseForgeRemoteModRepository.MODPACKS;
                }
                if (t.contains("RESOURCE")) {
                    return CurseForgeRemoteModRepository.RESOURCE_PACKS;
                }
                if (t.contains("WORLD")) {
                    return CurseForgeRemoteModRepository.WORLDS;
                }
                if (t.contains("CUSTOM")) {
                    return CurseForgeRemoteModRepository.CUSTOMIZATIONS;
                }
                return CurseForgeRemoteModRepository.MODS;
            }
            if (t.contains("MODPACK")) {
                return ModrinthRemoteModRepository.MODPACKS;
            }
            if (t.contains("RESOURCE")) {
                return ModrinthRemoteModRepository.RESOURCE_PACKS;
            }
            if (t.contains("SHADER")) {
                return ModrinthRemoteModRepository.SHADERS;
            }
            return ModrinthRemoteModRepository.MODS;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** FCL 的资源类别名 → 适配器用的 type 整数。 */
    private int typeOf(String typeName) {
        try {
            return RemoteModRepository.Type.valueOf(typeName).ordinal();
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private int dp(int v) {
        return Math.round(v * this.context.getResources().getDisplayMetrics().density);
    }
}
