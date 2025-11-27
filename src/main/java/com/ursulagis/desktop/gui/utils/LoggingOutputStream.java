package com.ursulagis.desktop.gui.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

 public class LoggingOutputStream extends OutputStream {
    private final Logger logger;
    private final Level level;
    private StringBuilder stringBuilder;

    public LoggingOutputStream(Logger logger, Level level) {
        this.logger = logger;
        this.level = level;
        this.stringBuilder = new StringBuilder();
    }

    @Override
    public void write(int b) throws IOException {
        char c = (char) b;
        if (c == '\r' || c == '\n') {
            if (stringBuilder.length() > 0) {
                logger.log(level, stringBuilder.toString());
                stringBuilder = new StringBuilder(); // Reset for next line
            }
        } else {
            stringBuilder.append(c);
        }
    }

    @Override
    public void flush() throws IOException {
        if (stringBuilder.length() > 0) {
            logger.log(level, stringBuilder.toString());
            stringBuilder = new StringBuilder();
        }
    }

    @Override
    public void close() throws IOException {
        flush();
        super.close();
    }

    public static void installLogger() {
        Logger stdoutLogger = Logger.getLogger("STDOUT_LOGGER");
        Logger stderrLogger = Logger.getLogger("STDERR_LOGGER");

        PrintStream defaultOutStream = System.out;
        PrintStream defaultErrStream = System.err;

        // Redirect System.out to stdoutLogger at INFO level
        System.setOut(new PrintStream(new LoggingOutputStream(stdoutLogger, Level.INFO)));

        // Redirect System.err to stderrLogger at SEVERE level
        System.setErr(new PrintStream(new LoggingOutputStream(stderrLogger, Level.SEVERE)));

        // Now, any System.out.println() or System.err.println() will go to the loggers
        System.out.println("This message will be logged as INFO.");
        System.err.println("This error will be logged as SEVERE.");

        // Example of direct logger usage (for comparison)
        stdoutLogger.info("This is a direct INFO log message.");
    }
}