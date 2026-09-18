package com.qcl.launcher.launcher.list.local.controller;

import com.google.gson.Gson;
import com.qcl.launcher.control.bean.BaseButtonInfo;
import com.qcl.launcher.control.bean.BaseRockerViewInfo;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.utils.file.FileStringUtils;
import java.util.ArrayList;

/* loaded from: classes2.dex */
public class ChildLayout {
    public ArrayList<BaseButtonInfo> baseButtonList;
    public ArrayList<BaseRockerViewInfo> baseRockerViewList;
    public String name;
    public int visibility;

    public ChildLayout(String str, int i, ArrayList<BaseButtonInfo> arrayList, ArrayList<BaseRockerViewInfo> arrayList2) {
        this.name = str;
        this.visibility = i;
        this.baseButtonList = arrayList;
        this.baseRockerViewList = arrayList2;
    }

    public static void saveChildLayout(String str, ChildLayout childLayout) {
        FileStringUtils.writeFile(AppManifest.CONTROLLER_DIR + "/" + str + "/" + childLayout.name + ".json", new Gson().toJson(childLayout));
    }
}
