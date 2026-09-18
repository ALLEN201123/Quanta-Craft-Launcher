package com.qcl.launcher.utils.io;

import android.os.Handler;
import android.os.Message;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/* loaded from: classes2.dex */
public class SocketServer {
    private final String ip;
    private final Handler mHandler = new Handler() { // from class: com.qcl.launcher.utils.io.SocketServer.1
        @Override // android.os.Handler
        public void handleMessage(Message message) {
            super.handleMessage(message);
            SocketServer.this.mListener.onReceive(SocketServer.this, (String) message.obj);
        }
    };
    private final Listener mListener;
    private DatagramPacket packet;
    private final int port;
    private DatagramSocket socket;

    /* loaded from: classes2.dex */
    public interface Listener {
        void onReceive(SocketServer socketServer, String str);
    }

    public SocketServer(String str, int i, Listener listener) {
        this.mListener = listener;
        this.ip = str;
        this.port = i;
        try {
            this.packet = new DatagramPacket(new byte[1024], 1024);
            this.socket = new DatagramSocket(i, InetAddress.getByName(str));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start() {
        if (this.packet == null || this.socket == null) {
            return;
        }
        new Thread(new Runnable() { // from class: com.qcl.launcher.utils.io.SocketServer$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                SocketServer.this.m621lambda$start$0$comqcllauncherutilsioSocketServer();
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$start$0$com-qcl-launcher-utils-io-SocketServer, reason: not valid java name */
    public /* synthetic */ void m621lambda$start$0$comqcllauncherutilsioSocketServer() {
        while (true) {
            try {
                this.socket.receive(this.packet);
                String str = new String(this.packet.getData(), 0, this.packet.getLength());
                System.out.println(str);
                Message message = new Message();
                message.obj = str;
                this.mHandler.sendMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
    }

    public void send(String str) throws IOException {
        this.socket.connect(new InetSocketAddress(this.ip, this.port));
        byte[] bytes = str.getBytes();
        this.socket.send(new DatagramPacket(bytes, bytes.length));
    }

    public void stop() {
        this.socket.close();
    }
}
