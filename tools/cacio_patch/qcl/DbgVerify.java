package qcl;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;

import java.io.File;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 精确定位栈不平衡：模拟 ASM 的 DataflowInterpreter 行为，
 * 逐条指令推进帧，出错时打印该指令与前后文。
 */
public final class DbgVerify {

    public static void main(String[] args) throws Exception {
        String jar = args[0];
        String cls = args[1];
        String methName = args.length > 2 ? args[2] : "a";
        String methDesc = args.length > 3 ? args[3] : "(Lnet/minecraft/client/c/r;)V";

        ClassNode cn = new ClassNode();
        File src = new File(jar);
        if (src.isFile() && jar.toLowerCase().endsWith(".class")) {
            java.io.FileInputStream fis = new java.io.FileInputStream(src);
            new ClassReader(fis).accept(cn, 0);
            fis.close();
        } else {
            File root = src.isDirectory() ? src : null;
            if (root != null) {
                File f = new File(root, cls.replace('.', '/') + ".class");
                java.io.FileInputStream fis = new java.io.FileInputStream(f);
                new ClassReader(fis).accept(cn, 0);
                fis.close();
            } else {
                ZipFile zf = new ZipFile(src);
                try {
                    ZipEntry ze = zf.getEntry(cls.replace('.', '/') + ".class");
                    InputStream in = zf.getInputStream(ze);
                    new ClassReader(in).accept(cn, 0);
                    in.close();
                } finally {
                    zf.close();
                }
            }
        }
        for (MethodNode mn : cn.methods) {
            if (!methName.equals(mn.name) || !methDesc.equals(mn.desc)) {
                continue;
            }
            System.out.println("方法 " + mn.name + mn.desc + " maxStack=" + mn.maxStack
                    + " maxLocals=" + mn.maxLocals + " 指令数=" + mn.instructions.size());
            Interpreter<BasicValue> it = new BasicInterpreter(Opcodes.ASM9) {
                @Override
                public BasicValue naryOperation(org.objectweb.asm.tree.AbstractInsnNode insn, java.util.List<? extends BasicValue> values) throws AnalyzerException {
                    try {
                        return super.naryOperation(insn, values);
                    } catch (AnalyzerException e) {
                        throw new AnalyzerException(insn, "nary: " + e.getMessage(), e);
                    }
                }
            };
            try {
                Analyzer<BasicValue> an = new Analyzer<>(it) {
                    @Override
                    protected Frame<BasicValue> computeInitialFrame(String owner, MethodNode m) {
                        return super.computeInitialFrame(owner, m);
                    }
                };
                an.analyze(cn.name, mn);
                System.out.println("PASS");
                Frame<BasicValue>[] all = an.getFrames();
                for (int i = 0; i < Math.min(all.length, 240); i++) {
                    if (all[i] != null) {
                        System.out.println("   帧[" + i + "] 栈深=" + all[i].getStackSize()
                                + " 指令=" + all[i].getStackSize() + " " + mn.instructions.get(i));
                    }
                }
            } catch (AnalyzerException ae) {
                AbstractInsnNode bad = ae.node;
                System.out.println("FAIL: " + ae.getMessage());
                int idx = bad == null ? -1 : mn.instructions.indexOf(bad);
                System.out.println("出错指令下标 " + idx + " :: " + bad);
                AbstractInsnNode[] arr = mn.instructions.toArray();
                int from = Math.min(Math.max(0, idx - 3), 100000);
                try {
                    Frame<BasicValue>[] ff = null;
                } catch (Throwable ignored2) {
                }
                for (int i = from; i < Math.min(arr.length, idx + 6); i++) {
                    AbstractInsnNode ins = arr[i];
                    String det = ins.toString();
                    if (ins instanceof org.objectweb.asm.tree.MethodInsnNode) {
                        org.objectweb.asm.tree.MethodInsnNode mi = (org.objectweb.asm.tree.MethodInsnNode) ins;
                        det = "INVOKE " + mi.getOpcode() + " " + mi.owner + "." + mi.name + mi.desc;
                    } else if (ins instanceof org.objectweb.asm.tree.FieldInsnNode) {
                        org.objectweb.asm.tree.FieldInsnNode fi = (org.objectweb.asm.tree.FieldInsnNode) ins;
                        det = "FIELD op=" + fi.getOpcode() + " " + fi.owner + "." + fi.name + " : " + fi.desc;
                    } else if (ins instanceof org.objectweb.asm.tree.VarInsnNode) {
                        org.objectweb.asm.tree.VarInsnNode vi = (org.objectweb.asm.tree.VarInsnNode) ins;
                        det = "VAR op=" + vi.getOpcode() + " var=" + vi.var;
                    } else if (ins instanceof org.objectweb.asm.tree.InsnNode) {
                        det = "INSN opcode=" + ins.getOpcode();
                    }
                    System.out.println((i == idx ? " >>> [" : "     [") + i + "] " + det);
                }
            }
        }
    }
}
