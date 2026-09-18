package com.qcl.launcher.launcher.dialogs.account;

import android.app.Dialog;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.qcl.launcher.auth.Account;
import com.qcl.launcher.skin.utils.Avatar;
import com.qcl.launcher.utils.gson.UUIDTypeAdapter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class AddOfflineAccountDialog extends Dialog implements View.OnClickListener {
    private ArrayList<Account> accounts;
    private Button cancel;
    private Context context;
    private EditText editName;
    private EditText editUUID;
    private LinearLayout editUUIDLayout;
    private LinearLayout hintLayout;
    private Button login;
    private OnOfflineAccountAddListener onOfflineAccountAddListener;
    private TextView purchaseLink;
    private LinearLayout showAdvanceSetting;
    private ImageView spinView;

    /* loaded from: classes2.dex */
    public interface OnOfflineAccountAddListener {
        void onPositive(Account account);
    }

    public AddOfflineAccountDialog(Context context, ArrayList<Account> arrayList, OnOfflineAccountAddListener onOfflineAccountAddListener) {
        super(context);
        this.context = context;
        this.accounts = arrayList;
        this.onOfflineAccountAddListener = onOfflineAccountAddListener;
        setContentView(R.layout.dialog_add_offline_account);
        setCancelable(false);
        init();
    }

    private void init() {
        this.editName = (EditText) findViewById(R.id.edit_user_name);
        this.editUUID = (EditText) findViewById(R.id.edit_uuid);
        TextView textView = (TextView) findViewById(R.id.purchase_link);
        this.purchaseLink = textView;
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.show_advance_setting);
        this.showAdvanceSetting = linearLayout;
        linearLayout.setOnClickListener(this);
        this.spinView = (ImageView) findViewById(R.id.spin_view);
        this.editUUIDLayout = (LinearLayout) findViewById(R.id.edit_uuid_layout);
        this.hintLayout = (LinearLayout) findViewById(R.id.hint_layout);
        this.login = (Button) findViewById(R.id.login_offline);
        this.cancel = (Button) findViewById(R.id.cancel_login_offline);
        this.login.setOnClickListener(this);
        this.cancel.setOnClickListener(this);
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        String str;
        if (view == this.showAdvanceSetting) {
            if (this.editUUIDLayout.getVisibility() == 8) {
                this.editUUIDLayout.setVisibility(0);
                this.hintLayout.setVisibility(0);
                RotateAnimation rotateAnimation = new RotateAnimation(0.0f, 180.0f, 1, 0.5f, 1, 0.5f);
                rotateAnimation.setDuration(30L);
                rotateAnimation.setInterpolator(new LinearInterpolator());
                rotateAnimation.setRepeatMode(2);
                rotateAnimation.setFillAfter(true);
                this.spinView.startAnimation(rotateAnimation);
            } else {
                this.editUUIDLayout.setVisibility(8);
                this.hintLayout.setVisibility(8);
                RotateAnimation rotateAnimation2 = new RotateAnimation(180.0f, 0.0f, 1, 0.5f, 1, 0.5f);
                rotateAnimation2.setDuration(30L);
                rotateAnimation2.setInterpolator(new LinearInterpolator());
                rotateAnimation2.setRepeatMode(2);
                rotateAnimation2.setFillAfter(true);
                this.spinView.startAnimation(rotateAnimation2);
            }
        }
        if (view == this.login) {
            ArrayList arrayList = new ArrayList();
            Iterator<Account> it = this.accounts.iterator();
            while (it.hasNext()) {
                Account next = it.next();
                if (next.loginType == 1) {
                    arrayList.add(next.auth_player_name);
                }
            }
            if (this.editName.getText().toString().equals("")) {
                Context context = this.context;
                Toast.makeText(context, context.getString(R.string.dialog_add_offline_account_empty_warn), 0).show();
            } else if (arrayList.contains(this.editName.getText().toString())) {
                Context context2 = this.context;
                Toast.makeText(context2, context2.getString(R.string.dialog_add_offline_account_exist_warn), 0).show();
            } else {
                try {
                    str = Avatar.bitmapToString(BitmapFactory.decodeStream(this.context.getAssets().open("img/alex.png")));
                } catch (IOException e) {
                    e.printStackTrace();
                    str = "";
                }
                this.onOfflineAccountAddListener.onPositive(new Account(1, "", "", "mojang", "0", this.editName.getText().toString(), this.editUUID.getText().toString().equals("") ? UUID.randomUUID().toString() : this.editUUID.getText().toString(), UUIDTypeAdapter.fromUUID(UUID.randomUUID()), "", "", "", str));
                dismiss();
            }
        }
        if (view == this.cancel) {
            dismiss();
        }
    }
}
