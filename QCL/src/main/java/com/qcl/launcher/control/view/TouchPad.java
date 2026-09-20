/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.annotation.SuppressLint
 *  android.content.Context
 *  android.content.res.Resources
 *  android.graphics.Bitmap
 *  android.graphics.BitmapFactory
 *  android.graphics.Canvas
 *  android.graphics.Paint
 *  android.graphics.Rect
 *  android.os.Handler
 *  android.util.Log
 *  android.view.MotionEvent
 *  android.view.View
 */
package com.qcl.launcher.control.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import com.qcl.launcher.control.InputBridge;
import com.qcl.launcher.control.MenuHelper;
import com.qcl.launcher.launcher.launch.MCOptionUtils;
import com.qcl.launcher.utils.convert.ConvertUtils;
import com.qcl.launcher.utils.io.SocketServer;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Objects;

import com.qcl.launcher.R;
@SuppressLint(value={"ViewConstructor"})
public class TouchPad
extends View {
    private String rayTraceResultType;
    private static final String RAYTRACE_RESULT_TYPE_UNKNOWN = "UNKNOWN";
    private static final String RAYTRACE_RESULT_TYPE_MISS = "MISS";
    private static final String RAYTRACE_RESULT_TYPE_BLOCK = "BLOCK";
    private static final String RAYTRACE_RESULT_TYPE_ENTITY = "ENTITY";
    private final int launcher;
    private final int screenWidth;
    private final int screenHeight;
    private final MenuHelper menuHelper;
    private final Bitmap bitmap;
    private float startCursorX;
    private float startCursorY;
    private float downX;
    private float downY;
    private float initialX;
    private float initialY;
    // ★ 09-20 照搬 FCL：逐帧位移累积用（FCL 里是每帧把 downX/downY 推进到当前点）
    private float lastMoveX;
    private float lastMoveY;
    private long downTime;
    private int pointerID;
    private final Handler handler = new Handler();
    private final Runnable runnable = new Runnable(){

        @Override
        public void run() {
            if (((TouchPad)TouchPad.this).menuHelper.gameMenuSetting.enableTouch) {
                if (Objects.equals(TouchPad.this.rayTraceResultType, TouchPad.RAYTRACE_RESULT_TYPE_BLOCK)) {
                    InputBridge.sendMouseEvent(TouchPad.this.launcher, 0, true);
                } else if (Objects.equals(TouchPad.this.rayTraceResultType, TouchPad.RAYTRACE_RESULT_TYPE_ENTITY) || Objects.equals(TouchPad.this.rayTraceResultType, TouchPad.RAYTRACE_RESULT_TYPE_MISS)) {
                    InputBridge.sendMouseEvent(TouchPad.this.launcher, 1, true);
                } else {
                    if (((TouchPad)TouchPad.this).menuHelper.gameMenuSetting.touchMode == 0) {
                        InputBridge.sendMouseEvent(TouchPad.this.launcher, 0, true);
                    }
                    if (((TouchPad)TouchPad.this).menuHelper.gameMenuSetting.touchMode == 1) {
                        InputBridge.sendMouseEvent(TouchPad.this.launcher, 1, true);
                    }
                }
            }
        }
    };
    private final Handler throwHandler = new Handler();
    private final Runnable throwRunnable = new Runnable(){

        @Override
        public void run() {
            InputBridge.sendEvent(TouchPad.this.launcher, 81, true);
            InputBridge.sendEvent(TouchPad.this.launcher, 81, false);
        }
    };

    public TouchPad(Context context, int launcher, int screenWidth, int screenHeight, MenuHelper menuHelper) {
        super(context);
        this.launcher = launcher;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.menuHelper = menuHelper;
        SocketServer server = new SocketServer("127.0.0.1", 2332, (server1, msg) -> {
            this.handleRayTraceResult(msg);
            Log.i((String)"ReceiveRaytraceResultType", (String)Long.toString(System.currentTimeMillis()));
        });
        server.start();
        this.bitmap = BitmapFactory.decodeResource((Resources)this.getContext().getResources(), (int)R.drawable.ic_cursor);
    }

    private void handleRayTraceResult(String msg) {
        switch (msg) {
            case "MISS": 
            case "BLOCK": 
            case "ENTITY": {
                this.rayTraceResultType = msg;
                break;
            }
            default: {
                this.rayTraceResultType = RAYTRACE_RESULT_TYPE_UNKNOWN;
            }
        }
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        this.setMeasuredDimension(this.screenWidth, this.screenHeight);
    }

    @SuppressLint(value={"DrawAllocation"})
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.menuHelper.gameCursorMode == 0) {
            Rect src = new Rect(0, 0, this.bitmap.getWidth(), this.bitmap.getHeight());
            Rect dst = new Rect((int)this.menuHelper.cursorX, (int)this.menuHelper.cursorY, (int)this.menuHelper.cursorX + ConvertUtils.dip2px(this.getContext(), this.menuHelper.gameMenuSetting.mouseSize), (int)this.menuHelper.cursorY + ConvertUtils.dip2px(this.getContext(), this.menuHelper.gameMenuSetting.mouseSize));
            canvas.drawBitmap(this.bitmap, src, dst, new Paint(1));
        }
        this.invalidate();
    }

    @SuppressLint(value={"ClickableViewAccessibility"})
    public boolean onTouchEvent(MotionEvent event) {
        if (this.menuHelper.gameCursorMode == 1 && event.getActionMasked() == 0) {
            Log.i((String)"StartGettingRaytraceResultType", (String)Long.toString(System.currentTimeMillis()));
            new Thread(() -> {
                try {
                    DatagramSocket socket = new DatagramSocket();
                    socket.connect(new InetSocketAddress("127.0.0.1", 2333));
                    byte[] data = "refresh".getBytes();
                    DatagramPacket packet = new DatagramPacket(data, data.length);
                    socket.send(packet);
                    socket.close();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
        if (event.getActionMasked() == 0) {
            this.downX = event.getX();
            this.downY = event.getY();
        }
        int guiScale = -1;
        if (this.menuHelper.gameDir != null) {
            MCOptionUtils.load(this.menuHelper.gameDir);
            String str = MCOptionUtils.get("guiScale");
            guiScale = str == null ? 0 : Integer.parseInt(str);
            int scale = (int)Math.max(Math.min((float)this.screenWidth * this.menuHelper.scaleFactor / 320.0f, (float)this.screenHeight * this.menuHelper.scaleFactor / 240.0f), 1.0f);
            if (scale < guiScale || guiScale == 0) {
                guiScale = scale;
            }
        }
        int inventoryWidth = 0;
        int inventoryHeight = 0;
        int slotWidth = 0;
        if (guiScale != -1) {
            inventoryWidth = (int)((float)(182 * guiScale) / this.menuHelper.scaleFactor);
            inventoryHeight = (int)((float)(22 * guiScale) / this.menuHelper.scaleFactor);
            slotWidth = (int)((float)(20 * guiScale) / this.menuHelper.scaleFactor);
        }
        if (this.menuHelper.gameCursorMode == 1 && this.downX >= (float)(this.getWidth() / 2 - inventoryWidth / 2) && this.downX <= (float)(this.getWidth() / 2 + inventoryWidth / 2) && this.downY >= (float)(this.getHeight() - inventoryHeight)) {
            int start = this.getWidth() / 2 - inventoryWidth / 2;
            switch (event.getActionMasked()) {
                case 0: {
                    this.initialX = event.getX();
                    this.initialY = event.getY();
                    this.downTime = System.currentTimeMillis();
                    if (event.getX() <= (float)(start + slotWidth)) {
                        InputBridge.sendEvent(this.launcher, 49, true);
                    }
                    if (event.getX() > (float)(start + slotWidth) && event.getX() <= (float)(start + 2 * slotWidth)) {
                        InputBridge.sendEvent(this.launcher, 50, true);
                    }
                    if (event.getX() > (float)(start + 2 * slotWidth) && event.getX() <= (float)(start + 3 * slotWidth)) {
                        InputBridge.sendEvent(this.launcher, 51, true);
                    }
                    if (event.getX() > (float)(start + 3 * slotWidth) && event.getX() <= (float)(start + 4 * slotWidth)) {
                        InputBridge.sendEvent(this.launcher, 52, true);
                    }
                    if (event.getX() > (float)(start + 4 * slotWidth) && event.getX() <= (float)(start + 5 * slotWidth)) {
                        InputBridge.sendEvent(this.launcher, 53, true);
                    }
                    if (event.getX() > (float)(start + 5 * slotWidth) && event.getX() <= (float)(start + 6 * slotWidth)) {
                        InputBridge.sendEvent(this.launcher, 54, true);
                    }
                    if (event.getX() > (float)(start + 6 * slotWidth) && event.getX() <= (float)(start + 7 * slotWidth)) {
                        InputBridge.sendEvent(this.launcher, 55, true);
                    }
                    if (event.getX() > (float)(start + 7 * slotWidth) && event.getX() <= (float)(start + 8 * slotWidth)) {
                        InputBridge.sendEvent(this.launcher, 56, true);
                    }
                    if (event.getX() > (float)(start + 8 * slotWidth)) {
                        InputBridge.sendEvent(this.launcher, 57, true);
                    }
                    this.throwHandler.postDelayed(this.throwRunnable, 800L);
                    break;
                }
                case 2: {
                    if (!(Math.abs(event.getX() - this.initialX) > 1.0f) && !(Math.abs(event.getY() - this.initialY) > 1.0f) || System.currentTimeMillis() - this.downTime >= 800L) break;
                    this.throwHandler.removeCallbacks(this.throwRunnable);
                    break;
                }
                case 1: {
                    this.throwHandler.removeCallbacks(this.throwRunnable);
                    InputBridge.sendEvent(this.launcher, 49, false);
                    InputBridge.sendEvent(this.launcher, 50, false);
                    InputBridge.sendEvent(this.launcher, 51, false);
                    InputBridge.sendEvent(this.launcher, 52, false);
                    InputBridge.sendEvent(this.launcher, 53, false);
                    InputBridge.sendEvent(this.launcher, 54, false);
                    InputBridge.sendEvent(this.launcher, 55, false);
                    InputBridge.sendEvent(this.launcher, 56, false);
                    InputBridge.sendEvent(this.launcher, 57, false);
                }
            }
        } else {
            if (this.menuHelper.gameMenuSetting.mouseMode == 0 && this.menuHelper.gameCursorMode == 0) {
                this.menuHelper.cursorX = event.getX();
                this.menuHelper.cursorY = event.getY();
                this.menuHelper.pointerX = event.getX();
                this.menuHelper.pointerY = event.getY();
                InputBridge.setPointer(this.launcher, (int)event.getX(), (int)event.getY());
            }
            switch (event.getActionMasked()) {
                case 0: {
                    this.initialX = event.getX();
                    this.initialY = event.getY();
                    this.lastMoveX = event.getX();
                    this.lastMoveY = event.getY();
                    this.downTime = System.currentTimeMillis();
                    this.pointerID = event.getPointerId(event.getActionIndex());
                    if (this.menuHelper.gameMenuSetting.mouseMode == 1 && this.menuHelper.gameCursorMode == 0) {
                        this.startCursorX = this.menuHelper.cursorX;
                        this.startCursorY = this.menuHelper.cursorY;
                    }
                    if (this.menuHelper.gameCursorMode == 1 && (!this.menuHelper.gameMenuSetting.disableHalfScreen || this.initialX > (float)(this.screenWidth >> 1))) {
                        this.handler.postDelayed(this.runnable, 400L);
                    }
                    if (this.menuHelper.gameMenuSetting.mouseMode != 0 || this.menuHelper.gameCursorMode != 0) break;
                    if (this.launcher == 1) {
                        InputBridge.sendMouseEvent(this.launcher, 0, true);
                    }
                    if (this.launcher != 2) break;
                    new Handler().postDelayed(() -> InputBridge.sendMouseEvent(this.launcher, 0, true), 20L);
                    break;
                }
                case 2: {
                    if (this.menuHelper.gameMenuSetting.mouseMode == 1 && this.menuHelper.gameCursorMode == 0) {
                        float targetX = this.startCursorX + (event.getX() - this.initialX) * this.menuHelper.gameMenuSetting.mouseSpeed < 0.0f ? 0.0f : (this.startCursorX + (event.getX() - this.initialX) * this.menuHelper.gameMenuSetting.mouseSpeed > (float)this.screenWidth ? (float)this.screenWidth : this.startCursorX + (event.getX() - this.initialX) * this.menuHelper.gameMenuSetting.mouseSpeed);
                        float targetY = this.startCursorY + (event.getY() - this.initialY) * this.menuHelper.gameMenuSetting.mouseSpeed < 0.0f ? 0.0f : (this.startCursorY + (event.getY() - this.initialY) * this.menuHelper.gameMenuSetting.mouseSpeed > (float)this.screenHeight ? (float)this.screenHeight : this.startCursorY + (event.getY() - this.initialY) * this.menuHelper.gameMenuSetting.mouseSpeed);
                        this.menuHelper.cursorX = targetX;
                        this.menuHelper.cursorY = targetY;
                        this.menuHelper.pointerX = targetX;
                        this.menuHelper.pointerY = targetY;
                        // FCL 同款：不乘 scaleFactor（FCL TouchPad 第193行 setPointer(targetX, targetY, POINTER_ID)）
                        InputBridge.setPointer(this.launcher, (int)targetX, (int)targetY);
                    }
                    if (this.menuHelper.gameCursorMode != 1 || this.menuHelper.gameMenuSetting.disableHalfScreen && !(this.initialX > (float)(this.screenWidth >> 1)) || event.getPointerId(event.getActionIndex()) != this.pointerID) break;
                    // ★★★ 2026-09-20 完全照搬 FCL TouchPad.onTouchEvent 的 grab 分支（FCL 第226-251行）：
                    //   FCL 用「本帧点 − 上一帧点」得到位移，累加到 DOWN 时记下的基准（getPointerX），
                    //   再 setPointer 投递**累积后的绝对坐标**；每帧把基准推进到当前点。
                    //   QCL 原实现调 viewManager.setGamePointer("1", true, dx, dy)，而该函数在
                    //   hold=true（手指还按着）时**直接 return，pointerX 永不推进** → 每次都拿同一个
                    //   旧 pointerX 加上「从 initialX 起算的绝对位移」重复投递 → MC 侧算出的 delta
                    //   恒定不变 → 视角纹丝不动（实测：滑动 800px 画面完全没变；且因为乘了
                    //   scaleFactor，投递值高达 3057 远超屏宽 1600，坐标完全离谱）。
                    int movePointerIndex = event.findPointerIndex(this.pointerID);
                    if (movePointerIndex != -1) {
                        float newX = event.getX(movePointerIndex);
                        float newY = event.getY(movePointerIndex);
                        float moveDeltaX = (newX - this.lastMoveX) * this.menuHelper.gameMenuSetting.mouseSpeed;
                        float moveDeltaY = (newY - this.lastMoveY) * this.menuHelper.gameMenuSetting.mouseSpeed;
                        this.menuHelper.pointerX += moveDeltaX;
                        this.menuHelper.pointerY += moveDeltaY;
                        this.menuHelper.currentX = this.menuHelper.pointerX;
                        this.menuHelper.currentY = this.menuHelper.pointerY;
                        this.lastMoveX = newX;
                        this.lastMoveY = newY;
                        // FCL 直接传未缩放坐标（gameMenu.getInput().setPointer(initialX + deltaX, ...)）
                        InputBridge.setPointer(this.launcher,
                                (int) this.menuHelper.pointerX,
                                (int) this.menuHelper.pointerY);
                    }
                    if (!(Math.abs(event.getX() - this.initialX) > 1.0f) && !(Math.abs(event.getY() - this.initialY) > 1.0f) || System.currentTimeMillis() - this.downTime >= 400L) break;
                    this.handler.removeCallbacks(this.runnable);
                    break;
                }
                case 1: 
                case 3: 
                case 6: {
                    if (this.menuHelper.gameMenuSetting.mouseMode == 0 && this.menuHelper.gameCursorMode == 0) {
                        InputBridge.sendMouseEvent(this.launcher, 0, false);
                    }
                    if (event.getPointerId(event.getActionIndex()) != this.pointerID) break;
                    if (this.menuHelper.gameCursorMode == 1 && event.getPointerId(event.getActionIndex()) == this.pointerID && (!this.menuHelper.gameMenuSetting.disableHalfScreen || this.initialX > (float)(this.screenWidth >> 1))) {
                        // ★ 09-20：抬手时**不再**调 setGamePointer 累加位移 —— 位移已在 ACTION_MOVE 里
                        //   逐帧累积并投递完毕；这里重复投递会让视角在松手瞬间多跳一段。
                        this.handler.removeCallbacks(this.runnable);
                        if (Objects.equals(this.rayTraceResultType, RAYTRACE_RESULT_TYPE_BLOCK)) {
                            InputBridge.sendMouseEvent(this.launcher, 0, false);
                        } else if (Objects.equals(this.rayTraceResultType, RAYTRACE_RESULT_TYPE_ENTITY) || Objects.equals(this.rayTraceResultType, RAYTRACE_RESULT_TYPE_MISS)) {
                            InputBridge.sendMouseEvent(this.launcher, 1, false);
                        } else {
                            if (this.menuHelper.gameMenuSetting.touchMode == 0) {
                                InputBridge.sendMouseEvent(this.launcher, 0, false);
                            }
                            if (this.menuHelper.gameMenuSetting.touchMode == 1) {
                                InputBridge.sendMouseEvent(this.launcher, 1, false);
                            }
                        }
                    }
                    if (System.currentTimeMillis() - this.downTime > 200L || !(Math.abs(event.getX() - this.initialX) <= 10.0f) || !(Math.abs(event.getY() - this.initialY) <= 10.0f)) break;
                    if (this.menuHelper.gameMenuSetting.mouseMode == 1 && this.menuHelper.gameCursorMode == 0) {
                        InputBridge.sendMouseEvent(this.launcher, 0, true);
                        InputBridge.sendMouseEvent(this.launcher, 0, false);
                    }
                    if (this.menuHelper.gameCursorMode != 1 || event.getPointerId(event.getActionIndex()) != this.pointerID || this.menuHelper.gameMenuSetting.disableHalfScreen && !(this.initialX > (float)(this.screenWidth >> 1)) || !this.menuHelper.gameMenuSetting.enableTouch) break;
                    if (Objects.equals(this.rayTraceResultType, RAYTRACE_RESULT_TYPE_BLOCK)) {
                        InputBridge.sendMouseEvent(this.launcher, 1, true);
                        InputBridge.sendMouseEvent(this.launcher, 1, false);
                        break;
                    }
                    if (Objects.equals(this.rayTraceResultType, RAYTRACE_RESULT_TYPE_ENTITY) || Objects.equals(this.rayTraceResultType, RAYTRACE_RESULT_TYPE_MISS)) {
                        InputBridge.sendMouseEvent(this.launcher, 0, true);
                        InputBridge.sendMouseEvent(this.launcher, 0, false);
                        break;
                    }
                    if (this.menuHelper.gameMenuSetting.touchMode == 0) {
                        InputBridge.sendMouseEvent(this.launcher, 1, true);
                        InputBridge.sendMouseEvent(this.launcher, 1, false);
                    }
                    if (this.menuHelper.gameMenuSetting.touchMode != 1) break;
                    InputBridge.sendMouseEvent(this.launcher, 0, true);
                    InputBridge.sendMouseEvent(this.launcher, 0, false);
                }
            }
        }
        return true;
    }
}

