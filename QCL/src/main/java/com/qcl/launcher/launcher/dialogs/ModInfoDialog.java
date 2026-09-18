package com.qcl.launcher.launcher.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.qcl.launcher.launcher.mod.LocalModFile;
import com.qcl.launcher.launcher.mod.ModManager;
import com.qcl.launcher.utils.Lang;
import com.qcl.launcher.utils.Pair;
import com.qcl.launcher.utils.io.NetworkUtils;
import com.qcl.launcher.utils.io.ZipTools;
import com.qcl.launcher.utils.string.ModTranslations;
import com.qcl.launcher.utils.string.StringUtils;
import java.io.IOException;
import java.io.InputStream;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class ModInfoDialog extends Dialog implements View.OnClickListener {
    private TextView description;
    private Button exit;
    private TextView fileName;
    private ImageView icon;
    private LocalModFile localModFile;
    private LinearLayout mcmod;
    private ModTranslations.Mod modTranslations;
    private LinearLayout modWiki;
    private TextView name;
    private LinearLayout official;
    private TextView version;

    public ModInfoDialog(Context context, LocalModFile localModFile) {
        super(context);
        this.localModFile = localModFile;
        setContentView(R.layout.dialog_local_mod_info);
        setCancelable(false);
        init();
    }

    private void init() {
        this.icon = (ImageView) findViewById(R.id.icon);
        this.name = (TextView) findViewById(R.id.name);
        this.version = (TextView) findViewById(R.id.version);
        this.fileName = (TextView) findViewById(R.id.file_name);
        this.description = (TextView) findViewById(R.id.description);
        this.official = (LinearLayout) findViewById(R.id.official_link);
        this.modWiki = (LinearLayout) findViewById(R.id.mcmod_search_link);
        this.mcmod = (LinearLayout) findViewById(R.id.mcmod_link);
        this.exit = (Button) findViewById(R.id.exit);
        String string = getContext().getString(R.string.mod_manager_ui_unknown_info);
        String name = StringUtils.isBlank(this.localModFile.getName()) ? string : this.localModFile.getName();
        if (!StringUtils.isBlank(this.localModFile.getVersion())) {
            string = this.localModFile.getVersion();
        }
        if (StringUtils.isNotBlank(this.localModFile.getLogoPath())) {
            try {
                InputStream fileInputStream = ZipTools.getFileInputStream(this.localModFile.getFile().toString(), this.localModFile.getLogoPath());
                if (fileInputStream != null) {
                    this.icon.setBackground(new BitmapDrawable(BitmapFactory.decodeStream(fileInputStream)));
                    fileInputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.name.setText(name);
        this.version.setText(string);
        this.fileName.setText(this.localModFile.getFile().toFile().getName());
        this.description.setText(this.localModFile.getDescription().toString());
        int i = 0;
        this.official.setVisibility(StringUtils.isNotBlank(this.localModFile.getUrl()) ? 0 : 8);
        this.modTranslations = ModTranslations.MOD.getModById(this.localModFile.getId());
        this.modWiki.setVisibility(8);
        while (true) {
            if (i >= this.mcmod.getChildCount()) {
                break;
            }
            if (this.mcmod.getChildAt(i) instanceof TextView) {
                ModTranslations.Mod mod = this.modTranslations;
                if (mod == null || StringUtils.isBlank(mod.getMcmod())) {
                    ((TextView) this.mcmod.getChildAt(i)).setText(getContext().getString(R.string.mod_manager_ui_mcmod_search));
                } else {
                    ((TextView) this.mcmod.getChildAt(i)).setText(getContext().getString(R.string.mod_manager_ui_mcmod_page));
                }
            } else {
                i++;
            }
        }
        this.official.setOnClickListener(this);
        this.modWiki.setOnClickListener(this);
        this.mcmod.setOnClickListener(this);
        this.exit.setOnClickListener(this);
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        Uri parse;
        if (view == this.official) {
            getContext().startActivity(new Intent("android.intent.action.VIEW", Uri.parse(this.localModFile.getUrl())));
        }
        if (view == this.modWiki) {
            getContext().startActivity(new Intent("android.intent.action.VIEW", Uri.parse(ModManager.getModWikiUrl(this.modTranslations.getMcbbs()))));
        }
        if (view == this.mcmod) {
            ModTranslations.Mod mod = this.modTranslations;
            if (mod == null || StringUtils.isBlank(mod.getMcmod())) {
                parse = Uri.parse(NetworkUtils.withQuery("https://search.mcmod.cn/s", Lang.mapOf(Pair.pair("key", this.localModFile.getName()), Pair.pair("site", "all"), Pair.pair("filter", "0"))));
            } else {
                parse = Uri.parse(ModManager.getMcmodUrl(this.modTranslations.getMcmod()));
            }
            getContext().startActivity(new Intent("android.intent.action.VIEW", parse));
        }
        if (view == this.exit) {
            dismiss();
        }
    }
}
