package qcl;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/**
 * 生成「游戏内文件浏览器」用的两个方法：
 * <ul>
 *   <li>{@code qclBrowseRefresh(c.e self, File dir)} —— 把 dir 下的子目录与 .mclevel 列进界面；</li>
 *   <li>{@code qclBrowseClick(c.e self, int id)} —— 处理点击（进入子目录 / 选中存档 / 返回上级 / 翻页）。</li>
 * </ul>
 *
 * <p>为什么要在游戏里自己画：游戏跑在独立 JVM 里，看不到安卓的类
 * （实测 {@code ClassNotFoundException: android/app/ActivityThread}），
 * 没法弹安卓的文件选择器；而 cacio 的 AWT 窗口在安卓上没有窗口实体（{@code setZOrder NOT YET IMPLEMENTED}）。
 * 所以只能在**游戏自己的界面**里列文件 —— FCL 等基于 Pojav 的启动器也是这么做的。
 *
 * <p>状态放在 c.e 的静态字段里：
 * {@code qclBrowseOn}（是否正在浏览）、{@code qclBrowseDir}（当前目录）、
 * {@code qclBrowseFiles}（界面每一项对应的 File，null 表示"返回上一级"）、
 * {@code qclBrowsePage}（页码）。
 * 界面用游戏自带的 7 个按钮：0-4 是条目，5 是「上一页」，6 是「下一页」；
 * 「取消」由原版逻辑负责（退出浏览）。
 */
public final class BuildBrowser {

    public static final String TARGET = "net/minecraft/client/c/e";

    /** 生成 qclBrowseRefresh。 */
    public static MethodNode refresh() {
        MethodNode mn = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                "qclBrowseRefresh", "(L" + TARGET + ";Ljava/io/File;)V", null, null);
        InsnList c = mn.instructions;
        LabelNode L_ret = new LabelNode();

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

