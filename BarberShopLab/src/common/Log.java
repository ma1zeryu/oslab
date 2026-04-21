package common;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Log {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public static synchronized void info(String msg) {
        System.out.println("[" + LocalTime.now().format(FMT) + "] " + msg);
    }
}