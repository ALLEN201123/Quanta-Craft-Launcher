package com.qcl.launcher.launcher.uis.tools;

import android.content.Context;
import android.content.Intent;

import com.qcl.launcher.launcher.MainActivity;

public class BaseUI implements UILifecycleCallbacks{

    public Context context;
    public MainActivity activity;

    //Method instruction
    public BaseUI(Context context,MainActivity activity){
        super();
        this.context = context;
        this.activity = activity;
    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onStart() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onRestart() {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void onStop() {

    }

    @Override
    public void onDestroy() {

    }

    @Override
    public void onLoaded() {

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

    @Override
    public void onNewIntent() {

    }

    /**
     * 1.0.6 新增：安全判断「当前页面是否需要显示『回到主界面』按钮」。
     *
     * <p>原来各处都写 {@code uis.get(uis.size() - 2) != mainUI}，但这不是恒成立的：
     * {@code backToHome()} 会把 uis 清空后只留 mainUI，{@code closeCurrentUI()} 结束时
     * 也只是重置 currentUI。若此时 uis 里元素不足 2 个，{@code get(size()-2)} 会拿到 -1
     * 并抛 IndexOutOfBoundsException，把调用它的 onStart 整段打断 —— 结果就是
     * 「二级页面能打开，但返回栏不显示、也退不回去」。
     * 这里统一改成安全判断：越界一律返回 false（宁可不显示多余的按钮，也不能崩）。
     */
    public boolean canGoBackToLast() {
        try {
            if (activity == null || activity.uiManager == null || activity.uiManager.uis == null) {
                return false;
            }
            int size = activity.uiManager.uis.size();
            if (size < 2) {
                return false;
            }
            return activity.uiManager.uis.get(size - 2) != activity.uiManager.mainUI;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
