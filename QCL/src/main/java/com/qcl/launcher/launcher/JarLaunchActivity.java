package com.qcl.launcher.launcher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.qcl.launcher.R;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.utils.game.JarJavaProbe;

import net.kdt.pojavlaunch.utils.JREUtils;

import java.io.File;
import java.util.ArrayList;

/**
 * ★★★★★ 2026-10-17（1.5.2 新增）**JAR 启动器**（FCL 那种独立功能，不是「启动游戏」）。
 *
 * <p>用户需求原话：「FCL 做的是这个 JAR 的启动，可以启动其他应用来玩玩的，
 * 不是专门在启动游戏的那个按钮，而是专门搞了个独立按钮，就是 JAR 启动」。
 *
 * <p>★ 为什么必须是**独立进程**（Manifest 里标了 {@code android:process=":jarlaunch"}）：
 * <ul>
 *   <li>Android 上一个进程**只能启动一次 JVM**（{@code VMLauncher.launchJVM} 之后就无法再启）；</li>
 *   <li>而 QCL 启动 Minecraft 也是「在本进程内启 JVM」；</li>
 *   <li>如果 JAR 启动器跑在主进程里，玩家跑完一个 jar 之后**就再也启动不了 Minecraft**，
 *       只能杀进程重开 —— 这是不可接受的。</li>
 * </ul>
 * ⇒ 放进 {@code :jarlaunch} 子进程：JVM 只污染这个子进程，跑完直接 finish，
 *   主进程完全不受影响，玩家回到启动器可以照常启动游戏。
 *
 * <p>启动方式复用 {@link JREUtils#launchAPIInstaller}（Forge/Cleanroom 安装器就是用它跑起来的），
 * 传的命令是 {@code ["java", "-jar", <路径>]}。
 */
public class JarLaunchActivity extends Activity implements View.OnClickListener {

    public static final String EXTRA_JAR_PATH = "jar_path";

    private final Handler ui = new Handler(Looper.getMainLooper());

    private TextView titleView;
    private TextView pathView;
    private TextView outputView;
    private ScrollView scrollView;

    private volatile boolean running = false;
    private Thread worker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ui_jar_launch);

        AppManifest.initializeManifest(getApplicationContext());

        this.titleView = findViewById(R.id.jar_launch_title);
        this.pathView = findViewById(R.id.jar_launch_path);
        this.outputView = findViewById(R.id.jar_launch_output);
        this.scrollView = findViewById(R.id.jar_launch_scroll);
        Button stop = findViewById(R.id.jar_launch_stop);
        Button exit = findViewById(R.id.jar_launch_exit);
        if (stop != null) {
            stop.setOnClickListener(this);
        }
        if (exit != null) {
            exit.setOnClickListener(this);
        }

        String jarPath = getIntent() == null ? null : getIntent().getStringExtra(EXTRA_JAR_PATH);
        if (jarPath == null) {
            append("没有收到 jar 路径，退出。");
            return;
        }
        File jar = new File(jarPath);
        if (!jar.isFile()) {
            append("文件不存在：" + jarPath);
            return;
        }
        if (this.pathView != null) {
            this.pathView.setText(jarPath);
        }
        start(jar);
    }

    /** 起后台线程跑 JVM（JVM 必须在非 UI 线程启动）。 */
    private void start(final File jar) {
        final int needJava = JarJavaProbe.requiredJavaVersion(jar.getAbsolutePath());
        if (needJava == JarJavaProbe.UNKNOWN) {
            append("提示：读不出这个 jar 要求的 Java 版本，将用内置 Java 8 试跑。");
        } else {
            append("这个 jar 需要 Java " + needJava + " 或更高。");
        }
        append("开始启动 JVM…（首次启动较慢，请耐心等待）");

        this.running = true;
        this.worker = new Thread(() -> {
            int code = -1;
            try {
                // ★ launchAPIInstaller 的约定：commands[0] 会被丢弃，然后前面补一个 "java"
                //   ⇒ 这里传 ["java", "-jar", <path>]
                ArrayList<String> commands = new ArrayList<>();
                commands.add("java");
                commands.add("-jar");
                commands.add(jar.getAbsolutePath());

                code = JREUtils.launchAPIInstaller(
                        JarLaunchActivity.this,
                        AppManifest.JAVA_DIR + "/default",
                        commands,
                        AppManifest.DEBUG_DIR);
            } catch (Throwable t) {
                append("启动失败：" + t);
            } finally {
                this.running = false;
                final int exit = code;
                ui.post(() -> append("进程已退出，退出码 = " + exit
                        + "\n（回到启动器后可以照常启动 Minecraft，不受影响。）"));
            }
        }, "qcl-jar-launch");
        this.worker.start();
    }

    /** 往控制台追加一行（自动滚到底）。 */
    private void append(final String line) {
        ui.post(() -> {
            if (this.outputView == null) {
                return;
            }
            this.outputView.append(line);
            this.outputView.append("\n");
            if (this.scrollView != null) {
                this.scrollView.post(() -> this.scrollView.fullScroll(View.FOCUS_DOWN));
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.jar_launch_stop) {
            append("已请求退出（JVM 无法被安全强杀，请按「退出」关闭本页）。");
        } else if (id == R.id.jar_launch_exit) {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ★ 本 Activity 在 :jarlaunch 子进程里，finish 后由系统回收该进程，
        //   主进程的 JVM 状态完全不受影响。
    }

    /** 供外部（版本列表等）调用：用独立进程启动一个 jar。 */
    public static void launch(Activity from, String jarPath) {
        if (from == null || jarPath == null) {
            return;
        }
        Intent intent = new Intent(from, JarLaunchActivity.class);
        intent.putExtra(EXTRA_JAR_PATH, jarPath);
        from.startActivity(intent);
    }
}
