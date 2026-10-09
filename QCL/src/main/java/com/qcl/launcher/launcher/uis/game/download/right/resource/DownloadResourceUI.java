package com.qcl.launcher.launcher.uis.game.download.right.resource;

import static com.qcl.launcher.launcher.mod.ModManager.getModWikiUrl;
import static com.qcl.launcher.launcher.mod.ModManager.getMcmodUrl;
import static com.qcl.launcher.utils.Lang.mapOf;
import static com.qcl.launcher.utils.Pair.pair;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
// ★ 1.4.1：模组简介一键翻译
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.qcl.launcher.R;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.dialogs.EditDownloadNameDialog;
import com.qcl.launcher.launcher.list.download.ModIconLoader;
import com.qcl.launcher.launcher.list.download.ModDependencyAdapter;
import com.qcl.launcher.launcher.list.download.ModGameVersionAdapter;
import com.qcl.launcher.launcher.mod.ModLoaderType;
import com.qcl.launcher.launcher.mod.RemoteMod;
import com.qcl.launcher.launcher.mod.RemoteModRepository;
import com.qcl.launcher.launcher.download.modloader.ModLoaderDetector;
import com.qcl.launcher.launcher.setting.SettingUtils;
import com.qcl.launcher.launcher.mod.curse.CurseForgeRemoteModRepository;
import com.qcl.launcher.utils.LocaleUtils;
import com.qcl.launcher.utils.SimpleMultimap;
import com.qcl.launcher.utils.file.UriUtils;
import com.qcl.launcher.utils.io.NetworkUtils;
// ★ 1.4.1：模组简介一键翻译（自 1.4.0 朋友源码包合并）
import com.qcl.launcher.utils.network.ModTranslateHelper;
import com.qcl.launcher.utils.string.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

public class DownloadResourceUI extends BaseDownloadUI implements View.OnClickListener {

    private ImageView icon;
    private TextView name;
    private TextView type;
    private TextView description;
    // ★ 1.4.1：翻译按钮
    private Button translateButton;
    private ModTranslateHelper.Toggle translateToggle;
    private LinearLayout mcmod;
    private LinearLayout modWiki;
    private LinearLayout curseForge;
    private LinearLayout modrinth;

    private ProgressBar progressBar;
    private LinearLayout dependencyLayout;
    private ListView dependencyList;
    private ListView versionList;

    private ModGameVersionAdapter modGameVersionAdapter;
    private ModDependencyAdapter modDependencyAdapter;

    public RemoteMod.Version selectedVersion;

    public static final int DOWNLOAD_RESOURCE_REQUEST = 2700;

