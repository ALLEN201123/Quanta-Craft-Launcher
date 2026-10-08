package com.qcl.launcher.launcher.launch.vulkan;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.qcl.launcher.R;

import java.util.List;

/**
 * ★ 1.5.0 新增：Vulkan 检测结果对话框（FCL VulkanCheckDialog 的纯 Java 版）。
 *
 * <p>展示：总体结论 → 各版本区间支持情况 → Vulkan 版本/硬件等级 → 逐项扩展/功能依赖。
 * 缺失的必需项标红。纯代码构建视图（不引新布局/不引新依赖）。
 */
public final class VulkanCheckDialog {

    /** 缺失条目的警示红（对齐 FCL 的 #E53935）。 */
    private static final int ERROR_COLOR = 0xFFE53935;

    private VulkanCheckDialog() {
    }

    /** 弹出检测结果。{@code onConfirm} 在关闭后回调，可为 null。 */
    public static void show(Context context, VulkanCapabilities caps, Runnable onConfirm) {
        VulkanSupport support = new VulkanSupport(caps);

        ScrollView scroll = new ScrollView(context);
        int pad = dp(context, 16);
        scroll.setPadding(pad, dp(context, 8), pad, dp(context, 8));

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);

        if (caps == null || !caps.isSupported()) {
            addText(context, content, context.getString(R.string.vulkan_check_failed), ERROR_COLOR, false);
        } else {
            // 总体结论
            List<VulkanSupport.Profile> profiles = support.profileSupport();
            String summary;
            switch (support.summary()) {
                case ALL_SUPPORTED:
                    summary = context.getString(R.string.vulkan_check_support_all,
                            support.firstSupportedSince() == null ? VulkanRequirement.MIN_MC_VERSION
                                    : support.firstSupportedSince());
                    break;
                case NONE_SUPPORTED:
                    summary = context.getString(R.string.vulkan_check_support_none);
                    break;
                default:
                    summary = context.getString(R.string.vulkan_check_support_partial);
                    break;
            }
            addText(context, content, summary, 0, false);

            // 版本区间（仅在"部分支持"时有意义）
            if (hasMixedSupport(profiles)) {
                addSectionTitle(context, content, context.getString(R.string.vulkan_check_versions));
                for (VulkanSupport.Profile p : profiles) {
                    String tag = context.getString(p.supported
                            ? R.string.vulkan_check_profile_support
                            : R.string.vulkan_check_profile_unsupport);
                    addText(context, content, p.versionRangeText() + "  " + tag,
                            p.supported ? 0 : ERROR_COLOR, true);
                }
            }

            // 版本号与硬件等级
            addText(context, content, context.getString(R.string.vulkan_check_version, caps.versionString()), 0, false);
            addText(context, content, context.getString(R.string.vulkan_check_level,
                    caps.hardwareLevel < 0 ? "?" : String.valueOf(caps.hardwareLevel)), 0, false);

            // 逐项依赖（针对 26.2 与 26.3 两个代表版本各列一次，避免歧义）
            addDependencySection(context, content, caps, support, VulkanRequirement.MIN_MC_VERSION);
            if (!"26.3".equals(VulkanRequirement.MIN_MC_VERSION)) {
                addDependencySection(context, content, caps, support, "26.3");
            }
        }

        new AlertDialog.Builder(context)
                .setTitle(R.string.vulkan_check_title)
                .setView(scroll)
                .setPositiveButton(R.string.vulkan_check_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (onConfirm != null) {
                            onConfirm.run();
                        }
                    }
                })
                .setCancelable(true)
                .show();
    }

    private static void addDependencySection(Context context, LinearLayout content, VulkanCapabilities caps,
                                             VulkanSupport support, String mcVersion) {
        addSectionTitle(context, content,
                context.getString(R.string.vulkan_check_extensions) + "  ·  MC " + mcVersion);
        List<VulkanSupport.Item> items = support.supportFor(mcVersion);
        for (VulkanSupport.Item it : items) {
            if (it.level == VulkanRequirement.Level.UNUSED) {
                continue;   // 该版本不用这一项，不展示
            }
            boolean required = it.level == VulkanRequirement.Level.REQUIRED;
            String name = it.supported
                    ? it.dependency.name
                    : context.getString(R.string.vulkan_check_item_name_missing, it.dependency.name);
            // 必需但缺失 → 标红；可选缺失 → 不标红（不影响启动）
            int color = (!it.supported && required) ? ERROR_COLOR : 0;
            addText(context, content, name + (required ? "" : "  (可选)"), color, true);
        }
    }

    private static boolean hasMixedSupport(List<VulkanSupport.Profile> profiles) {
        if (profiles.size() < 2) {
            return false;
        }
        boolean first = profiles.get(0).supported;
        for (VulkanSupport.Profile p : profiles) {
            if (p.supported != first) {
                return true;
            }
        }
        return false;
    }

    private static void addSectionTitle(Context context, LinearLayout content, String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        tv.setPadding(0, dp(context, 10), 0, dp(context, 2));
        content.addView(tv);
    }

    private static void addText(Context context, LinearLayout content, String text, int color, boolean indent) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        tv.setGravity(Gravity.START);
        tv.setPadding(indent ? dp(context, 12) : 0, dp(context, 2), 0, dp(context, 2));
        if (color != 0) {
            tv.setTextColor(color);
        }
        content.addView(tv);
    }

    private static int dp(Context context, int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                context.getResources().getDisplayMetrics());
    }

    /** 供外部拼「缺失项」文本用。 */
    public static String missingNames(VulkanSupport support, String mcVersion) {
        StringBuilder sb = new StringBuilder();
        for (VulkanSupport.Item it : support.missingRequired(mcVersion)) {
            if (sb.length() > 0) {
                sb.append("、");
            }
            sb.append(it.dependency.name);
        }
        return sb.length() == 0 ? "—" : sb.toString();
    }
}
