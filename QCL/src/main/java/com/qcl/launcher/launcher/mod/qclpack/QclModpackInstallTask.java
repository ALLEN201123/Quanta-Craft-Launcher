package com.qcl.launcher.launcher.mod.qclpack;

import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.list.install.DownloadTaskListAdapter;
import com.qcl.launcher.launcher.mod.BaseModpackInstallTask;
import com.qcl.launcher.launcher.mod.Modpack;

import java.io.File;

/**
 * ★ 整合包（包里有 minecraft/pack.json）的真实安装任务。
 *
 * 【原来是 18 行空壳】构造函数空、doInBackground 直接 return null —— 导入这种包
 * "看起来成功了"，实际一个文件都没装。
 *
 * 现在真正的安装步骤都在 {@link BaseModpackInstallTask#installHmclZip()} 里
 * （放在基类是因为「zip 内容像这种包、但 manifest 没解析出来」的兜底路径也要用）：
 *   1. minecraft/ 的内容解到**版本目录**（排除 pack.json）—— 照 FCL，
 *      客户端 mod / config 都在 minecraft/ 下
 *   2. 准备基础游戏版本（本体 jar + json；本地有就复制，没有就下载）
 *   3. 把 minecraft/pack.json 当 patch 合进基础版本 json
 *      （PatchMerger.mergePatch，与 GameInstallDialog 装 Forge / Fabric 同一条路径），
 *      mainClass / 参数 / libraries 都能正确叠上去
 *   4. 把合并后版本 json 里声明的库补下到全局 libraries/（best-effort）
 *   5. 写 versions/&lt;name&gt;/&lt;name&gt;.json.modpack
 *
 * <p>★ 2026-10-06 改名：原 {@code HMCLModpackInstallTask} → {@code QclModpackInstallTask}
 * （只清 Java 类名的品牌前缀，磁盘上的格式标识 "HMCL" 保持不变，见 QclModpackProvider）。
 */
public class QclModpackInstallTask extends BaseModpackInstallTask {

    /** 保留原提供器用的构造签名（提供器 new 的时候没有 Activity，走基类静态 setActivity） */
    public QclModpackInstallTask(File file, QclModpackProvider.QclModpack qclModpack, String name) {
        this(sActivity, file, qclModpack, name, null);
    }

    public QclModpackInstallTask(MainActivity activity, File file, Modpack modpack,
                                 String name, DownloadTaskListAdapter adapter) {
        super(activity, file, modpack, name, adapter);
    }

    @Override
    protected void install() throws Exception {
        installHmclZip();
    }
}
