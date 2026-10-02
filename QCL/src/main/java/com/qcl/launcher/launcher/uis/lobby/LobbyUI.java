package com.qcl.launcher.launcher.uis.lobby;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qcl.launcher.R;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.list.download.ModIconLoader;
import com.qcl.launcher.launcher.mod.modrinth.ModrinthRemoteModRepository;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;
import com.qcl.launcher.utils.file.AssetsUtils;
import com.qcl.launcher.utils.gson.JsonUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 大厅页面：我的世界新闻 + 新发布的模组 + 版本日历。
 *
 * <p>网络请求一律放在子线程，回调通过 {@link MainActivity#runOnUiThread} 回到主线程刷新界面；
 * 图标走项目现成的 {@link ModIconLoader}（内存 + 磁盘缓存、镜像回退），不重复造。
 */
public class LobbyUI extends BaseUI implements View.OnClickListener {

    private static final String USER_AGENT = "QCL/1.4.0";
    private static final String NEWS_URL = "https://launchercontent.mojang.com/v2/news.json";
    private static final String MODRINTH_SEARCH = "https://api.modrinth.com/v2/search";
    /** 官方新闻图片是相对路径，需要补齐域名。 */
    private static final String NEWS_IMAGE_BASE = "https://launchercontent.mojang.com";
    private static final String ARCHIVE_ASSET = "legacy_version_archive.json";
    private static final int NEWS_MAX = 12;
    private static final int MODS_MAX = 20;

    private static final String[] WEEK_LABELS = {"一", "二", "三", "四", "五", "六", "日"};

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    private LinearLayout rootView;

    // A. 新闻
    private TextView newsStatus;
    private LinearLayout newsContainer;
    private TextView newsRetry;

    // B. 模组
    private TextView modsStatus;
    private LinearLayout modsContainer;
    private TextView modsRetry;

    // C. 日历
    private TextView calendarMonth;
    private LinearLayout calendarWeek;
    private LinearLayout calendarGrid;
    private TextView calendarSelected;
    private LinearLayout calendarEvents;
    private TextView calendarNote;

    private boolean loaded;

    private final Map<Long, List<ArchiveEntry>> events = new HashMap<>();
    private final List<ArchiveEntry> noDateEntries = new ArrayList<>();
    private int calYear;
    private int calMonth0;
    private int selectedDay = 1;

    public LobbyUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onCreate() {
        super.onCreate();
        this.rootView = this.activity.findViewById(R.id.ui_lobby);

        this.newsStatus = this.activity.findViewById(R.id.lobby_news_status);
        this.newsContainer = this.activity.findViewById(R.id.lobby_news_container);
        this.newsRetry = this.activity.findViewById(R.id.lobby_news_retry);

        this.modsStatus = this.activity.findViewById(R.id.lobby_mods_status);
        this.modsContainer = this.activity.findViewById(R.id.lobby_mods_container);
        this.modsRetry = this.activity.findViewById(R.id.lobby_mods_retry);

        this.calendarMonth = this.activity.findViewById(R.id.lobby_calendar_month);
        this.calendarWeek = this.activity.findViewById(R.id.lobby_calendar_week);
        this.calendarGrid = this.activity.findViewById(R.id.lobby_calendar_grid);
        this.calendarSelected = this.activity.findViewById(R.id.lobby_calendar_selected);
        this.calendarEvents = this.activity.findViewById(R.id.lobby_calendar_events);
        this.calendarNote = this.activity.findViewById(R.id.lobby_calendar_note);

        this.newsRetry.setOnClickListener(this);
        this.modsRetry.setOnClickListener(this);
        this.activity.findViewById(R.id.lobby_calendar_prev).setOnClickListener(this);
        this.activity.findViewById(R.id.lobby_calendar_next).setOnClickListener(this);
        this.activity.findViewById(R.id.lobby_calendar_today_view).setOnClickListener(this);

        buildWeekHeader();
        loadArchive();
        Calendar today = Calendar.getInstance();
        this.calYear = today.get(Calendar.YEAR);
        this.calMonth0 = today.get(Calendar.MONTH);
        this.selectedDay = today.get(Calendar.DAY_OF_MONTH);
        renderCalendar();
        updateEvents();
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStart() {
        super.onStart();
        this.activity.showBarTitle(this.context.getResources().getString(R.string.lobby_title), canGoBackToLast(), true);
        CustomAnimationUtils.showViewFromLeft(this.rootView, this.activity, this.context, true);
        if (!this.loaded) {
            this.loaded = true;
            loadNews();
            loadMods();
        }
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.rootView, this.activity, this.context, true);
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.lobby_news_retry) {
            loadNews();
        } else if (id == R.id.lobby_mods_retry) {
            loadMods();
        } else if (id == R.id.lobby_calendar_prev) {
            if (this.calMonth0 == 0) {
                this.calMonth0 = 11;
                this.calYear--;
            } else {
                this.calMonth0--;
            }
            renderCalendar();
        } else if (id == R.id.lobby_calendar_next) {
            if (this.calMonth0 == 11) {
                this.calMonth0 = 0;
                this.calYear++;
            } else {
                this.calMonth0++;
            }
            renderCalendar();
        } else if (id == R.id.lobby_calendar_today_view) {
            Calendar today = Calendar.getInstance();
            this.calYear = today.get(Calendar.YEAR);
            this.calMonth0 = today.get(Calendar.MONTH);
            this.selectedDay = today.get(Calendar.DAY_OF_MONTH);
            renderCalendar();
            updateEvents();
        }
    }

    // ============================================================
    //                         A. 我的世界新闻
    // ============================================================

    private void loadNews() {
        this.newsStatus.setVisibility(View.VISIBLE);
        this.newsStatus.setText(R.string.lobby_news_loading);
        this.newsRetry.setVisibility(View.GONE);
        this.newsContainer.removeAllViews();
        request(NEWS_URL, new NetCallback() {
            @Override
            public void onResult(boolean ok, String body) {
                onNewsLoaded(ok, body);
            }
        });
    }

    private void onNewsLoaded(boolean ok, String body) {
        NewsRoot root = null;
        if (ok && body != null) {
            try {
                root = new Gson().fromJson(body, NewsRoot.class);
            } catch (Throwable ignored) {
            }
        }
        if (root == null || root.entries == null || root.entries.isEmpty()) {
            this.newsContainer.removeAllViews();
            this.newsStatus.setVisibility(View.VISIBLE);
            this.newsStatus.setText(ok ? R.string.lobby_news_empty : R.string.lobby_news_failed);
            this.newsRetry.setVisibility(View.VISIBLE);
            return;
        }
        this.newsStatus.setVisibility(View.GONE);
        this.newsRetry.setVisibility(View.GONE);
        this.newsContainer.removeAllViews();
        int shown = 0;
        for (NewsEntry entry : root.entries) {
            if (entry == null) {
                continue;
            }
            if (shown >= NEWS_MAX) {
                break;
            }
            addNewsItem(entry, shown);
            shown++;
        }
    }

    private void addNewsItem(final NewsEntry entry, int position) {
        LinearLayout item = new LinearLayout(this.context);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(dp(6), dp(6), dp(6), dp(6));
        item.setBackgroundResource(R.drawable.qcl_button_gray);
        item.setClickable(true);
        item.setFocusable(true);
        LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        itemLp.topMargin = dp(6);
        item.setLayoutParams(itemLp);

        ImageView thumb = new ImageView(this.context);
        thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
        thumb.setBackgroundResource(R.drawable.launcher_view_white);
        LinearLayout.LayoutParams thumbLp = new LinearLayout.LayoutParams(dp(84), dp(56));
        thumb.setLayoutParams(thumbLp);

        LinearLayout column = new LinearLayout(this.context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(8), 0, 0, 0);
        column.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = new TextView(this.context);
        title.setText(entry.title == null ? "" : entry.title);
        title.setTextSize(14);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setTextColor(colorBlack());

        TextView date = new TextView(this.context);
        date.setText(entry.date == null ? "" : entry.date);
        date.setTextSize(11);
        date.setTextColor(colorAccent());

        TextView summary = new TextView(this.context);
        summary.setText(entry.text == null ? "" : entry.text.trim());
        summary.setTextSize(12);
        summary.setMaxLines(3);
        summary.setEllipsize(TextUtils.TruncateAt.END);
        summary.setTextColor(colorBlack());

        column.addView(title);
        column.addView(date);
        column.addView(summary);

        item.addView(thumb);
        item.addView(column);
        item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openUrl(entry.readMoreLink);
            }
        });

        this.newsContainer.addView(item);
        ModIconLoader.load(this.context, thumb, newsImageUrl(entry), position);
    }

    private String newsImageUrl(NewsEntry entry) {
        NewsImage image = entry.newsPageImage != null ? entry.newsPageImage : entry.playPageImage;
        if (image == null || image.url == null || image.url.trim().isEmpty()) {
            return null;
        }
        String url = image.url.trim();
        if (url.startsWith("http")) {
            return url;
        }
        return NEWS_IMAGE_BASE + url;
    }

    // ============================================================
    //                         B. 新发布的模组
    // ============================================================

    private void loadMods() {
        this.modsStatus.setVisibility(View.VISIBLE);
        this.modsStatus.setText(R.string.lobby_mods_loading);
        this.modsRetry.setVisibility(View.GONE);
        this.modsContainer.removeAllViews();

        HttpUrl url = HttpUrl.parse(MODRINTH_SEARCH).newBuilder()
                .addQueryParameter("limit", Integer.toString(MODS_MAX))
                .addQueryParameter("index", "newest")
                .addQueryParameter("facets", "[[\"project_type:mod\"]]")
                .build();
        request(url.toString(), new NetCallback() {
            @Override
            public void onResult(boolean ok, String body) {
                onModsLoaded(ok, body);
            }
        });
    }

    private void onModsLoaded(boolean ok, String body) {
        List<ModrinthRemoteModRepository.ProjectSearchResult> hits = null;
        if (ok && body != null) {
            try {
                Type type = new TypeToken<ModrinthRemoteModRepository.Response<ModrinthRemoteModRepository.ProjectSearchResult>>() {
                }.getType();
                ModrinthRemoteModRepository.Response<ModrinthRemoteModRepository.ProjectSearchResult> response =
                        JsonUtils.fromNonNullJson(body, type);
                if (response != null) {
                    hits = response.getHits();
                }
            } catch (Throwable ignored) {
            }
        }
        if (hits == null || hits.isEmpty()) {
            this.modsContainer.removeAllViews();
            this.modsStatus.setVisibility(View.VISIBLE);
            this.modsStatus.setText(ok ? R.string.lobby_mods_empty : R.string.lobby_mods_failed);
            this.modsRetry.setVisibility(View.VISIBLE);
            return;
        }
        this.modsStatus.setVisibility(View.GONE);
        this.modsRetry.setVisibility(View.GONE);
        this.modsContainer.removeAllViews();
        int shown = 0;
        for (ModrinthRemoteModRepository.ProjectSearchResult hit : hits) {
            if (hit == null) {
                continue;
            }
            if (shown >= MODS_MAX) {
                break;
            }
            addModItem(hit, shown);
            shown++;
        }
    }

    private void addModItem(final ModrinthRemoteModRepository.ProjectSearchResult hit, int position) {
        LinearLayout item = new LinearLayout(this.context);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(dp(6), dp(6), dp(6), dp(6));
        item.setBackgroundResource(R.drawable.qcl_button_gray);
        item.setClickable(true);
        item.setFocusable(true);
        LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        itemLp.topMargin = dp(6);
        item.setLayoutParams(itemLp);

        ImageView icon = new ImageView(this.context);
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        icon.setBackgroundResource(R.drawable.launcher_view_white);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(40), dp(40));
        icon.setLayoutParams(iconLp);

        LinearLayout column = new LinearLayout(this.context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(8), 0, 0, 0);
        column.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView name = new TextView(this.context);
        name.setText(hit.getTitle() == null ? "" : hit.getTitle());
        name.setTextSize(14);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        name.setTextColor(colorBlack());

        TextView versions = new TextView(this.context);
        versions.setText(this.context.getString(R.string.lobby_mods_versions, joinVersions(hit.getVersions())));
        versions.setTextSize(11);
        versions.setSingleLine(true);
        versions.setEllipsize(TextUtils.TruncateAt.END);
        versions.setTextColor(colorAccent());

        TextView downloads = new TextView(this.context);
        downloads.setText(this.context.getString(R.string.lobby_mods_downloads,
                String.format(Locale.US, "%,d", hit.getDownloads())));
        downloads.setTextSize(11);
        downloads.setSingleLine(true);
        downloads.setTextColor(colorAccent());

        TextView description = new TextView(this.context);
        description.setText(hit.getDescription() == null ? "" : hit.getDescription().trim());
        description.setTextSize(12);
        description.setMaxLines(2);
        description.setEllipsize(TextUtils.TruncateAt.END);
        description.setTextColor(colorBlack());

        column.addView(name);
        column.addView(versions);
        column.addView(downloads);
        column.addView(description);

        item.addView(icon);
        item.addView(column);
        item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openUrl("https://modrinth.com/mod/" + hit.getSlug());
            }
        });

        this.modsContainer.addView(item);
        ModIconLoader.load(this.context, icon, hit.getIconUrl(), position);
    }

    private String joinVersions(List<String> versions) {
        if (versions == null || versions.isEmpty()) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String version : versions) {
            if (version == null || version.trim().isEmpty()) {
                continue;
            }
            if (count >= 5) {
                sb.append(" …");
                break;
            }
            if (count > 0) {
                sb.append(", ");
            }
            sb.append(version.trim());
            count++;
        }
        return sb.length() == 0 ? "-" : sb.toString();
    }

    // ============================================================
    //                         C. 版本日历
    // ============================================================

    private void buildWeekHeader() {
        this.calendarWeek.removeAllViews();
        for (String label : WEEK_LABELS) {
            TextView cell = new TextView(this.context);
            cell.setText(label);
            cell.setTextSize(12);
            cell.setGravity(Gravity.CENTER);
            cell.setTextColor(colorAccent());
            cell.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            this.calendarWeek.addView(cell);
        }
    }

    private void loadArchive() {
        try {
            String raw = AssetsUtils.readAssetsTxt(this.context, ARCHIVE_ASSET);
            if (raw == null || raw.trim().isEmpty()) {
                return;
            }
            ArchiveRoot root = new Gson().fromJson(raw, ArchiveRoot.class);
            if (root == null || root.entries == null) {
                return;
            }
            for (ArchiveEntry entry : root.entries) {
                if (entry == null || entry.id == null || entry.id.trim().isEmpty()) {
                    continue;
                }
                long stamp = entry.sortTime > 0 ? entry.sortTime
                        : (entry.compileTime > 0 ? entry.compileTime : entry.releaseTime);
                if (stamp <= 0) {
                    this.noDateEntries.add(entry);
                    continue;
                }
                // 归档时间戳是 UTC，按 UTC 取年月日，避免时区把日期挪到前后一天。
                Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                c.setTimeInMillis(stamp);
                long key = keyOf(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1,
                        c.get(Calendar.DAY_OF_MONTH));
                List<ArchiveEntry> list = this.events.get(key);
                if (list == null) {
                    list = new ArrayList<>();
                    this.events.put(key, list);
                }
                list.add(entry);
            }
        } catch (Throwable ignored) {
            // 归档数据缺失不应该影响页面其它部分。
        }
        if (this.noDateEntries.isEmpty()) {
            this.calendarNote.setVisibility(View.GONE);
        } else {
            StringBuilder sb = new StringBuilder();
            for (ArchiveEntry entry : this.noDateEntries) {
                if (sb.length() > 0) {
                    sb.append("、");
                }
                sb.append(TextUtils.isEmpty(entry.otherName) ? entry.id : entry.otherName + "(" + entry.id + ")");
            }
            this.calendarNote.setVisibility(View.VISIBLE);
            this.calendarNote.setText(this.context.getString(R.string.lobby_calendar_no_date, sb.toString()));
        }
    }

    private void renderCalendar() {
        this.calendarMonth.setText(String.format(Locale.US, "%d年%d月", this.calYear, this.calMonth0 + 1));
        this.calendarGrid.removeAllViews();

        Calendar first = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        first.clear();
        first.set(this.calYear, this.calMonth0, 1);
        // Calendar.DAY_OF_WEEK：周日=1…周六=7；换算成「周一为第一列」的前置空格数。
        int lead = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        int daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH);

        int day = 1;
        for (int row = 0; row < 6; row++) {
            LinearLayout rowView = new LinearLayout(this.context);
            rowView.setOrientation(LinearLayout.HORIZONTAL);
            rowView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            for (int column = 0; column < 7; column++) {
                int cellIndex = row * 7 + column;
                TextView cell = new TextView(this.context);
                cell.setTextSize(12);
                cell.setGravity(Gravity.CENTER);
                cell.setTextColor(colorBlack());
                cell.setLayoutParams(new LinearLayout.LayoutParams(0, dp(40), 1f));
                if (cellIndex >= lead && day <= daysInMonth) {
                    final int dayOfMonth = day;
                    boolean hasEvent = this.events.containsKey(keyOf(this.calYear, this.calMonth0 + 1, dayOfMonth));
                    cell.setText(hasEvent ? dayOfMonth + "\n•" : String.valueOf(dayOfMonth));
                    if (dayOfMonth == this.selectedDay) {
                        cell.setBackgroundResource(R.drawable.launcher_button_transparent_blue);
                    } else {
                        cell.setBackgroundResource(R.drawable.qcl_button_gray);
                    }
                    cell.setClickable(true);
                    cell.setFocusable(true);
                    cell.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            LobbyUI.this.selectedDay = dayOfMonth;
                            renderCalendar();
                            updateEvents();
                        }
                    });
                    day++;
                } else {
                    cell.setText("");
                }
                rowView.addView(cell);
            }
            this.calendarGrid.addView(rowView);
        }
    }

    private void updateEvents() {
        this.calendarSelected.setText(String.format(Locale.US, "%d年%d月%d日",
                this.calYear, this.calMonth0 + 1, this.selectedDay));
        this.calendarEvents.removeAllViews();

        List<ArchiveEntry> list = this.events.get(keyOf(this.calYear, this.calMonth0 + 1, this.selectedDay));
        if (list == null || list.isEmpty()) {
            TextView empty = new TextView(this.context);
            empty.setText(R.string.lobby_calendar_no_event);
            empty.setTextSize(12);
            empty.setTextColor(colorAccent());
            this.calendarEvents.addView(empty);
            return;
        }
        for (ArchiveEntry entry : list) {
            LinearLayout row = new LinearLayout(this.context);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(6), dp(4), dp(6), dp(4));
            row.setBackgroundResource(R.drawable.qcl_button_gray);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowLp.topMargin = dp(4);
            row.setLayoutParams(rowLp);

            TextView name = new TextView(this.context);
            String label = TextUtils.isEmpty(entry.otherName) ? entry.id : entry.id + " / " + entry.otherName;
            name.setText(label);
            name.setTextSize(13);
            name.setTextColor(colorBlack());

            TextView type = new TextView(this.context);
            type.setText(this.context.getString(R.string.lobby_type_prefix, categoryLabel(entry.category)));
            type.setTextSize(11);
            type.setTextColor(colorAccent());

            row.addView(name);
            row.addView(type);
            this.calendarEvents.addView(row);
        }
    }

    private String categoryLabel(String category) {
        if (category == null) {
            return "-";
        }
        switch (category) {
            case "beta/pre-release":
                return "beta";
            case "release-candidate/variant":
                return "RC";
            default:
                return category;
        }
    }

    private static long keyOf(int year, int month, int day) {
        return year * 10000L + month * 100L + day;
    }

    // ============================================================
    //                         通用工具
    // ============================================================

    private interface NetCallback {
        void onResult(boolean ok, String body);
    }

    private void request(final String url, final NetCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String body = null;
                boolean ok = false;
                try {
                    Request request = new Request.Builder()
                            .url(url)
                            .header("User-Agent", USER_AGENT)
                            .build();
                    Response response = client.newCall(request).execute();
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            body = response.body().string();
                            ok = true;
                        }
                    } finally {
                        response.close();
                    }
                } catch (Throwable ignored) {
                }
                final String result = body;
                final boolean success = ok;
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResult(success, result);
                    }
                });
            }
        }).start();
    }

    private void openUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url.trim()));
            this.activity.startActivity(intent);
        } catch (Throwable ignored) {
            Toast.makeText(this.context, R.string.lobby_open_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(float value) {
        return (int) (this.context.getResources().getDisplayMetrics().density * value + 0.5f);
    }

    private int colorBlack() {
        return this.context.getResources().getColor(R.color.colorPureBlack);
    }

    private int colorAccent() {
        return this.context.getResources().getColor(R.color.colorAccent);
    }

    // ============================================================
    //                        JSON 数据模型
    // ============================================================

    /** news.json 根：{"version":1,"entries":[...]} */
    private static class NewsRoot {
        private List<NewsEntry> entries;
    }

    /** 一条新闻：标题、日期(yyyy-MM-dd)、摘要、配图、跳转链接。 */
    private static class NewsEntry {
        private String title;
        private String date;
        private String text;
        private String readMoreLink;
        private NewsImage newsPageImage;
        private NewsImage playPageImage;
    }

    private static class NewsImage {
        private String url;
    }

    /** legacy_version_archive.json 根：{"source":..,"entries":[...]} */
    private static class ArchiveRoot {
        private List<ArchiveEntry> entries;
    }

    /** 归档条目：id、别名、时间戳、类别（pre-classic/classic/indev/infdev/alpha/beta/RC）。 */
    private static class ArchiveEntry {
        private String id;
        private String otherName;
        private long releaseTime;
        private long compileTime;
        private long sortTime;
        private String category;
    }
}