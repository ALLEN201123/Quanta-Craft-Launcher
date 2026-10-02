package com.qcl.launcher.launcher.dialogs.lab;

import android.app.Dialog;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.qcl.launcher.R;
import com.qcl.launcher.launcher.MainActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 数据包图形化制作与导出：填写名称/命名空间/pack_format，逐条添加 function 命令，导出为 zip。
 */
public class LabDatapackDialog extends Dialog {

    /** pack_format 展示标签。 */
    private static final String[] PACK_LABELS = {
            "4 (1.13-1.14)", "5 (1.15-1.16.1)", "6 (1.16.2-1.16.5)", "7 (1.17)",
            "8 (1.18)", "9 (1.19)", "10 (1.19.3)", "12 (1.19.4)",
            "13 (1.20-1.20.1)", "15 (1.20.2)", "18 (1.20.3-1.20.4)"
    };
    private static final int[] PACK_VALUES = {4, 5, 6, 7, 8, 9, 10, 12, 13, 15, 18};
    /** 默认选中 15（索引 9）。 */
    private static final int PACK_DEFAULT_INDEX = 9;

    private static final String[] TEMPLATE_NAMES = {"give", "tp", "tellraw", "summon", "setblock"};
    private static final String[] TEMPLATE_COMMANDS = {
            "give @p minecraft:diamond 1",
            "tp @p ~ ~1 ~",
            "tellraw @a {\"text\":\"Hello\"}",
            "summon minecraft:zombie ~ ~ ~",
            "setblock ~ ~ ~ minecraft:stone"
    };

    /** 单个 function 的编辑数据。 */
    private static class Func {
        EditText nameEdit;
        EditText cmdEdit;
        LinearLayout card;
    }

    private final MainActivity activity;
    private final List<Func> funcs = new ArrayList<>();
    private EditText activeCmdEdit;

    private EditText nameEdit;
    private EditText namespaceEdit;
    private Spinner packSpinner;
    private LinearLayout functionsContainer;
    private LinearLayout templatesContainer;
    /** 目标版本的数据包元信息（读自该版本 jar，离线权威）。 */
    private LabUtils.VersionPackInfo packInfo = new LabUtils.VersionPackInfo();
    /** 自动取到的 pack_format（≤0 表示没读到）。 */
    private int autoPackFormat = -1;
    /** 下拉每一项对应的 pack_format；-1 = 用自动值。 */
    private int[] packSpinnerValues = PACK_VALUES;

    public LabDatapackDialog(MainActivity activity) {
        super(activity);
        this.activity = activity;
        setContentView(R.layout.dialog_lab_datapack);
        init();
        LabUtils.setupDialogWindow(this);
    }

