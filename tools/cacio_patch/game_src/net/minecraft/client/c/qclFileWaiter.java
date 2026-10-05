package net.minecraft.client.c;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * 「载入文件…」按钮的等待线程。
 *
 * <p>玩家的操作是异步的：游戏内点了「载入文件…」之后，由启动器侧的
 * {@code QclFileBridge} 弹出系统文件选择器，玩家挑完才把路径写进响应文件。
 * 这个线程负责在那儿等着，拿到路径后把存档目标设上，剩下的交给游戏
 * 原版每帧的读档逻辑（{@code e.f_()}）去真正打开那个存档。
 *
 * <p>为什么不放在每帧的方法里判断：{@code f_()} 每帧都跑，往那里注入任何东西
 * 都容易变成"没点按钮也自动读档"（这个坑已经踩过一次）。
 *
 * <p><b>两个刻意的写法</b>：
 * <ul>
 *   <li>owner 用 {@code Object} 而不是 {@code e} —— 否则编译本类就必须把游戏 jar
 *       放进 classpath，而那个 jar 里有 {@code net.minecraft.client.c} 这个包，
 *       它与 {@code c} 作为类型名的解析规则冲突，javac 会直接报"包与类型同名"。</li>
 *   <li>写 {@code o} 用反射 —— 它是 private。</li>
 * </ul>
 *
 * <p><b>注意</b>：本类由构建脚本用 JDK8（{@code -source 7}）编译，与补丁类一起
 * 打进 {@code qcl_saves_<版本>.jar}，不要放进游戏原 jar。
 */
public class qclFileWaiter extends Thread {

    /** 信箱目录（必须与启动器侧 QclFileBridge.DIR 一致）。 */
    private static final String MAILBOX = "/sdcard/QCL/.qcl_bridge";
    /** 等待上限：约 2 分钟（玩家在文件选择器里慢慢翻也够）。 */
    private static final int MAX_TRIES = 240;

    private final Object owner;

    public qclFileWaiter(Object owner) {
        this.owner = owner;
        setDaemon(true);
        setName("QCL-filewaiter");
    }

    @Override
    public void run() {
        File res = new File(MAILBOX + "/res.txt");
        for (int i = 0; i < MAX_TRIES; i++) {
            try {
                Thread.sleep(500L);
            } catch (InterruptedException ie) {
                return;
            }
            if (!res.isFile()) {
                continue;
            }
            String path = readAll(res);
            //noinspection ResultOfMethodCallIgnored
            res.delete();
            if (path == null) {
                continue;
            }
            path = path.trim();
            if (path.length() == 0) {
                System.out.println("[QCL-saves] 玩家取消了文件选择");
                return;
            }
            File f = new File(path);
            if (!f.isFile()) {
                System.out.println("[QCL-saves] 选择的文件不存在: " + path);
                return;
            }
            System.out.println("[QCL-saves] 玩家选择了: " + path);
            assign(f);
            return;
        }
        System.out.println("[QCL-saves] 等待文件选择超时");
    }

    /**
     * 把目标文件交给游戏。
     *
     * <p>做法：把文件交给补丁注入的静态字段并调用补丁注入的方法，让它搬到游戏的
     * 目标字段上；随后原版每帧逻辑就会读这个存档。
     *
     * <p><b>为什么全部走反射</b>：本类的包名是 {@code net.minecraft.client.c}，
     * 而 {@code c} 在 javac 里会被当成类型名解析（"包与类型同名"），
     * 所以编译期没法引用游戏里的类；反射还顺带避免了对具体类名的硬依赖。
     */
    private void assign(File f) {
        Class<?> cls = owner.getClass();
        try {
            java.lang.reflect.Field pend = cls.getField("qclPending");
            pend.set(null, f);
            java.lang.reflect.Method apply = cls.getMethod("qclApplyPending", cls);
            apply.invoke(null, owner);
            System.out.println("[QCL-saves] 已交给游戏: " + f);
        } catch (Throwable t) {
            System.out.println("[QCL-saves] 交给游戏失败: " + t);
        }
    }

    private static String readAll(File f) {
        FileInputStream in = null;
        try {
            in = new FileInputStream(f);
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
        } catch (IOException ioe) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