    public DownloadResourceUI(Context context, MainActivity activity, RemoteModRepository repository, RemoteMod bean, int resourceType) {
        super(context, activity, repository, bean, resourceType);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        icon = findViewById(R.id.resource_icon);
        name = findViewById(R.id.resource_name);
        type = findViewById(R.id.resource_type);
        description = findViewById(R.id.resource_description);
        mcmod = findViewById(R.id.mcmod_link);
        modWiki = findViewById(R.id.mcmod_search_link);
        curseForge = findViewById(R.id.curse_forge_link);
        modrinth = findViewById(R.id.modrinth_link);

        mcmod.setOnClickListener(this);
        modWiki.setOnClickListener(this);
        curseForge.setOnClickListener(this);
        modrinth.setOnClickListener(this);

        progressBar = findViewById(R.id.mod_info_progress);
        dependencyLayout = findViewById(R.id.dependency_layout);
        dependencyList = findViewById(R.id.dependency_list);
        versionList = findViewById(R.id.mod_version_list);


        // ★ 1.2.9：详情页顶部的大图标也交给 ModIconLoader ——
        //   本来就是在子线程里下的（异步），但**没有超时也没有缓存**：服务器慢就一直挂着、
        //   每次进来都要重下一遍。现在跟列表图标共用一套：缓存命中秒开、拉不到就放弃。
        ModIconLoader.load(context, icon, bean.getIconUrl(), 0);
        name.setText(bean.getTitle());
        if (LocaleUtils.isChinese(context)) {
            name.setText(modTranslation != null ? modTranslation.getDisplayName() : bean.getTitle());
        }
        StringBuilder categories = new StringBuilder();
        for (String category : bean.getCategories()) {
            boolean isCurse = bean.getPageUrl() != null && bean.getPageUrl().contains("curseforge");
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
        type.setText(categories.toString());
        description.setText(bean.getDescription());
        // ★ 1.4.1：简介下方「一键翻译 / 显示原文」切换按钮
        translateButton = findViewById(R.id.resource_translate);
        translateToggle = new ModTranslateHelper.Toggle(context, description, translateButton);

        mcmod.setVisibility(resourceType == 0 ? View.VISIBLE : View.GONE);
        // Forum thread IDs are not download-mirror IDs. Hide the retired forum link.
        modWiki.setVisibility(View.GONE);
        curseForge.setVisibility((bean.getPageUrl() != null && bean.getPageUrl().contains("curseforge")) ? View.VISIBLE : View.GONE);
        modrinth.setVisibility((bean.getPageUrl() != null && !bean.getPageUrl().contains("curseforge")) ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (isFirst) {
            refresh();
            isFirst = false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == DOWNLOAD_RESOURCE_REQUEST && resultCode == Activity.RESULT_OK && data != null && selectedVersion != null) {
            Uri uri = data.getData();
            String dir = UriUtils.getRealPathFromUri_AboveApi19(context,uri);
            EditDownloadNameDialog dialog = new EditDownloadNameDialog(context, this, selectedVersion, false, dir);
            dialog.show();
        }
    }

    @Override
    public void onClick(View view) {
        if (view == mcmod) {
            Uri uri;
            if (modTranslation == null || StringUtils.isBlank(modTranslation.getMcmod())) {
                uri = Uri.parse(NetworkUtils.withQuery("https://search.mcmod.cn/s", mapOf(
                        pair("key", bean.getSlug()),
                        pair("site", "all"),
                        pair("filter", "0")
                )));
            }
            else {
                uri = Uri.parse(getMcmodUrl(modTranslation.getMcmod()));
            }
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            context.startActivity(intent);
        }
        if (view == modWiki) {
            Uri uri = Uri.parse(getModWikiUrl(modTranslation.getMcbbs()));
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            context.startActivity(intent);
        }
        if (view == curseForge || view == modrinth) {
            Uri uri = Uri.parse(bean.getPageUrl());
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            context.startActivity(intent);
        }
    }

    /**
     * ★ 1.2.5：「推荐的版本」分组的 key（没有就是 null）。
     * 抄 FCL RemoteModInfoPage.sortVersions —— 详情页最前面单独放一组
     * 「适配你现在玩的这个版本的」，点一下就能下载；这个模组没有适配你当前版本的
     * 版本时就不插这一组。
     */
    private String recommendedKey;

    public String getRecommendedKey() {
        return this.recommendedKey;
    }

    /**
     * ★ 1.2.5：这个模组的「前置」（依赖）。下载模组时会一起排队下载 ——
     * 以前只下本体，玩家点完还得自己回来一个个找前置。
     */
    private List<RemoteMod> dependencies = new ArrayList<>();

    public List<RemoteMod> getDependencies() {
        return this.dependencies;
    }

    /** ★ 1.2.5：依赖类型表（远程 id → required/optional/...），用来标「必需前置 / 可选前置」 */
    private java.util.Map<String, String> dependencyTypes = new java.util.HashMap<>();

    public java.util.Map<String, String> getDependencyTypes() {
        return this.dependencyTypes;
    }

    public RemoteModRepository getRepository() {
        return this.repository;
    }

    /** ★ 1.2.5：当前版本装的加载器名（用于「推荐的版本」标签与筛选），没有就 null */
    private String currentLoaderName() {
        try {
            String cur = activity.publicGameSetting.currentVersion;
            if (cur == null || cur.isEmpty()) {
                return null;
            }
            String loader = ModLoaderDetector.detect(new java.io.File(cur));
            if (ModLoaderDetector.MODLOADER.equals(loader)) return "ModLoader";
            if (ModLoaderDetector.BABRIC.equals(loader)) return "Babric";
            if (ModLoaderDetector.FABRIC.equals(loader)) return "Fabric";
            if (ModLoaderDetector.FORGE.equals(loader)) return "Forge";
            if (ModLoaderDetector.NEOFORGE.equals(loader)) return "NeoForge";
            if (ModLoaderDetector.QUILT.equals(loader)) return "Quilt";
            if (ModLoaderDetector.LITELOADER.equals(loader)) return "LiteLoader";
        } catch (Throwable ignored) {
        }
        return null;
    }

    /** ★ 1.2.5：当前加载器对应的 RemoteMod 加载器类型（远古 ModLoader 没有对应枚举，返回 null 表示不筛） */
    private ModLoaderType currentLoaderType() {
        String loader = currentLoaderName();
        if (loader == null) {
            return null;
        }
        if ("Fabric".equals(loader) || "Quilt".equals(loader) || "Babric".equals(loader)) {
            return ModLoaderType.FABRIC;
        }
        if ("Forge".equals(loader) || "NeoForge".equals(loader)) {
            return ModLoaderType.FORGE;
        }
        if ("LiteLoader".equals(loader)) {
            return ModLoaderType.LITE_LOADER;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private SimpleMultimap<String, RemoteMod.Version> sortVersions(Stream<RemoteMod.Version> versions) {
        SimpleMultimap<String, RemoteMod.Version> classifiedVersions = new SimpleMultimap<>(HashMap::new, ArrayList::new);
        versions.forEach(version -> {
            for (String gameVersion : version.getGameVersions()) {
                classifiedVersions.put(gameVersion, version);
            }
        });

        for (String gameVersion : classifiedVersions.keys()) {
            List<RemoteMod.Version> versionList = (List<RemoteMod.Version>) classifiedVersions.get(gameVersion);
            versionList.sort(Comparator.comparing(RemoteMod.Version::getDatePublished).reversed());
        }

        // ★★★ 1.2.5（照 FCL）：把「适配你当前游戏版本」的版本单独拎成一组，放最前面。
        //   ① 取你当前版本解析出的游戏版本号（Fabric/Forge 版也能拿到本体版本）；
        //   ② 这个模组/资源包/光影/世界/整合包支持这个版本 → 才建这一组；
        //      不支持就直接不建（列表保持原来的版本号倒序）。
        //   ③ 如果还认得出你装的加载器（Fabric/Forge/LiteLoader），
        //      优先只放加载器也匹配的那几个文件；一个都没匹配上就退回整组。
        this.recommendedKey = null;
        try {
            // ★ 2026-10-06（用户要求）：「推荐的版本」这一组**只给 模组(0) / 资源包(2) / 世界(3)**，
            //   整合包(1) 不要 —— 整合包是整包安装，不存在"适配你现在玩的这个版本"这种概念，
            //   给它插一组"推荐的版本"只会让人看不懂。
            String mcv = resourceType == 1 ? null : SettingUtils.getCurrentGameVersion(activity);
            if (mcv != null && !mcv.isEmpty() && classifiedVersions.keys().contains(mcv)) {
                List<RemoteMod.Version> matched =
                        new ArrayList<>((List<RemoteMod.Version>) classifiedVersions.get(mcv));
                ModLoaderType type = currentLoaderType();
                if (type != null) {
                    List<RemoteMod.Version> byLoader = new ArrayList<>();
                    for (RemoteMod.Version v : matched) {
                        if (v.getLoaders() != null && v.getLoaders().contains(type)) {
                            byLoader.add(v);
                        }
                    }
                    if (!byLoader.isEmpty()) {
                        matched = byLoader;
                    }
                }
                if (!matched.isEmpty()) {
                    String loaderName = currentLoaderName();
                    String key = "推荐的版本：" + mcv + (loaderName == null ? "" : " · " + loaderName);
                    for (RemoteMod.Version v : matched) {
                        classifiedVersions.put(key, v);
                    }
                    this.recommendedKey = key;
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return classifiedVersions;
    }

    public void refresh() {
        new Thread(() -> {
            activity.runOnUiThread(() -> {
                dependencyLayout.setVisibility(View.GONE);
                versionList.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
            });
            try {
                // ★★★ 1.2.5：依赖拉不到**不该**让整页「版本列表加载失败」——
                //   依赖单独兜一层，失败就当没有依赖，版本列表照常显示。
                List<RemoteMod> deps;
                // ★★★ 2026-10-06 修复（用户实测）：前置（依赖）**只在「模组」详情页显示**。
                //   这个详情页是 模组 / 整合包 / 资源包 / 世界 **共用**的，原来无条件拉依赖，
                //   于是整合包详情页也冒出一段「模组前置」—— 玩家点的是整合包，看到"模组前置"
                //   只会困惑（用户原话：「你加在整合包里有个屁用」）。
                //   resourceType == 0 才是模组（与上面 mcmod 的可见性判断同一口径）。
                // ★★★ 2026-10-06 修正（用户指出）：**模组(0) 和 资源包(2) 都要显示前置** ——
                //   很多资源包本身依赖模组（Fabric API / OptiFine / Continuity 之类），
                //   只给模组显示反而不够。真正不该显示前置的是：
                //     · 整合包(1)：整包安装，它的"依赖"是加载器，不是一个个前置模组
                //     · 世界(3)  ：跟模组依赖无关
                // ★★★★★ 2026-10-11 提速（用户实测「点击模组详情页/整合包详情页要加载半天」）：
                //
                //   原来这里是**串行**的：先 `loadDependencies()`（有几个前置就发几次 API 请求），
                //   等它全部跑完，才轮到 `loadVersions()` —— 而"版本列表"才是玩家真正要等的东西。
                //   前置多的模组（或网络慢时）就要等十几秒，页面一直只有转圈
                //   ⇒ 看着就是"加载很久都还没加载完"。
                //
                //   现在把**依赖**挪到独立线程里并行拉（它只影响下面那一段"模组前置"区域，
                //   拉到了再单独刷那一块），主线程立刻去拉版本列表并**尽快显示**。
                //   依赖失败依旧当"没有依赖"处理，绝不影响版本列表。
                final boolean needDeps = (resourceType == 0 || resourceType == 2);
                if (needDeps) {
                    new Thread(() -> {
                        List<RemoteMod> depsAsync;
                        try {
                            depsAsync = bean.getData().loadDependencies(repository);
                        } catch (Throwable t) {
                            depsAsync = new ArrayList<>();
                        }
                        final List<RemoteMod> ready = (depsAsync == null)
                                ? new ArrayList<>() : depsAsync;
                        activity.runOnUiThread(() -> {
                            try {
                                this.dependencies = new ArrayList<>(ready);
                                // 依赖到位后单独刷这一块（版本列表已经在显示了，不受影响）
                                if (!ready.isEmpty()) {
                                    modDependencyAdapter = new ModDependencyAdapter(context, activity,
                                            repository, this.dependencies, this.dependencyTypes);
                                    dependencyList.setAdapter(modDependencyAdapter);
                                    dependencyLayout.setVisibility(View.VISIBLE);
                                    reSetListViewHeight(dependencyList,
                                            getDependencyListHeight(dependencyList)
                                                    - dependencyList.getLayoutParams().height);
                                }
                            } catch (Throwable ignored) {
                                // 依赖区域刷不出来也不影响已经显示的版本列表
                            }
                        });
                    }, "qcl-deps").start();
                }
                // ★ 注意：这里**不要**给 this.dependencies 赋值 ——
                //   上面那个异步线程拿到结果后会自己赋值并刷新依赖区域。
                //   主流程只负责"版本列表"，两件事互不阻塞。
                SimpleMultimap<String, RemoteMod.Version> versions = sortVersions(bean.getData().loadVersions(repository));
                // ★ 1.2.9：依赖类型（必需/可选）**直接从已经取回来的版本列表里建表**。
                //   1.2.7 时这一步是单独发一次请求（把整个版本列表又拉一遍），
                //   详情页「加载半天」有一截就是这个多出来的请求造成的 —— 现在等于白拿，零额外请求。
                try {
                    java.util.Map<String, String> types = new java.util.HashMap<>();
                    for (String key : versions.keys()) {
                        java.util.List<RemoteMod.Version> vs = (java.util.List<RemoteMod.Version>) versions.get(key);
                        if (vs == null) {
                            continue;
                        }
                        for (RemoteMod.Version v : vs) {
                            java.util.Map<String, String> m = v.getDependencyTypes();
                            if (m == null) {
                                continue;
                            }
                            for (java.util.Map.Entry<String, String> e : m.entrySet()) {
                                String oldT = types.get(e.getKey());
                                if (oldT == null || "required".equals(e.getValue())) {
                                    types.put(e.getKey(), e.getValue());
                                }
                            }
                        }
                    }
                    if (!types.isEmpty()) {
                        this.dependencyTypes = types;
                    }
                }
                catch (Throwable ignored) {
                }
                // ★★★★★ 2026-10-11：版本列表**立刻显示**，不再等依赖。
                //   依赖是否为空由上面那个异步线程决定 ——
                //   它拉到东西才把"模组前置"那块显示出来，拉不到/为空就保持隐藏。
                //   这里先把依赖区域收起来、把版本列表亮出来、收掉转圈。
                modGameVersionAdapter = new ModGameVersionAdapter(context, versions, this);
                activity.runOnUiThread(() -> {
                    dependencyLayout.setVisibility(View.GONE);
                    versionList.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    versionList.setAdapter(modGameVersionAdapter);
                    reSetListViewHeight(versionList,
                            getVersionListHeight(versionList) - versionList.getLayoutParams().height);
                });
            } catch (Exception e) {
                e.printStackTrace();
                activity.runOnUiThread(() -> {
                    dependencyLayout.setVisibility(View.GONE);
                    versionList.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    public void refreshVersionListHeight(int change) {
        reSetListViewHeight(versionList,change);
    }

    public static int getVersionListHeight(ListView listView) {
        int count = listView.getAdapter().getCount();
        View view = listView.getAdapter().getView(0,null,listView);
        view.measure(0, 0);
        return (view.getMeasuredHeight() * count) + (listView.getDividerHeight() * (count - 1));
    }

    public static int getDependencyListHeight(ListView listView) {
        int count = listView.getAdapter().getCount();
        View view = listView.getAdapter().getView(0,null,listView);
        view.measure(0, 0);
        return (view.getMeasuredHeight() * count) + (listView.getDividerHeight() * (count - 1));
    }

    public static void reSetListViewHeight(ListView listView,int change) {
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height += change;
        listView.setLayoutParams(params);
    }
}
