package com.qcl.launcher.launcher.dialogs.lab;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.steveice10.opennbt.NBTIO;
import com.github.steveice10.opennbt.tag.builtin.ByteArrayTag;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.ShortTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.qcl.launcher.R;
import com.qcl.launcher.launcher.MainActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * 投影工坊：用参数生成球体/圆柱/立方体/像素画，等轴俯视分层预览，导出为 .schematic（MCEdit NBT+gzip）。
 */
public class LabSchematicStudioDialog extends Dialog {

    /** 常用方块名 -> 数字 id 映射（≤ 255）。 */
    private static final Map<String, Integer> BLOCKS = new LinkedHashMap<>();
    /** 方块 id -> 预览颜色。 */
    private static final Map<Integer, Integer> BLOCK_COLORS = new HashMap<>();

    static {
        BLOCKS.put("石头", 1);
        BLOCKS.put("草方块", 2);
        BLOCKS.put("泥土", 3);
        BLOCKS.put("圆石", 4);
        BLOCKS.put("木板", 5);
        BLOCKS.put("基岩", 7);
        BLOCKS.put("水", 9);
        BLOCKS.put("岩浆", 11);
        BLOCKS.put("沙", 12);
        BLOCKS.put("沙砾", 13);
        BLOCKS.put("金矿石", 14);
        BLOCKS.put("铁矿石", 15);
        BLOCKS.put("煤矿石", 16);
        BLOCKS.put("原木", 17);
        BLOCKS.put("树叶", 18);
        BLOCKS.put("玻璃", 20);
        BLOCKS.put("青金石矿石", 21);
        BLOCKS.put("砂岩", 24);
        BLOCKS.put("羊毛", 35);
        BLOCKS.put("黄花", 37);
        BLOCKS.put("红花", 38);
        BLOCKS.put("蘑菇", 39);
        BLOCKS.put("金块", 41);
        BLOCKS.put("铁块", 42);
        BLOCKS.put("石台阶", 44);
        BLOCKS.put("砖块", 45);
        BLOCKS.put("TNT", 46);
        BLOCKS.put("书架", 47);
        BLOCKS.put("苔石", 48);
        BLOCKS.put("黑曜石", 49);
        BLOCKS.put("火把", 50);
        BLOCKS.put("箱子", 54);
        BLOCKS.put("钻石矿石", 56);
        BLOCKS.put("钻石块", 57);
        BLOCKS.put("工作台", 58);
        BLOCKS.put("熔炉", 61);
        BLOCKS.put("梯子", 65);
        BLOCKS.put("铁轨", 66);
        BLOCKS.put("红石矿石", 73);
        BLOCKS.put("冰", 79);
        BLOCKS.put("雪块", 80);
        BLOCKS.put("南瓜", 86);
        BLOCKS.put("下界岩", 87);
        BLOCKS.put("荧石", 89);
        BLOCKS.put("石英块", 155);

        BLOCK_COLORS.put(1, 0xFF808080);
        BLOCK_COLORS.put(2, 0xFF5FBF4A);
        BLOCK_COLORS.put(3, 0xFF8B5A2B);
        BLOCK_COLORS.put(4, 0xFF6E6E6E);
        BLOCK_COLORS.put(5, 0xFFB08C4F);
        BLOCK_COLORS.put(7, 0xFF555555);
        BLOCK_COLORS.put(9, 0xFF3A6FE0);
        BLOCK_COLORS.put(11, 0xFFE05A10);
        BLOCK_COLORS.put(12, 0xFFE3D9A6);
        BLOCK_COLORS.put(13, 0xFF9A9A9A);
        BLOCK_COLORS.put(14, 0xFFE8C84A);
        BLOCK_COLORS.put(15, 0xFFC9A98C);
        BLOCK_COLORS.put(16, 0xFF3A3A3A);
        BLOCK_COLORS.put(17, 0xFF9A6B3F);
        BLOCK_COLORS.put(18, 0xFF3F8F3F);
        BLOCK_COLORS.put(20, 0xC8CDE8FF);
        BLOCK_COLORS.put(21, 0xFF2A4FA8);
        BLOCK_COLORS.put(24, 0xFFD8C88A);
        BLOCK_COLORS.put(35, 0xFFE8E8E8);
        BLOCK_COLORS.put(37, 0xFFF0E24A);
        BLOCK_COLORS.put(38, 0xFFD93A3A);
        BLOCK_COLORS.put(39, 0xFFB08070);
        BLOCK_COLORS.put(41, 0xFFF2D340);
        BLOCK_COLORS.put(42, 0xFFD8D8D8);
        BLOCK_COLORS.put(44, 0xFFA0A0A0);
        BLOCK_COLORS.put(45, 0xFFA65A45);
        BLOCK_COLORS.put(46, 0xFFC03030);
        BLOCK_COLORS.put(47, 0xFFA5793F);
        BLOCK_COLORS.put(48, 0xFF6E8A5A);
        BLOCK_COLORS.put(49, 0xFF2A1F3A);
        BLOCK_COLORS.put(50, 0xFFF2B93A);
        BLOCK_COLORS.put(54, 0xFF9A6B3F);
        BLOCK_COLORS.put(56, 0xFF4FD8D8);
        BLOCK_COLORS.put(57, 0xFF5FE0D8);
        BLOCK_COLORS.put(58, 0xFFA5793F);
        BLOCK_COLORS.put(61, 0xFF7A7A7A);
        BLOCK_COLORS.put(65, 0xFFB08C4F);
        BLOCK_COLORS.put(66, 0xFFC0C0C0);
        BLOCK_COLORS.put(73, 0xFFB03030);
        BLOCK_COLORS.put(79, 0xB0B0D8FF);
        BLOCK_COLORS.put(80, 0xFFF4F4F4);
        BLOCK_COLORS.put(86, 0xFFE08A20);
        BLOCK_COLORS.put(87, 0xFF703030);
        BLOCK_COLORS.put(89, 0xFFF0D060);
        BLOCK_COLORS.put(155, 0xFFEDE8DC);
    }

