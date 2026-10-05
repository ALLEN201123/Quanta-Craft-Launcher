package qcl;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.io.File;
import java.io.FileInputStream;

/**
 * 报告 a(r) 的每条指令 + 该处的栈深，专门用来找"空栈"那一条。
 */
public final class DbgVerify2 {
    public static void main(String[] args) throws Exception {
        File f = new File(args[0], "net/minecraft/client/c/e.class");
        ClassNode cn = new ClassNode();
        FileInputStream fis = new FileInputStream(f);
        new ClassReader(fis).accept(cn, 0);
        fis.close();
        for (MethodNode mn : cn.methods) {
            if (!"a".equals(mn.name) || !"(Lnet/minecraft/client/c/r;)V".equals(mn.desc)) {
                continue;
            }
            Analyzer<BasicValue> an = new Analyzer<>(new BasicInterpreter());
            String status;
            try {
                an.analyze(cn.name, mn);
                status = "PASS";
            } catch (Throwable t) {
                status = "FAIL: " + t.getMessage();
            }
            System.out.println("a(r) maxStack=" + mn.maxStack + " 指令数=" + mn.instructions.size()
                    + " → " + status);
            Frame<BasicValue>[] frames = an.getFrames();
            AbstractInsnNode[] arr = mn.instructions.toArray();
            for (int i = 0; i < arr.length; i++) {
                int depth = -1;
                if (frames != null && i < frames.length && frames[i] != null) {
                    depth = frames[i].getStackSize();
                }
                // 只在"栈深很小"或可疑处打印，避免刷屏
                if (i >= 195 && i <= 220) {
                    System.out.println("   [" + i + "] 栈深=" + depth + "  " + describe(arr[i]));
                }
            }
        }
    }

    private static String describe(AbstractInsnNode n) {
        if (n instanceof MethodInsnNode) {
            MethodInsnNode m = (MethodInsnNode) n;
            return "INVOKE(op=" + m.getOpcode() + ") " + m.owner + "." + m.name + m.desc;
        }
        if (n instanceof FieldInsnNode) {
            FieldInsnNode fi = (FieldInsnNode) n;
            return "FIELD(op=" + fi.getOpcode() + ") " + fi.owner + "." + fi.name;
        }
        if (n instanceof VarInsnNode) {
            return "VAR(op=" + n.getOpcode() + ") var=" + ((VarInsnNode) n).var;
        }
        if (n instanceof TypeInsnNode) {
            return "TYPE(op=" + n.getOpcode() + ") " + ((TypeInsnNode) n).desc;
        }
        if (n instanceof InsnNode) {
            return "INSN opcode=" + n.getOpcode();
        }
        return n.getClass().getSimpleName();
    }
}
