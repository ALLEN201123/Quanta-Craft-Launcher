package com.qcl.launcher.launcher.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class OutdatedWarningDialog extends Dialog implements View.OnClickListener {
    private CheckBox checkBox;
    private Button fcl;
    private Button pojav;
    private Button positive;

    public static void init(Context context) {
    }

    public OutdatedWarningDialog(Context context) {
        super(context);
        setCancelable(false);
        setContentView(R.layout.dialog_outdated_warning);
        this.checkBox = (CheckBox) findViewById(R.id.hide);
        this.pojav = (Button) findViewById(R.id.pojav);
        this.fcl = (Button) findViewById(R.id.fcl);
        this.positive = (Button) findViewById(R.id.positive);
        this.pojav.setOnClickListener(this);
        this.fcl.setOnClickListener(this);
        this.positive.setOnClickListener(this);
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.pojav) {
            getContext().startActivity(new Intent("android.intent.action.VIEW", Uri.parse("https://github.com/PojavLauncherTeam/PojavLauncher")));
        }
        if (view == this.fcl) {
            getContext().startActivity(new Intent("android.intent.action.VIEW", Uri.parse("https://github.com/FCL-Team/FoldCraftLauncher/releases")));
        }
        if (view == this.positive) {
            if (this.checkBox.isChecked()) {
                SharedPreferences.Editor edit = getContext().getSharedPreferences("warning", 0).edit();
                edit.putBoolean("outdated_warning", false);
                edit.apply();
            }
            dismiss();
        }
    }
}
