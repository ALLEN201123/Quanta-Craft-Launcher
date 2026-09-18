package com.qcl.launcher.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

/* loaded from: classes2.dex */
public final class Logging {
    public static final Logger LOG = Logger.getLogger("HMCL");
    private static ByteArrayOutputStream storedLogs = new ByteArrayOutputStream();

    private Logging() {
    }

    public static void start(Path path) {
        Logger logger = LOG;
        logger.setLevel(Level.ALL);
        logger.setUseParentHandlers(false);
        try {
            Files.createDirectories(path, new FileAttribute[0]);
            FileHandler fileHandler = new FileHandler(path.resolve("qcl.log").toAbsolutePath().toString());
            fileHandler.setLevel(Level.FINEST);
            fileHandler.setFormatter(DefaultFormatter.INSTANCE);
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            System.err.println("Unable to create qcl.log, " + e.getMessage());
        }
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(DefaultFormatter.INSTANCE);
        consoleHandler.setLevel(Level.FINER);
        LOG.addHandler(consoleHandler);
        StreamHandler streamHandler = new StreamHandler(storedLogs, DefaultFormatter.INSTANCE) { // from class: com.qcl.launcher.utils.Logging.1
            @Override // java.util.logging.StreamHandler, java.util.logging.Handler
            public synchronized void publish(LogRecord logRecord) {
                super.publish(logRecord);
                flush();
            }
        };
        try {
            streamHandler.setEncoding("UTF-8");
        } catch (UnsupportedEncodingException e2) {
            e2.printStackTrace();
        }
        streamHandler.setLevel(Level.ALL);
        LOG.addHandler(streamHandler);
    }

    public static void initForTest() {
        Logger logger = LOG;
        logger.setLevel(Level.ALL);
        logger.setUseParentHandlers(false);
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(DefaultFormatter.INSTANCE);
        consoleHandler.setLevel(Level.FINER);
        logger.addHandler(consoleHandler);
    }

    public static byte[] getRawLogs() {
        return storedLogs.toByteArray();
    }

    public static String getLogs() {
        return storedLogs.toString();
    }

    /* loaded from: classes2.dex */
    private static final class DefaultFormatter extends Formatter {
        static final DefaultFormatter INSTANCE = new DefaultFormatter();
        private final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");

        private DefaultFormatter() {
        }

        @Override // java.util.logging.Formatter
        public String format(LogRecord logRecord) {
            String format = String.format("[%s] [%s.%s/%s] %s%n", this.format.format(new Date(logRecord.getMillis())), logRecord.getSourceClassName(), logRecord.getSourceMethodName(), logRecord.getLevel().getName(), logRecord.getMessage());
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            if (logRecord.getThrown() != null) {
                PrintWriter printWriter = new PrintWriter(byteArrayOutputStream);
                try {
                    logRecord.getThrown().printStackTrace(printWriter);
                    printWriter.close();
                } catch (Throwable th) {
                    try {
                        printWriter.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                    throw th;
                }
            }
            return format + byteArrayOutputStream.toString();
        }
    }
}
