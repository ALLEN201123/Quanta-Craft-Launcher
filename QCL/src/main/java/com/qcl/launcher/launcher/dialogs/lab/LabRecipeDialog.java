package com.qcl.launcher.launcher.dialogs.lab;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.qcl.launcher.R;
import com.qcl.launcher.launcher.MainActivity;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 物品配方查询：内置常见物品配方表，支持按中文名搜索，展示 3x3 合成网格与熔炼信息。
 */
public class LabRecipeDialog extends Dialog {

    /** 配方数据模型，字段与 lab_recipes.json 对应。 */
    public static class Recipe {
        public String name;
        public String id;
        public int count;
        public String type;
        public List<String> grid;
        public List<String> materials;
        public String smelting;
    }

    private static class RecipeFile {
        List<Recipe> recipes;
    }

    private final List<Recipe> allRecipes = new ArrayList<>();
    /** ★ 1.4.3：直接持有 MainActivity（不要用 getContext() 转，Dialog 的主题包装会让 instanceof 落空）。 */
    private final MainActivity activity;
    private EditText searchInput;
    private LinearLayout resultsContainer;
    private LinearLayout detail;
    private TextView resultText;
    private LinearLayout gridBlock;
    private LinearLayout gridContainer;
    private LinearLayout materialsBlock;
    private TextView materialsText;
    private LinearLayout smeltingBlock;
    private TextView smeltingText;
    private Recipe current;

    public LabRecipeDialog(MainActivity activity) {
        super(activity);
        this.activity = activity;
        setContentView(R.layout.dialog_lab_recipe);
        loadRecipes();
        init();
        LabUtils.setupDialogWindow(this);
    }

    private void loadRecipes() {
        // ① 先加载内置静态表：作为**兜底**，并且它的中文名更贴合习惯（会用来覆盖同名条目）
        try (InputStreamReader reader = new InputStreamReader(
                getContext().getAssets().open("lab_recipes.json"), StandardCharsets.UTF_8)) {
            RecipeFile file = new Gson().fromJson(reader, RecipeFile.class);
            if (file != null && file.recipes != null) {
                allRecipes.addAll(file.recipes);
            }
        } catch (Throwable e) {
            LabUtils.toast(getContext(), "配方数据加载失败");
        }
        // ② ★★★ 1.4.3：再用当前版本 jar 里的**全量原版配方**覆盖（实测 1.20.6 有 1175 条，
        //   而内置静态表只有 121 条 —— 这就是用户说的「漏了很多东西」）。
        try {
            loadRecipesFromJar();
        } catch (Throwable ignored) {
            // 读不到就继续用静态表，绝不让对话框打不开
        }
    }

