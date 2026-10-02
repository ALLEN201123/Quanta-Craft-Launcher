package com.qcl.launcher.launcher.dialogs.lab;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.qcl.launcher.R;
import com.qcl.launcher.launcher.MainActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 投影资源：管理 schematics 目录下的投影文件（重命名/删除/复制路径），并支持从 http(s) 直链下载。
 */
public class LabSchematicDownloadDialog extends Dialog {

    private final MainActivity activity;
    private TextView dirText;
    private LinearLayout listContainer;
    private EditText urlInput;
    private ProgressBar progressBar;
    private TextView progressText;

    public LabSchematicDownloadDialog(MainActivity activity) {
        super(activity);
        this.activity = activity;
        setContentView(R.layout.dialog_lab_schematic_download);
        init();
        LabUtils.setupDialogWindow(this);
    }

    private void init() {
        this.dirText = findViewById(R.id.lab_sd_dir_text);
        this.listContainer = findViewById(R.id.lab_sd_list);
        this.urlInput = findViewById(R.id.lab_sd_url);
        this.progressBar = findViewById(R.id.lab_sd_progress);
        this.progressText = findViewById(R.id.lab_sd_progress_text);

        findViewById(R.id.lab_sd_refresh).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshList();
            }
        });
        findViewById(R.id.lab_sd_download_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDownload();
            }
        });
        findViewById(R.id.lab_sd_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        refreshList();
    }

    private void refreshList() {
        File dir = LabUtils.getSchematicsDir(activity);
        dirText.setText(getContext().getString(R.string.lab_sd_dir_label) + dir.getAbsolutePath());
        listContainer.removeAllViews();
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            TextView empty = new TextView(getContext());
            empty.setText(R.string.lab_sd_empty);
            empty.setTextSize(13);
            empty.setTextColor(Color.parseColor("#6E6E6E"));
            empty.setPadding(0, dp(6), 0, dp(6));
            listContainer.addView(empty);
            return;
        }
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                return Long.compare(b.lastModified(), a.lastModified());
            }
        });
        for (File file : files) {
            if (file.isFile()) {
                listContainer.addView(createFileRow(file));
            }
        }
    }

    private View createFileRow(final File file) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.qcl_button_gray);
        card.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.setMargins(0, dp(3), 0, dp(3));
        card.setLayoutParams(clp);

        TextView name = new TextView(getContext());
        name.setText(file.getName());
        name.setTextSize(13);
        name.setTextColor(Color.BLACK);
        name.setSingleLine(true);
        card.addView(name);

        TextView meta = new TextView(getContext());
        meta.setText(getContext().getString(R.string.lab_sd_size) + " " + LabUtils.humanSize(file.length())
                + "    " + getContext().getString(R.string.lab_sd_time) + " " + LabUtils.formatTime(file.lastModified()));
        meta.setTextSize(11);
        meta.setTextColor(Color.parseColor("#6E6E6E"));
        meta.setPadding(0, dp(2), 0, dp(4));
        card.addView(meta);

        LinearLayout actions = new LinearLayout(getContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        actions.addView(createActionButton(R.string.lab_sd_rename, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRenameDialog(file);
            }
        }));
        actions.addView(createActionButton(R.string.lab_sd_copy_path, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LabUtils.copyToClipboard(getContext(), file.getAbsolutePath());
                LabUtils.toast(getContext(), getContext().getString(R.string.lab_sd_toast_copied));
            }
        }));
        actions.addView(createActionButton(R.string.lab_sd_delete, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteDialog(file);
            }
        }));
        card.addView(actions);
        return card;
    }

    private Button createActionButton(int textRes, View.OnClickListener listener) {
        Button button = new Button(getContext());
        button.setText(textRes);
        button.setAllCaps(false);
        button.setTextSize(11);
        button.setTextColor(Color.parseColor("#0E9384"));
        button.setBackgroundResource(R.drawable.launcher_button_parent);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(2), 0, dp(2), 0);
        button.setLayoutParams(lp);
        button.setOnClickListener(listener);
        return button;
    }

    private void showRenameDialog(final File file) {
        final EditText input = new EditText(getContext());
        input.setSingleLine(true);
        input.setText(file.getName());
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.lab_sd_rename_title)
                .setView(input)
                .setPositiveButton(R.string.lab_sd_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newName = LabUtils.sanitizeFileName(input.getText().toString().trim());
                        if (newName.isEmpty()) {
                            return;
                        }
                        File target = new File(file.getParentFile(), newName);
                        if (file.renameTo(target)) {
                            LabUtils.toast(getContext(), getContext().getString(R.string.lab_sd_toast_renamed));
                            refreshList();
                        }
                    }
                })
                .setNegativeButton(R.string.lab_sd_cancel, null)
                .show();
    }

    private void showDeleteDialog(final File file) {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.lab_sd_delete_title)
                .setMessage(file.getName())
                .setPositiveButton(R.string.lab_sd_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (file.delete()) {
                            LabUtils.toast(getContext(), getContext().getString(R.string.lab_sd_toast_deleted));
                            refreshList();
                        }
                    }
                })
                .setNegativeButton(R.string.lab_sd_cancel, null)
                .show();
    }

    private void startDownload() {
        final String url = urlInput.getText().toString().trim();
        if (url.isEmpty()) {
            LabUtils.toast(getContext(), getContext().getString(R.string.lab_sd_toast_url_empty));
            return;
        }
        String fileName = Uri.parse(url).getLastPathSegment();
        if (fileName == null || fileName.isEmpty()) {
            fileName = "download_" + System.currentTimeMillis();
        }
        fileName = LabUtils.sanitizeFileName(fileName);
        if (!fileName.contains(".")) {
            fileName = fileName + ".schematic";
        }
        final String finalName = fileName;

        progressBar.setVisibility(View.VISIBLE);
        progressText.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        progressText.setText(getContext().getString(R.string.lab_sd_toast_downloading));
        LabUtils.toast(getContext(), getContext().getString(R.string.lab_sd_toast_downloading));

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                final String message = getContext().getString(R.string.lab_sd_toast_fail) + e.getMessage();
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideProgress();
                        LabUtils.toast(getContext(), message);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (!response.isSuccessful()) {
                    onFailure(call, new IOException("HTTP " + response.code()));
                    return;
                }
                ResponseBody body = response.body();
                if (body == null) {
                    onFailure(call, new IOException("empty body"));
                    return;
                }
                long total = body.contentLength();
                File outFile = new File(LabUtils.getSchematicsDir(activity), finalName);
                long read = 0;
                try (InputStream is = body.byteStream(); FileOutputStream fos = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                        read += len;
                        final long current = read;
                        final long max = total;
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateProgress(current, max);
                            }
                        });
                    }
                } catch (final IOException e) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hideProgress();
                            LabUtils.toast(getContext(), getContext().getString(R.string.lab_sd_toast_fail) + e.getMessage());
                        }
                    });
                    return;
                }
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideProgress();
                        LabUtils.toast(getContext(), getContext().getString(R.string.lab_sd_toast_done) + outFile.getAbsolutePath());
                        refreshList();
                    }
                });
            }
        });
    }

    private void updateProgress(long current, long total) {
        if (total > 0) {
            progressBar.setProgress((int) (current * 100 / total));
            progressText.setText(LabUtils.humanSize(current) + " / " + LabUtils.humanSize(total));
        } else {
            progressText.setText(LabUtils.humanSize(current));
        }
    }

    private void hideProgress() {
        progressBar.setVisibility(View.GONE);
        progressText.setVisibility(View.GONE);
    }

    private int dp(int value) {
        return (int) (value * getContext().getResources().getDisplayMetrics().density + 0.5f);
    }
}