package sun.awt.peer.cacio;

import java.awt.FileDialog;
import java.awt.peer.FileDialogPeer;
import java.io.File;
import java.io.FilenameFilter;
import javax.swing.JFileChooser;
import javax.swing.JRootPane;
import javax.swing.filechooser.FileFilter;

/**
 * ★ QCL 1.4.5 修补版：覆盖 cacio-shared-1.10 里的同名类。
 *
 * <p>原版问题：{@code postInitSwingComponent()} 会把 {@code FileDialog.getFile()} /
 * {@code getDirectory()} 的返回值直接 {@code new File(...)}。而游戏（例如 indev / infdev
 * 的「保存世界」「读取世界」）创建 FileDialog 时**通常不会先调 setFile/setDirectory**，
 * 这两个方法返回的就是 {@code null} → {@code new File((String) null)} 抛 NPE
 * → 对话框建不起来（表现：点「保存世界」/「读取世界」毫无反应，日志里反复刷同一个 NPE）。
 *
 * <p>本修补：
 * <ul>
 *   <li>{@code setFile}：null 时不设置选中文件（保留 JFileChooser 默认），不崩；</li>
 *   <li>{@code setDirectory}：null 时**回退到 QCL 的 home 目录**（user.home 就是 /storage/emulated/0/QCL），
 *       至少让用户从一个可写、看得懂的位置开始挑；</li>
 *   <li>{@code setFilenameFilter}：null 时不设过滤器（原版会在用户浏览目录时 NPE）。</li>
 * </ul>
 *
 * <p>放在 ResConfHack.jar（位于 {@code -Xbootclasspath/p} 且排在 cacio-shared 之前），
 * 因此本类会优先于原版被加载。
 */
class CacioFileDialogPeer extends CacioDialogPeer implements FileDialogPeer {
    private JFileChooser fileChooser;

    /* 保留：原 jar 里的内部类，仅在原版真的用到时才会走到。 */
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
        setDirectory(fd.getDirectory());
        setFile(fd.getFile());
        setFilenameFilter(fd.getFilenameFilter());
    }

    public void setFile(String file) {
        // ★ 原版在这里 new File(null) → NPE
        if (file == null) {
            return;
        }
        this.fileChooser.setSelectedFile(new File(file));
    }

    public void setDirectory(String dir) {
        // ★ 原版在这里 new File(null) → NPE；回退到 QCL home，至少是可写的位置
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
}
