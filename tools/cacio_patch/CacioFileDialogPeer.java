package sun.awt.peer.cacio;

import java.awt.Dialog;
import java.awt.FileDialog;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Field;
import javax.swing.JFileChooser;
import javax.swing.JRootPane;
import javax.swing.filechooser.FileFilter;

/**
 * ★ QCL 修补版 CacioFileDialogPeer（覆盖 cacio-shared-1.10 的同名类）。
 *
 * <p><b>为什么不能靠"显示对话框"来修</b>：cacio 的 AWT 窗口在安卓上没有窗口实体
 * （日志原话：{@code CacioComponentPeer::setZOrder: NOT YET IMPLEMENTED}），
 * 对话框虽然被创建，却画不到屏幕上 —— 用户表现就是「点保存/读取世界毫无反应、选不了文件夹」。
 *
 * <p><b>所以改成"替游戏作答"</b>：远古版本（indev / infdev）用 AWT FileDialog 只是为了拿一个
 * 「文件名 + 目录」，拿到之后自己写盘（见 {@code net.minecraft.client.c.f}：读 getDirectory()/getFile()
 * 拼成路径再交给存档逻辑）。既然如此，我们直接给出一个可用的答案，跳过那个画不出来的对话框：
 * <ul>
 *   <li>不调用 {@code super.setVisible(true)}（不显示任何窗口），自己设置 fd 的 file/directory；</li>
 *   <li>随后让对话框"隐藏"，使游戏那边的模态循环正常返回，它读到的就是我们设好的值；</li>
 *   <li>保存：自动起名（世界 / 世界2 / 世界3…，避开已存在的名字）；</li>
 *   <li>读取：优先返回最近修改的存档文件；读不到就给默认名（由启动器存档页兜底显示）。</li>
 * </ul>
 *
 * <p><b>坑</b>：不能重写 {@code setVisible} 来做这件事 —— AWT 的 {@code Dialog.show()} 会先把
 * visible 置为 true 再调 peer 的 setVisible，此时我们若调用 {@code fd.setVisible(false)} 会被
 * AWT 直接忽略。必须重写 {@code show()} 本体，并在模态循环真正跑起来之后（延迟一小会儿）
 * 用 {@code Dialog.hide()} 收场。
 */
class CacioFileDialogPeer extends CacioDialogPeer implements java.awt.peer.FileDialogPeer {
    private JFileChooser fileChooser;

    /** 保存对话框的默认世界名。 */
    private static final String DEFAULT_WORLD = "世界";

    /* 保留原 jar 里的内部类，避免外部引用不到。 */
    public static class ProxyFilter extends FileFilter {
        private FilenameFilter target;

        ProxyFilter(FilenameFilter f) {
            this.target = f;
        }

        public boolean accept(File f) {
            return this.target.accept(f.getAbsoluteFile().getParentFile(), f.getName());
        }

        public String getDescription() {
            return "No description";
        }
    }

    public CacioFileDialogPeer(FileDialog d, PlatformWindowFactory pwf) {
        super(d, pwf);
        System.out.println("[QCL-cacio] 补丁类被加载：构造 CacioFileDialogPeer");
    }

    @Override
    public void postInitSwingComponent() {
        System.out.println("[QCL-cacio] 进入 postInitSwingComponent（即将调 super）");
        try {
            super.postInitSwingComponent();
            System.out.println("[QCL-cacio] super.postInitSwingComponent() 正常返回");
        } catch (Throwable t) {
            System.out.println("[QCL-cacio] super.postInitSwingComponent() 抛异常：" + t);
            t.printStackTrace();
        }
        try {
            this.fileChooser = new JFileChooser();
            ((JRootPane) getSwingComponent()).getContentPane().add(this.fileChooser);
            ((JRootPane) getSwingComponent()).layout();
            FileDialog fd = (FileDialog) getAWTComponent();
            // 原版在这里 new File(null) → NPE，导致对话框根本建不起来。用防护版覆盖。
            setDirectory(fd.getDirectory());
            setFile(fd.getFile());
            setFilenameFilter(fd.getFilenameFilter());
            System.out.println("[QCL-cacio] postInitSwingComponent 收尾完成（dir="
                    + fd.getDirectory() + " file=" + fd.getFile() + "）");
        } catch (Throwable t) {
            System.out.println("[QCL-cacio] postInitSwingComponent 收尾抛异常：" + t);
            t.printStackTrace();
        }
    }

