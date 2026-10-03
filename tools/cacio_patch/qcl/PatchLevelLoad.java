package qcl;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * ★ 去掉 2010 年那套"在线校验"，让「载入世界」真的能把世界装上。
 *
 * <p>原版 {@code net.minecraft.client.d.a(<关卡类>)} 里：
 * <pre>
 *   try {
 *       new URL(getDocumentBase() + "?n=...&amp;i=").openStream()      // 连 minecraft.net 校验
 *       ...
 *       if (getDocumentBase().startsWith("http://www.minecraft.net/")) {
 *           this.d = gVar;                                        // ★ 只有官网页面运行才装世界
 *       }
 *   } catch (Throwable unused) { }                                // 离线 → 静默跳过 → 世界装不上
 * </pre>
 * 我们离线运行 → {@code this.d} 永远不被赋值 → 界面关掉、世界没载入（用户表现："点一下回主菜单"）。
 *
 * <p>本补丁在该方法**开头**插入 {@code if (gVar != null) { this.d = gVar; }}，其余逻辑原样保留。
 *
 * <p>★★ 各版本的这个方法**参数类型不同**（实测：in-20100223 是 {@code net.minecraft.a.a.g}，
 * inf-20100227-1433 是 {@code net.minecraft.a.a.e}，inf-20100313 是 {@code net.minecraft.a.a.f}），
 * 所以这里**从常量池里自动取字段 {@code d} 的实际类型**来定位方法与生成指令，
 * 不再硬编码 {@code g} —— 之前硬编码导致 5 个 infdev 版本的补丁**静默打不上**。
 */
public class PatchLevelLoad {

    private static final String TARGET = "net/minecraft/client/d";
    private static final String FIELD_D = "d";

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("用法: PatchLevelLoad <游戏jar> <输出目录>");
            System.exit(1);
        }
        File jar = new File(args[0]);
        File out = new File(new File(args[1]), TARGET + ".class");
        out.getParentFile().mkdirs();

        ZipFile zf = new ZipFile(jar);
        ClassNode cn;
        String levelType = null;
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

            // ① 先从字段表拿 d 的类型（最可靠）
            for (FieldNode fn : cn.fields) {
                if (FIELD_D.equals(fn.name)) {
                    levelType = fn.desc;
                    break;
                }
            }
            // ② 字段表没有就扫字节码里的 PUTFIELD d
            if (levelType == null) {
                for (MethodNode mn : cn.methods) {
                    if (mn.instructions == null) {
                        continue;
                    }
                    for (int i = 0; i < mn.instructions.size(); i++) {
                        if (mn.instructions.get(i) instanceof FieldInsnNode) {
                            FieldInsnNode fin = (FieldInsnNode) mn.instructions.get(i);
                            if (fin.getOpcode() == Opcodes.PUTFIELD && FIELD_D.equals(fin.name)) {
                                levelType = fin.desc;
                                break;
                            }
                        }
                    }
                    if (levelType != null) {
                        break;
                    }
                }
            }
        } finally {
            zf.close();
        }
        if (levelType == null) {
            System.out.println("!! 拿不到字段 " + FIELD_D + " 的类型");
            System.exit(4);
        }
        System.out.println("   关卡字段 " + FIELD_D + " 类型 = " + levelType);

        // ③ 找"单一关卡参数、返回 void"的方法：a(L<levelType>;)V
        String methodDesc = "(" + levelType + ")V";
        int patched = 0;
        for (MethodNode mn : cn.methods) {
            if ("a".equals(mn.name) && methodDesc.equals(mn.desc)) {
                InsnList inject = new InsnList();
                LabelNode skip = new LabelNode();
                // if (gVar == null) goto skip;
                inject.add(new VarInsnNode(Opcodes.ALOAD, 1));
                inject.add(new JumpInsnNode(Opcodes.IFNULL, skip));
                // this.d = gVar;
                inject.add(new VarInsnNode(Opcodes.ALOAD, 0));
                inject.add(new VarInsnNode(Opcodes.ALOAD, 1));
                inject.add(new FieldInsnNode(Opcodes.PUTFIELD, TARGET, FIELD_D, levelType));
                inject.add(skip);
                mn.instructions.insert(inject);
                mn.maxStack = Math.max(mn.maxStack, 3);
                patched++;
            }
        }
        if (patched == 0) {
            System.out.println("!! 没找到 a" + methodDesc);
            System.exit(3);
        }

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
        FileOutputStream fos = new FileOutputStream(out);
        fos.write(cw.toByteArray());
        fos.close();
        System.out.println("   已生成: " + out.getAbsolutePath() + "（改了 " + patched + " 个方法）");
    }
}
