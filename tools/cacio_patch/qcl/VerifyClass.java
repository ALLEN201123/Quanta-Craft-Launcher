package qcl;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import org.objectweb.asm.tree.analysis.Frame;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 逐方法做数据流校验（等价于 JVM 对 major version 49 类做的老式校验）。
 * 用法: VerifyClass <游戏jar> <被校验的class文件或目录> [类名...]
 */
public final class VerifyClass {
    public static void main(String[] args) throws Exception {
        File gameJar = new File(args[0]);
        File target = new File(args[1]);
        String[] names = args.length > 2 ? java.util.Arrays.copyOfRange(args, 2, args.length)
                : new String[]{"net.minecraft.client.c.e"};

        URLClassLoader cl = new URLClassLoader(
                new URL[]{gameJar.toURI().toURL()},
                VerifyClass.class.getClassLoader());

        int bad = 0;
        for (String name : names) {
            String res = name.replace('.', '/') + ".class";
            byte[] bytes = null;
            if (target.isDirectory()) {
                File f = new File(target, res);
                if (f.isFile()) {
                    try (InputStream in = new FileInputStream(f)) {
                        bytes = in.readAllBytes();
                    }
                }
            }
            if (bytes == null) {
                try (ZipFile z = new ZipFile(gameJar)) {
                    ZipEntry e = z.getEntry(res);
                    if (e != null) {
                        try (InputStream in = z.getInputStream(e)) {
                            bytes = in.readAllBytes();
                        }
                    }
                }
            }
            if (bytes == null) {
                System.out.println("SKIP  " + name + "（找不到字节码）");
                continue;
            }
            ClassNode cn = new ClassNode();
            new ClassReader(bytes).accept(cn, 0);
            System.out.println("=== " + name + " (major=" + cn.version + ") ===");
            for (MethodNode mn : cn.methods) {
                Analyzer<org.objectweb.asm.tree.analysis.BasicValue> a =
                        new Analyzer<>(new BasicVerifier());
                try {
                    a.analyze(cn.name, mn);
                    System.out.println("  PASS  " + mn.name + mn.desc);
                } catch (Throwable t) {
                    bad++;
                    System.out.println("  FAIL  " + mn.name + mn.desc + " :: " + t.getMessage());
                    // 逐条打印指令与帧（定位"空栈"那一条）
                    org.objectweb.asm.tree.AbstractInsnNode[] insns = mn.instructions.toArray();
                    int shown = 0;
                    for (int i = 0; i < insns.length && shown < 14; i++) {
                        String fs;
                        try {
                            fs = a.getFrames() != null && i < a.getFrames().length && a.getFrames()[i] != null
                                    ? ("栈深=" + a.getFrames()[i].getStackSize() + " 局部=" + a.getFrames()[i].getLocals())
                                    : "无帧";
                        } catch (Throwable t2) {
                            fs = "?";
                        }
                        System.out.println("        [" + i + "] " + insns[i] + "   " + fs);
                        shown++;
                    }
                }
            }
        }
        System.out.println(bad == 0 ? "全部通过" : ("有 " + bad + " 个方法失败"));
    }
}
