package qcl;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/**
 * 「游戏内文件浏览器」的界面填充方法（重写版，尽量简单）。
 *
 * <p>要点：只做「扫描目录 → 收集（文件, 标签）→ 填进界面 → 标记按钮可点」，
 * 不搞补位技巧、不搞复杂分支 —— 上一版就是分支太多导致字节码校验不过。
 * 每一项都至少有一行文字，不会出现空标签。
 */
public final class BuildBrowser2 {

    public static final String TARGET = "net/minecraft/client/c/e";

    public static MethodNode refresh() {
        MethodNode mn = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                "qclBrowseRefresh", "(L" + TARGET + ";Ljava/io/File;)V", null, null);
        InsnList c = mn.instructions;
        LabelNode L_ret = new LabelNode();
        LabelNode L_end = new LabelNode();

        // if (self == null || dir == null) return;
        c.add(new VarInsnNode(Opcodes.ALOAD, 0));
        c.add(new JumpInsnNode(Opcodes.IFNULL, L_ret));
        c.add(new VarInsnNode(Opcodes.ALOAD, 1));
        c.add(new JumpInsnNode(Opcodes.IFNULL, L_ret));

        // qclBrowseDir = dir; qclBrowseOn = true;
        c.add(new VarInsnNode(Opcodes.ALOAD, 1));
        c.add(new FieldInsnNode(Opcodes.PUTSTATIC, TARGET, "qclBrowseDir", "Ljava/io/File;"));
        c.add(new InsnNode(Opcodes.ICONST_1));
        c.add(new FieldInsnNode(Opcodes.PUTSTATIC, TARGET, "qclBrowseOn", "Z"));

        // ArrayList<File> files = new ArrayList<File>();
        c.add(new TypeInsnNode(Opcodes.NEW, "java/util/ArrayList"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false));
        c.add(new VarInsnNode(Opcodes.ASTORE, 2));
        // ArrayList<String> texts = new ArrayList<String>();
        c.add(new TypeInsnNode(Opcodes.NEW, "java/util/ArrayList"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false));
        c.add(new VarInsnNode(Opcodes.ASTORE, 3));

