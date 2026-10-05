package qcl;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 给 c.e 注入一个**不做新类名**的辅助方法：<br>
 * {@code private static java.io.File[] qclCollectExternal(File dir)} ——
 * 扫描手机上的 .mclevel 文件（下载目录等），把它们复制进存档目录，
 * 返回复制进来的文件数组。之后载入界面重新填列表就能看到它们。
 *
 * <p>为什么必须这么做：本版本的载入界面点「加载文件…」原本会走文件对话框，
 * 而对话框在安卓上由 cacio 处理（且我们是"替玩家作答"），
 * 那个按钮最终只会把界面关掉、不会真的让玩家选文件。
 * 这里改成"把手机里的存档收进来"，玩家在同一个列表里点选即可 —— 行为可预期、一定能用。
 */
public final class PatchImportFile {

    private static final String TARGET = "net/minecraft/client/c/e";
    private static final String METHOD = "qclCollectExternal";
    private static final String DESC = "(Ljava/io/File;)[Ljava/io/File;";

    public static void main(String[] args) throws Exception {
        File jar = new File(args[0]);
        File outDir = new File(args[1]);

        ClassNode cn = new ClassNode();
        ZipFile zf = new ZipFile(jar);
        try {
            ZipEntry ze = zf.getEntry(TARGET + ".class");
            if (ze == null) {
                System.out.println("   !! jar 里没有 " + TARGET + ".class，跳过导入补丁");
                return;
            }
            InputStream is = zf.getInputStream(ze);
            new ClassReader(is).accept(cn, 0);
            is.close();
        } finally {
            zf.close();
        }

        // 已经有就跳过（幂等）
        for (MethodNode mn : cn.methods) {
            if (METHOD.equals(mn.name) && DESC.equals(mn.desc)) {
                System.out.println("   已存在 " + METHOD + "，跳过");
                return;
            }
        }
        cn.methods.add(build());

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(String t1, String t2) {
                try {
                    return super.getCommonSuperClass(t1, t2);
                } catch (Throwable t) {
                    return "java/lang/Object";
                }
            }
        };
        cn.accept(cw);
        File out = new File(outDir, "net" + File.separator + "minecraft"
                + File.separator + "client" + File.separator + "c" + File.separator + "e.class");
        if (!out.getParentFile().exists() && !out.getParentFile().mkdirs()) {
            throw new IllegalStateException("无法创建输出目录: " + out.getParentFile());
        }
        FileOutputStream fos = new FileOutputStream(out);
        fos.write(cw.toByteArray());
        fos.close();
        System.out.println("   已注入 " + METHOD + "（扫描手机存档并收进存档目录）: " + out.getName());
    }

    /** 生成方法体。用 ASM 的 tree API 逐个指令搭出来。 */
    private static MethodNode build() {
        MethodNode mn = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, METHOD, DESC, null, null);
        InsnList c = mn.instructions;

        // ArrayList<File> found = new ArrayList<File>();
        c.add(new TypeInsnNode(Opcodes.NEW, "java/util/ArrayList"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false));
        c.add(new VarInsnNode(Opcodes.ASTORE, 2));

        // String[] roots = { "/sdcard/Download", "/sdcard/downloads", "/storage/emulated/0/Download" };
        c.add(new InsnNode(Opcodes.ICONST_3));
        c.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));
        String[] roots = {"/sdcard/Download", "/sdcard/downloads", "/storage/emulated/0/Download"};
        for (int i = 0; i < roots.length; i++) {
            c.add(new InsnNode(Opcodes.DUP));
            c.add(iconst(i));
            c.add(new LdcInsnNode(roots[i]));
            c.add(new InsnNode(Opcodes.AASTORE));
        }
        c.add(new VarInsnNode(Opcodes.ASTORE, 3));

        // int i = 0;  while (i < roots.length) { ... }
        c.add(iconst(0));
        c.add(new VarInsnNode(Opcodes.ISTORE, 4));
        LabelNode L_loop = new LabelNode();
        LabelNode L_loopEnd = new LabelNode();
        c.add(L_loop);
        c.add(new VarInsnNode(Opcodes.ILOAD, 4));
        c.add(new VarInsnNode(Opcodes.ALOAD, 3));
        c.add(new InsnNode(Opcodes.ARRAYLENGTH));
        c.add(new JumpInsnNode(Opcodes.IF_ICMPGE, L_loopEnd));

        // File[] list = new File(roots[i]).listFiles();
        c.add(new TypeInsnNode(Opcodes.NEW, "java/io/File"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new VarInsnNode(Opcodes.ALOAD, 3));
        c.add(new VarInsnNode(Opcodes.ILOAD, 4));
        c.add(new InsnNode(Opcodes.AALOAD));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/File", "<init>",
                "(Ljava/lang/String;)V", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "listFiles",
                "()[Ljava/io/File;", false));
        c.add(new VarInsnNode(Opcodes.ASTORE, 5));
        // if (list == null) { i++; continue; }
        LabelNode L_afterNull = new LabelNode();
        LabelNode L_inc = new LabelNode();
        c.add(new VarInsnNode(Opcodes.ALOAD, 5));
        c.add(new JumpInsnNode(Opcodes.IFNONNULL, L_afterNull));
        c.add(new JumpInsnNode(Opcodes.GOTO, L_inc));
        c.add(L_afterNull);

        // int j = 0; while (j < list.length) { ... }
        c.add(iconst(0));
        c.add(new VarInsnNode(Opcodes.ISTORE, 6));
        LabelNode L_inner = new LabelNode();
        LabelNode L_innerEnd = new LabelNode();
        LabelNode L_incInner = new LabelNode();
        LabelNode L_isMc = new LabelNode();
        LabelNode L_targetOk = new LabelNode();
        LabelNode L_copyTry = new LabelNode();
        LabelNode L_copyEnd = new LabelNode();
        LabelNode L_copyCatch = new LabelNode();
        c.add(L_inner);
        c.add(new VarInsnNode(Opcodes.ILOAD, 6));
        c.add(new VarInsnNode(Opcodes.ALOAD, 5));
        c.add(new InsnNode(Opcodes.ARRAYLENGTH));
        c.add(new JumpInsnNode(Opcodes.IF_ICMPGE, L_innerEnd));

        // File f = list[j];
        c.add(new VarInsnNode(Opcodes.ALOAD, 5));
        c.add(new VarInsnNode(Opcodes.ILOAD, 6));
        c.add(new InsnNode(Opcodes.AALOAD));
        c.add(new VarInsnNode(Opcodes.ASTORE, 7));

        // if (!f.isFile()) { j++; continue; }
        LabelNode L_isFile = new LabelNode();
        c.add(new VarInsnNode(Opcodes.ALOAD, 7));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "isFile", "()Z", false));
        c.add(new JumpInsnNode(Opcodes.IFNE, L_isFile));
        c.add(new JumpInsnNode(Opcodes.GOTO, L_incInner));
        c.add(L_isFile);

        // String nm = f.getName().toLowerCase();
        // if (!nm.endsWith(".mclevel")) { j++; continue; }
        c.add(new VarInsnNode(Opcodes.ALOAD, 7));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "getName",
                "()Ljava/lang/String;", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "toLowerCase",
                "()Ljava/lang/String;", false));
        c.add(new VarInsnNode(Opcodes.ASTORE, 8));
        c.add(new VarInsnNode(Opcodes.ALOAD, 8));
        c.add(new LdcInsnNode(".mclevel"));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "endsWith", "(Ljava/lang/String;)Z", false));
        c.add(new JumpInsnNode(Opcodes.IFNE, L_isMc));
        c.add(new JumpInsnNode(Opcodes.GOTO, L_incInner));
        c.add(L_isMc);

        // File target = new File(dir, f.getName());
        c.add(new TypeInsnNode(Opcodes.NEW, "java/io/File"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new VarInsnNode(Opcodes.ALOAD, 0));
        c.add(new VarInsnNode(Opcodes.ALOAD, 7));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "getName",
                "()Ljava/lang/String;", false));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/File", "<init>",
                "(Ljava/io/File;Ljava/lang/String;)V", false));
        c.add(new VarInsnNode(Opcodes.ASTORE, 9));

        // if (target.exists() || target.equals(f)) { j++; continue; }   —— 不覆盖已有存档
        c.add(new VarInsnNode(Opcodes.ALOAD, 9));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "exists", "()Z", false));
        c.add(new JumpInsnNode(Opcodes.IFEQ, L_targetOk));
        c.add(new JumpInsnNode(Opcodes.GOTO, L_incInner));
        c.add(L_targetOk);

        // 复制：用 FileInputStream + FileOutputStream（不依赖 java.nio.file，安卓上更稳）
        c.add(L_copyTry);
        c.add(new TypeInsnNode(Opcodes.NEW, "java/io/FileInputStream"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new VarInsnNode(Opcodes.ALOAD, 7));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/FileInputStream", "<init>",
                "(Ljava/io/File;)V", false));
        c.add(new VarInsnNode(Opcodes.ASTORE, 10));
        c.add(new TypeInsnNode(Opcodes.NEW, "java/io/FileOutputStream"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new VarInsnNode(Opcodes.ALOAD, 9));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/FileOutputStream", "<init>",
                "(Ljava/io/File;)V", false));
        c.add(new VarInsnNode(Opcodes.ASTORE, 11));
        // byte[] buf = new byte[8192];
        c.add(new IntInsnNode(Opcodes.SIPUSH, 8192));
        c.add(new IntInsnNode(Opcodes.NEWARRAY, Opcodes.T_BYTE));
        c.add(new VarInsnNode(Opcodes.ASTORE, 12));
        LabelNode L_read = new LabelNode();
        LabelNode L_readEnd = new LabelNode();
        c.add(L_read);
        // int n = in.read(buf); if (n < 0) break;
        c.add(new VarInsnNode(Opcodes.ALOAD, 10));
        c.add(new VarInsnNode(Opcodes.ALOAD, 12));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/FileInputStream", "read", "([B)I", false));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new VarInsnNode(Opcodes.ISTORE, 13));
        c.add(new JumpInsnNode(Opcodes.IFLT, L_readEnd));
        // out.write(buf, 0, n);
        c.add(new VarInsnNode(Opcodes.ALOAD, 11));
        c.add(new VarInsnNode(Opcodes.ALOAD, 12));
        c.add(iconst(0));
        c.add(new VarInsnNode(Opcodes.ILOAD, 13));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/FileOutputStream", "write", "([BII)V", false));
        c.add(new JumpInsnNode(Opcodes.GOTO, L_read));
        c.add(L_readEnd);
        c.add(new VarInsnNode(Opcodes.ALOAD, 10));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/FileInputStream", "close", "()V", false));
        c.add(new VarInsnNode(Opcodes.ALOAD, 11));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/FileOutputStream", "close", "()V", false));
        // found.add(target);
        c.add(new VarInsnNode(Opcodes.ALOAD, 2));
        c.add(new VarInsnNode(Opcodes.ALOAD, 9));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/ArrayList", "add",
                "(Ljava/lang/Object;)Z", false));
        c.add(new InsnNode(Opcodes.POP));
        // 日志
        c.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
        c.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false));
        c.add(new LdcInsnNode("[QCL-saves] 已导入手机存档: "));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
        c.add(new VarInsnNode(Opcodes.ALOAD, 9));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString",
                "()Ljava/lang/String;", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                "(Ljava/lang/String;)V", false));
        c.add(L_copyEnd);
        c.add(new JumpInsnNode(Opcodes.GOTO, L_incInner));
        c.add(L_copyCatch);
        c.add(new VarInsnNode(Opcodes.ASTORE, 14));
        c.add(new VarInsnNode(Opcodes.ALOAD, 14));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V", false));
        // 失败就把残缺文件删掉
        c.add(new VarInsnNode(Opcodes.ALOAD, 9));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "delete", "()Z", false));
        c.add(new InsnNode(Opcodes.POP));

        c.add(L_incInner);
        c.add(new IincInsnNode(6, 1));
        c.add(new JumpInsnNode(Opcodes.GOTO, L_inner));
        c.add(L_innerEnd);
        c.add(L_inc);
        c.add(new IincInsnNode(4, 1));
        c.add(new JumpInsnNode(Opcodes.GOTO, L_loop));
        c.add(L_loopEnd);

        // return (File[]) found.toArray(new File[0]);
        c.add(new VarInsnNode(Opcodes.ALOAD, 2));
        c.add(iconst(0));
        c.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/io/File"));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/ArrayList", "toArray",
                "([Ljava/lang/Object;)[Ljava/lang/Object;", false));
        c.add(new TypeInsnNode(Opcodes.CHECKCAST, "[Ljava/io/File;"));
        c.add(new InsnNode(Opcodes.ARETURN));

        mn.tryCatchBlocks.add(new org.objectweb.asm.tree.TryCatchBlockNode(
                L_copyTry, L_copyEnd, L_copyCatch, "java/lang/Throwable"));
        mn.maxStack = 8;
        mn.maxLocals = 16;
        return mn;
    }

    /** Java 只有 ICONST_0..5，6 以上必须 BIPUSH。 */
    private static AbstractInsnNode iconst(int v) {
        if (v >= -1 && v <= 5) {
            return new InsnNode(Opcodes.ICONST_0 + v);
        }
        return new IntInsnNode(Opcodes.BIPUSH, v);
    }
}
