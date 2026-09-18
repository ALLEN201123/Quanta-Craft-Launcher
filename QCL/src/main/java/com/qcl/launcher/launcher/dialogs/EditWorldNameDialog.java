package com.qcl.launcher.launcher.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.qcl.launcher.launcher.game.World;
import com.qcl.launcher.launcher.uis.game.manager.right.WorldManagerUI;
import java.io.File;
import java.io.IOException;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class EditWorldNameDialog extends Dialog implements View.OnClickListener {
    private EditText editText;
    private Button negative;
    private Button positive;
    private ProgressBar progressBar;
    private String saveDir;
    private World world;
    private WorldManagerUI worldManagerUI;

    public EditWorldNameDialog(Context context, World world, String str, WorldManagerUI worldManagerUI) {
        super(context);
        this.world = world;
        this.saveDir = str;
        this.worldManagerUI = worldManagerUI;
        setContentView(R.layout.dialog_edit_world_name);
        setCancelable(false);
        init();
    }

    private void init() {
        this.editText = (EditText) findViewById(R.id.world_name);
        this.progressBar = (ProgressBar) findViewById(R.id.progress);
        this.positive = (Button) findViewById(R.id.install);
        this.negative = (Button) findViewById(R.id.cancel);
        this.positive.setOnClickListener(this);
        this.negative.setOnClickListener(this);
        this.editText.setText(this.world.getWorldName());
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.positive && !this.editText.getText().toString().equals("") && !this.editText.getText().toString().contains("/")) {
            final Handler handler = new Handler();
            this.positive.setEnabled(false);
            this.negative.setEnabled(false);
            this.positive.setVisibility(8);
            this.progressBar.setVisibility(0);
            new Thread(new Runnable() { // from class: com.qcl.launcher.launcher.dialogs.EditWorldNameDialog$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    EditWorldNameDialog.this.m227xcaf14d91(handler);
                }
            }).start();
        }
        if (view == this.negative) {
            dismiss();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onClick$3$com-qcl-launcher-launcher-dialogs-EditWorldNameDialog, reason: not valid java name */
    public /* synthetic */ void m227xcaf14d91(Handler handler) {
        try {
            this.world.install(new File(this.saveDir).toPath(), this.editText.getText().toString());
            handler.post(new Runnable() { // from class: com.qcl.launcher.launcher.dialogs.EditWorldNameDialog$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    EditWorldNameDialog.this.m224x4ef44dce();
                }
            });
        } catch (IOException e) {
            handler.post(new Runnable() { // from class: com.qcl.launcher.launcher.dialogs.EditWorldNameDialog$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    EditWorldNameDialog.this.m225x7848a30f(e);
                }
            });
            e.printStackTrace();
        }
        handler.post(new Runnable() { // from class: com.qcl.launcher.launcher.dialogs.EditWorldNameDialog$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                EditWorldNameDialog.this.m226xa19cf850();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onClick$0$com-qcl-launcher-launcher-dialogs-EditWorldNameDialog, reason: not valid java name */
    public /* synthetic */ void m224x4ef44dce() {
        dismiss();
        WorldManagerUI worldManagerUI = this.worldManagerUI;
        worldManagerUI.refresh(worldManagerUI.versionName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onClick$1$com-qcl-launcher-launcher-dialogs-EditWorldNameDialog, reason: not valid java name */
    public /* synthetic */ void m225x7848a30f(IOException iOException) {
        Toast.makeText(getContext(), iOException.toString(), 0).show();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onClick$2$com-qcl-launcher-launcher-dialogs-EditWorldNameDialog, reason: not valid java name */
    public /* synthetic */ void m226xa19cf850() {
        this.positive.setVisibility(0);
        this.progressBar.setVisibility(8);
        this.positive.setEnabled(true);
        this.negative.setEnabled(true);
    }
}
