package net.minecraft.client.c;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 「保存文件…」按钮的等待线程。
 *
 * <p>与 {@link qclFileWaiter} 的区别：存盘要的是**一个文件**，而这个文件在玩家选目录之前
 * 还不存在。所以流程是：玩家在游戏里输了世界名 → 点「保存文件…」→ 启动器弹出文件夹选择器
 * → 玩家选目录 → 本线程把「目录 + 世界名」拼成目标文件，设进 {@code e.o}，
 * 再由游戏原版每帧的落盘逻辑把世界写进去。
 *
 * <p>owner 用 {@code Object} + 反射写 {@code o}：理由同 {@link qclFileWaiter}
 * （避免把游戏 jar 放进编译 classpath —— 里面的 {@code net.minecraft.client.c} 包名
 * 与 {@code c} 作为类型名的新解析规则冲突）。
 *
 * <p><b>注意</b>：本类由构建脚本用 JDK8（{@code -source 1.6}）编译，
 * 与补丁类一起打进 {@code qcl_saves_<版本>.jar}。
 */
public class qclFileSaver extends Thread {

    /** 信箱目录（必须与启动器侧 QclFileBridge.DIR 一致）。 */
    private static final String MAILBOX = "/sdcard/QCL/.qcl_bridge";
    /** 等待上限：约 2 分钟。 */
    private static final int MAX_TRIES = 240;

    private final Object owner;
    /** 玩家的世界名。第一轮从输入框拿到后存起来，供第二轮拼文件名。 */
    private String worldName;

    public qclFileSaver(Object owner, String worldName) {
        this.owner = owner;
        this.worldName = worldName;
        setDaemon(true);
        setName("QCL-filesaver");
    }

    @Override
    public void run() {
        File res = new File(MAILBOX + "/res.txt");
        int step = 0;   // 0 = 等世界名；1 = 等保存目录
        for (int i = 0; i < MAX_TRIES && step < 2; i++) {
            try {
                Thread.sleep(500L);
            } catch (InterruptedException ie) {
                return;
            }
            if (!res.isFile()) {
                continue;
            }
            String val = readAll(res);
            //noinspection ResultOfMethodCallIgnored
            res.delete();
            if (val == null) {
                continue;
            }
            val = val.trim();
            if (step == 0) {
                if (val.length() == 0) {
                    System.out.println("[QCL-saves] 玩家取消了命名");
                    return;
                }
                worldName = val;
                System.out.println("[QCL-saves] 世界名 = " + worldName + "，接着要保存位置");
                // ★ 第二轮：请启动器弹出文件夹选择器
                if (!writeRequest("save")) {
                    System.out.println("[QCL-saves] 无法请求保存位置");
                    return;
                }
                step = 1;
                continue;
            }
            // step == 1：拿到保存目录
            if (val.length() == 0) {
                System.out.println("[QCL-saves] 玩家取消了选择保存位置");
                return;
            }
            File target = buildTarget(val);
            System.out.println("[QCL-saves] 保存到: " + target.getAbsolutePath());
            assign(target);
            return;
        }
        System.out.println("[QCL-saves] 保存流程超时");
    }

    /** 写请求文件（第二轮请求保存目录）。 */
    private boolean writeRequest(String mode) {
        java.io.FileWriter w = null;
        try {
            w = new java.io.FileWriter(MAILBOX + "/req.txt");
            w.write(mode);
            return true;
        } catch (java.io.IOException ioe) {
            System.out.println("[QCL-saves] 写请求失败: " + ioe);
            return false;
        } finally {
            if (w != null) {
                try {
                    w.close();
                } catch (java.io.IOException ignored) {
                }
            }
        }
    }

    /** 目录 + 世界名 → 目标文件（补上 .mclevel 后缀）。 */
    private File buildTarget(String dir) {
        String name = worldName == null ? "" : worldName.trim();
        if (name.length() == 0) {
            // 玩家没输名字：用日期当名字，不覆盖已有存档
            name = "世界_" + new SimpleDateFormat("MMdd_HHmm", Locale.US).format(new Date());
        }
        // 去掉文件名里不允许的字符
        name = name.replace('/', '_').replace('\\', '_').replace(':', '_');
        if (!name.toLowerCase(Locale.US).endsWith(".mclevel")) {
            name = name + ".mclevel";
        }
        return new File(dir, name);
    }

    /**
     * 把保存目标交给游戏（理由同 qclFileWaiter：包名 c 导致编译期无法引用游戏类，走反射）。
     */
    private void assign(File f) {
        Class<?> cls = owner.getClass();
        try {
            java.lang.reflect.Field pend = cls.getField("qclPending");
            pend.set(null, f);
            java.lang.reflect.Method apply = cls.getMethod("qclApplyPending", cls);
            apply.invoke(null, owner);
            System.out.println("[QCL-saves] 已交给游戏保存: " + f);
        } catch (Throwable t) {
            System.out.println("[QCL-saves] 交给游戏失败: " + t);
        }
    }

    private static String readAll(File f) {
        java.io.FileInputStream in = null;
        try {
            in = new java.io.FileInputStream(f);
            byte[] buf = new byte[(int) f.length()];
            int off = 0;
            while (off < buf.length) {
                int r = in.read(buf, off, buf.length - off);
                if (r < 0) {
                    break;
                }
                off += r;
            }
            return new String(buf, 0, off, "UTF-8");
        } catch (java.io.IOException ioe) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (java.io.IOException ignored) {
                }
            }
        }
    }
}