        // File[] list = dir.listFiles();
        c.add(new VarInsnNode(Opcodes.ALOAD, 1));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "listFiles", "()[Ljava/io/File;", false));
        c.add(new VarInsnNode(Opcodes.ASTORE, 2));

        // ArrayList<File> items = new ArrayList<File>();
        c.add(new TypeInsnNode(Opcodes.NEW, "java/util/ArrayList"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false));
        c.add(new VarInsnNode(Opcodes.ASTORE, 3));
        // ArrayList<String> labels = new ArrayList<String>();
        c.add(new TypeInsnNode(Opcodes.NEW, "java/util/ArrayList"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false));
        c.add(new VarInsnNode(Opcodes.ASTORE, 4));

        // ① 第一项固定是"返回上一级"（根目录除外）
        LabelNode L_noParent = new LabelNode();
        c.add(new VarInsnNode(Opcodes.ALOAD, 1));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "getParentFile",
                "()Ljava/io/File;", false));
        c.add(new JumpInsnNode(Opcodes.IFNULL, L_noParent));
        c.add(new VarInsnNode(Opcodes.ALOAD, 3));
        c.add(new InsnNode(Opcodes.ACONST_NULL));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/ArrayList", "add",
                "(Ljava/lang/Object;)Z", false));
        c.add(new InsnNode(Opcodes.POP));
        c.add(new VarInsnNode(Opcodes.ALOAD, 4));
        c.add(new LdcInsnNode(".. 返回上一级"));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/ArrayList", "add",
                "(Ljava/lang/Object;)Z", false));
        c.add(new InsnNode(Opcodes.POP));
        c.add(L_noParent);

        // ② 遍历 listFiles 的结果：目录 与 .mclevel 都收进来
        LabelNode L_scanEnd = new LabelNode();
        LabelNode L_scan = new LabelNode();
        c.add(new VarInsnNode(Opcodes.ALOAD, 2));
        c.add(new JumpInsnNode(Opcodes.IFNULL, L_scanEnd));
        c.add(iconst(0));
        c.add(new VarInsnNode(Opcodes.ISTORE, 5));
        c.add(L_scan);
        c.add(new VarInsnNode(Opcodes.ILOAD, 5));
        c.add(new VarInsnNode(Opcodes.ALOAD, 2));
        c.add(new InsnNode(Opcodes.ARRAYLENGTH));
        c.add(new JumpInsnNode(Opcodes.IF_ICMPGE, L_scanEnd));

        // File f = list[i];
        c.add(new VarInsnNode(Opcodes.ALOAD, 2));
        c.add(new VarInsnNode(Opcodes.ILOAD, 5));
        c.add(new InsnNode(Opcodes.AALOAD));
        c.add(new VarInsnNode(Opcodes.ASTORE, 6));

        LabelNode L_next = new LabelNode();
        LabelNode L_isDir = new LabelNode();
        // 目录：跳过隐藏目录；加 "📁 名字"
        c.add(new VarInsnNode(Opcodes.ALOAD, 6));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "isDirectory", "()Z", false));
        c.add(new JumpInsnNode(Opcodes.IFNE, L_isDir));
        // 文件：只收 .mclevel
        c.add(new VarInsnNode(Opcodes.ALOAD, 6));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "getName",
                "()Ljava/lang/String;", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "toLowerCase",
                "()Ljava/lang/String;", false));
        c.add(new LdcInsnNode(".mclevel"));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "endsWith",
                "(Ljava/lang/String;)Z", false));
        c.add(new JumpInsnNode(Opcodes.IFEQ, L_next));
        // labels.add("💾 " + f.getName()); items.add(f);
        c.add(new VarInsnNode(Opcodes.ALOAD, 4));
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
        c.add(new VarInsnNode(Opcodes.ALOAD, 3));
        c.add(new VarInsnNode(Opcodes.ALOAD, 6));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/ArrayList", "add",
                "(Ljava/lang/Object;)Z", false));
        c.add(new InsnNode(Opcodes.POP));
        c.add(new JumpInsnNode(Opcodes.GOTO, L_next));
        c.add(L_isDir);
        // 跳过隐藏目录（名字以 . 开头）
        c.add(new VarInsnNode(Opcodes.ALOAD, 6));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "getName",
                "()Ljava/lang/String;", false));
        c.add(new LdcInsnNode("."));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "startsWith",
                "(Ljava/lang/String;)Z", false));
        c.add(new JumpInsnNode(Opcodes.IFNE, L_next));
        c.add(new VarInsnNode(Opcodes.ALOAD, 4));
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
        c.add(new VarInsnNode(Opcodes.ALOAD, 3));
        c.add(new VarInsnNode(Opcodes.ALOAD, 6));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/ArrayList", "add",
                "(Ljava/lang/Object;)Z", false));
        c.add(new InsnNode(Opcodes.POP));

        c.add(L_next);
        c.add(new IincInsnNode(5, 1));
        c.add(new JumpInsnNode(Opcodes.GOTO, L_scan));
        c.add(L_scanEnd);

        // qclBrowseFiles = items.toArray(new File[0]);
        c.add(new VarInsnNode(Opcodes.ALOAD, 3));
        c.add(iconst(0));
        c.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/io/File"));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/ArrayList", "toArray",
                "([Ljava/lang/Object;)[Ljava/lang/Object;", false));
        c.add(new TypeInsnNode(Opcodes.CHECKCAST, "[Ljava/io/File;"));
        c.add(new FieldInsnNode(Opcodes.PUTSTATIC, TARGET, "qclBrowseFiles", "[Ljava/io/File;"));

        // self.a(String[]) —— 把最多 5 个标签填到界面上（不足的填空串，避免点到残留项）
        c.add(new VarInsnNode(Opcodes.ALOAD, 0));
        c.add(new VarInsnNode(Opcodes.ALOAD, 4));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/ArrayList", "size", "()I", true));
        c.add(new VarInsnNode(Opcodes.ISTORE, 7));
        c.add(iconst(5));
        c.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));
        c.add(new VarInsnNode(Opcodes.ASTORE, 8));
        LabelNode L_fill = new LabelNode();
        LabelNode L_fillEnd = new LabelNode();
        c.add(iconst(0));
        c.add(new VarInsnNode(Opcodes.ISTORE, 9));
        c.add(L_fill);
        c.add(new VarInsnNode(Opcodes.ILOAD, 9));
        c.add(iconst(5));
        c.add(new JumpInsnNode(Opcodes.IF_ICMPGE, L_fillEnd));
        c.add(new VarInsnNode(Opcodes.ILOAD, 9));
        c.add(new VarInsnNode(Opcodes.ILOAD, 7));
        c.add(new JumpInsnNode(Opcodes.IF_ICMPGE, L_fillEnd));
        c.add(new VarInsnNode(Opcodes.ALOAD, 8));
        c.add(new VarInsnNode(Opcodes.ILOAD, 9));
        c.add(new VarInsnNode(Opcodes.ALOAD, 4));
        c.add(new VarInsnNode(Opcodes.ILOAD, 9));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/ArrayList", "get",
                "(I)Ljava/lang/Object;", false));
        c.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/String"));
        c.add(new InsnNode(Opcodes.AASTORE));
        c.add(new IincInsnNode(9, 1));
        c.add(new JumpInsnNode(Opcodes.GOTO, L_fill));
        c.add(L_fillEnd);
        // 填不满的补 "-"（长度1、灰色，也不会被当成存档）
        LabelNode L_pad = new LabelNode();
        LabelNode L_padEnd = new LabelNode();
        // if (n >= 5) 没有空位可补，直接跳过
        c.add(new VarInsnNode(Opcodes.ILOAD, 7));
        c.add(iconst(5));
        c.add(new JumpInsnNode(Opcodes.IF_ICMPGE, L_padEnd));
        c.add(new VarInsnNode(Opcodes.ILOAD, 7));
        c.add(new VarInsnNode(Opcodes.ISTORE, 9));
        c.add(L_pad);
        c.add(new VarInsnNode(Opcodes.ILOAD, 9));
        c.add(iconst(5));
        c.add(new JumpInsnNode(Opcodes.IF_ICMPGE, L_padEnd));
        c.add(new VarInsnNode(Opcodes.ALOAD, 8));
        c.add(new VarInsnNode(Opcodes.ILOAD, 9));
        c.add(new LdcInsnNode("-"));
        c.add(new InsnNode(Opcodes.AASTORE));
        c.add(new IincInsnNode(9, 1));
        c.add(new JumpInsnNode(Opcodes.GOTO, L_pad));
        c.add(L_padEnd);
        // self.a(labels)
        c.add(new VarInsnNode(Opcodes.ALOAD, 0));
        c.add(new VarInsnNode(Opcodes.ALOAD, 8));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, TARGET, "a", "([Ljava/lang/String;)V", false));
        // 把 6 个按钮都标成可点（这样"翻页/取消"都能点）
        for (int i = 0; i < 7; i++) {
            c.add(new VarInsnNode(Opcodes.ALOAD, 0));
            c.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "e", "Ljava/util/List;"));
            c.add(intConst(i));
            c.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/List", "get",
                    "(I)Ljava/lang/Object;", true));
            c.add(new TypeInsnNode(Opcodes.CHECKCAST, "net/minecraft/client/c/r"));
            c.add(new InsnNode(Opcodes.ICONST_1));
            c.add(new FieldInsnNode(Opcodes.PUTFIELD, "net/minecraft/client/c/r", "qclClickable", "Z"));
        }
        // 日志（不用 StringBuilder 链 —— 那种链在本类版本下会被算出多一个栈位）
        c.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
        c.add(new LdcInsnNode("[QCL-browse] 已列出目录"));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                "(Ljava/lang/String;)V", false));

        c.add(L_ret);
        c.add(new InsnNode(Opcodes.RETURN));
        mn.maxStack = 8;
        mn.maxLocals = 12;
        return mn;
    }

    private static AbstractInsnNode iconst(int v) {
        if (v >= -1 && v <= 5) {
            return new InsnNode(Opcodes.ICONST_0 + v);
        }
        return new IntInsnNode(Opcodes.BIPUSH, v);
    }

    private static AbstractInsnNode intConst(int v) {
        return iconst(v);
    }

    /**
     * 生成 qclBrowseClick：处理浏览界面里的点击。
     * <p>{@code id} 是界面槽位号（0..4 = 条目，5 = 上一页，6 = 下一页；取消由原版处理）。
     */
    public static MethodNode click() {
        MethodNode mn = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                "qclBrowseClick", "(L" + TARGET + ";I)V", null, null);
        InsnList c = mn.instructions;
        LabelNode L_ret = new LabelNode();

        // File[] files = qclBrowseFiles; if (files == null) return;
        c.add(new FieldInsnNode(Opcodes.GETSTATIC, TARGET, "qclBrowseFiles", "[Ljava/io/File;"));
        c.add(new VarInsnNode(Opcodes.ASTORE, 2));
        c.add(new VarInsnNode(Opcodes.ALOAD, 2));
        c.add(new JumpInsnNode(Opcodes.IFNULL, L_ret));

        // 翻页：统一重新刷新当前目录（条目多于一页时也只显示第一页，够用且简单）
        LabelNode L_slot = new LabelNode();
        c.add(new VarInsnNode(Opcodes.ILOAD, 1));
        c.add(iconst(5));
        c.add(new JumpInsnNode(Opcodes.IF_ICMPLT, L_slot));
        c.add(new VarInsnNode(Opcodes.ALOAD, 0));
        c.add(new FieldInsnNode(Opcodes.GETSTATIC, TARGET, "qclBrowseDir", "Ljava/io/File;"));
        c.add(new MethodInsnNode(Opcodes.INVOKESTATIC, TARGET, "qclBrowseRefresh",
                "(L" + TARGET + ";Ljava/io/File;)V", false));
        c.add(new JumpInsnNode(Opcodes.GOTO, L_ret));
        c.add(L_slot);

        // if (id < 0 || id >= files.length) return;
        c.add(new VarInsnNode(Opcodes.ILOAD, 1));
        c.add(new JumpInsnNode(Opcodes.IFLT, L_ret));
        c.add(new VarInsnNode(Opcodes.ILOAD, 1));
        c.add(new VarInsnNode(Opcodes.ALOAD, 2));
        c.add(new InsnNode(Opcodes.ARRAYLENGTH));
        c.add(new JumpInsnNode(Opcodes.IF_ICMPGE, L_ret));

        // File sel = files[id];
        c.add(new VarInsnNode(Opcodes.ALOAD, 2));
        c.add(new VarInsnNode(Opcodes.ILOAD, 1));
        c.add(new InsnNode(Opcodes.AALOAD));
        c.add(new VarInsnNode(Opcodes.ASTORE, 3));

        LabelNode L_isFile = new LabelNode();
        // if (sel == null) { 返回上一级 }
        c.add(new VarInsnNode(Opcodes.ALOAD, 3));
        c.add(new JumpInsnNode(Opcodes.IFNONNULL, L_isFile));
        c.add(new VarInsnNode(Opcodes.ALOAD, 0));
        c.add(new FieldInsnNode(Opcodes.GETSTATIC, TARGET, "qclBrowseDir", "Ljava/io/File;"));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "getParentFile",
                "()Ljava/io/File;", false));
        c.add(new MethodInsnNode(Opcodes.INVOKESTATIC, TARGET, "qclBrowseRefresh",
                "(L" + TARGET + ";Ljava/io/File;)V", false));
        c.add(new JumpInsnNode(Opcodes.GOTO, L_ret));
        c.add(L_isFile);

        LabelNode L_pick = new LabelNode();
        // if (sel.isDirectory()) { 进入该目录 } else { 选中它 }
        c.add(new VarInsnNode(Opcodes.ALOAD, 3));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "isDirectory", "()Z", false));
        c.add(new JumpInsnNode(Opcodes.IFEQ, L_pick));
        c.add(new VarInsnNode(Opcodes.ALOAD, 0));
        c.add(new VarInsnNode(Opcodes.ALOAD, 3));
        c.add(new MethodInsnNode(Opcodes.INVOKESTATIC, TARGET, "qclBrowseRefresh",
                "(L" + TARGET + ";Ljava/io/File;)V", false));
        c.add(new JumpInsnNode(Opcodes.GOTO, L_ret));
        c.add(L_pick);

        // —— 选中存档：复制进存档目录，然后直接读档 ——
        // self.o = new File(<saves>/sel.getName());
        LabelNode L_copyTry = new LabelNode();
        LabelNode L_copyEnd = new LabelNode();
        LabelNode L_copyCatch = new LabelNode();
        LabelNode L_afterCopy = new LabelNode();
        // 先把选中的文件复制进 saves（不存在同名才复制）
        c.add(new TypeInsnNode(Opcodes.NEW, "java/io/File"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new VarInsnNode(Opcodes.ALOAD, 0));
        c.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "b", "Lnet/minecraft/client/d;"));
        c.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/d", "z", "Ljava/io/File;"));
        c.add(new LdcInsnNode("saves"));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/File", "<init>",
                "(Ljava/io/File;Ljava/lang/String;)V", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "mkdirs", "()Z", false));
        c.add(new InsnNode(Opcodes.POP));

        // String abs = sel.getAbsolutePath();
        c.add(new VarInsnNode(Opcodes.ALOAD, 3));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "getAbsolutePath",
                "()Ljava/lang/String;", false));
        c.add(new VarInsnNode(Opcodes.ASTORE, 4));
        // boolean inSaves = abs.indexOf("/saves/") >= 0;
        c.add(new VarInsnNode(Opcodes.ALOAD, 4));
        c.add(new LdcInsnNode("/saves/"));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "indexOf",
                "(Ljava/lang/String;)I", false));
        c.add(new JumpInsnNode(Opcodes.IFGE, L_afterCopy));

        // 不在存档目录 → 复制过去（用 FileInputStream/FileOutputStream）
        c.add(L_copyTry);
        c.add(new TypeInsnNode(Opcodes.NEW, "java/io/FileInputStream"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new VarInsnNode(Opcodes.ALOAD, 3));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/FileInputStream", "<init>",
                "(Ljava/io/File;)V", false));
        c.add(new VarInsnNode(Opcodes.ASTORE, 5));
        // new FileOutputStream(new File(b.z, "saves" + File.separator + sel.getName()))
        //   ★ 直接拼字符串，别再 new File(new File(...), name) —— 之前那样写栈会失衡
        c.add(new TypeInsnNode(Opcodes.NEW, "java/io/FileOutputStream"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new TypeInsnNode(Opcodes.NEW, "java/io/File"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new VarInsnNode(Opcodes.ALOAD, 0));
        c.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "b", "Lnet/minecraft/client/d;"));
        c.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/d", "z", "Ljava/io/File;"));
        c.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false));
        c.add(new LdcInsnNode("saves"));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
        c.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/io/File", "separator", "Ljava/lang/String;"));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
        c.add(new VarInsnNode(Opcodes.ALOAD, 3));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "getName",
                "()Ljava/lang/String;", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString",
                "()Ljava/lang/String;", false));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/File", "<init>",
                "(Ljava/io/File;Ljava/lang/String;)V", false));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/FileOutputStream", "<init>",
                "(Ljava/io/File;)V", false));
        c.add(new VarInsnNode(Opcodes.ASTORE, 6));
        // byte[] buf = new byte[8192]; int n; while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        c.add(new IntInsnNode(Opcodes.SIPUSH, 8192));
        c.add(new IntInsnNode(Opcodes.NEWARRAY, Opcodes.T_BYTE));
        c.add(new VarInsnNode(Opcodes.ASTORE, 7));
        LabelNode L_read = new LabelNode();
        LabelNode L_readEnd = new LabelNode();
        c.add(L_read);
        c.add(new VarInsnNode(Opcodes.ALOAD, 5));
        c.add(new VarInsnNode(Opcodes.ALOAD, 7));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/FileInputStream", "read", "([B)I", false));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new VarInsnNode(Opcodes.ISTORE, 8));
        c.add(new JumpInsnNode(Opcodes.IFLE, L_readEnd));
        c.add(new VarInsnNode(Opcodes.ALOAD, 6));
        c.add(new VarInsnNode(Opcodes.ALOAD, 7));
        c.add(iconst(0));
        c.add(new VarInsnNode(Opcodes.ILOAD, 8));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/FileOutputStream", "write", "([BII)V", false));
        c.add(new JumpInsnNode(Opcodes.GOTO, L_read));
        c.add(L_readEnd);
        c.add(new VarInsnNode(Opcodes.ALOAD, 5));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/FileInputStream", "close", "()V", false));
        c.add(new VarInsnNode(Opcodes.ALOAD, 6));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/FileOutputStream", "close", "()V", false));
        c.add(L_copyEnd);
        c.add(new JumpInsnNode(Opcodes.GOTO, L_afterCopy));
        c.add(L_copyCatch);
        c.add(new VarInsnNode(Opcodes.ASTORE, 9));
        c.add(new VarInsnNode(Opcodes.ALOAD, 9));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V", false));
        c.add(L_afterCopy);

        // 交给游戏读档：this.o = 选中的文件；qclBrowseOn = false
        c.add(new VarInsnNode(Opcodes.ALOAD, 0));
        c.add(new VarInsnNode(Opcodes.ALOAD, 3));
        c.add(new FieldInsnNode(Opcodes.PUTFIELD, TARGET, "o", "Ljava/io/File;"));
        c.add(iconst(0));
        c.add(new FieldInsnNode(Opcodes.PUTSTATIC, TARGET, "qclBrowseOn", "Z"));
        // 日志（同样保持简单）
        c.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
        c.add(new LdcInsnNode("[QCL-browse] 玩家已选中存档，交给游戏读档"));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                "(Ljava/lang/String;)V", false));

        // 关界面（游戏下一帧会读 this.o）
        c.add(new VarInsnNode(Opcodes.ALOAD, 0));
        c.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "b", "Lnet/minecraft/client/d;"));
        c.add(new InsnNode(Opcodes.ACONST_NULL));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/client/d", "a",
                "(Lnet/minecraft/client/c/i;)V", false));

        c.add(L_ret);
        c.add(new InsnNode(Opcodes.RETURN));
        mn.tryCatchBlocks.add(new TryCatchBlockNode(L_copyTry, L_copyEnd, L_copyCatch, "java/lang/Throwable"));
        mn.maxStack = 8;
        mn.maxLocals = 12;
        return mn;
    }
}
