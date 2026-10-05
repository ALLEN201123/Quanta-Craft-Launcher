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
            // ★★★ 保存时的文件名优先级（从高到低）：
            //   1) 玩家在「输入世界名称」界面输入的名字 —— 游戏侧补丁(c.p)把它存进了系统属性 qcl.savename；
            //      ★ 游戏本身**从来不用**这个名字（c.p 只拿它控制"保存"按钮是否可点，
            //        而 c.f 开对话框时也从不调 setFile()），所以必须由我们接上，
            //        否则玩家输的名字白输、存档会被存成"世界2.mclevel"这种新文件。
            //   2) 对话框已有的名字（fd.getFile()）。
            //   3) 目录里唯一的 .mclevel（远古版本通常只有一个世界 → 直接沿用，避免"每存一次多一个文件"）。
            //   4) 最后才自动起名（世界 / 世界2 / …）。
            String name;
            if (fd.getMode() == FileDialog.SAVE) {
                // ★ 保存也让玩家自己挑目标文件（挑不到再回退到"输入世界名称"里给的名字）
                if (tryNativePicker(fd)) {
                    System.out.println("[QCL-cacio] 保存：已弹出文件浏览器");
                    super.setVisible(true);
                    scheduleAutoClose();
                    return;
                }
                String fromProp = null;
                try {
                    fromProp = System.getProperty("qcl.savename");
                } catch (Throwable ignored) {
                }
                String typed = fd.getFile();
                if (fromProp != null && !fromProp.trim().isEmpty()) {
                    name = fromProp.trim();
                    System.out.println("[QCL-cacio] 采用玩家输入的世界名：" + name);
                } else if (typed != null && !typed.trim().isEmpty()) {
                    name = typed.trim();
                    System.out.println("[QCL-cacio] 采用对话框已有名字：" + name);
                } else {
                    String only = onlyMcLevelName(dir);
                    if (only != null) {
                        name = only;
                        System.out.println("[QCL-cacio] 目录里只有一个存档，沿用它：" + name);
                    } else {
                        name = pickSaveName(dir);
                        System.out.println("[QCL-cacio] 自动起名：" + name);
                    }
                }
            } else {
                // ★★ 读取世界：弹系统文件选择器，让玩家自己挑（拿不到再回退到"最新存档"）
                name = null;
                if (tryNativePicker(fd)) {
                    System.out.println("[QCL-cacio] 已用系统文件选择器作答");
                    super.setVisible(true);
                    scheduleAutoClose();
                    return;
                }
                name = pickLoadName(dir);
            }
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
        scheduleAutoClose();
    }

    /** 让游戏那边的模态循环正常返回：稍后把对话框收起来。 */
    private void scheduleAutoClose() {
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

    /**
     * ★★ 让玩家自己挑文件：打开启动器自带的文件浏览器（或系统选择器）。
     *
     * <p><b>关键：绝对不能在游戏线程里等选择结果。</b>
     * 这个 setVisible 是在游戏的「文件对话框线程」里被调用的，而**游戏主循环靠主线程跑**；
     * 一旦在这里阻塞等待，界面就不再刷新、选择结果也回不来，
     * 最后只能超时回退成"自动选最新存档"（用户看到的还是"它自己就进去了"）。
     *
     * <p>所以这里只负责"弹出来"：把本次是保存还是读档记进系统属性，
     * 选择结果由 {@code PojavMinecraftActivity.onActivityResult} 写进 {@code qcl.pickedfile}，
     * 游戏侧每帧的 {@code c.e.b()} 会取走并接着做保存/读档。
     *
     * @return true 表示已经交给文件浏览器处理（对话框可以收起来了）
     */
    private boolean tryNativePicker(FileDialog fd) {
        // ★★★ 1.4.8 实测结论：**这条路走不通，直接不试。**
        //   游戏跑在独立 JVM 里，看不到安卓的类（实测日志：
        //   ClassNotFoundException: android/app/ActivityThread），
        //   所以游戏进程里无法 startActivityForResult —— 这是 Pojav 架构的硬限制，
        //   基于 Pojav 的启动器（含 FCL）都只能在启动器自己的界面里选文件。
        //   要"自己挑存档文件"，请用启动器「存档」页的「导入世界」按钮
        //   （1.4.8 已支持选 .mclevel / .mcworld）。
        //   游戏内那个「加载文件…」按钮改成：把手机下载目录里现成的 .mclevel 收进存档目录。
        if (true) {
            return false;
        }
        if ("1".equals(System.getProperty("qclPickerBusy"))) {
            // 已经弹过、正在等玩家选择 —— 别再弹第二个
            return true;
        }
        Object activity = null;
        try {
            activity = findActivity();
        } catch (Throwable t) {
            System.out.println("[QCL-cacio] 找 Activity 失败：" + t);
        }
        if (activity == null) {
            System.out.println("[QCL-cacio] 找不到 Activity，无法弹文件浏览器");
            return false;
        }
        boolean wantSave = fd.getMode() == FileDialog.SAVE;
        System.setProperty("qclWantSave", wantSave ? "1" : "0");
        System.setProperty("qclPickerBusy", "1");
        System.out.println("[QCL-cacio] 弹出文件浏览器（" + (wantSave ? "保存" : "读档") + "）…");
        if (!launchPicker(activity, null)) {
            System.setProperty("qclPickerBusy", "0");
            return false;
        }
        return true;
    }

    /** 选择结果容器：在安卓 UI 线程被填，在游戏线程被读。 */
    static final class PickerResult implements java.util.concurrent.Callable<String> {
        volatile String path;
        private final Object lock = new Object();

        void set(String p) {
            path = p;
            synchronized (lock) {
                lock.notifyAll();
            }
        }

        void await(long ms) {
            long end = System.currentTimeMillis() + ms;
            synchronized (lock) {
                while (path == null) {
                    long left = end - System.currentTimeMillis();
                    if (left <= 0) {
                        return;
                    }
                    try {
                        lock.wait(Math.min(left, 1000L));
                    } catch (InterruptedException ignored) {
                        return;
                    }
                }
            }
        }

        /** 给启动器侧 onActivityResult 用：拿到就填进来。 */
        @Override
        public String call() {
            return path;
        }
    }

    /** 找到当前 Activity（游戏跑在自己的进程里，用 ActivityThread 反查）。 */
    private static Object findActivity() throws Exception {
        Class<?> at = Class.forName("android.app.ActivityThread");
        Object thread = at.getMethod("currentActivityThread").invoke(null);
        if (thread == null) {
            return null;
        }
        try {
            java.lang.reflect.Field f = at.getDeclaredField("mActivities");
            f.setAccessible(true);
            Object map = f.get(thread);
            if (map instanceof java.util.Map) {
                for (Object rec : ((java.util.Map<?, ?>) map).values()) {
                    if (rec == null) {
                        continue;
                    }
                    java.lang.reflect.Field af = rec.getClass().getDeclaredField("activity");
                    af.setAccessible(true);
                    Object act = af.get(rec);
                    if (act != null) {
                        return act;
                    }
                }
            }
        } catch (Throwable t) {
            System.out.println("[QCL-cacio] 遍历 mActivities 失败：" + t);
        }
        return null;
    }

    /** 真正弹出文件浏览器。优先用启动器自带的那个（一定有、且只列存档文件）。 */
    private static boolean launchPicker(Object activity, PickerResult result) {
        PickerHook.set(result);
        Class<?> actC;
        Class<?> intentC;
        try {
            actC = Class.forName("android.app.Activity");
            intentC = Class.forName("android.content.Intent");
        } catch (Throwable t) {
            System.out.println("[QCL-cacio] 拿不到 android 类：" + t);
            PickerHook.set(null);
            return false;
        }
        // ① 启动器自带的文件浏览器：一定能用，而且直接列出 .mclevel
        try {
            Object intent = intentC.getConstructor().newInstance();
            intentC.getMethod("setClassName", String.class, String.class).invoke(intent,
                    activity.getClass().getPackage().getName(),
                    "com.qcl.launcher.launcher.launch.pojav.LevelFileChooserActivity");
            actC.getMethod("startActivityForResult", intentC, int.class)
                    .invoke(activity, intent, Integer.valueOf(PickerHook.REQUEST_CODE));
            System.out.println("[QCL-cacio] 已打开内置文件浏览器（LevelFileChooserActivity）");
            return true;
        } catch (Throwable t) {
            System.out.println("[QCL-cacio] 内置文件浏览器打不开，改用系统选择器：" + t);
        }
        // ② 兜底：系统文件选择器
        try {
            Object intent = intentC.getConstructor(String.class).newInstance("android.intent.action.GET_CONTENT");
            intentC.getMethod("addCategory", String.class).invoke(intent, "android.intent.category.OPENABLE");
            intentC.getMethod("setType", String.class).invoke(intent, "*/*");
            actC.getMethod("startActivityForResult", intentC, int.class)
                    .invoke(activity, intent, Integer.valueOf(PickerHook.REQUEST_CODE));
            System.out.println("[QCL-cacio] 已打开系统文件选择器");
            return true;
        } catch (Throwable t) {
            System.out.println("[QCL-cacio] startActivityForResult 失败：" + t);
            t.printStackTrace();
            PickerHook.set(null);
            return false;
        }
    }

    /** 让启动器侧的 onActivityResult 能把结果交回来。 */
    public static final class PickerHook {
        /** 请求码（和启动器侧约定一致）。 */
        public static final int REQUEST_CODE = 0x0C1F;
        private static volatile PickerResult current;

        static void set(PickerResult r) {
            current = r;
        }

        /** 启动器侧 onActivityResult 调用它把路径填回来。 */
        public static void deliver(String path) {
            PickerResult r = current;
            if (r != null) {
                r.set(path);
            }
        }

        public static boolean hasPending() {
            return current != null;
        }
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
     * 目录里如果**只有一个** .mclevel，返回它的完整文件名（含后缀）；否则返回 null。
     * <p>远古版本通常就一个世界，直接沿用可以让「同一个世界反复保存」不会每存一次多出一个文件。
     */
    private static String onlyMcLevelName(String dir) {
        File[] list = new File(dir).listFiles();
        if (list == null) {
            return null;
        }
        String found = null;
        int count = 0;
        for (File f : list) {
            if (f.isFile() && f.getName().toLowerCase().endsWith(".mclevel")) {
                count++;
                found = f.getName();
                if (count > 1) {
                    return null;
                }
            }
        }
        return count == 1 ? found : null;
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
