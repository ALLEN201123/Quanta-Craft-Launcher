package com.qcl.launcher.launcher.setting.game.child;

/* loaded from: classes2.dex */
public class PojavLauncherSetting implements Cloneable {
    public boolean enable;
    public String java;
    public String renderer;

    /**
     * ★★★★★ 2026-10-11 新增（用户要求「在版本设置里加一个使用原生手机的 VK 驱动」）：
     * **使用系统（手机自带）的 Vulkan 驱动**，而不是启动器捆绑的 Turnip 等驱动。
     *
     * <p>照 FCL 的同名设置实现：FCL 的 {@code VersionSetting.vulkanDriverSystem}
     * 在 {@code FCLauncher} 里变成环境变量 {@code VULKAN_DRIVER_SYSTEM=1}：
     * <pre>if (config.getUseVKDriverSystem()) envMap.put("VULKAN_DRIVER_SYSTEM", "1");</pre>
     *
     * <p>FCL 的原文说明（{@code settings_fcl_vulkan_driver_system_desc}）：
     * "Use the system Vulkan driver. Better performance but may cause ..."
     * —— 即**性能更好、但可能更不稳定**，所以界面上要如实提示，把选择权交给玩家。
     *
     * <p>默认 false（= 用启动器自带驱动），与以前行为完全一致。
     */
    public boolean vulkanDriverSystem;

    public PojavLauncherSetting(boolean z, String str, String str2) {
        this.enable = z;
        this.renderer = str;
        this.java = str2;
    }

    public void setEnable(boolean z) {
        this.enable = z;
    }

    public boolean isEnable() {
        return this.enable;
    }

    public void setRenderer(String str) {
        this.renderer = str;
    }

    public String getRenderer() {
        return this.renderer;
    }

    public void setJava(String str) {
        this.java = str;
    }

    public String getJava() {
        return this.java;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
