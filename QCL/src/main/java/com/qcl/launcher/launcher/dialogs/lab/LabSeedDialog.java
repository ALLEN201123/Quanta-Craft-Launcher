package com.qcl.launcher.launcher.dialogs.lab;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.qcl.launcher.R;

/**
 * 种子查询器：不进行本地地形计算，直接用浏览器打开 chunkbase 在线地图。
 */
public class LabSeedDialog extends Dialog {

    /** 展示用版本号。 */
    private static final String[] VERSION_LABELS = {
            "1.21", "1.20", "1.19", "1.18", "1.17", "1.16", "1.15", "1.14",
            "1.13", "1.12", "1.11", "1.10", "1.9", "1.8", "1.7"
    };
    /** chunkbase 使用的平台后缀。 */
    private static final String[] VERSION_PLATFORM = {
            "1_21", "1_20", "1_19", "1_18", "1_17", "1_16", "1_15", "1_14",
            "1_13", "1_12", "1_11", "1_10", "1_9", "1_8", "1_7"
    };
    private static final String[] DIMENSION_VALUES = {"overworld", "nether", "end"};

    private EditText seedInput;
    private Spinner versionSpinner;
    private Spinner dimensionSpinner;

    public LabSeedDialog(Context context) {
        super(context);
        setContentView(R.layout.dialog_lab_seed);
        init();
        LabUtils.setupDialogWindowWrap(this);
    }

    private void init() {
        this.seedInput = findViewById(R.id.lab_seed_input);
        this.versionSpinner = findViewById(R.id.lab_seed_version);
        this.dimensionSpinner = findViewById(R.id.lab_seed_dimension);

        ArrayAdapter<String> versionAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, VERSION_LABELS);
        versionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        versionSpinner.setAdapter(versionAdapter);
        versionSpinner.setSelection(1); // 默认 1.20

        String[] dimensions = {
                getContext().getString(R.string.lab_seed_dim_overworld),
                getContext().getString(R.string.lab_seed_dim_nether),
                getContext().getString(R.string.lab_seed_dim_end)
        };
        ArrayAdapter<String> dimensionAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, dimensions);
        dimensionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dimensionSpinner.setAdapter(dimensionAdapter);

        findViewById(R.id.lab_seed_query).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                query();
            }
        });
        findViewById(R.id.lab_seed_copy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String seed = seedInput.getText().toString().trim();
                if (seed.isEmpty()) {
                    LabUtils.toast(getContext(), getContext().getString(R.string.lab_seed_toast_empty));
                    return;
                }
                LabUtils.copyToClipboard(getContext(), seed);
                LabUtils.toast(getContext(), getContext().getString(R.string.lab_seed_toast_copied));
            }
        });
        findViewById(R.id.lab_seed_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    private void query() {
        String seed = seedInput.getText().toString().trim();
        if (seed.isEmpty()) {
            LabUtils.toast(getContext(), getContext().getString(R.string.lab_seed_toast_empty));
            return;
        }
        int versionIndex = versionSpinner.getSelectedItemPosition();
        if (versionIndex < 0 || versionIndex >= VERSION_PLATFORM.length) {
            versionIndex = 1;
        }
        int dimensionIndex = dimensionSpinner.getSelectedItemPosition();
        if (dimensionIndex < 0 || dimensionIndex >= DIMENSION_VALUES.length) {
            dimensionIndex = 0;
        }
        String url = "https://www.chunkbase.com/apps/seed-map#seed=" + Uri.encode(seed)
                + "&platform=java_" + VERSION_PLATFORM[versionIndex]
                + "&dimension=" + DIMENSION_VALUES[dimensionIndex];
        try {
            getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Throwable e) {
            LabUtils.toast(getContext(), getContext().getString(R.string.lab_seed_toast_empty));
        }
    }
}