package qcl;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * ★ 给游戏侧 {@code net.minecraft.client.c.e} 打补丁：
 * <ol>
 *   <li>{@code run()} 改成：扫本地 {@code saves/} 目录 → 填 5 个槽位（不再连 2010 年那台已关闭的官网服务器）</li>
 *   <li>"列目录 + 取时间"的逻辑**作为静态方法注入到 c.e 自己身上**（{@code qclListSaves}）——
 *       ⚠️ 不能外挂新类：launchwrapper 的 LaunchClassLoader 只认游戏 jar 里本来就有的类名，
 *       新类会抛 ClassNotFoundException / IllegalArgumentException（实测踩过两次）。</li>
 * </ol>
 */
public class PatchSaveList {

    private static final String TARGET = "net/minecraft/client/c/e";
    private static final String NEW_FIELD = "qclSavesLocal";
    private static final String NEW_FIELD_DESC = "[Ljava/lang/String;";
    private static final String LIST_METHOD = "qclListSaves";
    private static final String LIST_DESC = "(Ljava/io/File;)[Ljava/lang/String;";
    /** 注入的填充方法：void qclFillSaves(e self, File dir) —— 同时填显示文本与文件数组。 */
    private static final String FILL_METHOD = "qclFillSaves";
    /** 存"槽位 → 文件"映射的字段（槽位点击时用它去加载）。 */
    private static final String SAVES_FIELD = "qclSaves";

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("用法: PatchSaveList <游戏jar> <输出目录>");
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
                System.out.println("!! jar 里没有 " + TARGET + ".class（该版本没有载入/保存世界界面）");
                System.exit(2);
            }
            InputStream is = zf.getInputStream(ze);
            cn = new ClassNode();
            new ClassReader(is).accept(cn, 0);
            is.close();
        } finally {
            zf.close();
        }

        boolean hasField = false;
        boolean hasSavesField = false;
        boolean hasListMethod = false;
        MethodNode run = null;
        for (FieldNode fn : cn.fields) {
            if (NEW_FIELD.equals(fn.name)) {
                hasField = true;
            }
            if (SAVES_FIELD.equals(fn.name)) {
                hasSavesField = true;
            }
        }
        for (MethodNode mn : cn.methods) {
            if ("run".equals(mn.name) && "()V".equals(mn.desc)) {
                run = mn;
            }
            if (LIST_METHOD.equals(mn.name) && LIST_DESC.equals(mn.desc)) {
                hasListMethod = true;
            }
        }
        if (run == null) {
            System.out.println("!! 没找到 run()V");
            System.exit(3);
        }
        if (!hasField) {
            cn.fields.add(new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                    NEW_FIELD, NEW_FIELD_DESC, null, null));
        }
        // ★ 槽位 → 文件 的映射（槽位点击时 a(r) 会读 qclSaves[id]）
        if (!hasSavesField) {
            cn.fields.add(new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                    SAVES_FIELD, "[Ljava/io/File;", null, null));
        }
        // ★ 让字段初始化不为 null：注入静态初始化？—— 直接用 run() 里赋值即可，
        //    但首次点击可能早于 run()，所以再加一道保险：把字段声明为 null 时由 a(r) 兜住。
        if (!hasListMethod) {
            cn.methods.add(buildListMethod());
            cn.methods.add(buildFillMethod());
        }

        // ① run()：清空成空方法 —— 原版会去连 2010 年那台已关闭的官网服务器取列表，
        //    在某些网络下会长时间阻塞（用户表现：点「载入世界」卡死）。列表改由 b() 末尾填。
        run.instructions.clear();
        run.tryCatchBlocks.clear();
        if (run.localVariables != null) {
            run.localVariables.clear();
        }
        run.instructions.add(new InsnNode(Opcodes.RETURN));
        run.maxStack = 0;
        run.maxLocals = Math.max(run.maxLocals, 1);

        // ② b()：在**所有槽位/按钮都创建完之后**（方法末尾）处理槽位。
        //    ★★ 两个界面共用这一套代码（c.o 继承 c.e），必须**可靠区分**：
        //      载入界面：5 槽位 + 「加载文件…」+「取消」
        //      保存界面：5 槽位 + 「保存文件…」+「取消」
        //   区分方法：取第 6 个按钮（索引 5）的文字，startsWith("保存") → 保存界面。
        //   （早期版本用标题字段 a 判断，实测取不到，导致保存界面被当成载入界面处理 ——
        //     于是只有"有存档"的槽位被标成可点，3/4/5 空槽位点不了；用户实测反馈过。）
        //   保存界面要做的：① 不填存档列表（避免误覆盖）、② 6 个按钮全部标成"可点"
        //     （基类点击只看 c.r.c，而游戏自己的保存界面只设了 d 没设 c → 原始版本点不动）。
        for (MethodNode mn : cn.methods) {
            if ("b".equals(mn.name) && "()V".equals(mn.desc)) {
                InsnList fill = new InsnList();
                LabelNode L_load = new LabelNode();
                LabelNode L_done = new LabelNode();
                // if (this.e.size() <= 5) goto load;      // 没有第 6 个按钮 → 当载入界面
                fill.add(new VarInsnNode(Opcodes.ALOAD, 0));
                fill.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "e", "Ljava/util/List;"));
                fill.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/List", "size", "()I", true));
                fill.add(new InsnNode(Opcodes.ICONST_5));
                fill.add(new JumpInsnNode(Opcodes.IF_ICMPLE, L_load));
                // if (this.e.get(5).a != null && this.e.get(5).a.startsWith("保存")) → 保存界面
                fill.add(new VarInsnNode(Opcodes.ALOAD, 0));
                fill.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "e", "Ljava/util/List;"));
                fill.add(new InsnNode(Opcodes.ICONST_5));
                fill.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/List", "get",
                        "(I)Ljava/lang/Object;", true));
                fill.add(new TypeInsnNode(Opcodes.CHECKCAST, "net/minecraft/client/c/r"));
                fill.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/c/r", "a", "Ljava/lang/String;"));
                fill.add(new JumpInsnNode(Opcodes.IFNULL, L_load));
                fill.add(new VarInsnNode(Opcodes.ALOAD, 0));
                fill.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "e", "Ljava/util/List;"));
                fill.add(new InsnNode(Opcodes.ICONST_5));
                fill.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/List", "get",
                        "(I)Ljava/lang/Object;", true));
                fill.add(new TypeInsnNode(Opcodes.CHECKCAST, "net/minecraft/client/c/r"));
                fill.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/c/r", "a", "Ljava/lang/String;"));
                fill.add(new LdcInsnNode("保存"));
                fill.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "startsWith",
                        "(Ljava/lang/String;)Z", false));
                fill.add(new JumpInsnNode(Opcodes.IFEQ, L_load));
                // ——— 保存界面 ———
                // ① 不填存档列表：qclSaves 清空，点任何槽位都是"新建存档"而不是覆盖旧档
                fill.add(new InsnNode(Opcodes.ICONST_0));
                fill.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/io/File"));
                fill.add(new FieldInsnNode(Opcodes.PUTSTATIC, TARGET, SAVES_FIELD, "[Ljava/io/File;"));
                // ② 6 个按钮全部标成"可点"
                for (int i = 0; i < 6; i++) {
                    fill.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    fill.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "e", "Ljava/util/List;"));
                    fill.add(new InsnNode(Opcodes.ICONST_0 + i));
                    fill.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/List", "get",
                            "(I)Ljava/lang/Object;", true));
                    fill.add(new TypeInsnNode(Opcodes.CHECKCAST, "net/minecraft/client/c/r"));
                    fill.add(new InsnNode(Opcodes.ICONST_1));
                    fill.add(new FieldInsnNode(Opcodes.PUTFIELD, "net/minecraft/client/c/r", "c", "Z"));
                }
                // 自检日志：把第 6 个按钮的文字打出来（下次排查一眼就能看出走的哪条分支）
                fill.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
                fill.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
                fill.add(new InsnNode(Opcodes.DUP));
                fill.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false));
                fill.add(new LdcInsnNode("[QCL-saves] 保存界面：第6按钮="));
                fill.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                        "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
                fill.add(new VarInsnNode(Opcodes.ALOAD, 0));
                fill.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "e", "Ljava/util/List;"));
                fill.add(new InsnNode(Opcodes.ICONST_5));
                fill.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/List", "get",
                        "(I)Ljava/lang/Object;", true));
                fill.add(new TypeInsnNode(Opcodes.CHECKCAST, "net/minecraft/client/c/r"));
                fill.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/c/r", "a", "Ljava/lang/String;"));
                fill.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                        "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false));
                fill.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString",
                        "()Ljava/lang/String;", false));
                fill.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                        "(Ljava/lang/String;)V", false));
                fill.add(new JumpInsnNode(Opcodes.GOTO, L_done));
                // ——— 载入界面 ———
                fill.add(L_load);
                // this.qclFillSaves(new File(this.b.z, "saves"))
                fill.add(new VarInsnNode(Opcodes.ALOAD, 0));
                fill.add(new TypeInsnNode(Opcodes.NEW, "java/io/File"));
                fill.add(new InsnNode(Opcodes.DUP));
                fill.add(new VarInsnNode(Opcodes.ALOAD, 0));
                fill.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "b", "Lnet/minecraft/client/d;"));
                fill.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/d", "z", "Ljava/io/File;"));
                fill.add(new LdcInsnNode("saves"));
                fill.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/File", "<init>",
                        "(Ljava/io/File;Ljava/lang/String;)V", false));
                fill.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, TARGET, FILL_METHOD,
                        "(Ljava/io/File;)V", false));
                // this.a(this.l) —— 把文本填进 5 个槽位
                fill.add(new VarInsnNode(Opcodes.ALOAD, 0));
                fill.add(new VarInsnNode(Opcodes.ALOAD, 0));
                fill.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "l", "[Ljava/lang/String;"));
                fill.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, TARGET, "a", "([Ljava/lang/String;)V", false));
                // this.k = true —— 让槽位点击走"读档"分支
                fill.add(new VarInsnNode(Opcodes.ALOAD, 0));
                fill.add(new InsnNode(Opcodes.ICONST_1));
                fill.add(new FieldInsnNode(Opcodes.PUTFIELD, TARGET, "k", "Z"));
                // 自检日志
                fill.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
                fill.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
                fill.add(new InsnNode(Opcodes.DUP));
                fill.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false));
                fill.add(new LdcInsnNode("[QCL-saves] 载入界面：第6按钮="));
                fill.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                        "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
                fill.add(new VarInsnNode(Opcodes.ALOAD, 0));
                fill.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "e", "Ljava/util/List;"));
                fill.add(new InsnNode(Opcodes.ICONST_5));
                fill.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/List", "get",
                        "(I)Ljava/lang/Object;", true));
                fill.add(new TypeInsnNode(Opcodes.CHECKCAST, "net/minecraft/client/c/r"));
                fill.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/c/r", "a", "Ljava/lang/String;"));
                fill.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                        "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false));
                fill.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString",
                        "()Ljava/lang/String;", false));
                fill.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                        "(Ljava/lang/String;)V", false));
                fill.add(L_done);
                // 插到最后一个 RETURN 之前
                AbstractInsnNode last = null;
                for (AbstractInsnNode p = mn.instructions.getLast(); p != null; p = p.getPrevious()) {
                    if (p.getOpcode() == Opcodes.RETURN) {
                        last = p;
                        break;
                    }
                }
                if (last != null) {
                    mn.instructions.insertBefore(last, fill);
                } else {
                    mn.instructions.add(fill);
                }
                mn.maxStack = Math.max(mn.maxStack, 6);
                mn.maxLocals = Math.max(mn.maxLocals, 1);
            }
        }

        // ★★★ 1.4.8：重写槽位点击 a(r) —— **载入界面点哪个槽位就读哪个存档**。
        //   为什么必须重写：原版 a(r) 依赖后台线程 c.f 把对话框结果塞进 this.o，
        //   而 run() 已被清空（原版会去连 2010 年那台已关闭的服务器，会卡死），
        //   所以 this.o 永远是 null → 点槽位什么都不会发生。
        //   保存界面不受影响：那时 qclSaves 是空数组（见 b() 的保存分支），直接走原逻辑
        //   （原逻辑是 c.o 覆盖的 a(int) → 弹「输入世界名称」）。
        for (MethodNode mn : cn.methods) {
            if (!"a".equals(mn.name) || !"(Lnet/minecraft/client/c/r;)V".equals(mn.desc)) {
                continue;
            }
            InsnList click = new InsnList();
            LabelNode L_ret = new LabelNode();
            LabelNode L_try = new LabelNode();
            LabelNode L_tryEnd = new LabelNode();
            LabelNode L_catch = new LabelNode();
            LabelNode L_orig = new LabelNode();
            // if (!rVar.c) return;                 // 不可点的按钮不处理
            click.add(new VarInsnNode(Opcodes.ALOAD, 1));
            click.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/c/r", "c", "Z"));
            click.add(new JumpInsnNode(Opcodes.IFEQ, L_ret));
            // ★★ 在"点击发生时"判断是哪个界面（此时第 6 个按钮的文字一定已设置好；
            //    早先在 b() 里判断是错的 —— b() 跑在 c.o.b() 设置文字之前，永远读到默认值"加载文件…"）。
            //    保存界面 = "保存文件…"（startsWith 保存）→ 走原逻辑，由 c.o 覆盖的 a(int) 弹「输入世界名称」；
            //    载入界面 = "加载文件…" → 读所选存档。
            click.add(new VarInsnNode(Opcodes.ALOAD, 0));
            click.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "e", "Ljava/util/List;"));
            click.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/List", "size", "()I", true));
            click.add(new InsnNode(Opcodes.ICONST_5));
            click.add(new JumpInsnNode(Opcodes.IF_ICMPLE, L_orig));
            click.add(new VarInsnNode(Opcodes.ALOAD, 0));
            click.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "e", "Ljava/util/List;"));
            click.add(new InsnNode(Opcodes.ICONST_5));
            click.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/List", "get",
                    "(I)Ljava/lang/Object;", true));
            click.add(new TypeInsnNode(Opcodes.CHECKCAST, "net/minecraft/client/c/r"));
            click.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/c/r", "a", "Ljava/lang/String;"));
            click.add(new JumpInsnNode(Opcodes.IFNULL, L_orig));
            click.add(new VarInsnNode(Opcodes.ALOAD, 0));
            click.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "e", "Ljava/util/List;"));
            click.add(new InsnNode(Opcodes.ICONST_5));
            click.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/List", "get",
                    "(I)Ljava/lang/Object;", true));
            click.add(new TypeInsnNode(Opcodes.CHECKCAST, "net/minecraft/client/c/r"));
            click.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/c/r", "a", "Ljava/lang/String;"));
            click.add(new LdcInsnNode("保存"));
            click.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "startsWith",
                    "(Ljava/lang/String;)Z", false));
            click.add(new JumpInsnNode(Opcodes.IFNE, L_orig));
            // if (qclSaves == null || rVar.b < 0 || rVar.b >= qclSaves.length) return;
            click.add(new FieldInsnNode(Opcodes.GETSTATIC, TARGET, SAVES_FIELD, "[Ljava/io/File;"));
            click.add(new JumpInsnNode(Opcodes.IFNULL, L_ret));
            click.add(new VarInsnNode(Opcodes.ALOAD, 1));
            click.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/c/r", "b", "I"));
            click.add(new FieldInsnNode(Opcodes.GETSTATIC, TARGET, SAVES_FIELD, "[Ljava/io/File;"));
            click.add(new InsnNode(Opcodes.ARRAYLENGTH));
            click.add(new JumpInsnNode(Opcodes.IF_ICMPGE, L_ret));
            // if (qclSaves[rVar.b] == null) return;
            click.add(new FieldInsnNode(Opcodes.GETSTATIC, TARGET, SAVES_FIELD, "[Ljava/io/File;"));
            click.add(new VarInsnNode(Opcodes.ALOAD, 1));
            click.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/c/r", "b", "I"));
            click.add(new InsnNode(Opcodes.AALOAD));
            click.add(new JumpInsnNode(Opcodes.IFNULL, L_ret));
            // this.o = qclSaves[rVar.b]
            click.add(new VarInsnNode(Opcodes.ALOAD, 0));
            click.add(new FieldInsnNode(Opcodes.GETSTATIC, TARGET, SAVES_FIELD, "[Ljava/io/File;"));
            click.add(new VarInsnNode(Opcodes.ALOAD, 1));
            click.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/c/r", "b", "I"));
            click.add(new InsnNode(Opcodes.AALOAD));
            click.add(new FieldInsnNode(Opcodes.PUTFIELD, TARGET, "o", "Ljava/io/File;"));
            click.add(L_try);
            // 打点：读了哪个文件
            click.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
            click.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
            click.add(new InsnNode(Opcodes.DUP));
            click.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false));
            click.add(new LdcInsnNode("[QCL-saves] 载入所选存档: "));
            click.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
            click.add(new VarInsnNode(Opcodes.ALOAD, 0));
            click.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "o", "Ljava/io/File;"));
            click.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                    "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false));
            click.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString",
                    "()Ljava/lang/String;", false));
            click.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                    "(Ljava/lang/String;)V", false));
            // this.a(this.o) 读档；然后关界面
            click.add(new VarInsnNode(Opcodes.ALOAD, 0));
            click.add(new VarInsnNode(Opcodes.ALOAD, 0));
            click.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "o", "Ljava/io/File;"));
            click.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, TARGET, "a", "(Ljava/io/File;)V", false));
            click.add(new VarInsnNode(Opcodes.ALOAD, 0));
            click.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "b", "Lnet/minecraft/client/d;"));
            click.add(new InsnNode(Opcodes.ACONST_NULL));
            click.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/client/d", "a",
                    "(Lnet/minecraft/client/c/i;)V", false));
            click.add(L_tryEnd);
            click.add(new JumpInsnNode(Opcodes.GOTO, L_ret));
            click.add(L_catch);
            click.add(new VarInsnNode(Opcodes.ASTORE, 2));
            click.add(new VarInsnNode(Opcodes.ALOAD, 2));
            click.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V", false));
            click.add(new JumpInsnNode(Opcodes.GOTO, L_ret));
            // ——— 原逻辑（保存界面走这里）：照抄原版 a(r) 的前半段即可，
            //     实际由 c.o 覆盖的 a(int) 负责弹「输入世界名称」，
            //     所以这里只需要调用 this.a(rVar.b)
            click.add(L_orig);
            click.add(new VarInsnNode(Opcodes.ALOAD, 0));
            click.add(new VarInsnNode(Opcodes.ALOAD, 1));
            click.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/c/r", "b", "I"));
            click.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, TARGET, "a", "(I)V", false));
            click.add(L_ret);
            click.add(new InsnNode(Opcodes.RETURN));
            mn.instructions.clear();
            mn.tryCatchBlocks.clear();
            if (mn.localVariables != null) {
                mn.localVariables.clear();
            }
            mn.instructions.add(click);
            mn.tryCatchBlocks.add(new TryCatchBlockNode(L_try, L_tryEnd, L_catch, "java/lang/Throwable"));
            mn.maxStack = 6;
            mn.maxLocals = Math.max(mn.maxLocals, 3);
            System.out.println("   已重写槽位点击 a(r)：载入=读所选存档 / 保存=走原逻辑弹输入名字");
        }

        // ★ 打点：也给 a(r)（槽位点击）和 f_()（每帧收尾）加一句状态打印，定位"点了没反应"
        for (MethodNode mn : cn.methods) {
            if (false && "a".equals(mn.name) && "(Lnet/minecraft/client/c/r;)V".equals(mn.desc)) {
                InsnList dbg = new InsnList();
                dbg.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
                dbg.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
                dbg.add(new InsnNode(Opcodes.DUP));
                dbg.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false));
                dbg.add(new LdcInsnNode("[QCL-saves] 槽位点击 id="));
                dbg.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
                dbg.add(new VarInsnNode(Opcodes.ALOAD, 1));
                dbg.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/c/r", "b", "I"));
                dbg.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", false));
                dbg.add(new LdcInsnNode(" enabled="));
                dbg.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
                dbg.add(new VarInsnNode(Opcodes.ALOAD, 1));
                dbg.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/c/r", "c", "Z"));
                dbg.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Z)Ljava/lang/StringBuilder;", false));
                dbg.add(new LdcInsnNode(" k="));
                dbg.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
                dbg.add(new VarInsnNode(Opcodes.ALOAD, 0));
                dbg.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD, TARGET, "k", "Z"));
                dbg.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Z)Ljava/lang/StringBuilder;", false));
                dbg.add(new LdcInsnNode(" n="));
                dbg.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
                dbg.add(new VarInsnNode(Opcodes.ALOAD, 0));
                dbg.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD, TARGET, "n", "Z"));
                dbg.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Z)Ljava/lang/StringBuilder;", false));
                dbg.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false));
                dbg.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false));
                mn.instructions.insert(dbg);
                mn.maxStack = Math.max(mn.maxStack, 5);
            }
            if (("f_".equals(mn.name) || "e_".equals(mn.name) || "d_".equals(mn.name)
                    || "g_".equals(mn.name) || "c_".equals(mn.name)) && "()V".equals(mn.desc)) {
                // ★★★ 1.4.8：f_()（每帧收尾）里**读"要保存到哪个文件"，让保存真正落盘**。
                //   游戏的落盘逻辑本来就在 f_() 里：
                //       if (o != null) { 补 .mclevel → this.a(o) 写盘 → o = null → 关界面 }
                //   原设计是文件对话框线程 c.f 先把 o 设好；而 run() 已被我们清空（原版会去连
                //   2010 年那台已关闭的服务器、会卡死）→ 那条路没了 → o 永远是 null →
                //   玩家点保存什么都不发生（用户实测："点击保存之后没反应"）。
                //   现在：点保存时（c.p）把目标路径写进系统属性 qcl.savefile，
                //   这里取出来塞进 this.o，下一帧原版逻辑就会真的写盘。
                //   ⚠️ 只保留"从属性取目标文件"这一种注入 —— 绝不能再加"o 为空就自动取最新存档"
                //      （那会抢走玩家的选择权，正是本版修掉的另一个 bug）。
                InsnList save = new InsnList();
                LabelNode L_skipSave = new LabelNode();
                // if (this.o != null) goto skip;
                save.add(new VarInsnNode(Opcodes.ALOAD, 0));
                save.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "o", "Ljava/io/File;"));
                save.add(new JumpInsnNode(Opcodes.IFNONNULL, L_skipSave));
                // if (System.getProperty("qcl.savefile") == null) goto skip;
                save.add(new LdcInsnNode("qcl.savefile"));
                save.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/System", "getProperty",
                        "(Ljava/lang/String;)Ljava/lang/String;", false));
                save.add(new JumpInsnNode(Opcodes.IFNULL, L_skipSave));
                // this.o = new File(System.getProperty("qcl.savefile"));
                save.add(new VarInsnNode(Opcodes.ALOAD, 0));
                save.add(new TypeInsnNode(Opcodes.NEW, "java/io/File"));
                save.add(new InsnNode(Opcodes.DUP));
                save.add(new LdcInsnNode("qcl.savefile"));
                save.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/System", "getProperty",
                        "(Ljava/lang/String;)Ljava/lang/String;", false));
                save.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/File", "<init>",
                        "(Ljava/lang/String;)V", false));
                save.add(new FieldInsnNode(Opcodes.PUTFIELD, TARGET, "o", "Ljava/io/File;"));
                // System.clearProperty("qcl.savefile")  —— 用掉就清，避免反复触发
                save.add(new LdcInsnNode("qcl.savefile"));
                save.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/System", "clearProperty",
                        "(Ljava/lang/String;)Ljava/lang/String;", false));
                save.add(new InsnNode(Opcodes.POP));
                // 日志
                save.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out",
                        "Ljava/io/PrintStream;"));
                save.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
                save.add(new InsnNode(Opcodes.DUP));
                save.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder",
                        "<init>", "()V", false));
                save.add(new LdcInsnNode("[QCL-saves] 开始写盘: "));
                save.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                        "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
                save.add(new VarInsnNode(Opcodes.ALOAD, 0));
                save.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "o", "Ljava/io/File;"));
                save.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                        "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false));
                save.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString",
                        "()Ljava/lang/String;", false));
                save.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                        "(Ljava/lang/String;)V", false));
                save.add(L_skipSave);
                mn.instructions.insert(save);
                mn.maxStack = Math.max(mn.maxStack, 6);
                System.out.println("   已给 f_() 注入：从 qcl.savefile 取目标文件，让保存真正落盘");
            }
        }

        // ★ 说明：曾经在这里重写 a(r)/f_()，因引入 VerifyError 与卡死已回退；只保留 run() 注入。

        // ★★★ 保存界面（c.o）的槽位"可点"修复。
        //   基类点击流程是「if (r.c) a(r)」—— c.r.c 才是"可点"标志；
        //   而游戏自己的保存界面 c.o.a(String[]) **只设了 d（显示）没设 c**，
        //   所以原始版本里保存界面的槽位一个都点不动（游戏自身缺陷）。
        //   这里在 c.o.a(String[]) 开头补上 c = true。
        try {
            ClassNode co;
            ZipFile zfo = new ZipFile(jar);
            try {
                ZipEntry zeo = zfo.getEntry("net/minecraft/client/c/o.class");
                if (zeo == null) {
                    System.out.println("   注：该版本没有 c/o.class（保存界面类）");
                    co = null;
                } else {
                    InputStream iso = zfo.getInputStream(zeo);
                    co = new ClassNode();
                    new ClassReader(iso).accept(co, 0);
                    iso.close();
                }
            } finally {
                zfo.close();
            }
            if (co != null) {
                int n = 0;
                for (MethodNode mn : co.methods) {
                    if (!"a".equals(mn.name) || !"([Ljava/lang/String;)V".equals(mn.desc)) {
                        continue;
                    }
                    InsnList pre = new InsnList();
                    for (int i = 0; i < 6; i++) {
                        pre.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        pre.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/c/o",
                                "e", "Ljava/util/List;"));
                        pre.add(new InsnNode(Opcodes.ICONST_0 + i));
                        pre.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/List", "get",
                                "(I)Ljava/lang/Object;", true));
                        pre.add(new TypeInsnNode(Opcodes.CHECKCAST, "net/minecraft/client/c/r"));
                        pre.add(new InsnNode(Opcodes.ICONST_1));
                        pre.add(new FieldInsnNode(Opcodes.PUTFIELD, "net/minecraft/client/c/r", "c", "Z"));
                    }
                    // 自检日志
                    pre.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
                    pre.add(new LdcInsnNode("[QCL-saves] 保存界面：已把 6 个按钮标成可点"));
                    pre.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                            "(Ljava/lang/String;)V", false));
                    mn.instructions.insert(pre);
                    mn.maxStack = Math.max(mn.maxStack, 3);
                    n++;
                }
                if (n > 0) {
                    File outO = new File(out.getParentFile(), "o.class");
                    ClassWriter cwo = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
                        @Override
                        protected String getCommonSuperClass(String t1, String t2) {
                            try {
                                return super.getCommonSuperClass(t1, t2);
                            } catch (Throwable t) {
                                return "java/lang/Object";
                            }
                        }
                    };
                    co.accept(cwo);
                    FileOutputStream foso = new FileOutputStream(outO);
                    foso.write(cwo.toByteArray());
                    foso.close();
                    System.out.println("   已生成保存界面补丁: " + outO.getAbsolutePath() + "（6 个按钮标可点）");
                } else {
                    System.out.println("   !! c/o 里没找到 a(String[])");
                }
            }
        } catch (Throwable t) {
            System.out.println("   保存界面补丁失败（不致命）: " + t);
        }


        // ★★★ 1.4.8：**不再找"每帧收尾"方法注入任何东西**。
        //   历史上就是这段代码在每帧把最新存档塞进 this.o，导致玩家一点「载入世界」
        //   就被自动读档、跳过选择界面（用户实测反馈："选择全丢了、直接进去了"）。
        //   现在读档只由槽位点击 a(r) 负责，这里保持完全不注入。
        // ④ ★★★ 「输入世界名称」界面（c.p）：把玩家输入的名字**存到系统属性**里，
        //    否则这个名字会被游戏直接丢掉（c.p 只拿它控制"保存"按钮是否可点，然后切回上一屏），
        //    接着 c.f 开文件对话框时又从不调用 setFile() —— 结果玩家输的名字根本没参与保存，
        //    存档被存成"世界2.mclevel"这种新文件（用户表现："输名字没用、它自己就保存了"）。
        //    对话框侧（cacio 补丁）会读这个属性，把它作为存档文件名。
        //    插在"切回上一屏"之前：aload_0 / getfield b / aconst_null / invokevirtual d.a(...)
        try {
            ZipEntry zeP = null;
            ZipFile zf2 = new ZipFile(jar);
            try {
                zeP = zf2.getEntry("net/minecraft/client/c/p.class");
            } finally {
                zf2.close();
            }
            if (zeP == null) {
                System.out.println("   注：该版本没有 c/p.class（无「输入世界名称」界面），跳过取名补丁");
            } else {
                ClassNode cp;
                ZipFile zf3 = new ZipFile(jar);
                try {
                    InputStream is = zf3.getInputStream(zf3.getEntry("net/minecraft/client/c/p.class"));
                    cp = new ClassNode();
                    new ClassReader(is).accept(cp, 0);
                    is.close();
                } finally {
                    zf3.close();
                }
                int done = 0;
                for (MethodNode mn : cp.methods) {
                    if (!"a".equals(mn.name) || !"(Lnet/minecraft/client/c/r;)V".equals(mn.desc)) {
                        continue;
                    }
                    // 找 aconst_null 后面紧跟 d.a(...) 的那个点
                    AbstractInsnNode anchor = null;
                    for (AbstractInsnNode p = mn.instructions.getFirst(); p != null; p = p.getNext()) {
                        if (p.getOpcode() == Opcodes.ACONST_NULL && p.getNext() != null
                                && p.getNext() instanceof MethodInsnNode) {
                            MethodInsnNode mi = (MethodInsnNode) p.getNext();
                            if ("net/minecraft/client/d".equals(mi.owner) && "a".equals(mi.name)) {
                                anchor = p;
                                break;
                            }
                        }
                    }
                    if (anchor == null) {
                        continue;
                    }
                    InsnList put = new InsnList();
                    // ★★★ 关键：把"要保存到哪个文件"直接设好 —— 这才是真正落盘的关键。
                    //   游戏的落盘发生在每帧的 f_() 里：
                    //       if (o != null) { 补 .mclevel → this.a(o) 写盘 → o = null → 关界面 }
                    //   原设计是文件对话框线程 c.f 先把 o 设好；run() 已被我们清空 → 那条路没了，
                    //   o 永远是 null → 点保存什么都不发生（用户实测："点保存之后没反应"）。
                    //   这里在"点保存"时直接算出 saves/<名字>.mclevel 交付给它。
                    //   ① System.setProperty("qcl.savefile", <绝对路径>) —— 由 f_() 读取后落盘
                    //      （不能在 c.p 里直接写 c.e.o：编译期类型是 c.p，没有那个字段）
                    put.add(new LdcInsnNode("qcl.savefile"));
                    put.add(new TypeInsnNode(Opcodes.NEW, "java/io/File"));
                    put.add(new InsnNode(Opcodes.DUP));
                    put.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    put.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/c/p",
                            "b", "Lnet/minecraft/client/d;"));
                    put.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/d",
                            "z", "Ljava/io/File;"));
                    put.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
                    put.add(new InsnNode(Opcodes.DUP));
                    put.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder",
                            "<init>", "()V", false));
                    put.add(new LdcInsnNode("saves/"));
                    put.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                            "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
                    put.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    put.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/c/p",
                            "k", "Ljava/lang/String;"));
                    put.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "trim",
                            "()Ljava/lang/String;", false));
                    put.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                            "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
                    put.add(new LdcInsnNode(".mclevel"));
                    put.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                            "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
                    put.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString",
                            "()Ljava/lang/String;", false));
                    put.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/File", "<init>",
                            "(Ljava/io/File;Ljava/lang/String;)V", false));
                    put.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "getAbsolutePath",
                            "()Ljava/lang/String;", false));
                    put.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/System", "setProperty",
                            "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", false));
                    put.add(new InsnNode(Opcodes.POP));
                    //   ② 顺手保留名字（cacio 对话框侧兜底用）
                    put.add(new LdcInsnNode("qcl.savename"));
                    put.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    put.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/c/p",
                            "k", "Ljava/lang/String;"));
                    put.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "trim",
                            "()Ljava/lang/String;", false));
                    put.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/System", "setProperty",
                            "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", false));
                    put.add(new InsnNode(Opcodes.POP));
                    //   ③ 自检日志
                    put.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out",
                            "Ljava/io/PrintStream;"));
                    put.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
                    put.add(new InsnNode(Opcodes.DUP));
                    put.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder",
                            "<init>", "()V", false));
                    put.add(new LdcInsnNode("[QCL-saves] 点保存，目标文件="));
                    put.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                            "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
                    put.add(new LdcInsnNode("qcl.savefile"));
                    put.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/System", "getProperty",
                            "(Ljava/lang/String;)Ljava/lang/String;", false));
                    put.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                            "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false));
                    put.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString",
                            "()Ljava/lang/String;", false));
                    put.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                            "(Ljava/lang/String;)V", false));
                    // 插在 aconst_null 之前：不会破坏后面 aload_0 / getfield b / d.a(...) 的栈
                    mn.instructions.insertBefore(anchor, put);
                    mn.maxStack = Math.max(mn.maxStack, 6);
                    done++;
                }
                if (done > 0) {
                    File outP = new File(out.getParentFile(), "p.class");
                    ClassWriter cwP = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
                        @Override
                        protected String getCommonSuperClass(String t1, String t2) {
                            try {
                                return super.getCommonSuperClass(t1, t2);
                            } catch (Throwable t) {
                                return "java/lang/Object";
                            }
                        }
                    };
                    cp.accept(cwP);
                    FileOutputStream fosP = new FileOutputStream(outP);
                    fosP.write(cwP.toByteArray());
                    fosP.close();
                    System.out.println("   已生成取名补丁: " + outP.getAbsolutePath() + "（改了 " + done + " 个方法）");
                } else {
                    System.out.println("   !! c/p 里没找到可插入点");
                }
            }
        } catch (Throwable t) {
            System.out.println("   取名补丁失败（不致命）: " + t);
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                // 补丁类引用的都是游戏类/JDK 类；解析不了就退回 Object，够用且不会抛异常
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
        System.out.println("已生成补丁类: " + out.getAbsolutePath());
    }

    /**
     * 注入 {@code private static void qclFillSaves(e self, File dir)}：
     * 扫 dir 下的 *.mclevel（最多 5 个），同时填
     * {@code self.l}（显示文本 "世界  -  10-03 17:00"）与 {@code self.qclSaves}（对应的 File 数组）。
     * ★ 为什么必须同时填文件数组：槽位点击时用 {@code qclSaves[id]} 去加载；
     * 只填文本的话点击会取空数组 → ArrayIndexOutOfBoundsException → 界面被清掉回主菜单。
     */
    private static MethodNode buildFillMethod() {
        MethodNode m = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC,
                FILL_METHOD, "(Ljava/io/File;)V", null, null);
        InsnList c = m.instructions;

        LabelNode L_start = new LabelNode();
        LabelNode L_iLoop = new LabelNode();
        LabelNode L_iDone = new LabelNode();
        LabelNode L_scanLoop = new LabelNode();
        LabelNode L_scanNext = new LabelNode();
        LabelNode L_noStrip = new LabelNode();
        LabelNode L_end = new LabelNode();
        LabelNode L_catch = new LabelNode();

        int slotSelf = 0;    // this（实例方法：ALOAD 0 直接就是 this）
        int slotDir = 1;     // File
        int slotOut = 2;     // String[]
        int slotFiles = 3;   // File[]
        int slotI = 4;       // int
        int slotAll = 5;     // File[]
        int slotJ = 6;       // int
        int slotCount = 7;   // int
        int slotF = 8;       // File
        int slotName = 9;    // String
        int slotTmp = 10;    // Throwable
        int slotMaxLocals = 11;

        // out = new String[5]; files = new File[5];
        c.add(new InsnNode(Opcodes.ICONST_5));
        c.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));
        c.add(new VarInsnNode(Opcodes.ASTORE, slotOut));
        c.add(new InsnNode(Opcodes.ICONST_5));
        c.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/io/File"));
        c.add(new VarInsnNode(Opcodes.ASTORE, slotFiles));
        // for (i=0;i<5;i++) out[i]="-";
        c.add(new InsnNode(Opcodes.ICONST_0));
        c.add(new VarInsnNode(Opcodes.ISTORE, slotI));
        c.add(L_iLoop);
        c.add(new VarInsnNode(Opcodes.ILOAD, slotI));
        c.add(new InsnNode(Opcodes.ICONST_5));
        c.add(new JumpInsnNode(Opcodes.IF_ICMPGE, L_iDone));
        c.add(new VarInsnNode(Opcodes.ALOAD, slotOut));
        c.add(new VarInsnNode(Opcodes.ILOAD, slotI));
        c.add(new LdcInsnNode("-"));
        c.add(new InsnNode(Opcodes.AASTORE));
        c.add(new IincInsnNode(slotI, 1));
        c.add(new JumpInsnNode(Opcodes.GOTO, L_iLoop));
        c.add(L_iDone);

        // try { ... } catch (Throwable t) { }
        c.add(L_start);
        c.add(new VarInsnNode(Opcodes.ALOAD, slotDir));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "listFiles",
                "()[Ljava/io/File;", false));
        c.add(new VarInsnNode(Opcodes.ASTORE, slotAll));
        c.add(new InsnNode(Opcodes.ICONST_0));
        c.add(new VarInsnNode(Opcodes.ISTORE, slotCount));
        c.add(new InsnNode(Opcodes.ICONST_0));
        c.add(new VarInsnNode(Opcodes.ISTORE, slotJ));
        c.add(L_scanLoop);
        c.add(new VarInsnNode(Opcodes.ILOAD, slotJ));
        c.add(new VarInsnNode(Opcodes.ALOAD, slotAll));
        c.add(new InsnNode(Opcodes.ARRAYLENGTH));
        c.add(new JumpInsnNode(Opcodes.IF_ICMPGE, L_end));
        c.add(new VarInsnNode(Opcodes.ILOAD, slotCount));
        c.add(new InsnNode(Opcodes.ICONST_5));
        c.add(new JumpInsnNode(Opcodes.IF_ICMPGE, L_end));
        c.add(new VarInsnNode(Opcodes.ALOAD, slotAll));
        c.add(new VarInsnNode(Opcodes.ILOAD, slotJ));
        c.add(new InsnNode(Opcodes.AALOAD));
        c.add(new VarInsnNode(Opcodes.ASTORE, slotF));
        // if (!f.isFile()) continue;
        c.add(new VarInsnNode(Opcodes.ALOAD, slotF));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "isFile", "()Z", false));
        c.add(new JumpInsnNode(Opcodes.IFEQ, L_scanNext));
        // if (!f.getName().toLowerCase().endsWith(".mclevel")) continue;
        c.add(new VarInsnNode(Opcodes.ALOAD, slotF));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "getName", "()Ljava/lang/String;", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "toLowerCase", "()Ljava/lang/String;", false));
        c.add(new LdcInsnNode(".mclevel"));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "endsWith", "(Ljava/lang/String;)Z", false));
        c.add(new JumpInsnNode(Opcodes.IFEQ, L_scanNext));
        // files[count] = f;   ★ 关键
        c.add(new VarInsnNode(Opcodes.ALOAD, slotFiles));
        c.add(new VarInsnNode(Opcodes.ILOAD, slotCount));
        c.add(new VarInsnNode(Opcodes.ALOAD, slotF));
        c.add(new InsnNode(Opcodes.AASTORE));
        // name = strip(f.getName())
        c.add(new VarInsnNode(Opcodes.ALOAD, slotF));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "getName", "()Ljava/lang/String;", false));
        c.add(new VarInsnNode(Opcodes.ASTORE, slotName));
        c.add(new VarInsnNode(Opcodes.ALOAD, slotName));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "toLowerCase", "()Ljava/lang/String;", false));
        c.add(new LdcInsnNode(".mclevel"));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "endsWith", "(Ljava/lang/String;)Z", false));
        c.add(new JumpInsnNode(Opcodes.IFEQ, L_noStrip));
        c.add(new VarInsnNode(Opcodes.ALOAD, slotName));
        c.add(new InsnNode(Opcodes.ICONST_0));
        c.add(new VarInsnNode(Opcodes.ALOAD, slotName));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "length", "()I", false));
        c.add(new org.objectweb.asm.tree.IntInsnNode(Opcodes.BIPUSH, 8));
        c.add(new InsnNode(Opcodes.ISUB));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "substring",
                "(II)Ljava/lang/String;", false));
        c.add(new VarInsnNode(Opcodes.ASTORE, slotName));
        c.add(L_noStrip);
        // out[count] = name + "  -  " + fmt(new Date(f.lastModified()))
        c.add(new VarInsnNode(Opcodes.ALOAD, slotOut));
        c.add(new VarInsnNode(Opcodes.ILOAD, slotCount));
        c.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false));
        c.add(new VarInsnNode(Opcodes.ALOAD, slotName));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
        c.add(new LdcInsnNode("  -  "));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
        c.add(new TypeInsnNode(Opcodes.NEW, "java/text/SimpleDateFormat"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new LdcInsnNode("MM-dd HH:mm"));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/text/SimpleDateFormat", "<init>",
                "(Ljava/lang/String;)V", false));
        c.add(new TypeInsnNode(Opcodes.NEW, "java/util/Date"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new VarInsnNode(Opcodes.ALOAD, slotF));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "lastModified", "()J", false));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/Date", "<init>", "(J)V", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/text/SimpleDateFormat", "format",
                "(Ljava/util/Date;)Ljava/lang/String;", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString",
                "()Ljava/lang/String;", false));
        c.add(new InsnNode(Opcodes.AASTORE));
        c.add(new IincInsnNode(slotCount, 1));
        c.add(L_scanNext);
        c.add(new IincInsnNode(slotJ, 1));
        c.add(new JumpInsnNode(Opcodes.GOTO, L_scanLoop));

        c.add(L_end);
        c.add(new VarInsnNode(Opcodes.ALOAD, slotSelf));
        c.add(new VarInsnNode(Opcodes.ALOAD, slotOut));
        c.add(new FieldInsnNode(Opcodes.PUTFIELD, TARGET, "l", "[Ljava/lang/String;"));
        c.add(new VarInsnNode(Opcodes.ALOAD, slotSelf));
        c.add(new VarInsnNode(Opcodes.ALOAD, slotFiles));
        c.add(new FieldInsnNode(Opcodes.PUTSTATIC, TARGET, SAVES_FIELD, "[Ljava/io/File;"));
        c.add(new InsnNode(Opcodes.RETURN));
        // catch (Throwable t) { t.printStackTrace(); self.l = out; self.qclSaves = files; }
        c.add(L_catch);
        c.add(new VarInsnNode(Opcodes.ASTORE, slotTmp));
        c.add(new VarInsnNode(Opcodes.ALOAD, slotTmp));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V", false));
        c.add(new VarInsnNode(Opcodes.ALOAD, slotSelf));
        c.add(new VarInsnNode(Opcodes.ALOAD, slotOut));
        c.add(new FieldInsnNode(Opcodes.PUTFIELD, TARGET, "l", "[Ljava/lang/String;"));
        c.add(new VarInsnNode(Opcodes.ALOAD, slotSelf));
        c.add(new VarInsnNode(Opcodes.ALOAD, slotFiles));
        c.add(new FieldInsnNode(Opcodes.PUTSTATIC, TARGET, SAVES_FIELD, "[Ljava/io/File;"));
        c.add(new InsnNode(Opcodes.RETURN));

        m.tryCatchBlocks.add(new TryCatchBlockNode(L_start, L_end, L_catch, "java/lang/Throwable"));
        m.maxStack = 8;
        m.maxLocals = slotMaxLocals;
        return m;
    }

    /**
     * 注入 {@code private static String[] qclListSaves(File dir)}：
     * 列出 dir 下的 *.mclevel，最多 5 个，形如 {@code 世界  -  10-03 17:00}；没有则返回全 "-"。
     * 纯 JDK API，不引用任何游戏类。
     */
    private static MethodNode buildListMethod() {
        MethodNode m = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                LIST_METHOD, LIST_DESC, null, null);
        InsnList c = m.instructions;

        LabelNode L_start = new LabelNode();
        LabelNode L_iLoop = new LabelNode();
        LabelNode L_iDone = new LabelNode();
        LabelNode L_scanLoop = new LabelNode();
        LabelNode L_scanNext = new LabelNode();
        LabelNode L_noStrip = new LabelNode();
        LabelNode L_end = new LabelNode();
        LabelNode L_catch = new LabelNode();

        int slotOut = 1;
        int slotI = 2;
        int slotAll = 3;
        int slotJ = 4;
        int slotCount = 5;
        int slotF = 6;
        int slotName = 7;
        int slotTmp = 8;
        int slotMaxLocals = 9;

        // String[] out = new String[5]; for (i=0;i<5;i++) out[i]="-";
        c.add(new InsnNode(Opcodes.ICONST_5));
        c.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));
        c.add(new VarInsnNode(Opcodes.ASTORE, slotOut));
        c.add(new InsnNode(Opcodes.ICONST_0));
        c.add(new VarInsnNode(Opcodes.ISTORE, slotI));
        c.add(L_iLoop);
        c.add(new VarInsnNode(Opcodes.ILOAD, slotI));
        c.add(new InsnNode(Opcodes.ICONST_5));
        c.add(new JumpInsnNode(Opcodes.IF_ICMPGE, L_iDone));
        c.add(new VarInsnNode(Opcodes.ALOAD, slotOut));
        c.add(new VarInsnNode(Opcodes.ILOAD, slotI));
        c.add(new LdcInsnNode("-"));
        c.add(new InsnNode(Opcodes.AASTORE));
        c.add(new IincInsnNode(slotI, 1));
        c.add(new JumpInsnNode(Opcodes.GOTO, L_iLoop));
        c.add(L_iDone);

        // try { all = dir.listFiles(); count=0; for (j...) { ... } } catch (Throwable t) {}
        c.add(L_start);
        c.add(new VarInsnNode(Opcodes.ALOAD, 0));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "listFiles",
                "()[Ljava/io/File;", false));
        c.add(new VarInsnNode(Opcodes.ASTORE, slotAll));
        c.add(new InsnNode(Opcodes.ICONST_0));
        c.add(new VarInsnNode(Opcodes.ISTORE, slotCount));
        c.add(new InsnNode(Opcodes.ICONST_0));
        c.add(new VarInsnNode(Opcodes.ISTORE, slotJ));
        c.add(L_scanLoop);
        c.add(new VarInsnNode(Opcodes.ILOAD, slotJ));
        c.add(new VarInsnNode(Opcodes.ALOAD, slotAll));
        c.add(new InsnNode(Opcodes.ARRAYLENGTH));
        c.add(new JumpInsnNode(Opcodes.IF_ICMPGE, L_end));
        c.add(new VarInsnNode(Opcodes.ILOAD, slotCount));
        c.add(new InsnNode(Opcodes.ICONST_5));
        c.add(new JumpInsnNode(Opcodes.IF_ICMPGE, L_end));
        // File f = all[j]
        c.add(new VarInsnNode(Opcodes.ALOAD, slotAll));
        c.add(new VarInsnNode(Opcodes.ILOAD, slotJ));
        c.add(new InsnNode(Opcodes.AALOAD));
        c.add(new VarInsnNode(Opcodes.ASTORE, slotF));
        c.add(new VarInsnNode(Opcodes.ALOAD, slotF));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "isFile", "()Z", false));
        c.add(new JumpInsnNode(Opcodes.IFEQ, L_scanNext));
        c.add(new VarInsnNode(Opcodes.ALOAD, slotF));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "getName", "()Ljava/lang/String;", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "toLowerCase", "()Ljava/lang/String;", false));
        c.add(new LdcInsnNode(".mclevel"));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "endsWith", "(Ljava/lang/String;)Z", false));
        c.add(new JumpInsnNode(Opcodes.IFEQ, L_scanNext));
        // name = f.getName(); strip ".mclevel"
        c.add(new VarInsnNode(Opcodes.ALOAD, slotF));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "getName", "()Ljava/lang/String;", false));
        c.add(new VarInsnNode(Opcodes.ASTORE, slotName));
        c.add(new VarInsnNode(Opcodes.ALOAD, slotName));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "toLowerCase", "()Ljava/lang/String;", false));
        c.add(new LdcInsnNode(".mclevel"));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "endsWith", "(Ljava/lang/String;)Z", false));
        c.add(new JumpInsnNode(Opcodes.IFEQ, L_noStrip));
        c.add(new VarInsnNode(Opcodes.ALOAD, slotName));
        c.add(new InsnNode(Opcodes.ICONST_0));
        c.add(new VarInsnNode(Opcodes.ALOAD, slotName));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "length", "()I", false));
        c.add(new org.objectweb.asm.tree.IntInsnNode(Opcodes.BIPUSH, 8));
        c.add(new InsnNode(Opcodes.ISUB));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "substring",
                "(II)Ljava/lang/String;", false));
        c.add(new VarInsnNode(Opcodes.ASTORE, slotName));
        c.add(L_noStrip);
        // out[count] = name + "  -  " + fmt(new Date(f.lastModified()))
        c.add(new VarInsnNode(Opcodes.ALOAD, slotOut));
        c.add(new VarInsnNode(Opcodes.ILOAD, slotCount));
        c.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false));
        c.add(new VarInsnNode(Opcodes.ALOAD, slotName));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
        c.add(new LdcInsnNode("  -  "));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
        c.add(new TypeInsnNode(Opcodes.NEW, "java/text/SimpleDateFormat"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new LdcInsnNode("MM-dd HH:mm"));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/text/SimpleDateFormat", "<init>",
                "(Ljava/lang/String;)V", false));
        c.add(new TypeInsnNode(Opcodes.NEW, "java/util/Date"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new VarInsnNode(Opcodes.ALOAD, slotF));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "lastModified", "()J", false));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/Date", "<init>", "(J)V", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/text/SimpleDateFormat", "format",
                "(Ljava/util/Date;)Ljava/lang/String;", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString",
                "()Ljava/lang/String;", false));
        c.add(new InsnNode(Opcodes.AASTORE));
        c.add(new IincInsnNode(slotCount, 1));
        c.add(L_scanNext);
        c.add(new IincInsnNode(slotJ, 1));
        c.add(new JumpInsnNode(Opcodes.GOTO, L_scanLoop));

        c.add(L_end);
        c.add(new VarInsnNode(Opcodes.ALOAD, slotOut));
        c.add(new InsnNode(Opcodes.ARETURN));
        c.add(L_catch);
        c.add(new VarInsnNode(Opcodes.ASTORE, slotTmp));
        c.add(new VarInsnNode(Opcodes.ALOAD, slotOut));
        c.add(new InsnNode(Opcodes.ARETURN));

        m.tryCatchBlocks.add(new TryCatchBlockNode(L_start, L_end, L_catch, "java/lang/Throwable"));
        m.maxStack = 8;
        m.maxLocals = slotMaxLocals;
        return m;
    }
}
