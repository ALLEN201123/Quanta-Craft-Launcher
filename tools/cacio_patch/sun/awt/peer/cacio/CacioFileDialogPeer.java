package sun.awt.peer.cacio;

import java.awt.FileDialog;
import java.awt.peer.FileDialogPeer;
import java.io.File;
import java.io.FilenameFilter;
import javax.swing.JFileChooser;
import javax.swing.JRootPane;
import javax.swing.filechooser.FileFilter;

/**
 * ★ QCL 1.4.5 修补版（覆盖 cacio-shared-1.10 里的同名类）。
 *
 * <p>要解决两件事：
 *
 * <p>① <b>空指针</b>：原版在 postInitSwingComponent() 里把
 * {@code FileDialog.getFile()} / {@code getDirectory()} / {@code getFilenameFilter()}
 * 直接 {@code new File(...)}。而 classic / indev / infdev 的「保存世界」「读取世界」创建
 * FileDialog 时<b>只会先 setDirectory，不会 setFile</b> → file 为 null → NPE。
 *
 * <p>② <b>对话框在安卓上根本没有屏幕呈现</b>：cacio 的 AWT 窗口只被合进游戏自己的 GL 表面
 * （FullScreenWindowFactory + ScreenManagedWindowContainer），而 {@code setZOrder} 明确
 * "NOT YET IMPLEMENTED"。实测：点了「读取世界」后安卓视图树里只有 com.qcl.launcher，
 * 并没有多出任何窗口 —— 所以界面永远出不来，看起来就是"点了没反应"。
 *
 * <p>因此本类改为：<b>不显示界面，直接给出一个可用的文件名</b>，让游戏把流程走完。
 * <ul>
 *   <li>读取（标题含 load / load）：选该目录下<b>最新</b>的普通文件；</li>
 *   <li>保存（其余情况）：给一个不覆盖已有文件的默认名（世界 / 世界2 / 世界3…）；
 *       名字之后可以在启动器的存档页里改（那里本来就有重命名）。</li>
 * </ul>
 * 目录由游戏通过 setDirectory 给出，不需要我们知道自己被关在哪个版本目录里。
 */
class CacioFileDialogPeer extends CacioDialogPeer implements FileDialogPeer {
    private JFileChooser fileChooser;

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
    }

    @Override
    public void postInitSwingComponent() {
        super.postInitSwingComponent();
        this.fileChooser = new JFileChooser();
        ((JRootPane) getSwingComponent()).getContentPane().add(this.fileChooser);
        ((JRootPane) getSwingComponent()).layout();
        FileDialog fd = (FileDialog) getAWTComponent();
        // 目录先设（游戏一定会先调 setDirectory），再按需补一个文件名
        setDirectory(fd.getDirectory());
        setFile(fd.getFile());
        setFilenameFilter(fd.getFilenameFilter());
    }

    public void setFile(String file) {
        // ★ 原版在这里 new File((String) null) → NPE
        if (file == null) {
            return;
        }
        this.fileChooser.setSelectedFile(new File(file));
    }

    public void setDirectory(String dir) {
        // ★ 原版 new File((String) null) → NPE；null 时回退到 QCL home（可写、用户认得）
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
            // 目录不可用就算了，别把对话框搞崩
        }
    }

    public void setFilenameFilter(FilenameFilter filter) {
        // ★ 原版不判空 → 用户浏览目录时 ProxyFilter.accept 会 NPE
        if (filter == null) {
            return;
        }
        this.fileChooser.setFileFilter(new ProxyFilter(filter));
    }

    /**
     * ★ 关键：不显示界面（本来也显示不了），直接补一个文件名让游戏继续。
     */
    @Override
    public void setVisible(boolean visible) {
        if (visible && this.fileChooser != null && this.fileChooser.getSelectedFile() == null) {
            File dir = this.fileChooser.getCurrentDirectory();
            String title = null;
            try {
                title = ((FileDialog) getAWTComponent()).getTitle();
            } catch (Throwable ignored) {
            }
            boolean loading = title != null && title.toLowerCase().contains("load");
            File chosen = loading ? newestFileIn(dir) : unusedNameIn(dir);
            if (chosen != null) {
                this.fileChooser.setSelectedFile(chosen);
            }
        }
        // 不调用 super：cacio 的窗口在安卓上没有呈现路径，调 super 也只是白走一遍。
    }

    private static File[] filesIn(File dir) {
        if (dir == null) {
            return new File[0];
        }
        File[] fs = dir.listFiles();
        return fs == null ? new File[0] : fs;
    }

    /** 读取：取该目录下最新的普通文件。 */
    private static File newestFileIn(File dir) {
        File best = null;
        for (File f : filesIn(dir)) {
            if (!f.isFile()) {
                continue;
            }
            if (best == null || f.lastModified() > best.lastModified()) {
                best = f;
            }
        }
        return best;
    }

    /** 保存：给一个不与现有文件重名的名字（世界 / 世界2 / 世界3 …）。 */
    private static File unusedNameIn(File dir) {
        File first = new File(dir, "世界");
        if (!first.exists()) {
            return first;
        }
        for (int i = 2; i < 1000; i++) {
            File candidate = new File(dir, "世界" + i);
            if (!candidate.exists()) {
                return candidate;
            }
        }
        return new File(dir, "世界" + System.currentTimeMillis());
    }
}
