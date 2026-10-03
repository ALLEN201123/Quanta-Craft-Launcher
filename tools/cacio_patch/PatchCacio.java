import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * 给 cacio 的 CacioFileDialogPeer 打三个 null 保护补丁。
 *
 * 背景：indev / infdev 的「保存世界」「读取世界」用的是 AWT FileDialog，
 * 而 cacio 的 CacioFileDialogPeer.postInitSwingComponent() 会直接
 *   setFile(fd.getFile())        → new File((String) null) → NPE
 *   setDirectory(fd.getDirectory()) → new File((String) null) → NPE
 *   setFilenameFilter(fd.getFilenameFilter()) → 浏览目录时 NPE
 * 游戏创建 FileDialog 时通常不会先调这些方法，返回值就是 null，
 * 于是对话框建不起来（点保存/读取毫无反应，日志里反复刷同一个 NPE）。
 *
 * 本工具**不重编译**（java.awt.peer.FileDialogPeer 是内部 API，本地没有 Java 8 JDK），
 * 直接用 ASM 在这三个方法开头插入 null 判断，再把改好的类塞进 ResConfHack.jar
 * （它在 -Xbootclasspath/p 里且排在 cacio-shared 之前，会优先被加载）。
 */
public class PatchCacio {
    static final String CLS = "sun/awt/peer/cacio/CacioFileDialogPeer.class";

    public static void main(String[] args) throws Exception {
        Path cacioJar = Paths.get(args[0]);
        Path resConfJar = Paths.get(args[1]);

        byte[] orig;
        try (ZipFile zf = new ZipFile(cacioJar.toFile())) {
            ZipEntry e = zf.getEntry(CLS);
            if (e == null) {
                System.out.println("!! cacio jar 里没有 " + CLS);
                return;
            }
            try (InputStream in = zf.getInputStream(e)) {
                orig = readAll(in);
            }
        }

        ClassNode cn = new ClassNode();
        new ClassReader(orig).accept(cn, 0);

        int patched = 0;
        for (MethodNode mn : cn.methods) {
            LabelNode end = new LabelNode();
            InsnList g = new InsnList();
            if ("setFile".equals(mn.name) && "(Ljava/lang/String;)V".equals(mn.desc)) {
                // if (file == null) return;
                g.add(new VarInsnNode(Opcodes.ILOAD, 1));
                g.add(new JumpInsnNode(Opcodes.IFNULL, end));
                mn.instructions.insert(g);
                mn.instructions.add(end);
                patched++;
            } else if ("setDirectory".equals(mn.name) && "(Ljava/lang/String;)V".equals(mn.desc)) {
                // if (dir == null) dir = System.getProperty("user.home");
                // if (dir == null) return;
                LabelNode hasDir = new LabelNode();
                g.add(new VarInsnNode(Opcodes.ILOAD, 1));
                g.add(new JumpInsnNode(Opcodes.IFNONNULL, hasDir));
                g.add(new LdcInsnNode("user.home"));
                g.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/System", "getProperty",
                        "(Ljava/lang/String;)Ljava/lang/String;", false));
                g.add(new VarInsnNode(Opcodes.ASTORE, 1));
                g.add(new VarInsnNode(Opcodes.ALOAD, 1));
                g.add(new JumpInsnNode(Opcodes.IFNULL, end));
                g.add(hasDir);
                mn.instructions.insert(g);
                mn.instructions.add(end);
                patched++;
            } else if ("setFilenameFilter".equals(mn.name)
                    && "(Ljava/io/FilenameFilter;)V".equals(mn.desc)) {
                // if (filter == null) return;
                g.add(new VarInsnNode(Opcodes.ILOAD, 1));
                g.add(new JumpInsnNode(Opcodes.IFNULL, end));
                mn.instructions.insert(g);
                mn.instructions.add(end);
                patched++;
            }
        }
        System.out.println("已加 null 保护的方法数: " + patched);

        ClassWriter cw = new ClassWriter(0);
        cn.accept(cw);
        byte[] fixed = cw.toByteArray();

        // 校验：能重新读回来，且三个方法都在
        ClassNode check = new ClassNode();
        new ClassReader(fixed).accept(check, 0);
        for (MethodNode mn : check.methods) {
            if (mn.name.startsWith("set")) {
                System.out.println("  保留方法: " + mn.name + mn.desc);
            }
        }

        // 写进 ResConfHack.jar（替换同名条目；顺带清掉签名文件，避免签名失效导致 jar 被拒）
        boolean replaced = false;
        boolean hadSignature = false;
        try (ZipFile zf = new ZipFile(resConfJar.toFile());
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            try (ZipOutputStream zo = new ZipOutputStream(bos)) {
                java.util.Enumeration<? extends ZipEntry> en = zf.entries();
                while (en.hasMoreElements()) {
                    ZipEntry e = en.nextElement();
                    String n = e.getName();
                    if (n.startsWith("META-INF/") && (n.endsWith(".SF") || n.endsWith(".RSA") || n.endsWith(".DSA"))) {
                        hadSignature = true;
                        continue;
                    }
                    if (n.equals(CLS)) {
                        replaced = true;
                        continue;
                    }
                    zo.putNextEntry(new ZipEntry(n));
                    try (InputStream in = zf.getInputStream(e)) {
                        in.transferTo(zo);
                    }
                }
                zo.putNextEntry(new ZipEntry(CLS));
                zo.write(fixed);
            }
            Files.write(resConfJar, bos.toByteArray());
        }
        System.out.println("ResConfHack.jar: 替换同名类=" + replaced + "  移除过签名文件=" + hadSignature);
        System.out.println("补丁后 class 字节数: " + fixed.length + "（原 " + orig.length + "）");
    }

    static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }
}