    /**
     * ★ 核心：不显示对话框，先把答案写进 FileDialog，再让对话框自己收起 ——
     * 游戏那边 {@code setVisible(true)} 的模态循环会正常返回，它读到的就是我们设好的 file/directory。
     *
     * <p>注意顺序：必须在 {@code super.setVisible(true)} 之前把值设好，
     * 让模态循环一开始就有答案可用；收起动作放在另一个线程（AWT 在 show() 期间会忽略本线程的 setVisible）。
     */
    @Override
    public void setVisible(boolean visible) {
        System.out.println("[QCL-cacio] setVisible(" + visible + ") 被调用");
        if (!visible) {
            super.setVisible(false);
            return;
        }
        FileDialog fd = (FileDialog) getAWTComponent();
        try {
            String dir = fd.getDirectory();
            if (dir == null || dir.isEmpty()) {
                dir = defaultDirectory();
            }
            if (!dir.endsWith(File.separator)) {
                dir = dir + File.separator;
            }
            String name = (fd.getMode() == FileDialog.SAVE)
                    ? pickSaveName(dir)
                    : pickLoadName(dir);
            System.out.println("[QCL-cacio] 替游戏作答：mode="
                    + (fd.getMode() == FileDialog.SAVE ? "保存" : "打开")
                    + " 目录=" + dir + " 文件名=" + name);
            try {
                fd.setDirectory(dir);
            } catch (Throwable ignored) {
            }
            try {
                fd.setFile(name);
            } catch (Throwable ignored) {
            }
        } catch (Throwable t) {
            System.out.println("[QCL-cacio] 作答失败：" + t);
        }
        super.setVisible(true);
        // 让游戏那边的模态循环正常返回：稍后把对话框收起来。
        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(700L);
                } catch (InterruptedException ignored) {
                }
                try {
                    Dialog d = (Dialog) getAWTComponent();
                    setVisibleFalse(d);
                    d.hide();
                    System.out.println("[QCL-cacio] 已自动收起对话框");
                } catch (Throwable t) {
                    System.out.println("[QCL-cacio] 收尾失败：" + t);
                }
            }
        }, "qcl-cacio-autoclose").start();
    }

    /** 直接改 visible 字段，绕开 AWT「show() 期间忽略 setVisible」的规则。 */
    private static void setVisibleFalse(Dialog d) {
        try {
            Field f = java.awt.Component.class.getDeclaredField("visible");
            f.setAccessible(true);
            f.setBoolean(d, false);
        } catch (Throwable ignored) {
        }
    }

    /** 默认目录：优先游戏目录下的 saves（游戏自己就是往那儿存的）。 */
    private String defaultDirectory() {
        String home = System.getProperty("user.home");
        String gameDir = System.getProperty("pojav.path.minecraft");
        File saves = null;
        if (gameDir != null) {
            saves = new File(gameDir, "saves");
        }
        if (saves == null && home != null) {
            saves = new File(home, "saves");
        }
        if (saves != null && !saves.exists()) {
            saves.mkdirs();
        }
        if (saves != null) {
            return saves.getAbsolutePath();
        }
        return home != null ? home : "/storage/emulated/0";
    }

    /** 保存：起一个不冲突的名字（世界 / 世界2 / 世界3…）。 */
    private String pickSaveName(String dir) {
        String name = DEFAULT_WORLD;
        File f = new File(dir, name);
        int i = 2;
        while (f.exists() && i < 1000) {
            name = DEFAULT_WORLD + i;
            f = new File(dir, name);
            i++;
        }
        return name;
    }

    /**
     * 读取：返回**最近修改的那个 .mclevel 文件的完整文件名**（含后缀）。
     *
     * <p>⚠️ 这里必须返回完整文件名：游戏拿 {@code getFile()} 之后直接拼
     * {@code getDirectory() + getFile()} 打开，**不会**补后缀
     * （只有保存路径的 {@code e.f_()} 才会补 .mclevel）。
     * 早期版本这里返回了去掉后缀的名字 → 游戏去找 {@code 世界1.mclevel} → FileNotFoundException。
     */
    private String pickLoadName(String dir) {
        File best = newestMcLevel(new File(dir));
        if (best == null) {
            // 也扫一遍所有版本的 saves（远古版本存在版本目录里，当前目录可能不是它）
            String gameDir = System.getProperty("pojav.path.minecraft");
            if (gameDir != null) {
                File[] vs = new File(gameDir, "versions").listFiles();
                if (vs != null) {
                    for (File v : vs) {
                        File cand = newestMcLevel(new File(v, "saves"));
                        if (cand != null && (best == null || cand.lastModified() > best.lastModified())) {
                            best = cand;
                        }
                    }
                }
            }
        }
        if (best != null) {
            System.out.println("[QCL-cacio] 读取目标：" + best.getAbsolutePath());
            return best.getName();
        }
        // 实在没有存档：给个带后缀的名字，让游戏报错也别崩
        System.out.println("[QCL-cacio] 没找到任何 .mclevel，返回默认名");
        return DEFAULT_WORLD + ".mclevel";
    }

    /** 在某个目录里找最近修改的 .mclevel 文件（不区分大小写）。 */
    private static File newestMcLevel(File dir) {
        if (dir == null) {
            return null;
        }
        File[] list = dir.listFiles();
        if (list == null) {
            return null;
        }
        File best = null;
        for (File f : list) {
            if (!f.isFile()) {
                continue;
            }
            if (!f.getName().toLowerCase().endsWith(".mclevel")) {
                continue;
            }
            if (best == null || f.lastModified() > best.lastModified()) {
                best = f;
            }
        }
        return best;
    }

    public void setFile(String file) {
        if (file == null) {
            return;
        }
        this.fileChooser.setSelectedFile(new File(file));
    }

    public void setDirectory(String dir) {
        String target = dir;
        if (target == null) {
            target = System.getProperty("user.home");
        }
        if (target == null) {
            return;
        }
        try {
            this.fileChooser.setCurrentDirectory(new File(target));
        } catch (Throwable ignored) {
        }
    }

    public void setFilenameFilter(FilenameFilter filter) {
        if (filter == null) {
            return;
        }
        this.fileChooser.setFileFilter(new ProxyFilter(filter));
    }
}
