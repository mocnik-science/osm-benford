package com.OSMBenford.Helper;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

public class Progress {
    private static Progress pi = null;
    private int counterQuery = 0;

    private Progress() {}

    public static Progress getInstance() {
        if (Progress.pi == null) Progress.pi = new Progress();
        return Progress.pi;
    }

    public static void query(String message) {
        Progress pi = Progress.getInstance();
        pi.counterQuery += 1;
        Progress.log("%s - Query %4d - %s\n", LocalDateTime.now(), pi.counterQuery, message);
    }

    public static void starting() {
        Progress.log("%s - Starting\n", LocalDateTime.now());
    }

    public static void finished() {
        Progress.log("%s - Finished\n", LocalDateTime.now());
    }

    private static void log(String format, Object... args) {
        String s = String.format(format, args);
        System.out.print(s);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("../log", true));
            writer.write(s);
            writer.close();
        } catch (IOException e) {}
    }
}
