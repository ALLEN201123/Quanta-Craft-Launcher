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
import org.objectweb.asm.tree.IntInsnNode;
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
    /** TARGET 的 JVM 类型描述符（拼构造器/方法签名用）。 */
    private static final String TARGET_DESC = "Lnet/minecraft/client/c/e;";

    /**
     * ★★★【2026-10-06 关键】{@code net.minecraft.client.d.d}（地图数据）的**真实描述符**。
     *
     * <p>写存档要用它，而**每个版本的地图类都不一样**（实测 javap 结果）：
     * <pre>
     *   inf-20100227-1433 / inf-20100316      → Lnet/minecraft/a/a/e;
     *   inf-20100313     / inf-20100320 / 1857 → Lnet/minecraft/a/a/f;
     *   in-20100223      / inf-20100325-1640   → Lnet/minecraft/a/a/g;
     * </pre>
     * 之前硬编码成 {@code g}，于是其它版本运行时抛 {@code NoSuchFieldError}，
     * 「保存文件…」写盘失败、界面卡住在"正在保存"。
     *
     * <p>本字段由 {@link #detectMapDesc(java.util.zip.ZipFile)} 在打补丁前从
     * 目标版本的 {@code client/d.class} 里读出来，**不再靠猜**。
     */
    private static String MAPS_DESC = "Lnet/minecraft/a/a/g;";
    private static final String NEW_FIELD = "qclSavesLocal";
    private static final String NEW_FIELD_DESC = "[Ljava/lang/String;";
    private static final String LIST_METHOD = "qclListSaves";
    private static final String LIST_DESC = "(Ljava/io/File;)[Ljava/lang/String;";
    /** 注入的填充方法：void qclFillSaves(e self, File dir) —— 同时填显示文本与文件数组。 */
    private static final String FILL_METHOD = "qclFillSaves";
    /** 存"槽位 → 文件"映射的字段（槽位点击时用它去加载）。 */
    private static final String SAVES_FIELD = "qclSaves";

    /**
     * 按钮类上新增的「可点」字段，与 c 分开：c 只管「文字亮/灰」，这个只管「能不能点」。
     * 本版本原版用一个 c 同时管两件事 → 空槽位 / 取消 / 加载文件既显示灰色又永远点不动。
     * 字段与命中判定由 qcl.PatchButtonClick 注入。
     */
    private static final String QCL_FLAG = "qclClickable";

    /** PatchImportFile 注入的"收编手机存档"方法名（c.e 上的静态方法）。 */
    private static final String IMPORT_METHOD = "qclCollectExternal";

    /** qclCollectExternal 的方法描述符。 */
    private static final String IMPORT_DESC = "(Ljava/io/File;)[Ljava/io/File;";

    /** ★★★ 文件桥：向启动器请求“弹系统文件选择器”的方法名。
     *  游戏 JVM 弹不出安卓组件（实测 ClassNotFoundException: android/app/ActivityThread），
     *  所以改成写一个请求文件，启动器侧的 QclFileBridge 轮询到后弹选择器，
     *  并把结果写回响应文件。 */
    private static final String ASK_METHOD = "qclAskFile";
    private static final String ASK_DESC = "()V";
    /** ★ “保存文件…”走的桥接方法：先问玩家要存到哪个目录，再把世界写进去。 */
    private static final String ASK_SAVE = "qclAskSave";
    /** ★ 存盘第一步：先向玩家要一个世界名（启动器侧弹输入框）。 */
    private static final String ASK_NAME = "qclAskName";
    /** 待处理的存档文件（等待线程写入，f_() 每帧开头取走）。 */
    private static final String PENDING_FIELD = "qclPending";
    /** 把 qclPending 搬到 this.o 的公开方法（等待线程调用）。 */
    private static final String APPLY_METHOD = "qclApplyPending";
    /** 等待玩家选文件/输名字的方法（完全做在 c.e 自身，不跨类）。 */
    private static final String WAIT_METHOD = "qclWait";
    /** 等待选择存档的**实例**方法（直接读写 this.o，无需传 this）。 */
    private static final String WAIT_LOAD = "qclWaitLoad";
    /** 等待保存目录的**实例**方法。 */
    private static final String WAIT_SAVE = "qclWaitSave";
    /** 读小文本文件（信箱响应）的工具方法。 */
    private static final String READ_METHOD = "qclRead";
    /** 文件档案夹（必须与 QclFileBridge.DIR 一致）。 */
    private static final String MAILBOX = "/sdcard/QCL/.qcl_bridge";


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
            // ★★★【2026-10-06】先探测本版本 client/d.d 的真实描述符再干活。
            //   每个版本的地图类不同（a/a/e、a/a/f、a/a/g 都有），硬编码必错。
            detectMapDesc(zf);
        } finally {
            zf.close();
        }

        boolean hasField = false;
        boolean hasPending = false;
        for (FieldNode fn : cn.fields) {
            if (PENDING_FIELD.equals(fn.name)) {
                hasPending = true;
            }
        }
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
        // ★★★ 把 c.e 的 o（“要读/要写的存档文件”）改成 public。
        //   原版是 private，而我们的 c.qclFileWaiter（文件选择等待线程）需要在拿到
        //   玩家选的路径后把它设上。改可见性不改名字与类型，
        //   对原版其他代码完全无影响（访问器变宽不会引起校验问题）。
        for (FieldNode fn : cn.fields) {
            if ("o".equals(fn.name) && "Ljava/io/File;".equals(fn.desc)) {
                fn.access = Opcodes.ACC_PUBLIC;
                System.out.println("   已把 c.e.o 改为 public");
            }
        }

        // ★ 槽位 → 文件 的映射（槽位点击时 a(r) 会读 qclSaves[id]）
        if (!hasSavesField) {
            cn.fields.add(new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                    SAVES_FIELD, "[Ljava/io/File;", null, null));
        }
        // ★★★ 非阻塞文件选择用的两个标志：
        //   qclPickerBusy —— 文件浏览器已经弹出了，别重复弹；
        //   qclWantSave   —— 这次是"保存"还是"读档"（选择结果回来后接着做对应的事）。
        //   ⚠️ 游戏线程**绝不能**等选择结果：游戏主循环就靠它跑，
        //      一旦阻塞，界面不刷新、结果也回不来，最后只能超时回退成"自动选最新存档"。
        cn.fields.add(new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "qclPickerBusy", "Z", null, null));
        cn.fields.add(new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "qclWantSave", "Z", null, null));
        // ★★★ 游戏内文件浏览器（在游戏自己的界面里选存档文件）：
        //   qclBrowseOn    —— 现在是否处于"浏览模式"（点击走浏览器逻辑）
        //   qclBrowseDir   —— 当前浏览的目录
        //   qclBrowseFiles —— 界面上每一项对应的 File（null = "返回上一级"）
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

        // 【2026-10-05 事故修复】下面这一整段（c.o 保存界面补丁）的「外壳」在批量
        //   删除调试日志时被一起删掉了 —— try 开头和 if (co != null) 判断都没了，
        //   只剩中间的主体，导致 try/catch 语法不成立。
        //   这里按 c.p 那一段（L381 起）的同样写法补回来：
        //     try { if (co != null) { ...遍历 a(String[]) 注入按钮标记... } } catch (Throwable t) {...}
        //   ⚠ 教训：删代码必须逐处精确编辑，绝不可用正则批量删除。
        try {
            // 【2026-10-05 事故修复】c/o.class 的读取也被一起删掉了，这里补回来。
            //   思路与 c.p 段（L391 起）一致：先取出该版本 jar 里的 c/o.class，
            //   没有这个类就跳过（远古早期版本没有独立的保存界面类）。
            ClassNode co = null;
            {
                ZipEntry zeO = null;
                ZipFile zfO = new ZipFile(jar);
                try {
                    zeO = zfO.getEntry("net/minecraft/client/c/o.class");
                } finally {
                    zfO.close();
                }
                if (zeO == null) {
                    System.out.println("   注：该版本没有 c/o.class（保存界面类），跳过保存界面补丁");
                } else {
                    InputStream iso = new ZipFile(jar).getInputStream(zeO);
                    co = new ClassNode();
                    new ClassReader(iso).accept(co, 0);
                    iso.close();
                }
            }
            if (co != null) {
            // 【2026-10-05 事故修复】计数器 n 的声明也被一起删掉了，这里补回来。
            //   用途：统计在 c.o 里成功注入了多少个 a(String[]) 方法（正常应为 1）。
            int n = 0;

        // ② b()：在**所有槽位/按钮都创建完之后**（方法末尾）处理槽位。
        //    ★★ 两个界面共用这一套代码（c.o 继承 c.e），必须**可靠区分**：
        //      载入界面：5 槽位 + 「加载文件…」+「取消」
        //      保存界面：5 槽位 + 「保存文件…」+「取消」
        //   区分方法：取第 6 个按钮（索引 5）的文字，startsWith("保存") → 保存界面。
        //   （早期版本用标题字段 a 判断，实测取不到，导致保存界面被当成载入界面处理 ——
        //     于是只有"有存档"的槽位被标成可点，3/4/5 空槽位点不了；用户实测反馈过。）
        //   保存界面要做的：① 不填存档列表（避免误覆盖）、② 6 个按钮全部标成"可点"
        //     （基类点击只看 c.r.c，而游戏自己的保存界面只设了 d 没设 c → 原始版本点不动）。
        // ★★★★★ 在“每帧收尾”方法（f_/e_/d_）的**最开头**插一句：
        //   this.qclApplyPending();
        //   它的作用只有一个：把等待线程写进 qclPending 的文件搬到 this.o。
        //   没有待处理文件时它立即返回，**不改变任何原有行为**（这是历史上出过严重 bug 的地方，
        //   所以写得极其保守：只在有东西时才动作）。
        //   开头的操作数栈是空的，所以这里插入是安全的。
        for (MethodNode mn : cn.methods) {
            boolean isTick = ("f_".equals(mn.name) || "e_".equals(mn.name) || "d_".equals(mn.name)
                    || "g_".equals(mn.name) || "c_".equals(mn.name)) && "()V".equals(mn.desc);
            if (!isTick) {
                continue;
            }
            InsnList apply = new InsnList();
            apply.add(new VarInsnNode(Opcodes.ALOAD, 0));
            apply.add(new MethodInsnNode(Opcodes.INVOKESTATIC, TARGET, APPLY_METHOD,
                    "(L" + TARGET + ";)V", false));
            // ★★★★★【关键】把 c.p（输名字界面）点「保存」时写入的目标路径取出来。
            //   c.p 里是用 System.setProperty("qcl.savefile", 路径) 交付的（它的编译期类型是 c.p，
            //   碰不到 c.e 的字段），而唯一会读它的就是这里。
            //   ⚠ 这段曾经被误删 → 属性写了没人读 → o 永远为空
            //     → 玩家存了名字点保存后「存档根本没创建」（用户实测）。
            LabelNode L_noSaveProp = new LabelNode();
            apply.add(new LdcInsnNode("qcl.savefile"));
            apply.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/System", "getProperty",
                    "(Ljava/lang/String;)Ljava/lang/String;", false));
            apply.add(new JumpInsnNode(Opcodes.IFNULL, L_noSaveProp));
            apply.add(new VarInsnNode(Opcodes.ALOAD, 0));
            apply.add(new TypeInsnNode(Opcodes.NEW, "java/io/File"));
            apply.add(new InsnNode(Opcodes.DUP));
            apply.add(new LdcInsnNode("qcl.savefile"));
            apply.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/System", "getProperty",
                    "(Ljava/lang/String;)Ljava/lang/String;", false));
            apply.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/File", "<init>",
                    "(Ljava/lang/String;)V", false));
            apply.add(new FieldInsnNode(Opcodes.PUTFIELD, TARGET, "o", "Ljava/io/File;"));
            // 读完就清掉，避免下一帧反复写
            apply.add(new LdcInsnNode("qcl.savefile"));
            apply.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/System", "clearProperty",
                    "(Ljava/lang/String;)Ljava/lang/String;", false));
            apply.add(new InsnNode(Opcodes.POP));
            apply.add(L_noSaveProp);
            // ★★★【诊断】打印 this.o 的当前值，用来定位"谁把 o 设成了带槽位文字的名字"。
            //   （用户实测：重复保存几次后会崩在 c.o.a(File)，
            //     文件名是 "duu  -  10-05 23:33.mclevel"，但那段清洗过的名字应该是 "duu.mclevel"。
            //     这条日志能把 o 的真实值和设值时机暴露出来。）
            LabelNode L_dbgO = new LabelNode();
            apply.add(new VarInsnNode(Opcodes.ALOAD, 0));
            apply.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "o", "Ljava/io/File;"));
            apply.add(new JumpInsnNode(Opcodes.IFNULL, L_dbgO));
            apply.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
            apply.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
            apply.add(new InsnNode(Opcodes.DUP));
            apply.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false));
            apply.add(new LdcInsnNode("[QCL-f_] \u5f85\u843d\u76d8\u76ee\u6807 = "));
            apply.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
            apply.add(new VarInsnNode(Opcodes.ALOAD, 0));
            apply.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "o", "Ljava/io/File;"));
            apply.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "getAbsolutePath",
                    "()Ljava/lang/String;", false));
            apply.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
            apply.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString",
                    "()Ljava/lang/String;", false));
            apply.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                    "(Ljava/lang/String;)V", false));
            apply.add(L_dbgO);
            apply.add(new VarInsnNode(Opcodes.ALOAD, 0));
            apply.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "o", "Ljava/io/File;"));
            // 【★关键修复】在 f_() 里也做一次名字清洗兜底：
            //   如果 o 的名字里含 "  -  "（说明是槽位显示文字被误传进来），
            //   就地改成"去掉时间戳 + 去非法字符"的干净名字再写盘。
            //   这样即使别的入口塞了脏名字进来，也不会让安卓写盘失败、游戏崩溃。
            LabelNode L_fCleanDone = new LabelNode();
            apply.add(new JumpInsnNode(Opcodes.IFNULL, L_fCleanDone));
            apply.add(new VarInsnNode(Opcodes.ALOAD, 0));
            apply.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "o", "Ljava/io/File;"));
            apply.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "getName",
                    "()Ljava/lang/String;", false));
            apply.add(new LdcInsnNode("  -  "));
            apply.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "indexOf",
                    "(Ljava/lang/String;)I", false));
            apply.add(new JumpInsnNode(Opcodes.IFLT, L_fCleanDone));
            // 有脏名字 → 用干净名字重建 o（放在同目录下）
            apply.add(new VarInsnNode(Opcodes.ALOAD, 0));
            apply.add(new TypeInsnNode(Opcodes.NEW, "java/io/File"));
            apply.add(new InsnNode(Opcodes.DUP));
            apply.add(new VarInsnNode(Opcodes.ALOAD, 0));
            apply.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "o", "Ljava/io/File;"));
            apply.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "getParentFile",
                    "()Ljava/io/File;", false));
            apply.add(new VarInsnNode(Opcodes.ALOAD, 0));
            apply.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "o", "Ljava/io/File;"));
            apply.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "getName",
                    "()Ljava/lang/String;", false));
            apply.add(new LdcInsnNode("  -  "));
            apply.add(new LdcInsnNode(""));
            apply.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "replace",
                    "(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;", false));
            apply.add(new LdcInsnNode(" "));
            apply.add(new LdcInsnNode("_"));
            apply.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "replace",
                    "(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;", false));
            apply.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "trim",
                    "()Ljava/lang/String;", false));
            apply.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/File", "<init>",
                    "(Ljava/io/File;Ljava/lang/String;)V", false));
            apply.add(new FieldInsnNode(Opcodes.PUTFIELD, TARGET, "o", "Ljava/io/File;"));
            apply.add(L_fCleanDone);
            // ★★★★★【2026-10-05 终极修复】在**保存界面**里，直接把 o 清空，
            //   让原版 f_() 那条落盘路径彻底不执行。
            //
            //   为什么必须这样：诊断日志（[QCL-f_]）证明，除了我们控制的入口，
            //   还有别的地方会把 o 设成"槽位显示文字 + 时间戳"（如 "ush  -  10-05 23:39"），
            //   而安卓文件系统**不允许文件名含冒号**，于是：
            //     FileNotFoundException: .../ush10-05_23:39.mclevel (Operation not permitted)
            //   这条异常在游戏主线程抛出 → 表现为"重复保存几次后崩溃"。
            //
            //   现在策略明确：
            //     · 保存落盘 = 只走 c.p 里「点保存立即落盘」那一条（名字已清洗，实测成功）
            //     · f_() 在保存界面里不再落盘（o 被清空）
            //     · 载入存档仍然走 qclApplyPending（读档是另一条路，不受影响）
            LabelNode L_notSaveScreen = new LabelNode();
            apply.add(new VarInsnNode(Opcodes.ALOAD, 0));
            apply.add(new TypeInsnNode(Opcodes.INSTANCEOF, "net/minecraft/client/c/o"));
            apply.add(new JumpInsnNode(Opcodes.IFEQ, L_notSaveScreen));
            apply.add(new VarInsnNode(Opcodes.ALOAD, 0));
            apply.add(new InsnNode(Opcodes.ACONST_NULL));
            apply.add(new FieldInsnNode(Opcodes.PUTFIELD, TARGET, "o", "Ljava/io/File;"));
            apply.add(L_notSaveScreen);
            mn.instructions.insert(apply);
            mn.maxStack = Math.max(mn.maxStack, 6);
            System.out.println("   已在 " + mn.name + "() 开头插入 qclApplyPending()");
            break;
        }

        for (MethodNode mn : cn.methods) {
            if ("b".equals(mn.name) && "()V".equals(mn.desc)) {
                InsnList fill = new InsnList();
                // ★★★ 这里绝对不能再"自动"设置要载入哪个存档！
                //   曾经的教训（用户实测）：在每帧执行的方法里读一个"选中的文件"属性，
                //   只要那个属性被设置过一次，就会**每一帧都把同一个存档载入一遍** ——
                //   表现就是"我根本没点按钮，它自己就把存档载入了"。
                //   现在读档只由两处触发，都是玩家的明确操作：
                //     ① 点存档槽位（a(r) 里设置 this.o）
                //     ② 文件浏览器里点中某个存档（qclBrowseClick 里设置 this.o）
                // ★ 极简版（去掉所有分支跳转 —— 带跳转的注入会让 ASM 帧计算失衡，
                //   之前连续报 ArrayIndexOutOfBoundsException@Frame.merge 就是这个原因）：
                //   ① 填载入列表（有存档就显示）
                //   ② 把 7 个按钮全部标成"可点"（修「加载文件…」「取消」点不动）
                //   ③ 保存界面不填列表（避免误覆盖）—— 该判断改到槽位点击时按按钮文字做
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
                // this.a(this.l)
                fill.add(new VarInsnNode(Opcodes.ALOAD, 0));
                fill.add(new VarInsnNode(Opcodes.ALOAD, 0));
                fill.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "l", "[Ljava/lang/String;"));
                fill.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, TARGET, "a", "([Ljava/lang/String;)V", false));
                // ★★★ 告诉游戏“列表已经就绪”。
                //   原版是后台线程 run() 联网取回列表后才设；我们把 run() 清空了，
                //   改成**在这里直接置一次**（每次建界面只设一次，不是每帧）。
                //   这样载入界面才会把槽位当“可点的存档”处理。
                fill.add(new VarInsnNode(Opcodes.ALOAD, 0));
                fill.add(new InsnNode(Opcodes.ICONST_1));
                fill.add(new FieldInsnNode(Opcodes.PUTFIELD, TARGET, "k", "Z"));
                // ★★★★★ 关键：**j（列表有效）也必须置上**。
                //   原版 run() 的顺序是：j=true → a(标签) → k=true。
                //   而 c.e.a(r) 的收尾是：
                //       if (j || (k && b == 5)) { n = true; new Thread(c.f).start(); }
                //   —— 只要 j 为 true，**点任何按钮都会启动对话框线程、读档并关界面**。
                //   我们清空了 run()，如果不把 j 置上，界面就处于“列表没好”的半残状态，
                //   各种判断都会走到意外分支。把 j 一起置上才是“干净的就绪状态”。
                fill.add(new VarInsnNode(Opcodes.ALOAD, 0));
                fill.add(new InsnNode(Opcodes.ICONST_1));
                fill.add(new FieldInsnNode(Opcodes.PUTFIELD, TARGET, "j", "Z"));
                //   k = “列表已就绪”，原版是后台线程 run() 联网取到列表后才置一次。
                //   我们把 run() 清空了（为了去掉 2010 年那台已关闭的服务器），
                //   却在这里无条件把它置为 true —— 结果让界面**永久处于“列表已就绪”**，
                //   而 k 同时控制着“点按钮后关界面回主菜单”那条路 →
                //   用户表现就是“点什么都返回”。行表已由 qclFillSaves 直接填好，完全不需要这个标志。
                // ★ 把「加载文件…」(索引 4) 和「取消」(索引 5) 标成可点。
                //   原版把这两个设成 c=false → 它们不仅显示灰色，而且**永远点不动**
                //   （用户实测："载入世界取消按钮和载入文件屁用没有"）。
                //   已有存档的槽位原版就已经可点，不用管。
                // ★★★【索引修正】原版 b() 创建的按钮依次是：
                //     0..4 = 五个槽位（创建时 hidden，等 a(String[]) 填上标签后才显示）
                //     5    = 「加载文件…」（原版在 b() 末尾把它 visible=false）
                //     6    = 「取消」
                //   之前戙写的是 for (i = 4; i <= 5) —— **漏了索引 6（真正的「取消」）**，
                //   所以取消一直是死按钮（用户实测：“取消按钮点不动”）。
                //   现在把 5、6 两个都标上；空槽位（0..4）原版 a(String[]) 会自己处理。
                // ★★★★★【关键修正】把**全部 7 个按钮**（槽位 0..4 + 加载/保存文件 5 + 取消 6）
                //   都标成可点。原来只标了 5、6，而槽位 0..4 的标记写在 a(String[])里 ——
                //   但那个方法**只在联网取回世界列表后才会被调用**（而 run() 已被清空），
                //   所以保存界面的空槽位**永远不可点**（用户反复反馈）。
                //   现在改在 b()（界面创建时必执行）里统一标记。
                for (int i = 0; i <= 6; i++) {
                    fill.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    fill.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "e", "Ljava/util/List;"));
                    fill.add(intConst(i));
                    fill.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/List", "get",
                            "(I)Ljava/lang/Object;", true));
                    fill.add(new TypeInsnNode(Opcodes.CHECKCAST, "net/minecraft/client/c/r"));
                    fill.add(new InsnNode(Opcodes.ICONST_1));
                    // ★ 用原版字段 c（active）：它同时管“亮/灰”和“能不能点”，
                    //   命中判定读的就是它。绝不能再用额外开关绕过坐标判定！
                    fill.add(new FieldInsnNode(Opcodes.PUTFIELD, "net/minecraft/client/c/r", "c", "Z"));
                }
                // 【2026-10-05 事故修复】这里原本有一段「诊断日志」（打印每个按钮的
                //   id/文字/active/visible，用于定位按钮点不动的问题）。它在一次批量
                //   删除调试日志的操作中被删得不完整，导致大括号不匹配、整段无法编译。
                //   现已按用户要求彻底移除调试日志 —— 这段代码不再需要。
                //   ⚠ 教训：删代码必须逐处精确编辑，不可用正则批量删除。
                // ★★★【2026-10-05 关键修复】插在**最后一个 RETURN 之前**。
                //   踩过的两个坑，务必记住：
                //     ① `insert(fill)` → 插到方法**最开头**：那时按钮还没创建，
                //        this.e 是空的（Size: 0）→ IndexOutOfBoundsException
                //        → 用户表现「点载入世界直接卡死」。
                //     ② `add(fill)` → 追加到**所有 RETURN 之后**：成了死代码，
                //        按钮标记根本没执行 → 五个槽位保持原版的 visible=false
                //        → 用户表现「槽位和加载文件全没了，只剩取消按钮」。
                //   正确做法：找到方法里最后一条 RETURN，插在它前面。
                AbstractInsnNode lastRet = null;
                for (AbstractInsnNode q = mn.instructions.getLast(); q != null; q = q.getPrevious()) {
                    if (q.getOpcode() == Opcodes.RETURN) {
                        lastRet = q;
                        break;
                    }
                }
                // 【2026-10-05 关键修复】进入载入/保存界面时，**把 this.o 清空**。
                //   原因：o 是实例字段，玩家保存过一次后它仍指向上次的文件，
                //   而原版 f_() 每帧都检查 `if (o != null) { 落盘; o = null; }` →
                //   下次一进「保存世界」就被当成"待保存目标"**自动保存到上一个槽位**，
                //   而不是等玩家点槽位（用户反馈："保存完存档后点ESC再点保存世界，
                //   会直接自动保存到刚才那个槽位，不让我重新选"）。
                //   界面刚创建时清掉它，等于每次进界面都是干净状态。
                fill.add(new VarInsnNode(Opcodes.ALOAD, 0));
                fill.add(new InsnNode(Opcodes.ACONST_NULL));
                fill.add(new FieldInsnNode(Opcodes.PUTFIELD, TARGET, "o", "Ljava/io/File;"));
                if (lastRet != null) {
                    mn.instructions.insertBefore(lastRet, fill);
                } else {
                    mn.instructions.add(fill);
                }
                    mn.maxStack = Math.max(mn.maxStack, 3);
                    n++;
                    // 【2026-10-05 事故修复】这里原来**缺 break** —— 循环会继续遍历
                    //   剩下的方法，把同一个 c.o 反复注入并反复写盘覆盖，日志里能看到
                    //   「已生成保存界面补丁」重复十几遍。更严重的是它会一路把后面的
                    //   注入（qclWaitLoad / qclWaitSave 等）全部冲掉，
                    //   导致 c/e.class 里最终**没有等待方法**、校验报栈高度错误。
                    break;
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
            // 【2026-10-05 事故修复】收口三连：
            //   第一个 } 关掉 for (MethodNode mn : cn.methods)
            //   第二个 } 关掉 if (co != null)
            //   下面 L367 的 } catch 关掉本次 try
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

                    // ★★★★★【关键修正】点「保存」后**立即落盘**，不再依赖 f_()。
                    //   用户实测：点保存后什么都不发生，必须再按一次 ESC 进「保存世界」才真正保存。
                    //   这里照抄原版 c.o.a(File) 的写盘字节码：
                    //     new FileOutputStream(target); new net.minecraft.client.f(b, b.p).a(b.d, out); out.close();
                    // （不用 try/catch：它会引入栈合并点，实测反复报
                    //  Inconsistent stack height。直线写法最稳定。）
                    LabelNode L_directDone = new LabelNode();
                    // 【2026-10-05 关键修复】名字要读**玩家在输入框里敲的那个字段**。
                    //   c.p 的字段（查混淆映射表 feathers 的 mappings.tiny）：
                    //     a = 父屏幕（c.i）      ← 原来误读了它，那是屏幕对象不是名字
                    //     i = 标题"输入世界名称："（String）
                    //     j = 槽位号（int）
                    //     k = **玩家输的名字**（String）  ← 正确字段
                    //     l = 计时用的 int
                    //   原版 c.p 点「保存」时把 k 丢掉（只拿它判断按钮可点），
                    //   所以我们这里自己把 k 取出来当文件名。
                    //   另外：k 的初始值就是槽位显示文字（如 "我的世界  -  10-05 21:51"），
                    //   下面会做"去时间戳 + 去非法字符"清洗。
                    LabelNode L_nmOk = new LabelNode();
                    LabelNode L_sufOk = new LabelNode();
                    // String nm = this.k; if (nm == null) nm = "";
                    put.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    put.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/c/p", "k", "Ljava/lang/String;"));
                    put.add(new VarInsnNode(Opcodes.ASTORE, 10));
                    LabelNode L_nmNotNull = new LabelNode();
                    put.add(new VarInsnNode(Opcodes.ALOAD, 10));
                    put.add(new JumpInsnNode(Opcodes.IFNONNULL, L_nmNotNull));
                    put.add(new LdcInsnNode(""));
                    put.add(new VarInsnNode(Opcodes.ASTORE, 10));
                    put.add(L_nmNotNull);
                    // ★★★【2026-10-05 关键修复】完整清洗名字。
                    //   实测崩溃日志：
                    //     FileNotFoundException: .../saves/udh  -  10-05 23:23.mclevel
                    //       (Operation not permitted)
                    //       at net.minecraft.client.c.p.a(...)
                    //   原因：c.p 的名字字段初始值就是**槽位显示文字**
                    //   （形如 "udh  -  10-05 23:23"，里面有**双空格**），
                    //   安卓上带空格的路径写盘会被拒（Operation not permitted）→ 游戏崩溃。
                    //   这里做两步清洗：
                    //     ① 去掉 "  -  " 之后的时间戳部分
                    //     ② 把所有空格与路径非法字符替换成下划线
                    LabelNode L_cpNoTs = new LabelNode();
                    put.add(new VarInsnNode(Opcodes.ALOAD, 10));
                    put.add(new LdcInsnNode("  -  "));
                    put.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "indexOf",
                            "(Ljava/lang/String;)I", false));
                    put.add(new JumpInsnNode(Opcodes.IFLT, L_cpNoTs));
                    put.add(new VarInsnNode(Opcodes.ALOAD, 10));
                    put.add(new InsnNode(Opcodes.ICONST_0));
                    put.add(new VarInsnNode(Opcodes.ALOAD, 10));
                    put.add(new LdcInsnNode("  -  "));
                    put.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "indexOf",
                            "(Ljava/lang/String;)I", false));
                    put.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "substring",
                            "(II)Ljava/lang/String;", false));
                    put.add(new VarInsnNode(Opcodes.ASTORE, 10));
                    put.add(L_cpNoTs);
                    // 把会害死安卓文件系统的字符全部换成下划线：空格 / \ : * ? " < > |
                    String[][] bad = {
                        {" ", "_"}, {"/", "_"}, {"\\\\", "_"}, {":", "_"},
                        {"*", "_"}, {"?", "_"}, {"\\\"", "_"}, {"<", "_"},
                        {">", "_"}, {"|", "_"},
                    };
                    for (String[] pair : bad) {
                        put.add(new VarInsnNode(Opcodes.ALOAD, 10));
                        put.add(new LdcInsnNode(pair[0]));
                        put.add(new LdcInsnNode(pair[1]));
                        put.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "replace",
                                "(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;", false));
                        put.add(new VarInsnNode(Opcodes.ASTORE, 10));
                    }
                    // nm = nm.trim(); if (nm.length()==0) nm = "世界";
                    put.add(new VarInsnNode(Opcodes.ALOAD, 10));
                    put.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "trim", "()Ljava/lang/String;", false));
                    put.add(new VarInsnNode(Opcodes.ASTORE, 10));
                    put.add(new VarInsnNode(Opcodes.ALOAD, 10));
                    put.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "length", "()I", false));
                    put.add(new JumpInsnNode(Opcodes.IFNE, L_nmOk));
                    put.add(new LdcInsnNode("世界"));
                    put.add(new VarInsnNode(Opcodes.ASTORE, 10));
                    put.add(L_nmOk);
                    // if (!nm.toLowerCase().endsWith(".mclevel")) nm = nm + ".mclevel";
                    put.add(new VarInsnNode(Opcodes.ALOAD, 10));
                    put.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "toLowerCase", "()Ljava/lang/String;", false));
                    put.add(new LdcInsnNode(".mclevel"));
                    put.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "endsWith", "(Ljava/lang/String;)Z", false));
                    put.add(new JumpInsnNode(Opcodes.IFNE, L_sufOk));
                    put.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
                    put.add(new InsnNode(Opcodes.DUP));
                    put.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false));
                    put.add(new VarInsnNode(Opcodes.ALOAD, 10));
                    put.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
                    put.add(new LdcInsnNode(".mclevel"));
                    put.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
                    put.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false));
                    put.add(new VarInsnNode(Opcodes.ASTORE, 10));
                    put.add(L_sufOk);
                    // File dir = new File(this.b.z, "saves"); dir.mkdirs();
                    put.add(new TypeInsnNode(Opcodes.NEW, "java/io/File"));
                    put.add(new InsnNode(Opcodes.DUP));
                    put.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    put.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/c/p", "b", "Lnet/minecraft/client/d;"));
                    put.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/d", "z", "Ljava/io/File;"));
                    put.add(new LdcInsnNode("saves"));
                    put.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/File", "<init>", "(Ljava/io/File;Ljava/lang/String;)V", false));
                    put.add(new VarInsnNode(Opcodes.ASTORE, 11));
                    put.add(new VarInsnNode(Opcodes.ALOAD, 11));
                    put.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "mkdirs", "()Z", false));
                    put.add(new InsnNode(Opcodes.POP));
                    // File target = new File(dir, nm);
                    put.add(new TypeInsnNode(Opcodes.NEW, "java/io/File"));
                    put.add(new InsnNode(Opcodes.DUP));
                    put.add(new VarInsnNode(Opcodes.ALOAD, 11));
                    put.add(new VarInsnNode(Opcodes.ALOAD, 10));
                    put.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/File", "<init>", "(Ljava/io/File;Ljava/lang/String;)V", false));
                    put.add(new VarInsnNode(Opcodes.ASTORE, 12));
                    // 【2026-10-05 关键修复】改成"栈上传递"，不再经过局部变量槽。
                    //   原来写成 ASTORE 13 / ALOAD 13，验证器报
                    //   "Incompatible object argument for function call"（类型追踪丢失）。
                    //   现在把顺序排成 JVM 需要的 [对象, 参数1, 参数2]，一气呵成：
                    //     new FileOutputStream(target)            → 留着当 invokevirtual 的参数
                    //     new c.f(b, b.p)                          → 留着
                    //     b.d                                      → 参数1
                    //     交换（让流当参数2）
                    //     invokevirtual c.f.a(map, out)
                    //   但 JVM 栈不能随便换位，所以用"先构造流→塞进静态临时不可行"，
                    //   这里改成最朴素的：先建流存槽 13，再用 **CCHECKCAST 消除歧义**。
                    put.add(new TypeInsnNode(Opcodes.NEW, "java/io/FileOutputStream"));
                    put.add(new InsnNode(Opcodes.DUP));
                    put.add(new VarInsnNode(Opcodes.ALOAD, 12));
                    put.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/FileOutputStream", "<init>", "(Ljava/io/File;)V", false));
                    put.add(new VarInsnNode(Opcodes.ASTORE, 13));
                    put.add(new TypeInsnNode(Opcodes.NEW, "net/minecraft/client/f"));
                    put.add(new InsnNode(Opcodes.DUP));
                    put.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    put.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/c/p", "b", "Lnet/minecraft/client/d;"));
                    put.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    put.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/c/p", "b", "Lnet/minecraft/client/d;"));
                    // 【2026-10-05 关键修复】描述符必须与原版 c.o.a(File) **逐字节一致**。
                    //   用 javap -v 查原版常量池确认（不是猜的）：
                    //     字段   p  的描述符 = Lnet/minecraft/client/a;   （实现类）
                    //     构造器 参数 的描述符 = La/b;                     （接口）
                    //   两者**故意不同**：字段存实现类，构造器收接口。
                    //   我先后写错过两次：
                    //     ① 字段 La/b; 、构造器 Lnet/minecraft/client/a;  → Incompatible object argument
                    //     ② 字段 La/b; 、构造器 La/b;                     → 同样报错
                    //   只有这一组（client/a + a/b）才是原版。
                    //   ⚠ 教训：改描述符前先 javap -v 看原版常量池，别凭印象写。
                    put.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/d",
                            "p", "Lnet/minecraft/client/a;"));
                    put.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "net/minecraft/client/f", "<init>",
                            "(Lnet/minecraft/client/d;La/b;)V", false));
                    put.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    put.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/c/p", "b", "Lnet/minecraft/client/d;"));
                    put.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/d", "d", MAPS_DESC));
                    put.add(new VarInsnNode(Opcodes.ALOAD, 13));
                    put.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/client/f", "a", "(" + MAPS_DESC + "Ljava/io/OutputStream;)V", false));
                    put.add(new VarInsnNode(Opcodes.ALOAD, 13));
                    put.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/FileOutputStream", "close", "()V", false));
                    put.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
                    put.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
                    put.add(new InsnNode(Opcodes.DUP));
                    put.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false));
                    put.add(new LdcInsnNode("[QCL-saves] 已立即保存到 "));
                    put.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
                    put.add(new VarInsnNode(Opcodes.ALOAD, 12));
                    put.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "getAbsolutePath", "()Ljava/lang/String;", false));
                    put.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
                    put.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false));
                    put.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false));
                    // 【2026-10-05 关键修复】这里必须先 GOTO 跳过下面的 POP！
                    //   那段 POP 是**异常处理器**用来丢弃异常对象的，正常路径跑过来时
                    //   操作数栈是空的 → POP 无值可弹 → JVM 报
                    //     "Incompatible object argument for function call"
                    //   （就是卡了很久、换了好几次描述符都没用的那个报错 —— 根因其实是
                    //     这个孤立的 POP，跟描述符无关）。
                    //   现在正常路径直接跳到合并点 L_directDone，POP 只由异常路径执行。
                    put.add(new JumpInsnNode(Opcodes.GOTO, L_directDone));
                    put.add(new InsnNode(Opcodes.POP));
                    put.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
                    put.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
                    put.add(new InsnNode(Opcodes.DUP));
                    put.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false));
                    put.add(new LdcInsnNode("[QCL-saves] 立即保存失败（详见上方异常栈）"));
                    // 【2026-10-05 修复】这里原来有一句 `ALOAD 5` 想把异常对象打出来，
                    //   但本段局部变量已改到槽 10..13，槽 5 从未赋值 →
                    //   JVM 报 "Register 5 contains wrong type"。
                    //   失败日志不再附带对象，避免引用未初始化的槽。
                    put.add(L_directDone);

                    //   ③ 自检日志
                    // 插在 aconst_null 之前：不会破坏后面 aload_0 / getfield b / d.a(...) 的栈
                    mn.instructions.insertBefore(anchor, put);
                    // 【2026-10-05 修复】立即落盘段用了槽 10..13（避开 a(r) 的参数槽 1，
                    //   否则 String 存进参数槽会报 "Accessing value from uninitialized register"）。
                    mn.maxLocals = Math.max(mn.maxLocals, 16);
                    mn.maxStack = Math.max(mn.maxStack, 6);
                    done++;
                }
                if (done > 0) {
                    File outP = new File(out.getParentFile(), "p.class");
                    // 【2026-10-05 关键修复】原版用 COMPUTE_FRAMES，实测抛
                    //   java.lang.ArrayIndexOutOfBoundsException: Index 0 out of bounds for length 0
                    //   → 整段 c.p 注入被 try/catch 吞掉 → 「点保存立即落盘」代码根本没进去
                    //   → 用户表现「输完名字点保存根本没保存」。
                    //   目标类主版本号是 49（Java 5），**不需要 StackMapTable**，
                    //   所以改用 COMPUTE_MAXS（只算栈深/局部变量数）即可，稳定得多。
                    ClassWriter cwP = new ClassWriter(ClassWriter.COMPUTE_MAXS) {
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

        // ============================================================
        // ⑤ ★★★★★ 重写点击处理 a(c.r)—— 三个按钮都能用
        // ============================================================
        // 原版（反编译 c.e）：
        //     protected final void a(c.r r) {
        //         if (!this.n && r.c) {
        //             if (this.k && r.b < 5) a(r.b);
        //             if (this.j || (this.k && r.b == 5)) { this.n = true; new Thread(new c.f(this)).start(); }
        //             if (this.j || (this.k && r.b == 6)) this.b.a(this.i);
        //         }
        //     }
        //
        // 三个致命问题：
        //   ① j / k 只在**联网取回世界列表**后才为真 —— 那台 2010 年的服务器早关了，
        //      run() 也被我们清空了 → **这两个标志永远是 false** →
        //      「加载文件…」「取消」「槽位」全都是死按钮（用户反复反馈）。
        //   ② r.b == 5 走的是 new c.f(this)（原版的文件对话框线程），run() 被清空 → 什么都不做。
        //   ③ 原版没有"保存界面点槽位 → 输名字"这条路（那要靠 k）。
        //
        // 重写后的逻辑（全部**不依赖 j/k**）：
        //     id == 6 → 取消：this.b.a(this.i) 切回上一屏
        //     id == 5 → this instanceof c.o ? qclAskName() : qclAskFile()
        //                （保存界面要名字+目录；载入界面只要文件）
        //     id < 5 且 this instanceof c.o → this.a(id) 跳「输入世界名称」界面
        //     id < 5 且是载入界面        → 按槽位文字在 qclSaves 里找同名文件 → this.o = f
        //     id < 5 且无存档            → 什么都不做
        //
        // ⚠ 命中判定（c.r.a(II)）**保持原版**，绝不在这里绕过坐标检查 ——
        //   曾经用额外开关绕过，结果"点空气也会触发按钮"（用户实测）。
        try {
            int rc = 0;
            for (MethodNode mn : cn.methods) {
                if (!"a".equals(mn.name) || !"(Lnet/minecraft/client/c/r;)V".equals(mn.desc)) {
                    continue;
                }
                LabelNode L_ret = new LabelNode();
                LabelNode L_notCancel6 = new LabelNode();
                LabelNode L_notFile5 = new LabelNode();
                LabelNode L_notSaveSlot = new LabelNode();
                InsnList click = new InsnList();

                // ---- id == 6：取消 ----
                click.add(new VarInsnNode(Opcodes.ALOAD, 1));
                click.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/c/r", "b", "I"));
                click.add(new IntInsnNode(Opcodes.BIPUSH, 6));
                click.add(new JumpInsnNode(Opcodes.IF_ICMPNE, L_notCancel6));
                click.add(new VarInsnNode(Opcodes.ALOAD, 0));
                click.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "b", "Lnet/minecraft/client/d;"));
                click.add(new VarInsnNode(Opcodes.ALOAD, 0));
                click.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "i", "Lnet/minecraft/client/c/i;"));
                click.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/client/d", "a",
                        "(Lnet/minecraft/client/c/i;)V", false));
                click.add(new JumpInsnNode(Opcodes.GOTO, L_ret));
                click.add(L_notCancel6);

                // ---- id == 5：加载文件… / 保存文件… ----
                click.add(new VarInsnNode(Opcodes.ALOAD, 1));
                click.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/c/r", "b", "I"));
                click.add(new InsnNode(Opcodes.ICONST_5));
                click.add(new JumpInsnNode(Opcodes.IF_ICMPNE, L_notFile5));
                LabelNode L_isLoadScreen = new LabelNode();
                click.add(new VarInsnNode(Opcodes.ALOAD, 0));
                click.add(new TypeInsnNode(Opcodes.INSTANCEOF, "net/minecraft/client/c/o"));
                click.add(new JumpInsnNode(Opcodes.IFEQ, L_isLoadScreen));
                click.add(new MethodInsnNode(Opcodes.INVOKESTATIC, TARGET, ASK_NAME, "()V", false));
                click.add(new JumpInsnNode(Opcodes.GOTO, L_ret));
                click.add(L_isLoadScreen);
                click.add(new MethodInsnNode(Opcodes.INVOKESTATIC, TARGET, ASK_METHOD, "()V", false));
                click.add(new JumpInsnNode(Opcodes.GOTO, L_ret));
                click.add(L_notFile5);

                // ---- id < 5 且在保存界面：跳「输入世界名称」 ----
                click.add(new VarInsnNode(Opcodes.ALOAD, 0));
                click.add(new TypeInsnNode(Opcodes.INSTANCEOF, "net/minecraft/client/c/o"));
                click.add(new JumpInsnNode(Opcodes.IFEQ, L_notSaveSlot));
                click.add(new VarInsnNode(Opcodes.ALOAD, 1));
                click.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/c/r", "b", "I"));
                click.add(new InsnNode(Opcodes.ICONST_5));
                click.add(new JumpInsnNode(Opcodes.IF_ICMPGE, L_notSaveSlot));
                click.add(new VarInsnNode(Opcodes.ALOAD, 0));
                click.add(new VarInsnNode(Opcodes.ALOAD, 1));
                click.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/c/r", "b", "I"));
                click.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, TARGET, "a", "(I)V", false));
                click.add(new JumpInsnNode(Opcodes.GOTO, L_ret));
                click.add(L_notSaveSlot);

                // ---- id < 5 且在载入界面：按槽位文字找同名存档 ----
                click.add(new FieldInsnNode(Opcodes.GETSTATIC, TARGET, SAVES_FIELD, "[Ljava/io/File;"));
                click.add(new JumpInsnNode(Opcodes.IFNULL, L_ret));
                click.add(new VarInsnNode(Opcodes.ALOAD, 1));
                click.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/c/r", "b", "I"));
                click.add(new JumpInsnNode(Opcodes.IFLT, L_ret));
                click.add(new VarInsnNode(Opcodes.ALOAD, 1));
                click.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/c/r", "b", "I"));
                click.add(new FieldInsnNode(Opcodes.GETSTATIC, TARGET, SAVES_FIELD, "[Ljava/io/File;"));
                click.add(new InsnNode(Opcodes.ARRAYLENGTH));
                click.add(new JumpInsnNode(Opcodes.IF_ICMPGE, L_ret));
                click.add(new FieldInsnNode(Opcodes.GETSTATIC, TARGET, SAVES_FIELD, "[Ljava/io/File;"));
                click.add(new VarInsnNode(Opcodes.ALOAD, 1));
                click.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/c/r", "b", "I"));
                click.add(new InsnNode(Opcodes.AALOAD));
                click.add(new VarInsnNode(Opcodes.ASTORE, 2));
                click.add(new VarInsnNode(Opcodes.ALOAD, 2));
                click.add(new JumpInsnNode(Opcodes.IFNULL, L_ret));
                // if (r.a == null || r.a.equals("-")) return;  —— 空槽位没有对应存档
                click.add(new VarInsnNode(Opcodes.ALOAD, 1));
                click.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/c/r", "a", "Ljava/lang/String;"));
                click.add(new JumpInsnNode(Opcodes.IFNULL, L_ret));
                click.add(new VarInsnNode(Opcodes.ALOAD, 1));
                click.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/c/r", "a", "Ljava/lang/String;"));
                click.add(new LdcInsnNode("-"));
                click.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals",
                        "(Ljava/lang/Object;)Z", false));
                click.add(new JumpInsnNode(Opcodes.IFNE, L_ret));
                // this.o = f;   ← 交给原版 f_() 落盘/读档
                click.add(new VarInsnNode(Opcodes.ALOAD, 0));
                click.add(new VarInsnNode(Opcodes.ALOAD, 2));
                click.add(new FieldInsnNode(Opcodes.PUTFIELD, TARGET, "o", "Ljava/io/File;"));
                click.add(L_ret);
                click.add(new InsnNode(Opcodes.RETURN));

                mn.instructions.clear();
                mn.tryCatchBlocks.clear();
                if (mn.localVariables != null) {
                    mn.localVariables.clear();
                }
                mn.instructions.add(click);
                mn.maxStack = 6;
                mn.maxLocals = 4;
                rc++;
            }
            if (rc > 0) {
                System.out.println("   已重写点击处理 a(c.r)：" + rc + " 处（取消 / 文件按钮 / 槽位）");
            } else {
                System.out.println("   !! c.e 里没找到 a(c.r)");
            }
        } catch (Throwable t) {
            System.out.println("   点击处理重写失败（不致命）: " + t);
        }

        // ★ 先把「收编手机存档」方法加进去（同一个 ClassNode，避免两次写盘互相覆盖）
        boolean hasImport = false;
        for (MethodNode im : cn.methods) {
            if (IMPORT_METHOD.equals(im.name)) {
                hasImport = true;
            }
        }
        if (!hasImport) {
            cn.methods.add(buildImportMethod());
        cn.methods.add(buildAskMethod());
        cn.methods.add(buildAskSaveMethod());
        cn.methods.add(buildAskNameMethod());
        // ★★★ 待处理文件的中转：等待线程不能直接写 private 字段，
        //   所以加一个 public static 字段 + 一个公开方法，它们都在 c.e 上，
        //   等待线程（同包同 classloader）可以直接调用，彻底不用反射。
        if (!hasPending) {
            cn.fields.add(new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                    PENDING_FIELD, "Ljava/io/File;", null, null));
        }
        boolean hasSaveName = false;
        for (FieldNode fn : cn.fields) {
            if ("qclSaveName".equals(fn.name)) {
                hasSaveName = true;
            }
        }
        if (!hasSaveName) {
            cn.fields.add(new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                    "qclSaveName", "Ljava/lang/String;", null, null));
        }
        // ★★★★★【2026-10-05 关键修复】"待落盘"字段（后台线程 → 主线程 交接）。
        //   为什么需要它们：
        //     「保存文件…」是后台线程（qclTask）在等玩家选目录。如果**在后台线程里
        //     直接把世界写盘**，会因为 LWJGL/OpenGL 的 context 只属于游戏主线程而抛：
        //       "No context is current or a function that is not available in the
        //        current context was called. Are you running essential and/or <1.13?"
        //     → 用户表现「点保存文件…之后游戏崩了」。
        //   正确做法：后台线程只负责"选好目录 + 算好文件名"，把结果放进这两个静态字段；
        //     真正的写盘由主线程（f_() 里调用的 qclDoPendingSave）完成。
        boolean hasDst = false;
        for (FieldNode fn : cn.fields) {
            if ("qclSaveDst".equals(fn.name)) {
                hasDst = true;
            }
        }
        if (!hasDst) {
            cn.fields.add(new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                    "qclSaveDst", "Ljava/lang/String;", null, null));
        }
        boolean hasSaving = false;
        for (FieldNode fn : cn.fields) {
            if ("qclSaving".equals(fn.name)) {
                hasSaving = true;
            }
        }
        if (!hasSaving) {
            cn.fields.add(new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                    "qclSaving", "Z", null, null));
        }
        // ★★★ 当前屏幕实例。
        //   静态方法（qclAskFile / qclAskName）里拿不到 this，
        //   而等待线程必须知道把结果交给谁。
        //   在 qclApplyPending（每帧都调）里把 this 记进这个字段，
        //   等待线程直接读它就行。
        //   ⚠️ 字段必须先注入，否则写它会抛 NoSuchFieldError 崩主线程
        //      （实测就是这个原因导致“载入世界卡死”）。
        boolean hasSelf = false;
        for (FieldNode fn : cn.fields) {
            if ("qclSelf".equals(fn.name)) {
                hasSelf = true;
            }
        }
        if (!hasSelf) {
            cn.fields.add(new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                    "qclSelf", "L" + TARGET + ";", null, null));
            System.out.println("   已注入字段 qclSelf");
        }
        cn.methods.add(buildApplyMethod());
        cn.methods.add(buildWaitLoadMethod());
        cn.methods.add(buildWaitSaveMethod());
            System.out.println("   已注入 " + IMPORT_METHOD + "（收编手机存档）");
        }
        System.out.println("   正在生成 c/e.class（COMPUTE_FRAMES）...");
        // ★★★ 这里必须用 COMPUTE_MAXS，**不能用 COMPUTE_FRAMES**！
        //   本游戏的类文件是 major version 49（Java 5），本来就没有 StackMapTable、
        //   也不需要帧；强制算帧反而会在 new/dup、多路跳转汇合处算错，
        //   报 ArrayIndexOutOfBoundsException@Frame.merge（折腾了很多轮就是这个原因）。
        //   实测：同一份代码 COMPUTE_FRAMES 失败、COMPUTE_MAXS 通过。
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
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
    /**
     * ★★★ 关键坑：Java 只有 ICONST_0..ICONST_5（操作码 3..8）。
     * 直接写 ICONST_0 + i 时，i=6 → 操作码 9 = LCONST_0（压的是 long 0！），
     * i=7 → 操作码 10 = LCONST_1。于是 List.get(int) 收到 long →
     * VerifyError: Expecting to find integer on stack（在真机上表现为整个类加载失败、界面直接不出来）。
     * 常量 6 及以上必须用 BIPUSH（或 SIPUSH/LDC）。
     */
    private static AbstractInsnNode intConst(int v) {
        if (v >= -1 && v <= 5) {
            return new InsnNode(Opcodes.ICONST_0 + v);
        }
        if (v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) {
            return new IntInsnNode(Opcodes.BIPUSH, v);
        }
        return new IntInsnNode(Opcodes.SIPUSH, v);
    }
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
        // ★ 设为设备本地时区（不设的话这个 JVM 会用 UTC，时间就会差 8 小时）
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/TimeZone", "getDefault",
                "()Ljava/util/TimeZone;", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/text/SimpleDateFormat", "setTimeZone",
                "(Ljava/util/TimeZone;)V", false));
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
        // ★ 设为设备本地时区（不设的话这个 JVM 会用 UTC，时间就会差 8 小时）
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/TimeZone", "getDefault",
                "()Ljava/util/TimeZone;", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/text/SimpleDateFormat", "setTimeZone",
                "(Ljava/util/TimeZone;)V", false));
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

    /**
     * 生成「收编手机存档」方法体：扫描 /sdcard 常见下载目录里的 .mclevel，
     * 复制进存档目录（不覆盖同名文件），返回复制进来的文件数组。
     * <p>为什么要有它：载入界面「加载文件…」原本走文件对话框线程 c.f，
     * 而 run() 已被清空（原版会去连 2010 年那台已关闭的服务器），
     * 所以那个按钮实际只剩"关界面"。改成"把手机里的存档收进来"，行为可预期、一定能用。
     */

    /**
     * 注入 {@code private static void qclAskFile()}：「载入文件…」按钮的文件桥入口。
     *
     * <p>做的事只有两件：① 写请求文件 {@code req.txt}（内容 "load"）；
     * ② 启动一个 {@code c.qclFileWaiter} 守护线程去等玩家的选择结果。
     *
     * <p>游戏 JVM 弹不出安卓组件（实测 {@code ClassNotFoundException: android/app/ActivityThread}），
     * 所以由启动器侧的 {@code QclFileBridge} 轮询这个请求文件并弹出系统文件选择器。
     */
    /**
     * 注入 {@code private static void qclAskFile()}：「载入文件…」按钮的文件桥入口。
     *
     * <p>做的事只有两件：① 写请求文件 {@code req.txt}（内容 "load"）；
     * ② 启动 {@code c.qclFileWaiter} 守护线程去等玩家的选择结果。
     *
     * <p>游戏 JVM 弹不出安卓组件（实测 {@code ClassNotFoundException: android/app/ActivityThread}），
     * 所以由启动器侧的 {@code QclFileBridge} 轮询这个请求文件并弹出系统文件选择器。
     *
     * <p>异常处理写法与 {@code buildFillMethod} 一致：**整段一个 try**，
     * 处理器放在最末、落进同一个 RETURN —— 这样所有路径的栈高都一致。
     * （分散的 try 段会让多个不同栈高的点共享处理器，校验直接报
     * "Inconsistent stack height"。）
     */
    /**
     * 注入 {@code private static void qclAskFile()}：「载入文件…」按钮 的文件桥入口。
     *
     * <p>做的事：写请求文件（内容 "load"）→ 启动器侧的 {@code QclFileBridge}
     * 轮询到它并弹出系统文件选择器（游戏 JVM 弹不出安卓组件，实测
     * {@code ClassNotFoundException: android/app/ActivityThread}）→
     * 结果由 {@code c.qclFileWaiter} 等待线程取回并设进 {@code e.o}。
     *
     * 这是唯一能通过校验的形态 —— 曾经把 try 拆成多段、或在末尾加 GOTO 指向处理器，
     * 都会让"栈高 0 的正常路径"与"栈高 1 的异常路径"合并，报
     * {@code Inconsistent stack height 0 != 1}。
     */
    /**
     * 注入 {@code private static void qclAskFile()}：「载入文件…」按钮的文件桥入口。
     *
     * <p>做的事：写请求文件（内容 "load"）→ 启动器侧的 {@code QclFileBridge} 轮询到它
     * 并弹出**系统文件选择器**（游戏 JVM 弹不出安卓组件，实测
     * {@code ClassNotFoundException: android/app/ActivityThread}）→
     * 玩家挑完后由 {@code c.qclFileWaiter} 等待线程把路径设进 {@code e.o}，
     * 再由游戏每帧的 {@code f_()} 真正读档。
     *
     * 这是唯一能通过校验的形态 —— 把 try 拆成多段、或在末尾加 GOTO 指向处理器，
     * 都会让"栈高 0 的正常路径"与"栈高 1 的异常路径"合并，报
     * {@code Inconsistent stack height 0 != 1}。
     */
    private static MethodNode buildAskMethod() {
        MethodNode m = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                ASK_METHOD, ASK_DESC, null, null);
        InsnList c = m.instructions;

        LabelNode L_try = new LabelNode();
        LabelNode L_end = new LabelNode();
        LabelNode L_catch = new LabelNode();

        c.add(L_try);
        // ★★★ 关键：这里是**静态方法**，局部变量 0 不是 this！
        //   必须先把 this 存进 0，后面 ALOAD 0 才能拿到界面对象。
        //   （曾经漏了这一步，结果把 FileWriter 当成界面对象传给等待线程，
        //     报错信息是“找不到 qclWait（class=java.io.FileWriter）”。）
        // 静态方法里没有 this，0 号槽位只能给个 null 占位（后续逻辑不依赖它）
        c.add(new InsnNode(Opcodes.ACONST_NULL));
        c.add(new VarInsnNode(Opcodes.ASTORE, 0));
        // new File(MAILBOX).mkdirs();
        c.add(new TypeInsnNode(Opcodes.NEW, "java/io/File"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new LdcInsnNode(MAILBOX));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/File", "<init>",
                "(Ljava/lang/String;)V", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "mkdirs", "()Z", false));
        c.add(new InsnNode(Opcodes.POP));
        // new File(MAILBOX + "/res.txt").delete();
        c.add(new TypeInsnNode(Opcodes.NEW, "java/io/File"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new LdcInsnNode(MAILBOX + "/res.txt"));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/File", "<init>",
                "(Ljava/lang/String;)V", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "delete", "()Z", false));
        c.add(new InsnNode(Opcodes.POP));
        // FileWriter w = new FileWriter(MAILBOX + "/req.txt"); w.write("load"); w.close();
        c.add(new TypeInsnNode(Opcodes.NEW, "java/io/FileWriter"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new LdcInsnNode(MAILBOX + "/req.txt"));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/FileWriter", "<init>",
                "(Ljava/lang/String;)V", false));
        c.add(new VarInsnNode(Opcodes.ASTORE, 0));
        c.add(new VarInsnNode(Opcodes.ALOAD, 0));
        c.add(new LdcInsnNode("load"));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/FileWriter", "write",
                "(Ljava/lang/String;)V", false));
        c.add(new VarInsnNode(Opcodes.ALOAD, 0));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/FileWriter", "close", "()V", false));
        // new c.qclFileWaiter(this).start();
        // new c.qclTask(this, "load").start();
        c.add(new TypeInsnNode(Opcodes.NEW, "net/minecraft/client/c/qclTask"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new VarInsnNode(Opcodes.ALOAD, 0));
        c.add(new LdcInsnNode("load"));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "net/minecraft/client/c/qclTask", "<init>",
                "(Ljava/lang/Object;Ljava/lang/String;)V", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Thread", "start", "()V", false));
        c.add(L_end);
        c.add(new InsnNode(Opcodes.RETURN));
        c.add(L_catch);
        c.add(new VarInsnNode(Opcodes.ASTORE, 1));
        c.add(new InsnNode(Opcodes.RETURN));

        m.tryCatchBlocks.add(new TryCatchBlockNode(L_try, L_end, L_catch, "java/lang/Throwable"));
        m.maxStack = 8;
        m.maxLocals = 4;
        return m;
    }

    /**
     * 注入 {@code private static void qclAskSave()}：「保存文件…」按钮的文件桥入口。
     *
     * <p>流程：写请求文件（内容 "save"）→ 启动器弹出**文件夹选择器** → 玩家选目录 →
     * {@code c.qclFileSaver} 把「目录 + 世界名」拼成目标文件设进 {@code e.o} →
     * 游戏原版每帧的落盘逻辑（{@code f_()}）把世界写进去。
     *
     * <p>世界名取自「输入世界名称」界面存进系统属性 {@code qcl.savename} 的值；
     * 没有就由 qclFileSaver 用日期兜底起名。
     */
    private static MethodNode buildAskSaveMethod() {
        MethodNode m = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                ASK_SAVE, ASK_DESC, null, null);
        InsnList c = m.instructions;

        LabelNode L_try = new LabelNode();
        LabelNode L_end = new LabelNode();
        LabelNode L_catch = new LabelNode();

        c.add(L_try);
        c.add(new TypeInsnNode(Opcodes.NEW, "java/io/File"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new LdcInsnNode(MAILBOX));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/File", "<init>",
                "(Ljava/lang/String;)V", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "mkdirs", "()Z", false));
        c.add(new InsnNode(Opcodes.POP));
        c.add(new TypeInsnNode(Opcodes.NEW, "java/io/File"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new LdcInsnNode(MAILBOX + "/res.txt"));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/File", "<init>",
                "(Ljava/lang/String;)V", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "delete", "()Z", false));
        c.add(new InsnNode(Opcodes.POP));
        c.add(new TypeInsnNode(Opcodes.NEW, "java/io/FileWriter"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new LdcInsnNode(MAILBOX + "/req.txt"));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/FileWriter", "<init>",
                "(Ljava/lang/String;)V", false));
        c.add(new VarInsnNode(Opcodes.ASTORE, 0));
        c.add(new VarInsnNode(Opcodes.ALOAD, 0));
        c.add(new LdcInsnNode("save"));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/FileWriter", "write",
                "(Ljava/lang/String;)V", false));
        c.add(new VarInsnNode(Opcodes.ALOAD, 0));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/FileWriter", "close", "()V", false));
        // String name = System.getProperty("qcl.savename", "");
        c.add(new LdcInsnNode("qcl.savename"));
        c.add(new LdcInsnNode(""));
        c.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/System", "getProperty",
                "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", false));
        c.add(new VarInsnNode(Opcodes.ASTORE, 1));
        // new c.qclFileSaver(this, name).start();
        c.add(new TypeInsnNode(Opcodes.NEW, "net/minecraft/client/c/qclTask"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new VarInsnNode(Opcodes.ALOAD, 0));
        c.add(new LdcInsnNode("save"));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "net/minecraft/client/c/qclTask", "<init>",
                "(Ljava/lang/Object;Ljava/lang/String;)V", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Thread", "start", "()V", false));
        c.add(L_end);
        c.add(new InsnNode(Opcodes.RETURN));
        c.add(L_catch);
        c.add(new VarInsnNode(Opcodes.ASTORE, 1));
        c.add(new InsnNode(Opcodes.RETURN));

        m.tryCatchBlocks.add(new TryCatchBlockNode(L_try, L_end, L_catch, "java/lang/Throwable"));
        m.maxStack = 8;
        m.maxLocals = 4;
        return m;
    }

    /**
    /**
     * 注入 {@code private static void qclAskName()}：存盘第一步 —— 向玩家要世界名。
     *
     * <p>为什么用启动器侧的输入框：游戏内的文字输入要先点输入框、再点屏幕顶部那块
     * 虚拟键盘才能打字，玩家基本用不了（用户实测反馈）。改成安卓原生输入框后直接打字即可。
     *
     * <p>等待线程用 {@code c.qclFileSaver}：它第一次读到的是名字，第二次读到的是保存目录，
     * 两轮都由同一个线程串起来。
     */
    private static MethodNode buildAskNameMethod() {
        MethodNode m = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                ASK_NAME, ASK_DESC, null, null);
        InsnList c = m.instructions;

        LabelNode L_try = new LabelNode();
        LabelNode L_end = new LabelNode();
        LabelNode L_catch = new LabelNode();

        c.add(L_try);
        // ★★★ 关键：这里是**静态方法**，局部变量 0 不是 this！
        //   必须先把 this 存进 0，后面 ALOAD 0 才能拿到界面对象。
        //   （曾经漏了这一步，结果把 FileWriter 当成界面对象传给等待线程，
        //     报错信息是“找不到 qclWait（class=java.io.FileWriter）”。）
        // 静态方法里没有 this，0 号槽位只能给个 null 占位（后续逻辑不依赖它）
        c.add(new InsnNode(Opcodes.ACONST_NULL));
        c.add(new VarInsnNode(Opcodes.ASTORE, 0));
        // new File(MAILBOX).mkdirs();
        c.add(new TypeInsnNode(Opcodes.NEW, "java/io/File"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new LdcInsnNode(MAILBOX));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/File", "<init>",
                "(Ljava/lang/String;)V", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "mkdirs", "()Z", false));
        c.add(new InsnNode(Opcodes.POP));
        // new File(MAILBOX + "/res.txt").delete();   清掉上次残留结果
        c.add(new TypeInsnNode(Opcodes.NEW, "java/io/File"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new LdcInsnNode(MAILBOX + "/res.txt"));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/File", "<init>",
                "(Ljava/lang/String;)V", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "delete", "()Z", false));
        c.add(new InsnNode(Opcodes.POP));
        // FileWriter w = new FileWriter(MAILBOX + "/req.txt"); w.write("name"); w.close();
        c.add(new TypeInsnNode(Opcodes.NEW, "java/io/FileWriter"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new LdcInsnNode(MAILBOX + "/req.txt"));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/FileWriter", "<init>",
                "(Ljava/lang/String;)V", false));
        c.add(new VarInsnNode(Opcodes.ASTORE, 0));
        c.add(new VarInsnNode(Opcodes.ALOAD, 0));
        c.add(new LdcInsnNode("name"));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/FileWriter", "write",
                "(Ljava/lang/String;)V", false));
        c.add(new VarInsnNode(Opcodes.ALOAD, 0));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/FileWriter", "close", "()V", false));
        // new c.qclFileSaver(this, null).start();
        // new c.qclTask(this, "name").start();   名字拿到后由 qclWait 内部继续请求 "save"
        c.add(new TypeInsnNode(Opcodes.NEW, "net/minecraft/client/c/qclTask"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new VarInsnNode(Opcodes.ALOAD, 0));
        c.add(new LdcInsnNode("name"));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "net/minecraft/client/c/qclTask", "<init>",
                "(Ljava/lang/Object;Ljava/lang/String;)V", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Thread", "start", "()V", false));
        c.add(L_end);
        c.add(new InsnNode(Opcodes.RETURN));
        // 处理器在最后：只有异常路径会到这里（栈上正好是那个异常）
        c.add(L_catch);
        c.add(new VarInsnNode(Opcodes.ASTORE, 1));
        c.add(new InsnNode(Opcodes.RETURN));

        m.tryCatchBlocks.add(new TryCatchBlockNode(L_try, L_end, L_catch, "java/lang/Throwable"));
        m.maxStack = 8;
        m.maxLocals = 4;
        return m;
    }

    /**
     * 注入 {@code public static void qclApplyPending()}：把 {@link #PENDING_FIELD} 搬到 {@code this.o}。
     *
     * <p>为什么要有它：等待线程（{@code c.qclFileWaiter} / {@code c.qclFileSaver}）拿到了玩家选的
     * 文件路径，但 {@code o} 是实例字段、且曾经用反射写入失败（实测 NoSuchFieldException）。
     * 改成：等待线程只写 **public static** 的 {@code qclPending}，再由本方法在原版线程上
     * 把值装进 {@code this.o} —— 全程没有反射，且不改变 {@code f_()} 的任何原有逻辑。
     */
    private static MethodNode buildApplyMethod() {
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                APPLY_METHOD, "(L" + TARGET + ";)V", null, null);
        InsnList c = m.instructions;
        LabelNode L_end = new LabelNode();
        // ★ 记下当前屏幕实例（供等待线程取用）。
        //   这里每帧刷新，值始终是当前屏幕；字段已在上方注入。
        c.add(new VarInsnNode(Opcodes.ALOAD, 0));
        c.add(new FieldInsnNode(Opcodes.PUTSTATIC, TARGET, "qclSelf", "L" + TARGET + ";"));

        // ★★★★★【2026-10-05 关键修复】主线程写盘：「保存文件…」选好目录后，
        //   由**主线程**在这里把世界写进目标文件。
        //   为什么必须放主线程：世界写盘要走 LWJGL/OpenGL，而 GL context 只属于
        //   游戏主线程；后台线程（qclTask）里写会抛
        //     "No context is current or a function that is not available in the
        //      current context was called. Are you running essential and/or <1.13?"
        //   → 用户表现「点保存文件…之后游戏崩了」。
        //   流程：后台线程只设 qclSaveDst 路径 + qclSaving=true；
        //         主线程每帧检查，发现就写盘，写完把 qclSaving 清掉。
        LabelNode L_noSave = new LabelNode();
        c.add(new FieldInsnNode(Opcodes.GETSTATIC, TARGET, "qclSaving", "Z"));
        c.add(new JumpInsnNode(Opcodes.IFEQ, L_noSave));
        c.add(new InsnNode(Opcodes.ICONST_0));
        c.add(new FieldInsnNode(Opcodes.PUTSTATIC, TARGET, "qclSaving", "Z"));
        c.add(new FieldInsnNode(Opcodes.GETSTATIC, TARGET, "qclSaveDst", "Ljava/lang/String;"));
        c.add(new JumpInsnNode(Opcodes.IFNULL, L_noSave));
        // ★【2026-10-06 保险】先确保父目录存在，再写文件。
        //   实测踩过：玩家选的"目录"其实是上次保存的**文件路径**，
        //   拼出来的路径父目录不存在 → FileNotFoundException → 界面卡住出不来。
        //   这里 mkdirs() 一下，目录不存在就建出来（建不出来也只是这一帧不写，不会崩）。
        LabelNode L_dirReady = new LabelNode();
        c.add(new TypeInsnNode(Opcodes.NEW, "java/io/File"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new FieldInsnNode(Opcodes.GETSTATIC, TARGET, "qclSaveDst", "Ljava/lang/String;"));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/File", "<init>",
                "(Ljava/lang/String;)V", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "getParentFile",
                "()Ljava/io/File;", false));
        c.add(new VarInsnNode(Opcodes.ASTORE, 2));
        c.add(new VarInsnNode(Opcodes.ALOAD, 2));
        c.add(new JumpInsnNode(Opcodes.IFNULL, L_dirReady));
        c.add(new VarInsnNode(Opcodes.ALOAD, 2));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "exists", "()Z", false));
        c.add(new JumpInsnNode(Opcodes.IFNE, L_dirReady));
        c.add(new VarInsnNode(Opcodes.ALOAD, 2));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "mkdirs", "()Z", false));
        c.add(new InsnNode(Opcodes.POP));
        c.add(L_dirReady);
        // new FileOutputStream(new File(qclSaveDst))
        c.add(new TypeInsnNode(Opcodes.NEW, "java/io/FileOutputStream"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new TypeInsnNode(Opcodes.NEW, "java/io/File"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new FieldInsnNode(Opcodes.GETSTATIC, TARGET, "qclSaveDst", "Ljava/lang/String;"));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/File", "<init>",
                "(Ljava/lang/String;)V", false));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/FileOutputStream", "<init>",
                "(Ljava/io/File;)V", false));
        c.add(new VarInsnNode(Opcodes.ASTORE, 1));
        // new net.minecraft.client.f(self.b, self.b.p).a(self.b.d, out); out.close();
        c.add(new TypeInsnNode(Opcodes.NEW, "net/minecraft/client/f"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new VarInsnNode(Opcodes.ALOAD, 0));
        c.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "b", "Lnet/minecraft/client/d;"));
        c.add(new VarInsnNode(Opcodes.ALOAD, 0));
        c.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "b", "Lnet/minecraft/client/d;"));
        c.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/d", "p",
                "Lnet/minecraft/client/a;"));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "net/minecraft/client/f", "<init>",
                "(Lnet/minecraft/client/d;La/b;)V", false));
        c.add(new VarInsnNode(Opcodes.ALOAD, 0));
        c.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "b", "Lnet/minecraft/client/d;"));
        c.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/d", "d",
                MAPS_DESC));
        c.add(new VarInsnNode(Opcodes.ALOAD, 1));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/client/f", "a",
                "(" + MAPS_DESC + "Ljava/io/OutputStream;)V", false));
        c.add(new VarInsnNode(Opcodes.ALOAD, 1));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/FileOutputStream", "close", "()V", false));
        // 日志 + 清掉目标
        c.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
        c.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false));
        c.add(new LdcInsnNode("[QCL-saves] 保存文件… 已写入 "));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
        c.add(new FieldInsnNode(Opcodes.GETSTATIC, TARGET, "qclSaveDst", "Ljava/lang/String;"));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString",
                "()Ljava/lang/String;", false));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                "(Ljava/lang/String;)V", false));
        c.add(new InsnNode(Opcodes.ACONST_NULL));
        c.add(new FieldInsnNode(Opcodes.PUTSTATIC, TARGET, "qclSaveDst", "Ljava/lang/String;"));
        c.add(L_noSave);

        // if (qclPending == null) return;
        c.add(new FieldInsnNode(Opcodes.GETSTATIC, TARGET, PENDING_FIELD, "Ljava/io/File;"));
        c.add(new JumpInsnNode(Opcodes.IFNULL, L_end));
        // self.o = qclPending;  qclPending = null;
        c.add(new VarInsnNode(Opcodes.ALOAD, 0));
        c.add(new FieldInsnNode(Opcodes.GETSTATIC, TARGET, PENDING_FIELD, "Ljava/io/File;"));
        c.add(new FieldInsnNode(Opcodes.PUTFIELD, TARGET, "o", "Ljava/io/File;"));
        c.add(new InsnNode(Opcodes.ACONST_NULL));
        c.add(new FieldInsnNode(Opcodes.PUTSTATIC, TARGET, PENDING_FIELD, "Ljava/io/File;"));
        c.add(L_end);
        c.add(new InsnNode(Opcodes.RETURN));
        m.maxStack = 2;
        m.maxLocals = 1;
        return m;
    }

    /**
     * 注入 {@code public static void qclWait(e self, String mode)}：等待玩家在启动器侧
     * 完成"选文件 / 输名字 / 选保存目录"，然后把结果搬到 {@code self.o}。
     *
     * <p><b>为什么把等待逻辑做进 c.e 自己</b>：之前放在独立的辅助类里、靠反射写字段，
     * 结果运行期 {@code getField("o")} / {@code getField("qclPending")} 都抛
     * {@code NoSuchFieldException}（跨类加载器的可见性问题，实测两次都失败）。
     * 现在整个等待线程都编译进 c.e 的字节码 —— 同一个类、同一个加载器，
     * 直接 {@code PUTFIELD}/{@code PUTSTATIC}，不存在任何可见性问题。
     *
     * <p>mode 取值：
     * <ul>
     *   <li>{@code "load"} —— 等一个存档路径，直接设给 self.o</li>
     *   <li>{@code "name"} —— 等世界名，存进 self.qclSaveName，再请求 "save"</li>
     *   <li>{@code "save"} —— 等保存目录，和 self.qclSaveName 拼成目标文件设给 self.o</li>
     * </ul>
     *
     * <p>线程用 {@code java.lang.Thread} 承载：它会 {@code Runnable} 派发到
     * {@code qclRun(self, mode)}（同样是本类的方法），逻辑与普通 Java 一致。
     */
    /**
     * 注入 {@code public static void qclWait(e self, String mode)}：轮询等待玩家在启动器侧
     * 完成"选文件 / 输名字 / 选保存目录"，拿到结果后搬到 {@code self.o}。
     *
     * <p><b>为什么全部塞进这一个方法里</b>：之前把等待逻辑放在独立辅助类、靠反射写字段，
     * 运行期 {@code getField(...)} 一律抛 {@code NoSuchFieldException}（跨类加载器可见性，实测失败两次）。
     * 现在等待循环、字符串处理、字段写入**全部是本类自己的字节码**：
     * 同一个类、同一个加载器，直接 {@code PUTFIELD}/{@code PUTSTATIC}，不存在可见性问题，
     * 也不依赖任何外部类。
     *
     * <p>调用方（{@code qclAskFile} / {@code qclAskName}）已经在**新线程**里跑它，
     * 所以这里的轮询不会卡住游戏主线程。
     *
     * <p>mode 取值：
     * <ul>
     *   <li>{@code "load"} —— 等一个存档路径，直接设给 self.o</li>
     *   <li>{@code "save"} —— 等保存目录，和 self.qclSaveName 拼成目标文件设给 self.o</li>
     * </ul>
     */
    /**
     * 注入两个**实例方法**（无参数，直接读写 this.o）：
     * <ul>
     *   <li>{@code void qclWaitLoad()} —— 等启动器把玩家选的存档路径写进 res.txt，设给 this.o</li>
     *   <li>{@code void qclWaitSave()} —— 等保存目录，和 this.qclSaveName 拼成目标文件设给 this.o</li>
     * </ul>
     *
     * <p><b>为什么是实例方法</b>：静态方法里没有 this，必须靠局部变量传递，而验证器对
     * "把 this 存进局部变量再传出去"的写法很敏感（实测报 "Accessing value from
     * uninitialized register 0"）。实例方法里 {@code ALOAD 0} 天然就是 this，
     * 字段读写也直接用 this，没有任何歧义。
     *
     * <p>等待循环、字符串处理、字段写入全部是本类自己的字节码 —— 同一个类、同一个加载器，
     * 不依赖反射，也不存在跨类可见性问题。
     */
    private static MethodNode buildWaitLoadMethod() {
        return buildWaiter(WAIT_LOAD, false);
    }

    private static MethodNode buildWaitSaveMethod() {
        return buildWaiter(WAIT_SAVE, true);
    }

    /**
     * 生成等待方法。
     *
     * @param name  方法名
     * @param saving true = 保存流程（拿目录 + 拼名字），false = 读档流程（拿文件路径）
     */
    private static MethodNode buildWaiter(String name, boolean saving) {
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                name, "()V", null, null);
        InsnList c = m.instructions;

        LabelNode L_loop = new LabelNode();
        LabelNode L_timeout = new LabelNode();
        LabelNode L_ret = new LabelNode();
        LabelNode L_sleep = new LabelNode();
        LabelNode L_afterSleep = new LabelNode();
        LabelNode L_try = new LabelNode();
        LabelNode L_tryEnd = new LabelNode();
        LabelNode L_catch = new LabelNode();
        LabelNode L_nameOk = new LabelNode();
        LabelNode L_nameNull = new LabelNode();
        LabelNode L_suffixOk = new LabelNode();
        LabelNode L_suffixOk2 = new LabelNode();

        // String resPath = MAILBOX + "/res.txt";
        c.add(new LdcInsnNode(MAILBOX + "/res.txt"));
        c.add(new VarInsnNode(Opcodes.ASTORE, 1));
        // int tries = 0;
        c.add(new InsnNode(Opcodes.ICONST_0));
        c.add(new VarInsnNode(Opcodes.ISTORE, 2));

        c.add(L_loop);
        // if (tries >= 240) goto timeout;
        c.add(new VarInsnNode(Opcodes.ILOAD, 2));
        c.add(new IntInsnNode(Opcodes.SIPUSH, 240));
        c.add(new JumpInsnNode(Opcodes.IF_ICMPGE, L_timeout));
        c.add(new IincInsnNode(2, 1));
        // try { Thread.sleep(500L); } catch { }
        c.add(L_try);
        c.add(new LdcInsnNode(500L));
        c.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Thread", "sleep", "(J)V", false));
        c.add(L_tryEnd);
        c.add(new JumpInsnNode(Opcodes.GOTO, L_afterSleep));
        c.add(L_catch);
        c.add(new VarInsnNode(Opcodes.ASTORE, 3));
        c.add(L_afterSleep);
        // File f = new File(resPath);
        c.add(new TypeInsnNode(Opcodes.NEW, "java/io/File"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new VarInsnNode(Opcodes.ALOAD, 1));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/File", "<init>",
                "(Ljava/lang/String;)V", false));
        c.add(new VarInsnNode(Opcodes.ASTORE, 3));
        // if (!f.isFile()) goto loop;
        c.add(new VarInsnNode(Opcodes.ALOAD, 3));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "isFile", "()Z", false));
        c.add(new JumpInsnNode(Opcodes.IFEQ, L_loop));
        // String val = qclRead(f);   ---- 改为内联读取（避免静态工具方法的可见性问题）
        // 这里直接把文件内容读成字符串：new String(Files.readAllBytes(f.toPath()), "UTF-8")
        c.add(new VarInsnNode(Opcodes.ALOAD, 3));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "toPath",
                "()Ljava/nio/file/Path;", false));
        c.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/nio/file/Files", "readAllBytes",
                "(Ljava/nio/file/Path;)[B", false));
        c.add(new VarInsnNode(Opcodes.ASTORE, 6));
        c.add(new TypeInsnNode(Opcodes.NEW, "java/lang/String"));
        c.add(new InsnNode(Opcodes.DUP));
        c.add(new VarInsnNode(Opcodes.ALOAD, 6));
        c.add(new LdcInsnNode("UTF-8"));
        c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>",
                "([BLjava/lang/String;)V", false));
        c.add(new VarInsnNode(Opcodes.ASTORE, 4));
        // f.delete();
        c.add(new VarInsnNode(Opcodes.ALOAD, 3));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "delete", "()Z", false));
        c.add(new InsnNode(Opcodes.POP));
        // if (val == null) goto loop;
        c.add(new VarInsnNode(Opcodes.ALOAD, 4));
        c.add(new JumpInsnNode(Opcodes.IFNULL, L_loop));
        // val = val.trim();
        c.add(new VarInsnNode(Opcodes.ALOAD, 4));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "trim",
                "()Ljava/lang/String;", false));
        c.add(new VarInsnNode(Opcodes.ASTORE, 4));
        // if (val.length() == 0) return;      // 玩家取消
        c.add(new VarInsnNode(Opcodes.ALOAD, 4));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "length", "()I", false));
        c.add(new JumpInsnNode(Opcodes.IFEQ, L_ret));

        if (!saving) {
            // ---- 读档：this.o = new File(val) ----
            c.add(new VarInsnNode(Opcodes.ALOAD, 0));
            c.add(new TypeInsnNode(Opcodes.NEW, "java/io/File"));
            c.add(new InsnNode(Opcodes.DUP));
            c.add(new VarInsnNode(Opcodes.ALOAD, 4));
            c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/File", "<init>",
                    "(Ljava/lang/String;)V", false));
            c.add(new FieldInsnNode(Opcodes.PUTFIELD, TARGET, "o", "Ljava/io/File;"));
            c.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
            c.add(new LdcInsnNode("[QCL-saves] \u5df2\u4ea4\u7ed9\u6e38\u620f\u8bfb\u6863"));
            c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                    "(Ljava/lang/String;)V", false));
            // 【2026-10-05 修复】读档分支**自己 RETURN**，不再 GOTO 到公共的 L_ret。
            //   原因：L_ret 是所有出口的合并点，而保存分支到那里时栈上有残留值，
            //   两条路栈高不一致 → JVM 报 "Inconsistent stack height 1 != 0"。
            //   让每个分支各自 return 就彻底没有合并点。
            c.add(new InsnNode(Opcodes.RETURN));
        } else {
            // ---- 保存分两轮 ----
            //   第一轮：qclSaveName 还是 null → 刚拿到的是**世界名**：
            //     存进 qclSaveName，发出 "save" 请求让启动器弹**文件夹选择器**，然后回到循环继续等。
            //   第二轮：拿到的是**目录** → 拼成目标文件设给 this.o。
            LabelNode L_secondRound = new LabelNode();
            c.add(new FieldInsnNode(Opcodes.GETSTATIC, TARGET, "qclSaveName", "Ljava/lang/String;"));
            c.add(new JumpInsnNode(Opcodes.IFNONNULL, L_secondRound));
            // 第一轮：保存名字
            c.add(new VarInsnNode(Opcodes.ALOAD, 4));
            c.add(new FieldInsnNode(Opcodes.PUTSTATIC, TARGET, "qclSaveName", "Ljava/lang/String;"));
            c.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
            c.add(new LdcInsnNode("[QCL-saves] 已记下世界名，接着请玩家选保存位置"));
            c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                    "(Ljava/lang/String;)V", false));
            // 发出第二轮请求：写 req.txt = "save"
            LabelNode L_reqOk = new LabelNode();
            c.add(new TypeInsnNode(Opcodes.NEW, "java/io/FileWriter"));
            c.add(new InsnNode(Opcodes.DUP));
            c.add(new LdcInsnNode(MAILBOX + "/req.txt"));
            c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/FileWriter", "<init>",
                    "(Ljava/lang/String;)V", false));
            c.add(new VarInsnNode(Opcodes.ASTORE, 7));
            c.add(new VarInsnNode(Opcodes.ALOAD, 7));
            c.add(new LdcInsnNode("save"));
            c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/FileWriter", "write",
                    "(Ljava/lang/String;)V", false));
            c.add(new VarInsnNode(Opcodes.ALOAD, 7));
            c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/FileWriter", "close", "()V", false));
            c.add(L_reqOk);
            c.add(new JumpInsnNode(Opcodes.GOTO, L_loop));   // 回到循环，等目录

            // ★★★【2026-10-05 关键修复】把玩家在**安卓输入框**里敲的名字，
            //   写回「输入世界名称」界面（c.p）的**名字字段**。
            //   为什么必须这样做：
            //     · 名字原先有两条路各写各的 —— 输入框那次存进静态字段 qclSaveName，
            //       而"点保存立即落盘"读的是 c.p 的字段 → 两者对不上，
            //       结果写盘的还是槽位原始文字（"我的世界  -  10-05 21:51"），
            //       在安卓上因含空格而写盘失败（用户实测："输完名字点保存根本没保存"）。
            //     · 现在两条路共用同一份数据：输入框的结果直接落到 c.p 字段，
            //       玩家随后点「保存」时，立即落盘读到的就是玩家真正输的名字。
            //   注：这里只是"顺手把界面字段填上"，第二轮仍然继续等目录，
            //       不影响原来"选目录保存"那条路。
            LabelNode L_setNameNull = new LabelNode();
            c.add(new VarInsnNode(Opcodes.ALOAD, 0));
            c.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "qclSelf", "L" + TARGET + ";"));
            c.add(new JumpInsnNode(Opcodes.IFNULL, L_setNameNull));
            c.add(new VarInsnNode(Opcodes.ALOAD, 0));
            c.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "qclSelf", "L" + TARGET + ";"));
            c.add(new TypeInsnNode(Opcodes.INSTANCEOF, "net/minecraft/client/c/p"));
            c.add(new JumpInsnNode(Opcodes.IFEQ, L_setNameNull));
            // ((c.p) qclSelf).k = 名字
            c.add(new VarInsnNode(Opcodes.ALOAD, 0));
            c.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET, "qclSelf", "L" + TARGET + ";"));
            c.add(new TypeInsnNode(Opcodes.CHECKCAST, "net/minecraft/client/c/p"));
            c.add(new VarInsnNode(Opcodes.ALOAD, 4));
            c.add(new FieldInsnNode(Opcodes.PUTFIELD, "net/minecraft/client/c/p", "k", "Ljava/lang/String;"));
            c.add(L_setNameNull);
            // 第二轮：val 是目录 → 拼目标文件
            //   【2026-10-05 重写】原先这段有多个分支合并点（L_cleanSkip / L_cleanNoTs /
            //   L_nmOk / L_sufOk），其中一个的栈高与其它不一致，导致 JVM 反复报
            //   "Inconsistent stack height 1 != 0"。这里改成**单一路径**：
            //     ① 把名字读进槽 5
            //     ② 去时间戳（只在"找到分隔符"时用 substring 结果，靠槽 5 传递，不留栈值）
            //     ③ 非法字符替换
            //     ④ 空名兜底
            //     ⑤ 补 .mclevel
            //     ⑥ this.o = new File(目录, 名字) 然后 return
            //   每一步算完都 ASTORE 5，栈上不残留任何值 → 没有任何合并点。
            c.add(L_secondRound);
            c.add(new FieldInsnNode(Opcodes.GETSTATIC, TARGET, "qclSaveName", "Ljava/lang/String;"));
            c.add(new VarInsnNode(Opcodes.ASTORE, 5));

            // ② if (nm != null && nm.indexOf("  -  ") >= 0) nm = nm.substring(0, idx).trim();
            LabelNode L_t1 = new LabelNode();
            c.add(new VarInsnNode(Opcodes.ALOAD, 5));
            c.add(new JumpInsnNode(Opcodes.IFNULL, L_t1));
            c.add(new VarInsnNode(Opcodes.ALOAD, 5));
            c.add(new LdcInsnNode("  -  "));
            c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "indexOf",
                    "(Ljava/lang/String;)I", false));
            c.add(new JumpInsnNode(Opcodes.IFLT, L_t1));
            c.add(new VarInsnNode(Opcodes.ALOAD, 5));
            c.add(new InsnNode(Opcodes.ICONST_0));
            c.add(new VarInsnNode(Opcodes.ALOAD, 5));
            c.add(new LdcInsnNode("  -  "));
            c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "indexOf",
                    "(Ljava/lang/String;)I", false));
            c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "substring",
                    "(II)Ljava/lang/String;", false));
            c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "trim",
                    "()Ljava/lang/String;", false));
            c.add(new VarInsnNode(Opcodes.ASTORE, 5));
            c.add(L_t1);

            // ③ nm = nm.replace("/","_").replace("\\","_").replace(":","_").trim();
            LabelNode L_t2 = new LabelNode();
            c.add(new VarInsnNode(Opcodes.ALOAD, 5));
            c.add(new JumpInsnNode(Opcodes.IFNULL, L_t2));
            c.add(new VarInsnNode(Opcodes.ALOAD, 5));
            c.add(new LdcInsnNode("/"));
            c.add(new LdcInsnNode("_"));
            c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "replace",
                    "(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;", false));
            c.add(new LdcInsnNode("\\"));
            c.add(new LdcInsnNode("_"));
            c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "replace",
                    "(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;", false));
            c.add(new LdcInsnNode(":"));
            c.add(new LdcInsnNode("_"));
            c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "replace",
                    "(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;", false));
            c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "trim",
                    "()Ljava/lang/String;", false));
            c.add(new VarInsnNode(Opcodes.ASTORE, 5));
            c.add(L_t2);

            // ④ if (nm == null || nm.length() == 0) nm = "世界";
            LabelNode L_t3 = new LabelNode();
            c.add(new VarInsnNode(Opcodes.ALOAD, 5));
            c.add(new JumpInsnNode(Opcodes.IFNULL, L_t3));
            c.add(new VarInsnNode(Opcodes.ALOAD, 5));
            c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "length", "()I", false));
            c.add(new JumpInsnNode(Opcodes.IFNE, L_t3));
            c.add(new LdcInsnNode("\u4e16\u754c"));
            c.add(new VarInsnNode(Opcodes.ASTORE, 5));
            c.add(L_t3);

            // ⑤ if (!nm.toLowerCase().endsWith(".mclevel")) nm = nm + ".mclevel";
            LabelNode L_t4 = new LabelNode();
            c.add(new VarInsnNode(Opcodes.ALOAD, 5));
            c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "toLowerCase",
                    "()Ljava/lang/String;", false));
            c.add(new LdcInsnNode(".mclevel"));
            c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "endsWith",
                    "(Ljava/lang/String;)Z", false));
            c.add(new JumpInsnNode(Opcodes.IFNE, L_t4));
            c.add(new VarInsnNode(Opcodes.ALOAD, 5));
            c.add(new LdcInsnNode(".mclevel"));
            c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "concat",
                    "(Ljava/lang/String;)Ljava/lang/String;", false));
            c.add(new VarInsnNode(Opcodes.ASTORE, 5));
            c.add(L_t4);

            // ⑥ 【2026-10-05 修复】第二轮**只记录目标路径**，不在本线程写盘！
            //   历史经过（三次返工，务必记住）：
            //     · 最初：设 this.o 交给 f_() 落盘 → 但 o 会被别处塞进"槽位文字+时间戳"
            //       的脏名字，安卓因文件名含冒号而写盘失败 → 崩溃。
            //     · 中间：改成"什么都不做" → 用户反馈"选完目录根本没存入"（确实没写）。
            //     · 然后：在**本线程**直接写盘 → 抛
            //       "No context is current ... Are you running essential and/or <1.13?"
            //       （世界写盘要用 LWJGL/OpenGL，只有**游戏主线程**有 context）→ 崩溃。
            //   现在：本线程只把"目标绝对路径"存进静态字段 qclSaveDst 并置 qclSaving=true，
            //        真正的写盘交给主线程的 qclApplyPending（在 f_() 里被调用）。
            c.add(new LdcInsnNode("qclSaveDst"));
            c.add(new TypeInsnNode(Opcodes.NEW, "java/io/File"));
            c.add(new InsnNode(Opcodes.DUP));
            c.add(new VarInsnNode(Opcodes.ALOAD, 4));
            c.add(new VarInsnNode(Opcodes.ALOAD, 5));
            c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/File", "<init>",
                    "(Ljava/lang/String;Ljava/lang/String;)V", false));
            // ★★★【2026-10-06 关键修复】val 既可能是**目录**，也可能是**完整文件路径**！
            //   实测日志（第二次点「保存文件…」卡死的原因）：
            //     第 1 次：选中 /sdcard/QCL/我的世界_1005_2352.mclevel        ← 这是**文件**
            //     第 2 次：把它当目录 → /sdcard/QCL/我的世界_1005_2352.mclevel/我的世界_1005_2352.mclevel
            //              → 目录不存在 → FileNotFoundException → 界面出不来（看起来卡死）
            //   而且 qclSaveName 存过一次后不清空，第二轮的 nm 一直是旧名字。
            //   修法：先判断 val 是不是以 .mclevel 结尾 ——
            //     是  → 直接当目标文件
            //     否  → 才把 nm 拼上去当目录
            LabelNode L_valIsFile = new LabelNode();
            LabelNode L_dstReady = new LabelNode();
            c.add(new VarInsnNode(Opcodes.ALOAD, 4));
            c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "toLowerCase",
                    "()Ljava/lang/String;", false));
            c.add(new LdcInsnNode(".mclevel"));
            c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "endsWith",
                    "(Ljava/lang/String;)Z", false));
            c.add(new JumpInsnNode(Opcodes.IFNE, L_valIsFile));
            // —— val 是目录：拼上名字 ——
            c.add(new TypeInsnNode(Opcodes.NEW, "java/io/File"));
            c.add(new InsnNode(Opcodes.DUP));
            c.add(new VarInsnNode(Opcodes.ALOAD, 4));
            c.add(new VarInsnNode(Opcodes.ALOAD, 5));
            c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/File", "<init>",
                    "(Ljava/lang/String;Ljava/lang/String;)V", false));
            c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/File", "getAbsolutePath",
                    "()Ljava/lang/String;", false));
            c.add(new JumpInsnNode(Opcodes.GOTO, L_dstReady));
            // —— val 本身就是文件：直接用它 ——
            c.add(L_valIsFile);
            c.add(new VarInsnNode(Opcodes.ALOAD, 4));
            c.add(L_dstReady);
            c.add(new FieldInsnNode(Opcodes.PUTSTATIC, TARGET, "qclSaveDst", "Ljava/lang/String;"));
            c.add(new InsnNode(Opcodes.ICONST_1));
            c.add(new FieldInsnNode(Opcodes.PUTSTATIC, TARGET, "qclSaving", "Z"));
            // ★ 用掉就把 qclSaveName 清空，否则下次进来还会拿旧名字当目录
            c.add(new InsnNode(Opcodes.ACONST_NULL));
            c.add(new FieldInsnNode(Opcodes.PUTSTATIC, TARGET, "qclSaveName", "Ljava/lang/String;"));
            c.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
            c.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
            c.add(new InsnNode(Opcodes.DUP));
            c.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false));
            c.add(new LdcInsnNode("[QCL-saves] 已选定保存位置，交给主线程写盘: "));
            c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
            c.add(new FieldInsnNode(Opcodes.GETSTATIC, TARGET, "qclSaveDst", "Ljava/lang/String;"));
            c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
            c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString",
                    "()Ljava/lang/String;", false));
            c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                    "(Ljava/lang/String;)V", false));
            c.add(new InsnNode(Opcodes.RETURN));
        }

        c.add(L_timeout);
        c.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
        c.add(new LdcInsnNode("[QCL-saves] \u7b49\u5f85\u73a9\u5bb6\u9009\u62e9\u8d85\u65f6"));
        c.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                "(Ljava/lang/String;)V", false));
        // ★★★【2026-10-05 关键修复】先 RETURN 再放 L_ret。
        //   ASM 输出时会把异常处理器（L_catch）插在 L_ret 的位置上，
        //   于是 `ifeq L_ret`（循环里"玩家取消就返回"那条）跳过去时，
        //   落点紧邻处理器入口 —— 处理器那条路栈上有 1 个异常对象，
        //   正常路径是 0 → JVM 报 "Inconsistent stack height 1 != 0"。
        //   在这里先 return，等于给超时路径一个独立出口；
        //   L_ret 随后只作普通 RETURN 使用，不再与处理器入口重合。
        c.add(new InsnNode(Opcodes.RETURN));
        c.add(L_ret);
        c.add(new InsnNode(Opcodes.RETURN));

        m.tryCatchBlocks.add(new TryCatchBlockNode(L_try, L_tryEnd, L_catch, "java/lang/Throwable"));
        m.maxStack = 6;
        m.maxLocals = 8;
        return m;
    }

    private static MethodNode buildImportMethod() {
        MethodNode mn = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, IMPORT_METHOD, IMPORT_DESC, null, null);
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
            c.add(qclIconst(i));
            c.add(new LdcInsnNode(roots[i]));
            c.add(new InsnNode(Opcodes.AASTORE));
        }
        c.add(new VarInsnNode(Opcodes.ASTORE, 3));

        // int i = 0;  while (i < roots.length) { ... }
        c.add(qclIconst(0));
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
        // 【2026-10-05 事故修复】这里原来漏了 ASTORE —— 注释写着 "File[] list = ..."，
        //   但返回值根本没存进槽位，紧接着就 ALOAD 5（读一个从未赋值过的槽）→
        //   验证器报 "Accessing value from uninitialized register 5"。
        //   补上把 listFiles() 的结果存进槽 5（即注释里的 list）。
        c.add(new VarInsnNode(Opcodes.ASTORE, 5));
        // if (list == null) { i++; continue; }
        LabelNode L_afterNull = new LabelNode();
        LabelNode L_inc = new LabelNode();
        c.add(new VarInsnNode(Opcodes.ALOAD, 5));
        c.add(new JumpInsnNode(Opcodes.IFNONNULL, L_afterNull));
        c.add(new JumpInsnNode(Opcodes.GOTO, L_inc));
        c.add(L_afterNull);

        // int j = 0; while (j < list.length) { ... }
        c.add(qclIconst(0));
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
        c.add(qclIconst(0));
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
        c.add(qclIconst(0));
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
    private static AbstractInsnNode qclIconst(int v) {
        if (v >= -1 && v <= 5) {
            return new InsnNode(Opcodes.ICONST_0 + v);
        }
        return new IntInsnNode(Opcodes.BIPUSH, v);
    }

    /**
     * ★★★【2026-10-06】探测 {@code net.minecraft.client.d.d}（地图数据）的真实描述符。
     *
     * <p>为什么必须探测：写存档要调用
     * {@code new net.minecraft.client.f(client, client.p).a(client.d, out)}，
     * 而 {@code client.d} 的类型**每个版本都不同**（javap 实测）：
     * <pre>
     *   inf-20100227-1433 / inf-20100316       → Lnet/minecraft/a/a/e;
     *   inf-20100313 / inf-20100320 / 1857     → Lnet/minecraft/a/a/f;
     *   in-20100223 / inf-20100325-1640        → Lnet/minecraft/a/a/g;
     * </pre>
     * 硬编码成 {@code g} 会让其它版本运行时抛 {@code NoSuchFieldError}，
     * 表现就是「保存文件… 点完之后一直卡在正在保存」。
     *
     * <p>做法：打开目标版本的 {@code client/d.class}，找名为 {@code d}、
     * 描述符形如 {@code Lnet/minecraft/a/a/…;} 的实例字段，把它的描述符记下来。
     * 找不到就保留默认值并在日志里告警（不致命，那一版的写盘会退化为报错但不崩）。
     */
    private static void detectMapDesc(java.util.zip.ZipFile zf) {
        try {
            java.util.zip.ZipEntry ze = zf.getEntry("net/minecraft/client/d.class");
            if (ze == null) {
                System.out.println("   !! 没有 client/d.class，无法探测地图字段类型");
                return;
            }
            InputStream is = zf.getInputStream(ze);
            ClassNode dn = new ClassNode();
            new ClassReader(is).accept(dn, 0);
            is.close();
            for (FieldNode fn : dn.fields) {
                if ("d".equals(fn.name) && fn.desc != null
                        && fn.desc.startsWith("Lnet/minecraft/a/a/")) {
                    MAPS_DESC = fn.desc;
                    System.out.println("   地图字段 client/d.d 的真实类型: " + MAPS_DESC);
                    return;
                }
            }
            System.out.println("   !! client/d 里没找到地图字段 d（保留默认 " + MAPS_DESC + "）");
        } catch (Throwable t) {
            System.out.println("   !! 探测地图字段类型失败（保留默认）: " + t);
        }
    }
}