    /** 从当前版本客户端 jar 里读出全量原版配方；中文名从该版本的 assets 语言文件取。 */
    private void loadRecipesFromJar() throws java.io.IOException {
        MainActivity act = this.activity;
        if (act == null || act.publicGameSetting == null) {
            return;
        }
        LabUtils.VersionPackInfo info = LabUtils.readVersionPackInfo(act);
        if (!info.ok) {
            return;
        }
        File versionDir = new File(act.publicGameSetting.currentVersion);
        File jar = jarOf(versionDir, info.versionName);
        if (jar == null) {
            return;
        }
        java.util.Map<String, String> zh = loadChineseNames(act, versionDir);
        java.util.LinkedHashMap<String, Recipe> fromJar = new java.util.LinkedHashMap<>();
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(jar)) {
            // ★ 目录名 1.21 起是单数 recipe/，更早是 recipes/ —— 由 jar 探测得到，不写死
            String prefix = "data/minecraft/" + info.recipeDir + "/";
            java.util.Enumeration<? extends java.util.zip.ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                java.util.zip.ZipEntry entry = en.nextElement();
                String name = entry.getName();
                if (!name.startsWith(prefix) || !name.endsWith(".json") || entry.isDirectory()) {
                    continue;
                }
                Recipe recipe = parseRecipe(readAll(zip.getInputStream(entry)), zh);
                if (recipe != null && recipe.id != null && !recipe.id.isEmpty() && !fromJar.containsKey(recipe.id)) {
                    fromJar.put(recipe.id, recipe);
                }
            }
        }
        if (fromJar.isEmpty()) {
            return;
        }
        // 静态表里的中文名/展示更贴合习惯 → 只覆盖「展示相关」字段
        java.util.HashMap<String, Recipe> staticById = new java.util.HashMap<>();
        for (Recipe r : allRecipes) {
            if (r.id != null) {
                staticById.put(r.id, r);
            }
        }
        for (Recipe r : fromJar.values()) {
            Recipe old = staticById.get(r.id);
            if (old == null) {
                continue;
            }
            if (old.name != null && !old.name.isEmpty()) {
                r.name = old.name;
            }
            if (old.grid != null) {
                r.grid = old.grid;
            }
            if (old.materials != null) {
                r.materials = old.materials;
            }
            r.smelting = old.smelting;
        }
        allRecipes.clear();
        allRecipes.addAll(fromJar.values());
    }

    /** 把一条配方 json 映射成界面用的模型（中文名优先）。 */
    private Recipe parseRecipe(String json, java.util.Map<String, String> zh) {
        try {
            com.google.gson.JsonObject o = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            String type = str(o, "type");
            if (type == null) {
                return null;
            }
            int colon = type.indexOf(':');
            if (colon >= 0) {
                type = type.substring(colon + 1);
            }
            com.google.gson.JsonObject result = o.has("result") && o.get("result").isJsonObject()
                    ? o.getAsJsonObject("result") : null;
            String rid = result != null ? str(result, "id") : null;
            if (rid == null) {
                rid = str(o, "result");   // 极老格式可能是字符串
            }
            if (rid == null) {
                return null;
            }
            Recipe r = new Recipe();
            r.id = rid;
            r.name = displayName(rid, zh);
            r.count = result != null && result.has("count") ? result.get("count").getAsInt() : 1;
            r.type = type;
            java.util.List<String> mats = new java.util.ArrayList<>();
            if ("crafting_shaped".equals(type)) {
                r.grid = shapedGrid(o, zh, mats);
            } else if ("crafting_shapeless".equals(type)) {
                r.grid = shapelessGrid(o, zh, mats);
            } else {
                String ing = ingredientName(o.has("ingredient") ? o.get("ingredient") : null, zh);
                if (ing != null) {
                    mats.add(ing);
                }
            }
            if (r.grid == null) {
                r.grid = new java.util.ArrayList<>();
            }
            // materials 用「名字 x数量」的形式统计
            java.util.LinkedHashMap<String, Integer> count = new java.util.LinkedHashMap<>();
            for (String m : mats) {
                if (m == null) {
                    continue;
                }
                Integer c = count.get(m);
                count.put(m, c == null ? 1 : c + 1);
            }
            java.util.List<String> matList = new java.util.ArrayList<>();
            for (java.util.Map.Entry<String, Integer> e : count.entrySet()) {
                matList.add(e.getValue() > 1 ? e.getKey() + " x" + e.getValue() : e.getKey());
            }
            r.materials = matList;
            return r;
        } catch (Throwable t) {
            return null;
        }
    }

    /** 有序合成：pattern + key → 3x3 网格。 */
    private java.util.List<String> shapedGrid(com.google.gson.JsonObject o, java.util.Map<String, String> zh,
                                             java.util.List<String> mats) {
        java.util.List<String> grid = new java.util.ArrayList<>();
        com.google.gson.JsonObject key = o.has("key") ? o.getAsJsonObject("key") : null;
        java.util.List<String> pattern = new java.util.ArrayList<>();
        if (o.has("pattern") && o.get("pattern").isJsonArray()) {
            for (com.google.gson.JsonElement e : o.getAsJsonArray("pattern")) {
                pattern.add(e.getAsString());
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                String cell = null;
                if (row < pattern.size() && col < pattern.get(row).length()) {
                    char c = pattern.get(row).charAt(col);
                    if (c != ' ') {
                        cell = key != null && key.has(String.valueOf(c))
                                ? ingredientName(key.get(String.valueOf(c)), zh) : String.valueOf(c);
                    }
                }
                grid.add(cell);
                if (cell != null) {
                    mats.add(cell);
                }
            }
        }
        return grid;
    }

    /** 无序合成：ingredients → 依次填入 3x3 网格。 */
    private java.util.List<String> shapelessGrid(com.google.gson.JsonObject o, java.util.Map<String, String> zh,
                                                 java.util.List<String> mats) {
        java.util.List<String> grid = new java.util.ArrayList<>();
        java.util.List<String> items = new java.util.ArrayList<>();
        if (o.has("ingredients") && o.get("ingredients").isJsonArray()) {
            for (com.google.gson.JsonElement e : o.getAsJsonArray("ingredients")) {
                String n = ingredientName(e, zh);
                items.add(n);
                if (n != null) {
                    mats.add(n);
                }
            }
        }
        for (int i = 0; i < 9; i++) {
            grid.add(i < items.size() ? items.get(i) : null);
        }
        return grid;
    }

    /**
     * 取一个 ingredient 的中文名。**三种写法都要认**（版本之间变过，实测）：
     * · 26.3 等新写法：`key`/`ingredients` 里是**纯字符串** —— `"#": "minecraft:acacia_planks"`
     * · 1.20.6 老写法：**对象** —— `{"item": "minecraft:acacia_planks"}`（也可能有 `{"tag": "..."}`）
     * · 还有一种是数组（多个候选材料）
     * 只认对象的话，新写法会全部返回 null → 合成网格整片空白（已实测踩到）。
     */
    private String ingredientName(com.google.gson.JsonElement e, java.util.Map<String, String> zh) {
        if (e == null || e.isJsonNull()) {
            return null;
        }
        if (e.isJsonPrimitive()) {
            // ★ 新写法：直接就是物品 id 字符串
            String id = e.getAsString();
            return id.startsWith("#") ? id : displayName(id, zh);
        }
        if (e.isJsonArray()) {
            StringBuilder sb = new StringBuilder();
            for (com.google.gson.JsonElement c : e.getAsJsonArray()) {
                String n = ingredientName(c, zh);
                if (n == null) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append("/");
                }
                sb.append(n);
            }
            return sb.length() == 0 ? null : sb.toString();
        }
        if (!e.isJsonObject()) {
            return null;
        }
        com.google.gson.JsonObject o = e.getAsJsonObject();
        String id = str(o, "item");
        if (id != null) {
            return displayName(id, zh);
        }
        String tag = str(o, "tag");
        return tag != null ? "#" + tag : null;
    }

    /** 物品 id → 中文名；查不到就退回短 id。 */
    private String displayName(String id, java.util.Map<String, String> zh) {
        if (id == null) {
            return null;
        }
        String shortId = id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
        if (zh != null) {
            String n = zh.get("item.minecraft." + shortId);
            if (n == null) {
                n = zh.get("block.minecraft." + shortId);
            }
            if (n != null && !n.isEmpty()) {
                return n;
            }
        }
        return shortId;
    }

    /** 从该版本的 assets 里读 zh_cn 语言文件（物品/方块中文名）。读不到返回空表。 */
    private java.util.Map<String, String> loadChineseNames(MainActivity activity, File versionDir) {
        try {
            File gameDir = LabUtils.getGameDir(activity);
            File assetIndexFile = null;
            // 版本 json 里的 assetIndex.id 就是索引文件名（权威）
            File vjson = new File(versionDir, versionDir.getName() + ".json");
            if (vjson.isFile()) {
                String s = readFile(vjson);
                com.google.gson.JsonObject o = com.google.gson.JsonParser.parseString(s).getAsJsonObject();
                if (o.has("assetIndex") && o.get("assetIndex").isJsonObject()) {
                    String idx = str(o.getAsJsonObject("assetIndex"), "id");
                    if (idx != null) {
                        assetIndexFile = new File(new File(gameDir, "assets/indexes"), idx + ".json");
                    }
                }
            }
            if (assetIndexFile == null || !assetIndexFile.isFile()) {
                return java.util.Collections.emptyMap();
            }
            // 索引很大，只做定向查找，不整体解析
            String idx = readFile(assetIndexFile);
            int i = idx.indexOf("\"minecraft/lang/zh_cn.json\"");
            if (i < 0) {
                return java.util.Collections.emptyMap();
            }
            int h = idx.indexOf("\"hash\"", i);
            if (h < 0) {
                return java.util.Collections.emptyMap();
            }
            int q1 = idx.indexOf('"', h + 6);
            int q2 = idx.indexOf('"', q1 + 1);
            if (q1 < 0 || q2 < 0) {
                return java.util.Collections.emptyMap();
            }
            String hash = idx.substring(q1 + 1, q2);
            File obj = new File(new File(gameDir, "assets/objects/" + hash.substring(0, 2)), hash);
            if (!obj.isFile()) {
                return java.util.Collections.emptyMap();
            }
            java.lang.reflect.Type t = new com.google.gson.reflect.TypeToken<java.util.Map<String, String>>() {
            }.getType();
            java.util.Map<String, String> m = new Gson().fromJson(readFile(obj), t);
            return m == null ? java.util.Collections.emptyMap() : m;
        } catch (Throwable t) {
            return java.util.Collections.emptyMap();
        }
    }

    private static String str(com.google.gson.JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : null;
    }

    private static String readAll(java.io.InputStream in) throws java.io.IOException {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        try {
            while ((n = in.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
        } finally {
            in.close();
        }
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String readFile(File f) throws java.io.IOException {
        return readAll(new java.io.FileInputStream(f));
    }

    private static File jarOf(File versionDir, String versionName) {
        if (versionDir == null || !versionDir.isDirectory()) {
            return null;
        }
        File jar = new File(versionDir, versionName + ".jar");
        if (jar.isFile()) {
            return jar;
        }
        File[] jars = versionDir.listFiles(new java.io.FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });
        return (jars != null && jars.length > 0) ? jars[0] : null;
    }

    private void init() {
        this.searchInput = findViewById(R.id.lab_recipe_search_input);
        this.resultsContainer = findViewById(R.id.lab_recipe_results);
        this.detail = findViewById(R.id.lab_recipe_detail);
        this.resultText = findViewById(R.id.lab_recipe_result_text);
        this.gridBlock = findViewById(R.id.lab_recipe_grid_block);
        this.gridContainer = findViewById(R.id.lab_recipe_grid);
        this.materialsBlock = findViewById(R.id.lab_recipe_materials_block);
        this.materialsText = findViewById(R.id.lab_recipe_materials_text);
        this.smeltingBlock = findViewById(R.id.lab_recipe_smelting_block);
        this.smeltingText = findViewById(R.id.lab_recipe_smelting_text);

        findViewById(R.id.lab_recipe_search_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSearch();
            }
        });
        findViewById(R.id.lab_recipe_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        findViewById(R.id.lab_recipe_mcmod).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String key = current != null ? current.name : searchInput.getText().toString().trim();
                if (key.isEmpty()) {
                    return;
                }
                try {
                    getContext().startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://search.mcmod.cn/s?key=" + Uri.encode(key))));
                } catch (Throwable ignored) {
                }
            }
        });
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                doSearch();
            }
        });
    }

    private void doSearch() {
        String query = searchInput.getText().toString().trim();
        resultsContainer.removeAllViews();
        detail.setVisibility(View.GONE);
        current = null;
        if (query.isEmpty()) {
            return;
        }
        int shown = 0;
        for (Recipe recipe : allRecipes) {
            if (matches(recipe, query)) {
                resultsContainer.addView(createResultRow(recipe));
                shown++;
                if (shown >= 50) {
                    break;
                }
            }
        }
        if (shown == 0) {
            TextView empty = new TextView(getContext());
            empty.setText(R.string.lab_recipe_empty);
            empty.setTextSize(13);
            empty.setTextColor(Color.parseColor("#6E6E6E"));
            empty.setPadding(0, dp(6), 0, dp(6));
            resultsContainer.addView(empty);
        }
    }

    private boolean matches(Recipe recipe, String query) {
        if (recipe == null || recipe.name == null) {
            return false;
        }
        if (recipe.name.contains(query)) {
            return true;
        }
        return recipe.id != null && recipe.id.toLowerCase().contains(query.toLowerCase());
    }

    private View createResultRow(Recipe recipe) {
        TextView row = new TextView(getContext());
        row.setText(recipe.name + "   " + typeLabel(recipe.type));
        row.setTextSize(13);
        row.setTextColor(Color.parseColor("#0E9384"));
        row.setBackgroundResource(R.drawable.qcl_button_gray);
        row.setPadding(dp(8), dp(10), dp(8), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(3), 0, dp(3));
        row.setLayoutParams(lp);
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRecipe(recipe);
            }
        });
        return row;
    }

    private void showRecipe(Recipe recipe) {
        current = recipe;
        detail.setVisibility(View.VISIBLE);
        resultText.setText(recipe.name + " x" + Math.max(recipe.count, 1)
                + "   " + recipe.id + "   " + typeLabel(recipe.type));

        boolean hasGrid = recipe.grid != null && recipe.grid.size() >= 9;
        gridBlock.setVisibility(hasGrid ? View.VISIBLE : View.GONE);
        if (hasGrid) {
            renderGrid(recipe.grid);
        }

        StringBuilder materials = new StringBuilder();
        if (recipe.materials != null) {
            for (int i = 0; i < recipe.materials.size(); i++) {
                if (i > 0) {
                    materials.append("\n");
                }
                materials.append("· ").append(recipe.materials.get(i));
            }
        }
        materialsBlock.setVisibility(materials.length() == 0 ? View.GONE : View.VISIBLE);
        materialsText.setText(materials.toString());

        boolean hasSmelting = recipe.smelting != null && !recipe.smelting.isEmpty();
        smeltingBlock.setVisibility(hasSmelting ? View.VISIBLE : View.GONE);
        smeltingText.setText(recipe.smelting);
    }

    private void renderGrid(List<String> grid) {
        gridContainer.removeAllViews();
        for (int row = 0; row < 3; row++) {
            LinearLayout rowLayout = new LinearLayout(getContext());
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                String item = index < grid.size() ? grid.get(index) : null;
                TextView cell = new TextView(getContext());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(38), dp(38));
                lp.setMargins(dp(2), dp(2), dp(2), dp(2));
                cell.setLayoutParams(lp);
                cell.setGravity(Gravity.CENTER);
                cell.setTextSize(9);
                cell.setText(item == null ? "" : item);
                cell.setTextColor(Color.BLACK);
                cell.setBackgroundColor(item == null ? Color.parseColor("#14000000") : Color.parseColor("#33000000"));
                rowLayout.addView(cell);
            }
            gridContainer.addView(rowLayout);
        }
    }

    private String typeLabel(String type) {
        if (type == null) {
            return "";
        }
        if ("crafting_shapeless".equals(type)) {
            return getContext().getString(R.string.lab_recipe_type_shapeless);
        }
        if ("smelting".equals(type)) {
            return getContext().getString(R.string.lab_recipe_type_smelting);
        }
        return getContext().getString(R.string.lab_recipe_type_shaped);
    }

    private int dp(int value) {
        return (int) (value * getContext().getResources().getDisplayMetrics().density + 0.5f);
    }
}