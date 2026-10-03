package qcl;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * ★ 去掉 2010 年那套"在线校验"，让「载入世界」真的能把世界装上。
 *
 * <p>原版 {@code net.minecraft.client.d.a(net.minecraft.a.a.g)} 里：
 * <pre>
 *   try {
 *       new URL(getDocumentBase() + "?n=...&amp;i=").openStream()      // 连 minecraft.net 校验
 *       ...
 *       if (getDocumentBase().startsWith("http://www.minecraft.net/")) {
 *           this.d = gVar;                                        // ★ 只有官网页面运行才装世界
 *       }
 *   } catch (Throwable unused) { }                                // 离线 → 静默跳过 → 世界装不上
 * </pre>
 * 我们离线运行，{@code getDocumentBase()} 不是 minecraft.net → {@code this.d} 永远不被赋值 →
 * 界面关掉、世界没载入（用户表现："点一下回到主菜单"）。
 *
 * <p>本补丁在该方法**开头**插入：
 * <pre>
 *   if (gVar != null) { this.d = gVar; }     // 无条件装上，跳过在线校验
 * </pre>
 * 其余逻辑保持原样。
 *
 * <p>用法：{@code java qcl.PatchLevelLoad <游戏jar> <输出目录>}
 */
public class PatchLevelLoad {

    private static final String TARGET = "net/minecraft/client/d";
    private static final String FIELD_D = "d";
    private static final String DESC_G = "Lnet/minecraft/a/a/g;";
    private static final String METHOD_DESC = "(Lnet/minecraft/a/a/g;)V";

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("用法: PatchLevelLoad <游戏jar> <输出目录>");
            System.exit(1);
        }
        File jar = new File(args[0]);
        File out = new File(new File(args[1]), TARGET + ".class");
        out.getParentFile().mkdirs();

        ClassNode cn;
        ZipFile zf = new ZipFile(jar);
        try {
            ZipEntry ze = zf.getEntry(TARGET + ".class");
            if (ze == null) {
                System.out.println("!! jar 里没有 " + TARGET + ".class");
                System.exit(2);
            }
            InputStream is = zf.getInputStream(ze);
            cn = new ClassNode();
            new ClassReader(is).accept(cn, 0);
            is.close();
        } finally {
            zf.close();
        }

        int patched = 0;
        for (MethodNode mn : cn.methods) {
            if ("a".equals(mn.name) && METHOD_DESC.equals(mn.desc)) {
                InsnList inject = new InsnList();
                LabelNode skip = new LabelNode();
                // if (gVar == null) goto skip;
                inject.add(new VarInsnNode(Opcodes.ALOAD, 1));
                inject.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFNULL, skip));
                // this.d = gVar;
                inject.add(new VarInsnNode(Opcodes.ALOAD, 0));
                inject.add(new VarInsnNode(Opcodes.ALOAD, 1));
                inject.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.PUTFIELD, TARGET, FIELD_D, DESC_G));
                inject.add(skip);
                mn.instructions.insert(inject);
                // 栈深度可能增加 2
                mn.maxStack = Math.max(mn.maxStack, 3);
                patched++;
            }
        }
        if (patched == 0) {
            System.out.println("!! 没找到 a(" + DESC_G + ")V");
            System.exit(3);
        }

        // ★ COMPUTE_FRAMES：Java 8 字节码需要重算 StackMapTable
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                try {
                    return super.getCommonSuperClass(type1, type2);
                } catch (Throwable t) {
                    return "java/lang/Object";
                }
            }
        };
        cn.accept(cw);
        FileOutputStream fos = new FileOutputStream(out);
        fos.write(cw.toByteArray());
        fos.close();
        System.out.println("已生成补丁类: " + out.getAbsolutePath() + "（改了 " + patched + " 个方法）");
    }
}
