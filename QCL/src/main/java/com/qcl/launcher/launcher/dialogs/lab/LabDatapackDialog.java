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

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, PACK_LABELS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        packSpinner.setAdapter(adapter);
        packSpinner.setSelection(PACK_DEFAULT_INDEX);

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
        int packFormat = PACK_VALUES[Math.max(0, packSpinner.getSelectedItemPosition())];

        File dir = LabUtils.getDatapacksDir(activity);
        File outFile = new File(dir, LabUtils.sanitizeFileName(packName) + ".zip");
        int suspicious = 0;
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outFile))) {
            String description = packName.replace("\\", "").replace("\"", "'");
            writeEntry(zos, "pack.mcmeta",
                    "{\"pack\":{\"pack_format\":" + packFormat + ",\"description\":\"" + description + "\"}}");

            for (int i = 0; i < funcs.size(); i++) {
                Func func = funcs.get(i);
                String funcName = LabUtils.sanitizeFileName(func.nameEdit.getText().toString().trim());
                if (funcName.isEmpty()) {
                    funcName = "function" + (i + 1);
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
                writeEntry(zos, "data/" + namespace + "/functions/" + funcName + ".mcfunction",
                        content.toString());
            }
        } catch (Throwable e) {
            LabUtils.toast(getContext(), getContext().getString(R.string.lab_dp_toast_fail) + e.getMessage());
            return;
        }
        String message = getContext().getString(R.string.lab_dp_toast_done) + outFile.getAbsolutePath();
        if (suspicious > 0) {
            message = message + "（" + suspicious + " 行命令格式可疑，已保留）";
        }
        LabUtils.toast(getContext(), message);
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