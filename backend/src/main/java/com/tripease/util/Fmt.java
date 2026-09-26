package com.tripease.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class Fmt {
    private Fmt() {}

    public static String inr(BigDecimal amount) {
        NumberFormat nf = NumberFormat.getIntegerInstance(new Locale("en", "IN"));
        return "₹" + nf.format(amount.longValue());
    }

    public static String duration(int minutes) {
        return (minutes / 60) + "h " + String.format("%02d", minutes % 60) + "m";
    }
}