    private final MainActivity activity;
    private Spinner typeSpinner;
    private EditText nameEdit;
    private EditText blockEdit;
    private LinearLayout paramsContainer;
    private FrameLayout previewContainer;
    private TextView infoText;
    private TextView layerText;

    private SchematicPreviewView previewView;

    private EditText fieldA;
    private EditText fieldB;
    private EditText fieldC;
    private CheckBox hollowCheck;
    private LinearLayout pixelGridContainer;
    private boolean[][] pixelCells;
    private View[][] pixelCellViews;
    private int pixelW;
    private int pixelL;

    private int width;
    private int height;
    private int length;
    private byte[] blocks;
    private byte[] data;
    private int layer;

    public LabSchematicStudioDialog(MainActivity activity) {
        super(activity);
        this.activity = activity;
        setContentView(R.layout.dialog_lab_schematic_studio);
        init();
        LabUtils.setupDialogWindow(this);
    }

    private void init() {
        this.typeSpinner = findViewById(R.id.lab_ss_type);
        this.nameEdit = findViewById(R.id.lab_ss_name);
        this.blockEdit = findViewById(R.id.lab_ss_block);
        this.paramsContainer = findViewById(R.id.lab_ss_params);
        this.previewContainer = findViewById(R.id.lab_ss_preview_container);
        this.infoText = findViewById(R.id.lab_ss_info);
        this.layerText = findViewById(R.id.lab_ss_layer_text);

        String[] types = {
                getContext().getString(R.string.lab_ss_type_sphere),
                getContext().getString(R.string.lab_ss_type_cylinder),
                getContext().getString(R.string.lab_ss_type_cube),
                getContext().getString(R.string.lab_ss_type_pixel)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, types);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);
        typeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                buildParams();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        blockEdit.setText("石头");
        previewView = new SchematicPreviewView(getContext());
        previewContainer.addView(previewView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        findViewById(R.id.lab_ss_gen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generate();
            }
        });
        findViewById(R.id.lab_ss_prev_layer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeLayer(-1);
            }
        });
        findViewById(R.id.lab_ss_next_layer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeLayer(1);
            }
        });
        findViewById(R.id.lab_ss_export_schematic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportSchematic();
            }
        });
        findViewById(R.id.lab_ss_export_litematic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LabUtils.toast(getContext(), getContext().getString(R.string.lab_ss_hint_litematic));
            }
        });
        findViewById(R.id.lab_ss_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        buildParams();
    }

    // ------------------------------------------------------------------ 参数区域

    private void buildParams() {
        paramsContainer.removeAllViews();
        fieldA = null;
        fieldB = null;
        fieldC = null;
        hollowCheck = null;
        pixelGridContainer = null;
        pixelCells = null;
        pixelCellViews = null;
        pixelW = 0;
        pixelL = 0;

        int type = typeSpinner.getSelectedItemPosition();
        if (type == 0) {
            fieldA = addField(R.string.lab_ss_radius, "8");
        } else if (type == 1) {
            fieldA = addField(R.string.lab_ss_radius, "6");
            fieldB = addField(R.string.lab_ss_height, "12");
        } else if (type == 2) {
            fieldA = addField(R.string.lab_ss_width, "7");
            fieldB = addField(R.string.lab_ss_length, "7");
            fieldC = addField(R.string.lab_ss_height, "7");
            hollowCheck = new CheckBox(getContext());
            hollowCheck.setText(R.string.lab_ss_hollow);
            hollowCheck.setTextSize(13);
            hollowCheck.setTextColor(Color.BLACK);
            hollowCheck.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            paramsContainer.addView(hollowCheck);
        } else {
            fieldA = addField(R.string.lab_ss_width, "8");
            fieldB = addField(R.string.lab_ss_length, "8");
            Button pixelButton = new Button(getContext());
            pixelButton.setText(R.string.lab_ss_build_pixel);
            pixelButton.setAllCaps(false);
            pixelButton.setTextSize(12);
            pixelButton.setTextColor(Color.parseColor("#0E9384"));
            pixelButton.setBackgroundResource(R.drawable.launcher_button_parent);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            blp.setMargins(0, dp(6), 0, 0);
            pixelButton.setLayoutParams(blp);
            pixelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    buildPixelGrid();
                }
            });
            paramsContainer.addView(pixelButton);

            TextView hint = new TextView(getContext());
            hint.setText(R.string.lab_ss_pixel_hint);
            hint.setTextSize(11);
            hint.setTextColor(Color.parseColor("#6E6E6E"));
            hint.setPadding(0, dp(4), 0, 0);
            paramsContainer.addView(hint);

            pixelGridContainer = new LinearLayout(getContext());
            pixelGridContainer.setOrientation(LinearLayout.VERTICAL);
            pixelGridContainer.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            paramsContainer.addView(pixelGridContainer);
        }
    }

    private EditText addField(int labelRes, String def) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.setMargins(0, dp(4), 0, 0);
        row.setLayoutParams(rlp);

        TextView label = new TextView(getContext());
        label.setText(labelRes);
        label.setTextSize(13);
        label.setTextColor(Color.BLACK);
        label.setLayoutParams(new LinearLayout.LayoutParams(dp(80), ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(label);

        EditText edit = new EditText(getContext());
        edit.setSingleLine(true);
        edit.setTextSize(13);
        edit.setText(def);
        edit.setBackgroundColor(Color.parseColor("#0D000000"));
        edit.setPadding(dp(6), dp(4), dp(6), dp(4));
        edit.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(edit);

        paramsContainer.addView(row);
        return edit;
    }

    private void buildPixelGrid() {
        int w = clamp(parseInt(fieldA, 8), 1, 32);
        int l = clamp(parseInt(fieldB, 8), 1, 32);
        pixelW = w;
        pixelL = l;
        pixelCells = new boolean[l][w];
        pixelCellViews = new View[l][w];
        if (pixelGridContainer == null) {
            return;
        }
        pixelGridContainer.removeAllViews();
        int blockId = resolveBlock();
        int onColor = colorOf(blockId);
        for (int z = 0; z < l; z++) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            for (int x = 0; x < w; x++) {
                final int fx = x;
                final int fz = z;
                pixelCells[z][x] = true;
                TextView cell = new TextView(getContext());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(20), dp(20));
                lp.setMargins(dp(1), dp(1), dp(1), dp(1));
                cell.setLayoutParams(lp);
                cell.setBackgroundColor(onColor);
                cell.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        pixelCells[fz][fx] = !pixelCells[fz][fx];
                        v.setBackgroundColor(pixelCells[fz][fx] ? colorOf(resolveBlock()) : Color.parseColor("#33000000"));
                    }
                });
                pixelCellViews[z][x] = cell;
                row.addView(cell);
            }
            pixelGridContainer.addView(row);
        }
    }

    // ------------------------------------------------------------------ 生成与预览

    private void generate() {
        int type = typeSpinner.getSelectedItemPosition();
        int blockId = resolveBlock();
        if (type == 0) {
            int r = clamp(parseInt(fieldA, 8), 1, 24);
            genSphere(r, blockId);
        } else if (type == 1) {
            int r = clamp(parseInt(fieldA, 6), 1, 24);
            int h = clamp(parseInt(fieldB, 12), 1, 64);
            genCylinder(r, h, blockId);
        } else if (type == 2) {
            int w = clamp(parseInt(fieldA, 7), 1, 64);
            int l = clamp(parseInt(fieldB, 7), 1, 64);
            int h = clamp(parseInt(fieldC, 7), 1, 64);
            genCube(w, h, l, hollowCheck != null && hollowCheck.isChecked(), blockId);
        } else {
            if (pixelCells == null || pixelW != clamp(parseInt(fieldA, 8), 1, 32)
                    || pixelL != clamp(parseInt(fieldB, 8), 1, 32)) {
                LabUtils.toast(getContext(), getContext().getString(R.string.lab_ss_pixel_hint));
                return;
            }
            genPixel(blockId);
        }
        layer = Math.max(0, height / 2);
        updateLayerText();
        previewView.invalidate();
        infoText.setText("尺寸 " + width + " x " + height + " x " + length
                + "（宽 X / 高 Y / 长 Z）");
    }

    private void genSphere(int r, int id) {
        int size = r * 2 + 1;
        newVolume(size, size, size);
        for (int y = 0; y < size; y++) {
            for (int z = 0; z < size; z++) {
                for (int x = 0; x < size; x++) {
                    int dx = x - r;
                    int dy = y - r;
                    int dz = z - r;
                    if (dx * dx + dy * dy + dz * dz <= r * r) {
                        set(x, y, z, id);
                    }
                }
            }
        }
    }

    private void genCylinder(int r, int h, int id) {
        int size = r * 2 + 1;
        newVolume(size, h, size);
        for (int y = 0; y < h; y++) {
            for (int z = 0; z < size; z++) {
                for (int x = 0; x < size; x++) {
                    int dx = x - r;
                    int dz = z - r;
                    if (dx * dx + dz * dz <= r * r) {
                        set(x, y, z, id);
                    }
                }
            }
        }
    }

    private void genCube(int w, int h, int l, boolean hollow, int id) {
        newVolume(w, h, l);
        for (int y = 0; y < h; y++) {
            for (int z = 0; z < l; z++) {
                for (int x = 0; x < w; x++) {
                    if (hollow && !(x == 0 || x == w - 1 || y == 0 || y == h - 1 || z == 0 || z == l - 1)) {
                        continue;
                    }
                    set(x, y, z, id);
                }
            }
        }
    }

    private void genPixel(int id) {
        newVolume(pixelW, 1, pixelL);
        for (int z = 0; z < pixelL; z++) {
            for (int x = 0; x < pixelW; x++) {
                if (pixelCells[z][x]) {
                    set(x, 0, z, id);
                }
            }
        }
    }

    private void changeLayer(int delta) {
        if (blocks == null) {
            return;
        }
        int next = layer + delta;
        if (next < 0 || next >= height) {
            return;
        }
        layer = next;
        updateLayerText();
        previewView.invalidate();
    }

    private void updateLayerText() {
        layerText.setText(getContext().getString(R.string.lab_ss_layer) + " " + (layer + 1) + "/" + height);
    }

    private void newVolume(int w, int h, int l) {
        this.width = w;
        this.height = h;
        this.length = l;
        this.blocks = new byte[w * h * l];
        this.data = new byte[w * h * l];
    }

    private void set(int x, int y, int z, int id) {
        if (x < 0 || y < 0 || z < 0 || x >= width || y >= height || z >= length) {
            return;
        }
        blocks[(y * length + z) * width + x] = (byte) id;
    }

    private int colorOf(int id) {
        Integer color = BLOCK_COLORS.get(id);
        return color == null ? 0xFF888888 : color;
    }

    private int resolveBlock() {
        String name = blockEdit.getText().toString().trim();
        if (name.isEmpty()) {
            return 1;
        }
        Integer id = BLOCKS.get(name);
        if (id == null) {
            LabUtils.toast(getContext(), getContext().getString(R.string.lab_ss_toast_block_unknown));
            return 1;
        }
        return id;
    }

    // ------------------------------------------------------------------ 导出

    private void exportSchematic() {
        if (blocks == null) {
            LabUtils.toast(getContext(), getContext().getString(R.string.lab_ss_toast_empty));
            return;
        }
        String name = nameEdit.getText().toString().trim();
        if (name.isEmpty()) {
            name = "schematic_" + System.currentTimeMillis();
        }
        File dir = LabUtils.getSchematicsDir(activity);
        File outFile = new File(dir, LabUtils.sanitizeFileName(name) + ".schematic");
        try {
            CompoundTag root = new CompoundTag("Schematic");
            root.put(new ShortTag("Width", (short) width));
            root.put(new ShortTag("Height", (short) height));
            root.put(new ShortTag("Length", (short) length));
            root.put(new StringTag("Materials", "Alpha"));
            root.put(new ByteArrayTag("Blocks", blocks));
            root.put(new ByteArrayTag("Data", data));
            root.put(new ListTag("Entities", CompoundTag.class));
            root.put(new ListTag("TileEntities", CompoundTag.class));
            try (GZIPOutputStream os = new GZIPOutputStream(new FileOutputStream(outFile))) {
                NBTIO.writeTag(os, root);
            }
        } catch (Throwable e) {
            LabUtils.toast(getContext(), getContext().getString(R.string.lab_ss_toast_fail) + e.getMessage());
            return;
        }
        LabUtils.toast(getContext(), getContext().getString(R.string.lab_ss_toast_done) + outFile.getAbsolutePath());
    }

    // ------------------------------------------------------------------ 预览 View

    /** 俯视分层预览：按当前 Y 层绘制 X-Z 平面的方块俯视图。 */
    private class SchematicPreviewView extends View {
        private final Paint paint = new Paint();

        SchematicPreviewView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (blocks == null || width == 0 || length == 0) {
                return;
            }
            float cell = Math.min((float) getWidth() / width, (float) getHeight() / length);
            float ox = (getWidth() - width * cell) / 2f;
            float oy = (getHeight() - length * cell) / 2f;
            paint.setStyle(Paint.Style.FILL);
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    int id = blocks[(layer * length + z) * width + x] & 0xFF;
                    float left = ox + x * cell;
                    float top = oy + z * cell;
                    paint.setColor(id == 0 ? Color.parseColor("#1AFFFFFF") : colorOf(id));
                    canvas.drawRect(left, top, left + cell, top + cell, paint);
                }
            }
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1f);
            paint.setColor(Color.parseColor("#30000000"));
            for (int x = 0; x <= width; x++) {
                canvas.drawLine(ox + x * cell, oy, ox + x * cell, oy + length * cell, paint);
            }
            for (int z = 0; z <= length; z++) {
                canvas.drawLine(ox, oy + z * cell, ox + width * cell, oy + z * cell, paint);
            }
        }
    }

    private int parseInt(EditText edit, int def) {
        if (edit == null) {
            return def;
        }
        try {
            return Integer.parseInt(edit.getText().toString().trim());
        } catch (Throwable e) {
            return def;
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int dp(int value) {
        return (int) (value * getContext().getResources().getDisplayMetrics().density + 0.5f);
    }
}