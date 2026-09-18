package com.qcl.launcher.launcher.list.account;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.qcl.launcher.auth.Account;
import com.qcl.launcher.auth.yggdrasil.GameProfile;
import com.qcl.launcher.launcher.dialogs.account.AddAuthlibInjectorAccountDialog;
import com.qcl.launcher.launcher.dialogs.account.SelectProfileDialog;
import com.qcl.launcher.skin.utils.Avatar;
import java.util.ArrayList;
import java.util.List;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class ProfileListAdapter extends BaseAdapter {
    private ArrayList<Bitmap> bitmaps;
    private Context context;
    private SelectProfileDialog dialog;
    private boolean isNide;
    private List<GameProfile> list;
    private AddAuthlibInjectorAccountDialog.OnAuthlibInjectorAccountAddListener onAuthlibInjectorAccountAddListener;

    @Override // android.widget.Adapter
    public long getItemId(int i) {
        return 0L;
    }

    public ProfileListAdapter(Context context, List<GameProfile> list, ArrayList<Bitmap> arrayList, AddAuthlibInjectorAccountDialog.OnAuthlibInjectorAccountAddListener onAuthlibInjectorAccountAddListener, SelectProfileDialog selectProfileDialog, boolean z) {
        this.context = context;
        this.list = list;
        this.bitmaps = arrayList;
        this.onAuthlibInjectorAccountAddListener = onAuthlibInjectorAccountAddListener;
        this.dialog = selectProfileDialog;
        this.isNide = z;
    }

    /* loaded from: classes2.dex */
    private class ViewHolder {
        ImageView face;
        ImageView hat;
        LinearLayout item;
        TextView name;

        private ViewHolder() {
        }
    }

    @Override // android.widget.Adapter
    public int getCount() {
        return this.list.size();
    }

    @Override // android.widget.Adapter
    public Object getItem(int i) {
        return this.list.get(i);
    }

    @Override // android.widget.Adapter
    public View getView(int i, View view, ViewGroup viewGroup) {
        View view2;
        ViewHolder viewHolder;
        if (view == null) {
            viewHolder = new ViewHolder();
            view2 = LayoutInflater.from(this.context).inflate(R.layout.item_profile, viewGroup, false);
            viewHolder.item = (LinearLayout) view2.findViewById(R.id.item);
            viewHolder.face = (ImageView) view2.findViewById(R.id.skin_face);
            viewHolder.hat = (ImageView) view2.findViewById(R.id.skin_hat);
            viewHolder.name = (TextView) view2.findViewById(R.id.name);
            view2.setTag(viewHolder);
        } else {
            view2 = view;
            viewHolder = (ViewHolder) view.getTag();
        }
        final GameProfile gameProfile = this.list.get(i);
        final Bitmap bitmap = this.bitmaps.get(i);
        viewHolder.name.setText(gameProfile.getName());
        Avatar.setAvatar(Avatar.bitmapToString(bitmap), viewHolder.face, viewHolder.hat);
        viewHolder.item.setOnClickListener(new View.OnClickListener() { // from class: com.qcl.launcher.launcher.list.account.ProfileListAdapter$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view3) {
                ProfileListAdapter.this.m395xc213d57e(bitmap, gameProfile, view3);
            }
        });
        return view2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getView$0$com-qcl-launcher-launcher-list-account-ProfileListAdapter, reason: not valid java name */
    public /* synthetic */ void m395xc213d57e(Bitmap bitmap, GameProfile gameProfile, View view) {
        this.onAuthlibInjectorAccountAddListener.onAccountAdd(new Account(this.isNide ? 5 : 4, this.dialog.email, this.dialog.password, "mojang", "0", gameProfile.getName(), gameProfile.getId().toString(), this.dialog.yggdrasilSession.getAccessToken(), this.dialog.yggdrasilSession.getClientToken(), "", this.dialog.url, Avatar.bitmapToString(bitmap)));
        this.dialog.dismiss();
    }
}
