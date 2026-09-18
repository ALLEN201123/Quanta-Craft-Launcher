package com.qcl.launcher.launcher.list.account.server;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.qcl.launcher.auth.authlibinjector.AuthlibInjectorServer;
import java.util.ArrayList;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class AuthlibInjectorServerSpinnerAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<AuthlibInjectorServer> list;

    @Override // android.widget.Adapter
    public long getItemId(int i) {
        return 0L;
    }

    /* loaded from: classes2.dex */
    private class ViewHolder {
        TextView name;
        TextView url;

        private ViewHolder() {
        }
    }

    public AuthlibInjectorServerSpinnerAdapter(Context context, ArrayList<AuthlibInjectorServer> arrayList) {
        this.context = context;
        this.list = arrayList;
    }

    public int getItemPosition(AuthlibInjectorServer authlibInjectorServer) {
        for (int i = 0; i < this.list.size(); i++) {
            if (this.list.get(i).equals(authlibInjectorServer)) {
                return i;
            }
        }
        return -1;
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
            view2 = LayoutInflater.from(this.context).inflate(R.layout.spinner_item_server_list, (ViewGroup) null);
            viewHolder.name = (TextView) view2.findViewById(R.id.server_name);
            viewHolder.url = (TextView) view2.findViewById(R.id.server_url);
            view2.setTag(viewHolder);
        } else {
            view2 = view;
            viewHolder = (ViewHolder) view.getTag();
        }
        AuthlibInjectorServer authlibInjectorServer = this.list.get(i);
        viewHolder.name.setText(authlibInjectorServer.getName());
        viewHolder.url.setText(authlibInjectorServer.getUrl());
        return view2;
    }
}
