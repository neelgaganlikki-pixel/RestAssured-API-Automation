package com.neel.api;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TestExecutionLogger {

    private PrintStream originalSystemOut;
    private PrintStream logPrintStream;
    private FileOutputStream fileOutputStream;

    public void startLogging() {

        try {
            // Save original console output
            originalSystemOut = System.out;

            // Create logs folder
            File logsFolder = new File("logs");

            if (!logsFolder.exists()) {
                logsFolder.mkdirs();
            }

            // Create timestamp
            String timestamp = new SimpleDateFormat(
                    "yyyy-MM-dd_HH-mm-ss"
            ).format(new Date());

            // Create log file
            File logFile = new File(
                    logsFolder,
                    "test-execution-" + timestamp + ".txt"
            );

            // Create file output stream
            fileOutputStream = new FileOutputStream(logFile);

            // Send output to both console and file
            logPrintStream = new PrintStream(
                    new TeeOutputStream(
                            originalSystemOut,
                            fileOutputStream
                    ),
                    true,
                    StandardCharsets.UTF_8
            );

            // Redirect System.out
            System.setOut(logPrintStream);

            System.out.println();
            System.out.println(
                    "=================================================="
            );
            System.out.println(
                    "             API TEST EXECUTION"
            );
            System.out.println(
                    "=================================================="
            );
            System.out.println(
                    "Execution Started : " + new Date()
            );
            System.out.println(
                    "Log File          : " + logFile.getAbsolutePath()
            );
            System.out.println(
                    "=================================================="
            );
            System.out.println();

        } catch (IOException e) {

            if (originalSystemOut != null) {
                originalSystemOut.println(
                        "ERROR: Unable to start test logging."
                );

                originalSystemOut.println(
                        "Reason: " + e.getMessage()
                );
            }
        }
    }

    public void stopLogging() {

        System.out.println();
        System.out.println(
                "=================================================="
        );
        System.out.println(
                "          API TEST EXECUTION COMPLETED"
        );
        System.out.println(
                "=================================================="
        );
        System.out.println(
                "Execution Completed : " + new Date()
        );
        System.out.println(
                "=================================================="
        );

        if (logPrintStream != null) {
            logPrintStream.flush();
        }

        if (fileOutputStream != null) {
            try {
                fileOutputStream.flush();
                fileOutputStream.close();
            } catch (IOException e) {
                if (originalSystemOut != null) {
                    originalSystemOut.println(
                            "ERROR closing log file: "
                                    + e.getMessage()
                    );
                }
            }
        }

        // Restore normal console output
        if (originalSystemOut != null) {
            System.setOut(originalSystemOut);
        }
    }

    /**
     * Writes output to both:
     * 1. VS Code terminal
     * 2. TXT log file
     */
    private static class TeeOutputStream extends OutputStream {

        private final OutputStream console;
        private final OutputStream file;

        public TeeOutputStream(
                OutputStream console,
                OutputStream file) {

            this.console = console;
            this.file = file;
        }

        @Override
        public void write(int b) throws IOException {
            console.write(b);
            file.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            console.write(b);
            file.write(b);
        }

        @Override
        public void write(
                byte[] b,
                int off,
                int len) throws IOException {

            console.write(b, off, len);
            file.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            console.flush();
            file.flush();
        }

        @Override
        public void close() throws IOException {
            file.close();
        }
    }
}
