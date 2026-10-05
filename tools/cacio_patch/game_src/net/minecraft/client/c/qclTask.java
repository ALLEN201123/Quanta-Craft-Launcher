package net.minecraft.client.c;

/**
 * 「载入文件…／保存文件…」的等待线程载体。
 *
 * <p>把 {@code run()} 转发给补丁注入到界面类上的**实例方法**：
 * {@code qclWaitLoad()}（读档）或 {@code qclWaitSave()}（保存）。
 * 真正的等待循环、字符串处理、字段写入全部在那两个方法的字节码里（界面类自身），
 * 同一个类、同一个加载器，直接读写 {@code this.o}，不存在跨类可见性问题。
 *
 * <p><b>为什么这里用反射</b>：本类包名是 {@code net.minecraft.client.c}，
 * javac 会把包名里的 {@code c} 当类型名参与解析（"包与类型同名"），
 * 编译期无法引用游戏类 {@code e}。这是 javac 的硬限制（已实测确认）。
 * 运行期反射查找做过加固：多路查找 + 重试。
 *
 * <p>本类由构建脚本用 JDK8（{@code -source 1.6}）编译，
 * 与补丁类一起打进 {@code qcl_saves_<版本>.jar}。
 */
public class qclTask extends Thread {

    private final Object self;
    private final String mode;

    public qclTask(Object self, String mode) {
        this.self = self;
        this.mode = mode;
        setDaemon(true);
        setName("QCL-task-" + mode);
    }

    @Override
    public void run() {
        // "load" → qclWaitLoad()；"name"/"save" → qclWaitSave()
        String method = "load".equals(mode) ? "qclWaitLoad" : "qclWaitSave";
        Object target = self;
        Class<?> cls = null;
        // 首选：从屏幕类的公开静态字段 qclSelf 取当前屏幕实例
        // （静态方法里启动线程时拿不到 this，只能这么取）
        try {
            Class<?> screen = Class.forName("net.minecraft.client.c.e");
            java.lang.reflect.Field sf = screen.getField("qclSelf");
            Object got = sf.get(null);
            if (got != null) {
                target = got;
            }
            cls = target.getClass();
        } catch (Throwable t) {
            System.out.println("[QCL-saves] 取 qclSelf 失败: " + t);
        }
        if (cls == null) {
            cls = target.getClass();
        }
        for (int tries = 0; tries < 10; tries++) {
            java.lang.reflect.Method w = resolve(cls, method);
            if (w != null) {
                try {
                    w.setAccessible(true);
                    w.invoke(target);
                    return;
                } catch (Throwable t) {
                    System.out.println("[QCL-saves] 调用 " + method + " 失败: " + t);
                    return;
                }
            }
            try {
                Thread.sleep(300L);
            } catch (InterruptedException ie) {
                return;
            }
        }
        System.out.println("[QCL-saves] 找不到 " + method + "（class=" + cls.getName()
                + " loader=" + cls.getClassLoader() + "）");
    }

    /** 多路查找目标方法（getMethod / getDeclaredMethod / 遍历 getMethods）。 */
    private static java.lang.reflect.Method resolve(Class<?> cls, String name) {
        try {
            return cls.getMethod(name);
        } catch (Throwable ignored) {
        }
        try {
            return cls.getDeclaredMethod(name);
        } catch (Throwable ignored) {
        }
        try {
            java.lang.reflect.Method[] all = cls.getMethods();
            for (int i = 0; i < all.length; i++) {
                if (name.equals(all[i].getName()) && all[i].getParameterTypes().length == 0) {
                    return all[i];
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
