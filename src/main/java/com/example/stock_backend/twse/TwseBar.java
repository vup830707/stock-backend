package com.example.stock_backend.twse;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public record TwseBar(
        LocalDate date,
        double open,
        double high,
        double low,
        double close,
        long volume
) {
    private static final DateTimeFormatter SLASH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    public String dateSlash() {
        return date.format(SLASH);
    }
}
