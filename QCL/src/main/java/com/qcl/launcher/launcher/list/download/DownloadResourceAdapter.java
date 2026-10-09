package com.qcl.launcher.launcher.list.download;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.qcl.launcher.R;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.download.modloader.ModLoaderDetector;
import com.qcl.launcher.launcher.download.favorite.DownloadFavorite;
import com.qcl.launcher.launcher.download.favorite.FavoriteManager;
import com.qcl.launcher.launcher.mod.RemoteMod;
import com.qcl.launcher.launcher.mod.RemoteModRepository;
import com.qcl.launcher.launcher.mod.curse.CurseForgeRemoteModRepository;
import com.qcl.launcher.launcher.uis.game.download.right.resource.DownloadResourceUI;
import com.qcl.launcher.utils.LocaleUtils;
import com.qcl.launcher.utils.string.ModTranslations;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;

public class DownloadResourceAdapter extends BaseAdapter {

    private Context context;
    private MainActivity activity;
    private RemoteModRepository repository;
    private ArrayList<RemoteMod> modList;
    private int type;

    private static class ViewHolder{
        LinearLayout item;
        ImageView icon;
        TextView name;
        TextView categories;
        TextView introduction;
        /** ★ 2026-10-09：收藏星标 */
        TextView favorite;
    }

    public DownloadResourceAdapter(Context context, MainActivity activity, RemoteModRepository repository, ArrayList<RemoteMod> modList, int type){
        this.context = context;
        this.activity = activity;
        this.repository = repository;
        this.modList = modList;
        this.type = type;
    }

    @Override
    public int getCount() {
        return modList.size();
    }

