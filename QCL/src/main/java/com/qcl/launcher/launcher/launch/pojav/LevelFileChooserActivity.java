package com.qcl.launcher.launcher.launch.pojav;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 远古版本「加载文件…」用的**游戏内文件浏览器**。
 *
 * <p>为什么需要它（而不是直接用 AWT FileDialog 或系统选择器）：
 * <ul>
 *   <li>cacio 的 AWT 窗口在安卓上没有窗口实体（日志原话
 *       {@code CacioComponentPeer::setZOrder: NOT YET IMPLEMENTED}），
 *       所以游戏里那个 FileDialog **根本画不出来** —— 用户表现就是"点了没反应、选不了文件"；</li>
 *   <li>系统文件选择器（GET_CONTENT）在模拟器/部分设备上不一定有可用的"文件"应用，
 *       而且不同 ROM 返回的结果差别很大。</li>
 * </ul>
 * 所以自己画一个：目录能点进去、能返回上一级，选中 {@code .mclevel} 就把**绝对路径**返回给游戏。
 *
 * <p>结果通过 {@code PojavMinecraftActivity.onActivityResult} → cacio 的 PickerHook 交回游戏进程，
 * 游戏随后用那个路径读档（并把文件收进存档目录）。
 */
public class LevelFileChooserActivity extends Activity {

    /** PojavMinecraftActivity 里用来识别这次请求的请求码（与 cacio 的 PickerHook.REQUEST_CODE 一致）。 */
    public static final int REQUEST_CODE = 0x0C1F;

    private File currentDir;
    private TextView pathView;
    private ArrayAdapter<String> adapter;
    private final List<File> entries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF1B1B1B);

        pathView = new TextView(this);
        pathView.setTextColor(0xFFFFFFFF);
        pathView.setTextSize(13f);
        pathView.setPadding(24, 24, 24, 12);
        root.addView(pathView);

        ListView list = new ListView(this);
        list.setBackgroundColor(0xFF101010);
        root.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                new ArrayList<String>()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(0xFFFFFFFF);
                tv.setTextSize(15f);
                return tv;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File f = entries.get(position);
                if (f.isDirectory()) {
                    currentDir = f;
                    refresh();
                } else {
                    Intent data = new Intent();
                    data.putExtra("path", f.getAbsolutePath());
                    setResult(RESULT_OK, data);
                    finish();
                }
            }
        });

        // 未选中任何文件就返回 = 取消（返回 null 路径，游戏侧会放弃这次读取）
        setResult(RESULT_CANCELED);

        currentDir = pickStartDir();
        refresh();
        setContentView(root);
    }

    /** 起始目录：优先游戏存档目录，其次手机下载目录，最后外部存储根。 */
    private File pickStartDir() {
        String[] roots = {
                "/storage/emulated/0/QCL/.minecraft/saves",
                "/sdcard/QCL/.minecraft/saves",
                Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download",
                Environment.getExternalStorageDirectory().getAbsolutePath(),
                "/sdcard",
                "/storage/emulated/0",
        };
        for (String r : roots) {
            File f = new File(r);
            if (f.isDirectory()) {
                return f;
            }
        }
        return new File("/");
    }

    private void refresh() {
        entries.clear();
        adapter.clear();
        pathView.setText("当前目录：" + currentDir.getAbsolutePath()
                + "\n（点文件夹进入；点 .mclevel 选中它；按返回键退出）");

        File parent = currentDir.getParentFile();
        if (parent != null) {
            entries.add(parent);
            adapter.add("⬆  ..返回上一级");
        }
        File[] list = currentDir.listFiles();
        if (list != null) {
            Arrays.sort(list, new Comparator<File>() {
                @Override
                public int compare(File a, File b) {
                    if (a.isDirectory() != b.isDirectory()) {
                        return a.isDirectory() ? -1 : 1;
                    }
                    return a.getName().compareToIgnoreCase(b.getName());
                }
            });
            for (File f : list) {
                String n = f.getName();
                if (f.isDirectory()) {
                    if (n.startsWith(".")) {
                        continue;
                    }
                    entries.add(f);
                    adapter.add("📁 " + n);
                } else {
                    String lower = n.toLowerCase();
                    if (lower.endsWith(".mclevel") || lower.endsWith(".zip")
                            || lower.endsWith(".mcworld") || lower.endsWith(".dat")) {
                        entries.add(f);
                        adapter.add("💾 " + n);
                    }
                }
            }
        }
        if (adapter.isEmpty()) {
            adapter.add("（这个目录里没有可用的存档文件）");
            entries.add(currentDir);
        }
        adapter.notifyDataSetChanged();
    }
}
