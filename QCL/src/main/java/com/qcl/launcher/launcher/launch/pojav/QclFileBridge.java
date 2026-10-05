package com.qcl.launcher.launcher.launch.pojav;

import android.app.Activity;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import com.qcl.launcher.R;
import com.tungsten.filepicker.Constants;
import com.tungsten.filepicker.FileChooser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;

/**
 * 游戏内「载入文件…／保存文件…」按钮 ↔ 安卓文件选择器 之间的桥。
 *
 * <p><b>为什么需要它：</b>游戏跑在独立的 JVM 里（不是安卓运行时），
 * 实测 {@code Class.forName("android.app.ActivityThread")} 直接
 * {@code ClassNotFoundException} —— 所以游戏代码**无法自己弹出安卓组件**。
 * 但游戏进程是启动器的子进程，两边共享同一个外部存储，于是用文件当信箱：
 *
 * <pre>
 *   /sdcard/QCL/.qcl_bridge/req.txt   游戏写入请求（内容只有 "load" 或 "save"）
 *   /sdcard/QCL/.qcl_bridge/res.txt   启动器写入结果（绝对路径；取消则写空文件）
 * </pre>
 *
 * <p>游戏侧的动作（见 tools/cacio_patch 的 PatchSaveList）：点按钮 → 写 req.txt →
 * 起一个守护线程等 res.txt → 拿到路径后交给原版的读档/写盘逻辑。
 * 本类只负责：轮询 req.txt → 弹系统文件选择器 → 把结果写进 res.txt。
 *
 * <p>轮询间隔 400ms，且只在**有请求文件**时才做任何事，平时完全是空转，不影响游戏性能。
 */
public final class QclFileBridge {

    private static final String TAG = "QCL-filebridge";

    /** 信箱目录（与游戏侧的路径必须一致）。 */
    public static final String DIR = "/sdcard/QCL/.qcl_bridge";
    private static final String REQ = DIR + "/req.txt";
    private static final String RES = DIR + "/res.txt";

    /** 请求码（避免与启动器其它 startActivityForResult 冲突）。 */
    private static final int REQ_PICK_LOAD = 0x0C2A;
    private static final int REQ_PICK_SAVE = 0x0C2B;

    private static volatile boolean running = false;
    private static Activity activity;

    private QclFileBridge() {
    }

