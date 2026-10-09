package qcl;

import java.lang.reflect.Field;

/**
 * ★ 2026-10-01 诊断探针 v2（内窥镜）：监视 lwjglx 的鼠标内部状态 + MouseInfo。
 *   - Mouse.dx / dy（累计增量，读后清——这里用反射不干扰）
 *   - Mouse.absolute_x / absolute_y / x / y
 *   - GLFWInputImplementation.mouseX/mouseY/mouseLastX/mouseLastY/grab
 *   - MouseInfo.getPointerInfo（游戏实际读的 Cacio 光标位置）
 *   验证完成后删除本类与触发点。
 */
public class MouseProbe {
    public static void start() {
        System.out.println("[QCL_MOUSE_PROBE] v2 start()");
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Class<?> mc = Class.forName("org.lwjgl.input.Mouse");
                    Field fDx = mc.getDeclaredField("dx");
                    Field fDy = mc.getDeclaredField("dy");
                    Field fX = mc.getDeclaredField("x");
                    Field fY = mc.getDeclaredField("y");
                    Field fAx = mc.getDeclaredField("absolute_x");
                    Field fAy = mc.getDeclaredField("absolute_y");
                    Field fImpl = mc.getDeclaredField("implementation");
                    for (Field f : new Field[]{fDx, fDy, fX, fY, fAx, fAy, fImpl}) {
                        f.setAccessible(true);
                    }
                    Class<?> gic = Class.forName("org.lwjgl.input.GLFWInputImplementation");
                    Field fMx = gic.getDeclaredField("mouseX");
                    Field fMy = gic.getDeclaredField("mouseY");
                    Field fMlx = gic.getDeclaredField("mouseLastX");
                    Field fMly = gic.getDeclaredField("mouseLastY");
                    Field fGrab = gic.getDeclaredField("grab");
                    for (Field f : new Field[]{fMx, fMy, fMlx, fMly, fGrab}) {
                        f.setAccessible(true);
                    }
                    // 初值打印一次
                    Thread.sleep(3000);
                    String lastLine = "";
                    int errCount = 0;
                    while (true) {
                        try {
                            Object impl = fImpl.get(null);
                            String line = "dx=" + fDx.getInt(null)
                                    + " dy=" + fDy.getInt(null)
                                    + " x=" + fX.getInt(null)
                                    + " y=" + fY.getInt(null)
                                    + " absX=" + fAx.getInt(null)
                                    + " absY=" + fAy.getInt(null)
                                    + " mX=" + (impl != null ? fMx.getInt(impl) : -1)
                                    + " mLastX=" + (impl != null ? fMlx.getInt(impl) : -1)
                                    + " mY=" + (impl != null ? fMy.getInt(impl) : -1)
                                    + " mLastY=" + (impl != null ? fMly.getInt(impl) : -1)
                                    + " grab=" + (impl != null ? fGrab.getBoolean(impl) : false);
                            java.awt.Point p = null;
                            try {
                                p = java.awt.MouseInfo.getPointerInfo().getLocation();
                                line += " MI=" + p.x + "," + p.y;
                            } catch (Throwable ignored) {
                                line += " MI=?";
                            }
                            if (!line.equals(lastLine)) {
                                System.out.println("[QCL_MOUSE_PROBE2] " + line);
                                lastLine = line;
                            }
                        } catch (Throwable t) {
                            if (errCount++ < 8) {
                                System.out.println("[QCL_MOUSE_PROBE2] err: " + t);
                                t.printStackTrace();
                            }
                        }
                        try {
                            Thread.sleep(50);
                        } catch (Throwable t) {
                            // 忽略
                        }
                    }
                } catch (Throwable t) {
                    System.out.println("[QCL_MOUSE_PROBE2] init err: " + t);
                    t.printStackTrace();
                }
            }
        }, "QCLMouseProbe");
        th.setDaemon(true);
        th.start();
    }
}
