package com.example.stock_backend.twse;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.Optional;

public final class TwseBarParser {
    private TwseBarParser() {}

    public static Optional<TwseBar> parseRow(JsonNode row) {
        try {
            String taiwanDate = row.get(0).asText();
            if (taiwanDate == null || taiwanDate.isBlank() || taiwanDate.equals("--")) {
                return Optional.empty();
            }
            String[] parts = taiwanDate.split("/");
            int year = Integer.parseInt(parts[0]) + 1911;
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);
            Double volume = parseNumber(row.get(1).asText());
            Double open = parseNumber(row.get(3).asText());
            Double high = parseNumber(row.get(4).asText());
            Double low = parseNumber(row.get(5).asText());
            Double close = parseNumber(row.get(6).asText());
            if (volume == null || open == null || high == null || low == null || close == null) {
                return Optional.empty();
            }
            return Optional.of(new TwseBar(
                    LocalDate.of(year, month, day),
                    open, high, low, close, volume.longValue()
            ));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    static Double parseNumber(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.replace(",", "").trim();
        if (t.isEmpty() || t.equals("--")) {
            return null;
        }
        return Double.parseDouble(t);
    }
}
