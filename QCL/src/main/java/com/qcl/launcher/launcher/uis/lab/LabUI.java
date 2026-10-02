package com.qcl.launcher.launcher.uis.lab;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

import com.qcl.launcher.R;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.dialogs.lab.LabColorTextDialog;
import com.qcl.launcher.launcher.dialogs.lab.LabDatapackDialog;
import com.qcl.launcher.launcher.dialogs.lab.LabRecipeDialog;
import com.qcl.launcher.launcher.dialogs.lab.LabSchematicDownloadDialog;
import com.qcl.launcher.launcher.dialogs.lab.LabSchematicStudioDialog;
import com.qcl.launcher.launcher.dialogs.lab.LabSeedDialog;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;

/**
 * 实验室页面：集中放置若干实验性小工具，点击卡片弹出对应弹窗。
 */
public class LabUI extends BaseUI implements View.OnClickListener {
    public LinearLayout labUI;
    private LinearLayout cardColorText;
    private LinearLayout cardSeed;
    private LinearLayout cardRecipe;
    private LinearLayout cardDatapack;
    private LinearLayout cardSchematicStudio;
    private LinearLayout cardSchematicDownload;

    public LabUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.labUI = (LinearLayout) this.activity.findViewById(R.id.ui_lab);
        this.cardColorText = (LinearLayout) this.activity.findViewById(R.id.lab_card_color_text);
        this.cardSeed = (LinearLayout) this.activity.findViewById(R.id.lab_card_seed);
        this.cardRecipe = (LinearLayout) this.activity.findViewById(R.id.lab_card_recipe);
        this.cardDatapack = (LinearLayout) this.activity.findViewById(R.id.lab_card_datapack);
        this.cardSchematicStudio = (LinearLayout) this.activity.findViewById(R.id.lab_card_schematic_studio);
        this.cardSchematicDownload = (LinearLayout) this.activity.findViewById(R.id.lab_card_schematic_download);
        this.cardColorText.setOnClickListener(this);
        this.cardSeed.setOnClickListener(this);
        this.cardRecipe.setOnClickListener(this);
        this.cardDatapack.setOnClickListener(this);
        this.cardSchematicStudio.setOnClickListener(this);
        this.cardSchematicDownload.setOnClickListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        this.activity.showBarTitle(this.context.getResources().getString(R.string.lab_ui_title), canGoBackToLast(), true);
        CustomAnimationUtils.showViewFromLeft(this.labUI, this.activity, this.context, true);
    }

    @Override
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.labUI, this.activity, this.context, true);
    }

    @Override
    public void onClick(View view) {
        if (view == this.cardColorText) {
            new LabColorTextDialog(this.activity).show();
        } else if (view == this.cardSeed) {
            new LabSeedDialog(this.activity).show();
        } else if (view == this.cardRecipe) {
            new LabRecipeDialog(this.activity).show();
        } else if (view == this.cardDatapack) {
            new LabDatapackDialog(this.activity).show();
        } else if (view == this.cardSchematicStudio) {
            new LabSchematicStudioDialog(this.activity).show();
        } else if (view == this.cardSchematicDownload) {
            new LabSchematicDownloadDialog(this.activity).show();
        }
    }
}