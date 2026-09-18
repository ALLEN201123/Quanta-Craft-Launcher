package net.kdt.pojavlaunch;

import android.content.Context;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.ref.WeakReference;

/* loaded from: classes2.dex */
public class Logger {
    private static Logger loggerSingleton = null;
    private static volatile boolean streamCaptureInstalled = false;
    private final File logFile;
    private WeakReference<eventLogListener> logListenerWeakReference;
    private PrintStream logStream;

    /* loaded from: classes2.dex */
    public interface eventLogListener {
        void onEventLogged(String str);
    }

    private Logger(Context context) {
        this(context.getExternalFilesDir("debug").getAbsolutePath() + "/pojav_latest_log.txt");
    }

    private Logger(String str) {
        this.logListenerWeakReference = null;
        File file = new File(str);
        this.logFile = file;
        file.delete();
        try {
            file.createNewFile();
            this.logStream = new PrintStream(file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Logger getInstance(Context context) {
        if (loggerSingleton == null) {
            synchronized (Logger.class) {
                if (loggerSingleton == null) {
                    loggerSingleton = new Logger(context);
                }
            }
        }
        return loggerSingleton;
    }

    public void appendToLog(String str) {
        if (shouldCensorLog(str)) {
            return;
        }
        appendToLogUnchecked(str);
    }

    public synchronized void appendToLogUnchecked(String str) {
        PrintStream printStream = this.logStream;
        if (printStream != null) {
            printStream.println(str);
        }
        notifyLogListener(str);
    }

    public void reset() {
        try {
            this.logFile.delete();
            this.logFile.createNewFile();
            this.logStream = new PrintStream(this.logFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        this.logStream.close();
    }

    private static boolean shouldCensorLog(String str) {
        return str.contains("Session ID is");
    }

    public void setLogListener(eventLogListener eventloglistener) {
        this.logListenerWeakReference = new WeakReference<>(eventloglistener);
    }

    public static synchronized void setStreamCapture(boolean z) {
        synchronized (Logger.class) {
            if (z) {
                if (!streamCaptureInstalled) {
                    streamCaptureInstalled = true;
                    try {
                        System.setOut(new PrintStream((OutputStream) new LoggerOutputStream(), true));
                        System.setErr(new PrintStream((OutputStream) new LoggerOutputStream(), true));
                    } catch (Throwable unused) {
                    }
                }
            }
        }
    }

    /* loaded from: classes2.dex */
    private static final class LoggerOutputStream extends OutputStream {
        private final StringBuilder pending;

        private LoggerOutputStream() {
            this.pending = new StringBuilder();
        }

        @Override // java.io.OutputStream
        public synchronized void write(int i) {
            try {
                if (i == 10) {
                    flushPending();
                } else if (i != 13) {
                    this.pending.append((char) i);
                }
            } catch (Throwable th) {
                throw th;
            }
        }

        @Override // java.io.OutputStream
        public synchronized void write(byte[] bArr, int i, int i2) {
            for (int i3 = 0; i3 < i2; i3++) {
                write(bArr[i + i3]);
            }
        }

        private void flushPending() {
            Logger logger;
            String sb = this.pending.toString();
            this.pending.setLength(0);
            if (sb.isEmpty() || (logger = Logger.loggerSingleton) == null) {
                return;
            }
            try {
                logger.appendToLogUnchecked(sb);
            } catch (Throwable unused) {
            }
        }
    }

    private void notifyLogListener(String str) {
        WeakReference<eventLogListener> weakReference = this.logListenerWeakReference;
        if (weakReference == null) {
            return;
        }
        eventLogListener eventloglistener = weakReference.get();
        if (eventloglistener == null) {
            this.logListenerWeakReference = null;
        } else {
            eventloglistener.onEventLogged(str);
        }
    }
}