    /** 在游戏 Activity 起来后调用一次；由 PojavMinecraftActivity 转发生命周期。 */
    public static void start(Activity act) {
        activity = act;
        if (running) {
            return;
        }
        running = true;
        File dir = new File(DIR);
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        // 清掉上次残留，免得一进游戏就误弹选择器
        //noinspection ResultOfMethodCallIgnored
        new File(REQ).delete();
        //noinspection ResultOfMethodCallIgnored
        new File(RES).delete();

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                pollLoop();
            }
        }, "QCL-filebridge");
        t.setDaemon(true);
        t.start();
        Log.i(TAG, "文件桥已启动，信箱目录 " + DIR);
    }

    public static void stop() {
        running = false;
        activity = null;
    }

    private static void pollLoop() {
        while (running) {
            try {
                Thread.sleep(400L);
            } catch (InterruptedException ie) {
                return;
            }
            try {
                Activity act = activity;
                if (act == null) {
                    continue;
                }
                File req = new File(REQ);
                if (!req.isFile()) {
                    continue;
                }
                String mode = readText(req);
                //noinspection ResultOfMethodCallIgnored
                req.delete();
                mode = mode == null ? "" : mode.trim();
                if (mode.length() == 0) {
                    continue;
                }
                Log.i(TAG, "收到游戏请求: " + mode);
                if ("name".equals(mode)) {
                    // ★★★ 存盘前先向玩家要一个世界名。
                    //   为什么用启动器侧的输入框而不是游戏内的：游戏内的文字输入要先点输入框、
                    //   再点屏幕顶部那块虚拟键盘才能打字，玩家基本用不了。
                    askName(act);
                } else {
                    launchPicker(act, mode);
                }
            } catch (Throwable tr) {
                Log.w(TAG, "轮询出错", tr);
            }
        }
    }

    /**
     * 弹出安卓原生输入框，让玩家给世界起名（比游戏内的输入体验好得多）。
     * 结果同样写进 {@code res.txt}；取消则写空文件。
     */
    private static void askName(final Activity act) {
        try {
            act.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final android.widget.EditText input = new android.widget.EditText(act);
                        input.setHint("世界名称");
                        input.setSingleLine(true);
                        // 预填一个不会覆盖已有存档的名字
                        input.setText("我的世界_"
                                + new java.text.SimpleDateFormat("MMdd_HHmm", java.util.Locale.US)
                                        .format(new java.util.Date()));
                        input.setSelection(input.getText().length());
                        new android.app.AlertDialog.Builder(act)
                                .setTitle("保存世界")
                                .setMessage("给这个存档起个名字：")
                                .setView(input)
                                .setCancelable(false)
                                .setPositiveButton("确定", new android.content.DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(android.content.DialogInterface d, int which) {
                                        String name = input.getText() == null
                                                ? "" : input.getText().toString().trim();
                                        Log.i(TAG, "玩家输入存档名: " + name);
                                        writeResult(name.length() == 0 ? null : name);
                                    }
                                })
                                .setNegativeButton("取消", new android.content.DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(android.content.DialogInterface d, int which) {
                                        Log.i(TAG, "玩家取消命名");
                                        writeResult(null);
                                    }
                                })
                                .show();
                    } catch (Throwable tr) {
                        Log.w(TAG, "弹输入框失败", tr);
                        writeResult(null);
                    }
                }
            });
        } catch (Throwable tr) {
            Log.w(TAG, "askName 失败", tr);
            writeResult(null);
        }
    }

    private static void launchPicker(Activity act, String mode) {
        try {
            boolean saving = "save".equals(mode);
            // 保存时选**文件夹**（"存到哪里"），载入时选**文件**（.mclevel）
            if (saving) {
                Intent intent = new Intent(act, com.tungsten.filepicker.FolderChooser.class);
                intent.putExtra(Constants.SELECTION_MODE,
                        Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal());
                // 默认开在 QCL 目录（存档就在它下面），玩家一般直接点确定即可
                File root = new File(Environment.getExternalStorageDirectory(), "QCL");
                intent.putExtra(Constants.INITIAL_DIRECTORY,
                        (root.isDirectory() ? root
                                : Environment.getExternalStorageDirectory()).getAbsolutePath());
                act.startActivityForResult(intent, REQ_PICK_SAVE);
                return;
            }
            Intent intent = new Intent(act, FileChooser.class);
            intent.putExtra(Constants.SELECTION_MODE,
                    Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal());
            // ★★★ 后缀必须用【分号】分隔 —— FileChooser 里是 split(";")，
            //   传逗号会让整串被当成一个后缀名，结果一个文件都显示不出来
            //   （用户实测："下载文件夹里有存档，但选择器里看不到"）。
            intent.putExtra(Constants.ALLOWED_FILE_EXTENSIONS, "mclevel;zip;mcworld");
            // ★★★ 直接开在 Download —— 玩家下载的存档基本都在那儿，
            //   省掉"从根目录一路翻/滚动找 Download"这一大堆操作（实测很容易点偏）。
            //   路径不存在时 FileChooser 会自己回退到根目录。
            File dl = new File(Environment.getExternalStorageDirectory(), "Download");
            intent.putExtra(Constants.INITIAL_DIRECTORY,
                    (dl.isDirectory() ? dl : Environment.getExternalStorageDirectory()).getAbsolutePath());
            act.startActivityForResult(intent, REQ_PICK_LOAD);
        } catch (Throwable tr) {
            Log.w(TAG, "打不开文件选择器", tr);
            writeResult(null);
        }
    }

    /**
     * 由 PojavMinecraftActivity.onActivityResult 调用。
     *
     * @return true 表示这个结果属于文件桥（已被消费）
     */
    public static boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQ_PICK_LOAD && requestCode != REQ_PICK_SAVE) {
            return false;
        }
        String path = null;
        try {
            if (resultCode == Activity.RESULT_OK && data != null) {
                // ① FileChooser 现在会直接给绝对路径（最可靠）
                path = data.getStringExtra("path");
                // ② 兼容单选取模式：结果放在 data 的 Uri 里
                if (path == null && data.getData() != null) {
                    android.net.Uri uri = data.getData();
                    if ("file".equals(uri.getScheme())) {
                        path = uri.getPath();
                    } else {
                        path = com.qcl.launcher.utils.file.UriUtils.getRealPathFromUri_AboveApi19(
                                activity, uri);
                    }
                }
                // ③ 再兜底：FileChooser 原本返回的是 Uri 列表
                if (path == null) {
                    java.util.ArrayList<android.os.Parcelable> items =
                            data.getParcelableArrayListExtra(Constants.SELECTED_ITEMS);
                    if (items != null && !items.isEmpty()) {
                        Object first = items.get(0);
                        if (first instanceof android.net.Uri) {
                            android.net.Uri uri = (android.net.Uri) first;
                            path = "file".equals(uri.getScheme())
                                    ? uri.getPath()
                                    : com.qcl.launcher.utils.file.UriUtils
                                            .getRealPathFromUri_AboveApi19(activity, uri);
                        } else if (first != null) {
                            path = first.toString();
                        }
                    }
                }
            }
        } catch (Throwable tr) {
            Log.w(TAG, "解析选择结果失败", tr);
        }
        Log.i(TAG, "玩家选择: " + (path == null ? "（取消）" : path));
        writeResult(path);
        return true;
    }

    private static void writeResult(String path) {
        try {
            File dir = new File(DIR);
            if (!dir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                dir.mkdirs();
            }
            FileOutputStream fos = new FileOutputStream(RES, false);
            try {
                // 取消时写空文件 —— 游戏侧读到空内容就知道是取消
                if (path != null) {
                    fos.write(path.getBytes(Charset.forName("UTF-8")));
                }
                fos.flush();
            } finally {
                fos.close();
            }
        } catch (Throwable tr) {
            Log.w(TAG, "写结果失败", tr);
        }
    }

    private static String readText(File f) {
        try {
            RandomAccessFile raf = new RandomAccessFile(f, "r");
            try {
                byte[] buf = new byte[(int) raf.length()];
                raf.readFully(buf);
                return new String(buf, Charset.forName("UTF-8"));
            } finally {
                raf.close();
            }
        } catch (Throwable tr) {
            return null;
        }
    }
}
