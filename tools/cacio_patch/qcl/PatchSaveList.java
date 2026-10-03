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

        // ② b()：在**所有槽位按钮都创建完之后**（方法末尾）填本地列表。
        //    这样槽位对象一定已存在，不会 NPE；且改动极小、帧简单。
        for (MethodNode mn : cn.methods) {
            if ("b".equals(mn.name) && "()V".equals(mn.desc)) {
                InsnList fill = new InsnList();
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
                // this.k = true —— 让 a(r) 走"列表模式"
                fill.add(new VarInsnNode(Opcodes.ALOAD, 0));
                fill.add(new InsnNode(Opcodes.ICONST_1));
                fill.add(new FieldInsnNode(Opcodes.PUTFIELD, TARGET, "k", "Z"));
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

        // ★ 打点：也给 a(r)（槽位点击）和 f_()（每帧收尾）加一句状态打印，定位"点了没反应"
        for (MethodNode mn : cn.methods) {
            if ("a".equals(mn.name) && "(Lnet/minecraft/client/c/r;)V".equals(mn.desc)) {
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
            if ("f_".equals(mn.name) && "()V".equals(mn.desc)) {
                // ★★ 最后一步：在 f_() **开头**插入
                //    if (this.o == null && qclSaves != null && qclSaves.length > 0) this.o = qclSaves[0];
                //    之后原版 f_() 的「补 .mclevel → a(this.o) 读档 → 关界面」就会真正执行。
                //    只加前缀、完全不动原逻辑（原逻辑里有合成访问桥的调用，不能打散）。
                InsnList pre = new InsnList();
                LabelNode L_skip = new LabelNode();
                pre.add(new VarInsnNode(Opcodes.ALOAD, 0));
                pre.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD, TARGET, "o", "Ljava/io/File;"));
                pre.add(new JumpInsnNode(Opcodes.IFNONNULL, L_skip));
                pre.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETSTATIC, TARGET, SAVES_FIELD, "[Ljava/io/File;"));
                pre.add(new JumpInsnNode(Opcodes.IFNULL, L_skip));
                pre.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETSTATIC, TARGET, SAVES_FIELD, "[Ljava/io/File;"));
                pre.add(new InsnNode(Opcodes.ARRAYLENGTH));
                pre.add(new JumpInsnNode(Opcodes.IFEQ, L_skip));
                pre.add(new VarInsnNode(Opcodes.ALOAD, 0));
                pre.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETSTATIC, TARGET, SAVES_FIELD, "[Ljava/io/File;"));
                pre.add(new InsnNode(Opcodes.ICONST_0));
                pre.add(new InsnNode(Opcodes.AALOAD));
                pre.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.PUTFIELD, TARGET, "o", "Ljava/io/File;"));
                pre.add(L_skip);
                // 打点（保留，方便下次排查）
                pre.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
                pre.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
                pre.add(new InsnNode(Opcodes.DUP));
                pre.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false));
                pre.add(new LdcInsnNode("[QCL-saves] f_() o="));
                pre.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
                pre.add(new VarInsnNode(Opcodes.ALOAD, 0));
                pre.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD, TARGET, "o", "Ljava/io/File;"));
                pre.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false));
                pre.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false));
                pre.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false));
                mn.instructions.insert(pre);
                mn.maxStack = Math.max(mn.maxStack, 5);
            }
        }

        // ★ 说明：曾经在这里重写 a(r)/f_()，因引入 VerifyError 与卡死已回退；只保留 run() 注入。

        // ③ 找到"每帧收尾"方法：in-20100223 叫 f_()，其余 infdev 叫 e_()/d_()（体一样、名字不同）
        //    → 按"无参无返回且在同一个类里"的名字集合自动识别。
        MethodNode tick = null;
        String tickName = null;
        for (String cand : new String[]{"f_", "e_", "d_", "g_", "c_"}) {
            for (MethodNode mn : cn.methods) {
                if (cand.equals(mn.name) && "()V".equals(mn.desc)) {
                    tick = mn;
                    tickName = cand;
                    break;
                }
            }
            if (tick != null) {
                break;
            }
        }
        if (tick != null) {
            System.out.println("   每帧收尾方法识别为: " + tickName + "()");
            InsnList prefix = new InsnList();
            LabelNode L_skip = new LabelNode();
            prefix.add(new VarInsnNode(Opcodes.ALOAD, 0));
            prefix.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "o", "Ljava/io/File;"));
            prefix.add(new JumpInsnNode(Opcodes.IFNONNULL, L_skip));
            prefix.add(new FieldInsnNode(Opcodes.GETSTATIC, TARGET, SAVES_FIELD, "[Ljava/io/File;"));
            prefix.add(new JumpInsnNode(Opcodes.IFNULL, L_skip));
            prefix.add(new FieldInsnNode(Opcodes.GETSTATIC, TARGET, SAVES_FIELD, "[Ljava/io/File;"));
            prefix.add(new InsnNode(Opcodes.ARRAYLENGTH));
            prefix.add(new JumpInsnNode(Opcodes.IFEQ, L_skip));
            prefix.add(new VarInsnNode(Opcodes.ALOAD, 0));
            prefix.add(new FieldInsnNode(Opcodes.GETSTATIC, TARGET, SAVES_FIELD, "[Ljava/io/File;"));
            prefix.add(new InsnNode(Opcodes.ICONST_0));
            prefix.add(new InsnNode(Opcodes.AALOAD));
            prefix.add(new FieldInsnNode(Opcodes.PUTFIELD, TARGET, "o", "Ljava/io/File;"));
            prefix.add(L_skip);
            tick.instructions.insert(prefix);
            tick.maxStack = Math.max(tick.maxStack, 3);
        } else {
            System.out.println("   !! 没找到每帧收尾方法（f_/e_/d_）");
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