    @Override
    public Object getItem(int position) {
        return modList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;
        if (convertView == null){
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(context).inflate(R.layout.item_download_mod,null);
            viewHolder.item = convertView.findViewById(R.id.item);
            viewHolder.icon = convertView.findViewById(R.id.mod_icon);
            viewHolder.name = convertView.findViewById(R.id.mod_name);
            viewHolder.categories = convertView.findViewById(R.id.mod_categories);
            viewHolder.introduction = convertView.findViewById(R.id.mod_introduction);
            viewHolder.favorite = convertView.findViewById(R.id.mod_favorite);
            activity.exteriorConfig.apply(viewHolder.categories);
            convertView.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.icon.setImageDrawable(context.getDrawable(R.drawable.launcher_background_color_white));
        viewHolder.icon.setTag(position);
        // ★★★ 1.2.7：图标下载交给 ModIconLoader（内存+磁盘缓存 / 固定线程池 / 带超时 /
        //   兼容动图 GIF / 列表复用校验）。
        //   老代码是「每个条目 new 一个线程 + 没有超时 + 没有缓存」：
        //   图标服务器一慢（国内连 Modrinth 的 cdn.modrinth.com 尤其慢），
        //   线程就一直挂着 → 表现就是「图标一直白着、加载半天出不来」；
        //   而且每次滚动都要重下一遍，永远不会变快。
        ModIconLoader.load(context, viewHolder.icon, modList.get(position).getIconUrl(), position);
        StringBuilder categories = new StringBuilder();
        for (String category : modList.get(position).getCategories()) {
            boolean isCurse = modList.get(position).getPageUrl() != null && modList.get(position).getPageUrl().contains("curseforge");
            String c;
            int resId = context.getResources().getIdentifier((isCurse ? "curse_category_" : "modrinth_category_") + category.replace("-","_"),"string",context.getPackageName());
            if (resId != 0 && context.getString(resId) != null) {
                c = context.getString(resId);
            }
            else {
                c = category;
            }
            categories.append(c).append("   ");
        }
        viewHolder.categories.setText(categories.toString());

        // ★ 1.2.3：按「你当前的版本」装的加载器，给不兼容的模组卡片标红字警告（只对模组页生效）。
        //   规则（用户定的）：没装加载器 → 只有不依赖加载器的（纯 class 型）不标；
        //   ModLoader → 依赖 ModLoader 的和纯 class 型不标；Babric → 只有 Babric/Fabric 的不标。
        //   加载器信息取自 Modrinth 的 categories（CurseForge 的 categories 是玩法分类，没有就按无依赖处理）。
        if (type == 0) {
            try {
                // ★★★ 1.2.5 修：publicGameSetting.currentVersion 里存的是**完整路径**
                //   （<游戏目录>/versions/<版本名>，见 GameListAdapter / MainUI 的赋值），
                //   老代码又给它拼了一次 "/versions/" → 拼出来的目录根本不存在
                //   → ModLoaderDetector.detect() 恒返回 null（等于「没装任何加载器」）
                //   → 不管玩家装的是 ModLoader / Babric / Fabric / Forge，
                //     整个模组页都被标上「不支持你当前的版本」。
                //   现在直接用这个路径；只有它不是目录时才退回归属拼接（兼容老数据）。
                // ★ 1.3.0：改用「下载页上选的版本」（gameVersion），而不是全局 currentVersion。
                //   玩家在下载页把版本从 Babric 切成 ModLoader 后，这个徽章要跟着变，
                //   不能还拿全局旧版本去判断 → 否则「我明明切了版本，还提示不支持」。
                String cur = activity.uiManager.downloadUI.downloadUIManager.downloadModUI.gameVersion;
                if (cur == null || cur.isEmpty()) {
                    String g = activity.publicGameSetting.currentVersion;
                    cur = g == null ? null : new File(g).getName();
                }
                File vDir = cur == null ? null
                        : new File(activity.launcherSetting.gameFileDirectory + "/versions/" + cur);
                String currentLoader = ModLoaderDetector.detect(vDir);
                java.util.List<String> modLoaders = new java.util.ArrayList<>();
                for (String c : modList.get(position).getCategories()) {
                    String low = c.toLowerCase();
                    if (low.contains("babric")) modLoaders.add("babric");
                    else if (low.contains("modloader")) modLoaders.add("modloader");
                    else if (low.contains("fabric")) modLoaders.add("fabric");
                    // ★ neoforge 要放 forge 前面判：它包含 "forge" 字样
                    else if (low.contains("neoforge")) modLoaders.add("neoforge");
                    else if (low.contains("forge")) modLoaders.add("forge");
                    else if (low.contains("quilt")) modLoaders.add("quilt");
                    else if (low.contains("liteloader")) modLoaders.add("liteloader");
                }
                if (!ModLoaderDetector.isSupported(currentLoader, modLoaders)) {
                    android.text.SpannableStringBuilder ssb = new android.text.SpannableStringBuilder();
                    String warn = "⚠ 不支持你当前的版本";
                    ssb.append(warn);
                    ssb.setSpan(new android.text.style.ForegroundColorSpan(0xFFFF4444),
                            0, warn.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ssb.append("   ").append(categories);
                    viewHolder.categories.setText(ssb);
                }
            } catch (Throwable ignored) {
            }
        }
        ModTranslations modTranslations;
        if (type == 0) {
            modTranslations = ModTranslations.MOD;
        }
        else if (type == 1) {
            modTranslations = ModTranslations.MODPACK;
        }
        else {
            modTranslations = ModTranslations.EMPTY;
        }
        // ★★★ 1.5.0 用户要求：「模组名称首先显示中文，后面显示英文」。
        //   原逻辑是**二选一**：查到中文就只显示中文，查不到才显示英文 ⇒ 英文名整个看不到。
        //   现在改成**中文在前 + 英文在后**（"中文名  English Name"）。
        //   ★ 中文系统才拼；英文系统保持原样只显示英文（用户明确说"首先中文后面英文"）。
        viewHolder.name.setText(displayTitleWithEn(modList.get(position), modTranslations));
        viewHolder.introduction.setText(modList.get(position).getDescription());
        final RemoteMod mod = modList.get(position);
        viewHolder.item.setOnClickListener(view -> {
            // ★ 1.5.0 混合搜索：列表里混着 CurseForge 与 Modrinth 的结果，
            //   详情页必须拿 **这个 mod 自己那个源** 的仓库，
            //   否则版本列表 / 依赖 / 安装全部会打到另一站去（串台 → 404 / 空列表）。
            DownloadResourceUI downloadResourceUI =
                    new DownloadResourceUI(context, activity, repositoryFor(mod), mod, type);
            activity.uiManager.switchMainUI(downloadResourceUI);
        });
        // ★ 2026-10-09 用户要求：下载页收藏（照 FCL 的 FavoriteActions —— 列表项上放星标，点一下加入/移出）
        if (viewHolder.favorite != null) {
            try {
                FavoriteManager.init(context);
                // ★ 1.5.0 混合模式下 sourceOf(repository) 会是 UNKNOWN ⇒ 改按 mod 自身来源判
                final String src = sourceOf(repository);
                final String realSrc = "UNKNOWN".equals(src)
                        ? com.qcl.launcher.launcher.mod.HybridRemoteModRepository.platformOf(mod)
                        : src;
                final String rid = remoteIdOf(mod);
                viewHolder.favorite.setText(FavoriteManager.isFavorite(realSrc, rid, mod.getSlug()) ? "★" : "☆");
                viewHolder.favorite.setOnClickListener(v -> {
                    DownloadFavorite item = new DownloadFavorite();
                    item.source = realSrc;
                    item.modId = rid;
                    item.type = typeName(type);
                    item.slug = mod.getSlug();
                    // ★★ 2026-10-09 用户实测「收藏夹里标题只有英文，没中文」——
                    //   列表上显示的是**翻译后**的名字（上面 183 行查 ModTranslations），
                    //   但收藏时却存 `mod.getTitle()`（**原名**）⇒ 中文那个名字根本没入库。
                    //   ⇒ 这里把**显示用的那个名字**（可能已是中文）一起存下来。
                    String shown = viewHolder.name.getText().toString();
                    item.title = (shown != null && !shown.isEmpty()) ? shown : mod.getTitle();
                    item.description = mod.getDescription();
                    item.iconUrl = mod.getIconUrl();
                    item.pageUrl = mod.getPageUrl();
                    if (mod.getCategories() != null) {
                        item.categories = new java.util.ArrayList<>(mod.getCategories());
                    }
                    // 建库时再取一次 title/description（列表数据可能已被中文翻译层改过，以详情为准更准）
                    boolean now = FavoriteManager.toggle(context, item);
                    ((TextView) v).setText(now ? "★" : "☆");
                    android.widget.Toast.makeText(context, now ? "已收藏" : "已取消收藏",
                            android.widget.Toast.LENGTH_SHORT).show();
                });
            } catch (Throwable ignoredFav) {
                viewHolder.favorite.setVisibility(View.GONE);
            }
        }
        return convertView;
    }

    /**
     * ★ 1.5.0 混合搜索：给某个 mod 挑「它自己那个源」的仓库。
     *
     * <p>非混合（普通 CurseForge / Modrinth 列表）时原样返回，不改变任何既有行为；
     * 混合时才按 mod 的来源分派。
     */
    private RemoteModRepository repositoryFor(RemoteMod mod) {
        try {
            if (this.repository instanceof com.qcl.launcher.launcher.mod.HybridRemoteModRepository) {
                return ((com.qcl.launcher.launcher.mod.HybridRemoteModRepository) this.repository).repositoryFor(mod);
            }
        } catch (Throwable ignored) {
        }
        return this.repository;
    }

    /** 收藏用的平台标识（与 FCL 的 "CURSEFORGE" / "MODRINTH" 对齐）。 */
    private static String sourceOf(RemoteModRepository repo) {
        try {
            // ★ 1.5.0 混合仓库：光看仓库对象判不出平台（它同时含两站），
            //   这种情况由调用方改用 sourceOf(mod)；这里兜底成 CurseForge 也不对，
            //   所以返回 UNKNOWN 让上层用 mod 自己的来源。
            String n = repo.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT);
            if (n.contains("hybrid")) {
                return "UNKNOWN";
            }
            if (n.contains("curse")) {
                return "CURSEFORGE";
            }
            if (n.contains("modrinth")) {
                return "MODRINTH";
            }
        } catch (Throwable ignored) {
        }
        return "UNKNOWN";
    }

    /** 远程项目 id（Modrinth projectId / CurseForge modId），拿不到就用 slug 兜底。 */
    private static String remoteIdOf(RemoteMod mod) {
        try {
            if (mod.getData() != null) {
                String id = mod.getData().getRemoteId();
                if (id != null && !id.isEmpty()) {
                    return id;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            Object d = mod.getData();
            if (d instanceof com.qcl.launcher.launcher.mod.curse.CurseAddon) {
                return String.valueOf(((com.qcl.launcher.launcher.mod.curse.CurseAddon) d).getId());
            }
        } catch (Throwable ignored) {
        }
        return mod.getSlug() == null ? "" : mod.getSlug();
    }

    /**
     * ★ 1.5.0：给整个列表加一次「淡入 + 轻微上移」的入场动画。
     *
     * <p>用户要求「下载页/收藏夹的列表项没有动态切换」—— 原来五个下载页
     * （模组 / 整合包 / 资源包 / 光影 / 世界）都是数据一填好就瞬间显示。
     *
     * <p>★ 为什么**整块**动画而不是逐项：这是 ListView，`getView` 会复用 convertView，
     *   逐项 animate 会在滚动复用时反复触发、出现跳动 ⇒ 只在数据填充后整体动一次。
     *   （收藏夹是 LinearLayout 逐条 addView，所以那边用的是逐行入场动画。）
     */
    public void animateList(android.widget.ListView listView) {
        try {
            if (listView == null) {
                return;
            }
            // ★★★★★ 2026-10-11 与收藏夹统一（用户要求「收藏页面的动画统一了没，其他页面呢」）：
            //
            //   改之前这里只动**第 0 个可见行**、而且是**上下**滑 14dp ——
            //   收藏夹那边是**逐行**入场、**左右**滑 18dp、180ms、逐行延迟 18ms，
            //   两边观感完全不同（用户实测反馈不统一）。
            //
            //   现在照**收藏夹同一套参数**改：逐行 alpha 0→1 + translationX 18dp→0，
            //   180ms，每行延迟 18ms，超过第 20 行不再累加（封顶约 360ms）。
            //   这也正是 FCL 的做法（`AnimUtil.playTranslationX(view, ..., -100f, 0f)`
            //   —— FCL 也是逐行 + 横向，不是整块纵向）。
            //
            //   ⚠️ ListView 复用 convertView 的顾虑在这里不成立：这段动画只在
            //     **数据填好之后跑一次**（调用处是 search 成功的分支），
            //     动的是当时已经 measure 出来的可见行，不参与 getView 复用。
            // 先把上一轮可能还挂着的延迟动画清掉（转成 View 才有这个方法）
            ((android.view.View) listView).removeCallbacks(null);
            final int count = listView.getChildCount();
            for (int i = 0; i < count; i++) {
                final android.view.View row = listView.getChildAt(i);
                if (row == null) {
                    continue;
                }
                final int index = i;
                row.setAlpha(0f);
                row.setTranslationX(dp(18));
                listView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            row.animate()
                                    .alpha(1f)
                                    .translationX(0f)
                                    .setDuration(180L)
                                    .start();
                        } catch (Throwable ignored) {
                        }
                    }
                }, Math.min(index, 20) * 18L);
            }
        } catch (Throwable ignored) {
            // 动画失败就静态显示
        }
    }

    private int dp(int v) {
        try {
            return (int) (v * context.getResources().getDisplayMetrics().density + 0.5f);
        } catch (Throwable ignored) {
            return v;
        }
    }

    /**
     * 适配器的 type 整数 → FCL 的资源类别名。 */
    private static String typeName(int type) {
        try {
            return RemoteModRepository.Type.values()[type].name();
        } catch (Throwable ignored) {
            return "MOD";
        }
    }

    /**
     * ★★★ 1.5.0 用户要求：「首先显示中文，后面显示英文」。
     *
     * <p>原逻辑是二选一（查到中文就只显示中文），导致英文原名整个看不到。
     * 这里改成拼接：<b>中文名 + 两个空格 + 英文原名</b>。
     *
     * <p>规则：
     * <ul>
     *   <li>非中文系统 / 翻译表查不到 / 中文名与英文名相同 → 只显示英文原名（不重复、不加空格）；</li>
     *   <li>查到中文且与英文不同 → "中文名  English Name"。</li>
     * </ul>
     * ★ 资源包 / 世界 / 光影用的是 {@link ModTranslations#EMPTY}（没有翻译表），
     *   所以自动退化成只显示英文 —— 行为与以前一致，不会出现空名字。
     */
    private String displayTitleWithEn(RemoteMod mod, ModTranslations translations) {
        if (mod == null) {
            return "";
        }
        String en = mod.getTitle() == null ? "" : mod.getTitle().trim();
        if (!LocaleUtils.isChinese(context) || translations == null) {
            return en;
        }
        String cn = null;
        try {
            // ★★★ 1.5.0 修正（用户实测「下载里只翻译了描述，根本没翻译名称」）：
            //   原来**只查 CurseForge slug** 一条路。但下载页是**混合搜索**，混着 CurseForge 与
            //   Modrinth 的结果，两站 slug 互不相同 ⇒ 大量条目查不到中文 ⇒ 只显示英文原名。
            //   翻译表其实有三路索引（见 ModTranslations）：curseForgeMap(slug) / modIdMap(modIds)，
            //   外加名称本身可查。⇒ 现在按 slug → modId → 英文名 三路依次回退。
            ModTranslations.Mod t = translations.getModByCurseForgeId(mod.getSlug());
            if (t == null) {
                t = translations.getModById(mod.getSlug());
            }
            if (t == null && en.indexOf(' ') > 0) {
                // 有些站点的 slug 就是"名称 去空格"，用整名再试一次
                t = translations.getModByCurseForgeId(en.replace(" ", ""));
            }
            if (t != null && t.getDisplayName() != null) {
                cn = t.getDisplayName().trim();
            }
        } catch (Throwable ignored) {
        }
        if (cn == null || cn.isEmpty() || cn.equals(en)) {
            return en;
        }
        return cn + "  " + en;
    }

    @SuppressLint("HandlerLeak")
    public final Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
        }
    };
}
