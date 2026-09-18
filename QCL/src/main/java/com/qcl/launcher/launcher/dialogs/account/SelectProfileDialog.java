package com.qcl.launcher.launcher.dialogs.account;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.qcl.launcher.auth.yggdrasil.YggdrasilService;
import com.qcl.launcher.auth.yggdrasil.YggdrasilSession;
import com.qcl.launcher.launcher.dialogs.account.AddAuthlibInjectorAccountDialog;
import com.qcl.launcher.launcher.list.account.ProfileListAdapter;
import java.util.ArrayList;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class SelectProfileDialog extends Dialog implements View.OnClickListener {
    private ArrayList<Bitmap> bitmaps;
    private Button cancel;
    public String email;
    private boolean isNide;
    private ListView listView;
    private AddAuthlibInjectorAccountDialog.OnAuthlibInjectorAccountAddListener onAuthlibInjectorAccountAddListener;
    public String password;
    public String url;
    public YggdrasilService yggdrasilService;
    public YggdrasilSession yggdrasilSession;

    public SelectProfileDialog(Context context, YggdrasilService yggdrasilService, YggdrasilSession yggdrasilSession, String str, String str2, String str3, ArrayList<Bitmap> arrayList, AddAuthlibInjectorAccountDialog.OnAuthlibInjectorAccountAddListener onAuthlibInjectorAccountAddListener, boolean z) {
        super(context);
        setContentView(R.layout.dialog_select_profile);
        setCancelable(false);
        this.yggdrasilService = yggdrasilService;
        this.yggdrasilSession = yggdrasilSession;
        this.email = str;
        this.password = str2;
        this.url = str3;
        this.bitmaps = arrayList;
        this.onAuthlibInjectorAccountAddListener = onAuthlibInjectorAccountAddListener;
        this.isNide = z;
        init();
    }

    private void init() {
        Button button = (Button) findViewById(R.id.exit);
        this.cancel = button;
        button.setOnClickListener(this);
        this.listView = (ListView) findViewById(R.id.profile_list);
        refreshList();
    }

    private void refreshList() {
        this.listView.setAdapter((ListAdapter) new ProfileListAdapter(getContext(), this.yggdrasilSession.getAvailableProfiles(), this.bitmaps, this.onAuthlibInjectorAccountAddListener, this, this.isNide));
        ViewGroup.LayoutParams layoutParams = this.listView.getLayoutParams();
        layoutParams.width = getMaxWidth(this.listView);
        this.listView.setLayoutParams(layoutParams);
    }

    private int getMaxWidth(ListView listView) {
        int i = 550;
        if (listView.getAdapter() == null) {
            return 550;
        }
        int count = listView.getAdapter().getCount();
        for (int i2 = 0; i2 < count; i2++) {
            View view = listView.getAdapter().getView(i2, null, listView);
            view.measure(View.MeasureSpec.makeMeasureSpec(0, 0), View.MeasureSpec.makeMeasureSpec(0, 0));
            if (view.getMeasuredWidth() > i) {
                i = view.getMeasuredWidth();
            }
        }
        return i;
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.cancel) {
            dismiss();
        }
    }
}
