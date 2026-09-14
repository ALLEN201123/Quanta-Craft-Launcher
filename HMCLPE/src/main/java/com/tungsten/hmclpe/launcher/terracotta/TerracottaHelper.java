package com.tungsten.hmclpe.launcher.terracotta;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;

import net.burningtnt.terracotta.TerracottaAndroidAPI;

/**
 * QCL 版 Terracotta（陶瓷联机）包装：初始化原生后端、申请 VPN 权限、维护房间状态。
 *
 * <p>状态 JSON 由原生层给出，形如
 * {@code {"index":N,"state":"waiting|host_scanning|host_ok|guest_connecting|guest_ok|exception", ...}}；
 * 房主拿到 {@code room}（邀请码），访客拿到 {@code url}（在游戏里当服务器地址填）。
 * 这里用纯 Gson 解析，不依赖 FCL 自研的状态模型。
 *
 * <p>协议互通：Terracotta 原生层内部支持 Scaffolding / Terracotta Legacy / PCL2CE 三种房间码
 * （{@link TerracottaAndroidAPI#parseRoomCode(String)}），所以可以和 FCL、PCL2 等客户端互通。
 */
public final class TerracottaHelper {

    /** VPN 授权请求码 */
    public static final int REQUEST_VPN = 0x5151;

    private static volatile boolean initialized = false;
    private static TerracottaAndroidAPI.Metadata metadata;
    private static Thread poller;

    private static volatile int lastIndex = -1;
    private static volatile String state = "";
    private static volatile String room = "";
    private static volatile String url = "";
    private static volatile String exceptionType = "";

    private TerracottaHelper() {
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static TerracottaAndroidAPI.Metadata getMetadata() {
        return metadata;
    }

    public static String getState() {
        return state;
    }

    /** 房主邀请码 */
    public static String getInviteCode() {
        return room;
    }

    /** 访客要填进游戏的服务器地址 */
    public static String getServerAddress() {
        return url;
    }

    public static String getExceptionType() {
        return exceptionType;
    }

    /** 是否已进入可用状态（房主拿到邀请码 / 访客拿到地址） */
    public static boolean isReady() {
        return !room.isEmpty() || !url.isEmpty();
    }

    /** 初始化原生后端（幂等）。回调会要求启动 VPN 服务。 */
    public static void initialize(final Activity activity) {
        if (initialized) {
            return;
        }
        try {
            metadata = TerracottaAndroidAPI.initialize(activity.getApplicationContext(),
                    () -> activity.runOnUiThread(() -> startVpn(activity)));
            initialized = true;
            startPoller();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /** 房主：重置到等待状态，原生层会自动生成房间邀请码（room）。 */
    public static void host(Activity activity) {
        initialize(activity);
        try {
            TerracottaAndroidAPI.setWaiting();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /** 访客：填入邀请码加入房间，成功后状态里会带服务器地址（url）。 */
    public static boolean join(Activity activity, String code) {
        initialize(activity);
        try {
            TerracottaAndroidAPI.setWaiting();
            return TerracottaAndroidAPI.setGuesting(code == null ? "" : code.trim(), null, null);
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    /** 断开/退出房间：回到等待状态并停掉 VPN。 */
    public static void reset(Activity activity) {
        try {
            TerracottaAndroidAPI.setWaiting();
        } catch (Throwable ignored) {
        }
        if (activity != null) {
            try {
                activity.stopService(new Intent(activity, TerracottaVpnService.class)
                        .setAction(TerracottaVpnService.ACTION_STOP));
            } catch (Throwable ignored) {
            }
        }
        room = "";
        url = "";
        state = "";
    }

    /** 邀请码类型（SCAFFOLDING / TERRACOTTA_LEGACY / PCL2CE / UNKNOWN），用于提示互通情况。 */
    public static String describeRoomCode(String code) {
        try {
            TerracottaAndroidAPI.RoomType type = TerracottaAndroidAPI.parseRoomCode(code);
            return type == null ? "UNKNOWN" : type.name();
        } catch (Throwable t) {
            return "UNKNOWN";
        }
    }

    /** VPN 授权回调：{@code onActivityResult} 里调用。 */
    public static boolean onActivityResult(Activity activity, int requestCode) {
        if (requestCode == REQUEST_VPN) {
            startVpn(activity);
            return true;
        }
        return false;
    }

    private static void startVpn(Activity activity) {
        try {
            Intent prepare = VpnService.prepare(activity);
            if (prepare != null) {
                activity.startActivityForResult(prepare, REQUEST_VPN);
                return;
            }
            activity.startService(new Intent(activity, TerracottaVpnService.class)
                    .setAction(TerracottaVpnService.ACTION_START));
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static void startPoller() {
        if (poller != null) {
            return;
        }
        poller = new Thread(() -> {
            while (true) {
                try {
                    String json = TerracottaAndroidAPI.getState();
                    if (json != null && !json.isEmpty()) {
                        parse(json);
                    }
                    Thread.sleep(500);
                } catch (Throwable t) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }, "qcl-terracotta");
        poller.setDaemon(true);
        poller.start();
    }

    /** 解析状态 JSON：只取我们 UI 需要的字段，旧状态（index 更小）丢弃。 */
    private static void parse(String json) {
        try {
            com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            int index = obj.has("index") ? obj.get("index").getAsInt() : -1;
            if (index < lastIndex) {
                return;
            }
            lastIndex = index;
            state = obj.has("state") ? obj.get("state").getAsString() : "";
            room = obj.has("room") ? obj.get("room").getAsString() : "";
            url = obj.has("url") ? obj.get("url").getAsString() : "";
            exceptionType = obj.has("type") ? obj.get("type").getAsString() : "";
        } catch (Throwable ignored) {
        }
    }
}
