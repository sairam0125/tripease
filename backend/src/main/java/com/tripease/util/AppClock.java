package com.tripease.util;

import java.time.LocalDate;
import java.time.ZoneId;

/** All "today" logic uses Indian time so servers running in UTC behave correctly. */
public final class AppClock {
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private AppClock() {}

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }
}
