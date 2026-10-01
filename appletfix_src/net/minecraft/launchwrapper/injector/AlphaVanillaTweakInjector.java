package net.minecraft.launchwrapper.injector;

import java.applet.Applet;
import java.applet.AppletStub;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JPanel;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

/**
 * QCL 1.4.0 修复版（由 launchwrapper 原版字节码 1:1 重建 + 尺寸修复）。
 *
 * <p>背景：classic / indev 等远古版本以 applet 形式启动，其「窗口尺寸」完全
 * 取自本类创建的 AWT Frame（applet 容器）。原版把容器硬编码为
 * {@code new Dimension(854, 480)}（Minecraft 默认小窗口），而这些老版本
 * 自身没有任何「把窗口调大」的代码（beta 1.7.3 起才有读 options.txt
 * overrideWidth/overrideHeight 自调整的逻辑），结果是游戏画面永远只渲染在
 * 屏幕角落的一小块（854x480），周围大面积空白。</p>
 *
 * <p>修复：把容器尺寸改为动态读取 —— 优先使用启动器通过系统属性注入的真实
 * 全屏尺寸（qcl.applet.width / qcl.applet.height），其次向 AWT/Cacio 询问屏幕
 * 尺寸，最后才退回原版的 854x480。这样走 applet 路径的远古版本（classic /
 * indev / infdev / alpha / pre-classic）启动即按全屏尺寸渲染。</p>
 *
 * <p>除尺寸外，本类其余行为与 launchwrapper 1.5/1.6 中的原版完全一致：
 * main() 的 applet 类探测与工作目录字段修补、Frame/LauncherFake(AppletStub)
 * 的组装顺序、applet.init/start、关闭钩子（stop applet）与
 * VanillaTweakInjector.loadIconsOnFrames() 调用均保持不变。</p>
 */
public class AlphaVanillaTweakInjector implements IClassTransformer {

    public AlphaVanillaTweakInjector() {
    }

    /** 与原始实现一致：本类作为 launch target 使用，不做任何字节码变换。 */
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        return bytes;
    }

    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, java.lang.reflect.InvocationTargetException, InstantiationException {
        Class<?> appletClass;
        try {
            appletClass = getaClass("net.minecraft.client.MinecraftApplet");
        } catch (ClassNotFoundException e) {
            appletClass = getaClass("com.mojang.minecraft.MinecraftApplet");
        }
        System.out.println("AlphaVanillaTweakInjector.class.getClassLoader() = " + AlphaVanillaTweakInjector.class.getClassLoader());
        Constructor<?> constructor = appletClass.getConstructor(new Class[0]);
        Object appletInstance = constructor.newInstance(new Object[0]);
        Field[] declaredFields = appletClass.getDeclaredFields();
        int length = declaredFields.length;
        for (int i = 0; i < length; i++) {
            Field field = declaredFields[i];
            String typeName = field.getType().getName();
            if (typeName.contains("awt") || typeName.contains("java") || typeName.equals("long")) {
                continue;
            }
            System.out.println("Found likely Minecraft candidate: " + field);
            Field workingDirField = getWorkingDirField(typeName);
            if (workingDirField != null) {
                System.out.println("Found File, changing to " + Launch.minecraftHome);
                workingDirField.setAccessible(true);
                workingDirField.set(null, Launch.minecraftHome);
                break;
            }
        }
        startMinecraft((Applet) appletInstance, args);
    }

    /**
     * ★ QCL 修复：计算 applet 容器（AWT Frame）应使用的尺寸。
     *
     * <p>优先级：① 启动器注入的系统属性 qcl.applet.width / qcl.applet.height
     * （即真实渲染 surface 的全屏尺寸，最准确）；② AWT/Cacio 报告的屏幕尺寸
     * （-Dcacio.managed.screensize 已由启动器设置为全屏）；③ 原版兜底 854x480。</p>
     */
    private static Dimension qclAppletDimension() {
        int width = Integer.getInteger("qcl.applet.width", -1).intValue();
        int height = Integer.getInteger("qcl.applet.height", -1).intValue();
        if (width <= 0 || height <= 0) {
            try {
                Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
                if (screen != null && screen.width > 0 && screen.height > 0) {
                    width = screen.width;
                    height = screen.height;
                }
            } catch (Throwable ignored) {
                // Cacio/Toolkit 不可用时保持原版兜底
            }
        }
        if (width <= 0 || height <= 0) {
            width = 854;
            height = 480;
        }
        return new Dimension(width, height);
    }

    private static void startMinecraft(Applet applet, String[] args) {
        Map<String, String> params = new HashMap<String, String>();
        String username = "Player" + (System.currentTimeMillis() % 1000L);
        if (args.length > 0) {
            username = args[0];
        }
        String sessionid = "-";
        if (args.length > 1) {
            sessionid = args[1];
        }
        params.put("username", username);
        params.put("sessionid", sessionid);

        Frame frame = new Frame();
        frame.setTitle("Minecraft");
        frame.setBackground(Color.BLACK);

        JPanel panel = new JPanel();
        frame.setLayout(new BorderLayout());
        // ★★★ QCL 修复点：原版此处为 panel.setPreferredSize(new Dimension(854, 480))，
        // 修改为动态全屏尺寸（见 qclAppletDimension()）。
        panel.setPreferredSize(qclAppletDimension());
        frame.add(panel, "Center");
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(1);
            }
        });

        class LauncherFake extends Applet implements AppletStub {
            private final Map<String, String> params;

            LauncherFake(Map<String, String> params) {
                this.params = params;
            }

            public void appletResize(int width, int height) {
            }

            public boolean isActive() {
                return true;
            }

            public URL getDocumentBase() {
                try {
                    return new URL("http://www.minecraft.net/game/");
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            public URL getCodeBase() {
                try {
                    return new URL("http://www.minecraft.net/game/");
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            public String getParameter(String name) {
                if (this.params.containsKey(name)) {
                    return this.params.get(name);
                }
                System.err.println("Client asked for parameter:" + name);
                return null;
            }
        }

        LauncherFake launcherFake = new LauncherFake(params);
        applet.setStub(launcherFake);
        launcherFake.setLayout(new BorderLayout());
        launcherFake.add(applet, "Center");
        launcherFake.validate();

        frame.removeAll();
        frame.setLayout(new BorderLayout());
        frame.add(launcherFake, "Center");
        frame.validate();

        applet.init();
        applet.start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                applet.stop();
            }
        });
        VanillaTweakInjector.loadIconsOnFrames();
    }

    private static Class<?> getaClass(String name) throws ClassNotFoundException {
        return Launch.classLoader.findClass(name);
    }

    private static Field getWorkingDirField(String name) throws ClassNotFoundException {
        Class<?> clazz = getaClass(name);
        Field[] declaredFields = clazz.getDeclaredFields();
        int length = declaredFields.length;
        for (int i = 0; i < length; i++) {
            Field field = declaredFields[i];
            if (Modifier.isStatic(field.getModifiers()) && field.getType().getName().equals("java.io.File")) {
                return field;
            }
        }
        return null;
    }
}