        // ① 「返回上一级」：dir.getParentFile() != null 时才加
        LabelNode L_noParent = new LabelNode();
        c.add(new VarInsnNode(Opcodes.ALOAD, 1));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "getParentFile",
                "()Ljava/io/File;", false));
        c.add(new JumpInsnNode(Opcodes.IFNULL, L_noParent));
        c.add(new VarInsnNode(Opcodes.ALOAD, 2));
        c.add(new InsnNode(Opcodes.ACONST_NULL));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/ArrayList", "add",
                "(Ljava/lang/Object;)Z", false));
        c.add(new InsnNode(Opcodes.POP));
        c.add(new VarInsnNode(Opcodes.ALOAD, 3));
        c.add(new LdcInsnNode("< 返回上一级"));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/ArrayList", "add",
                "(Ljava/lang/Object;)Z", false));
        c.add(new InsnNode(Opcodes.POP));
        c.add(L_noParent);

        // File[] list = dir.listFiles();
        c.add(new VarInsnNode(Opcodes.ALOAD, 1));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "listFiles", "()[Ljava/io/File;", false));
        c.add(new VarInsnNode(Opcodes.ASTORE, 4));
        c.add(new VarInsnNode(Opcodes.ALOAD, 4));
        c.add(new JumpInsnNode(Opcodes.IFNULL, L_end));

        // for (int i = 0; i < list.length; i++)
        LabelNode L_loop = new LabelNode();
        LabelNode L_next = new LabelNode();
        c.add(new InsnNode(Opcodes.ICONST_0));
        c.add(new VarInsnNode(Opcodes.ISTORE, 5));
        c.add(L_loop);
        c.add(new VarInsnNode(Opcodes.ILOAD, 5));
        c.add(new VarInsnNode(Opcodes.ALOAD, 4));
        c.add(new InsnNode(Opcodes.ARRAYLENGTH));
        c.add(new JumpInsnNode(Opcodes.IF_ICMPGE, L_end));
        // File f = list[i];
        c.add(new VarInsnNode(Opcodes.ALOAD, 4));
        c.add(new VarInsnNode(Opcodes.ILOAD, 5));
        c.add(new InsnNode(Opcodes.AALOAD));
        c.add(new VarInsnNode(Opcodes.ASTORE, 6));

        LabelNode L_isDir = new LabelNode();
        c.add(new VarInsnNode(Opcodes.ALOAD, 6));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "isDirectory", "()Z", false));
        c.add(new JumpInsnNode(Opcodes.IFNE, L_isDir));

        // —— 文件：只收 .mclevel ——
        c.add(new VarInsnNode(Opcodes.ALOAD, 6));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "getName",
                "()Ljava/lang/String;", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "toLowerCase",
                "()Ljava/lang/String;", false));
        c.add(new LdcInsnNode(".mclevel"));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "endsWith",
                "(Ljava/lang/String;)Z", false));
        c.add(new JumpInsnNode(Opcodes.IFEQ, L_next));
        // texts.add("[存档] " + f.getName()); files.add(f);
        c.add(new VarInsnNode(Opcodes.ALOAD, 3));
        c.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false));
        c.add(new LdcInsnNode("[存档] "));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
        c.add(new VarInsnNode(Opcodes.ALOAD, 6));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "getName",
                "()Ljava/lang/String;", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString",
                "()Ljava/lang/String;", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/ArrayList", "add",
                "(Ljava/lang/Object;)Z", false));
        c.add(new InsnNode(Opcodes.POP));
        c.add(new VarInsnNode(Opcodes.ALOAD, 2));
        c.add(new VarInsnNode(Opcodes.ALOAD, 6));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/ArrayList", "add",
                "(Ljava/lang/Object;)Z", false));
        c.add(new InsnNode(Opcodes.POP));
        c.add(new JumpInsnNode(Opcodes.GOTO, L_next));

        // —— 目录：跳过隐藏目录 ——
        c.add(L_isDir);
        c.add(new VarInsnNode(Opcodes.ALOAD, 6));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "getName",
                "()Ljava/lang/String;", false));
        c.add(new LdcInsnNode("."));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "startsWith",
                "(Ljava/lang/String;)Z", false));
        c.add(new JumpInsnNode(Opcodes.IFNE, L_next));
        // texts.add("[目录] " + f.getName()); files.add(f);
        c.add(new VarInsnNode(Opcodes.ALOAD, 3));
        c.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false));
        c.add(new LdcInsnNode("[目录] "));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
        c.add(new VarInsnNode(Opcodes.ALOAD, 6));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "getName",
                "()Ljava/lang/String;", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString",
                "()Ljava/lang/String;", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/ArrayList", "add",
                "(Ljava/lang/Object;)Z", false));
        c.add(new InsnNode(Opcodes.POP));
        c.add(new VarInsnNode(Opcodes.ALOAD, 2));
        c.add(new VarInsnNode(Opcodes.ALOAD, 6));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/ArrayList", "add",
                "(Ljava/lang/Object;)Z", false));
        c.add(new InsnNode(Opcodes.POP));

        c.add(L_next);
        c.add(new IincInsnNode(5, 1));
        c.add(new JumpInsnNode(Opcodes.GOTO, L_loop));
        c.add(L_end);

        // qclBrowseFiles = (File[]) files.toArray(new File[0]);
        c.add(new VarInsnNode(Opcodes.ALOAD, 2));
        c.add(new InsnNode(Opcodes.ICONST_0));
        c.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/io/File"));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/ArrayList", "toArray",
                "([Ljava/lang/Object;)[Ljava/lang/Object;", false));
        c.add(new TypeInsnNode(Opcodes.CHECKCAST, "[Ljava/io/File;"));
        c.add(new FieldInsnNode(Opcodes.PUTSTATIC, TARGET, "qclBrowseFiles", "[Ljava/io/File;"));

        // String[] slot = new String[5]; 先全部填 "-"
        c.add(new InsnNode(Opcodes.ICONST_5));
        c.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));
        c.add(new VarInsnNode(Opcodes.ASTORE, 7));
        // 用 4 条独立语句填 "-"（不用循环，避免任何标签）
        for (int i = 0; i < 5; i++) {
            c.add(new VarInsnNode(Opcodes.ALOAD, 7));
            c.add(iconst(i));
            c.add(new LdcInsnNode("-"));
            c.add(new InsnNode(Opcodes.AASTORE));
        }
        // 把 texts 的前 5 条覆盖进去（循环 + 边界判断）
        // for (int j = 0; j < 5 && j < texts.size(); j++) slot[j] = (String) texts.get(j);
        LabelNode L_fill = new LabelNode();
        LabelNode L_fillEnd = new LabelNode();
        c.add(new InsnNode(Opcodes.ICONST_0));
        c.add(new VarInsnNode(Opcodes.ISTORE, 8));
        c.add(L_fill);
        c.add(new VarInsnNode(Opcodes.ILOAD, 8));
        c.add(new InsnNode(Opcodes.ICONST_5));
        c.add(new JumpInsnNode(Opcodes.IF_ICMPGE, L_fillEnd));
        c.add(new VarInsnNode(Opcodes.ILOAD, 8));
        c.add(new VarInsnNode(Opcodes.ALOAD, 3));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/ArrayList", "size", "()I", false));
        c.add(new JumpInsnNode(Opcodes.IF_ICMPGE, L_fillEnd));
        c.add(new VarInsnNode(Opcodes.ALOAD, 7));
        c.add(new VarInsnNode(Opcodes.ILOAD, 8));
        c.add(new VarInsnNode(Opcodes.ALOAD, 3));
        c.add(new VarInsnNode(Opcodes.ILOAD, 8));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/ArrayList", "get",
                "(I)Ljava/lang/Object;", false));
        c.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/String"));
        c.add(new InsnNode(Opcodes.AASTORE));
        c.add(new IincInsnNode(8, 1));
        c.add(new JumpInsnNode(Opcodes.GOTO, L_fill));
        c.add(L_fillEnd);

        // self.a(slot);
        c.add(new VarInsnNode(Opcodes.ALOAD, 0));
        c.add(new VarInsnNode(Opcodes.ALOAD, 7));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, TARGET, "a", "([Ljava/lang/String;)V", false));

        // 把 7 个按钮标成可点（4 条独立语句 × 索引常量，不写循环）
        for (int i = 0; i < 7; i++) {
            c.add(new VarInsnNode(Opcodes.ALOAD, 0));
            c.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "e", "Ljava/util/List;"));
            c.add(iconst(i));
            c.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/List", "get",
                    "(I)Ljava/lang/Object;", true));
            c.add(new TypeInsnNode(Opcodes.CHECKCAST, "net/minecraft/client/c/r"));
            c.add(new InsnNode(Opcodes.ICONST_1));
            c.add(new FieldInsnNode(Opcodes.PUTFIELD, "net/minecraft/client/c/r", "qclClickable", "Z"));
        }

        c.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
        c.add(new LdcInsnNode("[QCL-browse] 已列出目录"));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                "(Ljava/lang/String;)V", false));

        c.add(L_ret);
        c.add(new InsnNode(Opcodes.RETURN));
        mn.maxStack = 6;
        mn.maxLocals = 12;
        return mn;
    }

    private static AbstractInsnNode iconst(int v) {
        if (v >= -1 && v <= 5) {
            return new InsnNode(Opcodes.ICONST_0 + v);
        }
        return new IntInsnNode(Opcodes.BIPUSH, v);
    }
}
