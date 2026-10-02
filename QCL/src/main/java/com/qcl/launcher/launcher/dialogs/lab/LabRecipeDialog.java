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

    public LabRecipeDialog(Context context) {
        super(context);
        setContentView(R.layout.dialog_lab_recipe);
        loadRecipes();
        init();
        LabUtils.setupDialogWindow(this);
    }

    private void loadRecipes() {
        try (InputStreamReader reader = new InputStreamReader(
                getContext().getAssets().open("lab_recipes.json"), StandardCharsets.UTF_8)) {
            RecipeFile file = new Gson().fromJson(reader, RecipeFile.class);
            if (file != null && file.recipes != null) {
                allRecipes.addAll(file.recipes);
            }
        } catch (Throwable e) {
            LabUtils.toast(getContext(), "配方数据加载失败");
        }
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