    private void init() {
        this.nameEdit = findViewById(R.id.lab_dp_name);
        this.namespaceEdit = findViewById(R.id.lab_dp_namespace);
        this.packSpinner = findViewById(R.id.lab_dp_packformat);
        this.functionsContainer = findViewById(R.id.lab_dp_functions);
        this.templatesContainer = findViewById(R.id.lab_dp_templates);

        // ★★★ 1.4.3：pack_format 改成**从目标版本的 jar 里自动取**（离线、权威、自维护），
        //   手选档位只作兜底。原实现写死最高 18、默认 15，而实测 1.20.6 实际是 41
        //   → 用旧档位导出的数据包在新版本上根本不会被加载。
        this.packInfo = LabUtils.readVersionPackInfo(activity);
        this.autoPackFormat = packInfo.packFormat;
        java.util.List<String> pfLabels = new java.util.ArrayList<>();
        java.util.List<Integer> pfValues = new java.util.ArrayList<>();
        pfLabels.add(autoPackFormat > 0
                ? getContext().getString(R.string.lab_dp_packformat_auto, packInfo.versionName, autoPackFormat)
                : getContext().getString(R.string.lab_dp_packformat_auto_miss, packInfo.versionName));
        pfValues.add(-1);
        for (int i = 0; i < PACK_LABELS.length; i++) {
            pfLabels.add(PACK_LABELS[i]);
            pfValues.add(PACK_VALUES[i]);
        }
        this.packSpinnerValues = new int[pfValues.size()];
        for (int i = 0; i < pfValues.size(); i++) {
            this.packSpinnerValues[i] = pfValues.get(i);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, pfLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        packSpinner.setAdapter(adapter);
        packSpinner.setSelection(0);   // 默认「自动」

        findViewById(R.id.lab_dp_add_function).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addFunction();
            }
        });
        findViewById(R.id.lab_dp_export).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                export();
            }
        });
        findViewById(R.id.lab_dp_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        buildTemplates();
        addFunction();
    }

    private void buildTemplates() {
        LinearLayout row = null;
        for (int i = 0; i < TEMPLATE_NAMES.length; i++) {
            if (i % 3 == 0) {
                row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rlp.setMargins(0, dp(3), 0, dp(3));
                row.setLayoutParams(rlp);
                templatesContainer.addView(row);
            }
            final int index = i;
            Button button = new Button(getContext());
            button.setText(TEMPLATE_NAMES[i]);
            button.setAllCaps(false);
            button.setTextSize(11);
            button.setTextColor(Color.parseColor("#0E9384"));
            button.setBackgroundResource(R.drawable.launcher_button_parent);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i % 3 != 0) {
                lp.setMargins(dp(6), 0, 0, 0);
            }
            button.setLayoutParams(lp);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    insertTemplate(index);
                }
            });
            row.addView(button);
        }
    }

    private void addFunction() {
        final Func func = new Func();

        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.parseColor("#0D000000"));
        card.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.setMargins(0, dp(6), 0, dp(6));
        card.setLayoutParams(clp);

        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final EditText nameEdit = new EditText(getContext());
        nameEdit.setSingleLine(true);
        nameEdit.setTextSize(13);
        nameEdit.setHint(R.string.lab_dp_func_name_hint);
        nameEdit.setBackgroundColor(Color.parseColor("#0D000000"));
        nameEdit.setPadding(dp(6), dp(4), dp(6), dp(4));
        nameEdit.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        nameEdit.setText(funcs.isEmpty() ? "main" : "func" + (funcs.size() + 1));

        Button delete = new Button(getContext());
        delete.setText(R.string.lab_dp_delete_function);
        delete.setAllCaps(false);
        delete.setTextSize(11);
        delete.setTextColor(Color.parseColor("#0E9384"));
        delete.setBackgroundResource(R.drawable.launcher_button_parent);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dlp.setMargins(dp(6), 0, 0, 0);
        delete.setLayoutParams(dlp);

        header.addView(nameEdit);
        header.addView(delete);

        final EditText cmdEdit = new EditText(getContext());
        cmdEdit.setGravity(Gravity.TOP | Gravity.START);
        cmdEdit.setTextSize(12);
        cmdEdit.setHint(R.string.lab_dp_cmd_hint);
        cmdEdit.setMinLines(3);
        cmdEdit.setBackgroundColor(Color.parseColor("#0D000000"));
        cmdEdit.setPadding(dp(6), dp(4), dp(6), dp(4));
        LinearLayout.LayoutParams cmdLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cmdLp.setMargins(0, dp(6), 0, 0);
        cmdEdit.setLayoutParams(cmdLp);

        func.card = card;
        func.nameEdit = nameEdit;
        func.cmdEdit = cmdEdit;

        cmdEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    activeCmdEdit = cmdEdit;
                }
            }
        });
        cmdEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activeCmdEdit = cmdEdit;
            }
        });

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                funcs.remove(func);
                functionsContainer.removeView(func.card);
                if (activeCmdEdit == cmdEdit) {
                    activeCmdEdit = funcs.isEmpty() ? null : funcs.get(funcs.size() - 1).cmdEdit;
                }
            }
        });

        card.addView(header);
        card.addView(cmdEdit);
        functionsContainer.addView(card);
        funcs.add(func);
        activeCmdEdit = cmdEdit;
    }

    private void insertTemplate(int index) {
        if (activeCmdEdit == null) {
            LabUtils.toast(getContext(), getContext().getString(R.string.lab_dp_hint_no_func));
            return;
        }
        String current = activeCmdEdit.getText().toString();
        if (!current.isEmpty() && !current.endsWith("\n")) {
            current = current + "\n";
        }
        activeCmdEdit.setText(current + TEMPLATE_COMMANDS[index] + "\n");
        activeCmdEdit.setSelection(activeCmdEdit.getText().length());
    }

    /** 解析最终使用的 pack_format：选「自动」时用从 jar 读到的值；都没读到才退回旧默认。 */
    private int resolvePackFormat() {
        int pos = Math.max(0, packSpinner.getSelectedItemPosition());
        int picked = (pos < packSpinnerValues.length) ? packSpinnerValues[pos] : -1;
        if (picked > 0) {
            return picked;
        }
        return autoPackFormat > 0 ? autoPackFormat : PACK_VALUES[PACK_DEFAULT_INDEX];
    }

    private void export() {
        String packName = nameEdit.getText().toString().trim();
        if (packName.isEmpty()) {
            LabUtils.toast(getContext(), getContext().getString(R.string.lab_dp_toast_name_empty));
            return;
        }
        String namespace = LabUtils.sanitizeNamespace(namespaceEdit.getText().toString());
        if (namespace.isEmpty()) {
            namespace = "default";
            LabUtils.toast(getContext(), getContext().getString(R.string.lab_dp_toast_ns_empty));
        }
        // ★ 1.4.3：至少要有一条命令，否则导出的是空壳包（新手最容易卡在这）
        boolean anyCommand = false;
        for (Func func : funcs) {
            if (!func.cmdEdit.getText().toString().trim().isEmpty()) {
                anyCommand = true;
                break;
            }
        }
        if (!anyCommand) {
            LabUtils.toast(getContext(), getContext().getString(R.string.lab_dp_toast_cmd_empty));
            return;
        }
        int packFormat = resolvePackFormat();
        // ★ 1.4.3：数据包目录名 1.21 起改成**单数**（function/），由 jar 探测得出，不写死。
        String functionDir = packInfo.functionDir;

        File dir = LabUtils.getDatapacksDir(activity);
        File outFile = new File(dir, LabUtils.sanitizeFileName(packName) + ".zip");
        int suspicious = 0;
        int autoNamed = 0;
        String firstFuncName = "";
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outFile))) {
            String description = packName.replace("\\", "").replace("\"", "'");
            // ★ 1.4.3：pack.mcmeta 的**写法要跟着目标版本走**（照抄游戏自带 pack.mcmeta 的风格）：
            //   · 1.20.6 及更早 → {"pack":{"pack_format":41,…}}
            //   · 26.3 等新版本 → {"pack":{"min_format":121,"max_format":121,…}}（**没有** pack_format 键）
            //   用错写法游戏会直接不认这个数据包。
            String mcmeta;
            if (packInfo.newPackFormatStyle) {
                int maxFormat = packInfo.packFormatMax > 0 ? packInfo.packFormatMax : packFormat;
                mcmeta = "{\"pack\":{\"description\":\"" + description + "\",\"min_format\":" + packFormat
                        + ",\"max_format\":" + maxFormat + "}}";
            } else {
                mcmeta = "{\"pack\":{\"pack_format\":" + packFormat + ",\"description\":\"" + description + "\"}}";
            }
            writeEntry(zos, "pack.mcmeta", mcmeta);

            for (int i = 0; i < funcs.size(); i++) {
                Func func = funcs.get(i);
                String funcName = LabUtils.sanitizeFileName(func.nameEdit.getText().toString().trim());
                if (funcName.isEmpty()) {
                    funcName = "function" + (i + 1);
                    autoNamed++;
                }
                if (firstFuncName.isEmpty()) {
                    firstFuncName = funcName;
                }
                StringBuilder content = new StringBuilder();
                String[] lines = func.cmdEdit.getText().toString().split("\n", -1);
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    }
                    if (!Character.isLetter(trimmed.charAt(0))) {
                        suspicious++;
                    }
                    content.append(trimmed).append("\n");
                }
                writeEntry(zos, "data/" + namespace + "/" + functionDir + "/" + funcName + ".mcfunction",
                        content.toString());
            }
        } catch (Throwable e) {
            LabUtils.toast(getContext(), getContext().getString(R.string.lab_dp_toast_fail) + e.getMessage());
            return;
        }
        if (autoNamed > 0) {
            LabUtils.toast(getContext(), getContext().getString(R.string.lab_dp_toast_func_empty));
        }
        showGuide(outFile, namespace, firstFuncName.isEmpty() ? "main" : firstFuncName,
                functionDir, packFormat, suspicious);
    }

    /**
     * ★ 1.4.3 新增：导出后的「怎么用」引导。
     * 用户原话：「不会制作数据包的人在那里制作，等于盲人摸象」——
     * 所以这里把「放哪、输什么命令」全部按用户填的名字**动态生成**出来，可一键复制。
     */
    private void showGuide(final File outFile, final String namespace, final String funcName,
                           final String functionDir, final int packFormat, final int suspicious) {
        StringBuilder body = new StringBuilder();
        body.append(getContext().getString(R.string.lab_dp_guide_file, outFile.getAbsolutePath())).append("\n\n");
        if (packInfo.ok && autoPackFormat > 0 && packFormat == autoPackFormat) {
            body.append(getContext().getString(R.string.lab_dp_guide_pack_format, packFormat, packInfo.versionName)).append("\n\n");
        }
        body.append(getContext().getString(R.string.lab_dp_guide_steps, namespace, funcName, functionDir));
        if (suspicious > 0) {
            body.append("\n\n（" + suspicious + " 行命令格式可疑，已原样保留）");
        }
        final String functionCommand = "/function " + namespace + ":" + funcName;
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle(R.string.lab_dp_guide_title);
        builder.setMessage(body.toString());
        builder.setPositiveButton(R.string.lab_dp_guide_copy_cmd, new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                LabUtils.copyToClipboard(getContext(), functionCommand);
                LabUtils.toast(getContext(), getContext().getString(R.string.lab_dp_guide_copy_cmd));
            }
        });
        builder.setNegativeButton(R.string.lab_dp_guide_ok, null);
        try {
            builder.show();
        } catch (Throwable t) {
            // 万一弹窗失败，至少别让用户以为没导出成功
            LabUtils.toast(getContext(), getContext().getString(R.string.lab_dp_toast_done) + outFile.getAbsolutePath());
        }
    }

    private void writeEntry(ZipOutputStream zos, String path, String content) throws Exception {
        ZipEntry entry = new ZipEntry(path);
        zos.putNextEntry(entry);
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private int dp(int value) {
        return (int) (value * getContext().getResources().getDisplayMetrics().density + 0.5f);
    }
}