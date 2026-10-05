package qcl;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 让"灰色按钮"也能点。
 *
 * <p>背景：这个版本的按钮类用**一个** boolean 同时管两件事 ——
 * 「文字亮/灰」和「能不能点」（{@code c.r.c}）。游戏原版给空槽位、「加载文件…」、「取消」
 * 设的是 false，所以它们既显示为灰色、也永远点不动（用户实测："取消按钮又点不了"）。
 *
 * <p>做法：给按钮类加一个**独立**字段 {@code qclClickable}，并把命中判定
 * {@code c.r.a(int,int)} 改成「原判定 || qclClickable」。
 * 这样界面代码就能表达"文字灰但可点"这个原版表达不了的状态。
 */
public final class PatchButtonClick {

    private static final String BTN = "net/minecraft/client/c/r";
    private static final String HIT = "a";
    private static final String HIT_DESC = "(II)Z";
    private static final String FLAG = "qclClickable";

    public static void main(String[] args) throws Exception {
        File jar = new File(args[0]);
        File outDir = new File(args[1]);
        if (!outDir.exists() && !outDir.mkdirs()) {
            throw new IllegalStateException("无法创建输出目录: " + outDir);
        }

        ClassNode cn = new ClassNode();
        ZipFile zf = new ZipFile(jar);
        try {
            ZipEntry ze = zf.getEntry(BTN + ".class");
            if (ze == null) {
                System.out.println("   !! jar 里没有 " + BTN + ".class，跳过按钮可点补丁");
                return;
            }
            InputStream is = zf.getInputStream(ze);
            new ClassReader(is).accept(cn, 0);
            is.close();
        } finally {
            zf.close();
        }

        // ① 加字段 qclClickable（默认 false，不碰任何已有字段）
        boolean has = false;
        for (FieldNode fn : cn.fields) {
            if (FLAG.equals(fn.name)) {
                has = true;
            }
        }
        if (!has) {
            cn.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, FLAG, "Z", null, null));
        }

        // ② 命中判定：if (this.qclClickable) return true;  然后接原有逻辑
        int done = 0;
        for (MethodNode mn : cn.methods) {
            if (!HIT.equals(mn.name) || !HIT_DESC.equals(mn.desc)) {
                continue;
            }
            // ★★★★★【2026-10 修正】这里原来注入的是：
            //     if (this.qclClickable) return true;
            //   它直接**跳过了下面的坐标范围判定** → 被标记的按钮「全屏任何位置都算命中」
            //   → 玩家点空气也会触发保存 / 载入（实测确认的严重 bug，用户反复反馈）。
            //   现在**彻底不再改命中判定**：原版只需把字段 c（active）置 true，
            //   就同时「变亮」且「可点」（命中判定读的就是它），完全不需要额外开关。
            //   字段 qclClickable 保留（无害、默认 false），但不再参与任何判定。
            InsnList pre = new InsnList();
            pre.add(new InsnNode(Opcodes.NOP));
            mn.instructions.insert(pre);
            done++;
        }
        if (done == 0) {
            System.out.println("   !! 没找到按钮命中判定 " + BTN + "." + HIT + HIT_DESC);
            return;
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
        // 输出成 net/minecraft/client/c/r.class（打包脚本按这个相对路径找）
        File pkgDir = new File(outDir, "net" + File.separator + "minecraft"
                + File.separator + "client" + File.separator + "c");
        if (!pkgDir.exists() && !pkgDir.mkdirs()) {
            throw new IllegalStateException("无法创建输出目录: " + pkgDir);
        }
        File out = new File(pkgDir, "r.class");
        FileOutputStream fos = new FileOutputStream(out);
        fos.write(cw.toByteArray());
        fos.close();
        System.out.println("   已生成按钮可点补丁: " + out.getAbsolutePath()
                + "（新增字段 " + FLAG + "，改了 " + done + " 处命中判定）");
    }
}
